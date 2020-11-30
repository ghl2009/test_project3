package cn.schina.dbfw.service.strategy;

import java.util.ArrayList;
import java.util.List;

import cn.schina.dbfw.base.tools.Tools;
import cn.schina.dbfw.common.lang.DBFWConstant;
import cn.schina.dbfw.common.lang.ServiceException;
import cn.schina.dbfw.dao.strategy.HaConfigDao;
import cn.schina.dbfw.dao.strategy.NetworkInterfaceDAO;
import cn.schina.dbfw.pojo.strategy.BridgeInterfaceInfo;
import cn.schina.dbfw.pojo.strategy.BywayInterfaceInfo;
import cn.schina.dbfw.pojo.strategy.ClientGroupItem;
import cn.schina.dbfw.pojo.strategy.CommonInterfaceInfo;
import cn.schina.dbfw.pojo.strategy.GroupPort;
import cn.schina.dbfw.pojo.strategy.HaConfigInfo;
import cn.schina.dbfw.pojo.strategy.InterfaceGroup;
import cn.schina.dbfw.pojo.strategy.InterfaceInfo;
import cn.schina.dbfw.pojo.strategy.ManageInterfaceInfo;
import cn.schina.dbfw.pojo.strategy.ProxyInterfaceInfo;
import cn.schina.dbfw.pojo.strategy.RouteConfig;
import cn.schina.dbfw.service.strategy.command.ArpPingCommand;
import cn.schina.dbfw.service.strategy.command.GetInterfaceListCommand;
import cn.schina.dbfw.service.strategy.command.NativeCommand;
import cn.schina.dbfw.service.strategy.command.NativeExecutor;
import cn.schina.dbfw.util.SystemAuditResultSingelton;

public class NetworkInterfaceService {

	private NetworkInterfaceDAO networkInterfaceDAO = NetworkInterfaceDAO.getDAO();

	/**
	 * 获取网卡信息
	 * 
	 * @return 所有的网卡信息
	 */
	public List<InterfaceInfo> getInterfaceInfo() {
		return networkInterfaceDAO.getInterfaceInfo();
	}

	/**
	 * 将当前网卡的信息刷新至策略中心
	 */
	public void loadInterfaceInfo() {
		List<NativeCommand> commands = new ArrayList<NativeCommand>();
		NativeCommand command = new GetInterfaceListCommand();
		commands.add(command);
		String result = "";
		try {
			result = "1";
			NativeExecutor.execute(commands);

		} catch (Exception e) {
			result = "2";
			e.printStackTrace();
			throw new ServiceException("刷新网卡失败 " + e.getMessage());
		} finally {
			SystemAuditResultSingelton.getInstance().insertSystemAudit(7, 1, 1, 3, result, "刷新网卡");
		}
	}

	/**
	 * 获取可添加网卡的网卡组
	 * 
	 * @return
	 */
	public List<InterfaceGroup> getIncompleteInterfaceGroup() {
		return networkInterfaceDAO.getIncompleteInterfaceGroup();
	}

	/**
	 * 获取一个可用的网卡组名
	 * 
	 * @param groupType
	 * @return
	 */
	public String generateGroupName(int groupType) {
		if (groupType < DBFWConstant.IFGROUPTYPE_BRIDGE || groupType > DBFWConstant.IFGROUPTYPE_BYPASS) {
			throw new ServiceException("未知的网卡组类型");
		}
		return networkInterfaceDAO.generateGroupName(groupType);
	}

	/**
	 * 添加网卡组并添加网卡
	 * 
	 * @param group
	 * @param ifId
	 */
	public void addGroupAndInterface(InterfaceGroup group, int ifId) {
		// group.setGroupName(networkInterfaceDAO.generateGroupName(group.getGroupType()));
		String result = "";
		String message = "";
		if (group != null) {
			if (group.getGroupType() == 1) {
				message = "管理接口";
			} else if (group.getGroupType() == 2) {
				message = "网桥组";
			} else if (group.getGroupType() == 3) {
				message = "代理组";
			} else if (group.getGroupType() == 4) {
				message = "旁路组";
			}
		}
		try {
			if (Tools.StringFilter(group.getGroupName().trim()).equals("")) {
				throw new ServiceException("组名称为空或包含特殊字符");
			}
			result = "1";
			networkInterfaceDAO.addGroupAndInterface(Tools.StringFilter(group.getGroupName().trim()), group
					.getGroupType(), group.getGroupIp(), group.getGroupMask(), group.getGroupGateway(), group
					.getGroupEnable(), ifId);
		} catch (Exception e) {
			result = "2";
			e.printStackTrace();
			throw new ServiceException(message + ":添加" + group.getGroupName() + "失败 " + e.getMessage());
		} finally {
			SystemAuditResultSingelton.getInstance().insertSystemAudit(7, 1, 1, 1, result,
					message + ":添加" + group.getGroupName());
		}
	}

