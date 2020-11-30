package cn.schina.dbfw.service.strategy;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import cn.schina.dbfw.base.tools.DesEncrypt;
import cn.schina.dbfw.common.lang.DBFWConstant;
import cn.schina.dbfw.common.lang.ServiceException;
import cn.schina.dbfw.dao.strategy.DatabaseConfigDAO;
import cn.schina.dbfw.pojo.DBAudit;
import cn.schina.dbfw.pojo.DBConfigDetail;
import cn.schina.dbfw.pojo.strategy.DBFWForDbAddr;
import cn.schina.dbfw.pojo.strategy.DatabaseAddress;
import cn.schina.dbfw.pojo.strategy.DatabaseInfo;
import cn.schina.dbfw.pojo.strategy.NoSession;
import cn.schina.dbfw.pojo.strategy.UserInfo;
import cn.schina.dbfw.pojo.strategy.dbversion;
import cn.schina.dbfw.service.strategy.command.NativeCommand;
import cn.schina.dbfw.util.SystemAuditResultSingelton;

public class DatabaseConfigService {

	private DatabaseConfigDAO databaseConfigDAO = DatabaseConfigDAO.getDAO();

	/**
	 * 获取系统中所有注册的受保护数据库
	 * 
	 * @return
	 */
	public List<DatabaseInfo> getAllDatabases() {
		return databaseConfigDAO.getAllDatabases();
	}

