package cn.dbsec.auto;

import java.sql.Connection;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import cn.dbsec.dbfw.dao.baseline.BaselineManageDAO;
import cn.dbsec.dbfw.dao.database.DatabaseDao;
import cn.dbsec.dbfw.pojo.database.DatabaseAddress;
import cn.dbsec.dbfw.pojo.database.DatabaseInfo;
import cn.dbsec.dbfw.pojo.database.ProtectModeRule;
import cn.schina.dbfw.common.exception.DAOException;
import cn.schina.dbfw.common.exception.FlushEngineException;
import cn.schina.dbfw.common.exception.ServiceException;
import cn.schina.dbfw.common.lang.DBFWConstant;
import cn.schina.dbfw.config.Globals;
import cn.schina.dbfw.dao.core.utils.db.ConnectionManager;
import cn.schina.dbfw.dao.core.utils.db.TransactionManager;
import cn.schina.dbfw.pojo.LicenseInfo;
import cn.schina.dbfw.pojo.StaticDefined;
import cn.schina.dbfw.pojo.strategy.DBFWForDbAddr;
import cn.schina.dbfw.pojo.strategy.DBFWInfo;
import cn.schina.dbfw.pojo.strategy.GroupPort;
import cn.schina.dbfw.pojo.strategy.InterfaceGroup;
import cn.schina.dbfw.pojo.strategy.InterfaceInfo;
import cn.schina.dbfw.service.SystemLicenseSrever;
import cn.schina.dbfw.service.strategy.command.CreateDBCommand;
import cn.schina.dbfw.service.strategy.command.CreateNewNPlsCommand;
import cn.schina.dbfw.service.strategy.command.DeleteDBCommand;
import cn.schina.dbfw.service.strategy.command.FlushDbfwForDbInfoCommand;
import cn.schina.dbfw.service.strategy.command.FlushNfwCommand;
import cn.schina.dbfw.service.strategy.command.FlushSystemRuleChangeCommand;
import cn.schina.dbfw.service.strategy.command.KillNplsCommand;
import cn.schina.dbfw.service.strategy.command.NativeCommand;
import cn.schina.dbfw.service.strategy.command.NativeExecutor;

public class DatabaseServiceAuto {

	private DatabaseDao databaseDao = new DatabaseDao();