	public void addGroupAndInterfaceForAutoTest(InterfaceGroup group, int ifId) {
		// group.setGroupName(networkInterfaceDAO.generateGroupName(group.getGroupType()));
		String message = "";
		if (group != null) {
			if (group.getGroupType() == 1) {
				message = "管理接口";
			} else if (group.getGroupType() == 2) {
				message = "网桥组";
			} else if (group.getGroupType() == 3) {
				message = "代理组";
			} else if (group.getGroupType() == 4) {
				message = "旁路组";
			}
		}
		try {
			if (Tools.StringFilter(group.getGroupName().trim()).equals("")) {
				throw new ServiceException("组名称为空或包含特殊字符");
			}
			networkInterfaceDAO.addGroupAndInterface(Tools.StringFilter(group.getGroupName().trim()), group
					.getGroupType(), group.getGroupIp(), group.getGroupMask(), group.getGroupGateway(), group
					.getGroupEnable(), ifId);
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServiceException(message + ":添加" + group.getGroupName() + "失败 " + e.getMessage());
		}
	}

	/**
	 * 将网卡添加至指定网卡组
	 * 
	 * @param groupId
	 * @param ifId
	 */
	public void addGroupInterface(int groupId, int ifId) {
		InterfaceGroup group = networkInterfaceDAO.getGroupById(groupId);
		String result = "";
		try {
			result = "1";
			networkInterfaceDAO.addGroupInterface(groupId, ifId);
		} catch (Exception e) {
			result = "2";
			e.printStackTrace();
			throw new ServiceException("已有网卡组:添加" + group.getGroupName() + "失败 " + e.getMessage());
		} finally {
			SystemAuditResultSingelton.getInstance().insertSystemAudit(7, 1, 1, 1, result,
					"已有网卡组:添加" + group.getGroupName());
		}
	}

	/**
	 * 将网卡从指定网卡组移除
	 * 
	 * @param groupId
	 * @param ifId
	 */
	public void removeInterfaceFromGroup(int groupId, int ifId) {
		InterfaceGroup group = networkInterfaceDAO.getGroupById(groupId);
		InterfaceInfo info = networkInterfaceDAO.getInterfaceInfoById(ifId);
		String ifName = "";
		String result = "";
		if ("eth0".equals(info.getIfName())) {
			ifName = "Mgt";
		} else if ("eth1".equals(info.getIfName())) {
			ifName = "HA";
		} else if ("eth2".equals(info.getIfName())) {
			ifName = "E2";
		} else if ("eth3".equals(info.getIfName())) {
			ifName = "E3";
		} else if ("eth4".equals(info.getIfName())) {
			ifName = "E4";
		} else if ("eth5".equals(info.getIfName())) {
			ifName = "E5";
		} else if ("eth6".equals(info.getIfName())) {
			ifName = "E6";
		} else if ("eth7".equals(info.getIfName())) {
			ifName = "E7";
		}
		try {
			result = "1";
			List<NativeCommand> commands = new ArrayList<NativeCommand>();
			networkInterfaceDAO.removeInterfaceFromGroup(groupId, ifId, commands);
			// NativeExecutor.execute(commands);

		} catch (Exception e) {
			result = "2";
			e.printStackTrace();
			throw new ServiceException("从" + ifName + "移除网卡组：" + group.getGroupName() + "失败 " + e.getMessage());
		} finally {
			SystemAuditResultSingelton.getInstance().insertSystemAudit(7, 1, 1, 2, result,
					"从" + ifName + "移除网卡组：" + group.getGroupName());
		}
	}

