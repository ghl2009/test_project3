package cn.dbsec.auto;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;

import cn.dbsec.dbfw.pojo.database.DatabaseAddress;
import cn.dbsec.dbfw.pojo.database.DatabaseInfo;
import cn.dbsec.dbfw.service.network.NetworkService;
import cn.dbsec.net.bean.GroupPort;
import cn.dbsec.net.bean.InterfaceGroup;
import cn.schina.dbfw.dao.BaseDAO;

public class AutoAddDbMain extends BaseDAO {
	private DatabaseServiceAuto databaseService = new DatabaseServiceAuto();
	private NetworkService networkService = new NetworkService();
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
		int database_id = 0;
		int groupId = 0;

		// 添加数据库ip端口
		if (SystemUtils.IS_OS_LINUX) {
			if ((args.length < 15) && ((args.length - 9) % 6 != 0)) {
				return;
			}
			int monitorType = "1".equals(args[5]) ? 2 : 4;
			if (StringUtils.equals(args[5], "3")) {
				monitorType = 3;
			}
			List<Object> argList = new ArrayList<Object>();
			for (int i = 0; i < args.length; i++) {
				argList.add(args[i].equalsIgnoreCase("NULL") ? "" : args[i]);
			}
			// 添加网卡组，如果存在返回ID
			groupId = AutoAddDbMain.getAutoAddDbMain().addNetWork(argList.toArray());

			database_id = AutoAddDbMain.getAutoAddDbMain().addDatabase(argList.toArray(), groupId, monitorType);
		} else {
			String[] selfArgs = { "test_oracle0", "1", "2", "10020002", "0", "2", "eth4", "", "", "192.168.11.171",
					"1521", "orcl", "0", "", "", "192.168.11.170", "1521", "orcl", "0", "", "" };
			// 添加网卡组，如果存在返回ID
			// 部署模式；1-远程代理，2-旁路监听，3-本地代理，4-网桥（半透明）
			int monitorType = "1".equals(selfArgs[5]) ? 2 : 4;
			if (StringUtils.equals(selfArgs[5], "3")) {
				monitorType = 3;
			}
			groupId = AutoAddDbMain.getAutoAddDbMain().addNetWork(selfArgs);

			database_id = AutoAddDbMain.getAutoAddDbMain().addDatabase(selfArgs, groupId, monitorType);
		}
		System.out.println("-----DatabaseId-------" + database_id);
	}

	/**
	 * <b>根据传入值添加数据库</b>
	 * 
	 * @author <b>LCL</b><br/>
	 * @date 2016-05-03
	 * @param args
	 * @throws Exception
	 */
	synchronized private int addDatabase(Object[] args, int groupId, int monitorType) throws Exception {
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
		// dbInfo.setType(Integer.parseInt(type));
		dbInfo.setDialect(DBEnum.getDBEnumByDbType(Integer.parseInt(type)).dialect);
		dbInfo.setTypeText(DBEnum.getDBEnumByDbType(Integer.parseInt(type)).typeName);
		dbInfo.setVersion(Integer.parseInt(version));
		dbInfo.setMonitorType(monitorType);
		InterfaceGroup interfaceGroup = null;
		if (monitorType == 3) {
			String ifName = args[6].toString();
			int ifId = this.getJdbcUtils().queryForInteger(
					"SELECT if_id FROM  interface_info WHERE if_name='" + ifName + "'");
			interfaceGroup = networkService.getInterfaceGroup(groupId, ifId);
		}
		List<DatabaseAddress> addresses = new ArrayList<DatabaseAddress>();

		String address = "";// 数据库IP
		String port = "";// 数据库端口
		String serviceName = "";// 实例名
		String userName = "";// 用户（Sqlserver）
		String userPwd = "";// 密码（Sqlserver）
		String dynaPort = "";// 是否为静态端口（Oracle 9I）
		for (int i = 9; i < args.length; i += 6) {
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
			databaseAddress.setGroupIdStr(groupId + "");
			if (interfaceGroup != null && monitorType == 3) {
				boolean ifAddPort = false;
				for (GroupPort groupPort : interfaceGroup.getGroupPortList()) {
					if (groupPort.getUsed() == 0) {
						databaseAddress.setGroupIp(interfaceGroup.getGroupIp());
						databaseAddress.setPortNum(groupPort.getPortNum());
						databaseAddress.setPortId(groupPort.getId());
						databaseAddress.setPortName("代理组" + groupPort.getPortNum());
						databaseAddress.setGroupId(groupId);
						groupPort.setUsed(1);// 已使用状态
						ifAddPort = true;
						break;
					}
				}
				if (!ifAddPort) {
					throw new Exception("代理组无可用端口");
				}
			}
			databaseAddress.setDynaPort(Integer.parseInt(dynaPort));
			addresses.add(databaseAddress);
		}
		dbInfo.setDbAddrList(addresses);
		dbInfo.setRunMode(1);// 代理组默认保护模式
		int database_id = databaseService.saveDataBase(dbInfo);
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
		if (args.length < 15) {
			return -1;
		}
		// 网卡组类型；1-管理接口；2-网桥组；3-代理组；4-旁路组
		int groupType = "1".equals(args[5]) ? 4 : 2;
		// 代理组判断
		if (StringUtils.equals(args[5].toString(), "3")) {
			groupType = 3;
		}

		String ifName = args[6].toString();

		// 代理IP
		String proxyIp = args[7].toString();

		// 代理IP
		String proxyMask = args[8].toString();

		// 判断是否是否存在
		int groupId = this.getJdbcUtils().queryForInteger(
				"SELECT IFNULL(group_id,0) as group_id  FROM  interface_info WHERE if_name='" + ifName + "'");
		// 如果存在直接返回网卡组ID
		if (groupId != 0) {
			int groupTypeOld = this.getJdbcUtils().queryForInteger(
					"SELECT group_type FROM interface_group WHERE group_id=" + groupId);
			if (groupTypeOld != groupType) {
				throw new Exception("传值错误，已存在网卡和传入类型不匹配");
			}
			return groupId;
		}
		int ifId = this.getJdbcUtils().queryForInteger(
				"SELECT if_id FROM  interface_info WHERE if_name='" + ifName + "'");
		if (ifId == 0) {
			throw new Exception("网卡不存在!");
		}
		String groupName = this.getJdbcUtils().queryForString(
				"SELECT IFNULL(MAX(group_id),0)+1 as id FROM  interface_group ");
		InterfaceGroup group = new InterfaceGroup();
		group.setGroupType(groupType);
		if (groupType == 4) {
			group.setGroupName("旁路组:" + groupName);
		} else if (groupType == 2) {
			group.setGroupName("网桥组:" + groupName);
		} else if (groupType == 3) {
			group.setGroupName("代理组:" + groupName);
		}
		group.setIfId(ifId);
		group.setIfName(ifName);
		group.setGroupEnable(1);

		if (groupType == 3) {
			group.setGroupIp(proxyIp);
			group.setGroupMask(proxyMask);
			List<GroupPort> groupPortList = new ArrayList<GroupPort>();
			for (int i = 0; i <= 8; i++) {
				GroupPort groupPort = new GroupPort();
				groupPort.setEnable(1);
				groupPort.setGroupId(0);
				groupPort.setPortNum(10000 + i);
				groupPort.setName("代理" + group.getIfName() + ":" + groupPort.getPortNum());
				groupPort.setStatus(0);
				groupPort.setUsed(0);
				groupPortList.add(groupPort);
			}
			group.setGroupPortList(groupPortList);
		}
		// 网桥组查询另外的网卡
		if (groupType == 2) {
			String ifIdBridgeSql = "SELECT (CASE WHEN masterIf=" + ifId + " THEN slaveIf WHEN slaveIf=" + ifId
					+ " THEN masterIf ELSE 0 END ) as ifId   FROM  bypassnc WHERE (masterIf=" + ifId + " OR slaveIf="
					+ ifId + ")";
			int ifIdBridge = this.getJdbcUtils().queryForInteger(ifIdBridgeSql);
			if (ifIdBridge > 0) {
				String ifNameBridgeSql = "SELECT if_name FROM  interface_info WHERE if_id=" + ifIdBridge;
				String ifNameBridge = this.getJdbcUtils().queryForString(ifNameBridgeSql);
				group.setIfIdBridge(ifIdBridge);
				group.setIfNameBridge(ifNameBridge);
			}
		}
		networkService.saveInterfaceGroup(group);
		groupId = this.getJdbcUtils().queryForInteger(
				"SELECT IFNULL(group_id,0) as group_id FROM  interface_info WHERE if_name='" + ifName + "'");

		return groupId;

	}
}
