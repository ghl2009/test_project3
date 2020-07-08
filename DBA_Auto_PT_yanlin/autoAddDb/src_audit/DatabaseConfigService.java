package cn.schina.dbfw.service.strategy;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;

import cn.schina.dbfw.base.tools.DesEncrypt;
import cn.schina.dbfw.common.lang.DBFWConstant;
import cn.schina.dbfw.common.lang.ServiceException;
import cn.schina.dbfw.dao.strategy.DatabaseConfigDAO;
import cn.schina.dbfw.filters.SystemAuditFilter;
import cn.schina.dbfw.pojo.AuditLog;
import cn.schina.dbfw.pojo.DBAudit;
import cn.schina.dbfw.pojo.DBConfigDetail;
import cn.schina.dbfw.pojo.strategy.DBFWForDbAddr;
import cn.schina.dbfw.pojo.strategy.DatabaseAddress;
import cn.schina.dbfw.pojo.strategy.DatabaseInfo;
import cn.schina.dbfw.pojo.strategy.DatabaseMirInfo;
import cn.schina.dbfw.pojo.strategy.InterfaceGroup;
import cn.schina.dbfw.pojo.strategy.NoSession;
import cn.schina.dbfw.pojo.strategy.UserInfo;
import cn.schina.dbfw.pojo.strategy.dbversion;
import cn.schina.dbfw.service.strategy.command.NativeCommand;
import cn.schina.dbfw.util.SystemAuditResultSingelton;
import cn.schina.dbfw.util.json.JSONUtil;

public class DatabaseConfigService {

	private DatabaseConfigDAO databaseConfigDAO = DatabaseConfigDAO.getDAO();
	private AuditLog auditLog = new AuditLog();

	/**
	 * 获取系统中所有注册的受保护数据库
	 * 
	 * @return
	 */
	public List<DatabaseInfo> getAllDatabases() {
		return databaseConfigDAO.getDatabases(0);
	}

	/**
	 * 获取审计对象列表信息
	 * 
	 * @return
	 */
	public Map<DatabaseInfo, List<DatabaseAddress>> getAllDatabases2Map(int uid) {
		Map<DatabaseInfo, List<DatabaseAddress>> mapDbInfoAndAddress = new TreeMap<DatabaseInfo, List<DatabaseAddress>>(
				new Comparator<DatabaseInfo>() {
					public int compare(DatabaseInfo o1, DatabaseInfo o2) {
						return o1.getId() - o2.getId();
					}
				});
		if (uid == -1) {
			HttpSession session = SystemAuditFilter.getHttpSession();
			if (session.getAttribute("currentUserInfoId") != null) {
				HashMap userinfo = (HashMap) session.getAttribute("currentUserInfoId");
				uid = Integer.valueOf((String) userinfo.get("UID"));
			}
		}
		// 获取所有的审计对象
		List<DatabaseInfo> databaseInfos = databaseConfigDAO.getDatabases(uid);
		if (databaseInfos != null && databaseInfos.size() > 0) {
			for (DatabaseInfo databaseInfo : databaseInfos) {
				mapDbInfoAndAddress.put(databaseInfo, getDatabaseAddresses(databaseInfo.getId()));
			}
		}
		return mapDbInfoAndAddress;
	}

	/**
	 * 获取系统中所有的镜像数据库信息
	 * 
	 * @return
	 */
	public List<DatabaseMirInfo> getAllMirDatabases() {
		return databaseConfigDAO.getAllMirDatabases();
	}

	/**
	 * 获取已忽略的镜像数据列表
	 */
	public List<DatabaseMirInfo> getIgnoreMirDatabases() {
		return databaseConfigDAO.getIgnoreMirDatabases();
	}

	/**
	 * 获取镜像数据库的总数(不包括已忽略的数据库)
	 * 
	 * @return
	 */
	public int getMirDatabaseCount() {
		return databaseConfigDAO.getMirDatabaseCount();
	}

	/**
	 * 获取指定id的镜像数据
	 * 
	 * @param mirDBId
	 * @return
	 */
	public DatabaseMirInfo getMirDatabaseById(int mirDbId) {
		return databaseConfigDAO.getMirDatabaseById(mirDbId);
	}

	/**
	 * 忽略镜像数据
	 * 
	 * @param addressId
	 *            镜像数据addressId
	 */
	public void ignoreMirData(int addressId) {
		databaseConfigDAO.ignoreMirData(addressId);
	}

	/**
	 * 恢复此已忽略的镜像数据
	 * 
	 * @param addressId
	 */
	public void recoveryMirData(int addressId) {
		databaseConfigDAO.recoveryMirData(addressId);
	}

	/**
	 * 获取已有的同类型的数据库
	 */
	public List<DatabaseInfo> getExistDatabasesByType(int dbType) {
		return databaseConfigDAO.getExistDatabasesByType(dbType);
	}