	/**
	 * 获取管理组信息
	 * 
	 * @return
	 */
	public ManageInterfaceInfo getManageInterfaceInfo() {
		ManageInterfaceInfo manageInterfaceInfo = new ManageInterfaceInfo();

		InterfaceGroup manageGroup = networkInterfaceDAO.getGroupById(1);
		if (manageGroup != null)
			manageInterfaceInfo.setGroup(manageGroup);
		else
			manageInterfaceInfo.setGroup(new InterfaceGroup());

		List<InterfaceInfo> manageInterface = networkInterfaceDAO.getInterfaceByType(DBFWConstant.IFGROUPTYPE_MANAGE);
		if (manageInterface.size() > 0)
			manageInterfaceInfo.setInterfaceInfo(networkInterfaceDAO
					.getInterfaceByType(DBFWConstant.IFGROUPTYPE_MANAGE).get(0));
		else
			manageInterfaceInfo.setInterfaceInfo(new InterfaceInfo());

		manageInterfaceInfo.setGroupPorts(networkInterfaceDAO.getPortsByGroup(1));

		manageInterfaceInfo.setUsed(networkInterfaceDAO.checkGroupInterfaceIsUsed(manageGroup.getGroupId()));

		return manageInterfaceInfo;
	}

	/**
	 * PING指定的IP地址
	 * 
	 * @param ifName1
	 * @param ifName2
	 * @param ip
	 * @return
	 */
	public boolean ping(String ifName1, String ifName2, String ip) {
		List<NativeCommand> commands = new ArrayList<NativeCommand>();
		NativeCommand command = new ArpPingCommand(ip, ifName1);
		commands.add(command);
		int ret = NativeExecutor.execute(commands);
		if (ret == 1) {
			return false;
		} else if (ifName2 != null && !ifName2.equals("")) {
			commands.clear();
			command = new ArpPingCommand(ip, ifName2);
			commands.add(command);
			ret = NativeExecutor.execute(commands);
			if (ret == 0)
				return true;
			else
				return false;
		} else {
			return true;
		}
	}

	/**
	 * 保存网卡组的信息
	 * 
	 * @param group
	 * @param deletePortIds
	 * @param addPorts
	 */
	public void saveGroupInfo(InterfaceGroup group, List<GroupPort> changedPorts) {
		List<NativeCommand> commands = new ArrayList<NativeCommand>();
		String result = "";
		String message = "";
		if (group.getGroupType() == 1) {
			message = "管理接口";
		} else if (group.getGroupType() == 2) {
			message = "网桥组";
		} else if (group.getGroupType() == 3) {
			message = "代理组";
		}
		try {
			result = "1";
			networkInterfaceDAO.saveGroupInfo(group, changedPorts, commands);
			// NativeExecutor.execute(commands);
		} catch (Exception e) {
			result = "2";
			e.printStackTrace();
			throw new ServiceException("保存" + message + "：" + group.getGroupName() + "失败 " + e.getMessage());
		} finally {
			if (group.getGroupType() == 1) {
				SystemAuditResultSingelton.getInstance().insertSystemAudit(7, 1, 1, 4, result,
						"保存" + message + "：" + group.getGroupName());
			} else if (group.getGroupType() == 2) {
				SystemAuditResultSingelton.getInstance().insertSystemAudit(7, 1, 1, 5, result,
						"保存" + message + "：" + group.getGroupName());
			} else if (group.getGroupType() == 3) {
				SystemAuditResultSingelton.getInstance().insertSystemAudit(7, 1, 1, 7, result,
						"保存" + message + "：" + group.getGroupName());
			}
		}
	}

	/**
	 * 保存旁路网卡组的信息
	 * 
	 * @param group
	 * @param clientGroupItems
	 */
	public void saveBywayGroupInfo(InterfaceGroup group, List<ClientGroupItem> clientGroupItems) {
		String result = "";
		try {
			result = "1";
			networkInterfaceDAO.saveGroupInfo(group, clientGroupItems);
		} catch (Exception e) {
			result = "2";
			e.printStackTrace();
			throw new ServiceException("保存旁路组：" + group.getGroupName() + "失败 " + e.getMessage());
		} finally {
			SystemAuditResultSingelton.getInstance().insertSystemAudit(7, 1, 1, 9, result,
					"保存旁路组：" + group.getGroupName());
		}
	}