	/**
	 * 保存数据库信息
	 * 
	 * @param dbInfo
	 * @return
	 * @throws Exception
	 */
	public int saveDataBase(DatabaseInfo dbInfo) throws Exception {

		TransactionManager tx = null;

		int dbId = 0;

		try {

			dbId = dbInfo.getId();

			tx = TransactionManager.newTransactionManager();

			Connection conn = ConnectionManager.getConnection();

			// List<NativeCommand> commands = new ArrayList<NativeCommand>();
			List<NativeCommand> flushCommands = new ArrayList<NativeCommand>();

			int databaseAddressId = 0; // 记录最后插入的databaseaddressId值，因事务最后才提交

			List<DatabaseAddress> addressList = dbInfo.getDbAddrList();

			if (dbInfo.getId() == 0) { // 添加操作
				int i = 0;
				// databaseinfo id
				dbId = databaseDao.addXsecDatabaseAndGetId(conn, dbInfo.getName(), dbInfo.getDesc(),
						dbInfo.getVersion(), dbInfo.getDialect(), dbInfo.getIsdpa(), dbInfo.getIssox(),
						dbInfo.getIspci(), dbInfo.getIsglba(), dbInfo.getAuditMode(), dbInfo.getIpFilter(),
						dbInfo.getSqlFilter());
				// 记录当前插入的多地址列表的个数
				for (DatabaseAddress dbAddress : addressList) {
					// 获取addressId
					databaseAddressId = databaseDao.addAddressAndGetId(0, databaseAddressId, conn,
							dbAddress.getAddress(), dbAddress.getPort(), dbAddress.getServiceName(), dbId,
							dbAddress.getUserName(), dbAddress.getUserPwd(), i, dbAddress.getDynaPort());
					i++;
				}
				// 判断是否允许添加
				judgeAddDatabases(conn);

				// 修改操作
			} else {

				databaseDao.updateDBInfo(conn, dbInfo);

				// 多地址
				if (addressList != null && addressList.size() > 0) {

					// 记录当前插入的多地址列表的个数
					int j = 0;
					for (DatabaseAddress dbAddress : addressList) {

						// 用户名密码解密
						// dbAddress.setUserName(desEncrypt.strDec(dbAddress.getUserName(),
						// "20160101", "1", "1"));
						// dbAddress.setUserPwd(desEncrypt.strDec(dbAddress.getUserPwd(),
						// "20160101", "1", "1"));

						boolean addressIsExist = databaseDao.checkDBAddressIsExist(conn, dbAddress);
						if (dbAddress.getId() != 0) { // 修改 需刷新后台
							int dbIdCheck = databaseDao.getDbIdByAddress(conn, dbAddress);
							if (addressIsExist && dbIdCheck != dbInfo.getId()) { // 不是当前数据库抛异常
								throw new DAOException("该IP、端口的数据库已经被添加!" + dbAddress.getAddress() + ":"
										+ dbAddress.getPort());
							} else {
								databaseDao.updateDBAddress(conn, dbAddress);
							}
						} else { // 新增
							if (addressIsExist) {
								throw new DAOException("该IP、端口的数据库已经被添加!" + dbAddress.getAddress() + ":"
										+ dbAddress.getPort());
							} else {
								// 获取addressId
								databaseAddressId = databaseDao.addAddressAndGetId(1, databaseAddressId, conn,
										dbAddress.getAddress(), dbAddress.getPort(), dbAddress.getServiceName(),
										dbInfo.getId(), dbAddress.getUserName(), dbAddress.getUserPwd(), j,
										dbAddress.getDynaPort());
								j++;
							}
						}

					}
					// 判断是否允许添加
					judgeAddDatabases(conn);
				}
			}

			List<NativeCommand> removeCommands = new ArrayList<NativeCommand>();
			List<NativeCommand> removeFlushCommands = removeDbfwForDb(conn, StaticDefined.DBFW_ID, dbId, removeCommands);

			DBFWInfo dbfwInfo = databaseDao.getDBFWInfoById(conn, StaticDefined.DBFW_ID);

			tx.submit();

			tx = TransactionManager.newTransactionManager();

			try {
				NativeExecutor.execute(removeCommands);
			} catch (ServiceException e) {
				if (dbfwInfo != null && dbfwInfo.getInstStat() == 3) {
					throw new FlushEngineException("引擎未启动，启动引擎后操作生效！");
				} else {
					throw e;
				}
			}

			try {
				NativeExecutor.execute(removeFlushCommands);
			} catch (ServiceException e) {
				if (dbfwInfo != null && dbfwInfo.getInstStat() == 3) {
					throw new FlushEngineException("引擎未启动，启动引擎后操作生效！");
				} else {
					throw e;
				}
			}

			List<DBFWForDbAddr> dbfwForDbAddress = new ArrayList<DBFWForDbAddr>();

			// 根据数据库id查询多地址
			List<DatabaseAddress> queryAddressList = databaseDao.queryDBAddressInfo(conn, dbId);

			List<DatabaseAddress> dbAddrList = dealDatabaseAddressInfo(queryAddressList, addressList);

			for (DatabaseAddress databaseAddress : dbAddrList) {
				DBFWForDbAddr dbAddr = new DBFWForDbAddr();
				dbAddr.setDbfwId(StaticDefined.DBFW_ID);// 实例ID默认为1
				dbAddr.setAddressId(databaseAddress.getId());
				dbAddr.setMonitorType(dbInfo.getMonitorType());
				dbAddr.setGroupId(databaseAddress.getGroupId());
				dbAddr.setGroupIdStr(databaseAddress.getGroupIdStr());
				dbAddr.setPortId(databaseAddress.getPortId());
				dbAddr.setRemoteHost(databaseAddress.getRemoteHost());
				dbAddr.setRemotePort(databaseAddress.getRemotePort());
				dbAddr.setState(1);
				dbfwForDbAddress.add(dbAddr);
			}

			List<NativeCommand> commands = addDbfwForDbAddrs(conn, dbfwForDbAddress, flushCommands);

			int activeBaseline = databaseDao.getActiveBaseline(conn, StaticDefined.DBFW_ID, dbId);

			ProtectModeRule protectModeRule = databaseDao.getCurrentProtectMode(conn, StaticDefined.DBFW_ID, dbId,
					activeBaseline);

			boolean brushBaseline = false;
			// 有变化时，更新模式信息
			if (protectModeRule == null || dbInfo.getRunMode() != protectModeRule.getRunMode()
					|| dbInfo.getLearnMode() != protectModeRule.getLearnMode() || dbInfo.getLearnInterval() > 0
					|| dbInfo.getLearnEndTime() != protectModeRule.getLearnEndTime()) {
				protectModeRule.setRunMode(dbInfo.getRunMode());
				protectModeRule.setLearnMode(dbInfo.getLearnMode());
				protectModeRule.setLearnInterval(dbInfo.getLearnInterval());
				protectModeRule.setLearnEndTime(dbInfo.getLearnEndTime());

				databaseDao.updateProtectMode(conn, activeBaseline, protectModeRule);
				brushBaseline = true;
			}

			// 保护时间未计时
			if (!databaseDao.startProtecting(conn)) {
				databaseDao.setProtectStartTime(conn, new Timestamp(System.currentTimeMillis()));
			}

			tx.submit();

			if (brushBaseline) {
				BaselineManageDAO.getDAO().baselineManage(conn, StaticDefined.DBFW_ID, dbId, activeBaseline);
				BaselineManageDAO.getDAO().brushBackGround(0);
			}

			try {
				NativeExecutor.execute(commands);
			} catch (ServiceException e) {
				if (dbfwInfo != null && dbfwInfo.getInstStat() == 3) {
					throw new FlushEngineException("引擎未启动，启动引擎后操作生效！");
				} else {
					throw e;
				}
			}

			try {
				NativeExecutor.execute(flushCommands);
			} catch (ServiceException e) {
				if (dbfwInfo != null && dbfwInfo.getInstStat() == 3) {
					throw new FlushEngineException("引擎未启动，启动引擎后操作生效！");
				} else {
					throw e;
				}
			}

		} catch (Exception e) {
			if (!(e instanceof FlushEngineException)) {
				tx.rollback();
			}
			throw e;
		} finally {
			ConnectionManager.closeConnection();
		}
		return dbId;
	}