	/**
	 * 
	 * 保存所有的审计对象信息 镜像数据添加
	 * 
	 * @param databaseInfos
	 *            审计对象（被保护数据库）与数据库地址的映射关系.<br>
	 *            KEY是审计对象的Json格式，Value为数据库地址的Json格式
	 * @param interfaceGroup
	 */
	public void saveAllAudit(Map<String, String> databaseInfos, InterfaceGroup interfaceGroup) {
		// 加入审计日志
		auditLog.setEventDesc("配置向导：保存审计对象和设置管理IP");
		auditLog.setDesigner("配置向导");
		auditLog.setMovement("保存审计对象和设置管理IP");
		try {
			databaseConfigDAO.saveAllAudit(convertDatabaseInfoMap(databaseInfos), interfaceGroup);
			auditLog.setEventResult("1");
			auditLog.setAlterBefore("保存审计对象和设置管理IP成功");
		} catch (Exception e) {
			auditLog.setEventResult("2");
			auditLog.setAlterBefore("保存审计对象和设置管理IP失败;原因：" + e.getLocalizedMessage());
			throw new ServiceException("保存审计对象和设置管理IP失败 " + e.getLocalizedMessage());
		} finally {
			SystemAuditResultSingelton.getInstance().insertAuditLog(auditLog);
		}
	}

	/**
	 * 保存基本配置页面的审计对象
	 * 
	 * @param databaseInfos
	 */
	public void saveBasicAudit(Map<String, String> databaseInfos, int dbId, int op) {
		// 加入审计日志 flag=1 添加 2修改
		auditLog.setEventDesc(dbId == 0 ? "添加审计对象：手动添加" : "修改审计对象");
		auditLog.setDesigner("概况");
		auditLog.setMovement(dbId == 0 ? "手动添加审计对象" : "修改审计对象");
		String alterBeforeInfo = "", alterAfterInfo = "";
		try {
			Map<DatabaseInfo, List<DatabaseAddress>> map = convertDatabaseInfoMap(databaseInfos);
			for (Map.Entry<DatabaseInfo, List<DatabaseAddress>> entry : map.entrySet()) {
				alterBeforeInfo = getAuditLogMsg(entry.getKey(), getDatabaseAddresses(entry.getKey() != null ? entry
						.getKey().getId() : -1)); // 修改前
				alterAfterInfo = getAuditLogMsg(entry.getKey(), entry.getValue()); // 修改后
			}
			if (dbId != 0) {
				auditLog.setAlterBefore(alterBeforeInfo);
			}
			databaseConfigDAO.saveBasicAudit(map, op);
			auditLog.setEventResult("1");
			if (dbId == 0) {
				auditLog.setAlterBefore(alterAfterInfo);
			} else {
				auditLog.setAlterAfter(alterAfterInfo);
			}
		} catch (Exception e) {
			auditLog.setEventResult("2");
			auditLog.setAlterBefore((dbId == 0 || op == 1 ? "添加" : "修改") + "失败;原因：" + e.getLocalizedMessage());
			throw new ServiceException((dbId == 0 || op == 1 ? "添加失败: " : "修改失败: ") + e.getLocalizedMessage());
		} finally {
			SystemAuditResultSingelton.getInstance().insertAuditLog(auditLog);
		}
	}

	/**
	 * 保存基本配置页面的审计对象
	 * 
	 * @param databaseInfos
	 */
	public void saveBasicAuditNoSelfLog(Map<String, String> databaseInfos, int dbId, int op) {
		// 加入审计日志 flag=1 添加 2修改
		try {
			Map<DatabaseInfo, List<DatabaseAddress>> map = convertDatabaseInfoMap(databaseInfos);
			databaseConfigDAO.saveBasicAudit(map, op);
		} catch (Exception e) {
			throw new ServiceException((dbId == 0 || op == 1 ? "添加失败: " : "修改失败: ") + e.getLocalizedMessage());
		}
	}

	public String getAuditLogMsg(DatabaseInfo databaseInfo, List<DatabaseAddress> addresses) {
		StringBuffer temp = new StringBuffer();
		temp.append("审计对象名称：");
		temp.append(databaseInfo != null ? databaseInfo.getName() : " ");
		if (addresses != null && addresses.size() > 0) {
			temp.append("    IP地址:端口=[");
			for (DatabaseAddress dbAddress : addresses) {
				temp.append(dbAddress.getAddress() + ":" + dbAddress.getPort() + ",");
			}
			temp.deleteCharAt(temp.length() - 1).append("]");
		}
		return temp.toString();
	}