	public List<BridgeInterfaceInfo> getBridgeInterfaceInfos() {
		List<BridgeInterfaceInfo> retList = new ArrayList<BridgeInterfaceInfo>();

		List<InterfaceGroup> bridgeGroups = networkInterfaceDAO.getGroupByType(DBFWConstant.IFGROUPTYPE_BRIDGE);

		for (InterfaceGroup interfaceGroup : bridgeGroups) {
			BridgeInterfaceInfo bridgeInterfaceInfo = new BridgeInterfaceInfo();
			bridgeInterfaceInfo.setGroup(interfaceGroup);

			List<InterfaceInfo> interfaceInfos = networkInterfaceDAO.getInterfaceByGroup(interfaceGroup.getGroupId());
			if (interfaceInfos.size() == 0) {
				bridgeInterfaceInfo.setIf1Info(new InterfaceInfo());
				bridgeInterfaceInfo.setIf2Info(new InterfaceInfo());
			} else if (interfaceInfos.size() == 1) {
				bridgeInterfaceInfo.setIf1Info(interfaceInfos.get(0));
				bridgeInterfaceInfo.setIf2Info(new InterfaceInfo());
			} else {
				bridgeInterfaceInfo.setIf1Info(interfaceInfos.get(0));
				bridgeInterfaceInfo.setIf2Info(interfaceInfos.get(1));
			}

			bridgeInterfaceInfo.setGroupPorts(networkInterfaceDAO.getPortsByGroup(interfaceGroup.getGroupId()));

			bridgeInterfaceInfo.setUsed(networkInterfaceDAO.checkGroupInterfaceIsUsed(interfaceGroup.getGroupId()));

			retList.add(bridgeInterfaceInfo);
		}

		return retList;
	}

	/**
	 * 移除网卡组
	 * 
	 * @param groupId
	 */
	public void removeGroup(int groupId) {
		List<NativeCommand> commands = new ArrayList<NativeCommand>();
		InterfaceGroup group = networkInterfaceDAO.getGroupById(groupId);
		String result = "";
		String message = "";
		if (group != null) {
			if (group.getGroupType() == 2) {
				message = "网桥组";
			} else if (group.getGroupType() == 3) {
				message = "代理组";
			} else if (group.getGroupType() == 4) {
				message = "旁路组";
			}
		}
		try {
			result = "1";
			networkInterfaceDAO.removeInterfaceGroup(groupId, commands);
			// NativeExecutor.execute(commands);
		} catch (Exception e) {
			result = "2";
			e.printStackTrace();
			throw new ServiceException(message + "删除组:" + group.getGroupName() + "失败 " + e.getMessage());
		} finally {
			if (group.getGroupType() == 2) {
				SystemAuditResultSingelton.getInstance().insertSystemAudit(7, 1, 1, 5, result,
						message + "删除组:" + group.getGroupName());

			} else if (group.getGroupType() == 3) {
				SystemAuditResultSingelton.getInstance().insertSystemAudit(7, 1, 1, 8, result,
						message + "删除组:" + group.getGroupName());
			} else if (group.getGroupType() == 4) {
				SystemAuditResultSingelton.getInstance().insertSystemAudit(7, 1, 1, 10, result,
						message + "删除组:" + group.getGroupName());
			}
		}
	}

	/**
	 * 启用/停用网卡组
	 * 
	 * @param groupId
	 * @param start
	 */
	public void startupGroup(int groupId, boolean start) {
		List<NativeCommand> commands = new ArrayList<NativeCommand>();
		networkInterfaceDAO.updateGroupStatus(groupId, (start ? 1 : 0), commands);
		// NativeExecutor.execute(commands);
	}

	/**
	 * 获取所有代理组信息
	 * 
	 * @return
	 */
	public List<ProxyInterfaceInfo> getProxyInterfaceInfo() {
		List<ProxyInterfaceInfo> retList = new ArrayList<ProxyInterfaceInfo>();

		List<InterfaceGroup> proxyGroups = networkInterfaceDAO.getGroupByType(DBFWConstant.IFGROUPTYPE_PROXY);
		// 获取高可用是否可以用
		boolean haStart = false;
		if (proxyGroups != null) {
			if (proxyGroups.size() > 0) {
				haStart = HaConfigDao.getDAO().ifHaStart();
			}
		}
		for (InterfaceGroup interfaceGroup : proxyGroups) {
			ProxyInterfaceInfo proxyInterfaceInfo = new ProxyInterfaceInfo();
			proxyInterfaceInfo.setGroup(interfaceGroup);

			List<InterfaceInfo> interfaceInfos = networkInterfaceDAO.getInterfaceByGroup(interfaceGroup.getGroupId());
			if (interfaceInfos.size() == 0) {
				proxyInterfaceInfo.setInterfaceInfo(new InterfaceInfo());
			} else {
				proxyInterfaceInfo.setInterfaceInfo(interfaceInfos.get(0));
			}

			proxyInterfaceInfo.setGroupPorts(networkInterfaceDAO.getPortsByGroup(interfaceGroup.getGroupId()));

			proxyInterfaceInfo.setUsed(networkInterfaceDAO.checkGroupInterfaceIsUsed(interfaceGroup.getGroupId()));
			proxyInterfaceInfo.getInterfaceInfo().setHaIfStart(haStart);
			retList.add(proxyInterfaceInfo);
		}

		return retList;
	}