	/**
	 * 添加保护数据库地址
	 * 
	 * @param list
	 */
	private synchronized List<NativeCommand> addDbfwForDbAddrs(Connection conn, List<DBFWForDbAddr> list,
			List<NativeCommand> flushCommands) {

		DBFWInfo dbfwInfo = null;

		List<NativeCommand> commands = new ArrayList<NativeCommand>();

		// 重新加载管理IP端口
		Globals.reloadConfigs();

		NativeCommand command = null;

		// PHASE 0. 校验是否允许添加此监听
		boolean checkResult = false;
		String errMsg = "";
		for (DBFWForDbAddr dbfwForDb : list) {
			switch (dbfwForDb.getMonitorType()) {
			case DBFWConstant.DEPLOY_REMOTEPROXY:
				checkResult = databaseDao.checkRemoteDBFWAddress(conn, dbfwForDb.getPortId());
				if (!checkResult)
					errMsg = "该端口已经占用";
				break;
			case DBFWConstant.DEPLOY_BYWAY:
				String groupIds = dbfwForDb.getGroupIdStr();
				int groupIdCount = 0;
				if (groupIds.contains(";")) {
					String[] groupIdArr = groupIds.split(";");
					for (int i = 0; i < groupIdArr.length; i++) {
						checkResult = databaseDao.checkBywayDBFWAddress(conn, dbfwForDb.getDbfwId(),
								Integer.valueOf(groupIdArr[i]));
						if (!checkResult) {
							groupIdCount++;
						}
					}
				} else {
					checkResult = databaseDao.checkBywayDBFWAddress(conn, dbfwForDb.getDbfwId(),
							Integer.valueOf(dbfwForDb.getGroupIdStr()));
					if (!checkResult) {
						groupIdCount++;
					}
				}
				if (groupIdCount > 0) {
					errMsg = "同一安全实例只允许监听" + DBFWConstant.MAX_NPC_PER_INST + "块网卡";
				}
				break;
			case DBFWConstant.DEPLOY_NATIVEPROXY:
				checkResult = databaseDao.checkNativeDBFWAddress(conn, dbfwForDb.getPortId());
				if (!checkResult)
					errMsg = "该端口已经占用";
				break;
			case DBFWConstant.DEPLOY_HALFTRANS:
				checkResult = databaseDao.checkHalftransDBFWAddress(conn, dbfwForDb.getAddressId(),
						dbfwForDb.getPortId());
				if (!checkResult) {
					// errMsg = "该数据库此端口已被重定向至其它端口"; fix bug 3054
					errMsg = "同一个数据库不允许同时被多个安全实例以网桥模式保护";
				}
				// 判断网桥组是否重用
				groupIds = dbfwForDb.getGroupIdStr() + ";";
				String[] groupIdArr = groupIds.split(";");
				for (int i = 0; i < groupIdArr.length; i++) {
					if (!groupIdArr[i].equals("")) {
						checkResult = databaseDao
								.checkHalftransDBFWAGroupId(conn, dbfwForDb.getDbfwId(), groupIdArr[i]);
						if (!checkResult) {
							errMsg = "同一个网桥组不能同时用多个安全实例关联！";
							break;
						}
					}
				}
				break;
			default:
				break;
			}

			if (!checkResult) {
				throw new ServiceException("监听配置错误：" + errMsg);
			}
		}

		for (DBFWForDbAddr dbfwForDbAddr : list) {
			DatabaseAddress address = databaseDao.getDatabaseAddressesById(conn, dbfwForDbAddr.getAddressId());
			int port = databaseDao.getPortNumById(conn, dbfwForDbAddr.getPortId());
			//
			if (dbfwForDbAddr.getMonitorType() == DBFWConstant.DEPLOY_HALFTRANS) {
				port = databaseDao.getBridgePort(conn);
			}
			List<GroupPort> all_port_list = databaseDao.getAllGroupPort(conn);
			if (port > 15000) {
				if (all_port_list.get(0).getPortNum() != 11001) {
					port = 11001;
				} else {
					for (int i = 0; i < all_port_list.size() - 1; i++) {
						if (all_port_list.get(i + 1).getPortNum() - all_port_list.get(i).getPortNum() > 0) {
							port = all_port_list.get(i).getPortNum() + 1;
							break;
						}
					}
				}
			}

			dbfwInfo = databaseDao.getDBFWInfoById(conn, dbfwForDbAddr.getDbfwId());
			String ifNameVal = databaseDao.getIfName(conn, dbfwForDbAddr.getGroupId());
			if (dbfwForDbAddr.getMonitorType() != DBFWConstant.DEPLOY_BYWAY
					&& dbfwForDbAddr.getMonitorType() != DBFWConstant.DEPLOY_HALFTRANS) {
				command = new CreateDBCommand(ifNameVal, dbfwForDbAddr.getMonitorType(), address.getAddress(),
						address.getPort(), port, dbfwForDbAddr.getGroupId(), "");
				commands.add(command);
				System.out.println("CreateDBCommand参数：" + Globals.MANAGE_ETHNAME + "," + dbfwForDbAddr.getMonitorType()
						+ "," + address.getAddress() + "," + address.getPort() + "," + port + ","
						+ dbfwForDbAddr.getGroupId());
				InterfaceGroup group = databaseDao.getGroupById(conn, dbfwForDbAddr.getGroupId());
				if (group == null) {
					throw new ServiceException("无法在策略中心中找到组：" + dbfwForDbAddr.getGroupName());
				}

				if (dbfwForDbAddr.getMonitorType() == DBFWConstant.DEPLOY_HALFTRANS) {
					if (dbfwInfo != null && dbfwInfo.getInstStat() != 3) {
						command = new CreateNewNPlsCommand(dbfwInfo.getAddress(), dbfwInfo.getPort(), port,
								address.getAddress(), address.getPort(), dbfwForDbAddr.getMonitorType(),
								group.getGroupIp());
						commands.add(command);
					}
				} else {
					command = new CreateNewNPlsCommand(dbfwInfo.getAddress(), dbfwInfo.getPort(), port,
							address.getAddress(), address.getPort(), dbfwForDbAddr.getMonitorType(), group.getGroupIp());
					commands.add(command);
				}
			}

			// 检查dbfwId安全实例下是否已经存在非旁路的部署模式
			/*
			 * boolean deployModeMatch = this.deployModeMatch(conn,
			 * dbfwForDbAddr.getDbfwId(), dbfwForDbAddr.getMonitorType()); if
			 * (!deployModeMatch) { if (dbfwForDbAddr.getMonitorType() == 2) {
			 * removeDatabaseAndDBAddress(list); throw new
			 * ServiceException("该引擎下已经存在非旁路部署模式，不能添加旁路部署模式"); } else {
			 * removeDatabaseAndDBAddress(list); throw new
			 * ServiceException("该引擎下已经存在旁路部署模式，不能添加非旁路部署模式"); } }
			 */
			// PHASE 1. 添加至dbfw_fordb
			databaseDao.addDbfwForDbAddr(conn, dbfwForDbAddr.getDbfwId(), dbfwForDbAddr.getAddressId(),
					dbfwForDbAddr.getMonitorType(), dbfwForDbAddr.getClientGroupId());
			// PHASE 2. 根据不同部署模式
			switch (dbfwForDbAddr.getMonitorType()) {
			case DBFWConstant.DEPLOY_REMOTEPROXY:
				databaseDao.addRemoteDbfwForAddrInfo(conn, dbfwForDbAddr.getRemoteHost(),
						dbfwForDbAddr.getRemotePort(), dbfwForDbAddr.getDbfwId(), dbfwForDbAddr.getAddressId(),
						dbfwForDbAddr.getPortId());
				break;
			case DBFWConstant.DEPLOY_BYWAY:
				List<InterfaceInfo> interfaces = null;
				String groupIds = dbfwForDbAddr.getGroupIdStr();
				if (groupIds.contains(";")) {
					String[] groupIdArr = groupIds.split(";");
					for (int i = 0; i < groupIdArr.length; i++) {
						interfaces = databaseDao.getInterfaceByGroup(conn, Integer.valueOf(groupIdArr[i]));
						// 判断部署模式与网卡组是否匹配
						int groupType = databaseDao.getInterfaceGroupType(conn, Integer.valueOf(groupIdArr[i]));
						if (groupType != DBFWConstant.IFGROUPTYPE_BYPASS) {
							throw new ServiceException("网卡组与部署模式不匹配");
						}
						databaseDao.addBywayDbfwForAddrInfo(conn, Integer.valueOf(groupIdArr[i]),
								dbfwForDbAddr.getDbfwId(), dbfwForDbAddr.getAddressId(), dbfwInfo, interfaces.get(0),
								commands, flushCommands);
					}
				} else {
					interfaces = databaseDao.getInterfaceByGroup(conn, Integer.valueOf(dbfwForDbAddr.getGroupIdStr()));
					// 判断部署模式与网卡组是否匹配
					int groupType = databaseDao.getInterfaceGroupType(conn,
							Integer.valueOf(dbfwForDbAddr.getGroupIdStr()));
					if (groupType != DBFWConstant.IFGROUPTYPE_BYPASS) {
						throw new ServiceException("网卡组与部署模式不匹配");
					}
					databaseDao.addBywayDbfwForAddrInfo(conn, Integer.valueOf(dbfwForDbAddr.getGroupIdStr()),
							dbfwForDbAddr.getDbfwId(), dbfwForDbAddr.getAddressId(), dbfwInfo, interfaces.get(0),
							commands, flushCommands);
				}
				break;
			case DBFWConstant.DEPLOY_NATIVEPROXY:
				databaseDao.addNativeDbfwForAddrInfo(conn, dbfwForDbAddr.getDbfwId(), dbfwForDbAddr.getAddressId(),
						dbfwForDbAddr.getPortId());
				break;
			case DBFWConstant.DEPLOY_HALFTRANS:
				interfaces = null;
				groupIds = dbfwForDbAddr.getGroupIdStr();
				if (groupIds.contains(";")) {
					String[] groupIdArr = groupIds.split(";");
					for (int i = 0; i < groupIdArr.length; i++) {
						interfaces = databaseDao.getInterfaceByGroup(conn, Integer.valueOf(groupIdArr[i]));
						// 判断部署模式与网卡组是否匹配
						int groupType = databaseDao.getInterfaceGroupType(conn, Integer.valueOf(groupIdArr[i]));
						if (groupType != DBFWConstant.IFGROUPTYPE_BRIDGE) {
							throw new ServiceException("网卡组与部署模式不匹配");
						}
						databaseDao.addBypassDbfwForAddrInfo(conn, Integer.valueOf(groupIdArr[i]),
								dbfwForDbAddr.getDbfwId(), dbfwForDbAddr.getAddressId(), dbfwInfo, interfaces.get(0),
								commands, flushCommands);
					}
				} else {
					interfaces = databaseDao.getInterfaceByGroup(conn, Integer.valueOf(dbfwForDbAddr.getGroupIdStr()));
					// 判断部署模式与网卡组是否匹配
					int groupType = databaseDao.getInterfaceGroupType(conn,
							Integer.valueOf(dbfwForDbAddr.getGroupIdStr()));
					if (groupType != DBFWConstant.IFGROUPTYPE_BRIDGE) {
						throw new ServiceException("网卡组与部署模式不匹配");
					}
					databaseDao.addBypassDbfwForAddrInfo(conn, Integer.valueOf(dbfwForDbAddr.getGroupIdStr()),
							dbfwForDbAddr.getDbfwId(), dbfwForDbAddr.getAddressId(), dbfwInfo, interfaces.get(0),
							commands, flushCommands);
				}
				break;
			default:
				break;
			}

			// 初始化DBFW-DB的数据信息
			int dbId = databaseDao.getDatabaseAddressesById(conn, dbfwForDbAddr.getAddressId()).getDatabaseId();
			boolean inited = databaseDao.dbfwDbInited(conn, dbfwForDbAddr.getDbfwId(), dbId);
			if (!inited) {
				databaseDao.initDbfwDb(conn, dbfwForDbAddr.getDbfwId(), dbId);
			}

			// PHASE 3. 刷新SMON
			command = new FlushDbfwForDbInfoCommand(dbfwInfo.getAddress(), dbfwInfo.getPort(), dbId);
			flushCommands.add(command);
		}

		// 增加刷新告警配置
		NativeCommand commandRule = new FlushSystemRuleChangeCommand();
		flushCommands.add(commandRule);

		if (list != null) {
			// 接口调用重新调整旁路、网桥修改为相同的调用
			if (list.get(0).getMonitorType() == DBFWConstant.DEPLOY_HALFTRANS
					|| list.get(0).getMonitorType() == DBFWConstant.DEPLOY_BYWAY) {
				command = new FlushNfwCommand(dbfwInfo.getAddress(), dbfwInfo.getPort(), list.get(0).getDbfwId());
				commands.add(command);
			}
		}

		return commands;
	}

