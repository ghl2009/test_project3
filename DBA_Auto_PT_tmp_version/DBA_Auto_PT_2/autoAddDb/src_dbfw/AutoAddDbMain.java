package cn.schina.dbfw.test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import cn.schina.dbfw.common.lang.DBFWConstant;
import cn.schina.dbfw.dao.BaseDAO;
import cn.schina.dbfw.dao.strategy.DBFWConfigDAO;
import cn.schina.dbfw.db.QueryCallback;
import cn.schina.dbfw.pojo.strategy.DBFWAttr;
import cn.schina.dbfw.pojo.strategy.DBFWForDbAddr;
import cn.schina.dbfw.pojo.strategy.DBFWInfo;
import cn.schina.dbfw.pojo.strategy.DatabaseAddress;
import cn.schina.dbfw.pojo.strategy.DatabaseInfo;
import cn.schina.dbfw.pojo.strategy.InterfaceGroup;
import cn.schina.dbfw.pojo.strategy.NoSession;
import cn.schina.dbfw.service.strategy.DBFWConfigService;
import cn.schina.dbfw.service.strategy.DatabaseConfigService;
import cn.schina.dbfw.service.strategy.NetworkInterfaceService;

public class AutoAddDbMain extends BaseDAO {
	private static AutoAddDbMain addDbMain = null;

	public static AutoAddDbMain getAutoAddDbMain() {
		if (addDbMain == null) {
			addDbMain = new AutoAddDbMain();
		}

		return addDbMain;
	}

	/**
	 * 防火墙自动添加数据库
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		// 创建实例，如果存在返回ID
		int dbfw_inst_id = AutoAddDbMain.getAutoAddDbMain().addDbfw();
		int database_id = 0;
		int groupId = 0;

		// 添加数据库ip端口
		if (System.getProperty("os.name").equals("Linux")) {
			if ((args.length < 13) && ((args.length - 7) % 6 != 0)) {
				return;
			}
			List<Object> argList = new ArrayList<Object>();
			for (int i = 0; i < args.length; i++) {
				argList.add(args[i].equalsIgnoreCase("NULL") ? "" : args[i]);
			}
			database_id = AutoAddDbMain.getAutoAddDbMain().addDatabase(argList.toArray());
			// 添加网卡组，如果存在返回ID
			groupId = AutoAddDbMain.getAutoAddDbMain().addNetWork(argList.toArray());
			// 添加保护
			AutoAddDbMain.getAutoAddDbMain().addDbfwForDbAddrs(dbfw_inst_id, database_id, groupId, argList.toArray());
		} else {
			String[] selfArgs = { "test_oracle", "1", "2", "10020002", "0", "2", "eth4", "192.168.1.77", "1521",
					"orcl", "1", "", "", "192.168.1.76", "1521", "orcl", "1", "", "" };
			database_id = AutoAddDbMain.getAutoAddDbMain().addDatabase(selfArgs);

			// 添加网卡组，如果存在返回ID
			groupId = AutoAddDbMain.getAutoAddDbMain().addNetWork(selfArgs);
			// 添加保护
			AutoAddDbMain.getAutoAddDbMain().addDbfwForDbAddrs(dbfw_inst_id, database_id, groupId, selfArgs);
		}

	}

	/**
	 * 判断安全实例是否存在，如果不存在，添加，实例ID必须为1，否则添加失败
	 * 
	 * @author <b>LCL</b><br/>
	 * @date 2016-05-03
	 * 
	 * @return
	 */
	synchronized private int addDbfw() {
		int dbfw_inst_id = 0;

		DBFWConfigService service = new DBFWConfigService();
		// 判断是否存在实例
		int dbfwId = this.getJdbcUtils()
				.queryForInteger("SELECT MAX(instance_id) as dbfw_inst_id FROM  dbfw_instances");
		int dbfwStatus = 0;
		if (dbfwId == 1) {
			dbfw_inst_id = dbfwId;
			// 判断实例是否启动
			dbfwStatus = this.getJdbcUtils().queryForInteger(
					"SELECT inst_stat FROM  dbfw_instances WHERE instance_id=" + dbfwId);
			// 如果没有启动，启动实例
			if (dbfwStatus != 1) {
				service.setDbfwStatus(dbfwId, 2);
			}
		} else {
			// 添加实例
			DBFWInfo dbfwInfo = new DBFWInfo();
			dbfwInfo.setName("autoTestDbfw");
			dbfwInfo.setPort(9901);
			// 获取并设置参数值
			List<DBFWAttr> attrList = DBFWConfigDAO.getDAO().getDBFWAttrDefaultList();
			for (int i = 0; i < attrList.size(); i++) {
				if (attrList.get(i).getId() == 50) {
					attrList.get(i).setValue("autoTestDbfw");
				}
				if (attrList.get(i).getId() == 225) {
					attrList.get(i).setValue("9901");
				}
				if (attrList.get(i).getId() == 252) {
					attrList.get(i).setValue("3");
				}
			}
			service.addDbfwInstanceAutoTest(dbfwInfo, attrList);

			dbfw_inst_id = 1;
		}
		return dbfw_inst_id;

	}