	/**
	 * 获取所有旁路组信息
	 * 
	 * @return
	 */
	public List<BywayInterfaceInfo> getBywayInterfaceInfo() {
		List<BywayInterfaceInfo> retList = new ArrayList<BywayInterfaceInfo>();

		List<InterfaceGroup> bywayGroups = networkInterfaceDAO.getGroupByType(DBFWConstant.IFGROUPTYPE_BYPASS);

		for (InterfaceGroup interfaceGroup : bywayGroups) {
			BywayInterfaceInfo bywayInterfaceInfo = new BywayInterfaceInfo();
			bywayInterfaceInfo.setGroup(interfaceGroup);

			List<InterfaceInfo> interfaceInfos = networkInterfaceDAO.getInterfaceByGroup(interfaceGroup.getGroupId());
			if (interfaceInfos.size() == 0) {
				bywayInterfaceInfo.setInterfaceInfo(new InterfaceInfo());
			} else {
				bywayInterfaceInfo.setInterfaceInfo(interfaceInfos.get(0));
			}

			// bywayInterfaceInfo.setGroupPorts(networkInterfaceDAO.getPortsByGroup(interfaceGroup.getGroupId()));
			bywayInterfaceInfo.setClientGroupItems(networkInterfaceDAO.getClientItemsByGroup(interfaceGroup
					.getGroupId()));

			bywayInterfaceInfo.setUsed(networkInterfaceDAO.checkGroupInterfaceIsUsed(interfaceGroup.getGroupId()));

			retList.add(bywayInterfaceInfo);
		}

		return retList;
	}

	/**
	 * 获取所有可用的网桥组信息
	 * 
	 * @return
	 */
	public List<CommonInterfaceInfo> getBridgeInterfaceInfosAvailable() {
		List<CommonInterfaceInfo> retList = new ArrayList<CommonInterfaceInfo>();

		List<InterfaceGroup> commonGroups = networkInterfaceDAO
				.getGroupAvailableByType(DBFWConstant.IFGROUPTYPE_BRIDGE);

		for (InterfaceGroup interfaceGroup : commonGroups) {
			CommonInterfaceInfo commonInterfaceInfo = new CommonInterfaceInfo();
			commonInterfaceInfo.setGroup(interfaceGroup);

			commonInterfaceInfo
					.setGroupPorts(networkInterfaceDAO.getPortsAvailableByGroup(interfaceGroup.getGroupId()));

			retList.add(commonInterfaceInfo);
		}

		return retList;
	}

	/**
	 * 获取所有可用的代理组信息
	 * 
	 * @return
	 */
	public List<CommonInterfaceInfo> getProxyInterfaceInfosAvailable() {
		List<CommonInterfaceInfo> retList = new ArrayList<CommonInterfaceInfo>();

		List<InterfaceGroup> commonGroups = networkInterfaceDAO.getGroupAvailableByType(DBFWConstant.IFGROUPTYPE_PROXY);

		for (InterfaceGroup interfaceGroup : commonGroups) {
			CommonInterfaceInfo commonInterfaceInfo = new CommonInterfaceInfo();
			commonInterfaceInfo.setGroup(interfaceGroup);

			commonInterfaceInfo
					.setGroupPorts(networkInterfaceDAO.getPortsAvailableByGroup(interfaceGroup.getGroupId()));

			retList.add(commonInterfaceInfo);
		}

		return retList;
	}