	/**
	 * 判断是否允许添加数据库
	 */
	private void judgeAddDatabases(Connection conn) throws Exception {

		LicenseInfo licenseInfo = null;

		// 获取当前已添加的ip:port个数
		int dbAddressCount = databaseDao.getDBAddrSum(conn);
		// 获取当前license信息
		licenseInfo = SystemLicenseSrever.getLicenseInfoFinal();
		if (licenseInfo != null) {
			if (licenseInfo.getAddressLimit() == 1) { // 允许添加64个ip:port
														// 旧判断现在已经作废:2016-05-23
				if (dbAddressCount > 64) {
					throw new ServiceException("当前允许添加的数据库实例数(64)已达上限!");
				}
			} else {
				if (dbAddressCount > licenseInfo.getAddressLimit()) {
					throw new ServiceException("当前允许添加的数据库实例数(" + licenseInfo.getAddressLimit() + ")已达上限!");
				}
			}
		}
	}

	/**
	 * 处理数据库地址
	 * 
	 * @param addressList
	 *            通过数据库id查询的地址列表
	 * @param netAddressList
	 *            通过页面传递查询的地址列表
	 * @return
	 */
	private List<DatabaseAddress> dealDatabaseAddressInfo(List<DatabaseAddress> addressList,
			List<DatabaseAddress> netAddressList) {

		List<DatabaseAddress> list = new ArrayList<DatabaseAddress>();

		for (int i = 0; i < addressList.size(); i++) {
			DatabaseAddress temAddressI = addressList.get(i);
			// ip地址
			String addressI = temAddressI.getAddress();
			// 端口号
			int portI = temAddressI.getPort();

			for (int j = 0; j < netAddressList.size(); j++) {
				//
				DatabaseAddress temAddressII = netAddressList.get(j);
				// ip地址
				String addressII = temAddressII.getAddress();
				// 端口号
				int portII = temAddressII.getPort();

				// 非空判断
				if (StringUtils.isNotEmpty(addressI) && StringUtils.isNotEmpty(addressII)) {
					// 赋值操作
					if (addressI.equals(addressII) && (portI == portII)) {
						temAddressI.setGroupId(temAddressII.getGroupId());
						temAddressI.setPortId(temAddressII.getPortId());
						temAddressI.setRemoteHost(temAddressII.getRemoteHost());
						temAddressI.setRemotePort(temAddressII.getRemotePort());
						temAddressI.setGroupIdStr(temAddressII.getGroupIdStr());
						list.add(temAddressI);
						break;
					}
				}
			}
		}
		return list;
	}