	/**
	 * <b>根据传入值添加数据库</b>
	 * 
	 * @author <b>LCL</b><br/>
	 * @date 2016-05-03
	 * @param args
	 */
	synchronized private int addDatabase(Object[] args) {
		if (args.length < 10) {
			return -1;
		}
		String name = args[0] + "";// 数据库名
		String type = args[1] + "";// 数据库类型
		String isglba = args[2] + "";// 操作系统
		String version = args[3] + "";// 版本
		String isdpa = args[4] + "";// 字符集

		int dbIfexit = this.getJdbcUtils().queryForInteger(
				"SELECT database_id FROM  xsec_databases WHERE name='" + name + "'");
		if (dbIfexit != 0) {
			return dbIfexit;
		}
		DatabaseInfo dbInfo = new DatabaseInfo();
		dbInfo.setIsdpa(Integer.parseInt(isdpa));
		dbInfo.setIsglba(isglba);
		dbInfo.setIspci("0");
		dbInfo.setIssox("0");
		dbInfo.setName(name);
		dbInfo.setState(0);
		dbInfo.setType(Integer.parseInt(type));
		dbInfo.setVersion(Integer.parseInt(version));
		List<DatabaseAddress> addresses = new ArrayList<DatabaseAddress>();

		String address = "";// 数据库IP
		String port = "";// 数据库端口
		String serviceName = "";// 实例名
		String userName = "";// 用户（Sqlserver）
		String userPwd = "";// 密码（Sqlserver）
		String dynaPort = "";// 是否为静态端口（Oracle 9I）

		for (int i = 7; i < args.length; i += 6) {
			address = args[i] + "";
			port = args[(i + 1)] + "";
			serviceName = args[(i + 2)] + "";
			dynaPort = args[(i + 3)] + "";
			userName = args[(i + 4)] + "";
			userPwd = args[(i + 5)] + "";
			DatabaseAddress databaseAddress = new DatabaseAddress();

			databaseAddress.setAddress(address);
			databaseAddress.setPort(Integer.parseInt(port));
			databaseAddress.setServiceName(serviceName);
			databaseAddress.setUserName(userName);
			databaseAddress.setUserPwd(userPwd);
			databaseAddress.setDynaPort(Integer.parseInt(dynaPort));
			addresses.add(databaseAddress);

		}

		DatabaseConfigService service = new DatabaseConfigService();
		service.addProtectedDatabaseForautoTest(dbInfo, addresses, new ArrayList<NoSession>());
		// 返回数据库ID
		int database_id = this.getJdbcUtils().queryForInteger(
				"SELECT database_id FROM  xsec_databases  WHERE name='" + name + "'");
		return database_id;
	}

	/**
	 * 添加网卡组，返回值为网卡组ID
	 * 
	 * @param args
	 * @return
	 * @throws Exception
	 */
	synchronized private int addNetWork(Object[] args) throws Exception {
		if (args.length < 13) {
			return -1;
		}
		int groupType = "1".equals(args[5]) ? 4 : 2;
		String ifName = args[6].toString();

		// 判断是否是否存在
		int groupId = this.getJdbcUtils().queryForInteger(
				"SELECT IFNULL(group_id,0) as group_id  FROM  interface_info WHERE if_name='" + ifName + "'");
		// 如果存在直接返回网卡组ID
		if (groupId != 0) {
			int groupTypeOld = this.getJdbcUtils().queryForInteger(
					"SELECT group_type FROM  interface_group WHERE group_id=" + groupId);
			if (groupTypeOld != groupType) {
				throw new Exception("传值错误，已存在网卡和传入类型不匹配");
			}
			return groupId;
		}
		int ifId = this.getJdbcUtils().queryForInteger(
				"SELECT if_id FROM  interface_info WHERE if_name='" + ifName + "'");
		String groupName = this.getJdbcUtils().queryForString(
				"SELECT IFNULL(MAX(group_id),0)+1 as id FROM  interface_group ");
		InterfaceGroup group = new InterfaceGroup();
		group.setGroupType(groupType);
		if (groupType == 4) {
			group.setGroupName("旁路组:" + groupName);
		} else {
			group.setGroupName("网桥组:" + groupName);
		}
		NetworkInterfaceService service = new NetworkInterfaceService();
		service.addGroupAndInterfaceForAutoTest(group, ifId);
		groupId = this.getJdbcUtils().queryForInteger(
				"SELECT IFNULL(group_id,0) as group_id  FROM  interface_info WHERE if_name='" + ifName + "'");
		return groupId;

	}

	/**
	 * 数据库实例添加关联
	 * 
	 * @param dbfw_inst_id
	 * @param database_id
	 * @param group_id
	 * @param args
	 */
	synchronized private void addDbfwForDbAddrs(final int dbfw_inst_id, int database_id, final int group_id,
			Object[] args) {
		final int monitorType = "1".equals(args[5]) ? DBFWConstant.DEPLOY_BYWAY : DBFWConstant.DEPLOY_HALFTRANS;
		final List<DBFWForDbAddr> dbAddrs = new ArrayList<DBFWForDbAddr>();
		String sql = "SELECT address_id FROM  database_addresses WHERE database_id=" + database_id;
		this.getJdbcUtils().query(sql, new QueryCallback() {
			public void dealResultSet(ResultSet rs) throws SQLException {
				while (rs.next()) {
					DBFWForDbAddr address = new DBFWForDbAddr();
					address.setAddressId(rs.getInt("address_id"));
					address.setDbfwId(dbfw_inst_id);
					address.setGroupIdStr(group_id + "");
					address.setMonitorType(monitorType);
					dbAddrs.add(address);
				}
			}
		});
		// 添加被保护数据库
		DatabaseConfigService service = new DatabaseConfigService();
		service.addDbfwForDbAddrsForAutoTest(dbAddrs);
	}
}