	/**
	 * 获取所有可用的旁路组信息
	 * 
	 * @return
	 */
	public List<CommonInterfaceInfo> getBywayInterfaceInfosAvailable() {
		List<CommonInterfaceInfo> retList = new ArrayList<CommonInterfaceInfo>();

		List<InterfaceGroup> commonGroups = networkInterfaceDAO
				.getGroupAvailableByType(DBFWConstant.IFGROUPTYPE_BYPASS);

		for (InterfaceGroup interfaceGroup : commonGroups) {
			CommonInterfaceInfo commonInterfaceInfo = new CommonInterfaceInfo();
			commonInterfaceInfo.setGroup(interfaceGroup);

			commonInterfaceInfo
					.setGroupPorts(networkInterfaceDAO.getPortsAvailableByGroup(interfaceGroup.getGroupId()));

			retList.add(commonInterfaceInfo);
		}

		return retList;
	}

	/**
	 * 获取指定网卡组下可用端口的信息
	 * 
	 * @param groupId
	 *            网卡组ID
	 * @return 列表，元素为GroupPort实例
	 */
	public List<GroupPort> getPortsAvailableByGroupId(int groupId) {
		return networkInterfaceDAO.getPortsAvailableByGroup(groupId);
	}

	public static void main(String[] args) {
		NetworkInterfaceService service = new NetworkInterfaceService();
		/*
		 * // getInterfaceInfo System.out.println(service.getInterfaceInfo());
		 * 
		 * // getIncompleteInterfaceGroup
		 * System.out.println(service.getIncompleteInterfaceGroup());
		 * 
		 * // addGroupAndInterface InterfaceGroup iGroup = new InterfaceGroup();
		 * iGroup.setGroupType(DBFWConstant.IFGROUPTYPE_PROXY);
		 * service.addGroupAndInterface(iGroup, 2);
		 * 
		 * iGroup.setGroupType(DBFWConstant.IFGROUPTYPE_PROXY);
		 * service.addGroupAndInterface(iGroup, 3);
		 * 
		 * iGroup.setGroupType(DBFWConstant.IFGROUPTYPE_BYPASS);
		 * service.addGroupAndInterface(iGroup, 4);
		 * 
		 * iGroup.setGroupType(DBFWConstant.IFGROUPTYPE_BRIDGE);
		 * service.addGroupAndInterface(iGroup, 5);
		 * 
		 * System.out.println(service.getInterfaceInfo());
		 * System.out.println(service.getIncompleteInterfaceGroup());
		 * 
		 * // addGroupInterface service.addGroupInterface(16, 6);
		 * 
		 * System.out.println(service.getInterfaceInfo());
		 * System.out.println(service.getIncompleteInterfaceGroup());
		 * 
		 * // removeInterfaceFromGroup service.removeInterfaceFromGroup(16, 6);
		 * System.out.println(service.getInterfaceInfo());
		 * System.out.println(service.getIncompleteInterfaceGroup());
		 * 
		 * // getManageInterfaceInfo
		 * System.out.println(service.getManageInterfaceInfo());
		 * 
		 * // getBridgeInterfaceInfos
		 * System.out.println(service.getBridgeInterfaceInfos());
		 * 
		 * // startupGroup service.startupGroup(16, false);
		 * System.out.println(service.getBridgeInterfaceInfos());
		 * 
		 * // getProxyInterfaceInfo
		 * System.out.println(service.getProxyInterfaceInfo());
		 * 
		 * // getBywayInterfaceInfo
		 * System.out.println(service.getBywayInterfaceInfo());
		 * 
		 * // saveGroupInfo InterfaceGroup group =
		 * service.getBridgeInterfaceInfos().get(0).getGroup();
		 * group.setGroupIp("192.168.1.191");
		 * group.setGroupMask("255.255.255.0");
		 * group.setGroupGateway("192.168.1.1"); group.setChanged(0);
		 * 
		 * List<GroupPort> groupPorts = new ArrayList<GroupPort>(); GroupPort
		 * port = new GroupPort(); port.setPortNum(1234); port.setEnable(1);
		 * port.setStatus(DBFWConstant.PORT_ADDED); groupPorts.add(port);
		 * 
		 * port = new GroupPort(); port.setPortNum(1235); port.setEnable(1);
		 * port.setStatus(DBFWConstant.PORT_ADDED); groupPorts.add(port);
		 * 
		 * service.saveGroupInfo(group, groupPorts);
		 * 
		 * InterfaceGroup group =
		 * service.getBridgeInterfaceInfos().get(0).getGroup();
		 * group.setGroupIp("192.168.1.191");
		 * group.setGroupMask("255.255.255.0");
		 * group.setGroupGateway("192.168.1.1"); group.setChanged(0);
		 * 
		 * List<GroupPort> groupPorts = new ArrayList<GroupPort>(); GroupPort
		 * port = new GroupPort(); port.setId(7); port.setPortNum(1234);
		 * port.setStatus(DBFWConstant.OP_DELETED); groupPorts.add(port);
		 * 
		 * port = new GroupPort(); port.setId(8); port.setPortNum(1235);
		 * port.setEnable(0); port.setStatus(DBFWConstant.OP_CHANGED);
		 * groupPorts.add(port);
		 * 
		 * service.saveGroupInfo(group, groupPorts);
		 * 
		 * 
		 * // removeGroup //service.removeGroup(16);
		 */
		// getBridgeInterfaceInfosAvailable
		// List<CommonInterfaceInfo> infoList =
		// service.getBridgeInterfaceInfosAvailable();

		// getProxyInterfaceInfosAvailable
		// List<BywayInterfaceInfo> infoList = service.getBywayInterfaceInfo();
		// System.out.println(infoList);

		InterfaceGroup iGroup = new InterfaceGroup();
		iGroup.setGroupType(DBFWConstant.IFGROUPTYPE_BRIDGE);
		service.addGroupAndInterface(iGroup, 3);
	}