	@SuppressWarnings("unchecked")
	public Map<DatabaseInfo, List<DatabaseAddress>> convertDatabaseInfoMap(Map<String, String> databaseInfos) {

		// 由于传入的key是字符类型，需要将json转为对象
		Map<DatabaseInfo, List<DatabaseAddress>> dbInfos = new HashMap<DatabaseInfo, List<DatabaseAddress>>();
		for (String dbJson : databaseInfos.keySet()) {
			// 转换审计对象
			DatabaseInfo dbInfo = (DatabaseInfo) JSONUtil.JSONStringtoBean(dbJson, DatabaseInfo.class);
			// 转换数据库地址列表
			List<DatabaseAddress> addrs = (List<DatabaseAddress>) JSONUtil.JSONStringtoList(databaseInfos.get(dbJson),
					DatabaseAddress.class);
			//
			dbInfos.put(dbInfo, addrs);
		}
		return dbInfos;
	}

	/**
	 * 将镜像数据添加到已有同类型的审计对象中
	 * 
	 * @param databaseAddress
	 */
	public void saveMir2DatabaseAddresss(int dbId, int mirdbAddressId, String instanceName) {
		// 加入审计日志
		auditLog.setEventDesc("添加审计对象：镜像数据加入到已存在同类型审计对象中");
		auditLog.setDesigner("配置->基本配置");
		auditLog.setMovement("镜像数据加入到已存在同类型审计对象中");
		try {
			DatabaseMirInfo dbDatabaseMirInfo = getMirDatabaseById(mirdbAddressId);
			DatabaseAddress dbAddress = new DatabaseAddress();
			dbAddress.setAddress(dbDatabaseMirInfo.getAddress());
			dbAddress.setPort(dbDatabaseMirInfo.getPort());
			dbAddress.setServiceName(StringUtils.isEmpty(dbDatabaseMirInfo.getServiceName()) ? "" : dbDatabaseMirInfo
					.getServiceName());
			dbAddress.setDatabaseId(dbId);
			databaseConfigDAO.saveMir2DatabaseAddresss(dbAddress, mirdbAddressId, instanceName);
			auditLog.setEventResult("1");
			auditLog.setAlterBefore("添加成功");
		} catch (Exception e) {
			auditLog.setEventResult("2");
			auditLog.setAlterBefore("添加失败;原因：" + e.getLocalizedMessage());
			throw new ServiceException("添加失败 " + e.getLocalizedMessage());
		} finally {
			SystemAuditResultSingelton.getInstance().insertAuditLog(auditLog);
		}
	}

	/**
	 * 返回指定ID的数据库对象
	 * 
	 * @param dbId
	 * @return
	 */
	public DatabaseInfo getDatabaseByID(int dbId) {
		return databaseConfigDAO.getDatabaseInfoByID(dbId);
	}

	/**
	 * 返回数据库总数
	 * 
	 * @return
	 */
	public int getDatabasesCount() {
		return databaseConfigDAO.getDatabasesCount();
	}

	/**
	 * 获取指定数据库的所有地址
	 * 
	 * @param dbId
	 * @return
	 */
	public List<DatabaseAddress> getDatabaseAddresses(int dbId) {
		return databaseConfigDAO.getDatabaseAddresses(dbId);
	}

	/**
	 * 改变数据库的地址
	 * 
	 * @param dbId
	 * @param addressId
	 */
	public void updateSessionDbUserInfo(int addressID, String username, String userpwd) {
		databaseConfigDAO.updateSessionDbUserInfo(addressID, username, userpwd);
		// NativeExecutor.execute(commands);
	}

	public void removeLogTable(String tablename) {
		databaseConfigDAO.removeLogTable(tablename);
		// NativeExecutor.execute(commands);
	}

	/**
	 * 改变数据库的地址
	 * 
	 * @param dbId
	 * @param addressId
	 */
	public void setDatabaseAddresses(List<DatabaseAddress> addresses) {
		List<NativeCommand> commands = new ArrayList<NativeCommand>();
		// 加入审计日志
		auditLog.setEventDesc("基本配置：移除数据库地址");
		auditLog.setDesigner("配置->基本配置");
		auditLog.setMovement("移除数据库地址");
		StringBuffer stringBuffer = new StringBuffer("移除数据库地址：[");
		try {
			for (DatabaseAddress address : addresses) {
				switch (address.getOp()) {
				// 修改业务逻辑后，不走此分支
				// case DBFWConstant.OP_ADDED:
				// databaseConfigDAO.addAddress(address.getAddress(),
				// address.getPort(), address.getServiceName(),
				// address.getDatabaseId());
				// break;
				case DBFWConstant.OP_DELETED:
					stringBuffer.append(address.getAddress() + ":" + address.getPort() + ",");
					databaseConfigDAO.removeAddress(address.getId(), commands);
					break;
				default:
					break;
				}
			}
			auditLog.setEventResult("1");
			stringBuffer.deleteCharAt(stringBuffer.length() - 1);
			auditLog.setAlterBefore("移除数据库地址成功!" + stringBuffer.toString());
		} catch (Exception e) {
			auditLog.setEventResult("2");
			auditLog.setAlterBefore("移除数据库地址失败;原因：" + e.getLocalizedMessage());
			throw new ServiceException("移除数据库地址失败 " + e.getLocalizedMessage());
		} finally {
			SystemAuditResultSingelton.getInstance().insertAuditLog(auditLog);
		}

		// NativeExecutor.execute(commands);
	}