	public DatabaseInfo getDatabasesByID(int dbId) {
		return databaseConfigDAO.getDatabasesByID(dbId);
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
		username = DesEncrypt.getDesEncrypt().strDec(username, "encrypt", "1", "1");
		userpwd = DesEncrypt.getDesEncrypt().strDec(userpwd, "encrypt", "1", "1");
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
		// 判断npc是否有引用
		if (databaseConfigDAO.judgeNpcByAdress(addresses) > 0) {
			throw new ServiceException("数据库地址存在着引用，不允许移除！");
		}
		List<NativeCommand> commands = new ArrayList<NativeCommand>();
		for (DatabaseAddress address : addresses) {
			switch (address.getOp()) {
			// 修改业务逻辑后，不走此分支
			// case DBFWConstant.OP_ADDED:
			// databaseConfigDAO.addAddress(address.getAddress(),
			// address.getPort(), address.getServiceName(),
			// address.getDatabaseId());
			// break;
			case DBFWConstant.OP_DELETED:
				databaseConfigDAO.removeAddress(address.getId(), commands);
				break;
			default:
				break;
			}
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
			if (e.getMessage() != null && e.getMessage().startsWith("安全实例未启动")) {
				throw new ServiceException("添加安全实例保护 成功 " + e.getMessage());
			} else {
				throw new ServiceException("添加安全实例保护 失败 " + e.getMessage());
			}
		} finally {
			for (DBFWForDbAddr dbf : list) {
				SystemAuditResultSingelton.getInstance().insertSystemAudit(
						7,
						2,
						2,
						2,
						result,
						"数据库：" + databaseConfigDAO.getDatabaseNameByaddressId(dbf.getAddressId()) + " 添加安全实例保护："
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
			if (e.getMessage() != null && e.getMessage().startsWith("安全实例未启动")) {
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
						"安全实例：" + databaseConfigDAO.getDBFWName(dbf.getDbfwId()) + "添加受保护数据库："
								+ databaseConfigDAO.getDatabaseNameByaddressId(dbf.getAddressId()) + " 部署模式："
								+ getMonitorName(dbf.getMonitorType()));
			}
		}
	}

	/**
	 * 自动化测试
	 * 
	 * @param list
	 */
	public void addDbfwForDbAddrsForAutoTest(List<DBFWForDbAddr> list) {
		try {
			List<NativeCommand> commands = new ArrayList<NativeCommand>();
			databaseConfigDAO.addDbfwForDbAddrs(list, commands);
			// NativeExecutor.execute(commands);
		} catch (Exception e) {
			e.printStackTrace();
			if (e.getMessage() != null && e.getMessage().startsWith("安全实例未启动")) {
				throw new ServiceException("添加受保护数据库成功 " + e.getMessage());
			} else {
				throw new ServiceException("添加受保护数据库失败 " + e.getMessage());
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
						"数据库：" + databaseConfigDAO.getDatabaseNameByaddressId(dbf.getAddressId()) + " 更新安全实例保护："
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
			if (e.getMessage() != null && e.getMessage().startsWith("安全实例未启动")) {
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
						"安全实例：" + databaseConfigDAO.getDBFWName(dbf.getDbfwId()) + "更新受保护数据库："
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
			if (e.getMessage() != null && e.getMessage().startsWith("安全实例未启动")) {
				throw new ServiceException("数据库：" + dbName + ".安全实例：" + dbfwName + message + " 成功 " + e.getMessage());
			} else {
				throw new ServiceException("数据库：" + dbName + ".安全实例：" + dbfwName + message + " 失败 " + e.getMessage());
			}
		} finally {
			SystemAuditResultSingelton.getInstance().insertSystemAudit(7, 2, 2, 8, result,
					"数据库：" + dbName + ".安全实例：" + dbfwName + " " + message);
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
		if (!databaseConfigDAO.canAddDatabaseIp(addresses.size())) {
			throw new ServiceException("数据库IP端口达到上限！");
		}
		String result = "";
		List<DatabaseAddress> list = null;
		try {
			result = "1";
			list = this.databaseConfigDAO.addDatabase(dbInfo, addresses, noSessionList);
		} catch (Exception e) {
			result = "2";
			e.printStackTrace();
			if (e.getMessage() != null && e.getMessage().startsWith("安全实例未启动")) {
				throw new ServiceException("保存数据库：" + dbInfo.getName() + " 成功 " + e.getMessage());
			} else {
				throw new ServiceException("保存数据库：" + dbInfo.getName() + " 失败 " + e.getMessage());
			}
		} finally {
			SystemAuditResultSingelton.getInstance().insertSystemAudit(7, 2, 1, 1, result, "保存数据库：" + dbInfo.getName());
		}
		return list;
	}

	/**
	 * 自动化测试
	 * 
	 * @param dbInfo
	 * @param addresses
	 * @param noSessionList
	 * @return
	 */
	public synchronized void addProtectedDatabaseForautoTest(DatabaseInfo dbInfo, List<DatabaseAddress> addresses,
			List<NoSession> noSessionList) {
		if (!databaseConfigDAO.canAddDatabaseIp(addresses.size())) {
			throw new ServiceException("数据库IP端口达到上限！");
		}
		try {
			this.databaseConfigDAO.addDatabase(dbInfo, addresses, noSessionList);
		} catch (Exception e) {
			e.printStackTrace();
			if (e.getMessage() != null && e.getMessage().startsWith("安全实例未启动")) {
				throw new ServiceException("保存数据库：" + dbInfo.getName() + " 成功 " + e.getMessage());
			} else {
				throw new ServiceException("保存数据库：" + dbInfo.getName() + " 失败 " + e.getMessage());
			}
		}
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
		userName = DesEncrypt.getDesEncrypt().strDec(userName, "encrypt", "1", "1");
		password = DesEncrypt.getDesEncrypt().strDec(password, "encrypt", "1", "1");
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
		case DBFWConstant.DBTYPE_GBASE8T:
			return databaseConfigDAO.getGbase8tVersion(host, port, sid, userName, password);
		case DBFWConstant.DBTYPE_OSCAR:
			return databaseConfigDAO.getOscarVersion(host, port, sid, userName, password);
		case DBFWConstant.DBTYPE_SYBASE:
			return databaseConfigDAO.getSybaseVersion(host, port, sid, userName, password);
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
		if (!databaseConfigDAO.canAddDatabaseIp(1)) {
			throw new ServiceException("数据库IP端口达到上限！");
		}
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
			if (e.getMessage() != null && e.getMessage().startsWith("安全实例未启动")) {
				throw new ServiceException("数据库：" + dbName + "移除安全实例：" + dbfwName + " 成功 " + e.getMessage());
			} else {
				throw new ServiceException("数据库：" + dbName + "移除安全实例：" + dbfwName + " 失败 " + e.getMessage());
			}
		} finally {
			SystemAuditResultSingelton.getInstance().insertSystemAudit(7, 2, 2, 9, result,
					"数据库：" + dbName + "移除安全实例：" + dbfwName);
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
			if (e.getMessage() != null && e.getMessage().startsWith("安全实例未启动")) {
				throw new ServiceException("安全实例：" + dbfwName + "移除数据库：" + dbName + " 成功  " + e.getMessage());
			} else {
				throw new ServiceException("安全实例：" + dbfwName + "移除数据库：" + dbName + " 失败  " + e.getMessage());
			}
		} finally {
			SystemAuditResultSingelton.getInstance().insertSystemAudit(7, 3, 2, 7, result,
					"安全实例：" + dbfwName + "移除数据库：" + dbName);
		}
	}

	public void removeDatabase(int dbId) {
		String databaseName = databaseConfigDAO.getDatabaseName(dbId);
		String result = "";
		try {
			result = "1";
			List<NativeCommand> commands = new ArrayList<NativeCommand>();
			this.databaseConfigDAO.removeDatabase(dbId, commands);
			// NativeExecutor.execute(commands);
		} catch (Exception e) {
			result = "2";
			e.printStackTrace();
			if (e.getMessage() != null && e.getMessage().startsWith("安全实例未启动")) {
				throw new ServiceException("移除数据库：" + databaseName + " 成功 " + e.getMessage());
			} else {
				throw new ServiceException("移除数据库：" + databaseName + " 失败 " + e.getMessage());
			}
		} finally {
			SystemAuditResultSingelton.getInstance().insertSystemAudit(7, 2, 2, 10, result, "移除数据库：" + databaseName);
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
			name = "网桥";
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
}