	public int getMonitorType(int dbfwId, int dbId) {
		return networkInterfaceDAO.getMonitorType(dbfwId, dbId);

	}

	public boolean changeNetworkValue(int ifId, String ifName) throws Exception {
		int tempFlag = networkInterfaceDAO.changeNetworkValue(ifId, ifName);
		boolean success = true;
		if (tempFlag == 2) {
			success = false;
			throw new Exception("待变更网卡已被占用或者与当前管理接口对应网卡冲突，请刷新后再变更！");
		} else if (tempFlag == 3) {
			success = false;
			throw new Exception("待变更网卡为未链接状态，请刷新后再变更！");
		} else if (tempFlag == 1) {
			success = false;
		}
		return success;
	}

	/**
	 * 获取路由信息
	 * 
	 * @return
	 */
	public List<RouteConfig> getRoutesInfo() {
		return networkInterfaceDAO.getRoutesInfo();
	}

	/**
	 * 修改路由信息
	 * 
	 * @param routeConfig
	 */
	public void updateRoutesInfo(RouteConfig oldRouteConfig, RouteConfig newRouteConfig) {
		networkInterfaceDAO.updateRoutesInfo(oldRouteConfig, newRouteConfig);

	}

	/**
	 * 删除路由信息
	 * 
	 * @param routeConfig
	 */
	public void delRoutesInfo(RouteConfig routeConfig) {
		networkInterfaceDAO.delRoutesInfo(routeConfig);
	}

	/**
	 * 添加路由信息
	 * 
	 * @param routeConfig
	 */
	public void addRoutesInfo(RouteConfig routeConfig) {
		networkInterfaceDAO.addRoutesInfo(routeConfig);
	}

	/**
	 * 返回网卡名信息
	 * 
	 * @return
	 */
	public List<String> ethInfo() {
		return networkInterfaceDAO.ethInfo();
	}

	/**
	 * 获取所有HA接口信息
	 * 
	 * @return
	 */
	public List<HaConfigInfo> getHaConfigInfo() {
		List<HaConfigInfo> retList = new ArrayList<HaConfigInfo>();

		List<InterfaceGroup> proxyGroups = networkInterfaceDAO.getGroupByType(DBFWConstant.IFGROUPTYPE_HA);

		for (InterfaceGroup interfaceGroup : proxyGroups) {
			HaConfigInfo haConfigInfo = new HaConfigInfo();
			haConfigInfo.setGroup(interfaceGroup);

			List<InterfaceInfo> interfaceInfos = networkInterfaceDAO.getInterfaceByGroup(interfaceGroup.getGroupId());
			if (interfaceInfos.size() == 0) {
				haConfigInfo.setInterfaceInfo(new InterfaceInfo());
			} else {
				haConfigInfo.setInterfaceInfo(interfaceInfos.get(0));
			}

			haConfigInfo.setUsed(networkInterfaceDAO.checkGroupInterfaceIsUsed(interfaceGroup.getGroupId()));

			retList.add(haConfigInfo);
		}
		return retList;
	}
}