	/**
	 * 获取指定数据库地址的安全实例保护信息
	 * 
	 * @param addressId
	 * @return
	 */
	public List<DBFWForDbAddr> getDBFWForDbAddrByAddress(int addressId) {
		return databaseConfigDAO.getDBFWForDbAddrByAddress(addressId);
	}

	/**
	 * 设置数据库管理的安全实例地址的监听信息
	 * 
	 * @param list
	 */
	public void addDbfwForDbAddrs(List<DBFWForDbAddr> list) {
		String result = "";
		try {
			result = "1";
			List<NativeCommand> commands = new ArrayList<NativeCommand>();
			databaseConfigDAO.addDbfwForDbAddrs(list, commands);
			// NativeExecutor.execute(commands);
		} catch (Exception e) {
			result = "2";
			e.printStackTrace();
			if (e.getMessage() != null && e.getMessage().indexOf("引擎未启动") > 0) {
				throw new ServiceException("添加引擎保护 成功 " + e.getMessage());
			} else {
				throw new ServiceException("添加引擎保护 失败 " + e.getMessage());
			}
		} finally {
			for (DBFWForDbAddr dbf : list) {
				SystemAuditResultSingelton.getInstance().insertSystemAudit(
						7,
						2,
						2,
						2,
						result,
						"数据库：" + databaseConfigDAO.getDatabaseNameByaddressId(dbf.getAddressId()) + " 添加引擎保护："
								+ databaseConfigDAO.getDBFWName(dbf.getDbfwId()) + " 部署模式："
								+ getMonitorName(dbf.getMonitorType()));
			}
		}
	}

	/**
	 * 设置安全实例管理的数据库地址的监听信息
	 * 
	 * @param list
	 */
	public void addDbfwForDbAddrs1(List<DBFWForDbAddr> list) {
		String result = "";
		try {
			result = "1";
			List<NativeCommand> commands = new ArrayList<NativeCommand>();
			databaseConfigDAO.addDbfwForDbAddrs(list, commands);
			// NativeExecutor.execute(commands);
		} catch (Exception e) {
			result = "2";
			e.printStackTrace();
			if (e.getMessage() != null && e.getMessage().startsWith("引擎未启动")) {
				throw new ServiceException("添加受保护数据库成功 " + e.getMessage());
			} else {
				throw new ServiceException("添加受保护数据库失败 " + e.getMessage());
			}
		} finally {
			for (DBFWForDbAddr dbf : list) {
				SystemAuditResultSingelton.getInstance().insertSystemAudit(
						7,
						3,
						2,
						3,
						result,
						"引擎：" + databaseConfigDAO.getDBFWName(dbf.getDbfwId()) + "添加受保护数据库："
								+ databaseConfigDAO.getDatabaseNameByaddressId(dbf.getAddressId()) + " 部署模式："
								+ getMonitorName(dbf.getMonitorType()));
			}
		}
	}

	/**
	 * 移除数据库地址的监听信息
	 * 
	 * @param dbfwId
	 * @param addressId
	 */
	public void removeDbfwForDbAddr(int dbfwId, int addressId) {
		List<NativeCommand> commands = new ArrayList<NativeCommand>();
		databaseConfigDAO.removeDbfwForDbAddr(dbfwId, addressId, commands);
		// NativeExecutor.execute(commands);
	}

	/**
	 * 更新数据库地址的监听信息
	 * 
	 * @param dbfwForDbAddr
	 */
	public void updateDbfwForDbAddr(List<DBFWForDbAddr> dbfwForDbAddrList) {
		String result = "";
		try {
			result = "1";
			List<NativeCommand> commands = new ArrayList<NativeCommand>();
			databaseConfigDAO.updateDbfwForDbAddr(dbfwForDbAddrList, commands);
			// NativeExecutor.execute(commands);
		} catch (Exception e) {
			result = "2";
			e.printStackTrace();
			throw new ServiceException(e.getMessage());
		} finally {
			for (DBFWForDbAddr dbf : dbfwForDbAddrList) {
				SystemAuditResultSingelton.getInstance().insertSystemAudit(
						7,
						2,
						2,
						7,
						result,
						"数据库：" + databaseConfigDAO.getDatabaseNameByaddressId(dbf.getAddressId()) + " 更新引擎保护："
								+ databaseConfigDAO.getDBFWName(dbf.getDbfwId()) + " 部署模式："
								+ getMonitorName(dbf.getMonitorType()));
			}
		}
	}

