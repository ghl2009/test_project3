package cn.schina.dbfw.dao.strategy;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.schina.dbfw.common.lang.DAOException;
import cn.schina.dbfw.common.lang.DBFWConstant;
import cn.schina.dbfw.common.lang.RunException;
import cn.schina.dbfw.common.lang.ServiceException;
import cn.schina.dbfw.config.Configures;
import cn.schina.dbfw.config.Globals;
import cn.schina.dbfw.dao.BaseDAO;
import cn.schina.dbfw.db.QueryCallback;
import cn.schina.dbfw.pojo.DBAudit;
import cn.schina.dbfw.pojo.DBConfigDetail;
import cn.schina.dbfw.pojo.LicenseInfo;
import cn.schina.dbfw.pojo.NpcInfos;
import cn.schina.dbfw.pojo.strategy.DBConfigDetailDC;
import cn.schina.dbfw.pojo.strategy.DBFWForDbAddr;
import cn.schina.dbfw.pojo.strategy.DBFWInfo;
import cn.schina.dbfw.pojo.strategy.DatabaseAddress;
import cn.schina.dbfw.pojo.strategy.DatabaseInfo;
import cn.schina.dbfw.pojo.strategy.GroupPort;
import cn.schina.dbfw.pojo.strategy.InterfaceGroup;
import cn.schina.dbfw.pojo.strategy.InterfaceInfo;
import cn.schina.dbfw.pojo.strategy.NoSession;
import cn.schina.dbfw.pojo.strategy.NpcInfo;
import cn.schina.dbfw.pojo.strategy.UserInfo;
import cn.schina.dbfw.pojo.strategy.dbversion;
import cn.schina.dbfw.service.SystemLicenseSrever;
import cn.schina.dbfw.service.strategy.command.CreateDBCommand;
import cn.schina.dbfw.service.strategy.command.CreateNewNPlsCommand;
import cn.schina.dbfw.service.strategy.command.DeleteDBCommand;
import cn.schina.dbfw.service.strategy.command.FlushDbfwForDbInfoCommand;
import cn.schina.dbfw.service.strategy.command.FlushNfwCommand;
import cn.schina.dbfw.service.strategy.command.KillNplsCommand;
import cn.schina.dbfw.service.strategy.command.NativeCommand;
import cn.schina.dbfw.service.strategy.command.NativeExecutor;
import cn.schina.dbfw.util.db.ConnectionFactory;
import cn.schina.dbfw.util.db.DatabaseVersion;

public class DatabaseConfigDAO extends BaseDAO {

	private static final List<DBAudit> ResultSet = null;
	private static DatabaseConfigDAO databaseConfigDAO = null;

	private DatabaseConfigDAO() {
	}

	public static synchronized DatabaseConfigDAO getDAO() {
		if (databaseConfigDAO == null) {
			databaseConfigDAO = new DatabaseConfigDAO();
		}

		return databaseConfigDAO;
	}