	/**
	 * 移除安全实例中的数据库地址
	 * 
	 * @param dbfwId
	 * @param dbId
	 * @param commands
	 */
	private List<NativeCommand> removeDbfwForDb(Connection conn, int dbfwId, int dbId, List<NativeCommand> commands) {

		// 重新加载管理IP端口
		Globals.reloadConfigs();

		NativeCommand command = null;

		List<NativeCommand> flushCommands = new ArrayList<NativeCommand>();

		int montiType = 0;
		DBFWInfo dbfwInfo = null;

		List<DatabaseAddress> addresses = databaseDao.getDatabaseAddresses(conn, dbId);

		for (DatabaseAddress address : addresses) {
			List<DBFWForDbAddr> dbfwForDbAddr = databaseDao.getDBFWForDbAddrByAddressAndDbfwList(conn, address.getId(),
					dbfwId);
			if (dbfwForDbAddr == null || dbfwForDbAddr.size() == 0) {
				continue;
			}
			int port = databaseDao.getPortNumById(conn, dbfwForDbAddr.get(0).getPortId());
			dbfwInfo = databaseDao.getDBFWInfoById(conn, dbfwForDbAddr.get(0).getDbfwId());

			if (dbfwForDbAddr.get(0).getMonitorType() != DBFWConstant.DEPLOY_BYWAY
					&& dbfwForDbAddr.get(0).getMonitorType() != DBFWConstant.DEPLOY_HALFTRANS) {
				int cnt = databaseDao.getProxyPortCount(conn, address.getId(), dbfwForDbAddr.get(0).getGroupId());
				String ifNameVal = databaseDao.getIfName(conn, dbfwForDbAddr.get(0).getGroupId());
				if (cnt < 2) {
					command = new DeleteDBCommand(ifNameVal, dbfwForDbAddr.get(0).getMonitorType(),
							address.getAddress(), address.getPort(), port, dbfwForDbAddr.get(0).getGroupId(), "");
					commands.add(command);
				}
				InterfaceGroup group = databaseDao.getGroupById(conn, dbfwForDbAddr.get(0).getGroupId());
				if (group == null) {
					throw new ServiceException("无法在策略中心中找到组：" + dbfwForDbAddr.get(0).getGroupName());
				}
				command = new KillNplsCommand(dbfwInfo.getAddress(), dbfwInfo.getPort(), port, address.getAddress(),
						address.getPort(), dbfwForDbAddr.get(0).getMonitorType(), group.getGroupIp());
				commands.add(command);
			}

			montiType = dbfwForDbAddr.get(0).getMonitorType();
			switch (dbfwForDbAddr.get(0).getMonitorType()) {
			case DBFWConstant.DEPLOY_REMOTEPROXY:
				databaseDao.removeRemoteProxyAndDbfwForAddr(conn, dbfwForDbAddr.get(0).getRemoteId(), dbfwForDbAddr
						.get(0).getPortId(), dbfwId, address.getId());
				break;
			case DBFWConstant.DEPLOY_BYWAY:
				databaseDao.removeBywayAndDbfwForAddr(conn, dbfwId, address.getId());
				break;
			case DBFWConstant.DEPLOY_NATIVEPROXY:
				databaseDao.removeNativeProxyAndDbfwForAddr(conn, dbfwForDbAddr.get(0).getPortId(), dbfwId,
						address.getId());
				break;
			case DBFWConstant.DEPLOY_HALFTRANS:
				databaseDao.removeHalfTransAndDbfwForAddr(conn, dbfwForDbAddr.get(0).getPortId(), dbfwId,
						address.getId());
				break;
			default:
				break;
			}
			databaseDao.removeDbfwForDbAddr(conn, dbfwId, address.getId());

			// PHASE 3. 刷新SMON
			command = new FlushDbfwForDbInfoCommand(dbfwInfo.getAddress(), dbfwInfo.getPort(), dbId);
			flushCommands.add(command);
			// 增加刷新告警配置
			NativeCommand commandRule = new FlushSystemRuleChangeCommand();
			flushCommands.add(commandRule);
		}
		// 解除关系时将dbfw_fordb中所有相应address_id不为空的记录删除，为空的记录isdelete置1
		// (fjw20130821)
		databaseDao.deleteDbfwForDb(conn, dbfwId, dbId);

		// 清除DBFW-DB的数据信息
		databaseDao.deinitDbfwDb(conn, dbfwId, dbId);

		// 接口调用重新调整旁路、网桥修改为相同的调用
		if (montiType == DBFWConstant.DEPLOY_HALFTRANS || montiType == DBFWConstant.DEPLOY_BYWAY) {
			command = new FlushNfwCommand(dbfwInfo.getAddress(), dbfwInfo.getPort(), dbfwId);
			commands.add(command);
		}

		return flushCommands;
	}

}