	/**
	 * 更新安全实例管理的数据库地址的监听信息
	 * 
	 * @param dbfwForDbAddrList
	 */
	public void updateDbfwForDbAddr1(List<DBFWForDbAddr> dbfwForDbAddrList) {
		String result = "";
		try {
			result = "1";
			List<NativeCommand> commands = new ArrayList<NativeCommand>();
			databaseConfigDAO.updateDbfwForDbAddr(dbfwForDbAddrList, commands);
			// NativeExecutor.execute(commands);
		} catch (Exception e) {
			result = "2";
			e.printStackTrace();
			if (e.getMessage() != null && e.getMessage().startsWith("引擎未启动")) {
				throw new ServiceException("更新受保护数据库 成功 " + e.getMessage());
			} else {
				throw new ServiceException("更新受保护数据库 失败 " + e.getMessage());
			}
		} finally {
			for (DBFWForDbAddr dbf : dbfwForDbAddrList) {
				SystemAuditResultSingelton.getInstance().insertSystemAudit(
						7,
						3,
						2,
						4,
						result,
						"引擎：" + databaseConfigDAO.getDBFWName(dbf.getDbfwId()) + "更新受保护数据库："
								+ databaseConfigDAO.getDatabaseNameByaddressId(dbf.getAddressId()) + " 部署模式："
								+ getMonitorName(dbf.getMonitorType()));
			}
		}
	}

	/**
	 * 更新数据库地址的监听状态
	 * 
	 * @param dbfwId
	 * @param dbId
	 * @param status
	 */
	public void updateDbfwForDbAddrStatus(int dbfwId, int dbId, int status) {

		String dbfwName = databaseConfigDAO.getDBFWName(dbfwId);
		String dbName = databaseConfigDAO.getDatabaseName(dbId);
		String result = "";
		String message = "";
		if (status == 0) {
			message = "挂起";
		} else if (status == 1) {
			message = "恢复";
		}
		try {
			result = "1";
			List<NativeCommand> commands = new ArrayList<NativeCommand>();
			databaseConfigDAO.updateDbfwForDbAddrStatus(dbfwId, dbId, status, commands);
			// NativeExecutor.execute(commands);
		} catch (Exception e) {
			result = "2";
			e.printStackTrace();
			if (e.getMessage() != null && e.getMessage().startsWith("引擎未启动")) {
				throw new ServiceException("数据库：" + dbName + ".引擎：" + dbfwName + message + " 成功 " + e.getMessage());
			} else {
				throw new ServiceException("数据库：" + dbName + ".引擎：" + dbfwName + message + " 失败 " + e.getMessage());
			}
		} finally {
			SystemAuditResultSingelton.getInstance().insertSystemAudit(7, 2, 2, 8, result,
					"数据库：" + dbName + ".引擎：" + dbfwName + " " + message);
		}
	}

	/**
	 * 获取数据库用户信息
	 * 
	 * @param dbId
	 * @return
	 */
	public List<UserInfo> getUserInfoByDb(int dbId, int type) {
		return databaseConfigDAO.getUserInfoByDb(dbId, type);
	}

	/**
	 * 添加一个受保护数据库
	 * 
	 * @param dbInfo
	 * @param addresses
	 */
	public synchronized List<DatabaseAddress> addProtectedDatabase(DatabaseInfo dbInfo,
			List<DatabaseAddress> addresses, List<NoSession> noSessionList) {
		String result = "";
		List<DatabaseAddress> list = null;
		try {
			result = "1";
			list = this.databaseConfigDAO.addDatabase(dbInfo, addresses, noSessionList);
		} catch (Exception e) {
			result = "2";
			e.printStackTrace();
			if (e.getMessage() != null && e.getMessage().startsWith("引擎未启动")) {
				throw new ServiceException("保存数据库：" + dbInfo.getName() + " 成功 " + e.getMessage());
			} else {
				throw new ServiceException("保存数据库：" + dbInfo.getName() + " 失败 " + e.getMessage());
			}
		} finally {
			SystemAuditResultSingelton.getInstance().insertSystemAudit(7, 2, 1, 1, result, "保存数据库：" + dbInfo.getName());
		}
		return list;
	}