	@SuppressWarnings("unchecked")
	public List<DatabaseInfo> getAllDatabases() {
		// String sql =
		// "SELECT t1.database_id AS id, t1.name AS name, t1.description AS `desc`, IFNULL(t4.state,1) AS state, IFNULL(t1.db_type,0) AS type, t1.db_version AS version "
		// +
		/*
		 * String sql =
		 * "SELECT t1.database_id AS id, t1.name AS name, t1.description AS `desc`, IFNULL(t1.state,1) AS state, IFNULL(t1.db_type,0) AS type, t1.db_version AS version "
		 * + "FROM xsec_databases t1 LEFT JOIN " + "(" +
		 * "SELECT MAX(t2.state) AS state, t3.database_id AS database_id " +
		 * "FROM dbfw.dbfw_fordb t2, dbfw.database_addresses t3 " +
		 * "WHERE t2.address_id=t3.address_id AND t2.isdelete='0' " +
		 * "GROUP BY t3.database_id " + ") t4 ON t1.database_id=t4.database_id "
		 * + "ORDER BY id";
		 */
		String sql = "SELECT t1.database_id AS id, t1.name AS name, t1.description AS `desc`,t1.is_dpa as isdpa,t1.is_sox as issox,t1.is_pci as ispci,t1.is_glba as isglba, IFNULL(MIN(t2.state),2) AS state, "
				+ "		IFNULL(t1.db_type,0) AS type, t1.db_version AS version  "
				+ "FROM xsec_databases t1  "
				+ "LEFT JOIN dbfw.dbfw_fordb t2  "
				+ "			ON t1.database_id=t2.database_id  "
				+ "			AND t2.isdelete='0'  "
				+ "			AND t2.address_id IS NOT NULL "
				+ "GROUP BY t1.database_id "
				+ "ORDER BY t1.database_id";
		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql, null);
		return (List<DatabaseInfo>) this.getJdbcUtils().query(sql, DatabaseInfo.class);
	}

	@SuppressWarnings("unchecked")
	public DatabaseInfo getDatabasesByID(int dbId) {
		String sql = "SELECT t1.database_id AS id, t1.name AS name, t1.description AS `desc`,t1.is_dpa as isdpa,  "
				+ "		IFNULL(t1.db_type,0) AS type, t1.db_version AS version  " + "FROM xsec_databases t1  "
				+ "WHERE t1.database_id=? ";
		Object[] args = { dbId };
		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql, args);
		return (DatabaseInfo) this.getJdbcUtils().query(sql, args, DatabaseInfo.class).get(0);
	}

	@SuppressWarnings("unchecked")
	public List<DatabaseAddress> getDatabaseAddresses(int dbId) {
		String sql = "SELECT address_id AS id, t1.address AS address, t1.port AS port, t1.service_name AS serviceName,"
				+ " t1.database_id AS databaseId,t1.db_username AS userName,t1.db_passwd AS userPwd,t1.dyna_port AS dynaPort "
				+ "FROM dbfw.database_addresses t1 " + "WHERE t1.database_id=? " + "ORDER BY id";
		Object[] args = { dbId };
		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql, args);
		return (List<DatabaseAddress>) this.getJdbcUtils().query(sql, args, DatabaseAddress.class);
	}

	public DatabaseAddress getDatabaseAddressesById(Connection conn, int addressId) {
		String sql = "SELECT address_id AS id, t1.address AS address, t1.port AS port, t1.service_name AS serviceName, t1.database_id AS databaseId,t1.dyna_port AS dynaPort "
				+ "FROM dbfw.database_addresses t1 " + "WHERE t1.address_id=?";
		Object[] args = { addressId };
		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql, args);
		return (DatabaseAddress) this.getJdbcUtils().queryForObject(conn, sql, args, DatabaseAddress.class);
	}

	@SuppressWarnings("unchecked")
	public List<DatabaseAddress> getDatabaseAddresses(Connection conn, int dbId) {
		String sql = "SELECT address_id AS id, t1.address AS address, t1.port AS port, t1.service_name AS serviceName, t1.database_id AS databaseId,t1.dyna_port AS dynaPort "
				+ "FROM dbfw.database_addresses t1 " + "WHERE t1.database_id=? " + "ORDER BY id";
		Object[] args = { dbId };
		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql, args);
		return (List<DatabaseAddress>) this.getJdbcUtils().query(conn, sql, args, DatabaseAddress.class);
	}

	@SuppressWarnings("unchecked")
	public List<DBFWForDbAddr> getDBFWForDbAddrByAddress(int addressId) {
		String sql = "SELECT DISTINCT t1.address_id AS addressId, t1.dbfw_inst_id AS dbfwId, t2. NAME AS dbfwName, t1.state AS state, t1.monitor_type AS monitorType, IFNULL(t3.port_id, 0) AS portId, IFNULL(t3.port_name, '') AS portName, t1.npc_info_id AS npcInfoId, 0 AS npcNum, t1.client_group_id AS clientGroupId, IFNULL(t6. NAME, '') AS clientGroupName, t1.remote_address_id AS remoteId, IFNULL(t7. HOST, '') AS remoteHost, IFNULL(t7. PORT, 0) AS remotePort FROM dbfw.dbfw_fordb t1 INNER JOIN dbfw.dbfw_instances t2 ON t1.dbfw_inst_id = t2.instance_id AND t1.isdelete = '0' AND t2.isdelete = '0' LEFT JOIN dbfw.group_port t3 ON t1.port_id = t3.port_id LEFT JOIN interface_group t4 ON t3.group_id = t4.group_id LEFT JOIN dbfw.client_group_info t6 ON t1.client_group_id = t6.id LEFT JOIN dbfw.remote_proxy t7 ON t1.remote_address_id = t7.id WHERE t1.address_id = ? ORDER BY dbfwId";
		/*
		 * "SELECT DISTINCT t1.address_id AS addressId, t1.dbfw_inst_id AS dbfwId, t2.name AS dbfwName, t1.state AS state, t1.monitor_type AS monitorType, CASE WHEN t1.monitor_type=2 THEN t5.group_id ELSE t3.group_id END AS groupId, CASE WHEN t1.monitor_type=2 THEN t8.group_name ELSE t4.group_name END  AS groupName, IFNULL(t3.port_id,0) AS portId, "
		 * +
		 * "IFNULL(t3.port_name,'') AS portName, t1.npc_info_id AS npcInfoId, IFNULL(t5.npc_id,0) AS npcNum, t1.client_group_id AS clientGroupId, IFNULL(t6.name,'') AS clientGroupName, "
		 * +
		 * "t1.remote_address_id AS remoteId, IFNULL(t7.host,'') AS remoteHost, IFNULL(t7.port,0) AS remotePort "
		 * +
		 * "FROM dbfw.dbfw_fordb t1 INNER JOIN dbfw.dbfw_instances t2 ON t1.dbfw_inst_id=t2.instance_id AND t1.isdelete='0' AND t2.isdelete='0' "
		 * + "LEFT JOIN dbfw.group_port t3 ON t1.port_id=t3.port_id " +
		 * "LEFT JOIN interface_group t4 ON t3.group_id=t4.group_id " +
		 * //"LEFT JOIN dbfw.npc_info t5 ON t1.npc_info_id=t5.id " +
		 * "LEFT JOIN dbfw.npc_info t5 ON t5.dbfw_inst_id = t1.dbfw_inst_id AND t5.address_id = t1.address_id AND t5.database_id = t1.database_id "
		 * + "LEFT JOIN interface_group t8 ON t5.group_id=t8.group_id " +
		 * "LEFT JOIN dbfw.client_group_info t6 ON t1.client_group_id=t6.id " +
		 * "LEFT JOIN dbfw.remote_proxy t7 ON t1.remote_address_id=t7.id " +
		 * "WHERE t1.address_id=? " + "ORDER BY dbfwId";
		 */
		Object[] args = { addressId };
		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql, args);
		return (List<DBFWForDbAddr>) this.getJdbcUtils().query(sql, args, DBFWForDbAddr.class);
	}

	@SuppressWarnings("unchecked")
	private List<DBFWForDbAddr> getDBFWForDbAddrByAddress(Connection conn, int addressId) {
		String sql = "SELECT DISTINCT t1.address_id AS addressId, t1.dbfw_inst_id AS dbfwId, t2.name AS dbfwName, t1.state AS state, t1.monitor_type AS monitorType, CASE WHEN t1.monitor_type=2 THEN t5.group_id ELSE t3.group_id END AS groupId, CASE WHEN t1.monitor_type=2 THEN t8.group_name ELSE t4.group_name END  AS groupName, IFNULL(t3.port_id,0) AS portId, "
				+ "IFNULL(t3.port_name,'') AS portName, t1.npc_info_id AS npcInfoId, IFNULL(t5.npc_id,0) AS npcNum, t1.client_group_id AS clientGroupId, IFNULL(t6.name,'') AS clientGroupName, "
				+ "t1.remote_address_id AS remoteId, IFNULL(t7.host,'') AS remoteHost, IFNULL(t7.port,0) AS remotePort "
				+ "FROM dbfw.dbfw_fordb t1 INNER JOIN dbfw.dbfw_instances t2 ON t1.dbfw_inst_id=t2.instance_id AND t1.isdelete='0' AND t2.isdelete='0' "
				+ "LEFT JOIN dbfw.group_port t3 ON t1.port_id=t3.port_id "
				+ "LEFT JOIN interface_group t4 ON t3.group_id=t4.group_id "
				+
				// "LEFT JOIN dbfw.npc_info t5 ON t1.npc_info_id=t5.id " +
				"LEFT JOIN dbfw.npc_info t5 ON  t5.dbfw_inst_id = t1.dbfw_inst_id AND t5.address_id = t1.address_id AND t5.database_id = t1.database_id "
				+ "LEFT JOIN interface_group t8 ON t5.group_id=t8.group_id "
				+ "LEFT JOIN dbfw.client_group_info t6 ON t1.client_group_id=t6.id "
				+ "LEFT JOIN dbfw.remote_proxy t7 ON t1.remote_address_id=t7.id "
				+ "WHERE t1.address_id=? "
				+ "ORDER BY dbfwId";
		Object[] args = { addressId };
		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql, args);
		return (List<DBFWForDbAddr>) this.getJdbcUtils().query(conn, sql, args, DBFWForDbAddr.class);
	}

	public void addDbfwForDbAddr(int dbfwId, int addressId, int monitorType, int clientGroupId) {
		String sql = "INSERT INTO dbfw.dbfw_fordb(dbfw_inst_id,address_id,state,monitor_type,client_group_id,isdelete) VALUES(?,?,?,?,?,0)";
		Object[] args = { dbfwId, addressId, ifDBFWStoped(dbfwId), monitorType, clientGroupId };
		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql, args);
		this.getJdbcUtils().execute(sql, args);
	}

	/* 根据数据库地址ID获取数据库ID */
	public int getDbIdByDbAddrID(Connection conn, int DbaddrId) {
		String getdbid = "SELECT database_id FROM dbfw.database_addresses WHERE address_id=? ";
		Object[] args = { DbaddrId };
		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), getdbid, args);
		return this.getJdbcUtils().queryForInteger(conn, getdbid, args);
	}

	public void addDbfwForDbAddr(Connection conn, int dbfwId, int addressId, int monitorType, int clientGroupId) {
		int dbid = getDbIdByDbAddrID(conn, addressId);
		String checkSql = "SELECT COUNT(*) FROM dbfw.dbfw_fordb WHERE dbfw_inst_id=? AND address_id=? AND database_id=? AND isdelete='0'";
		String checknullSql = "SELECT COUNT(*) FROM dbfw.dbfw_fordb WHERE dbfw_inst_id=? AND database_id=? AND address_id IS NULL ";
		String sql = "INSERT INTO dbfw.dbfw_fordb(dbfw_inst_id,database_id,address_id,state,monitor_type,client_group_id,isdelete) VALUES(?,?,?,?,?,?,0)";
		Object[] checkArgs = { dbfwId, addressId, dbid };
		Object[] checknullArgs = { dbfwId, dbid };
		Object[] args = { dbfwId, dbid, addressId, ifDBFWStoped(dbfwId), monitorType, clientGroupId };
		Object[] nullargs = { dbfwId, dbid, null, ifDBFWStoped(dbfwId), monitorType, clientGroupId };

		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), checknullSql,
				checknullArgs);
		int cnt = this.getJdbcUtils().queryForInteger(conn, checknullSql, checknullArgs);
		if (cnt == 0) {
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql, nullargs);
			this.getJdbcUtils().execute(conn, sql, nullargs);
		} else {
			String updatenullSql = "UPDATE dbfw.dbfw_fordb SET isdelete='0' WHERE dbfw_inst_id=? AND database_id=? AND address_id IS NULL ";
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), updatenullSql,
					checknullArgs);
			this.getJdbcUtils().execute(conn, updatenullSql, checknullArgs);
		}

		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), checkSql, checkArgs);
		cnt = this.getJdbcUtils().queryForInteger(conn, checkSql, checkArgs);
		if (cnt > 0) {
			throw new DAOException("该安全实例已经保护了此数据库实例");
		}
		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql, args);
		this.getJdbcUtils().execute(conn, sql, args);
	}

	public void addRemoteDbfwForAddrInfo(String remoteHost, int remotePort, int dbfwId, int addressId, int portId) {
		String insertRemoteSql = "INSERT INTO dbfw.remote_proxy(host,port) VALUES(?,?)";
		String updatePortSql = "UPDATE dbfw.group_port SET port_used=1 WHERE port_id=?";
		String updateDbfwForAddrSql = "UPDATE dbfw.dbfw_fordb SET port_id=?,remote_address_id=LAST_INSERT_ID() WHERE dbfw_inst_id=? AND address_id=? AND isdelete='0'";
		Object[] insertRemoteArgs = { remoteHost, remotePort };
		Object[] updatePortArgs = { portId };
		Object[] updateDbfwForAddrArgs = { portId, dbfwId, addressId };

		Connection conn = null;

		try {
			conn = this.getJdbcUtils().getConnection();
			this.getJdbcUtils().beginTransaction(conn);
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), insertRemoteSql,
					insertRemoteArgs);
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), updatePortSql,
					updatePortArgs);
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), updateDbfwForAddrSql,
					updateDbfwForAddrArgs);
			this.getJdbcUtils().execute(conn, insertRemoteSql, insertRemoteArgs);
			this.getJdbcUtils().execute(conn, updatePortSql, updatePortArgs);
			this.getJdbcUtils().execute(conn, updateDbfwForAddrSql, updateDbfwForAddrArgs);
			this.getJdbcUtils().commit(conn);
		} catch (DAOException ex) {
			this.getJdbcUtils().rollback(conn);
			throw ex;
		} finally {
			this.getJdbcUtils().close(conn);
		}
	}

	public void updateSessionDbUserInfo(int addressID, String username, String userpwd) {
		String updateSql = "UPDATE dbfw.database_addresses SET db_username=?, db_passwd=?  WHERE address_id=?";
		Object[] updateArgs = { username, userpwd, addressID };

		Connection conn = null;

		try {
			conn = this.getJdbcUtils().getConnection();
			this.getJdbcUtils().beginTransaction(conn);
			this
					.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), updateSql,
							updateArgs);
			this.getJdbcUtils().execute(conn, updateSql, updateArgs);
			this.getJdbcUtils().commit(conn);
		} catch (DAOException ex) {
			this.getJdbcUtils().rollback(conn);
			throw ex;
		} finally {
			this.getJdbcUtils().close(conn);
		}
	}

	public void removeLogTable(String tablename) {
		String dropSql = "Drop table " + tablename;
		Object[] dropArgs = {};

		Connection conn = null;

		try {
			conn = this.getJdbcUtils().getConnection();
			this.getJdbcUtils().beginTransaction(conn);
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), dropSql, dropArgs);
			this.getJdbcUtils().execute(conn, dropSql, dropArgs);
			this.getJdbcUtils().commit(conn);
		} catch (DAOException ex) {
			this.getJdbcUtils().rollback(conn);
			throw ex;
		} finally {
			this.getJdbcUtils().close(conn);
		}
	}

	public void addRemoteDbfwForAddrInfo(Connection conn, String remoteHost, int remotePort, int dbfwId, int addressId,
			int portId) {
		String insertRemoteSql = "INSERT INTO dbfw.remote_proxy(host,port) VALUES(?,?)";
		String updatePortSql = "UPDATE dbfw.group_port SET port_used=1 WHERE port_id=?";
		String updateDbfwForAddrSql = "UPDATE dbfw.dbfw_fordb SET port_id=?,remote_address_id=LAST_INSERT_ID() WHERE dbfw_inst_id=? AND address_id=? AND isdelete='0'";
		Object[] insertRemoteArgs = { remoteHost, remotePort };
		Object[] updatePortArgs = { portId };
		Object[] updateDbfwForAddrArgs = { portId, dbfwId, addressId };

		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), insertRemoteSql,
				insertRemoteArgs);
		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), updatePortSql,
				updatePortArgs);
		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), updateDbfwForAddrSql,
				updateDbfwForAddrArgs);
		this.getJdbcUtils().execute(conn, insertRemoteSql, insertRemoteArgs);
		this.getJdbcUtils().execute(conn, updatePortSql, updatePortArgs);
		this.getJdbcUtils().execute(conn, updateDbfwForAddrSql, updateDbfwForAddrArgs);
	}

	public void addBywayDbfwForAddrInfo(int groupId, int dbfwId, int addressId) {
		Connection conn = null;
		String existsSql = "SELECT t2.id FROM dbfw.dbfw_fordb t1, dbfw.npc_info t2 WHERE t1.dbfw_inst_id =? AND t2.dbfw_inst_id = t1.dbfw_inst_id AND t2.address_id = t1.address_id AND t2.database_id = t1.database_id AND t2.group_id =? AND t1.isdelete = '0'";
		Object[] existsArgs = { dbfwId, groupId };

		try {
			conn = this.getJdbcUtils().getConnection();
			this.getJdbcUtils().beginTransaction(conn);
			this
					.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), existsSql,
							existsArgs);
			Integer id = this.getJdbcUtils().queryForInteger(conn, existsSql, existsArgs);
			if (null != id && id != 0) {
				String sql = "UPDATE dbfw.dbfw_fordb SET npc_info_id=? WHERE dbfw_inst_id=? AND address_id=? AND isdelete='0'";
				Object[] args = { id, dbfwId, addressId };
				this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql, args);
				this.getJdbcUtils().execute(conn, sql, args);
			} else {
				String sql = "INSERT INTO dbfw.npc_info(npc_id,group_id) SELECT IFNULL(MAX(t2.id),0)+1,? "
						+ "FROM dbfw.dbfw_fordb t1, dbfw.npc_info t2 "
						+ "WHERE t1.dbfw_inst_id=? AND t1.isdelete='0' AND t2.dbfw_inst_id = t1.dbfw_inst_id AND t2.address_id = t1.address_id AND t2.database_id = t1.database_id";
				// "WHERE t1.dbfw_inst_id=? AND t1.isdelete='0'";
				Object[] args = { groupId, dbfwId };
				this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql, args);
				this.getJdbcUtils().execute(conn, sql, args);

				String sql1 = "UPDATE dbfw.dbfw_fordb SET npc_info_id=LAST_INSERT_ID() WHERE dbfw_inst_id=? AND address_id=? AND isdelete='0'";
				Object[] args1 = { dbfwId, addressId };
				this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql1, args1);
				this.getJdbcUtils().execute(conn, sql1, args1);
			}
			this.getJdbcUtils().commit(conn);
		} catch (DAOException ex) {
			this.getJdbcUtils().rollback(conn);
			throw ex;
		} finally {
			this.getJdbcUtils().close(conn);
		}
	}

	/**
	 * 网桥插入npc_info表
	 * 
	 * @param conn
	 * @param groupId
	 * @param dbfwId
	 * @param addressId
	 * @param dbfwInfo
	 * @param interfaceInfo
	 * @param commands
	 * @param flushCommands
	 */
	public void addBypassDbfwForAddrInfo(Connection conn, int groupId, int dbfwId, int addressId, DBFWInfo dbfwInfo,
			InterfaceInfo interfaceInfo, List<NativeCommand> commands, List<NativeCommand> flushCommands) {

		int dbid = getDbIdByDbAddrID(conn, addressId);
		Object[] npcIdArgs = { dbfwId };
		String npc_id_sql = "select ifnull(max(npc_id),0)+1 from  npc_info where dbfw_inst_id=?";
		int v_npc_id = this.getJdbcUtils().queryForInteger(conn, npc_id_sql, npcIdArgs);
		// 插入npc——info表
		String sql = "INSERT INTO dbfw.npc_info(npc_id,group_id,dbfw_inst_id,database_id,address_id) VALUES(?,?,?,?,?)";
		Object[] argsx = { v_npc_id, groupId, dbfwId, dbid, addressId };
		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql, argsx);
		this.getJdbcUtils().execute(conn, sql, argsx);
	}

	public void addBywayDbfwForAddrInfo(Connection conn, int groupId, int dbfwId, int addressId, DBFWInfo dbfwInfo,
			InterfaceInfo interfaceInfo, List<NativeCommand> commands, List<NativeCommand> flushCommands) {

		NativeCommand command = null;
		int dbid = getDbIdByDbAddrID(conn, addressId);
		String existsSql = "SELECT t2.id FROM dbfw.dbfw_fordb t1, dbfw.npc_info t2 WHERE t1.dbfw_inst_id =? AND t2.group_id =? AND t1.address_id=? AND t1.isdelete = '0' AND t1.dbfw_inst_id = t2.dbfw_inst_id AND t1.database_id = t2.database_id AND t1.address_id = t2.address_id LIMIT 1";
		Object[] existsArgs = { dbfwId, groupId, addressId };

		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), existsSql, existsArgs);
		Integer id = this.getJdbcUtils().queryForInteger(conn, existsSql, existsArgs);
		if (null != id && id != 0) {
			/*
			 * String sql =
			 * "UPDATE dbfw.dbfw_fordb SET npc_info_id=? WHERE dbfw_inst_id=? AND address_id=? AND isdelete='0'"
			 * ; Object[] args = { id, dbfwId, addressId };
			 * this.debugSqlWithClassname
			 * (Thread.currentThread().getStackTrace()[1].getMethodName(),
			 * sql,args); this.getJdbcUtils().execute(conn, sql, args);
			 */
			/*
			 * String npcidSql =
			 * "SELECT t2.npc_id FROM dbfw.npc_info t2 WHERE t2.id=? "; Object[]
			 * npcidArgs = {id}; Integer npcid =
			 * this.getJdbcUtils().queryForInteger(conn, npcidSql, npcidArgs);
			 * command = new FlushNpcInfoCommand(dbfwInfo.getAddress(),
			 * dbfwInfo.getPort(), dbfwId, npcid); flushCommands.add(command);
			 */
		} else {
			/*
			 * // 由于老马那边暂时只能启动一个NPC，因此将所有的NPCID全部设置为1 String sql =
			 * "INSERT INTO dbfw.npc_info(npc_id,group_id) VALUES(1,?)";
			 * //"SELECT IFNULL(MAX(t2.npc_id),0)+1,? " +
			 * //"FROM dbfw.dbfw_fordb t1, dbfw.npc_info t2 " +
			 * //"WHERE t1.dbfw_inst_id=? AND t1.isdelete='0'"; Object[] args =
			 * { groupId };
			 * this.debugSqlWithClassname(Thread.currentThread().getStackTrace
			 * ()[1].getMethodName(), sql,args);
			 * this.getJdbcUtils().execute(conn, sql, args);
			 */
			// >>>fjw20140507开始，npc_id范围1~4
			int v_npc_id = 1;
			String cur_npc = "";
			int npcNum = 0;
			String sql_check_cnt = "SELECT DISTINCT t1.npc_id AS npcid FROM npc_info t1, dbfw_fordb t2 WHERE t1.dbfw_inst_id = t2.dbfw_inst_id AND t1.address_id = t2.address_id AND t1.database_id = t2.database_id AND t2.isdelete = 0 AND t1.dbfw_inst_id =?";
			/*
			 * "SELECT npc_id as npcid FROM npc_info WHERE id in("+
			 * "SELECT npc_info_id FROM dbfw_fordb WHERE dbfw_inst_id=? AND isdelete=0 AND npc_info_id>0 GROUP BY npc_info_id)"
			 * ;
			 */
			Object[] args = { dbfwId };
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql_check_cnt, null);
			List<NpcInfos> ret = (List<NpcInfos>) this.getJdbcUtils().query(conn, sql_check_cnt, args, NpcInfos.class);
			// int npcNum = this.getJdbcUtils().queryForInteger(conn,
			// sql_check_cnt,args);
			if (ret.size() > 3) // 如果被同一防火墙使用达到四个则报错
			{
				String groupIdSql = "SELECT count(DISTINCT group_id) AS group_count FROM npc_info t1, dbfw_fordb t2 WHERE t1.dbfw_inst_id = t2.dbfw_inst_id AND t1.address_id = t2.address_id AND t1.database_id = t2.database_id AND t2.isdelete = 0 AND t1.dbfw_inst_id =? AND t1.group_id != ?";
				int groupCount = this.getJdbcUtils().queryForInteger(groupIdSql, new Object[] { dbfwId, groupId });
				if (groupCount > 3) {
					throw new DAOException("该安全实例已达NPC最大数量（4个）");
				}
			}
			String sql_user_npcid1 = "SELECT DISTINCT t1.npc_id AS npcid FROM npc_info t1 WHERE t1.dbfw_inst_id = ? AND t1.group_id = ?";
			List<NpcInfos> usrNpcIdList1 = (List<NpcInfos>) this.getJdbcUtils().query(conn, sql_user_npcid1,
					new Object[] { dbfwId, groupId }, NpcInfos.class);
			String sql_user_npcid = "SELECT DISTINCT t1.npc_id AS npcid FROM npc_info t1 WHERE t1.dbfw_inst_id = ? AND t1.group_id != ?";
			List<NpcInfos> usrNpcIdList = (List<NpcInfos>) this.getJdbcUtils().query(conn, sql_user_npcid,
					new Object[] { dbfwId, groupId }, NpcInfos.class);
			int[] npcIdArry = { 1, 2, 3, 4 };
			if (usrNpcIdList1 != null) {
				if (usrNpcIdList1.size() == 1) {
					v_npc_id = usrNpcIdList1.get(0).getNpcid();
				} else {
					if (usrNpcIdList != null) {
						if (usrNpcIdList.size() > 0) {
							for (int i = 0; i < npcIdArry.length; i++) {
								int npcCount = 0;
								for (int j = 0; j < usrNpcIdList.size(); j++) {
									if (npcIdArry[i] != usrNpcIdList.get(j).getNpcid()) {
										npcCount++;
									}
								}
								if (npcCount == usrNpcIdList.size()) {
									v_npc_id = npcIdArry[i];
									break;
								}
							}
						}
					}
				}
			} else {
				if (usrNpcIdList != null) {
					if (usrNpcIdList.size() > 0) {
						for (int i = 0; i < npcIdArry.length; i++) {
							int npcCount = 0;
							for (int j = 0; j < usrNpcIdList.size(); j++) {
								if (npcIdArry[i] != usrNpcIdList.get(j).getNpcid()) {
									npcCount++;
								}
							}
							if (npcCount == usrNpcIdList.size()) {
								v_npc_id = npcIdArry[i];
								break;
							}
						}
					}
				}
			}

			/*
			 * if(ret.size()<1)//当前旁路组没有被使用，npc_id从1开始 { v_npc_id=1; }
			 * if(ret.size()>0 && ret.size()<4) { for(NpcInfos npc :ret) {
			 * cur_npc=cur_npc+ ","+npc.getNpcid(); }
			 * cur_npc=cur_npc.substring(1); // 查找可用的npc_id String sql3 =
			 * "SELECT a FROM ( "+ "SELECT 1 AS a "+ "UNION "+ "SELECT 2 AS a "+
			 * "UNION "+ "SELECT 3 AS a "+ "UNION "+
			 * "SELECT 4 AS a) temp WHERE a NOT IN("+cur_npc+") LIMIT 1";
			 * this.debugSqlWithClassname
			 * (Thread.currentThread().getStackTrace()[1].getMethodName(),
			 * sql3,null); v_npc_id = this.getJdbcUtils().queryForInteger(conn,
			 * sql3); }
			 */

			// 插入npc——info表
			String sql = "INSERT INTO dbfw.npc_info(npc_id,group_id,dbfw_inst_id,database_id,address_id) VALUES(?,?,?,?,?)";
			Object[] argsx = { v_npc_id, groupId, dbfwId, dbid, addressId };
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql, argsx);
			this.getJdbcUtils().execute(conn, sql, argsx);
			// <<<<结束
			// String sql1 =
			// "UPDATE dbfw.dbfw_fordb SET npc_info_id=LAST_INSERT_ID() WHERE dbfw_inst_id=? AND database_id=(select addr.database_id from database_addresses addr where addr.address_id=?) AND isdelete='0'";

			String sql2 = "SELECT id FROM dbfw.npc_info WHERE id=LAST_INSERT_ID()";
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql2, null);
			npcNum = this.getJdbcUtils().queryForInteger(conn, sql2);
			/* 20140624 付金旺修改： dbfw_fordb的npc_info_id应存id，新建和刷新npc应传npc_id */
			String sqlnpcid = "SELECT npc_id FROM dbfw.npc_info WHERE id=" + npcNum;
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sqlnpcid, null);
			v_npc_id = this.getJdbcUtils().queryForInteger(conn, sqlnpcid);

			/*
			 * String sql1 =
			 * "UPDATE dbfw.dbfw_fordb SET npc_info_id=? WHERE dbfw_inst_id=? AND address_id=? AND isdelete='0'"
			 * ; Object[] args1 = {npcNum, dbfwId, addressId };
			 * this.debugSqlWithClassname
			 * (Thread.currentThread().getStackTrace()[1].getMethodName(),
			 * sql1,args1); this.getJdbcUtils().execute(conn, sql1, args1);
			 */

			/*
			 * command = new StartNewNpcCommand(dbfwInfo.getAddress(),
			 * dbfwInfo.getPort(), v_npc_id, interfaceInfo.getIfName());
			 * commands.add(command); command = new
			 * FlushNpcInfoCommand(dbfwInfo.getAddress(), dbfwInfo.getPort(),
			 * dbfwId, v_npc_id); flushCommands.add(command);
			 */
		}
	}

	public void addNativeDbfwForAddrInfo(int dbfwId, int addressId, int portId) {
		Connection conn = null;
		String updatePortSql = "UPDATE dbfw.group_port SET port_used=1 WHERE port_id=?";
		String updateDbfwForAddrSql = "UPDATE dbfw.dbfw_fordb SET port_id=? WHERE dbfw_inst_id=? AND address_id=? AND isdelete='0'";
		Object[] updatePortArgs = { portId };
		Object[] updateDbfwForAddrArgs = { portId, dbfwId, addressId };

		try {
			conn = this.getJdbcUtils().getConnection();
			this.getJdbcUtils().beginTransaction(conn);
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), updatePortSql,
					updatePortArgs);
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), updateDbfwForAddrSql,
					updateDbfwForAddrArgs);
			this.getJdbcUtils().execute(conn, updatePortSql, updatePortArgs);
			this.getJdbcUtils().execute(conn, updateDbfwForAddrSql, updateDbfwForAddrArgs);
			this.getJdbcUtils().commit(conn);
		} catch (DAOException ex) {
			this.getJdbcUtils().rollback(conn);
			throw ex;
		} finally {
			this.getJdbcUtils().close(conn);
		}
	}

	public void addNativeDbfwForAddrInfo(Connection conn, int dbfwId, int addressId, int portId) {
		String updatePortSql = "UPDATE dbfw.group_port SET port_used=1 WHERE port_id=?";
		String updateDbfwForAddrSql = "UPDATE dbfw.dbfw_fordb SET port_id=? WHERE dbfw_inst_id=? AND address_id=? AND isdelete='0'";
		Object[] updatePortArgs = { portId };
		Object[] updateDbfwForAddrArgs = { portId, dbfwId, addressId };

		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), updatePortSql,
				updatePortArgs);
		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), updateDbfwForAddrSql,
				updateDbfwForAddrArgs);
		this.getJdbcUtils().execute(conn, updatePortSql, updatePortArgs);
		this.getJdbcUtils().execute(conn, updateDbfwForAddrSql, updateDbfwForAddrArgs);
	}

	public void addHalfTransDbfwForAddrInfo(int dbfwId, int addressId, int portId) {
		Connection conn = null;
		String updatePortSql = "UPDATE dbfw.group_port SET port_used=1 WHERE port_id=?";
		String updateDbfwForAddrSql = "UPDATE dbfw.dbfw_fordb SET port_id=? WHERE dbfw_inst_id=? AND address_id=? AND isdelete='0'";
		Object[] updatePortArgs = { portId };
		Object[] updateDbfwForAddrArgs = { portId, dbfwId, addressId };

		try {
			conn = this.getJdbcUtils().getConnection();
			this.getJdbcUtils().beginTransaction(conn);
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), updatePortSql,
					updatePortArgs);
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), updateDbfwForAddrSql,
					updateDbfwForAddrArgs);
			this.getJdbcUtils().execute(conn, updatePortSql, updatePortArgs);
			this.getJdbcUtils().execute(conn, updateDbfwForAddrSql, updateDbfwForAddrArgs);
			this.getJdbcUtils().commit(conn);
		} catch (DAOException ex) {
			this.getJdbcUtils().rollback(conn);
			throw ex;
		} finally {
			this.getJdbcUtils().close(conn);
		}
	}

	public void addHalfTransDbfwForAddrInfo(Connection conn, int dbfwId, int addressId, int portId) {
		String updatePortSql = "UPDATE dbfw.group_port SET port_used=1 WHERE port_id=?";
		String updateDbfwForAddrSql = "UPDATE dbfw.dbfw_fordb SET port_id=? WHERE dbfw_inst_id=? AND address_id=? AND isdelete='0'";
		Object[] updatePortArgs = { portId };
		Object[] updateDbfwForAddrArgs = { portId, dbfwId, addressId };

		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), updatePortSql,
				updatePortArgs);
		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), updateDbfwForAddrSql,
				updateDbfwForAddrArgs);
		this.getJdbcUtils().execute(conn, updatePortSql, updatePortArgs);
		this.getJdbcUtils().execute(conn, updateDbfwForAddrSql, updateDbfwForAddrArgs);
	}

	public void removeDbfwForDbAddr(Connection conn, int dbfwId, int addressId) {
		// 删除数据库地址时，将dbfw_fordb对应记录删除 (fjw20130821)
		// String sql =
		// "UPDATE dbfw.dbfw_fordb SET isdelete='1' WHERE dbfw_inst_id=? AND address_id=? AND isdelete='0'";
		String sql = "DELETE FROM dbfw.dbfw_fordb WHERE dbfw_inst_id=? AND address_id=? AND isdelete='0'";
		Object[] args = { dbfwId, addressId };
		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql, args);
		this.getJdbcUtils().execute(conn, sql, args);
	}

	private void removeDbfwForDbAddr(Connection conn, int addressId) {
		// String sql =
		// "UPDATE dbfw.dbfw_fordb SET isdelete='1' WHERE address_id=? AND isdelete='0'";
		String sql = "DELETE FROM dbfw.dbfw_fordb WHERE address_id=? AND isdelete='0'";
		Object[] args = { addressId };
		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql, args);
		this.getJdbcUtils().execute(conn, sql, args);
	}

	public DBFWForDbAddr getDBFWForDbAddrByAddressAndDbfw(int addressId, int dbfwId) {
		String sql = "SELECT DISTINCT t1.address_id AS addressId, t1.dbfw_inst_id AS dbfwId, t2.name AS dbfwName, t1.state AS state, t1.monitor_type AS monitorType, CASE WHEN t1.monitor_type=2 THEN t5.group_id ELSE t3.group_id END AS groupId, CASE WHEN t1.monitor_type=2 THEN t8.group_name ELSE t4.group_name END  AS groupName, IFNULL(t3.port_id,0) AS portId, "
				+ "IFNULL(t3.port_name,'') AS portName, t1.npc_info_id AS npcInfoId, IFNULL(t5.npc_id,0) AS npcNum, t1.client_group_id AS clientGroupId, IFNULL(t6.name,'') AS clientGroupName, "
				+ "t1.remote_address_id AS remoteId, IFNULL(t7.host,'') AS remoteHost, IFNULL(t7.port,0) AS remotePort "
				+ "FROM dbfw.dbfw_fordb t1 INNER JOIN dbfw.dbfw_instances t2 ON t1.dbfw_inst_id=t2.instance_id AND t1.isdelete='0' AND t2.isdelete='0' "
				+ "LEFT JOIN dbfw.group_port t3 ON t1.port_id=t3.port_id "
				+ "LEFT JOIN interface_group t4 ON t3.group_id=t4.group_id "
				+
				// "LEFT JOIN dbfw.npc_info t5 ON t1.npc_info_id=t5.id " +
				"LEFT JOIN dbfw.npc_info t5 ON t5.dbfw_inst_id = t1.dbfw_inst_id AND t5.address_id = t1.address_id AND t5.database_id = t1.database_id "
				+ "LEFT JOIN interface_group t8 ON t5.group_id=t8.group_id "
				+ "LEFT JOIN dbfw.client_group_info t6 ON t1.client_group_id=t6.id "
				+ "LEFT JOIN dbfw.remote_proxy t7 ON t1.remote_address_id=t7.id "
				+ "WHERE t1.address_id=? AND t1.dbfw_inst_id=?";
		Object[] args = { addressId, dbfwId };
		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql, args);
		return (DBFWForDbAddr) this.getJdbcUtils().queryForObject(sql, args, DBFWForDbAddr.class);
	}

	public DBFWForDbAddr getDBFWForDbAddrByAddressAndDbfw(Connection conn, int addressId, int dbfwId) {
		String sql = "SELECT DISTINCT t1.address_id AS addressId, t1.dbfw_inst_id AS dbfwId, t2.name AS dbfwName, t1.state AS state, "
				+ "t1.monitor_type AS monitorType, CASE WHEN t1.monitor_type=2 THEN t5.group_id WHEN t1.monitor_type=4 THEN t5.group_id ELSE t3.group_id END AS groupId, "
				+ "CASE WHEN t1.monitor_type=2 THEN t8.group_name WHEN t1.monitor_type=4 THEN t8.group_name ELSE t4.group_name END  AS groupName, IFNULL(t3.port_id,0) AS portId, "
				+ "IFNULL(t3.port_name,'') AS portName, t1.npc_info_id AS npcInfoId, IFNULL(t5.npc_id,0) AS npcNum, t1.client_group_id AS clientGroupId, IFNULL(t6.name,'') AS clientGroupName, "
				+ "t1.remote_address_id AS remoteId, IFNULL(t7.host,'') AS remoteHost, IFNULL(t7.port,0) AS remotePort "
				+ "FROM dbfw.dbfw_fordb t1 INNER JOIN dbfw.dbfw_instances t2 ON t1.dbfw_inst_id=t2.instance_id AND t1.isdelete='0' AND t2.isdelete='0' "
				+ "LEFT JOIN dbfw.group_port t3 ON t1.port_id=t3.port_id "
				+ "LEFT JOIN interface_group t4 ON t3.group_id=t4.group_id "
				+ "LEFT JOIN dbfw.npc_info t5 ON t5.dbfw_inst_id = t1.dbfw_inst_id AND t5.address_id = t1.address_id AND t5.database_id = t1.database_id "
				+
				// "LEFT JOIN dbfw.npc_info t5 ON t1.npc_info_id=t5.id " +
				"LEFT JOIN interface_group t8 ON t5.group_id=t8.group_id "
				+ "LEFT JOIN dbfw.client_group_info t6 ON t1.client_group_id=t6.id "
				+ "LEFT JOIN dbfw.remote_proxy t7 ON t1.remote_address_id=t7.id "
				+ "WHERE t1.address_id=? AND t1.dbfw_inst_id=?";
		Object[] args = { addressId, dbfwId };
		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql, args);
		return (DBFWForDbAddr) this.getJdbcUtils().queryForObject(conn, sql, args, DBFWForDbAddr.class);
	}

	public void removeRemoteProxyAndDbfwForAddr(int remoteId, int portId, int dbfwId, int addressId) {
		Connection conn = null;
		String updatePortSql = "UPDATE dbfw.group_port SET port_used=0 WHERE port_id=?";
		String otherRemoteSql = "SELECT COUNT(*) FROM dbfw.dbfw_fordb t1 WHERE t1.dbfw_inst_id=? AND t1.remote_address_id=? AND t1.isdelete='0'";
		String removeRemoteSql = "DELETE FROM dbfw.remote_proxy WHERE id=?";
		Object[] updatePortArgs = { portId };
		Object[] otherRemoteArgs = { dbfwId, remoteId };
		Object[] removeRemoteArgs = { remoteId };

		try {
			conn = this.getJdbcUtils().getConnection();
			this.getJdbcUtils().beginTransaction(conn);
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), updatePortSql,
					updatePortArgs);
			this.getJdbcUtils().execute(conn, updatePortSql, updatePortArgs);

			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), otherRemoteSql,
					otherRemoteArgs);
			int cnt = this.getJdbcUtils().queryForInteger(conn, otherRemoteSql, otherRemoteArgs);
			if (cnt <= 1) {
				this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), removeRemoteSql,
						removeRemoteArgs);
				this.getJdbcUtils().execute(conn, removeRemoteSql, removeRemoteArgs);
			}
			this.getJdbcUtils().commit(conn);
		} catch (DAOException ex) {
			this.getJdbcUtils().rollback(conn);
			throw ex;
		} finally {
			this.getJdbcUtils().close(conn);
		}
	}

	public void removeRemoteProxyAndDbfwForAddr(Connection conn, int remoteId, int portId, int dbfwId, int addressId) {
		String updatePortSql = "UPDATE dbfw.group_port SET port_used=0 WHERE port_id=?";
		String otherRemoteSql = "SELECT COUNT(*) FROM dbfw.dbfw_fordb t1 WHERE t1.dbfw_inst_id=? AND t1.remote_address_id=? AND t1.isdelete='0'";
		String removeRemoteSql = "DELETE FROM dbfw.remote_proxy WHERE id=?";
		Object[] updatePortArgs = { portId };
		Object[] otherRemoteArgs = { dbfwId, remoteId };
		Object[] removeRemoteArgs = { remoteId };

		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), updatePortSql,
				updatePortArgs);
		this.getJdbcUtils().execute(conn, updatePortSql, updatePortArgs);

		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), otherRemoteSql,
				otherRemoteArgs);
		int cnt = this.getJdbcUtils().queryForInteger(conn, otherRemoteSql, otherRemoteArgs);
		if (cnt <= 1) {
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), removeRemoteSql,
					removeRemoteArgs);
			this.getJdbcUtils().execute(conn, removeRemoteSql, removeRemoteArgs);
		}
	}

	public void removeRemoteProxyAndDbfwForAddr(Connection conn, int remoteId, int portId, int addressId) {
		String updatePortSql = "UPDATE dbfw.group_port SET port_used=0 WHERE port_id=?";
		String otherRemoteSql = "SELECT COUNT(*) FROM dbfw.dbfw_fordb t1 WHERE t1.remote_address_id=? AND t1.isdelete='0'";
		String removeRemoteSql = "DELETE FROM dbfw.remote_proxy WHERE id=?";
		Object[] updatePortArgs = { portId };
		Object[] otherRemoteArgs = { remoteId };
		Object[] removeRemoteArgs = { remoteId };

		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), updatePortSql,
				updatePortArgs);
		this.getJdbcUtils().execute(conn, updatePortSql, updatePortArgs);

		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), otherRemoteSql,
				otherRemoteArgs);
		int cnt = this.getJdbcUtils().queryForInteger(conn, otherRemoteSql, otherRemoteArgs);
		if (cnt <= 1) {
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), removeRemoteSql,
					removeRemoteArgs);
			this.getJdbcUtils().execute(conn, removeRemoteSql, removeRemoteArgs);
		}
	}

	public void removeBywayAndDbfwForAddr(int npcInfoId, int dbfwId, int addressId) {
		Connection conn = null;
		String otherNpcSql = "SELECT COUNT(*) FROM dbfw.dbfw_fordb t1 WHERE t1.dbfw_inst_id=? AND t1.npc_info_id=? AND t1.isdelete='0'";
		String removeNpcSql = "DELETE FROM dbfw.npc_info WHERE id=?";
		Object[] otherNpcArgs = { dbfwId, npcInfoId };
		Object[] removeNpcArgs = { npcInfoId };

		try {
			conn = this.getJdbcUtils().getConnection();
			this.getJdbcUtils().beginTransaction(conn);
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), otherNpcSql,
					otherNpcArgs);
			int cnt = this.getJdbcUtils().queryForInteger(conn, otherNpcSql, otherNpcArgs);
			if (cnt <= 1) {
				this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), removeNpcSql,
						removeNpcArgs);
				this.getJdbcUtils().execute(conn, removeNpcSql, removeNpcArgs);
			}
			this.getJdbcUtils().commit(conn);
		} catch (DAOException ex) {
			this.getJdbcUtils().rollback(conn);
			throw ex;
		} finally {
			this.getJdbcUtils().close(conn);
		}
	}

	public boolean removeBywayAndDbfwForAddr(Connection conn, int npcInfoId, int dbfwId, int addressId) {
		String otherNpcSql = "SELECT COUNT(*) FROM dbfw.dbfw_fordb t1 WHERE t1.dbfw_inst_id=? AND t1.npc_info_id=? AND t1.isdelete='0' and address_id is not null";
		String removeNpcSql = "DELETE FROM dbfw.npc_info WHERE id=?";
		Object[] otherNpcArgs = { dbfwId, npcInfoId };
		Object[] removeNpcArgs = { npcInfoId };

		this
				.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), otherNpcSql,
						otherNpcArgs);
		int cnt = this.getJdbcUtils().queryForInteger(conn, otherNpcSql, otherNpcArgs);
		if (cnt <= 1) {
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), removeNpcSql,
					removeNpcArgs);
			this.getJdbcUtils().execute(conn, removeNpcSql, removeNpcArgs);
			return true;
		} else {
			return false;
		}
	}

	public boolean removeBywayAndDbfwForAddr(Connection conn, int npcInfoId, int addressId) {
		String otherNpcSql = "SELECT COUNT(*) FROM dbfw.dbfw_fordb t1 WHERE t1.npc_info_id=? AND t1.isdelete='0'";
		String removeNpcSql = "DELETE FROM dbfw.npc_info WHERE id=?";
		Object[] otherNpcArgs = { npcInfoId };
		Object[] removeNpcArgs = { npcInfoId };

		this
				.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), otherNpcSql,
						otherNpcArgs);
		int cnt = this.getJdbcUtils().queryForInteger(conn, otherNpcSql, otherNpcArgs);
		if (cnt <= 1) {
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), removeNpcSql,
					removeNpcArgs);
			this.getJdbcUtils().execute(conn, removeNpcSql, removeNpcArgs);
			return true;
		} else {
			return false;
		}
	}

	public void removeNativeProxyAndDbfwForAddr(int portId, int dbfwId, int addressId) {
		String updatePortSql = "UPDATE dbfw.group_port SET port_used=0 WHERE port_id=?";
		Object[] updatePortArgs = { portId };
		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), updatePortSql,
				updatePortArgs);
		this.getJdbcUtils().execute(updatePortSql, updatePortArgs);
	}

	public void removeNativeProxyAndDbfwForAddr(Connection conn, int portId, int dbfwId, int addressId) {
		String updatePortSql = "UPDATE dbfw.group_port SET port_used=0 WHERE port_id=?";
		Object[] updatePortArgs = { portId };
		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), updatePortSql,
				updatePortArgs);
		this.getJdbcUtils().execute(conn, updatePortSql, updatePortArgs);
	}

	public void removeNativeProxyAndDbfwForAddr(Connection conn, int portId, int addressId) {
		String updatePortSql = "UPDATE dbfw.group_port SET port_used=0 WHERE port_id=?";
		Object[] updatePortArgs = { portId };
		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), updatePortSql,
				updatePortArgs);
		this.getJdbcUtils().execute(conn, updatePortSql, updatePortArgs);
	}

	public void removeHalfTransAndDbfwForAddr(int portId, int dbfwId, int addressId) {
		String updatePortSql = "UPDATE dbfw.group_port SET port_used=0 WHERE port_id=?";
		Object[] updatePortArgs = { portId };
		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), updatePortSql,
				updatePortArgs);
		this.getJdbcUtils().execute(updatePortSql, updatePortArgs);
	}

	public void removeHalfTransAndDbfwForAddr(Connection conn, int portId, int dbfwId, int addressId) {
		// String updatePortSql =
		// "UPDATE dbfw.group_port SET port_used=0 WHERE port_id=?";
		// /*网桥端口，在数据库和安全实例关联时自动生成11000～15000，取消关联是删除记录*/
		String updatePortSql = "DELETE FROM dbfw.group_port  WHERE port_id=?";
		Object[] updatePortArgs = { portId };
		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), updatePortSql,
				updatePortArgs);
		this.getJdbcUtils().execute(conn, updatePortSql, updatePortArgs);

		String removeNpcSql = "DELETE FROM dbfw.npc_info WHERE dbfw_inst_id=? AND address_id=?";
		Object[] otherNpcArgs = { dbfwId, addressId };
		// 删除NPC信息
		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), removeNpcSql,
				otherNpcArgs);
		this.getJdbcUtils().execute(conn, removeNpcSql, otherNpcArgs);

	}

	public void removeHalfTransAndDbfwForAddr(Connection conn, int portId, int addressId) {
		String updatePortSql = "UPDATE dbfw.group_port SET port_used=0 WHERE port_id=?";
		Object[] updatePortArgs = { portId };
		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), updatePortSql,
				updatePortArgs);
		this.getJdbcUtils().execute(conn, updatePortSql, updatePortArgs);
	}

	/**
	 * 设置安全实例保护的数据库的挂起、恢复状态
	 * 
	 * @param dbfwId
	 * @param dbId
	 * @param status
	 *            0-挂起 1-恢复(正常)
	 * @param commands
	 */
	public void updateDbfwForDbAddrStatus(int dbfwId, int dbId, int status, List<NativeCommand> commands) {
		Connection conn = null;

		try {
			conn = this.getJdbcUtils().getConnection();
			this.getJdbcUtils().beginTransaction(conn);
			String sql = "UPDATE dbfw.dbfw_fordb SET state=? WHERE dbfw_inst_id=? AND isdelete='0' AND address_id IN ("
					+ "SELECT address_id FROM dbfw.database_addresses WHERE database_id=?" + ")";
			Object[] args = { status, dbfwId, dbId };
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql, args);
			this.getJdbcUtils().execute(sql, args);

			DBFWInfo dbfwInfo = this.getDBFWInfoById(conn, dbfwId);
			this.getJdbcUtils().commit(conn);

			NativeCommand command = new FlushDbfwForDbInfoCommand(dbfwInfo.getAddress(), dbfwInfo.getPort(), dbId);
			commands.add(command);
			NativeExecutor.execute(commands);
		} catch (RunException ex) {
			this.getJdbcUtils().rollback(conn);
			throw ex;
		} finally {
			this.getJdbcUtils().close(conn);
		}
	}

	public DBFWInfo getDBFWInfoById(int dbfwId) {
		String sql = "SELECT t1.instance_id AS id, t1.name AS name, t1.address AS address, t1.port AS port, 0 AS status,t1.inst_stat AS instStat "
				+ "FROM dbfw.dbfw_instances t1 " + "WHERE t1.instance_id=? AND t1.isdelete='0'";
		Object[] args = { dbfwId };
		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql, args);
		return (DBFWInfo) this.getJdbcUtils().queryForObject(sql, args, DBFWInfo.class);
	}

	private DBFWInfo getDBFWInfoById(Connection conn, int dbfwId) {
		String sql = "SELECT t1.instance_id AS id, t1.name AS name, t1.address AS address, t1.port AS port, 0 AS status, t1.inst_stat as instStat "
				+ "FROM dbfw.dbfw_instances t1 " + "WHERE t1.instance_id=? AND t1.isdelete='0'";
		Object[] args = { dbfwId };
		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql, args);
		return (DBFWInfo) this.getJdbcUtils().queryForObject(conn, sql, args, DBFWInfo.class);
	}

	@SuppressWarnings("unchecked")
	public List<UserInfo> getUserInfoByDb(int dbId, int type) {
		if (type == 2) {
			String sql = "SELECT t2.database_id AS dbId, 0 AS dbfwId, t1.db_user AS userName, MIN(t1.login_time) AS firstLoginTime, MAX(t1.login_time) AS lastLoginTime "
					+ "FROM dbfw.sessionlog_event t1, dbfw.database_addresses t2 ,dbfw.ac_users t3 "
					+ "WHERE t2.database_id=? "
					+ "AND t3.database_id = t2.database_id "
					+ "AND t3.username = t1.db_user "
					+ "AND t3.isdelete = '0' "
					+ "AND  t1.database_id = t2.database_id "
					+ "AND t1.login_time IS NOT NULL "
					+ "AND t1.login_errorno=0 " + "GROUP BY t2.database_id, t1.db_user";
			Object[] args = { dbId };
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql, args);
			return (List<UserInfo>) this.getJdbcUtils().query(sql, args, UserInfo.class);
		} else {
			String sql = "SELECT t2.database_id AS dbId, 0 AS dbfwId, t1.db_user AS userName, MIN(t1.login_time) AS firstLoginTime, MAX(t1.login_time) AS lastLoginTime "
					+ "FROM dbfw.sessionlog_event t1, dbfw.database_addresses t2 ,dbfw.ac_users t3 "
					+ "WHERE t2.database_id=? "
					+ "AND t3.database_id = t2.database_id "
					+ "AND t3.username = t1.db_user "
					+ "AND t3.isdelete = '0' "
					+ "AND t3.usertype = "
					+ type
					+ " "
					+ "AND t1.database_id = t2.database_id "
					+ "AND t1.login_time IS NOT NULL "
					+ "AND t1.login_errorno=0 " + "GROUP BY t2.database_id, t1.db_user";
			Object[] args = { dbId };
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql, args);
			return (List<UserInfo>) this.getJdbcUtils().query(sql, args, UserInfo.class);
		}
	}

	public List<DatabaseAddress> addDatabase(DatabaseInfo dbInfo, List<DatabaseAddress> addresses,
			List<NoSession> noSessionList) {
		Connection conn = null;
		int dbId = 0;
		int addressId = 0;

		try {
			conn = this.getJdbcUtils().getConnection();
			this.getJdbcUtils().beginTransaction(conn);

			dbId = addXsecDatabaseAndGetId(conn, dbInfo.getName(), dbInfo.getDesc(), dbInfo.getVersion(), dbInfo
					.getType(), dbInfo.getIsdpa(), dbInfo.getIssox(), dbInfo.getIspci(), dbInfo.getIsglba());
			int i = 0;
			for (DatabaseAddress address : addresses) {
				addressId = addAddressAndGetId(conn, address.getAddress(), address.getPort(), address.getServiceName(),
						dbId, address.getUserName(), address.getUserPwd(), address.getDynaPort(), i);
				address.setDatabaseId(dbId);
				address.setId(addressId);
				i++;
			}
			// 判断是否允许添加数据库
			judgeAddDatabases(i);
			if (dbInfo.getType() == 1) {
				if (noSessionList != null) {
					Set<NoSession> ts = new HashSet<NoSession>();
					ts.addAll(noSessionList);
					noSessionList.clear();// 清空list，不然下次把set元素加入此list的时候是在原来的基础上追加元素的
					noSessionList.addAll(ts);// 把set的
					for (NoSession nos : noSessionList) {
						insertNoConnect(conn, dbId, nos);
					}
				}
			}
			this.getJdbcUtils().commit(conn);
			return addresses;
		} catch (DAOException ex) {
			this.getJdbcUtils().rollback(conn);
			throw ex;
		} finally {
			this.getJdbcUtils().close(conn);
		}
	}

	private synchronized int addXsecDatabaseAndGetId(Connection conn, String name, String desc, int version, int type,
			int charset, String issox, String ispci, String isglba) {
		String checkSql = "SELECT COUNT(*) FROM xsec_databases WHERE name=?";
		String genIdSql1 = "SELECT max(t.database_id)+1 AS dbid  FROM   ( "
				+ "SELECT max(database_id) database_id FROM statistics_sqltype " + "UNION ALL "
				+ "SELECT max(database_id) database_id FROM sessionlog_event_summary " + "UNION ALL "
				+ "select max(database_id) database_id from rule_base " + "UNION ALL "
				+ "select max(database_id) database_id from xsec_databases " + ") t ";
		String genIdSql = "SELECT id FROM dbfw.quantum WHERE id<=64 AND id NOT IN (SELECT database_id FROM xsec_databases)";
		String insertSql = "INSERT INTO xsec_databases(database_id,name,dialect,description,is_sox,is_pci,is_dpa,is_glba,is_hipaa,state,db_version,db_type) VALUES(?,?,?,?,?,?,?,?,0,1,?,?)";
		Object[] checkArgs = { name };

		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), checkSql, checkArgs);
		int cnt = this.getJdbcUtils().queryForInteger(conn, checkSql, checkArgs);
		if (cnt > 0) {
			throw new DAOException("数据库名称已经存在");
		}

		int dbId = this.getJdbcUtils().queryForInteger(conn, genIdSql1);
		if (dbId > 64 || dbId == 0) {
			dbId = this.getJdbcUtils().queryForInteger(conn, genIdSql);
		}
		if (dbId == 0) {
			throw new DAOException("无可用数据库ID");
		}
		int dialects = type;
		if (type == 4) {
			dialects = 32;
		} else if (type == 3) {
			dialects = 8;
		} else if (type == 6) {// sysbase数据库
			dialects = 4;
		} else if (type == 10) {
			dialects = 128;
		} else if (type == 12) {
			dialects = 512;
		} else if (type == 5) {// postgres数据库
			dialects = 64;
		} else if (type == 11) {// kingbase数据库
			dialects = 256;
		} else if (type == 9) {// informi数据库
			dialects = 1024;
		} else if (type == 16) {// cachedb数据库
			dialects = 2048;
		} else if (type == 15) {// 神通数据库
			dialects = 4096;
		} else if (type == 18) {// Gbase8t数据库
			dialects = 8192;
		}
		Object[] insertArgs = { dbId, name, dialects, desc, issox, ispci, charset, isglba, version, type };

		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), insertSql, insertArgs);
		this.getJdbcUtils().execute(conn, insertSql, insertArgs);
		return dbId;
	}

	public void addAddress(String address, int port, String serviceName, int databaseId) {
		String checkSql = "SELECT COUNT(*) FROM dbfw.database_addresses WHERE address=? AND port=?";
		String genIdSql = "SELECT IFNULL(MAX(address_id)+1,1) FROM dbfw.database_addresses";
		String sql = "INSERT INTO dbfw.database_addresses(address_id,address,port,service_name,database_id) VALUES(?,?,?,?,?)";
		Object[] checkArgs = { address, port };

		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), checkSql, checkArgs);
		int cnt = this.getJdbcUtils().queryForInteger(checkSql, checkArgs);
		if (cnt > 0) {
			throw new DAOException("该IP、端口对应的数据库已经被注册");
		}

		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), genIdSql, null);
		int addressId = this.getJdbcUtils().queryForInteger(genIdSql);
		Object[] args = { addressId, address, port, serviceName, databaseId };

		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql, args);
		this.getJdbcUtils().execute(sql, args);
	}

	public boolean checkRemoteDBFWAddress(Connection conn, int portId) {
		String sql = "SELECT COUNT(*) FROM dbfw.dbfw_fordb t1 WHERE t1.port_id=? AND t1.isdelete='0' AND t1.address_id IS NOT NULL";
		Object[] args = { portId };
		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql, args);
		int cnt = this.getJdbcUtils().queryForInteger(conn, sql, args);
		if (cnt > 0) {
			return false;
		} else {
			return true;
		}
	}

	public boolean checkBywayDBFWAddress(Connection conn, int dbfwId, int groupId) {
		String sql = "SELECT COUNT(id) FROM (SELECT group_id AS id FROM dbfw.dbfw_fordb t1, dbfw.npc_info t2 WHERE t1.dbfw_inst_id=? "
				+ "AND t2.group_id=? AND t2.dbfw_inst_id = t1.dbfw_inst_id AND t2.address_id = t1.address_id AND t2.database_id = t1.database_id AND t1.isdelete='0' GROUP BY group_id) t_cnt";
		/*
		 * "SELECT COUNT(id) FROM (SELECT group_id AS id FROM dbfw.dbfw_fordb t1, dbfw.npc_info t2 WHERE t1.dbfw_inst_id=? "
		 * +
		 * "AND t2.group_id=? AND t1.npc_info_id=t2.id AND t1.isdelete='0' GROUP BY group_id) t_cnt"
		 * ;
		 */
		Object[] args = { dbfwId, groupId };
		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql, args);
		int cnt = this.getJdbcUtils().queryForInteger(conn, sql, args);
		if (cnt < DBFWConstant.MAX_NPC_PER_INST) {
			return true;
		} else {
			return false;
		}
	}

	public boolean checkNativeDBFWAddress(Connection conn, int portId) {
		String sql = "SELECT COUNT(*) FROM dbfw.dbfw_fordb t1 WHERE t1.port_id=? AND t1.isdelete='0' AND t1.address_id IS NOT NULL";
		Object[] args = { portId };
		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql, args);
		int cnt = this.getJdbcUtils().queryForInteger(conn, sql, args);
		if (cnt > 0) {
			return false;
		} else {
			return true;
		}
	}

	public boolean checkHalftransDBFWAddress(Connection conn, int addressId, int portId) {
		String sql = "SELECT count(*) " + "FROM dbfw.database_addresses t1, dbfw.dbfw_fordb t2 "
				+ "WHERE t2.monitor_type=? AND (t1.address,t1.port) IN "
				+ "(SELECT address,port FROM dbfw.database_addresses WHERE address_id=?) "
				+ "AND t2.port_id!=? AND t1.address_id=t2.address_id AND t2.isdelete='0' AND t2.address_id IS NOT NULL";
		Object[] args = { DBFWConstant.DEPLOY_HALFTRANS, addressId, portId };
		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql, args);
		int cnt = this.getJdbcUtils().queryForInteger(conn, sql, args);
		if (cnt > 0) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * 获取当前有多少IP端口,用来判断License
	 * 
	 * @return
	 */
	public int getDBAddressCount() {
		int dbcount = 0;
		String sql = "select count(*) from database_addresses";
		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql, null);
		dbcount = this.getJdbcUtils().queryForInteger(sql);
		return dbcount;
	}

	/**
	 * 是否能添加数据库IP端口
	 * 
	 * @param count
	 * @return
	 */
	public boolean canAddDatabaseIp(int count) {
		int limitDB = -1;
		int nowDB = getDBAddressCount() + count;
		try {
			limitDB = SystemLicenseSrever.getLicenseInfoFinal().getDbcount();
			if (nowDB <= limitDB) {
				return true;
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		return false;
	}

	/**
	 * 检测网桥组ID是否在其他安全实例下使用
	 * 
	 * @param conn
	 * @param dbfw_inst_id
	 * @param groupId
	 * @return
	 */
	public boolean checkHalftransDBFWAGroupId(Connection conn, int dbfw_inst_id, String groupId) {
		boolean type = true;
		String sql = "select distinct dbfw_inst_id from  npc_info where group_id=? and dbfw_inst_id<>?";
		Object[] args = { groupId, dbfw_inst_id };
		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql, args);
		int cnt = this.getJdbcUtils().queryForInteger(conn, sql, args);
		if (cnt > 0) {
			type = false;
		} else {
			type = true;
		}
		return type;
	}

	public boolean checkHalftransDBFWAddress_update(Connection conn, int addressId, int portId, int dbfwId) {
		int cnt = 0;
		boolean isbridge = true;
		String sql1 = "SELECT monitor_type AS monitorType,port_id AS portId " + "FROM `dbfw_fordb`  "
				+ "WHERE isdelete='0' AND address_id IS NOT NULL "
				+ "AND address_id = ?  AND dbfw_inst_id=? ORDER BY monitor_type ";
		Object[] args1 = { addressId, dbfwId };
		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql1, args1);
		List<DBFWForDbAddr> ret = (List<DBFWForDbAddr>) this.getJdbcUtils().query(conn, sql1, args1,
				DBFWForDbAddr.class);
		for (DBFWForDbAddr old : ret) {
			if (old.getMonitorType() != 4) // 如果原来不是网桥模式
			{
				String sql2 = "SELECT count(*) " + "FROM dbfw.dbfw_fordb t1 "
						+ "WHERE t1.monitor_type=? AND t1.address_id=?  AND t1.isdelete='0' ";
				Object[] args2 = { DBFWConstant.DEPLOY_HALFTRANS, addressId };
				this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql2, args2);
				cnt = this.getJdbcUtils().queryForInteger(conn, sql2, args2); // 判断这个地址是否存在网桥模式
				if (cnt > 0) {
					return false;
				}
			} else {
				return true;
			}
		}
		return isbridge;
	}

	public void updateDatabaseInfo(String desc, int version, int id, int charset, String issox, String ispci,
			String isglba) {
		List<NativeCommand> flushCommands = new ArrayList<NativeCommand>();

		String sql = "UPDATE xsec_databases SET description=?, db_version=?, is_dpa=?,is_sox=?,is_pci=?,is_glba=? WHERE database_id=?";
		Object[] args = { desc, version, charset, issox, ispci, isglba, id };
		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql, args);
		this.getJdbcUtils().execute(sql, args);

		// 修改数据库无连接会话后刷新，但不能单独刷新，只能有修改就刷新 fjw20140627>>start>>>
		String sql_dbfw = "SELECT t2.address AS address,t2.`port` AS port  "
				+ "FROM dbfw_fordb t1 ,dbfw_instances t2  " + "WHERE t1.database_id=?   " + "AND t1.isdelete=0   "
				+ "AND t1.address_id IS NULL   " + "AND t1.dbfw_inst_id=t2.instance_id  " + "AND t2.inst_stat<>3";
		Object[] args_dbfw = { id };
		List<DBFWInfo> dbfwinfolist = (List<DBFWInfo>) this.getJdbcUtils().query(sql_dbfw, args_dbfw, DBFWInfo.class);
		for (DBFWInfo dbfwInfo : dbfwinfolist) {
			NativeCommand command = new FlushDbfwForDbInfoCommand(dbfwInfo.getAddress(), dbfwInfo.getPort(), id);
			flushCommands.add(command);
		}
		NativeExecutor.execute(flushCommands);
		// 修改数据库无连接会话后刷新，但不能单独刷新，只能有修改就刷新 fjw20140627<<<end<<<<
	}

	private InterfaceGroup getGroupById(Connection conn, int id) {
		String sql = "SELECT t1.group_id AS groupId, t1.group_name AS groupName, t1.group_type AS groupType, t1.group_ip AS groupIp, t1.group_netmask AS groupMask, t1.group_mac AS groupMac, t1.gateway AS groupGateway, t1.group_enable AS groupEnable "
				+ "FROM interface_group t1 " + "WHERE group_id=?";
		Object[] args = { id };
		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql, args);
		return (InterfaceGroup) this.getJdbcUtils().queryForObject(conn, sql, args, InterfaceGroup.class);
	}

	public synchronized void addDatabaseAddress(DatabaseAddress address, List<DBFWForDbAddr> dbfwForAddrList,
			List<NativeCommand> commands) {
		Connection conn = null;
		int addressId = 0;
		NativeCommand command = null;
		List<NativeCommand> flushCommands = new ArrayList<NativeCommand>();

		// 重新加载管理IP端口
		Globals.reloadConfigs();
		try {
			conn = this.getJdbcUtils().getConnection();
			this.getJdbcUtils().beginTransaction(conn);
			// 判断是否能增加数据库地址配置
			judgeAddDatabases(1);
			// PHASE 1. 向dbfw.database_addresses表中插入数据
			addressId = addAddressAndGetId(conn, address.getAddress(), address.getPort(), address.getServiceName(),
					address.getDatabaseId(), address.getUserName(), address.getUserPwd(), address.getDynaPort(), 0);

			for (DBFWForDbAddr dbfwAddr : dbfwForAddrList) {
				// 检查dbfwId安全实例下是否已经存在非旁路的部署模式
				// boolean deployModeMatch = this.deployModeMatch(conn,
				// dbfwAddr.getDbfwId(), dbfwAddr.getMonitorType());
				// if (!deployModeMatch) {
				// if (dbfwAddr.getMonitorType() == 2)
				// throw new ServiceException("该安全实例下已经存在非旁路部署模式，不能添加旁路部署模式");
				// else
				// throw new ServiceException("该安全实例下已经存在旁路部署模式，不能添加非旁路部署模式");
				// }

				int port = this.getPortNumById(conn, dbfwAddr.getPortId());
				DBFWInfo dbfwInfo = this.getDBFWInfoById(conn, dbfwAddr.getDbfwId());
				String ifNameVal = getIfName(dbfwAddr.getGroupId());
				if (dbfwAddr.getMonitorType() != DBFWConstant.DEPLOY_BYWAY) {
					command = new CreateDBCommand(
							(dbfwAddr.getMonitorType() == DBFWConstant.DEPLOY_HALFTRANS ? ("br" + dbfwAddr.getGroupId())
									: ifNameVal), dbfwAddr.getMonitorType(), address.getAddress(), address.getPort(),
							port, dbfwAddr.getGroupId(), "");
					commands.add(command);

					InterfaceGroup group = getGroupById(conn, dbfwAddr.getGroupId());
					if (group == null) {
						throw new ServiceException("无法在策略中心中找到组：" + dbfwAddr.getGroupName());
					}
					command = new CreateNewNPlsCommand(dbfwInfo.getAddress(), dbfwInfo.getPort(), port, address
							.getAddress(), address.getPort(), dbfwAddr.getMonitorType(), group.getGroupIp());
					commands.add(command);
				}

				// PHASE 1. 添加至dbfw_fordb
				databaseConfigDAO.addDbfwForDbAddr(conn, dbfwAddr.getDbfwId(), addressId, dbfwAddr.getMonitorType(),
						dbfwAddr.getClientGroupId());

				// PHASE 2. 根据不同部署模式
				switch (dbfwAddr.getMonitorType()) {
				case DBFWConstant.DEPLOY_REMOTEPROXY:
					databaseConfigDAO.addRemoteDbfwForAddrInfo(conn, dbfwAddr.getRemoteHost(),
							dbfwAddr.getRemotePort(), dbfwAddr.getDbfwId(), addressId, dbfwAddr.getPortId());
					break;
				case DBFWConstant.DEPLOY_BYWAY:
					List<InterfaceInfo> interfaces = this.getInterfaceByGroup(dbfwAddr.getGroupId());
					databaseConfigDAO.addBywayDbfwForAddrInfo(conn, dbfwAddr.getGroupId(), dbfwAddr.getDbfwId(),
							addressId, dbfwInfo, interfaces.get(0), commands, flushCommands);
					break;
				case DBFWConstant.DEPLOY_NATIVEPROXY:
					databaseConfigDAO.addNativeDbfwForAddrInfo(conn, dbfwAddr.getDbfwId(), addressId, dbfwAddr
							.getPortId());
					break;
				case DBFWConstant.DEPLOY_HALFTRANS:
					databaseConfigDAO.addHalfTransDbfwForAddrInfo(conn, dbfwAddr.getDbfwId(), addressId, dbfwAddr
							.getPortId());
					// command = new
					// CreateRedirectNplsPortCommand(address.getAddress(),
					// address.getPort(), port, dbfwAddr
					// .getGroupId());
					// commands.add(command);
					break;
				default:
					break;
				}

				// PHASE 3. 刷新SMON
				int dbId = this.getDatabaseAddressesById(conn, addressId).getDatabaseId();
				command = new FlushDbfwForDbInfoCommand(dbfwInfo.getAddress(), dbfwInfo.getPort(), dbId);
				flushCommands.add(command);
			}

			NativeExecutor.execute(commands);
			this.getJdbcUtils().commit(conn);
		} catch (RunException ex) {
			this.getJdbcUtils().rollback(conn);
			throw ex;
		} finally {
			this.getJdbcUtils().close(conn);
		}

		NativeExecutor.execute(flushCommands);
	}

	private int addAddressAndGetId(Connection conn, String address, int port, String serviceName, int databaseId,
			String username, String passwd, int dynaPort, int times) {
		String checkSql = "SELECT COUNT(*) FROM dbfw.database_addresses WHERE address=? AND port=?";
		String checkCNTSql = "SELECT COUNT(*) FROM dbfw.database_addresses WHERE database_id=?";
		String genIdSql = "SELECT IFNULL(MAX(address_id)+1,1) FROM dbfw.database_addresses";
		String insertSql = "INSERT INTO dbfw.database_addresses(address_id,address,port,service_name,database_id,db_username,db_passwd,dyna_port)"
				+ "VALUES(?,?,?,?,?,?,?,?)";
		String querySql = "SELECT LAST_INSERT_ID()";
		Object[] checkArgs = { address, port };
		Object[] checkcntArgs = { databaseId };

		this
				.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), checkCNTSql,
						checkcntArgs);
		int cnt1 = this.getJdbcUtils().queryForInteger(conn, checkCNTSql, checkcntArgs);
		if (cnt1 >= 8) {
			throw new DAOException("数据库地址信息最多只能有8条,请确认");
		}

		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), checkSql, checkArgs);
		int cnt = this.getJdbcUtils().queryForInteger(conn, checkSql, checkArgs);
		if (cnt > 0) {
			throw new DAOException("该IP、端口的数据库已经被添加");
		}

		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), genIdSql, null);
		int addressId = this.getJdbcUtils().queryForInteger(genIdSql) + times;
		Object[] insertArgs = { addressId, address, port, serviceName, databaseId, username, passwd, dynaPort };

		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), insertSql, insertArgs);
		this.getJdbcUtils().execute(conn, insertSql, insertArgs);

		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), querySql, null);
		return this.getJdbcUtils().queryForInteger(conn, querySql);
	}

	public void removeAddress(int addressId, List<NativeCommand> commands) {
		// 重新加载管理IP端口
		Globals.reloadConfigs();

		Connection conn = null;
		NativeCommand command = null;
		List<NativeCommand> flushCommands = new ArrayList<NativeCommand>();
		boolean npcDelete = false;
		List<NpcInfos> npcStartVal = null;
		int montiType = 0;
		int dbfwId = 0;
		try {
			conn = this.getJdbcUtils().getConnection();
			this.getJdbcUtils().beginTransaction(conn);

			DatabaseAddress address = this.getDatabaseAddressesById(conn, addressId);
			List<DBFWForDbAddr> dbfwForDbAddrList = getDBFWForDbAddrByAddress(conn, addressId);
			if (dbfwForDbAddrList.size() > 0) {
				String npcIdQuery = "SELECT DISTINCT npc_id AS npcid,group_id AS groupid FROM `npc_info` WHERE dbfw_inst_id=?";
				// int dbid=getDbIdByDbAddrID(conn,
				// dbfwForDbAddrList.get(0).getAddressId());
				dbfwId = dbfwForDbAddrList.get(0).getDbfwId();
				npcStartVal = (List<NpcInfos>) this.getJdbcUtils().query(conn, npcIdQuery,
						new Object[] { dbfwForDbAddrList.get(0).getDbfwId() }, NpcInfos.class);
				for (DBFWForDbAddr dbfwForDbAddr : dbfwForDbAddrList) {
					int port = this.getPortNumById(conn, dbfwForDbAddr.getPortId());
					DBFWInfo dbfwInfo = this.getDBFWInfoById(conn, dbfwForDbAddr.getDbfwId());
					String ifNameVal = getIfName(dbfwForDbAddr.getGroupId());
					if (dbfwForDbAddr.getMonitorType() != DBFWConstant.DEPLOY_BYWAY
							&& dbfwForDbAddr.getMonitorType() != DBFWConstant.DEPLOY_HALFTRANS) {
						command = new DeleteDBCommand(
								(dbfwForDbAddr.getMonitorType() == DBFWConstant.DEPLOY_HALFTRANS ? ("br" + dbfwForDbAddr
										.getGroupId())
										: ifNameVal), dbfwForDbAddr.getMonitorType(), address.getAddress(), address
										.getPort(), port, dbfwForDbAddr.getGroupId(), "");
						commands.add(command);

						InterfaceGroup group = getGroupById(conn, dbfwForDbAddr.getGroupId());
						if (group == null) {
							throw new ServiceException("无法在策略中心中找到组：" + dbfwForDbAddr.getGroupName());
						}
						command = new KillNplsCommand(dbfwInfo.getAddress(), dbfwInfo.getPort(), port, address
								.getAddress(), address.getPort(), dbfwForDbAddr.getMonitorType(), group.getGroupIp());
						commands.add(command);
					}

					switch (dbfwForDbAddr.getMonitorType()) {
					case DBFWConstant.DEPLOY_REMOTEPROXY:
						removeRemoteProxyAndDbfwForAddr(conn, dbfwForDbAddr.getRemoteId(), dbfwForDbAddr.getPortId(),
								addressId);
						break;
					case DBFWConstant.DEPLOY_BYWAY:
						List<InterfaceInfo> interfaces = this.getInterfaceByGroup(dbfwForDbAddr.getGroupId());
						npcDelete = removeBywayAndDbfwForAddr(conn, dbfwForDbAddr, addressId);
						montiType = 2;
						/*
						 * if (npcDelete) { command = new
						 * KillNpcCommand(dbfwInfo.getAddress(),
						 * dbfwInfo.getPort(), dbfwForDbAddr.getNpcNum(),
						 * interfaces.get(0).getIfName());
						 * commands.add(command); } else { command = new
						 * FlushNpcInfoCommand(dbfwInfo.getAddress(),
						 * dbfwInfo.getPort(), dbfwForDbAddr.getDbfwId(),
						 * dbfwForDbAddr.getNpcNum());
						 * flushCommands.add(command); }
						 */
						break;
					case DBFWConstant.DEPLOY_NATIVEPROXY:
						removeNativeProxyAndDbfwForAddr(conn, dbfwForDbAddr.getPortId(), addressId);
						break;
					case DBFWConstant.DEPLOY_HALFTRANS:
						removeHalfTransAndDbfwForAddr(conn, dbfwForDbAddr.getPortId(), addressId);
						montiType = DBFWConstant.DEPLOY_HALFTRANS;
						break;
					default:
						break;
					}
					removeDbfwForDbAddr(conn, dbfwForDbAddr.getDbfwId(), addressId);

					// PHASE 3. 刷新SMON
					int dbId = this.getDatabaseAddressesById(conn, addressId).getDatabaseId();
					command = new FlushDbfwForDbInfoCommand(dbfwInfo.getAddress(), dbfwInfo.getPort(), dbId);
					flushCommands.add(command);
				}
			}
			removeDatabaseAddress(conn, addressId);

			this.getJdbcUtils().commit(conn);
			// 接口调用重新调整旁路、网桥修改为相同的调用
			if (montiType == DBFWConstant.DEPLOY_HALFTRANS || montiType == DBFWConstant.DEPLOY_BYWAY) {
				DBFWInfo dbfwInfo = this.getDBFWInfoById(conn, dbfwId);
				command = new FlushNfwCommand(dbfwInfo.getAddress(), dbfwInfo.getPort(), dbfwId);
				commands.add(command);
			}
			NativeExecutor.execute(commands);
		} catch (RunException ex) {
			this.getJdbcUtils().rollback(conn);
			throw ex;
		} finally {
			this.getJdbcUtils().close(conn);
		}

		NativeExecutor.execute(flushCommands);
	}

	private int getPortNumById(Connection conn, int portId) {
		String sql = "SELECT port_num FROM dbfw.group_port WHERE port_id=?";
		Object[] args = { portId };
		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql, args);
		return this.getJdbcUtils().queryForInteger(conn, sql, args);
	}

	private void removeDatabaseAddress(Connection conn, int addressId) {
		String sql = "DELETE FROM dbfw.database_addresses WHERE address_id=?";
		Object[] args = { addressId };
		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql, args);
		this.getJdbcUtils().execute(conn, sql, args);
	}

	@SuppressWarnings("unchecked")
	public List<DBFWForDbAddr> getDbfwForDbAddrs(int dbfwId, int dbId) {
		String sql = "SELECT DISTINCT t1.address_id AS addressId, t1.dbfw_inst_id AS dbfwId, t2.name AS dbfwName, t1.state AS state, t1.monitor_type AS monitorType, CASE WHEN t1.monitor_type=2 THEN t5.group_id ELSE t3.group_id END AS groupId, CASE WHEN t1.monitor_type=2 THEN t8.group_name ELSE t4.group_name END  AS groupName, IFNULL(t3.port_id,0) AS portId, "
				+ "IFNULL(t3.port_name,'') AS portName, t1.npc_info_id AS npcInfoId, IFNULL(t5.npc_id,0) AS npcNum, t1.client_group_id AS clientGroupId, IFNULL(t6.name,'') AS clientGroupName, "
				+ "t1.remote_address_id AS remoteId, IFNULL(t7.host,'') AS remoteHost, IFNULL(t7.port,0) AS remotePort "
				+ "FROM dbfw.dbfw_fordb t1 INNER JOIN dbfw.dbfw_instances t2 ON t1.dbfw_inst_id=t2.instance_id AND t1.isdelete='0' AND t2.isdelete='0' "
				+ "LEFT JOIN dbfw.group_port t3 ON t1.port_id=t3.port_id "
				+ "LEFT JOIN interface_group t4 ON t3.group_id=t4.group_id "
				+ "LEFT JOIN dbfw.npc_info t5 ON t5.dbfw_inst_id = t1.dbfw_inst_id AND t5.address_id = t1.address_id AND t5.database_id = t1.database_id "
				+
				// "LEFT JOIN dbfw.npc_info t5 ON t1.npc_info_id=t5.id " +
				"LEFT JOIN interface_group t8 ON t5.group_id=t8.group_id "
				+ "LEFT JOIN dbfw.client_group_info t6 ON t1.client_group_id=t6.id "
				+ "LEFT JOIN dbfw.remote_proxy t7 ON t1.remote_address_id=t7.id "
				+ "WHERE t2.instance_id=? AND t1.address_id IN "
				+ "("
				+ "SELECT address_id FROM dbfw.database_addresses WHERE database_id=? " + ") " + "ORDER BY addressId";
		Object[] args = { dbfwId, dbId };
		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql, args);
		return (List<DBFWForDbAddr>) this.getJdbcUtils().query(sql, args, DBFWForDbAddr.class);
	}

	@SuppressWarnings("unchecked")
	public List<dbversion> getDbversions() {
		String sql = "SELECT t1.dbkind AS dbkind, t1.dbversion AS dbversion, t1.dbverval AS dbverval  "
				+ "FROM dbfw.dbversion t1 " + "ORDER BY dbkind asc,dbverval asc";
		Object[] args = {};
		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql, args);
		return (List<dbversion>) this.getJdbcUtils().query(sql, dbversion.class);
	}

	public void removeDbfwForDb(int dbfwId, int dbId, List<NativeCommand> commands) {
		// 重新加载管理IP端口
		Globals.reloadConfigs();

		NativeCommand command = null;
		Connection conn = null;
		List<NativeCommand> flushCommands = new ArrayList<NativeCommand>();
		List<NpcInfos> npcStartVal = null;
		boolean npcDelete = false;
		int montiType = 0;
		DBFWInfo dbfwInfo = null;
		try {
			conn = this.getJdbcUtils().getConnection();
			this.getJdbcUtils().beginTransaction(conn);
			String npcIdQuery = "SELECT DISTINCT npc_id AS npcid,group_id AS groupid FROM `npc_info` WHERE dbfw_inst_id=?";
			// int dbid=getDbIdByDbAddrID(conn,
			// dbfwForDbAddrList.get(0).getAddressId());
			npcStartVal = (List<NpcInfos>) this.getJdbcUtils().query(conn, npcIdQuery, new Object[] { dbfwId },
					NpcInfos.class);
			List<DatabaseAddress> addresses = this.getDatabaseAddresses(conn, dbId);

			for (DatabaseAddress address : addresses) {
				// DBFWForDbAddr dbfwForDbAddr =
				// databaseConfigDAO.getDBFWForDbAddrByAddressAndDbfw(conn,
				// address.getId(), dbfwId);
				List<DBFWForDbAddr> dbfwForDbAddr = databaseConfigDAO.getDBFWForDbAddrByAddressAndDbfwList(conn,
						address.getId(), dbfwId);
				if (dbfwForDbAddr == null || dbfwForDbAddr.size() == 0) {
					continue;
				}
				int port = this.getPortNumById(conn, dbfwForDbAddr.get(0).getPortId());
				dbfwInfo = this.getDBFWInfoById(conn, dbfwForDbAddr.get(0).getDbfwId());

				if (dbfwForDbAddr.get(0).getMonitorType() != DBFWConstant.DEPLOY_BYWAY
						&& dbfwForDbAddr.get(0).getMonitorType() != DBFWConstant.DEPLOY_HALFTRANS) {
					// String rcntSql = "SELECT COUNT(*) " +
					// "FROM dbfw_fordb t1,group_port t2 "
					// + "WHERE t1.address_id=? " +
					// "AND t1.port_id = t2.port_id " + "AND t2.group_id=? "
					// + "AND t1.isdelete='0' ";
					// 删除静态路由判断，多个IP不同端口静态路由删除不正确问题
					String rcntSql = "SELECT COUNT(*) FROM dbfw_fordb t1,group_port t2 ,database_addresses t3"
							+ " WHERE  t1.port_id = t2.port_id  "
							+ " AND t1.isdelete='0' AND t3.address_id=t1.address_id AND t3.address=(SELECT address FROM "
							+ " database_addresses WHERE address_id=?) AND t2.group_id=?";
					Object[] rcntArgs = { address.getId(), dbfwForDbAddr.get(0).getGroupId() };
					this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), rcntSql,
							rcntArgs);
					int cnt = this.getJdbcUtils().queryForInteger(conn, rcntSql, rcntArgs);
					String ifNameVal = getIfName(dbfwForDbAddr.get(0).getGroupId());
					if (cnt < 2) {
						command = new DeleteDBCommand(
								(dbfwForDbAddr.get(0).getMonitorType() == DBFWConstant.DEPLOY_HALFTRANS ? ("br" + dbfwForDbAddr
										.get(0).getGroupId())
										: ifNameVal), dbfwForDbAddr.get(0).getMonitorType(), address.getAddress(),
								address.getPort(), port, dbfwForDbAddr.get(0).getGroupId(), "");
						commands.add(command);
					}
					InterfaceGroup group = getGroupById(conn, dbfwForDbAddr.get(0).getGroupId());
					if (group == null) {
						throw new ServiceException("无法在策略中心中找到组：" + dbfwForDbAddr.get(0).getGroupName());
					}
					command = new KillNplsCommand(dbfwInfo.getAddress(), dbfwInfo.getPort(), port,
							address.getAddress(), address.getPort(), dbfwForDbAddr.get(0).getMonitorType(), group
									.getGroupIp());
					commands.add(command);
				}

				switch (dbfwForDbAddr.get(0).getMonitorType()) {
				case DBFWConstant.DEPLOY_REMOTEPROXY:
					databaseConfigDAO.removeRemoteProxyAndDbfwForAddr(conn, dbfwForDbAddr.get(0).getRemoteId(),
							dbfwForDbAddr.get(0).getPortId(), dbfwId, address.getId());
					break;
				case DBFWConstant.DEPLOY_BYWAY:
					npcDelete = databaseConfigDAO
							.removeBywayAndDbfwForAddr(conn, dbfwForDbAddr.get(0), address.getId());
					montiType = 2;
					/*
					 * for(int i=0;i<dbfwForDbAddr.size();i++){
					 * List<InterfaceInfo> interfaces =
					 * this.getInterfaceByGroup(
					 * dbfwForDbAddr.get(i).getGroupId()); if (npcDelete) {
					 * boolean
					 * npcSuccess=brushOrKillNpc(dbfwId,dbfwForDbAddr.get
					 * (i).getNpcNum()); if(npcSuccess){ command = new
					 * FlushNpcInfoCommand(dbfwInfo.getAddress(),
					 * dbfwInfo.getPort(), dbfwForDbAddr.get(i).getDbfwId(),
					 * dbfwForDbAddr.get(i).getNpcNum());
					 * flushCommands.add(command); }else{ command = new
					 * KillNpcCommand(dbfwInfo.getAddress(), dbfwInfo.getPort(),
					 * dbfwForDbAddr.get(i).getNpcNum(),
					 * interfaces.get(0).getGroupName()); commands.add(command);
					 * } } else { command = new
					 * FlushNpcInfoCommand(dbfwInfo.getAddress(),
					 * dbfwInfo.getPort(), dbfwForDbAddr.get(i).getDbfwId(),
					 * dbfwForDbAddr.get(i).getNpcNum());
					 * flushCommands.add(command); } }
					 */
					break;
				case DBFWConstant.DEPLOY_NATIVEPROXY:
					databaseConfigDAO.removeNativeProxyAndDbfwForAddr(conn, dbfwForDbAddr.get(0).getPortId(), dbfwId,
							address.getId());
					break;
				case DBFWConstant.DEPLOY_HALFTRANS:
					/* 网桥端口，在数据库和安全实例关联时自动生成11000～15000，取消关联是删除记录 */
					// String port_num =
					// "SELECT port_num FROM group_port WHERE port_id="
					// + dbfwForDbAddr.get(0).getPortId();
					// command = new DeleteNplsPortCommand("br" +
					// dbfwForDbAddr.get(0).getGroupId(), this.getJdbcUtils()
					// .queryForInteger(conn, port_num),
					// dbfwForDbAddr.get(0).getGroupId());
					// commands.add(command);

					databaseConfigDAO.removeHalfTransAndDbfwForAddr(conn, dbfwForDbAddr.get(0).getPortId(), dbfwId,
							address.getId());
					// command = new
					// DeleteRedirectNplsPortCommand(address.getAddress(),
					// address.getPort(), port,
					// dbfwForDbAddr.get(0).getGroupId());
					// commands.add(command);

					montiType = DBFWConstant.DEPLOY_HALFTRANS;
					break;
				default:
					break;
				}
				databaseConfigDAO.removeDbfwForDbAddr(conn, dbfwId, address.getId());

				// PHASE 3. 刷新SMON
				command = new FlushDbfwForDbInfoCommand(dbfwInfo.getAddress(), dbfwInfo.getPort(), dbId);
				flushCommands.add(command);
			}
			// 解除关系时将dbfw_fordb中所有相应address_id不为空的记录删除，为空的记录isdelete置1
			// (fjw20130821)
			String sql = "UPDATE dbfw.dbfw_fordb SET isdelete='1' WHERE dbfw_inst_id=? AND database_id=? AND address_id IS NULL AND isdelete='0'";
			Object[] args = { dbfwId, dbId };
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql, args);
			this.getJdbcUtils().execute(conn, sql, args);
			// 清除DBFW-DB的数据信息
			this.deinitDbfwDb(conn, dbfwId, dbId);
			String status_sql = "SELECT IFNULL(inst_stat,0) FROM `dbfw_instances` t1  WHERE t1.isdelete=0 AND t1.instance_id= "
					+ dbfwId;
			int dbfwstatus = this.getJdbcUtils().queryForInteger(conn, status_sql);
			this.getJdbcUtils().commit(conn);

			// 接口调用重新调整旁路、网桥修改为相同的调用
			if (montiType == DBFWConstant.DEPLOY_HALFTRANS || montiType == DBFWConstant.DEPLOY_BYWAY) {
				command = new FlushNfwCommand(dbfwInfo.getAddress(), dbfwInfo.getPort(), dbfwId);
				commands.add(command);
			}

			if (dbfwstatus != 3) {
				try {
					NativeExecutor.execute(commands);
				} catch (RunException e) {
					if (dbfwInfo != null && dbfwInfo.getInstStat() == 3) {
						e = new RunException("安全实例未启动，请刷新页面，启动安全实例后操作生效！");
					}
					throw e;
				}
			}
		} catch (RunException ex) {
			this.getJdbcUtils().rollback(conn);
			throw ex;
		} finally {
			this.getJdbcUtils().close(conn);
		}
		try {
			NativeExecutor.execute(flushCommands);
		} catch (RunException e) {
			if (dbfwInfo != null && dbfwInfo.getInstStat() == 3) {
				e = new RunException("安全实例未启动，请刷新页面，启动安全实例后操作生效！");
			}
			throw e;
		}
	}

	public void removeDatabase(int dbId, List<NativeCommand> commands) {
		// 重新加载管理IP端口
		Globals.reloadConfigs();

		NativeCommand command = null;
		Connection conn = null;
		List<NativeCommand> flushCommands = new ArrayList<NativeCommand>();
		boolean npcDelete = false;

		try {
			conn = this.getJdbcUtils().getConnection();
			this.getJdbcUtils().beginTransaction(conn);

			List<DatabaseAddress> addresses = this.getDatabaseAddresses(conn, dbId);
			List<NoSession> sessionList = getNoSessionList(dbId);
			for (DatabaseAddress address : addresses) {
				List<DBFWForDbAddr> dbfwForDbAddrList = getDBFWForDbAddrByAddress(conn, address.getId());
				if (dbfwForDbAddrList.size() > 0) {
					for (DBFWForDbAddr dbfwForDbAddr : dbfwForDbAddrList) {
						int port = this.getPortNumById(conn, dbfwForDbAddr.getPortId());
						DBFWInfo dbfwInfo = this.getDBFWInfoById(conn, dbfwForDbAddr.getDbfwId());
						String ifNameVal = getIfName(dbfwForDbAddr.getGroupId());
						if (dbfwForDbAddr.getMonitorType() != DBFWConstant.DEPLOY_BYWAY) {
							command = new DeleteDBCommand(
									(dbfwForDbAddr.getMonitorType() == DBFWConstant.DEPLOY_HALFTRANS ? ("br" + dbfwForDbAddr
											.getGroupId())
											: ifNameVal), dbfwForDbAddr.getMonitorType(), address.getAddress(), address
											.getPort(), port, dbfwForDbAddr.getGroupId(), "");
							commands.add(command);

							InterfaceGroup group = getGroupById(conn, dbfwForDbAddr.getGroupId());
							if (group == null) {
								throw new ServiceException("无法在策略中心中找到组：" + dbfwForDbAddr.getGroupName());
							}
							command = new KillNplsCommand(dbfwInfo.getAddress(), dbfwInfo.getPort(), port, address
									.getAddress(), address.getPort(), dbfwForDbAddr.getMonitorType(), group
									.getGroupIp());
							commands.add(command);
						}

						switch (dbfwForDbAddr.getMonitorType()) {
						case DBFWConstant.DEPLOY_REMOTEPROXY:
							removeRemoteProxyAndDbfwForAddr(conn, dbfwForDbAddr.getRemoteId(), dbfwForDbAddr
									.getPortId(), address.getId());
							break;
						case DBFWConstant.DEPLOY_BYWAY:
							List<InterfaceInfo> interfaces = this.getInterfaceByGroup(dbfwForDbAddr.getGroupId());
							npcDelete = removeBywayAndDbfwForAddr(conn, dbfwForDbAddr.getNpcInfoId(), address.getId());
							command = new FlushNfwCommand(dbfwInfo.getAddress(), dbfwInfo.getPort(), dbfwInfo.getId());
							commands.add(command);
							break;
						case DBFWConstant.DEPLOY_NATIVEPROXY:
							removeNativeProxyAndDbfwForAddr(conn, dbfwForDbAddr.getPortId(), address.getId());
							break;
						case DBFWConstant.DEPLOY_HALFTRANS:
							// removeHalfTransAndDbfwForAddr(conn,
							// dbfwForDbAddr.getPortId(), address.getId());
							// command = new
							// DeleteRedirectNplsPortCommand(address.getAddress(),
							// address.getPort(), port,
							// dbfwForDbAddr.getGroupId());
							// commands.add(command);
							npcDelete = removeBywayAndDbfwForAddr(conn, dbfwForDbAddr.getNpcInfoId(), address.getId());
							command = new FlushNfwCommand(dbfwInfo.getAddress(), dbfwInfo.getPort(), dbfwInfo.getId());
							commands.add(command);
							break;
						default:
							break;
						}

						removeDbfwForDbAddr(conn, dbfwForDbAddr.getDbfwId(), address.getId());

						// PHASE 3. 刷新SMON
						command = new FlushDbfwForDbInfoCommand(dbfwInfo.getAddress(), dbfwInfo.getPort(), dbId);
						flushCommands.add(command);
					}
				}
				removeDatabaseAddress(conn, address.getId());
			}
			removeXsecDatabase(conn, dbId);
			for (NoSession nos : sessionList) {
				removeNoSession(nos.getId());
			}
			// 清除DB的数据信息
			this.deinitDb(conn, dbId);

			NativeExecutor.execute(commands);
			this.getJdbcUtils().commit(conn);
		} catch (RunException ex) {
			this.getJdbcUtils().rollback(conn);
			throw ex;
		} finally {
			this.getJdbcUtils().close(conn);
		}

		NativeExecutor.execute(flushCommands);
	}

	private void removeXsecDatabase(Connection conn, int dbId) {
		String sql = "DELETE FROM xsec_databases WHERE database_id=?";
		Object[] args = { dbId };
		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql, args);
		this.getJdbcUtils().execute(conn, sql, args);
	}

	public synchronized void addDbfwForDbAddrs(List<DBFWForDbAddr> list, List<NativeCommand> commands) {
		// 重新加载管理IP端口
		Globals.reloadConfigs();

		NativeCommand command = null;
		Connection conn = null;
		List<NativeCommand> flushCommands = new ArrayList<NativeCommand>();
		List<NpcInfos> npcStartVal = null;
		DBFWInfo dbfwInfo = null;
		try {
			conn = this.getJdbcUtils().getConnection();
			this.getJdbcUtils().beginTransaction(conn);

			// PHASE 0. 校验是否允许添加此监听
			boolean checkResult = false;
			String errMsg = "";
			if (list != null) {
				String npcIdQuery = "SELECT DISTINCT npc_id AS npcid,group_id AS groupid FROM `npc_info` WHERE dbfw_inst_id=?";
				// int dbid=getDbIdByDbAddrID(conn,
				// dbfwForDbAddrList.get(0).getAddressId());
				npcStartVal = (List<NpcInfos>) this.getJdbcUtils().query(conn, npcIdQuery,
						new Object[] { list.get(0).getDbfwId() }, NpcInfos.class);
			}
			for (DBFWForDbAddr dbfwForDb : list) {
				switch (dbfwForDb.getMonitorType()) {
				case DBFWConstant.DEPLOY_REMOTEPROXY:
					checkResult = databaseConfigDAO.checkRemoteDBFWAddress(conn, dbfwForDb.getPortId());
					if (!checkResult)
						errMsg = "该端口已经占用";
					break;
				case DBFWConstant.DEPLOY_BYWAY:
					String groupIds = dbfwForDb.getGroupIdStr();
					int groupIdCount = 0;
					if (groupIds.contains(";")) {
						String[] groupIdArr = groupIds.split(";");
						for (int i = 0; i < groupIdArr.length; i++) {
							checkResult = databaseConfigDAO.checkBywayDBFWAddress(conn, dbfwForDb.getDbfwId(), Integer
									.valueOf(groupIdArr[i]));
							if (!checkResult) {
								groupIdCount++;
							}
						}
					} else {
						checkResult = databaseConfigDAO.checkBywayDBFWAddress(conn, dbfwForDb.getDbfwId(), Integer
								.valueOf(dbfwForDb.getGroupIdStr()));
						if (!checkResult) {
							groupIdCount++;
						}
					}
					if (groupIdCount > 0) {
						errMsg = "同一安全实例只允许监听" + DBFWConstant.MAX_NPC_PER_INST + "块网卡";
					}
					break;
				case DBFWConstant.DEPLOY_NATIVEPROXY:
					checkResult = databaseConfigDAO.checkNativeDBFWAddress(conn, dbfwForDb.getPortId());
					if (!checkResult)
						errMsg = "该端口已经占用";
					break;
				case DBFWConstant.DEPLOY_HALFTRANS:
					checkResult = databaseConfigDAO.checkHalftransDBFWAddress(conn, dbfwForDb.getAddressId(), dbfwForDb
							.getPortId());
					if (!checkResult) {
						// errMsg = "该数据库此端口已被重定向至其它端口"; fix bug 3054
						errMsg = "同一个数据库不允许同时被多个安全实例以网桥模式保护";
					}
					// 判断网桥组是否重用
					groupIds = dbfwForDb.getGroupIdStr() + ";";
					String[] groupIdArr = groupIds.split(";");
					for (int i = 0; i < groupIdArr.length; i++) {
						if (!groupIdArr[i].equals("")) {
							checkResult = checkHalftransDBFWAGroupId(conn, dbfwForDb.getDbfwId(), groupIdArr[i]);
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
					this.getJdbcUtils().rollback(conn);
					throw new ServiceException("监听配置错误：" + errMsg);
				}
			}

			for (DBFWForDbAddr dbfwForDbAddr : list) {
				DatabaseAddress address = this.getDatabaseAddressesById(conn, dbfwForDbAddr.getAddressId());
				int port = this.getPortNumById(conn, dbfwForDbAddr.getPortId());
				//
				if (dbfwForDbAddr.getMonitorType() == DBFWConstant.DEPLOY_HALFTRANS) {
					/* 网桥时，自动生成port，范围11001～15000 */
					String port_num_sql = "SELECT IFNULL(MAX(port_num),11000)+1 FROM `group_port` t1 ,interface_group t2 "
							+ " WHERE t1.group_id=t2.group_id AND t2.group_type=2 ";
					port = this.getJdbcUtils().queryForInteger(conn, port_num_sql);
				}
				String all_port_sql = "SELECT t1.port_num as portNum FROM group_port t1 where t1.group_id in(SELECT group_id FROM interface_group where group_type=2) ORDER BY port_num ASC";
				List<GroupPort> all_port_list = new ArrayList<GroupPort>();
				all_port_list = (List<GroupPort>) this.getJdbcUtils().query(all_port_sql, GroupPort.class);
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
				//
				dbfwInfo = this.getDBFWInfoById(conn, dbfwForDbAddr.getDbfwId());
				String ifNameVal = getIfName(dbfwForDbAddr.getGroupId());
				if (dbfwForDbAddr.getMonitorType() != DBFWConstant.DEPLOY_BYWAY
						&& dbfwForDbAddr.getMonitorType() != DBFWConstant.DEPLOY_HALFTRANS) {
					command = new CreateDBCommand(
							(dbfwForDbAddr.getMonitorType() == DBFWConstant.DEPLOY_HALFTRANS ? ("br" + dbfwForDbAddr
									.getGroupId()) : ifNameVal), dbfwForDbAddr.getMonitorType(), address.getAddress(),
							address.getPort(), port, dbfwForDbAddr.getGroupId(), "");
					commands.add(command);

					InterfaceGroup group = getGroupById(conn, dbfwForDbAddr.getGroupId());
					if (group == null) {
						throw new ServiceException("无法在策略中心中找到组：" + dbfwForDbAddr.getGroupName());
					}
					if (dbfwForDbAddr.getMonitorType() == DBFWConstant.DEPLOY_HALFTRANS) {
						if (dbfwInfo != null && dbfwInfo.getInstStat() != 3) {
							command = new CreateNewNPlsCommand(dbfwInfo.getAddress(), dbfwInfo.getPort(), port, address
									.getAddress(), address.getPort(), dbfwForDbAddr.getMonitorType(), group
									.getGroupIp());
							commands.add(command);
						}
					} else {
						command = new CreateNewNPlsCommand(dbfwInfo.getAddress(), dbfwInfo.getPort(), port, address
								.getAddress(), address.getPort(), dbfwForDbAddr.getMonitorType(), group.getGroupIp());
						commands.add(command);

					}
				}

				// 检查dbfwId安全实例下是否已经存在非旁路的部署模式
				// boolean deployModeMatch = this.deployModeMatch(conn,
				// dbfwForDbAddr.getDbfwId(), dbfwForDbAddr
				// .getMonitorType());
				// if (!deployModeMatch) {
				// if (dbfwForDbAddr.getMonitorType() == 2)
				// throw new ServiceException("该安全实例下已经存在非旁路部署模式，不能添加旁路部署模式");
				// else
				// throw new ServiceException("该安全实例下已经存在旁路部署模式，不能添加非旁路部署模式");
				// }
				// PHASE 1. 添加至dbfw_fordb
				databaseConfigDAO.addDbfwForDbAddr(conn, dbfwForDbAddr.getDbfwId(), dbfwForDbAddr.getAddressId(),
						dbfwForDbAddr.getMonitorType(), dbfwForDbAddr.getClientGroupId());
				// PHASE 2. 根据不同部署模式
				switch (dbfwForDbAddr.getMonitorType()) {
				case DBFWConstant.DEPLOY_REMOTEPROXY:
					databaseConfigDAO.addRemoteDbfwForAddrInfo(conn, dbfwForDbAddr.getRemoteHost(), dbfwForDbAddr
							.getRemotePort(), dbfwForDbAddr.getDbfwId(), dbfwForDbAddr.getAddressId(), dbfwForDbAddr
							.getPortId());
					break;
				case DBFWConstant.DEPLOY_BYWAY:
					List<InterfaceInfo> interfaces = null;
					String groupIds = dbfwForDbAddr.getGroupIdStr();
					if (groupIds.contains(";")) {
						String[] groupIdArr = groupIds.split(";");
						for (int i = 0; i < groupIdArr.length; i++) {
							interfaces = this.getInterfaceByGroup(Integer.valueOf(groupIdArr[i]));
							// 判断部署模式与网卡组是否匹配
							int groupType = this.getJdbcUtils().queryForInteger(
									"select group_type from interface_group where group_id=" + groupIdArr[i]);
							if (groupType != DBFWConstant.IFGROUPTYPE_BYPASS) {
								throw new ServiceException("网卡组与部署模式不匹配");
							}
							databaseConfigDAO.addBywayDbfwForAddrInfo(conn, Integer.valueOf(groupIdArr[i]),
									dbfwForDbAddr.getDbfwId(), dbfwForDbAddr.getAddressId(), dbfwInfo, interfaces
											.get(0), commands, flushCommands);
						}
					} else {
						interfaces = this.getInterfaceByGroup(Integer.valueOf(dbfwForDbAddr.getGroupIdStr()));
						// 判断部署模式与网卡组是否匹配
						int groupType = this.getJdbcUtils().queryForInteger(
								"select group_type from interface_group where group_id="
										+ dbfwForDbAddr.getGroupIdStr());
						if (groupType != DBFWConstant.IFGROUPTYPE_BYPASS) {
							throw new ServiceException("网卡组与部署模式不匹配");
						}
						databaseConfigDAO.addBywayDbfwForAddrInfo(conn, Integer.valueOf(dbfwForDbAddr.getGroupIdStr()),
								dbfwForDbAddr.getDbfwId(), dbfwForDbAddr.getAddressId(), dbfwInfo, interfaces.get(0),
								commands, flushCommands);
					}
					break;
				case DBFWConstant.DEPLOY_NATIVEPROXY:
					databaseConfigDAO.addNativeDbfwForAddrInfo(conn, dbfwForDbAddr.getDbfwId(), dbfwForDbAddr
							.getAddressId(), dbfwForDbAddr.getPortId());
					break;
				case DBFWConstant.DEPLOY_HALFTRANS:

					interfaces = null;
					groupIds = dbfwForDbAddr.getGroupIdStr();
					if (groupIds.contains(";")) {
						String[] groupIdArr = groupIds.split(";");
						for (int i = 0; i < groupIdArr.length; i++) {
							interfaces = this.getInterfaceByGroup(Integer.valueOf(groupIdArr[i]));
							// 判断部署模式与网卡组是否匹配
							int groupType = this.getJdbcUtils().queryForInteger(
									"select group_type from interface_group where group_id=" + groupIdArr[i]);
							if (groupType != DBFWConstant.IFGROUPTYPE_BRIDGE) {
								throw new ServiceException("网卡组与部署模式不匹配");
							}
							databaseConfigDAO.addBypassDbfwForAddrInfo(conn, Integer.valueOf(groupIdArr[i]),
									dbfwForDbAddr.getDbfwId(), dbfwForDbAddr.getAddressId(), dbfwInfo, interfaces
											.get(0), commands, flushCommands);
						}
					} else {
						interfaces = this.getInterfaceByGroup(Integer.valueOf(dbfwForDbAddr.getGroupIdStr()));
						// 判断部署模式与网卡组是否匹配
						int groupType = this.getJdbcUtils().queryForInteger(
								"select group_type from interface_group where group_id="
										+ dbfwForDbAddr.getGroupIdStr());
						if (groupType != DBFWConstant.IFGROUPTYPE_BRIDGE) {
							throw new ServiceException("网卡组与部署模式不匹配");
						}
						databaseConfigDAO.addBypassDbfwForAddrInfo(conn,
								Integer.valueOf(dbfwForDbAddr.getGroupIdStr()), dbfwForDbAddr.getDbfwId(),
								dbfwForDbAddr.getAddressId(), dbfwInfo, interfaces.get(0), commands, flushCommands);
					}

					// 去掉之前的网桥逻辑
					// String group_name_sql =
					// "SELECT IFNULL(group_name,'网桥组') FROM interface_group  WHERE group_id= ? ";
					// Object[] args = { dbfwForDbAddr.getGroupId() };
					// NetworkInterfaceDAO.getDAO().addGroupPort(conn,
					// this.getJdbcUtils().queryForString(conn, group_name_sql,
					// args) + ":" + port,
					// dbfwForDbAddr.getGroupId(), port, 1);
					//
					// command = new CreateNplsPortCommand("br" +
					// dbfwForDbAddr.getGroupId(), port, dbfwForDbAddr
					// .getGroupId());
					// commands.add(command);
					// String port_id_sql =
					// "SELECT IFNULL(port_id,1) FROM `group_port` t1   WHERE t1.port_num= "
					// + port;
					//
					// databaseConfigDAO.addHalfTransDbfwForAddrInfo(conn,
					// dbfwForDbAddr.getDbfwId(), dbfwForDbAddr
					// .getAddressId(),
					// this.getJdbcUtils().queryForInteger(conn, port_id_sql));
					// command = new
					// CreateRedirectNplsPortCommand(address.getAddress(),
					// address.getPort(), port,
					// dbfwForDbAddr.getGroupId());
					// commands.add(command);
					break;
				default:
					break;
				}

				// 初始化DBFW-DB的数据信息
				int dbId = this.getDatabaseAddressesById(conn, dbfwForDbAddr.getAddressId()).getDatabaseId();
				boolean inited = this.dbfwDbInited(conn, dbfwForDbAddr.getDbfwId(), dbId);
				if (!inited) {
					this.initDbfwDb(conn, dbfwForDbAddr.getDbfwId(), dbId);
				}

			}
			int dbid = getDbIdByDbAddrID(conn, list.get(0).getAddressId());
			// PHASE 3. 刷新SMON
			DBFWInfo dbfwInfoVal = this.getDBFWInfoById(conn, list.get(0).getDbfwId());
			command = new FlushDbfwForDbInfoCommand(dbfwInfoVal.getAddress(), dbfwInfoVal.getPort(), dbid);
			flushCommands.add(command);
			this.getJdbcUtils().commit(conn);
			if (list != null) {
				// 接口调用重新调整旁路、网桥修改为相同的调用
				if (list.get(0).getMonitorType() == DBFWConstant.DEPLOY_HALFTRANS
						|| list.get(0).getMonitorType() == DBFWConstant.DEPLOY_BYWAY) {
					command = new FlushNfwCommand(dbfwInfo.getAddress(), dbfwInfo.getPort(), list.get(0).getDbfwId());
					commands.add(command);
				}
			}

			try {
				if (System.getProperty("os.name").equals("Linux")) {
					NativeExecutor.execute(commands);
				}
			} catch (RunException e) {
				if (dbfwInfo != null && dbfwInfo.getInstStat() == 3) {
					e = new RunException("安全实例未启动，请刷新页面，启动安全实例后操作生效！");
				}
				throw e;
			}
			// System.out.println(commands);
			// this.getJdbcUtils().rollback(conn);
		} catch (RunException ex) {
			this.getJdbcUtils().rollback(conn);
			throw ex;
		} finally {
			this.getJdbcUtils().close(conn);
		}
		try {
			if (System.getProperty("os.name").equals("Linux")) {
				NativeExecutor.execute(flushCommands);
			}
		} catch (RunException e) {
			if (dbfwInfo != null && dbfwInfo.getInstStat() == 3) {
				e = new RunException("安全实例未启动，请刷新页面，启动安全实例后操作生效！");
			}
			throw e;
		}
	}

	public void removeDbfwForDbAddr(int dbfwId, int addressId, List<NativeCommand> commands) {
		// 重新加载管理IP端口
		Globals.reloadConfigs();

		NativeCommand command = null;
		Connection conn = null;
		List<NativeCommand> flushCommands = new ArrayList<NativeCommand>();
		boolean npcDelete = false;

		try {
			conn = this.getJdbcUtils().getConnection();
			this.getJdbcUtils().beginTransaction(conn);
			DBFWForDbAddr dbfwForDbAddr = databaseConfigDAO.getDBFWForDbAddrByAddressAndDbfw(conn, addressId, dbfwId);
			if (dbfwForDbAddr != null) {
				DatabaseAddress address = this.getDatabaseAddressesById(conn, addressId);
				int port = this.getPortNumById(conn, dbfwForDbAddr.getPortId());
				DBFWInfo dbfwInfo = this.getDBFWInfoById(conn, dbfwForDbAddr.getDbfwId());
				String ifNameVal = getIfName(dbfwForDbAddr.getGroupId());
				if (dbfwForDbAddr.getMonitorType() != DBFWConstant.DEPLOY_BYWAY) {
					command = new DeleteDBCommand(
							(dbfwForDbAddr.getMonitorType() == DBFWConstant.DEPLOY_HALFTRANS ? ("br" + dbfwForDbAddr
									.getGroupId()) : ifNameVal), dbfwForDbAddr.getMonitorType(), address.getAddress(),
							address.getPort(), port, dbfwForDbAddr.getGroupId(), "");
					commands.add(command);

					InterfaceGroup group = getGroupById(conn, dbfwForDbAddr.getGroupId());
					if (group == null) {
						throw new ServiceException("无法在策略中心中找到组：" + dbfwForDbAddr.getGroupName());
					}
					command = new KillNplsCommand(dbfwInfo.getAddress(), dbfwInfo.getPort(), port,
							address.getAddress(), address.getPort(), dbfwForDbAddr.getMonitorType(), group.getGroupIp());
					commands.add(command);
				}

				switch (dbfwForDbAddr.getMonitorType()) {
				case DBFWConstant.DEPLOY_REMOTEPROXY:
					databaseConfigDAO.removeRemoteProxyAndDbfwForAddr(conn, dbfwForDbAddr.getRemoteId(), dbfwForDbAddr
							.getPortId(), dbfwId, addressId);
					break;
				case DBFWConstant.DEPLOY_BYWAY:
					npcDelete = databaseConfigDAO.removeBywayAndDbfwForAddr(conn, dbfwForDbAddr.getNpcInfoId(), dbfwId,
							addressId);
					// 接口调用重新调整旁路、网桥修改为相同的调用
					command = new FlushNfwCommand(dbfwInfo.getAddress(), dbfwInfo.getPort(), dbfwId);
					commands.add(command);
					break;
				case DBFWConstant.DEPLOY_NATIVEPROXY:
					databaseConfigDAO.removeNativeProxyAndDbfwForAddr(conn, dbfwForDbAddr.getPortId(), dbfwId,
							addressId);
					break;
				case DBFWConstant.DEPLOY_HALFTRANS:

					npcDelete = databaseConfigDAO.removeBywayAndDbfwForAddr(conn, dbfwForDbAddr.getNpcInfoId(), dbfwId,
							addressId);
					// 接口调用重新调整旁路、网桥修改为相同的调用
					command = new FlushNfwCommand(dbfwInfo.getAddress(), dbfwInfo.getPort(), dbfwId);
					commands.add(command);
					break;
				default:
					break;
				}
				databaseConfigDAO.removeDbfwForDbAddr(conn, dbfwId, addressId);

				// 清除DBFW-DB的数据信息
				int dbId = this.getDatabaseAddressesById(conn, dbfwForDbAddr.getAddressId()).getDatabaseId();
				this.deinitDbfwDb(conn, dbfwForDbAddr.getDbfwId(), dbId);

				// PHASE 3. 刷新SMON
				command = new FlushDbfwForDbInfoCommand(dbfwInfo.getAddress(), dbfwInfo.getPort(), dbId);
				flushCommands.add(command);
			}
			String status_sql = "SELECT IFNULL(inst_stat,0) FROM `dbfw_instances` t1  WHERE t1.isdelete=0 AND t1.instance_id= "
					+ dbfwId;
			int dbfwstatus = this.getJdbcUtils().queryForInteger(conn, status_sql);
			if (dbfwstatus != 3) {
				NativeExecutor.execute(commands);
			}
			this.getJdbcUtils().commit(conn);
		} catch (RunException ex) {
			this.getJdbcUtils().rollback(conn);
			throw ex;
		} finally {
			this.getJdbcUtils().close(conn);
		}
		NativeExecutor.execute(flushCommands);
	}

	private void removeDbfwForDbAddr(Connection conn, int dbfwId, int addressId, List<NativeCommand> commands) {
		// 重新加载管理IP端口
		Globals.reloadConfigs();

		NativeCommand command = null;
		List<NativeCommand> flushCommands = new ArrayList<NativeCommand>();
		List<NativeCommand> removeCommands = new ArrayList<NativeCommand>();
		boolean npcDelete = false;

		DBFWForDbAddr dbfwForDbAddr = databaseConfigDAO.getDBFWForDbAddrByAddressAndDbfw(conn, addressId, dbfwId);
		if (dbfwForDbAddr == null)
			return;
		DatabaseAddress address = this.getDatabaseAddressesById(conn, addressId);
		int port = this.getPortNumById(conn, dbfwForDbAddr.getPortId());
		DBFWInfo dbfwInfo = this.getDBFWInfoById(conn, dbfwForDbAddr.getDbfwId());
		String ifNameVal = getIfName(dbfwForDbAddr.getGroupId());
		if (dbfwForDbAddr.getMonitorType() != DBFWConstant.DEPLOY_BYWAY
				&& dbfwForDbAddr.getMonitorType() != DBFWConstant.DEPLOY_HALFTRANS) {
			command = new DeleteDBCommand(
					(dbfwForDbAddr.getMonitorType() == DBFWConstant.DEPLOY_HALFTRANS ? ("br" + dbfwForDbAddr
							.getGroupId()) : ifNameVal), dbfwForDbAddr.getMonitorType(), address.getAddress(), address
							.getPort(), port, dbfwForDbAddr.getGroupId(), "");
			commands.add(command);
			removeCommands.add(command);

			InterfaceGroup group = getGroupById(conn, dbfwForDbAddr.getGroupId());
			if (group == null) {
				throw new ServiceException("无法在策略中心中找到组：" + dbfwForDbAddr.getGroupName());
			}
			command = new KillNplsCommand(dbfwInfo.getAddress(), dbfwInfo.getPort(), port, address.getAddress(),
					address.getPort(), dbfwForDbAddr.getMonitorType(), group.getGroupIp());
			commands.add(command);
			removeCommands.add(command);
		}

		switch (dbfwForDbAddr.getMonitorType()) {
		case DBFWConstant.DEPLOY_REMOTEPROXY:
			databaseConfigDAO.removeRemoteProxyAndDbfwForAddr(conn, dbfwForDbAddr.getRemoteId(), dbfwForDbAddr
					.getPortId(), dbfwId, addressId);
			break;
		case DBFWConstant.DEPLOY_BYWAY:
			// String
			// sqlNpcVal="SELECT t1.npc_id AS npcid, t1.group_id AS groupid FROM `npc_info` t1, dbfw_fordb t2 WHERE t1.dbfw_inst_id = t2.dbfw_inst_id AND t1.database_id = t2.database_id AND t1.address_id = t2.address_id AND t2.isdelete = 0 AND t1.dbfw_inst_id = ? AND t1.address_id = ?";
			// List<NpcInfos> npcList=(List<NpcInfos>)
			// this.getJdbcUtils().query(conn, sqlNpcVal, new
			// Object[]{dbfwForDbAddr.getDbfwId(),addressId}, NpcInfos.class);
			npcDelete = databaseConfigDAO.removeBywayAndDbfwForAddr(conn, dbfwForDbAddr, addressId);
			/*
			 * if(npcList!=null){ for(int i=0;i<npcList.size();i++){
			 * List<InterfaceInfo> interfaces =
			 * this.getInterfaceByGroup(npcList.get(i).getGroupid()); if
			 * (npcDelete) { command = new KillNpcCommand(dbfwInfo.getAddress(),
			 * dbfwInfo.getPort(),npcList.get(i).getNpcid(),
			 * interfaces.get(0).getGroupName()); commands.add(command);
			 * removeCommands.add(command); } else { command = new
			 * FlushNpcInfoCommand(dbfwInfo.getAddress(), dbfwInfo.getPort(),
			 * dbfwForDbAddr.getDbfwId(), npcList.get(i).getNpcid());
			 * flushCommands.add(command); } } }
			 */
			break;
		case DBFWConstant.DEPLOY_NATIVEPROXY:
			databaseConfigDAO.removeNativeProxyAndDbfwForAddr(conn, dbfwForDbAddr.getPortId(), dbfwId, addressId);
			break;
		case DBFWConstant.DEPLOY_HALFTRANS:
			databaseConfigDAO.removeHalfTransAndDbfwForAddr(conn, dbfwForDbAddr.getPortId(), dbfwId, addressId);
			break;
		default:
			break;
		}
		databaseConfigDAO.removeDbfwForDbAddr(conn, dbfwId, addressId);

		int dbId = this.getDatabaseAddressesById(conn, dbfwForDbAddr.getAddressId()).getDatabaseId();
		// 注意：此函数在“更新数据库安全实例监听配置”时调用，不能删除规则信息！！！！！
		// this.deinitDbfwDb(conn, dbfwForDbAddr.getDbfwId(), dbId);

		// PHASE 3. 刷新SMON
		// command = new FlushDbfwForDbInfoCommand(dbfwInfo.getAddress(),
		// dbfwInfo.getPort(), dbId);
		// flushCommands.add(command);
		// 修复bug 3843：原因是此处多调用一次后台
		// NativeExecutor.execute(commands);
		NativeExecutor.execute(removeCommands);
		this.getJdbcUtils().commit(conn);
		NativeExecutor.execute(flushCommands);
	}

	public synchronized void updateDbfwForDbAddr(List<DBFWForDbAddr> dbfwForDbAddrList, List<NativeCommand> commands) {
		// 重新加载管理IP端口
		Globals.reloadConfigs();

		NativeCommand command = null;
		Connection conn = null;
		List<NativeCommand> flushCommands = new ArrayList<NativeCommand>();
		List<NpcInfos> npcStartVal = null;
		List<NativeCommand> removeCommands = new ArrayList<NativeCommand>();
		DBFWInfo dbfwInfo = null;
		try {
			conn = this.getJdbcUtils().getConnection();
			this.getJdbcUtils().beginTransaction(conn);

			// PHASE 0. 校验是否允许添加此监听; fix bug 3056
			boolean checkResult = false;
			String errMsg = "";

			if (dbfwForDbAddrList != null) {
				String npcIdQuery = "SELECT DISTINCT npc_id AS npcid,group_id AS groupid FROM `npc_info` WHERE dbfw_inst_id=?";
				// int dbid=getDbIdByDbAddrID(conn,
				// dbfwForDbAddrList.get(0).getAddressId());
				npcStartVal = (List<NpcInfos>) this.getJdbcUtils().query(conn, npcIdQuery,
						new Object[] { dbfwForDbAddrList.get(0).getDbfwId() }, NpcInfos.class);
			}
			for (DBFWForDbAddr dbfwForDb : dbfwForDbAddrList) {
				switch (dbfwForDb.getMonitorType()) {
				case DBFWConstant.DEPLOY_REMOTEPROXY:
					/* 返回的就是可用的端口，无需再检查是否被占用 */
					// checkResult =
					// databaseConfigDAO.checkRemoteDBFWAddress(conn,
					// dbfwForDb.getPortId());
					checkResult = true;
					if (!checkResult)
						errMsg = "未变更或该端口已经占用";
					break;
				case DBFWConstant.DEPLOY_BYWAY:
					String groupIds = dbfwForDb.getGroupIdStr();
					int groupIdCount = 0;
					if (groupIds.contains(";")) {
						String[] groupIdArr = groupIds.split(";");
						for (int i = 0; i < groupIdArr.length; i++) {
							checkResult = databaseConfigDAO.checkBywayDBFWAddress(conn, dbfwForDb.getDbfwId(), Integer
									.valueOf(groupIdArr[i]));
							if (!checkResult) {
								groupIdCount++;
							}
						}
					} else {
						checkResult = databaseConfigDAO.checkBywayDBFWAddress(conn, dbfwForDb.getDbfwId(), Integer
								.valueOf(dbfwForDb.getGroupIdStr()));
						if (!checkResult) {
							groupIdCount++;
						}
					}
					if (groupIdCount > 0) {
						errMsg = "同一安全实例只允许监听" + DBFWConstant.MAX_NPC_PER_INST + "块网卡";
					}
					/*
					 * checkResult =
					 * databaseConfigDAO.checkBywayDBFWAddress(conn,
					 * dbfwForDb.getDbfwId(), dbfwForDb.getGroupId()); if
					 * (!checkResult) errMsg = "同一安全实例只允许监听" +
					 * DBFWConstant.MAX_NPC_PER_INST + "块网卡";
					 */
					break;

				case DBFWConstant.DEPLOY_NATIVEPROXY:
					/* 返回的就是可用的端口，无需再检查是否被占用 */
					// checkResult =
					// databaseConfigDAO.checkNativeDBFWAddress(conn,
					// dbfwForDb.getPortId());
					checkResult = true;
					if (!checkResult)
						errMsg = "未变更或该端口已经占用";
					break;
				case DBFWConstant.DEPLOY_HALFTRANS:
					checkResult = databaseConfigDAO.checkHalftransDBFWAddress_update(conn, dbfwForDb.getAddressId(),
							dbfwForDb.getPortId(), dbfwForDb.getDbfwId());
					if (!checkResult)
						errMsg = "同一个数据库不允许同时被多个安全实例以网桥模式保护";
					break;
				default:
					break;
				}

				if (!checkResult) {
					this.getJdbcUtils().rollback(conn);
					throw new ServiceException("监听配置错误：" + errMsg);
				}
			}

			for (DBFWForDbAddr dbfwForDbAddr : dbfwForDbAddrList) {
				dbfwInfo = this.getDBFWInfoById(conn, dbfwForDbAddr.getDbfwId());
				int dbid = getDbIdByDbAddrID(conn, dbfwForDbAddr.getAddressId());
				List<NpcInfos> stopNpcStaus = null;
				if (dbfwForDbAddr.getMonitorType() != 2) {
					String stopNpcSql = "SELECT DISTINCT npc_id AS npcid,group_id AS groupid FROM `npc_info` WHERE dbfw_inst_id=?";
					stopNpcStaus = (List<NpcInfos>) this.getJdbcUtils().query(conn, stopNpcSql,
							new Object[] { dbfwForDbAddr.getDbfwId() }, NpcInfos.class);
				}
				/*
				 * if(dbfwForDbAddr.getMonitorType()==2){//湛福军暂时注释 List<NpcInfo>
				 * npcInfoValList
				 * =getNpcInfoList(dbfwForDbAddr.getDbfwId(),dbid);
				 * if(npcInfoValList!=null){ String
				 * groupIds=dbfwForDbAddr.getGroupIdStr(); String[]
				 * groupIdArr=groupIds.split(";"); int npcCount=0; for(int
				 * i=0;i<groupIdArr.length;i++){ int npcVal=0; for(int
				 * j=0;j<npcInfoValList.size();j++){
				 * if(Integer.valueOf(groupIdArr
				 * [i].trim())!=npcInfoValList.get(j).getGroupId()){ npcVal++;
				 * if(npcVal==npcInfoValList.size()){ npcCount++; } }else{
				 * if(npcVal==npcInfoValList.size()){ npcCount++; } break; } } }
				 * int npcAllCount=npcCount+npcInfoValList.size();
				 * if(npcAllCount>4){ throw new
				 * DAOException("该安全实例已达NPC最大数量（4个）"); } }
				 * 
				 * }
				 */
				// 删除原数据
				removeDbfwForDbAddr(conn, dbfwForDbAddr.getDbfwId(), dbfwForDbAddr.getAddressId(), commands);

				DatabaseAddress address = this.getDatabaseAddressesById(conn, dbfwForDbAddr.getAddressId());
				int port = this.getPortNumById(conn, dbfwForDbAddr.getPortId());
				if (dbfwForDbAddr.getMonitorType() == DBFWConstant.DEPLOY_HALFTRANS) {
					/* 网桥时，自动生成port，范围11001～15000 */
					String port_num_sql = "SELECT IFNULL(MAX(port_num),11000)+1 FROM `group_port` t1 ,interface_group t2 "
							+ " WHERE t1.group_id=t2.group_id AND t2.group_type=2 ";
					port = this.getJdbcUtils().queryForInteger(conn, port_num_sql);
					String all_port_sql = "SELECT t1.port_num as portNum FROM group_port t1 where t1.group_id in(SELECT group_id FROM interface_group where group_type=2) ORDER BY port_num ASC";
					// 验证网桥模式时，port的范围
					List<GroupPort> all_port_list = new ArrayList<GroupPort>();
					all_port_list = (List<GroupPort>) this.getJdbcUtils().query(all_port_sql, GroupPort.class);
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
				}
				String ifNameVal = getIfName(dbfwForDbAddr.getGroupId());
				if (dbfwForDbAddr.getMonitorType() != DBFWConstant.DEPLOY_BYWAY
						&& dbfwForDbAddr.getMonitorType() != DBFWConstant.DEPLOY_HALFTRANS) {
					command = new CreateDBCommand(
							(dbfwForDbAddr.getMonitorType() == DBFWConstant.DEPLOY_HALFTRANS ? ("br" + dbfwForDbAddr
									.getGroupId()) : ifNameVal), dbfwForDbAddr.getMonitorType(), address.getAddress(),
							address.getPort(), port, dbfwForDbAddr.getGroupId(), "");
					commands.add(command);

					InterfaceGroup group = getGroupById(conn, dbfwForDbAddr.getGroupId());
					if (group == null) {
						throw new ServiceException("无法在策略中心中找到组：" + dbfwForDbAddr.getGroupName());
					}
					if (dbfwForDbAddr.getMonitorType() == DBFWConstant.DEPLOY_HALFTRANS) {
						if (dbfwInfo != null && dbfwInfo.getInstStat() != 3) {
							command = new CreateNewNPlsCommand(dbfwInfo.getAddress(), dbfwInfo.getPort(), port, address
									.getAddress(), address.getPort(), dbfwForDbAddr.getMonitorType(), group
									.getGroupIp());
							commands.add(command);
						}
					} else {
						command = new CreateNewNPlsCommand(dbfwInfo.getAddress(), dbfwInfo.getPort(), port, address
								.getAddress(), address.getPort(), dbfwForDbAddr.getMonitorType(), group.getGroupIp());
						commands.add(command);

					}
				}

				// 检查dbfwId安全实例下是否已经存在非旁路的部署模式
				// boolean deployModeMatch = this.deployModeMatch1(conn,
				// dbfwForDbAddr.getDbfwId(), dbfwForDbAddr
				// .getMonitorType(), dbfwForDbAddr.getAddressId());
				// if (!deployModeMatch) {
				// if (dbfwForDbAddr.getMonitorType() == 2)
				// throw new ServiceException("该安全实例下已经存在非旁路部署模式，不能添加旁路部署模式");
				// else
				// throw new ServiceException("该安全实例下已经存在旁路部署模式，不能添加非旁路部署模式");
				// }
				// if (dbfwForDbAddr.getMonitorType() != 2) {
				// if (stopNpcStaus != null) {
				// for (int i = 0; i < stopNpcStaus.size(); i++) {
				// List<InterfaceInfo> interfacesval =
				// this.getInterfaceByGroup(stopNpcStaus.get(i)
				// .getGroupid());
				// command = new KillNpcCommand(dbfwInfo.getAddress(),
				// dbfwInfo.getPort(), stopNpcStaus.get(i)
				// .getNpcid(), interfacesval.get(0).getGroupName());
				// removeCommands.add(command);
				// }
				// NativeExecutor.execute(removeCommands);
				// }
				// }
				// 添加数据
				// PHASE 1. 添加至dbfw_fordb
				databaseConfigDAO.addDbfwForDbAddr(conn, dbfwForDbAddr.getDbfwId(), dbfwForDbAddr.getAddressId(),
						dbfwForDbAddr.getMonitorType(), dbfwForDbAddr.getClientGroupId());
				// PHASE 2. 根据不同部署模式
				switch (dbfwForDbAddr.getMonitorType()) {
				case DBFWConstant.DEPLOY_REMOTEPROXY:
					databaseConfigDAO.addRemoteDbfwForAddrInfo(conn, dbfwForDbAddr.getRemoteHost(), dbfwForDbAddr
							.getRemotePort(), dbfwForDbAddr.getDbfwId(), dbfwForDbAddr.getAddressId(), dbfwForDbAddr
							.getPortId());
					break;
				case DBFWConstant.DEPLOY_BYWAY:
					List<InterfaceInfo> interfaces = null;
					String groupIds = dbfwForDbAddr.getGroupIdStr();
					if (groupIds.contains(";")) {
						String[] groupIdArr = groupIds.split(";");
						for (int i = 0; i < groupIdArr.length; i++) {
							interfaces = this.getInterfaceByGroup(Integer.valueOf(groupIdArr[i]));
							if (interfaces.size() == 0) {
								throw new ServiceException("无效的网卡组ID");
							}
							// 判断部署模式与网卡组是否匹配
							int groupType = this.getJdbcUtils().queryForInteger(
									"select group_type from interface_group where group_id=" + groupIdArr[i]);
							if (groupType != DBFWConstant.IFGROUPTYPE_BYPASS) {
								throw new ServiceException("网卡组与部署模式不匹配");
							}
							databaseConfigDAO.addBywayDbfwForAddrInfo(conn, Integer.valueOf(groupIdArr[i]),
									dbfwForDbAddr.getDbfwId(), dbfwForDbAddr.getAddressId(), dbfwInfo, interfaces
											.get(0), commands, flushCommands);
						}
					} else {
						interfaces = this.getInterfaceByGroup(Integer.valueOf(dbfwForDbAddr.getGroupIdStr()));
						if (interfaces.size() == 0) {
							throw new ServiceException("无效的网卡组ID");
						}
						// 判断部署模式与网卡组是否匹配
						int groupType = this.getJdbcUtils().queryForInteger(
								"select group_type from interface_group where group_id="
										+ dbfwForDbAddr.getGroupIdStr());
						if (groupType != DBFWConstant.IFGROUPTYPE_BYPASS) {
							throw new ServiceException("网卡组与部署模式不匹配");
						}
						databaseConfigDAO.addBywayDbfwForAddrInfo(conn, Integer.valueOf(dbfwForDbAddr.getGroupIdStr()),
								dbfwForDbAddr.getDbfwId(), dbfwForDbAddr.getAddressId(), dbfwInfo, interfaces.get(0),
								commands, flushCommands);
					}
					break;
				case DBFWConstant.DEPLOY_NATIVEPROXY:
					databaseConfigDAO.addNativeDbfwForAddrInfo(conn, dbfwForDbAddr.getDbfwId(), dbfwForDbAddr
							.getAddressId(), dbfwForDbAddr.getPortId());
					break;
				case DBFWConstant.DEPLOY_HALFTRANS:
					// String group_name_sql =
					// "SELECT IFNULL(group_name,'网桥组') FROM interface_group  WHERE group_id= ? ";
					// Object[] args = { dbfwForDbAddr.getGroupId() };
					// NetworkInterfaceDAO.getDAO().addGroupPort(conn,
					// this.getJdbcUtils().queryForString(conn, group_name_sql,
					// args) + ":" + port,
					// dbfwForDbAddr.getGroupId(), port, 1);
					//
					// command = new CreateNplsPortCommand("br" +
					// dbfwForDbAddr.getGroupId(), port, dbfwForDbAddr
					// .getGroupId());
					// commands.add(command);
					// String port_id_sql =
					// "SELECT IFNULL(port_id,1) FROM `group_port` t1   WHERE t1.port_num= "
					// + port;
					//
					// databaseConfigDAO.addHalfTransDbfwForAddrInfo(conn,
					// dbfwForDbAddr.getDbfwId(), dbfwForDbAddr
					// .getAddressId(),
					// this.getJdbcUtils().queryForInteger(conn, port_id_sql));
					// // databaseConfigDAO.addHalfTransDbfwForAddrInfo(conn,
					// // dbfwForDbAddr.getDbfwId(),
					// //
					// dbfwForDbAddr.getAddressId(),dbfwForDbAddr.getPortId());
					// command = new
					// CreateRedirectNplsPortCommand(address.getAddress(),
					// address.getPort(), port,
					// dbfwForDbAddr.getGroupId());
					// commands.add(command);

					interfaces = null;
					groupIds = dbfwForDbAddr.getGroupIdStr();
					if (groupIds.contains(";")) {
						String[] groupIdArr = groupIds.split(";");
						for (int i = 0; i < groupIdArr.length; i++) {
							interfaces = this.getInterfaceByGroup(Integer.valueOf(groupIdArr[i]));
							if (interfaces.size() == 0) {
								throw new ServiceException("无效的网卡组ID");
							}
							// 判断部署模式与网卡组是否匹配
							int groupType = this.getJdbcUtils().queryForInteger(
									"select group_type from interface_group where group_id=" + groupIdArr[i]);
							if (groupType != DBFWConstant.IFGROUPTYPE_BRIDGE) {
								throw new ServiceException("网卡组与部署模式不匹配");
							}

							databaseConfigDAO.addBypassDbfwForAddrInfo(conn, Integer.valueOf(groupIdArr[i]),
									dbfwForDbAddr.getDbfwId(), dbfwForDbAddr.getAddressId(), dbfwInfo, interfaces
											.get(0), commands, flushCommands);
						}
					} else {
						interfaces = this.getInterfaceByGroup(Integer.valueOf(dbfwForDbAddr.getGroupIdStr()));
						if (interfaces.size() == 0) {
							throw new ServiceException("无效的网卡组ID");
						}
						// 判断部署模式与网卡组是否匹配
						int groupType = this.getJdbcUtils().queryForInteger(
								"select group_type from interface_group where group_id="
										+ dbfwForDbAddr.getGroupIdStr());
						if (groupType != DBFWConstant.IFGROUPTYPE_BRIDGE) {
							throw new ServiceException("网卡组与部署模式不匹配");
						}
						databaseConfigDAO.addBypassDbfwForAddrInfo(conn,
								Integer.valueOf(dbfwForDbAddr.getGroupIdStr()), dbfwForDbAddr.getDbfwId(),
								dbfwForDbAddr.getAddressId(), dbfwInfo, interfaces.get(0), commands, flushCommands);
					}
					break;
				default:
					break;
				}

				/*
				 * // PHASE 3. 刷新SMON int dbId =
				 * this.getDatabaseAddressesById(conn,
				 * dbfwForDbAddr.getAddressId()).getDatabaseId(); command = new
				 * FlushDbfwForDbInfoCommand(dbfwInfo.getAddress(),
				 * dbfwInfo.getPort(), dbId); flushCommands.add(command);
				 */
			}
			// PHASE 3. 刷新SMON
			int dbid = getDbIdByDbAddrID(conn, dbfwForDbAddrList.get(0).getAddressId());
			DBFWInfo dbfwInfoVal = this.getDBFWInfoById(conn, dbfwForDbAddrList.get(0).getDbfwId());
			command = new FlushDbfwForDbInfoCommand(dbfwInfoVal.getAddress(), dbfwInfoVal.getPort(), dbid);
			flushCommands.add(command);
			this.getJdbcUtils().commit(conn);
			if (dbfwForDbAddrList != null) {
				// 接口调用重新调整旁路、网桥修改为相同的调用
				if (dbfwForDbAddrList.get(0).getMonitorType() == DBFWConstant.DEPLOY_HALFTRANS
						|| dbfwForDbAddrList.get(0).getMonitorType() == DBFWConstant.DEPLOY_BYWAY) {
					command = new FlushNfwCommand(dbfwInfo.getAddress(), dbfwInfo.getPort(), dbfwInfo.getId());
					commands.add(command);
				}
			}
			try {
				NativeExecutor.execute(commands);
			} catch (RunException e) {
				if (dbfwInfo != null && dbfwInfo.getInstStat() == 3) {
					e = new RunException("安全实例未启动，请刷新页面，启动安全实例后操作生效！");
				}
				throw e;
			}
		} catch (RunException ex) {
			this.getJdbcUtils().rollback(conn);
			throw ex;
		} finally {
			this.getJdbcUtils().close(conn);
		}
		try {
			NativeExecutor.execute(flushCommands);
		} catch (RunException e) {
			if (dbfwInfo != null && dbfwInfo.getInstStat() == 3) {
				e = new RunException("安全实例未启动，请刷新页面，启动安全实例后操作生效！");
			}
			throw e;
		}
	}

	@SuppressWarnings("unchecked")
	private List<InterfaceInfo> getInterfaceByGroup(int groupId) {
		String sql = "SELECT t1.if_id AS ifId, t1.if_mac AS macAddr, t1.if_name AS ifName, t1.is_link AS isLink, t1.group_id AS groupId, t2.group_name AS groupName "
				+ "FROM interface_info t1, interface_group t2 " + "WHERE t2.group_id=? AND t1.group_id=t2.group_id";
		Object[] args = { groupId };
		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql, args);
		return (List<InterfaceInfo>) this.getJdbcUtils().query(sql, args, InterfaceInfo.class);
	}

	private boolean dbfwDbInited(Connection conn, int dbfwId, int dbId) {
		String sql = "SELECT COUNT(*) FROM dbfw.actived_baselines WHERE dbfw_inst_id=? AND db_id=? AND state=1";
		Object[] args = { dbfwId, dbId };
		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql, args);
		int cnt = this.getJdbcUtils().queryForInteger(conn, sql, args);
		if (cnt > 0) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 初始化被保护数据库相关的基线和规则相关数据
	 * 1：生成一个默认的'数据库初始默认基线(全部放行)'基线，注意基线号需要继承可能存在的该DBFW+DB的已有基线
	 * 并且保证将已有的活动基线设置为“废弃”状态 2：生成与该基线关联的VPatch规则数据，所有的VPatch规则状态设置为“禁用”
	 * 
	 * @param conn
	 * @param dbfwId
	 * @param dbId
	 */
	private void initDbfwDb(Connection conn, int dbfwId, int dbId) {
		String getNewBaselineSql = "SELECT IFNULL(MAX(baseline_id),0) FROM dbfw.actived_baselines WHERE dbfw_inst_id=? AND db_id=?";
		String chkRuleidForallSql = "SELECT COUNT(*) FROM dbfw.ruleid_forall WHERE dbfw_inst_id=? AND database_id=?";
		String chkACUsersSql = "SELECT COUNT(*) FROM dbfw.ac_users WHERE dbfw_inst_id=? AND database_id=?  AND isdelete='0' ";
		String chkACObjsSql = "SELECT COUNT(*) FROM dbfw.ac_objects WHERE dbfw_inst_id=? AND database_id=?  AND isdelete='0'";
		String chkACAppsSql = "SELECT COUNT(*) FROM dbfw.ac_dbapps WHERE dbfw_inst_id=? AND database_id=? AND isdelete='0'";
		String chkVPruleForDBSql = "SELECT COUNT(*) FROM dbfw.vpatchrule_for_db_and_dbfw WHERE dbfw_inst_id=? AND database_id=? AND is_deleted='0'";
		String chkVPStatForDBSql = "SELECT COUNT(*) FROM dbfw.vpatchstat_for_db_and_dbfw WHERE dbfw_inst_id=? AND database_id=? AND isdelete='0'";
		String chkACForbidSql = "SELECT COUNT(*) FROM dbfw.ac_forbiddentype WHERE dbfw_inst_id=? AND database_id=? AND isdelete='0'";
		String chkACRowtypeSql = "SELECT COUNT(*) FROM dbfw.ac_returnrowtype WHERE dbfw_inst_id=? AND database_id=? AND isdelete='0'";
		String chkACLoginRuleSql = "SELECT COUNT(*) FROM dbfw.ac_loginrules WHERE dbfw_inst_id=? AND database_id=? AND isdelete='0'";
		String chkACRuleSql = "SELECT COUNT(*) FROM dbfw.ac_rules WHERE dbfw_inst_id=? AND database_id=? AND isdelete='0'";
		String chkRuleBaseSql = "SELECT COUNT(*) FROM dbfw.rule_base WHERE instance_id=? AND database_id=? ";
		String chkRiskRuleSql = "SELECT COUNT(*) FROM dbfw.risk_rules WHERE dbfw_inst_id=? AND database_id=? AND is_delete=0 "
				+ "  AND dialect=(SELECT dialect FROM xsec_databases WHERE database_id=" + dbId + " LIMIT 1)";
		String chkDbfwModeSql = "SELECT COUNT(*) FROM dbfw.dbfwmode_fordb WHERE dbfw_inst_id=? AND database_id=? AND isdelete='0'";
		String chkRiskLevelSql = "SELECT COUNT(*) FROM dbfw.risklevel_blackwhite WHERE dbfwid=? AND dbid=? AND blackorwhite=? AND isdelete='0'";
		String chkIgnoreRuleSql = "SELECT COUNT(*) FROM dbfw.ignore_fordb WHERE dbfw_inst_id=? AND database_id=? AND isdelete='0'";
		String chkActionSql = "SELECT COUNT(*) FROM dbfw.action_forlevel WHERE dbfw_inst_id=? AND database_id=? AND isdelete='0'";
		int chkRet = 0;
		Object[] getNewBaselineArgs = { dbfwId, dbId };
		Object[] dbfwArgs = { dbfwId };
		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), getNewBaselineSql,
				getNewBaselineArgs);
		int activeBaseline = this.getJdbcUtils().queryForInteger(conn, getNewBaselineSql, getNewBaselineArgs);

		// 更新原有基线为“废弃状态”
		String updateBaselineSql = "UPDATE dbfw.actived_baselines SET state='0' WHERE dbfw_inst_id=? AND db_id=? AND baseline_id=?";
		Object[] updateBaselineArgs = { dbfwId, dbId, activeBaseline };

		// actived_baselines.
		Date now = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");// 可以方便地修改日期格式
		String str_now = dateFormat.format(now);
		String initBaselineSql = "INSERT INTO dbfw.actived_baselines(baseline_id,name,dbfw_inst_id,db_id,create_time,active_time,state,isbranch) "
				+ "VALUES(?,'数据库初始默认基线(全部放行)" + str_now + "',?,?,SYSDATE(),SYSDATE(),1,0)";
		Object[] initBaselineArgs = { activeBaseline + 1, dbfwId, dbId };

		// ruleid_forall.
		String initRuleidSql = "INSERT INTO dbfw.ruleid_forall(dbfw_inst_id,database_id,ruleid,rule_type) VALUES "
				+ "(?,?,32,0),(?,?,256,1),(?,?,2002,2),(?,?,3002,3),(?,?,4002,4),(?,?,9000,5),(?,?,9500,6),(?,?,9700,7),(?,?,10000,8)";
		Object[] initRuleidArgs = { dbfwId, dbId, dbfwId, dbId, dbfwId, dbId, dbfwId, dbId, dbfwId, dbId, dbfwId, dbId,
				dbfwId, dbId, dbfwId, dbId, dbfwId, dbId };
		// ac_users.
		String initUserSql = "INSERT INTO dbfw.ac_users(userid,username,usertype,dbfw_inst_id,database_id,baseline_id,is_dba,is_4a_user,logtime,dbfwusers_uid,state,isdelete) "
				+
				// "SELECT 0,t1.objectname,1,?,?,?,0,0,SYSDATE(),1,1,'0' " +
				"SELECT (@rowNum:=@rowNum+1) as rowNo,t1.objectname,1,?,?,?,0,0,SYSDATE(),1,1,'0' "
				+ "FROM dbfw.dbsysobjects t1, xsec_databases t2,(Select (@rowNum :=0) ) b "
				+ "WHERE t1.objtype=3 AND t2.database_id=? AND t1.dialect=t2.dialect AND dbfw.dbfw_xsecdbversion_2_sysdbversion(t2.db_version,t2.dialect)=t1.dbversion";
		Object[] initUserArgs = { dbfwId, dbId, activeBaseline + 1, dbId };
		// String updateUserSql =
		// "UPDATE dbfw.ac_users SET userid=id WHERE dbfw_inst_id=? AND database_id=?";
		// Object[] updateUserArgs = { dbfwId, dbId };

		// ac_objects.
		String initObjectSql = "INSERT INTO dbfw.ac_objects(objectid,dbfw_inst_id,database_id,objecttype,schemaname,objectname,baseline_id,logtime,dbfwusers_uid,state,isdelete) "
				+ "SELECT 5000+sysobj_id,?,?,objtype,schemaname,objectname,?,SYSDATE(),1,1,'0' "
				+ "FROM dbfw.dbsysobjects t1, xsec_databases t2 "
				+ "WHERE (objtype=1 OR objtype=2) AND t2.database_id=? AND t1.dialect=t2.dialect AND ";
		if ("32".equals(getXsec_databasesDialect(dbId))) {
			initObjectSql = initObjectSql
					+ "(select CASE WHEN t3.db_version>=4000 and t3.db_version<5000 THEN 4 WHEN t3.db_version>=5000 and t3.db_version<6000 THEN 5 ELSE 6 END as db_version  from xsec_databases t3 where t3.database_id="
					+ dbId + ")=t1.dbversion ";
		} else {
			initObjectSql = initObjectSql
					+ "dbfw.dbfw_xsecdbversion_2_sysdbversion(t2.db_version,t2.dialect)=t1.dbversion";
		}

		Object[] initObjectArgs = { dbfwId, dbId, activeBaseline + 1, dbId };

		// ac_dbapps.
		String initAppSql = "INSERT INTO dbfw.ac_dbapps(app_id,app_name,app_type,dbfw_inst_id,database_id,baseline_id,logtime,dbfwusers_uid,state,isdelete) "
				+ "SELECT ac_app_id,app_name,app_type,?,?,?,SYSDATE(),1,1,'0' "
				+ "FROM dbfw.dbapp_provider "
				+ "WHERE dbfw_inst_id=0";
		Object[] initAppArgs = { dbfwId, dbId, activeBaseline + 1 };
		// String updateAppSql =
		// "UPDATE dbfw.ac_dbapps SET app_id=id WHERE dbfw_inst_id=? AND database_id=?";
		// Object[] updateAppArgs = { dbfwId, dbId };

		// vpatchrule_for_db_and_dbfw.
		String initVpatchRuleSql = "INSERT INTO dbfw.vpatchrule_for_db_and_dbfw(dbfw_inst_id,database_id,rule_id,baseline_id,control_policy,category,risk_level,result_delevery,"
				+ "result_alarmaudit,result_audit,logtime,dbfwusers_uid,state,is_deleted,version,risk_level_4_bmj) "
				+ "SELECT ?,?,category_id+9700,?,0,category_id,3,1,1,1,SYSDATE(),3,1,'0',0,'2' "
				+ "FROM dbfw.vpatchrule_category";
		Object[] initVpatchRuleArgs = { dbfwId, dbId, activeBaseline + 1 };

		// vpatchstat_for_db_and_dbfw.
		// 生成与该基线关联的VPatch规则数据，所有的VPatch规则状态设置为“禁用”
		int dbVersion = this.getDbVersion(conn, dbId);
		String sql = "SELECT db_type FROM xsec_databases WHERE database_id=?";
		Object[] args = { dbId };
		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql, args);
		int dbtype = this.getJdbcUtils().queryForInteger(conn, sql, args);

		String versionStr = "(0";
		if (dbtype == 1)// ORACLE
		{
			if (dbVersion > 0) {
				for (int i = 0; i < 4; i++) {
					if (i < 3
							&& (dbVersion / (int) (Math.pow(10, (2 * i))) * (int) (Math.pow(10, (2 * i))) == (dbVersion
									/ (int) (Math.pow(10, 2 * (i + 1))) * (int) (Math.pow(10, 2 * (i + 1))))))
						continue;
					int queryVersion = dbVersion / (int) (Math.pow(10, (2 * i))) * (int) (Math.pow(10, (2 * i)));
					versionStr += "," + queryVersion;
				}
			}
		} else if (dbtype == 4)// MySql
		{
			versionStr += "," + (Integer) (dbVersion > 1000 ? (dbVersion / 1000) : dbVersion);
		} else {
			versionStr += "," + dbVersion;
		}
		versionStr += ")";
		String initVpatchStatSql = "INSERT INTO dbfw.vpatchstat_for_db_and_dbfw(dbfw_inst_id,database_id,baseline_id,vpatch_ruleid,logtime,dbfwusers_uid,state,isdelete) "
				+ "SELECT ?,?,?,rule_id,SYSDATE(),1,0,'0' "
				+ "FROM vpatchrule_for_dbversion "
				+ "WHERE version_id IN ("
				+ "SELECT id FROM dbfw.vpatch_dbversion WHERE db_type=(SELECT db_type FROM xsec_databases WHERE database_id=? LIMIT 1) "
				+ "AND db_version IN "
				+ versionStr
				+ ") AND rule_id IN (SELECT id FROM dbfw.vpatch_rules WHERE is_deleted=0 AND is_valid=1)";
		Object[] initVpatchStatArgs = { dbfwId, dbId, activeBaseline + 1, dbId };

		// ac_forbiddentype.
		String initForbidTypeSql = "INSERT INTO dbfw.ac_forbiddentype(dbfw_inst_id,database_id,rule_type,rule_id,forbidden_type,baseline_id,logtime,dbfwusers_uid,state,isdelete) "
				+ "VALUES(?,?,0,3001,0,?,SYSDATE(),1,1,'0'),(?,?,1,4001,0,?,SYSDATE(),1,1,'0')";
		Object[] initForbidTypeArgs = { dbfwId, dbId, activeBaseline + 1, dbfwId, dbId, activeBaseline + 1 };

		// ac_returnrowtype.
		String initReturnRowSql = "INSERT INTO dbfw.ac_returnrowtype(dbfw_inst_id,database_id,rule_id,type,row_num,baseline_id,logtime,dbfwusers_uid,state,isdelete) "
				+ "VALUES(?,?,2000,0,20,?,SYSDATE(),1,1,'0'),(?,?,2001,1,100,?,SYSDATE(),1,1,'0')";
		Object[] initReturnRuwArgs = { dbfwId, dbId, activeBaseline + 1, dbfwId, dbId, activeBaseline + 1 };

		// ac_loginrules.
		String initLoginRuleSql = "INSERT INTO dbfw.ac_loginrules(dbfw_inst_id,database_id,rule_type,rule_id,clientip,clientip_except,clientmac,risk_level,result_delevery,result_alarmaudit,"
				+ "result_audit,rulename,ruledescription,baseline_id,logtime,dbfwusers_uid,state,isdelete,risk_level_4_bmj) "
				+ "VALUES(?,?,0,3000,'','','',3,1,1,1,'违反信任登录规则','违反信任登录规则的行为',?,SYSDATE(),1,1,'0','2')";
		Object[] initLoginRuleArgs = { dbfwId, dbId, activeBaseline + 1 };

		// ac_rules.
		String initAcRuleSql = "INSERT INTO dbfw.ac_rules(dbfw_inst_id,database_id,rule_type,rule_id,objectid,risk_level,result_delevery,result_alarmaudit,result_audit,rulename,"
				+ "ruledescription,baseline_id,logtime,dbfwusers_uid,state,isdelete,risk_level_4_bmj) "
				+ "VALUES(?,?,0,4000,0,3,1,1,1,'违反信任规则(信任模型)','违反信任规则(信任模型)的行为',?,SYSDATE(),1,1,'0','2')";
		Object[] initAcRuleArgs = { dbfwId, dbId, activeBaseline + 1 };

		// rule_base.
		String initRuleBaseSql = "INSERT INTO dbfw.rule_base(database_id,instance_id,rule_id) VALUES(?,?,?)";
		Object[] initRuleBaseArgs = { dbId, dbfwId, dbId };

		String initRiskRuleSql = "INSERT INTO risk_rules ( rule_id, rulename, dbfw_inst_id,"
				+ " database_id, baseline_id, dialect, first_ope, sec_ope, is_comments, is_or, is_union,"
				+ " is_true, is_empty_pwd, is_mul_query, bruteforce_fun, regex, risk_score,result_delevery,"
				+ " result_audit, risk_action, issys, texts, dbfwusers_uid, state, is_delete, logtime,risk_level_4_bmj)"
				+ " SELECT rule_id, rulename, ?, ?, ?, dialect, first_ope, sec_ope, is_comments, is_or, is_union,"
				+ " is_true, is_empty_pwd, is_mul_query, bruteforce_fun, regex, risk_score,result_delevery,"
				+ " result_audit, risk_action, issys, texts, dbfwusers_uid, 0, 0, SYSDATE(),risk_level_4_bmj "
				+ " FROM risk_rules WHERE dbfw_inst_id = 0 AND database_id = 0 AND baseline_id = 0"
				+ "  AND dialect=(SELECT dialect FROM xsec_databases WHERE database_id=" + dbId + " LIMIT 1)";
		Object[] initRiskRelationArgs = { dbfwId, dbId, activeBaseline + 1 };

		// dbfwmode_fordb.
		String initDBFWModeSql = "INSERT INTO dbfw.dbfwmode_fordb(baseline_id,ruleid,dbfw_inst_id,database_id,runmode,learn_mode,learn_starttime,learn_interval,learn_endtime,"
				+ "protect_mode,controlmodel,logtime,dbfwusers_uid,state,isdelete) "
				+ "VALUES(?,9500,?,?,0,0,SYSDATE(),10080,DATE_ADD(SYSDATE(),INTERVAL 7 DAY),0,2,SYSDATE(),1,'1','0')";
		Object[] initDBFWModeArgs = { activeBaseline + 1, dbfwId, dbId };

		// risklevel_blackwhite.
		String initRisklevelBwSql = "INSERT INTO dbfw.risklevel_blackwhite(baseline_id,ruleid,dbfwid,dbid,blackorwhite,"
				+ "risk_level,result_delevery,result_alarmaudit,result_audit,logtime,dbfwusers_uid,state,isdelete,risk_level_4_bmj) "
				+ "VALUES(?,10000,?,?,?,?,1,1,1,SYSDATE(),1,1,'0','2')";
		Object[] initRiskLevelWhiteArgs = { activeBaseline + 1, dbfwId, dbId, 1, 3 };
		Object[] initRiskLevelBlackArgs = { activeBaseline + 1, dbfwId, dbId, 2, 5 };

		// ignore_fordb
		String initIgnoreruleSql = "INSERT INTO dbfw.ignore_fordb (baseline_id, ruleid, dbfw_inst_id, database_id, ignore_type, "
				+ "ignore_white, ignore_enable, logtime, dbfwusers_uid, state, isdelete) "
				+ "VALUES (?,?,?,?,?, 1, 1, SYSDATE(), 1, 1, 0)";

		// action_forlevel.
		String initActionForLevelSql = "INSERT INTO dbfw.action_forlevel(baseline_id,ruleid,dbfw_inst_id,database_id,"
				+ "risk_level,result_control,result_delevery,result_alarmaudit,result_audit,logtime,dbfwusers_uid,state,isdelete) "
				+ "VALUES(?,9001,?,?,1,0,0,1,1,SYSDATE(),1,1,0),(?,9002,?,?,3,2,0,1,1,SYSDATE(),1,1,0),(?,9003,?,?,5,1,1,1,1,SYSDATE(),1,1,0)";
		int dbTypeVal = getDbTypeVal(conn, dbId);
		if (dbTypeVal != 1 && dbTypeVal != 2 && dbTypeVal != 4 && dbTypeVal != 10 && dbTypeVal != 12 && dbTypeVal != 5
				&& dbTypeVal != 11 && dbTypeVal != 9 && dbTypeVal != 18 && dbTypeVal != 6) {// 如果不是Oracle、Sqlserver、sysbase、DM、informix、Gbase8T中风险默认值为阻断
			initActionForLevelSql = "INSERT INTO dbfw.action_forlevel(baseline_id,ruleid,dbfw_inst_id,database_id,"
					+ "risk_level,result_control,result_delevery,result_alarmaudit,result_audit,logtime,dbfwusers_uid,state,isdelete) "
					+ "VALUES(?,9001,?,?,1,0,0,1,1,SYSDATE(),1,1,0),(?,9002,?,?,3,1,0,1,1,SYSDATE(),1,1,0),(?,9003,?,?,5,1,1,1,1,SYSDATE(),1,1,0)";
		}
		Object[] initActionForLevelArgs = { activeBaseline + 1, dbfwId, dbId, activeBaseline + 1, dbfwId, dbId,
				activeBaseline + 1, dbfwId, dbId };

		// 步骤1：检查DBFWID和DBID对应的MAX基线，如果有则将该DBFWID+DBID下的所有基线记录设置为state=’0’
		if (activeBaseline > 0) {
			// 有原来的基线，将该基线设置为废弃
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), updateBaselineSql,
					updateBaselineArgs);
			this.getJdbcUtils().execute(conn, updateBaselineSql, updateBaselineArgs);
		}
		// 步骤2：向active_baseline表中添加一条新的“初始基线”记录，注意如果之前取得了基线ID，则在该ID上加一，否则直接设置基线ID为1
		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), initBaselineSql,
				initBaselineArgs);
		this.getJdbcUtils().execute(conn, initBaselineSql, initBaselineArgs);

		// 步骤3：ruleid_forall.
		// 首先检查ruleid_forall表中是否有该DBFWID+DBID的记录，如果有则不进行任何改变（保留原记录），如果没有则insert一条新的记录（使用初始值）
		chkRet = this.getJdbcUtils().queryForInteger(conn, chkRuleidForallSql, getNewBaselineArgs);
		if (chkRet == 0) {
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), initRuleidSql,
					initRuleidArgs);
			this.getJdbcUtils().execute(conn, initRuleidSql, initRuleidArgs);
		}
		// 步骤4：处理ac_users表：首先检查ac_users表中是否有该DBFWID+DBID的记录，如果有则不进行任何改变（保留原记录），如果没有则从dbfw.dbsysobjects表中将objtype=3的初始记录insert到ac_users表中（这里要根据版本判断）
		chkRet = this.getJdbcUtils().queryForInteger(conn, chkACUsersSql, getNewBaselineArgs);
		if (chkRet == 0) {
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), initUserSql,
					initUserArgs);
			this.getJdbcUtils().execute(conn, initUserSql, initUserArgs);

			// this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(),
			// updateUserSql, updateUserArgs);
			// this.getJdbcUtils().execute(conn, updateUserSql, updateUserArgs);
		}
		// 步骤5：处理ac_objects表：首先检查ac_users表中是否有该DBFWID+DBID的记录，如果有则不进行任何改变（保留原记录），如果没有则从dbfw.dbsysobjects表中将objtype=1
		// OR
		// objtype=2的初始记录insert到ac_users表中（这里要根据版本判断）。然后将userid设置为唯一值（userid=id）
		chkRet = this.getJdbcUtils().queryForInteger(conn, chkACObjsSql, getNewBaselineArgs);
		if (chkRet == 0) {
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), initObjectSql,
					initObjectArgs);
			this.getJdbcUtils().execute(conn, initObjectSql, initObjectArgs);
		}
		// 步骤6：ac_dbapps表：首先检查ac_dbapps表中是否有该DBFWID+DBID的记录，如果有则不进行任何改变（保留原记录），如果没有则从dbfw.dbapp_provider表中将dbfw_inst_id=0的初始记录insert到ac_dbapps表中
		chkRet = this.getJdbcUtils().queryForInteger(conn, chkACAppsSql, getNewBaselineArgs);
		if (chkRet == 0) {
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), initAppSql,
					initAppArgs);
			this.getJdbcUtils().execute(conn, initAppSql, initAppArgs);
			// this.getJdbcUtils().execute(conn, updateAppSql, updateAppArgs);
		}
		// 步骤7：
		// vpatchrule_for_db_and_dbfw表：检查过程同上，如果没有则从dbfw.vpatchrule_category表中将记录加入作为初始规则，全部规则的风险级别=3、不递送、开启告警审计，启用
		chkRet = this.getJdbcUtils().queryForInteger(conn, chkVPruleForDBSql, getNewBaselineArgs);
		if (chkRet == 0) {
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), initVpatchRuleSql,
					initVpatchRuleArgs);
			this.getJdbcUtils().execute(conn, initVpatchRuleSql, initVpatchRuleArgs);
		}
		// 步骤8：vpatchstat_for_db_and_dbfw表：检查过程同上，如果没有则从dbfw.
		// vpatchrule_for_dbversion表中将记录加入作为初始规则
		chkRet = this.getJdbcUtils().queryForInteger(conn, chkVPStatForDBSql, getNewBaselineArgs);
		if (chkRet == 0) {
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), initVpatchStatSql,
					initVpatchStatArgs);
			this.getJdbcUtils().execute(conn, initVpatchStatSql, initVpatchStatArgs);
		}
		// 步骤9：ac_forbiddentype表：检查过程同上，如果没有则添加2条默认规则（3001和4001规则）
		chkRet = this.getJdbcUtils().queryForInteger(conn, chkACForbidSql, getNewBaselineArgs);
		if (chkRet == 0) {
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), initForbidTypeSql,
					initForbidTypeArgs);
			this.getJdbcUtils().execute(conn, initForbidTypeSql, initForbidTypeArgs);
		}
		// 步骤10：ac_returnrowtype表：检查过程同上，如果没有则添加2条默认规则（20行、100行）
		chkRet = this.getJdbcUtils().queryForInteger(conn, chkACRowtypeSql, getNewBaselineArgs);
		if (chkRet == 0) {
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), initReturnRowSql,
					initReturnRuwArgs);
			this.getJdbcUtils().execute(conn, initReturnRowSql, initReturnRuwArgs);
		}
		// 步骤11：ac_loginrules表：检查过程同上，如果没有则添加1条默认规则（3000：违反登录许可（信任登录）规则）
		chkRet = this.getJdbcUtils().queryForInteger(conn, chkACLoginRuleSql, getNewBaselineArgs);
		if (chkRet == 0) {
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), initLoginRuleSql,
					initLoginRuleArgs);
			this.getJdbcUtils().execute(conn, initLoginRuleSql, initLoginRuleArgs);
		}
		// 步骤12：ac_rules表：检查过程同上，如果没有则添加1条默认规则（4000：违反白名单规则）
		chkRet = this.getJdbcUtils().queryForInteger(conn, chkACRuleSql, getNewBaselineArgs);
		if (chkRet == 0) {
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), initAcRuleSql,
					initAcRuleArgs);
			this.getJdbcUtils().execute(conn, initAcRuleSql, initAcRuleArgs);
		}
		// 步骤13：rule_base表：检查过程同上，如果没有则添加1条默认规则
		chkRet = this.getJdbcUtils().queryForInteger(conn, chkRuleBaseSql, getNewBaselineArgs);
		if (chkRet == 0) {
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), initRuleBaseSql,
					initRuleBaseArgs);
			this.getJdbcUtils().execute(conn, initRuleBaseSql, initRuleBaseArgs);
		}
		// 步骤14：risk_relation表：检查过程同上，如果没有则添加1条默认规则
		chkRet = this.getJdbcUtils().queryForInteger(conn, chkRiskRuleSql, getNewBaselineArgs);
		if (chkRet == 0) {
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), initRiskRuleSql,
					initRiskRelationArgs);
			this.getJdbcUtils().execute(conn, initRiskRuleSql, initRiskRelationArgs);
		}
		// 步骤15：dbfwmode_fordb表：检查过程同上，如果没有则添加1条默认规则
		chkRet = this.getJdbcUtils().queryForInteger(conn, chkDbfwModeSql, getNewBaselineArgs);
		if (chkRet == 0) {
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), initDBFWModeSql,
					initDBFWModeArgs);
			this.getJdbcUtils().execute(conn, initDBFWModeSql, initDBFWModeArgs);
		}
		// 步骤16：risklevel_blackwhite表：检查过程同上，如果没有则添加1条默认规则
		// 白名单
		chkRet = this.getJdbcUtils().queryForInteger(conn, chkRiskLevelSql, new Object[] { dbfwId, dbId, 1 });
		if (chkRet == 0) {
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), initRisklevelBwSql,
					initRiskLevelWhiteArgs);
			this.getJdbcUtils().execute(conn, initRisklevelBwSql, initRiskLevelWhiteArgs);
		}
		// 黑名单
		chkRet = this.getJdbcUtils().queryForInteger(conn, chkRiskLevelSql, new Object[] { dbfwId, dbId, 2 });
		if (chkRet == 0) {
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), initRisklevelBwSql,
					initRiskLevelBlackArgs);
			this.getJdbcUtils().execute(conn, initRisklevelBwSql, initRiskLevelBlackArgs);
		}

		// 步骤17：ignore_fordb表：检查过程同上
		chkRet = this.getJdbcUtils().queryForInteger(conn, chkIgnoreRuleSql, new Object[] { dbfwId, dbId });
		if (chkRet == 0) {
			Object[] initIgnoreRuleArgs_1 = { activeBaseline + 1, 9004, dbfwId, dbId, 1 };
			Object[] initIgnoreRuleArgs_2 = { activeBaseline + 1, 9005, dbfwId, dbId, 2 };
			Object[] initIgnoreRuleArgs_3 = { activeBaseline + 1, 9006, dbfwId, dbId, 3 };
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), initRisklevelBwSql,
					initIgnoreRuleArgs_1);
			this.getJdbcUtils().execute(conn, initIgnoreruleSql, initIgnoreRuleArgs_1);
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), initRisklevelBwSql,
					initIgnoreRuleArgs_2);
			this.getJdbcUtils().execute(conn, initIgnoreruleSql, initIgnoreRuleArgs_2);
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), initRisklevelBwSql,
					initIgnoreRuleArgs_3);
			this.getJdbcUtils().execute(conn, initIgnoreruleSql, initIgnoreRuleArgs_3);
		}

		// 步骤18：action_forlevel表：检查过程同上，如果没有则添加3条默认规则（高风险、中风险、低风险）
		chkRet = this.getJdbcUtils().queryForInteger(conn, chkActionSql, getNewBaselineArgs);
		if (chkRet == 0) {
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(),
					initActionForLevelSql, initActionForLevelArgs);
			this.getJdbcUtils().execute(conn, initActionForLevelSql, initActionForLevelArgs);
		}
	}

	private int getDbVersion(Connection conn, int dbId) {
		String sql = "SELECT db_version FROM xsec_databases WHERE database_id=?";
		Object[] args = { dbId };
		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql, args);
		return this.getJdbcUtils().queryForInteger(conn, sql, args);
	}

	private void deinitDbfwDb(Connection conn, int dbfwId, int dbId) {
		String sql = "CALL dbfw.deinit_dbfw_db(?,?)";
		Object[] args = { dbfwId, dbId };
		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql, args);
		this.getJdbcUtils().execute(conn, sql, args);
	}

	private void deinitDb(Connection conn, int dbId) {
		String sql = "CALL dbfw.deinit_db(?)";
		Object[] args = { dbId };
		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql, args);
		this.getJdbcUtils().execute(conn, sql, args);
	}

	public List<DBAudit> getAuditDataFromOracle(int dbId) {
		int ret = 0;
		List<DBAudit> dbauditdata = null;
		List<DBAudit> list = new ArrayList<DBAudit>();

		DatabaseAddress dbaddress = new DatabaseAddress();
		Connection conn = null;

		try {

			String sql = "SELECT t.address AS address,t.port AS port,t.service_name AS serviceName,t.db_username as userName,t.db_passwd AS userPwd,t.dyna_port AS dynaPort FROM database_addresses t WHERE t.database_id=? ORDER BY userName DESC LIMIT 1 ";
			Object[] args = { dbId };
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql, args);
			dbaddress = (DatabaseAddress) this.getJdbcUtils().query(sql, args, DatabaseAddress.class).get(0);
			conn = ConnectionFactory.createOracleConnection(dbaddress.getAddress(), dbaddress.getPort(), dbaddress
					.getServiceName(), dbaddress.getUserName(), dbaddress.getUserPwd());
			String sqlgetaudit = "SELECT t.SESSION_ID AS sessionid,to_char(t.TIMESTAMP,'yyyy-mm-dd hh:mi:ss') AS auditdate,t.DB_USER AS DBuser,"
					+ "t.OS_USER AS OSuser,t.USERHOST AS userhost,t.OBJECT_SCHEMA AS schemaname,"
					+ "t.OBJECT_NAME AS objectname,t.STATEMENT_TYPE AS statementtype,t.SQL_TEXT as sqltext "
					+ "FROM SYS.DBA_FGA_AUDIT_TRAIL t where userhost like '%'||sys.utl_inaddr.get_host_name()  ORDER BY  auditdate DESC ";
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sqlgetaudit, args);
			ResultSet rs = conn.createStatement().executeQuery(sqlgetaudit);
			while (rs.next()) {
				DBAudit obj = new DBAudit();
				obj.setSessionid(rs.getInt(1));
				obj.setAuditdate(rs.getString(2));
				obj.setDBuser(rs.getString(3));
				obj.setOSuser(rs.getString(4));
				obj.setUserhost(rs.getString(5));
				obj.setSchemaname(rs.getString(6));
				obj.setObjectname(rs.getString(7));
				obj.setStatementtype(rs.getString(8));
				obj.setSqltext(rs.getString(9));
				list.add(obj);
			}

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			throw new ServiceException("获取数据库审计信息失败：" + e.getMessage());
		} catch (SQLException e) {
			e.printStackTrace();
			throw new ServiceException("获取数据库审计信息失败：" + e.getMessage());
		}
		dbauditdata = list;
		return dbauditdata;
	}

	/*
	 * 从DC中获取数据库配置信息
	 */
	private List<DBConfigDetail> getDBConfigFromDC(Connection conn, int dbId) {
		List<DBConfigDetail> list = new ArrayList<DBConfigDetail>();
		List<DBConfigDetailDC> list2 = new ArrayList<DBConfigDetailDC>();
		String sql = "SELECT typeid,config1,config2,config3,config4 FROM dbconfigdetail WHERE dbid=? ORDER BY typeid";
		Object[] args = { dbId };
		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql, args);
		list2 = (List<DBConfigDetailDC>) this.getJdbcUtils().query(conn, sql, args, DBConfigDetailDC.class);
		for (DBConfigDetailDC DBConfig : list2) {
			if (DBConfig.getTypeid() == 1) {
				DBConfigDetail obj = new DBConfigDetail();
				obj.setCtype(1);
				obj.setDbversion(DBConfig.getConfig1());
				list.add(obj);
			}
			if (DBConfig.getTypeid() == 2) {
				DBConfigDetail objp = new DBConfigDetail();
				objp.setCtype(2);
				objp.setPname(DBConfig.getConfig1());
				objp.setPvalue(DBConfig.getConfig2());
				objp.setPdesc(DBConfig.getConfig3());
				list.add(objp);
			}
			if (DBConfig.getTypeid() == 3) {
				DBConfigDetail obju = new DBConfigDetail();
				obju.setCtype(3);
				obju.setUsername(DBConfig.getConfig1());
				obju.setUstatus(DBConfig.getConfig2());
				obju.setUobjtct(DBConfig.getConfig3());
				obju.setUpriv(DBConfig.getConfig4());
				list.add(obju);
			}
			if (DBConfig.getTypeid() == 4) {
				DBConfigDetail objd = new DBConfigDetail();
				objd.setCtype(4);
				objd.setDname(DBConfig.getConfig1());
				objd.setDcreatedt(DBConfig.getConfig2());
				objd.setDstartdt(DBConfig.getConfig3());
				objd.setDfilepath(DBConfig.getConfig4());
				list.add(objd);
			}
			if (DBConfig.getTypeid() == 5) {
				DBConfigDetail objf = new DBConfigDetail();
				objf.setCtype(5);
				objf.setSplat(DBConfig.getConfig1());
				list.add(objf);
			}
		}
		return list;
	}

	/*
	 * 将数据库配置信息更新到DC表中 kind:1 删除，2 新加
	 */
	private void setDBConfigToDC(Connection conn, int dbId, int typeid, int kind, String conf1, String conf2,
			String conf3, String conf4) {
		this.getJdbcUtils().beginTransaction(conn);
		if (kind == 1) {
			String sql = "DELETE FROM dbconfigdetail WHERE dbid=? AND typeid=?";
			Object[] args = { dbId, typeid };
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql, args);
			this.getJdbcUtils().execute(sql, args);
		} else {
			String sql = "INSERT INTO dbconfigdetail VALUES(?,?,?,?,?,?)";
			Object[] args = { dbId, typeid, conf1, conf2, conf3, conf4 };
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql, args);
			this.getJdbcUtils().execute(sql, args);
		}
		this.getJdbcUtils().commit(conn);
	}

	/*
	 * 获取Oracle数据库详细参数
	 */
	public List<DBConfigDetail> getDBConfigFromOracle(int dbId) {
		int ret = 0;
		List<DBConfigDetail> list = new ArrayList<DBConfigDetail>();
		List<DBConfigDetailDC> list2 = new ArrayList<DBConfigDetailDC>();

		DatabaseAddress dbaddress = new DatabaseAddress();
		Connection conn = null;
		Connection conntodc = this.getJdbcUtils().getConnection();

		try {
			String sql = "SELECT t.address AS address,t.port AS port,t.service_name AS serviceName,t.db_username as userName,t.db_passwd AS userPwd,t.dyna_port AS dynaPort FROM database_addresses t WHERE t.database_id=? ORDER BY userName DESC LIMIT 1 ";
			Object[] args = { dbId };
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql, args);
			dbaddress = (DatabaseAddress) this.getJdbcUtils().query(sql, args, DatabaseAddress.class).get(0);
			// 如果数据库不能连接则从DC获取数据
			try {
				conn = ConnectionFactory.createOracleConnection(dbaddress.getAddress(), dbaddress.getPort(), dbaddress
						.getServiceName(), dbaddress.getUserName(), dbaddress.getUserPwd());

			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				throw new ServiceException("获取数据库配置信息失败：" + e.getMessage());
			} catch (Exception e) {
				try {
					conntodc = this.getJdbcUtils().getConnection();
					list = getDBConfigFromDC(conntodc, dbId);
					if (list.size() == 0) {
						throw new ServiceException("不能获取配置信息，请检查连接配置是否正确");
					}
					return list;
				} catch (Exception ex) {
					ex.printStackTrace();
					throw new ServiceException("获取数据库配置信息失败：" + ex.getMessage());
				}
			}
			// Get DB version
			String dbverioin = "SELECT banner AS dbversion FROM sys.v$version WHERE banner LIKE 'Oracle%'";
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), dbverioin, args);
			ResultSet rs = conn.createStatement().executeQuery(dbverioin);

			setDBConfigToDC(conntodc, dbId, 1, 1, "", "", "", ""); // 删除DC中旧的版本信息

			List<Object[]> argsList = new ArrayList<Object[]>();
			while (rs.next()) {
				DBConfigDetail obj = new DBConfigDetail();
				obj.setCtype(1);
				obj.setDbversion(rs.getString(1));
				list.add(obj);
				// setDBConfigToDC(conntodc,dbId,1,2,rs.getString(1),"","","");
				// //将信息插入到DC表中
				argsList.add(new Object[] { dbId, 1, rs.getString(1), "", "", "" });
			}

			// Get DB parameter config
			String pconfig = "SELECT name AS pname,value AS pvalue,description AS pdesc FROM sys.v$parameter order by name";
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), pconfig, args);
			ResultSet rsp = conn.createStatement().executeQuery(pconfig);
			setDBConfigToDC(conntodc, dbId, 2, 1, "", "", "", ""); // 删除DC中旧的参数信息
			while (rsp.next()) {
				DBConfigDetail objp = new DBConfigDetail();
				objp.setCtype(2);
				objp.setPname(rsp.getString(1));
				objp.setPvalue(rsp.getString(2));
				objp.setPdesc(rsp.getString(3));
				list.add(objp);
				// setDBConfigToDC(conntodc,dbId,2,2,rsp.getString(1),rsp.getString(2),rsp.getString(3),"");
				// //将信息插入到DC表中
				argsList.add(new Object[] { dbId, 2, rsp.getString(1), rsp.getString(2), rsp.getString(3), "" });
			}
			// Get DB version
			String userpriv = "SELECT u.username,u.account_status,'角色' as priv_type,p.granted_role AS upriv "
					+ "FROM sys.dba_users u,sys.dba_role_privs p " + "WHERE u.username=p.grantee " + "UNION "
					+ "SELECT u.username,u.account_status,'系统权限' as priv_type,p.privilege AS upriv "
					+ "FROM sys.dba_users u,sys.dba_sys_privs p " + "WHERE u.username=p.grantee " + "UNION "
					+ "SELECT u.username,u.account_status,p.table_name as priv_type,p.privilege AS upriv "
					+ "FROM sys.dba_users u,sys.dba_tab_privs p " + "WHERE u.username=p.grantee "
					+ "ORDER BY username,priv_type";
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), userpriv, args);
			ResultSet rsu = conn.createStatement().executeQuery(userpriv);
			setDBConfigToDC(conntodc, dbId, 3, 1, "", "", "", ""); // 删除DC中旧的用户权限信息
			while (rsu.next()) {
				DBConfigDetail obju = new DBConfigDetail();
				obju.setCtype(3);
				obju.setUsername(rsu.getString(1));
				obju.setUstatus(rsu.getString(2));
				obju.setUobjtct(rsu.getString(3));
				obju.setUpriv(rsu.getString(4));
				list.add(obju);
				// setDBConfigToDC(conntodc,dbId,3,2,rsu.getString(1),rsu.getString(2),rsu.getString(3),rsu.getString(4));
				// //将信息插入到DC表中
				argsList.add(new Object[] { dbId, 3, rsu.getString(1), rsu.getString(2), rsu.getString(3),
						rsu.getString(4) });
			}

			// Get platform info
			String dfilepath = "SELECT SUBSTR(value,0,instr(value,'\\1')+6) FROM sys.v$parameter where name='spfile'";
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), dfilepath, args);
			ResultSet rspath = conn.createStatement().executeQuery(dfilepath);
			String v_path = "";
			while (rspath.next()) {
				v_path = rspath.getString(1);
			}
			// Get DB info
			String dbinfo = "SELECT i.INSTANCE_NAME,TO_CHAR(d.CREATED,'yyyy-mm-dd hh24:mi:ss'),TO_CHAR(i.STARTUP_TIME,'yyyy-mm-dd hh24:mi:ss') "
					+ "FROM sys.V$instance i,sys.V$database d " + "WHERE i.INSTANCE_NAME=d.DB_UNIQUE_NAME";
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), dbinfo, args);
			ResultSet rsd = conn.createStatement().executeQuery(dbinfo);
			setDBConfigToDC(conntodc, dbId, 4, 1, "", "", "", ""); // 删除DC中旧的版本信息
			while (rsd.next()) {
				DBConfigDetail objd = new DBConfigDetail();
				objd.setCtype(4);
				objd.setDname(rsd.getString(1));
				objd.setDcreatedt(rsd.getString(2));
				objd.setDstartdt(rsd.getString(3));
				objd.setDfilepath(v_path);
				list.add(objd);
				// setDBConfigToDC(conntodc,dbId,4,2,rsd.getString(1),rsd.getString(2),rsd.getString(3),v_path);
				// //将信息插入到DC表中
				argsList.add(new Object[] { dbId, 4, rsd.getString(1), rsd.getString(2), rsd.getString(3), v_path });
			}
			// Get platform info
			String platform = "SELECT d.PLATFORM_NAME FROM sys.V$database d WHERE rownum<2";
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), platform, args);
			ResultSet rsf = conn.createStatement().executeQuery(platform);
			setDBConfigToDC(conntodc, dbId, 5, 1, "", "", "", ""); // 删除DC中旧的版本信息
			while (rsf.next()) {
				DBConfigDetail objf = new DBConfigDetail();
				objf.setCtype(5);
				objf.setSplat(rsf.getString(1));
				list.add(objf);
				// setDBConfigToDC(conntodc,dbId,5,2,rsf.getString(1),"","","");
				// //将信息插入到DC表中
				argsList.add(new Object[] { dbId, 5, rsf.getString(1), "", "", "" });
			}
			//
			getJdbcUtils().execute("INSERT INTO dbconfigdetail VALUES(?,?,?,?,?,?)", argsList);
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServiceException(e.getMessage());
		}
		return list;
	}

	/*
	 * 获取MSSql数据库详细参数
	 */
	public List<DBConfigDetail> getDBConfigFromMSSql(int dbId) {
		int ret = 0;
		List<DBConfigDetail> list = new ArrayList<DBConfigDetail>();

		DatabaseAddress dbaddress = new DatabaseAddress();
		Connection conn = null;
		Connection conntodc = this.getJdbcUtils().getConnection();

		try {

			String sql = "SELECT t.address AS address,t.port AS port,t.service_name AS serviceName,t.db_username as userName,t.db_passwd AS userPwd,t.dyna_port AS dynaPort FROM database_addresses t WHERE t.database_id=? ORDER BY userName DESC LIMIT 1 ";
			Object[] args = { dbId };
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql, args);
			dbaddress = (DatabaseAddress) this.getJdbcUtils().query(sql, args, DatabaseAddress.class).get(0);
			// 如果数据库不能连接则从DC获取数据
			try {
				conn = ConnectionFactory.createMssqlConnection(dbaddress.getAddress(), dbaddress.getPort(), dbaddress
						.getUserName(), dbaddress.getUserPwd());
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				throw new ServiceException("获取数据库配置信息失败：" + e.getMessage());
			} catch (Exception e) {
				try {
					conntodc = this.getJdbcUtils().getConnection();
					list = getDBConfigFromDC(conntodc, dbId);
					if (list.size() == 0) {
						throw new ServiceException("不能获取配置信息，请检查连接配置是否正确");
					}
					return list;
				} catch (Exception ex) {
					ex.printStackTrace();
					throw new ServiceException("获取数据库配置信息失败：" + ex.getMessage());
				}
			}
			// Get DB version
			String dbverioin = "SELECT @@VERSION AS dbversion";
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), dbverioin, args);
			ResultSet rs = conn.createStatement().executeQuery(dbverioin);

			setDBConfigToDC(conntodc, dbId, 1, 1, "", "", "", ""); // 删除DC中旧的版本信息
			List<Object[]> argsList = new ArrayList<Object[]>();
			while (rs.next()) {
				DBConfigDetail obj = new DBConfigDetail();
				obj.setCtype(1);
				obj.setDbversion(rs.getString(1));
				list.add(obj);
				// setDBConfigToDC(conntodc,dbId,1,2,rs.getString(1),"","","");
				// //将信息插入到DC表中
				argsList.add(new Object[] { dbId, 1, rs.getString(1), "", "", "" });
			}
			// Get DB parameter config
			String pconfig = "SELECT config AS pname,value AS pvalue,comment AS pdesc FROM sys.sysconfigures ORDER BY config";
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), pconfig, args);
			ResultSet rsp = conn.createStatement().executeQuery(pconfig);

			setDBConfigToDC(conntodc, dbId, 2, 1, "", "", "", ""); // 删除DC中旧的参数信息
			while (rsp.next()) {
				DBConfigDetail objp = new DBConfigDetail();
				objp.setCtype(2);
				objp.setPname(rsp.getString(1));
				objp.setPvalue(rsp.getString(2));
				objp.setPdesc(rsp.getString(3));
				list.add(objp);
				argsList.add(new Object[] { dbId, 2, rsp.getString(1), rsp.getString(2), rsp.getString(3), "" });
			}
			// Get DB version
			String userpriv = "SELECT loginname AS username,'正常' AS ustatus,'' as priv_type, 'sysadmin' AS upriv FROM sys.syslogins WHERE sysadmin=1 "
					+ "UNION "
					+ "SELECT loginname AS username,'正常' AS ustatus,'' as priv_type, 'securityadmin' AS upriv FROM sys.syslogins WHERE securityadmin=1 "
					+ "UNION "
					+ "SELECT loginname AS username,'正常' AS ustatus,'' as priv_type, 'serveradmin' AS upriv FROM sys.syslogins WHERE serveradmin=1 "
					+ "UNION "
					+ "SELECT loginname AS username,'正常' AS ustatus,'' as priv_type, 'setupadmin' AS upriv FROM sys.syslogins WHERE setupadmin=1 "
					+ "UNION "
					+ "SELECT loginname AS username,'正常' AS ustatus,'' as priv_type, 'processadmin' AS upriv FROM sys.syslogins WHERE processadmin=1 "
					+ "UNION "
					+ "SELECT loginname AS username,'正常' AS ustatus,'' as priv_type, 'diskadmin' AS upriv FROM sys.syslogins WHERE diskadmin=1 "
					+ "UNION "
					+ "SELECT loginname AS username,'正常' AS ustatus,'' as priv_type, 'dbcreator' AS upriv FROM sys.syslogins WHERE dbcreator=1 "
					+ "UNION "
					+ "SELECT loginname AS username,'正常' AS ustatus,'' as priv_type, 'bulkadmin' AS upriv FROM sys.syslogins WHERE bulkadmin=1 "
					+ "ORDER BY loginname";
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), userpriv, args);
			ResultSet rsu = conn.createStatement().executeQuery(userpriv);
			setDBConfigToDC(conntodc, dbId, 3, 1, "", "", "", ""); // 删除DC中旧的用户权限信息
			while (rsu.next()) {
				DBConfigDetail obju = new DBConfigDetail();
				obju.setCtype(3);
				obju.setUsername(rsu.getString(1));
				obju.setUstatus(rsu.getString(2));
				obju.setUobjtct(rsu.getString(3));
				obju.setUpriv(rsu.getString(4));
				list.add(obju);
				argsList.add(new Object[] { dbId, 3, rsu.getString(1), rsu.getString(2), rsu.getString(3),
						rsu.getString(4) });
			}
			// Get DB info
			String dbinfo = "SELECT d.name,convert(varchar(30),d.crdate,120),convert(varchar(30),p.login_time,120),d.filename "
					+ "FROM sys.sysdatabases d,sys.sysprocesses p " + "WHERE p.spid=1";
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), dbinfo, args);
			ResultSet rsd = conn.createStatement().executeQuery(dbinfo);
			setDBConfigToDC(conntodc, dbId, 4, 1, "", "", "", ""); // 删除DC中旧的用户权限信息
			while (rsd.next()) {
				DBConfigDetail objd = new DBConfigDetail();
				objd.setCtype(4);
				objd.setDname(rsd.getString(1));
				objd.setDcreatedt(rsd.getString(2));
				objd.setDstartdt(rsd.getString(3));
				objd.setDfilepath(rsd.getString(4));
				list.add(objd);
				argsList.add(new Object[] { dbId, 4, rsd.getString(1), rsd.getString(2), rsd.getString(3),
						rsd.getString(4) });
			}
			// Get platform info
			String platform = "exec xp_msver 'WindowsVersion'";
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), platform, args);
			ResultSet rsf = conn.createStatement().executeQuery(platform);
			setDBConfigToDC(conntodc, dbId, 5, 1, "", "", "", ""); // 删除DC中旧的用户权限信息
			while (rsf.next()) {
				DBConfigDetail objf = new DBConfigDetail();
				objf.setCtype(5);
				objf.setSplat("Windows NT " + rsf.getString(4));
				list.add(objf);
				argsList.add(new Object[] { dbId, 5, "Windows NT " + rsf.getString(4), "", "", "" });
			}
			getJdbcUtils().execute("INSERT INTO dbconfigdetail VALUES(?,?,?,?,?,?)", argsList);
		} catch (SQLException e) {
			e.printStackTrace();
			throw new ServiceException(e.getMessage());
		}
		return list;
	}

	public List<DBAudit> getAuditDataFromMSSql(int dbId) {
		int ret = 0;
		List<DBAudit> dbauditdata = null;
		List<DBAudit> list = new ArrayList<DBAudit>();

		DatabaseAddress dbaddress = new DatabaseAddress();
		Connection conn = null;

		try {

			String sql = "SELECT t.address AS address,t.port AS port,t.db_username as userName,t.db_passwd AS userPwd,t.dyna_port AS dynaPort FROM database_addresses t WHERE t.database_id=? ORDER BY userName DESC LIMIT 1 ";
			Object[] args = { dbId };
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql, args);
			dbaddress = (DatabaseAddress) this.getJdbcUtils().query(sql, args, DatabaseAddress.class).get(0);
			conn = ConnectionFactory.createMssqlConnection(dbaddress.getAddress(), dbaddress.getPort(), dbaddress
					.getUserName(), dbaddress.getUserPwd());
			String sqlgetaudit = "SELECT t.SESSION_ID AS sessionid,CONVERT(varchar(19) ,t.event_time,120) AS auditdate,t.server_principal_name AS DBuser,"
					+ "'' AS OSuser,t.server_instance_name AS userhost,CASE WHEN datalength(t.database_name)=0 THEN t.schema_name ELSE t.database_name+'.'+t.schema_name END AS schemaname,"
					+ "t.OBJECT_NAME AS objectname,CASE WHEN t.action_id='SL' THEN 'SELECT' WHEN t.action_id='IN' THEN 'INSERT' WHEN t.action_id='UP' THEN 'UPDATE' WHEN t.action_id='DL' THEN 'DELETE' ELSE t.action_id END AS statementtype,t.statement as sqltext "
					+ "FROM sys.fn_get_audit_file('D:\\sqlserveraudit\\*', default, default) t WHERE t.action_id <>'AUSC' ORDER BY  auditdate DESC ";
			// dbauditdata = (List<DBAudit>)
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sqlgetaudit, args);
			ResultSet rs = conn.createStatement().executeQuery(sqlgetaudit);
			while (rs.next()) {
				DBAudit obj = new DBAudit();
				obj.setSessionid(rs.getInt(1));
				obj.setAuditdate(rs.getString(2));
				obj.setDBuser(rs.getString(3));
				obj.setOSuser(rs.getString(4));
				obj.setUserhost(rs.getString(5));
				obj.setSchemaname(rs.getString(6));
				obj.setObjectname(rs.getString(7));
				obj.setStatementtype(rs.getString(8));
				obj.setSqltext(rs.getString(9));
				list.add(obj);
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			throw new ServiceException("获取数据库审计信息失败：" + e.getMessage());
		} catch (SQLException e) {
			e.printStackTrace();
			throw new ServiceException("获取数据库审计信息失败：" + e.getMessage());
		}
		dbauditdata = list;
		return dbauditdata;
	}

	public int getOracleVersion(String host, int port, String sid, String userName, String password) {
		int ret = 0;
		String versionStr = "";
		String charSet = "";
		int charsetNum = 0;
		Connection conn = null;

		try {
			conn = ConnectionFactory.createOracleConnection(host, port, sid, userName, password);
			charSet = DatabaseVersion.getOracleCharSet(conn, false);
			versionStr = DatabaseVersion.getOracleVersionText(conn, true);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			throw new ServiceException("获取数据库版本失败：" + e.getMessage());
		} catch (SQLException e) {
			e.printStackTrace();
			if (e.getMessage().contains("ORA-12505")) {
				try {
					conn = ConnectionFactory.createOracleRacConnection(host, port, sid, userName, password);
					charSet = DatabaseVersion.getOracleCharSet(conn, false);
					versionStr = DatabaseVersion.getOracleVersionText(conn, true);
				} catch (ClassNotFoundException e1) {
					e1.printStackTrace();
					throw new ServiceException("获取数据库版本失败：" + e.getMessage());
				} catch (SQLException e2) {
					e2.printStackTrace();
					throw new ServiceException("获取数据库版本失败：" + e.getMessage());
				}
			} else {
				throw new ServiceException("获取数据库版本失败：" + e.getMessage());
			}
		}

		String[] strs = versionStr.split("\\.");
		if (strs.length != 5) {
			throw new ServiceException("获取数据库版本时出现异常：无效的版本号格式：" + versionStr);
		}

		// ret = Integer.parseInt(strs[0])*1000000 +
		// Integer.parseInt(strs[1])*10000 + Integer.parseInt(strs[2])*100 +
		// Integer.parseInt(strs[3]);
		ret = Integer.parseInt(strs[0]) * 10000000 + Integer.parseInt(strs[1]) * 100000 + Integer.parseInt(strs[2])
				* 1000 + Integer.parseInt(strs[3]) * 10 + Integer.parseInt(charSet);
		return ret;
	}

	public int getMssqlVersion(String host, int port, String userName, String password) {
		int ret = 0;
		String versionStr = "";
		String charSet = "";
		int charsetNum = 0;
		Connection conn = null;

		try {
			conn = ConnectionFactory.createMssqlConnection(host, port, userName, password);
			// charSet = DatabaseVersion.getOracleCharSet(conn, false);
			charSet = DatabaseVersion.getMssqlCharSet(conn, false);
			versionStr = DatabaseVersion.getMssqlVersionText(conn, true);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			throw new ServiceException("获取数据库版本失败：" + e.getMessage());
		} catch (SQLException e) {
			e.printStackTrace();
			throw new ServiceException("获取数据库版本失败：" + e.getMessage());
		}

		// ret = Integer.parseInt(strs[0])*1000000 +
		// Integer.parseInt(strs[1])*10000 + Integer.parseInt(strs[2])*100 +
		// Integer.parseInt(strs[3]);
		ret = Integer.parseInt(versionStr) * 10 + Integer.parseInt(charSet);
		return ret;
	}

	public String getRelatedDbfwNames(int dbId) {
		String sql = "SELECT GROUP_CONCAT(DISTINCT t3.name SEPARATOR ',') "
				+ "FROM dbfw.database_addresses t1, dbfw.dbfw_fordb t2, dbfw.dbfw_instances t3 "
				+ "WHERE t1.database_id=? AND t2.isdelete='0' AND t3.isdelete='0' AND t1.address_id=t2.address_id AND t2.dbfw_inst_id=t3.instance_id";
		Object[] args = { dbId };
		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql, args);
		return this.getJdbcUtils().queryForString(sql, args);
	}

	/**
	 * 判断给定安全实例dbfwId下能否部署deployMode模式（旁路模式不能与其它模式共存）
	 * 
	 * @param dbfwId
	 * @param deployMode
	 * @return
	 */
	private boolean deployModeMatch(Connection conn, int dbfwId, int deployMode) {
		String sql = "SELECT CASE WHEN monitor_type IN (1,3,4) THEN 1 ELSE 2 END AS deployMode "
				+ "FROM dbfw.dbfw_fordb " + "WHERE dbfw_inst_id=? AND isdelete='0' " + "AND address_id is NOT NULL "
				+ "LIMIT 1 ";
		Object[] args = { dbfwId };

		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql, args);
		int currentMode = this.getJdbcUtils().queryForInteger(conn, sql, args);
		if (currentMode == 1 && (deployMode == 1 || deployMode == 3 || deployMode == 4) || currentMode == 2
				&& deployMode == 2 || currentMode == 0) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 判断给定安全实例dbfwId下能否部署deployMode模式（旁路模式不能与其它模式共存）
	 * 
	 * @param dbfwId
	 * @param deployMode
	 * @return
	 */
	private boolean deployModeMatch1(Connection conn, int dbfwId, int deployMode, int addressID) {
		String sql = "SELECT CASE WHEN monitor_type IN (1,3,4) THEN 1 ELSE 2 END AS deployMode "
				+ "FROM dbfw.dbfw_fordb " + "WHERE dbfw_inst_id=? AND isdelete='0' " + "AND address_id is NOT NULL "
				+ "AND database_id <> (SELECT t.database_id FROM database_addresses t WHERE t.address_id=?) "
				+ "LIMIT 1 ";
		Object[] args = { dbfwId, addressID };

		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql, args);
		int currentMode = this.getJdbcUtils().queryForInteger(conn, sql, args);
		if (currentMode == 1 && (deployMode == 1 || deployMode == 3 || deployMode == 4) || currentMode == 2
				&& deployMode == 2 || currentMode == 0) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 判断安全实例是否是开启状态 return 3：停止，！=3开启
	 */
	public int ifDBFWStoped(int dbfwId) {
		String Sql = "SELECT IFNULL(MAX(t1.state),1) AS status  " + "FROM dbfw.dbfw_fordb t1  "
				+ "WHERE t1.isdelete='0'  " + "AND t1.dbfw_inst_id=" + dbfwId + " " + "LIMIT 1";
		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), Sql, new Object[] {});
		return this.getJdbcUtils().queryForInteger(Sql);
	}

	/**
	 * 获取安全实例的名字
	 * 
	 * @param id
	 * @return
	 */
	public String getDBFWName(int id) {
		String sql = "select name from dbfw_instances where instance_id=?";
		Object[] args = { id };
		return this.getJdbcUtils().queryForString(sql, args);
	}

	/**
	 * 获得数据库名称
	 * 
	 * @param id
	 * @return
	 */
	public String getDatabaseName(int id) {
		String sql = "select name from xsec_databases where database_id=?";
		Object[] args = { id };
		return this.getJdbcUtils().queryForString(sql, args);
	}

	/**
	 * 根据数据库地获得数据库名称
	 * 
	 * @param addressId
	 * @return
	 */
	public String getDatabaseNameByaddressId(int addressId) {
		String sql = "select name from xsec_databases where database_id=(select database_id from database_addresses where address_id=?)";
		Object[] args = { addressId };
		return this.getJdbcUtils().queryForString(sql, args);

	}

	/**
	 * 获取mysql版本信息
	 * 
	 * @param host
	 * @param port
	 * @param userName
	 * @param password
	 * @return
	 */
	public int getMysqlVersion(String host, int port, String userName, String password) {
		int ret = 0;
		String versionStr = "";
		String charSet = "";
		int charsetNum = 0;
		Connection conn = null;

		try {
			conn = ConnectionFactory.createMysqlConnection(host, port, "", userName, password);
			charSet = DatabaseVersion.getMysqlCharSet(conn, false);
			versionStr = DatabaseVersion.getMysqlVersionCode(conn, true);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			// throw new ServiceException("获取数据库版本失败：" + e.getMessage());
			throw new ServiceException("获取数据库版本失败!");
		} catch (SQLException e) {
			e.printStackTrace();
			String msg = e.getMessage();
			if (e.getMessage().contains("Connection refused: connect")) {
				msg = "Connection refused: connect";
			} else if (e.getMessage().contains("Connection timed out: connect")) {
				msg = "Connection timed out: connect";
			}
			throw new ServiceException("获取数据库版本失败!");
		}
		String[] strs = versionStr.split("\\.");
		if (strs.length < 1) {
			throw new ServiceException("获取数据库版本时出现异常：无效的版本号格式：" + versionStr);
		}
		if (strs.length == 1) {
			ret = Integer.parseInt(strs[0] + "" + "00" + charSet);
		} else {
			ret = Integer.parseInt(strs[0] + "" + strs[1] + "00" + charSet);
		}
		return ret;
	}

	public String getXsec_databasesDialect(int dbid) {
		String sql = "SELECT dialect FROM `xsec_databases` where database_id=" + dbid;
		return this.getJdbcUtils().queryForString(sql);
	}

	/**
	 * 获取DB2版本信息
	 * 
	 * @param host
	 * @param port
	 * @param sid
	 * @param userName
	 * @param password
	 * @return
	 */
	public int getDB2Version(String host, int port, String sid, String userName, String password) {
		int ret = 0;
		String versionStr = "";
		String charSet = "";
		int charsetNum = 0;
		Connection conn = null;

		try {
			conn = ConnectionFactory.createDB2Connection(host, port, sid, userName, password);
			// charSet=DatabaseVersion.getDB2CharSet(conn, false);
			versionStr = DatabaseVersion.getDB2VersionText(conn, true);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			throw new ServiceException("获取数据库版本失败：" + e.getMessage());
		} catch (SQLException e) {
			e.printStackTrace();
			throw new ServiceException("获取数据库版本失败：" + e.getMessage());
		}
		String[] strs = versionStr.split("\\.");
		if (strs.length < 1) {
			throw new ServiceException("获取数据库版本时出现异常：无效的版本号格式：" + versionStr);
		}
		if (strs.length == 1) {
			ret = Integer.parseInt(strs[0] + "" + "000");
		} else {
			ret = Integer.parseInt(strs[0] + "" + strs[1] + "00");
		}
		return ret;
	}

	/**
	 * 获取configures.properties配置中支持的数据库
	 * 
	 * @return
	 */
	public List<DatabaseInfo> getSupportDatabaseConfigure() {
		List<DatabaseInfo> list = new ArrayList<DatabaseInfo>();
		String dbConfig = Configures.getString("SupportDatabaseConfigure");
		String[] tempArry = dbConfig.split(";");
		for (int i = 0; i < tempArry.length; i++) {
			String[] arry = tempArry[i].split(",");
			DatabaseInfo db = new DatabaseInfo();
			db.setName(arry[0]);
			db.setDesc(arry[1]);
			list.add(db);
		}
		return list;
	}

	public int getDMVersion(String host, int port, String sid, String userName, String password) {
		int ret = 0;
		String versionStr = "";
		String charSet = "";
		Connection conn = null;

		try {
			conn = ConnectionFactory.createDmConnection(host, port, sid, userName, password);
			charSet = DatabaseVersion.getDMCharSet(conn, false);
			versionStr = DatabaseVersion.getDMVersionText(conn, true);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			throw new ServiceException("获取数据库版本失败：" + e.getMessage());
		} catch (SQLException e) {
			e.printStackTrace();
			throw new ServiceException("获取数据库版本失败：" + e.getMessage());
		}
		String ver = versionStr.substring(versionStr.indexOf("V") + 1, versionStr.indexOf("V") + 2);
		if (ver.length() < 1) {
			throw new ServiceException("获取数据库版本时出现异常：无效的版本号格式：" + versionStr);
		} else if (ver.length() == 1) {
			ret = Integer.parseInt(ver + "" + "00" + charSet);
		} else {
			ret = Integer.parseInt(ver.substring(0, 1) + "" + "00" + charSet);
		}
		return ret;
	}

	public int getGbaseVersion(String host, int port, String userName, String password) {
		int ret = 0;
		String versionStr = "";
		String charSet = "";
		int charsetNum = 0;
		Connection conn = null;

		try {
			conn = ConnectionFactory.createGaseConnection(host, port, "", userName, password);
			charSet = DatabaseVersion.getMysqlCharSet(conn, false);
			versionStr = DatabaseVersion.getGbaseVersionCode(conn, true);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			// throw new ServiceException("获取数据库版本失败：" + e.getMessage());
			throw new ServiceException("获取数据库版本失败!");
		} catch (SQLException e) {
			e.printStackTrace();
			String msg = e.getMessage();
			if (e.getMessage().contains("Connection refused: connect")) {
				msg = "Connection refused: connect";
			} else if (e.getMessage().contains("Connection timed out: connect")) {
				msg = "Connection timed out: connect";
			}
			throw new ServiceException("获取数据库版本失败!");
		}
		String[] strs = versionStr.split("\\.");
		if (strs.length < 1) {
			throw new ServiceException("获取数据库版本时出现异常：无效的版本号格式：" + versionStr);
		}
		if (strs.length == 1) {
			ret = Integer.parseInt(strs[0] + "" + "0" + charSet);
		} else {
			ret = Integer.parseInt(strs[0] + "" + strs[1] + "0" + charSet);
		}
		return ret;
	}

	public boolean removeBywayAndDbfwForAddr(Connection conn, DBFWForDbAddr forDbAddr, int addressId) {
		String otherNpcSql = "SELECT COUNT(*) FROM dbfw.dbfw_fordb t1 WHERE t1.dbfw_inst_id=?  AND t1.address_id=? AND t1.isdelete='0'";
		String removeNpcSql = "DELETE FROM dbfw.npc_info WHERE dbfw_inst_id=? AND address_id=?";
		Object[] otherNpcArgs = { forDbAddr.getDbfwId(), addressId };
		Object[] removeNpcArgs = { forDbAddr.getDbfwId(), addressId };
		this
				.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), otherNpcSql,
						otherNpcArgs);
		int cnt = this.getJdbcUtils().queryForInteger(conn, otherNpcSql, otherNpcArgs);
		if (cnt <= 1) {
			this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), removeNpcSql,
					removeNpcArgs);
			this.getJdbcUtils().execute(conn, removeNpcSql, removeNpcArgs);
			return true;
		} else {
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	public List<DBFWForDbAddr> getDbfwForDbAddrsByType(int dbfwId, int dbId, int monitorType) {
		String sql = "SELECT DISTINCT t1.address_id AS addressId, t1.dbfw_inst_id AS dbfwId, t2.name AS dbfwName, t1.state AS state, "
				+ "t1.monitor_type AS monitorType, CASE WHEN t1.monitor_type=2 THEN t5.group_id WHEN t1.monitor_type=4 THEN t5.group_id ELSE t3.group_id END AS groupId, "
				+ "CASE WHEN t1.monitor_type=2 THEN t8.group_name WHEN t1.monitor_type=4 THEN t8.group_name ELSE t4.group_name END  AS groupName, IFNULL(t3.port_id,0) AS portId, "
				+ "IFNULL(t3.port_name,'') AS portName, t1.npc_info_id AS npcInfoId, IFNULL(t5.npc_id,0) AS npcNum, t1.client_group_id AS clientGroupId, IFNULL(t6.name,'') AS clientGroupName, "
				+ "t1.remote_address_id AS remoteId, IFNULL(t7.host,'') AS remoteHost, IFNULL(t7.port,0) AS remotePort "
				+ "FROM dbfw.dbfw_fordb t1 INNER JOIN dbfw.dbfw_instances t2 ON t1.dbfw_inst_id=t2.instance_id AND t1.isdelete='0' AND t2.isdelete='0' "
				+ "LEFT JOIN dbfw.group_port t3 ON t1.port_id=t3.port_id "
				+ "LEFT JOIN interface_group t4 ON t3.group_id=t4.group_id "
				+ "LEFT JOIN dbfw.npc_info t5 ON t5.dbfw_inst_id = t1.dbfw_inst_id AND t5.address_id = t1.address_id AND t5.database_id = t1.database_id "
				+
				// "LEFT JOIN dbfw.npc_info t5 ON t1.npc_info_id=t5.id " +
				"LEFT JOIN interface_group t8 ON t5.group_id=t8.group_id "
				+ "LEFT JOIN dbfw.client_group_info t6 ON t1.client_group_id=t6.id "
				+ "LEFT JOIN dbfw.remote_proxy t7 ON t1.remote_address_id=t7.id "
				+ "WHERE t2.instance_id=? AND t1.address_id IN "
				+ "("
				+ "SELECT address_id FROM dbfw.database_addresses WHERE database_id=? "
				+ ") AND monitor_type= "
				+ monitorType + " ORDER BY addressId";
		Object[] args = { dbfwId, dbId };
		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql, args);
		return (List<DBFWForDbAddr>) this.getJdbcUtils().query(sql, args, DBFWForDbAddr.class);
	}

	public List<DBFWForDbAddr> getDBFWForDbAddrByAddressAndDbfwList(Connection conn, int addressId, int dbfwId) {
		String sql = "SELECT DISTINCT t1.address_id AS addressId, t1.dbfw_inst_id AS dbfwId, t2.name AS dbfwName, t1.state AS state, t1.monitor_type AS monitorType, CASE WHEN t1.monitor_type=2 THEN t5.group_id ELSE t3.group_id END AS groupId, CASE WHEN t1.monitor_type=2 THEN t8.group_name ELSE t4.group_name END  AS groupName, IFNULL(t3.port_id,0) AS portId, "
				+ "IFNULL(t3.port_name,'') AS portName, t1.npc_info_id AS npcInfoId, IFNULL(t5.npc_id,0) AS npcNum, t1.client_group_id AS clientGroupId, IFNULL(t6.name,'') AS clientGroupName, "
				+ "t1.remote_address_id AS remoteId, IFNULL(t7.host,'') AS remoteHost, IFNULL(t7.port,0) AS remotePort "
				+ "FROM dbfw.dbfw_fordb t1 INNER JOIN dbfw.dbfw_instances t2 ON t1.dbfw_inst_id=t2.instance_id AND t1.isdelete='0' AND t2.isdelete='0' "
				+ "LEFT JOIN dbfw.group_port t3 ON t1.port_id=t3.port_id "
				+ "LEFT JOIN interface_group t4 ON t3.group_id=t4.group_id "
				+ "LEFT JOIN dbfw.npc_info t5 ON t5.dbfw_inst_id = t1.dbfw_inst_id AND t5.address_id = t1.address_id AND t5.database_id = t1.database_id "
				+
				// "LEFT JOIN dbfw.npc_info t5 ON t1.npc_info_id=t5.id " +
				"LEFT JOIN interface_group t8 ON t5.group_id=t8.group_id "
				+ "LEFT JOIN dbfw.client_group_info t6 ON t1.client_group_id=t6.id "
				+ "LEFT JOIN dbfw.remote_proxy t7 ON t1.remote_address_id=t7.id "
				+ "WHERE t1.address_id="
				+ addressId
				+ " AND t1.dbfw_inst_id=" + dbfwId;
		// Object[] args = { addressId, dbfwId };
		/*
		 * this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName
		 * (), sql,args);
		 */
		return (List<DBFWForDbAddr>) this.getJdbcUtils().query(conn, sql, DBFWForDbAddr.class);
	}

	public boolean brushOrKillNpc(int dbfwId, int npcId) {
		boolean success = false;
		String sql = "SELECT count(*) FROM `npc_info` WHERE dbfw_inst_id=? and npc_id=?";
		int count = this.getJdbcUtils().queryForInteger(sql, new Object[] { dbfwId, npcId });
		if (count > 1) {
			success = true;
		}
		return success;
	}

	public void insertNoConnect(Connection conn, int dbId, NoSession noSession) {
		String sql = "INSERT INTO databases_unconnect(database_id,start_cli_address,end_cli_address,cli_api,cli_os,db_os,state) VALUES(?,?,?,?,?,?,?)";
		Object[] args = { dbId, noSession.getStartIp(), noSession.getEndIp(), noSession.getSoxSet(),
				noSession.getPciSet(), noSession.getGlbaSet(), 1 };
		this.getJdbcUtils().execute(conn, sql, args);
	}

	public List<NoSession> getNoSessionList(int dbId) {
		String sql = "SELECT id AS id,database_id AS dbId,start_cli_address AS startIp,end_cli_address AS endIp,cli_api AS soxSet,cli_os AS pciSet,db_os AS glbaSet FROM `databases_unconnect` WHERE database_id=?";
		List<NoSession> list = (List<NoSession>) this.getJdbcUtils().query(sql, new Object[] { dbId }, NoSession.class);
		return list;
	}

	public boolean removeNoSession(int id) {
		String sql = "DELETE FROM databases_unconnect WHERE id=" + id;
		boolean success = true;
		Connection conn = null;

		try {
			conn = this.getJdbcUtils().getConnection();
			this.getJdbcUtils().beginTransaction(conn);
			this.getJdbcUtils().execute(conn, sql);
			this.getJdbcUtils().commit(conn);
		} catch (DAOException ex) {
			success = false;
			this.getJdbcUtils().rollback(conn);
			throw ex;
		} finally {
			this.getJdbcUtils().close(conn);
		}
		return success;
	}

	public boolean insertNoConnectOne(int dbId, NoSession noSession) {
		String sql = "INSERT INTO databases_unconnect(database_id,start_cli_address,end_cli_address,cli_api,cli_os,db_os,state) VALUES(?,?,?,?,?,?,?)";
		Object[] args = { dbId, noSession.getStartIp(), noSession.getEndIp(), noSession.getSoxSet(),
				noSession.getPciSet(), noSession.getGlbaSet(), 1 };
		boolean success = true;
		Connection conn = null;
		try {
			conn = this.getJdbcUtils().getConnection();
			this.getJdbcUtils().beginTransaction(conn);
			this.getJdbcUtils().execute(conn, sql, args);
			this.getJdbcUtils().commit(conn);
		} catch (DAOException ex) {
			success = false;
			this.getJdbcUtils().rollback(conn);
			throw ex;
		} finally {
			this.getJdbcUtils().close(conn);
		}
		return success;
	}

	public boolean judgeNoConnect(int dbId, NoSession noSession) {
		String sql = "SELECT count(*) FROM databases_unconnect WHERE database_id=? AND start_cli_address=? AND end_cli_address=? AND cli_api=? AND cli_os=? AND db_os=?";
		Object[] args = { dbId, noSession.getStartIp(), noSession.getEndIp(), noSession.getSoxSet(),
				noSession.getPciSet(), noSession.getGlbaSet() };
		boolean success = true;
		int count = this.getJdbcUtils().queryForInteger(sql, args);
		if (count > 0) {
			success = false;
		}
		return success;
	}

	public List<NpcInfo> getNpcInfoList(int dbfwId, int dbId) {
		String sql = "SELECT DISTINCT group_id FROM npc_info t1, dbfw_fordb t2 WHERE t1.dbfw_inst_id = t2.dbfw_inst_id AND t1.address_id = t2.address_id AND t1.database_id = t2.database_id AND t2.isdelete = 0 AND t1.dbfw_inst_id ="
				+ dbfwId + " AND t1.database_id!=" + dbId;
		final List<NpcInfo> infos = new ArrayList<NpcInfo>();
		this.getJdbcUtils().query(sql, new QueryCallback() {
			public void dealResultSet(ResultSet rs) throws SQLException {
				while (rs.next()) {
					NpcInfo info = new NpcInfo();
					info.setGroupId(rs.getInt("group_id"));
					infos.add(info);
				}
			}
		});
		return infos;
	}

	public String getIfName(int groupId) {
		String sql = "SELECT if_name FROM `interface_info` WHERE group_id=" + groupId;
		return this.getJdbcUtils().queryForString(sql);
	}

	private int getDbTypeVal(Connection conn, int dbId) {
		String sql = "SELECT db_type FROM xsec_databases WHERE database_id=?";
		Object[] args = { dbId };
		this.debugSqlWithClassname(Thread.currentThread().getStackTrace()[1].getMethodName(), sql, args);
		return this.getJdbcUtils().queryForInteger(conn, sql, args);
	}

	/**
	 * 获取Postgres版本信息
	 * 
	 * @param host
	 * @param port
	 * @param userName
	 * @param password
	 * @return
	 */
	public int getPostgresVersion(String host, int port, String sid, String userName, String password) {
		int ret = 0;
		String versionStr = "";
		String charSet = "";
		Connection conn = null;

		try {
			conn = ConnectionFactory.createPostgresConnection(host, port, sid, userName, password);
			charSet = DatabaseVersion.getPostgresSet(conn, false);
			versionStr = DatabaseVersion.getPostgresVersionCode(conn, true);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			throw new ServiceException("获取数据库版本失败!");
		} catch (SQLException e) {
			e.printStackTrace();
			String msg = e.getMessage();
			if (e.getMessage().contains("Connection refused: connect")) {
				msg = "Connection refused: connect";
			} else if (e.getMessage().contains("Connection timed out: connect")) {
				msg = "Connection timed out: connect";
			}
			throw new ServiceException("获取数据库版本失败!");
		}
		if (versionStr.startsWith("9")) {
			ret = Integer.parseInt(90 + charSet);
		} else if (versionStr.startsWith("8")) {
			ret = Integer.parseInt(80 + charSet);
		} else {
			ret = Integer.parseInt(0 + charSet);
		}
		return ret;
	}

	/**
	 * 获取KingBase版本信息
	 * 
	 * @param host
	 * @param port
	 * @param userName
	 * @param password
	 * @return
	 */
	public int getKingBaseVersion(String host, int port, String sid, String userName, String password) {
		int ret = 0;
		String versionStr = "";
		String charSet = "";
		Connection conn = null;

		try {
			conn = ConnectionFactory.createKingBaseConnection(host, port, sid, userName, password);
			charSet = DatabaseVersion.getKingBaseSet(conn, false);
			versionStr = DatabaseVersion.getKingBaseVersionCode(conn, true);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			throw new ServiceException("获取数据库版本失败!");
		} catch (SQLException e) {
			e.printStackTrace();
			String msg = e.getMessage();
			if (e.getMessage().contains("Connection refused: connect")) {
				msg = "Connection refused: connect";
			} else if (e.getMessage().contains("Connection timed out: connect")) {
				msg = "Connection timed out: connect";
			}
			throw new ServiceException("获取数据库版本失败!");
		}
		if (versionStr.startsWith("7")) {
			ret = Integer.parseInt(70 + charSet);
		} else {
			ret = Integer.parseInt(0 + charSet);
		}
		return ret;
	}

	/**
	 * 获取Informix版本信息
	 * 
	 * @param host
	 * @param port
	 * @param userName
	 * @param password
	 * @return
	 */
	public int getInformixVersion(String host, int port, String sid, String userName, String password) {
		int ret = 0;
		String versionStr = "";
		String charSet = "";
		Connection conn = null;

		try {
			conn = ConnectionFactory.createInformixConnection(host, port, sid, userName, password);
			charSet = DatabaseVersion.getInformixSet(conn, false);
			versionStr = DatabaseVersion.getInformixVersionCode(conn, true);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			throw new ServiceException("获取数据库版本失败!");
		} catch (SQLException e) {
			e.printStackTrace();
			String msg = e.getMessage();
			if (e.getMessage().contains("Connection refused: connect")) {
				msg = "Connection refused: connect";
			} else if (e.getMessage().contains("Connection timed out: connect")) {
				msg = "Connection timed out: connect";
			}
			throw new ServiceException("获取数据库版本失败!");
		}
		if (!versionStr.equals("") && versionStr.split(" ").length > 0) {
			versionStr = versionStr.split(" ")[versionStr.split(" ").length - 1];
			if (!versionStr.equals("") && versionStr.split("\\.").length > 0) {
				versionStr = versionStr.split("\\.")[0];
			}
		}
		int version = 0;
		try {
			version = Integer.parseInt(versionStr);
		} catch (Exception e) {
		}

		ret = version * 10 + Integer.parseInt(charSet);

		return ret;
	}

	/**
	 * 获取Gbase 8t版本信息
	 * 
	 * @param host
	 * @param port
	 * @param userName
	 * @param password
	 * @return
	 */
	public int getGbase8tVersion(String host, int port, String sid, String userName, String password) {
		int ret = 0;
		String versionStr = "";
		String charSet = "";
		Connection conn = null;

		try {
			conn = ConnectionFactory.createInformixConnection(host, port, sid, userName, password);
			charSet = DatabaseVersion.getInformixSet(conn, false);
			versionStr = DatabaseVersion.getInformixVersionCode(conn, true);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			throw new ServiceException("获取数据库版本失败!");
		} catch (SQLException e) {
			e.printStackTrace();
			String msg = e.getMessage();
			if (e.getMessage().contains("Connection refused: connect")) {
				msg = "Connection refused: connect";
			} else if (e.getMessage().contains("Connection timed out: connect")) {
				msg = "Connection timed out: connect";
			}
			throw new ServiceException("获取数据库版本失败!");
		}
		if (!versionStr.equals("") && versionStr.split(" ").length > 0) {
			versionStr = versionStr.split(" ")[2];
			if (!versionStr.equals("") && versionStr.split("\\.").length > 0) {
				versionStr = versionStr.split("\\.")[0].replaceAll("V", "").replaceAll("v", "");
			}
		}
		int version = 0;
		try {
			version = Integer.parseInt(versionStr);
		} catch (Exception e) {
		}

		ret = version * 10 + Integer.parseInt(charSet);

		return ret;
	}

	/**
	 * 神通数据库自动获取
	 * 
	 * @param host
	 * @param port
	 * @param sid
	 * @param userName
	 * @param password
	 * @return
	 */
	public int getOscarVersion(String host, int port, String sid, String userName, String password) {
		int ret = 0;
		String versionStr = "";
		Connection conn = null;

		try {
			conn = ConnectionFactory.createOscarConnection(host, port, sid, userName, password);
			versionStr = DatabaseVersion.getOscareVersionCode(conn, true);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			throw new ServiceException("获取数据库版本失败!");
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServiceException("获取数据库版本失败!");
		}
		String tempArgs = versionStr.split("\\.")[0] + "0";
		ret = Integer.parseInt(tempArgs);
		return ret;
	}

	/**
	 * Sysbase数据库自动获取
	 * 
	 * @param host
	 * @param port
	 * @param sid
	 * @param userName
	 * @param password
	 * @return
	 */
	public int getSybaseVersion(String host, int port, String sid, String userName, String password) {
		int ret = 0;
		String versionStr = "";
		String charSet = "1";
		Connection conn = null;
		try {
			conn = ConnectionFactory.createSybaseConnection(host, port, userName, password);
			// charSet = DatabaseVersion.getMssqlCharSet(conn, false);
			versionStr = DatabaseVersion.getSybaseVersionText(conn, true);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			throw new ServiceException("获取数据库版本失败：" + e.getMessage());
		} catch (SQLException e) {
			e.printStackTrace();
			throw new ServiceException("获取数据库版本失败：" + e.getMessage());
		}

		ret = Integer.parseInt(versionStr) * 10 + Integer.parseInt(charSet);
		return ret;
	}

	/**
	 * 判断是否允许添加数据库
	 */
	public void judgeAddDatabases(int num) {
		LicenseInfo licenseInfo = null;
		try {
			// 获取当前已添加的ip:port个数
			int dbAddressCount = this.getJdbcUtils().queryForInteger("SELECT COUNT(1) FROM `database_addresses`");
			// 获取当前license信息
			licenseInfo = SystemLicenseSrever.getLicenseInfoFinal();
			if (licenseInfo != null) {
				if (licenseInfo.getDbcount() == 1) { // 允许添加64个ip:port
					if ((dbAddressCount + num) > 64) {
						throw new DAOException("当前允许添加的数据库实例数(64)已达上限!");
					}
				} else {
					if ((dbAddressCount + num) > licenseInfo.getDbcount()) {
						throw new DAOException("当前允许添加的数据库实例数(" + licenseInfo.getDbcount() + ")已达上限!");
					}
				}
			}
		} catch (Exception e) {
			throw new DAOException(e.getLocalizedMessage());
		}
	}

	/**
	 * 判断是否有npc引用该数据库id
	 * 
	 * @param addresses
	 * @return
	 */
	public int judgeNpcByAdress(List<DatabaseAddress> addresses) {
		int count = 0;
		String temp = "";
		if (addresses != null) {
			for (DatabaseAddress dbAdd : addresses) {
				if (!"".equals(temp)) {
					temp += ",";
				}
				temp += dbAdd.getId();
			}
		}
		if (!"".equals(temp)) {
			String sql = "SELECT count(*) FROM npc_info WHERE address_id in(" + temp + ")";
			count = this.getJdbcUtils().queryForInteger(sql);
		}
		return count;
	}
}