	public List<DBAudit> getDBAuditDataFromDB(int dbtype, int dbId) {
		try {
			if (dbtype == 1)// oracle
			{
				return this.databaseConfigDAO.getAuditDataFromOracle(dbId);
			}
			if (dbtype == 2)// ms sql server
			{
				return this.databaseConfigDAO.getAuditDataFromMSSql(dbId);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServiceException("获取数据库审计信息失败 " + e.getMessage());
		} finally {
			SystemAuditResultSingelton.getInstance().insertSystemAudit(7, 2, 1, 1, "", "获取审计完成");
		}
		return null;
	}

	/*
	 * 从数据库中直接获取审计数据
	 */
	public List<DBConfigDetail> getDBConfigFromDB(int dbtype, int dbId) {
		try {
			if (dbtype == 1)// oracle
			{
				return this.databaseConfigDAO.getDBConfigFromOracle(dbId);
			}
			if (dbtype == 2)// ms sql server
			{
				return this.databaseConfigDAO.getDBConfigFromMSSql(dbId);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServiceException("获取数据库审计信息失败 " + e.getMessage());
		} finally {
			SystemAuditResultSingelton.getInstance().insertSystemAudit(7, 2, 1, 1, "", "获取审计完成");
		}
		return null;
	}

	/**
	 * 获取数据库版本
	 * 
	 * @param dbType
	 *            数据库类型，1为Oracle，其它类型见DBFWConstant.DBTYPE_XXX
	 * @param host
	 *            数据库主机IP
	 * @param port
	 *            数据库主机端口
	 * @param dbName
	 *            数据库名称（当前仅MSSQL数据库使用）
	 * @param sid
	 *            数据库实例名
	 * @param userName
	 *            用户名
	 * @param password
	 *            密码
	 * @return 数据库版本号字符串
	 */
	public int getDatabaseVersion(int dbType, String host, int port, String dbName, String sid, String userName,
			String password) {
		// 用户名密码 解密
		userName = new DesEncrypt().strDec(userName, "20160101", "1", "1");
		password = new DesEncrypt().strDec(password, "20160101", "1", "1");
		switch (dbType) {
		case DBFWConstant.DBTYPE_ORACLE:
			return databaseConfigDAO.getOracleVersion(host, port, sid, userName, password);
		case DBFWConstant.DBTYPE_MSSQL:
			return databaseConfigDAO.getMssqlVersion(host, port, userName, password);
		case DBFWConstant.DBTYPE_MYSQL:
			return databaseConfigDAO.getMysqlVersion(host, port, userName, password);
		case DBFWConstant.DBTYPE_DB2:
			return databaseConfigDAO.getDB2Version(host, port, sid, userName, password);
		case DBFWConstant.DBTYPE_DM:
			return databaseConfigDAO.getDMVersion(host, port, sid, userName, password);
		case DBFWConstant.DBTYPE_GBASE:
			return databaseConfigDAO.getGbaseVersion(host, port, userName, password);
		case DBFWConstant.DBTYPE_PGSQL:
			return databaseConfigDAO.getPostgresVersion(host, port, sid, userName, password);
		case DBFWConstant.DBTYPE_KINGBASE:
			return databaseConfigDAO.getKingBaseVersion(host, port, sid, userName, password);
		case DBFWConstant.DBTYPE_INFORMIX:
			return databaseConfigDAO.getInformixVersion(host, port, sid, userName, password);
		case DBFWConstant.DBTYPE_SYBASE:
			return databaseConfigDAO.getSybaseVersion(host, port, sid, userName, password);
		case DBFWConstant.DBTYPE_GBASE8T:
			return databaseConfigDAO.getGbase8tVersion(host, port, sid, userName, password);
		default:
			throw new ServiceException("无效的数据库类型");
		}
	}

	/**
	 * 保存数据库信息
	 * 
	 * @param dbInfo
	 *            数据库基本信息
	 */
	public void setDatabaseInfo(DatabaseInfo dbInfo) {
		String result = "";
		try {
			result = "1";
			this.databaseConfigDAO.updateDatabaseInfo(dbInfo.getDesc(), dbInfo.getVersion(), dbInfo.getId(), dbInfo
					.getIsdpa(), dbInfo.getIssox(), dbInfo.getIspci(), dbInfo.getIsglba());
		} catch (Exception e) {
			result = "2";
			e.printStackTrace();
			throw new ServiceException("更新数据库：" + dbInfo.getName() + " 失败 " + e.getMessage());
		} finally {
			SystemAuditResultSingelton.getInstance().insertSystemAudit(7, 2, 2, 1, result, "更新数据库：" + dbInfo.getName());
		}
	}

	/**
	 * 添加数据库地址及其DBFW监听信息；当“修改数据库属性”--添加数据库地址时调用
	 * 
	 * @param address
	 *            数据库地址信息
	 * @param dbfwForAddrList
	 *            数据库地址监听信息
	 */
	public void addDatabaseAddress(DatabaseAddress address, List<DBFWForDbAddr> dbfwForAddrList) {
		List<NativeCommand> commands = new ArrayList<NativeCommand>();
		this.databaseConfigDAO.addDatabaseAddress(address, dbfwForAddrList, commands);
		// NativeExecutor.execute(commands);
	}

	/**
	 * 获取某一安全实例对数据库地址的监听信息
	 * 
	 * @param dbfwId
	 *            安全实例ID
	 * @param dbId
	 *            数据库ID
	 * @return 安全实例监听数据库地址信息列表
	 */
	public List<DBFWForDbAddr> getDbfwForDbAddrs(int dbfwId, int dbId) {
		return this.databaseConfigDAO.getDbfwForDbAddrs(dbfwId, dbId);
	}

	/**
	 * 获取某一数据库类型的所有版本
	 * 
	 * @param dbfwId
	 *            安全实例ID
	 * @return 安全实例监听数据库地址信息列表
	 */
	public List<dbversion> getDBversions() {
		return this.databaseConfigDAO.getDbversions();
	}

	public void removeDbfwForDb(int dbfwId, int dbId) {
		String dbfwName = databaseConfigDAO.getDBFWName(dbfwId);
		String dbName = databaseConfigDAO.getDatabaseName(dbId);
		String result = "";
		try {
			result = "1";
			List<NativeCommand> commands = new ArrayList<NativeCommand>();
			this.databaseConfigDAO.removeDbfwForDb(dbfwId, dbId, commands);
			// NativeExecutor.execute(commands);
		} catch (Exception e) {
			result = "2";
			e.printStackTrace();
			if (e.getMessage() != null && e.getMessage().startsWith("引擎未启动")) {
				throw new ServiceException("数据库：" + dbName + "移除引擎：" + dbfwName + " 成功 " + e.getMessage());
			} else {
				throw new ServiceException("数据库：" + dbName + "移除引擎：" + dbfwName + " 失败 " + e.getMessage());
			}
		} finally {
			SystemAuditResultSingelton.getInstance().insertSystemAudit(7, 2, 2, 9, result,
					"数据库：" + dbName + "移除引擎：" + dbfwName);
		}
	}

	/**
	 * 从安全实例移除数据库
	 * 
	 * @param dbfwId
	 * @param dbId
	 */
	public void removeDbfwForDb1(int dbfwId, int dbId) {
		String dbfwName = databaseConfigDAO.getDBFWName(dbfwId);
		String dbName = databaseConfigDAO.getDatabaseName(dbId);
		String result = "";
		try {
			result = "1";
			List<NativeCommand> commands = new ArrayList<NativeCommand>();
			this.databaseConfigDAO.removeDbfwForDb(dbfwId, dbId, commands);
			// NativeExecutor.execute(commands);
		} catch (Exception e) {
			result = "2";
			e.printStackTrace();
			if (e.getMessage() != null && e.getMessage().startsWith("引擎未启动")) {
				throw new ServiceException("引擎：" + dbfwName + "移除数据库：" + dbName + " 成功  " + e.getMessage());
			} else {
				throw new ServiceException("引擎：" + dbfwName + "移除数据库：" + dbName + " 失败  " + e.getMessage());
			}
		} finally {
			SystemAuditResultSingelton.getInstance().insertSystemAudit(7, 3, 2, 7, result,
					"引擎：" + dbfwName + "移除数据库：" + dbName);
		}
	}

	public void removeDatabase(int dbId) {
		String databaseName = databaseConfigDAO.getDatabaseName(dbId);
		// 加入审计日志
		auditLog.setEventDesc("基本配置：删除审计对象");
		auditLog.setDesigner("配置->基本配置");
		auditLog.setMovement("删除审计对象");
		try {
			List<NativeCommand> commands = new ArrayList<NativeCommand>();
			this.databaseConfigDAO.removeDatabase(dbId, commands);
			// NativeExecutor.execute(commands);
			auditLog.setEventResult("1");
			auditLog.setAlterBefore("删除审计对象：" + databaseName + " 成功 ");
		} catch (Exception e) {
			if (DatabaseConfigDAO.getDAO().getCurrentDbfwState() != 1) {
				auditLog.setEventResult("1");
				auditLog.setAlterBefore("删除审计对象：" + databaseName + " 成功 ，引擎未启动，启动引擎后操作生效！");
				throw new ServiceException("删除审计对象：" + databaseName + " 成功，引擎未启动，启动引擎后操作生效！");
			} else {
				auditLog.setEventResult("2");
				auditLog.setAlterBefore("删除审计对象：" + databaseName + " 失败 " + e.getLocalizedMessage());
				throw new ServiceException("删除审计对象：" + databaseName + " 失败 " + e.getLocalizedMessage());
			}
		} finally {
			SystemAuditResultSingelton.getInstance().insertAuditLog(auditLog);
		}
	}

	public String getRelatedDbfwNames(int dbId) {
		return this.databaseConfigDAO.getRelatedDbfwNames(dbId);
	}

	public static void main(String[] args) throws ClassNotFoundException, SQLException {
		DatabaseConfigService service = new DatabaseConfigService();
		/*
		 * // getAllDatabases System.out.println(service.getAllDatabases());
		 * 
		 * // getDatabaseAddresses
		 * System.out.println(service.getDatabaseAddresses(1));
		 * 
		 * // getDBFWForDbAddrByAddress
		 * System.out.println(service.getDBFWForDbAddrByAddress(1));
		 */
		// addDbfwForDbAddrs
		/*
		 * List<DBFWForDbAddr> list = new ArrayList<DBFWForDbAddr>();
		 * DBFWForDbAddr element = new DBFWForDbAddr(); element.setAddressId(1);
		 * element.setDbfwId(1);
		 * element.setMonitorType(DBFWConstant.DEPLOY_NATIVEPROXY);
		 * element.setPortId(9); list.add(element);
		 * 
		 * element = new DBFWForDbAddr(); element.setAddressId(2);
		 * element.setDbfwId(1);
		 * element.setMonitorType(DBFWConstant.DEPLOY_REMOTEPROXY);
		 * element.setPortId(10); element.setRemoteHost("192.168.0.2");
		 * element.setRemotePort(9200); list.add(element);
		 * 
		 * element = new DBFWForDbAddr(); element.setAddressId(3);
		 * element.setDbfwId(1);
		 * element.setMonitorType(DBFWConstant.DEPLOY_HALFTRANS);
		 * element.setPortId(8); list.add(element);
		 * 
		 * element = new DBFWForDbAddr(); element.setAddressId(4);
		 * element.setDbfwId(1);
		 * element.setMonitorType(DBFWConstant.DEPLOY_BYWAY);
		 * element.setGroupId(16); list.add(element);
		 * 
		 * service.addDbfwForDbAddrs(list);
		 */
		// updateDbfwForDbAddr
		// DBFWForDbAddr element = service.getDBFWForDbAddrByAddress(1).get(0);
		// element.setMonitorType(DBFWConstant.DEPLOY_REMOTEPROXY);
		// element.setRemoteHost("192.168.0.2");
		// element.setRemotePort(9201);
		// List<DBFWForDbAddr> list = new ArrayList<DBFWForDbAddr>();
		// list.add(element);
		// service.updateDbfwForDbAddr(list);

		// removeDbfwForDbAddr
		/*
		 * service.removeDbfwForDbAddr(1, 1); service.removeDbfwForDbAddr(1, 2);
		 * service.removeDbfwForDbAddr(1, 3); service.removeDbfwForDbAddr(1, 4);
		 */

		// getDbfwForDbAddrs
		// System.out.println(service.getDbfwForDbAddrs(1, 1));
		/*
		 * // addProtectedDatabase DatabaseInfo dbInfo = new DatabaseInfo();
		 * dbInfo.setName("jftest"); dbInfo.setType(DBFWConstant.DBTYPE_ORACLE);
		 * dbInfo.setDesc("test instance."); dbInfo.setVersion(10020001);
		 * 
		 * List<DatabaseAddress> addressList = new ArrayList<DatabaseAddress>();
		 * DatabaseAddress address = new DatabaseAddress();
		 * address.setAddress("192.168.1.15"); address.setPort(1522);
		 * address.setServiceName("JFORCL"); addressList.add(address);
		 * 
		 * address = new DatabaseAddress(); address.setAddress("192.168.1.16");
		 * address.setPort(1522); address.setServiceName("JFORCL");
		 * addressList.add(address);
		 * 
		 * System.out.println(service.addProtectedDatabase(dbInfo,
		 * addressList));
		 */
		// removeDbfwForDb
		// service.removeDbfwForDb(1, 3);

		// removeDatabase
		// service.removeDatabase(1);

		// getDatabaseVersion
		int version = service.getDatabaseVersion(1, "192.168.1.17", 1521, "DOMODB", "DOMODB", "test", "test");
		System.out.println(version);
	}

	/**
	 * 根据类型type返回名字
	 * 
	 * @param type
	 * @return
	 */
	public String getMonitorName(int type) {
		String name = "";
		if (type == 1) {
			name = "远程代理";
		} else if (type == 2) {
			name = "旁路监听";
		} else if (type == 3) {
			name = "本地代理";
		} else if (type == 4) {
			name = "网桥（半透明）";
		}
		return name;
	}

	/**
	 * 获取configures.properties配置中支持的数据库
	 * 
	 * @return
	 */
	public List<DatabaseInfo> getSupportDatabaseConfigure() {
		return this.databaseConfigDAO.getSupportDatabaseConfigure();
	}

	public List<DBFWForDbAddr> getDbfwForDbAddrsByType(int dbfwId, int dbId, int monitorType) {
		List<DBFWForDbAddr> list = this.databaseConfigDAO.getDbfwForDbAddrsByType(dbfwId, dbId, monitorType);
		return list;
	}

	public List<NoSession> getNoSessionList(int dbId) {
		List<NoSession> list = this.databaseConfigDAO.getNoSessionList(dbId);
		return list;
	}

	public boolean removeNoSession(int id) {
		return this.databaseConfigDAO.removeNoSession(id);
	}

	public boolean insertNoConnectOne(int dbId, NoSession noSession) {
		return this.databaseConfigDAO.insertNoConnectOne(dbId, noSession);
	}

	public boolean judgeNoConnect(int dbId, NoSession noSession) {
		return databaseConfigDAO.judgeNoConnect(dbId, noSession);
	}

	/**
	 * 输出当前tomcat连接数 查看连接池耗用情况
	 */
	public void getTomcatConnect() {
		databaseConfigDAO.getTomcatConnect();
	}

	/**
	 * 获取所有数据库信息
	 */
	public List<DatabaseInfo> getAllDbInfo() {
		return databaseConfigDAO.getAllDbInfo();
	}
}
