package cn.schina.dbfw.service.strategy;

import java.util.ArrayList;
import java.util.List;

import cn.schina.dbfw.common.lang.ServiceException;
import cn.schina.dbfw.config.Globals;
import cn.schina.dbfw.dao.strategy.DBFWConfigDAO;
import cn.schina.dbfw.pojo.LicenseInfo;
import cn.schina.dbfw.pojo.strategy.DBFWAttr;
import cn.schina.dbfw.pojo.strategy.DBFWForDbAddr;
import cn.schina.dbfw.pojo.strategy.DBFWInfo;
import cn.schina.dbfw.pojo.strategy.DatabaseInfo;
import cn.schina.dbfw.service.SystemLicenseSrever;
import cn.schina.dbfw.service.strategy.command.FlushDbfwInstanceInfoCommand;
import cn.schina.dbfw.service.strategy.command.NativeCommand;
import cn.schina.dbfw.service.strategy.command.NativeExecutor;
import cn.schina.dbfw.util.LangUtils;
import cn.schina.dbfw.util.SystemAuditResultSingelton;

public class DBFWConfigService {

	private DBFWConfigDAO dbfwConfigDAO = DBFWConfigDAO.getDAO();

	/**
	 * 获取所有安全实例
	 * 
	 * @return
	 */
	public List<DBFWInfo> getAllDBFWInstances() {
		return dbfwConfigDAO.getAllDBFWInstances();
	}

	/**
	 * 获取指定数据库地址的安全实例保护信息
	 * 
	 * @param dbfwId
	 * @return
	 */
	public List<DBFWForDbAddr> getDBFWForDbAddrByDbfw(int dbfwId) {
		return dbfwConfigDAO.getDBFWForDbAddrByDbfw(dbfwId);
	}

	/**
	 * 设置安全实例的状态
	 * 
	 * @param dbfwId
	 * @param status
	 */
	public void setDbfwStatus(int dbfwId, int status) {
		String dbfwName = dbfwConfigDAO.getDbfwInstanceName(dbfwId);
		String result = "";
		String message = "";
		if (status == 0) {
			message = "挂起";
		} else if (status == 1) {
			message = "恢复";
		} else if (status == 2) {
			message = "启动";
		} else if (status == 3) {
			message = "停止";
		}
		try {
			result = "1";
			List<NativeCommand> commands = new ArrayList<NativeCommand>();
			dbfwConfigDAO.setDbfwStatus(dbfwId, status, commands);
			NativeExecutor.execute(commands);
		} catch (Exception e) {
			result = "2";
			e.printStackTrace();
			throw new ServiceException("安全实例：" + dbfwName + message + " 失败 " + e.getMessage());
		} finally {
			SystemAuditResultSingelton.getInstance().insertSystemAudit(7, 3, 2, 5, result,
					"安全实例：" + dbfwName + " " + message);
		}
	}

	/**
	 * 判断安全实例数是否已达到上限
	 * 
	 * @return
	 */
	public boolean canAddDbfwInstance() {
		int limitNum = -1;
		try {
			limitNum = SystemLicenseSrever.getLicenseInfoFinal().getDbfwcount();
		} catch (Exception e1) {
			e1.printStackTrace();
			throw new ServiceException("读取许可证书信息失败！");
		}
		if (limitNum <= 0) {
			throw new ServiceException("许可证书未指定！");
		}
		return dbfwConfigDAO.getDBFWInstanceCount() < limitNum;
	}

	/**
	 * 添加一个安全实例
	 * 
	 * @param instance
	 */
	public int addDbfwInstance(DBFWInfo instance, List<DBFWAttr> attributes) {

		// 重新加载管理IP端口
		Globals.reloadConfigs();

		/*
		 * 判断安全实例数是否已达到上限，如果达到上限则抛出异常！
		 */
		int limitNum = -1;
		try {
			limitNum = SystemLicenseSrever.getLicenseInfoFinal().getDbfwcount();
		} catch (Exception e1) {
			e1.printStackTrace();
			throw new ServiceException("读取许可证书信息失败！");
		}
		if (limitNum <= 0) {
			throw new ServiceException("许可证书未指定！");
		}
		if (dbfwConfigDAO.getDBFWInstanceCount() >= limitNum) {
			throw new ServiceException("安全实例个数已达到上限（" + limitNum + "个），请删除其他安全实例后重试！");
		}

		String result = "";
		if (!LangUtils.isInteger(attributes.get(1).getValue())) {
			result = "2";
			ServiceException ex = new ServiceException("SMON监听端口不合法！");
			ex.printStackTrace();
			throw ex;
		}
		List<NativeCommand> commands = new ArrayList<NativeCommand>();

		Globals.reloadConfigs();
		// 防止前台传来的instance中未赋值name, address和port
		instance.setName(attributes.get(0).getValue());
		instance.setAddress(Globals.GMON_IP);
		instance.setPort(Integer.parseInt(attributes.get(1).getValue()));
		int dbfwId = 0;
		try {
			result = "1";
			dbfwId = dbfwConfigDAO.addDbfwInstance(instance, attributes, commands);
			NativeExecutor.execute(commands);
		} catch (ServiceException ex) {
			result = "2";
			ex.printStackTrace();
			dbfwConfigDAO.removeDbfw(dbfwId);
			throw ex;
		} catch (Exception e) {
			result = "2";
			e.printStackTrace();
			throw new ServiceException("保存安全实例：" + instance.getName() + " 失败 " + e.getMessage());
		} finally {
			SystemAuditResultSingelton.getInstance().insertSystemAudit(7, 3, 1, 1, result,
					"保存安全实例：" + instance.getName());
		}
		return dbfwId;
	}

	/**
	 * 自动添加数据库
	 * 
	 * @param instance
	 * @param attributes
	 * @return
	 */
	public int addDbfwInstanceAutoTest(DBFWInfo instance, List<DBFWAttr> attributes) {

		// 重新加载管理IP端口
		Globals.reloadConfigs();

		/*
		 * 判断安全实例数是否已达到上限，如果达到上限则抛出异常！
		 */
		int limitNum = -1;
		try {
			limitNum = SystemLicenseSrever.getLicenseInfoFinal().getDbfwcount();
		} catch (Exception e1) {
			e1.printStackTrace();
			throw new ServiceException("读取许可证书信息失败！");
		}
		if (limitNum <= 0) {
			throw new ServiceException("许可证书未指定！");
		}
		if (dbfwConfigDAO.getDBFWInstanceCount() >= limitNum) {
			throw new ServiceException("安全实例个数已达到上限（" + limitNum + "个），请删除其他安全实例后重试！");
		}

		if (!LangUtils.isInteger(attributes.get(1).getValue())) {
			ServiceException ex = new ServiceException("SMON监听端口不合法！");
			ex.printStackTrace();
			throw ex;
		}
		List<NativeCommand> commands = new ArrayList<NativeCommand>();

		Globals.reloadConfigs();
		// 防止前台传来的instance中未赋值name, address和port
		instance.setName(attributes.get(0).getValue());
		instance.setAddress(Globals.GMON_IP);
		instance.setPort(Integer.parseInt(attributes.get(1).getValue()));
		int dbfwId = 0;
		try {
			dbfwId = dbfwConfigDAO.addDbfwInstance(instance, attributes, commands);
			if (System.getProperty("os.name").equals("Linux")) {
				NativeExecutor.execute(commands);
			}
		} catch (ServiceException ex) {
			ex.printStackTrace();
			dbfwConfigDAO.removeDbfw(dbfwId);
			throw ex;
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServiceException("保存安全实例：" + instance.getName() + " 失败 " + e.getMessage());
		}
		return dbfwId;
	}

	/**
	 * 获取可保护当前数据库的安全实例
	 * 
	 * @param dbId
	 *            数据库ID
	 * @return 可用安全实例列表
	 */
	public List<DBFWInfo> getDbfwInstanceAvailable(int dbId) {
		return dbfwConfigDAO.getDbfwInstanceAvailable(dbId);
	}

	public void setDbfwInstance(List<DBFWAttr> attributes, int dbfwId) {
		if (attributes.size() > 0) {
			String name = dbfwConfigDAO.getDbfwInstanceName(dbfwId);
			String result = "";
			try {
				result = "1";
				List<NativeCommand> commands = new ArrayList<NativeCommand>();
				dbfwConfigDAO.setDbfwAttr(attributes, dbfwId);
				DBFWInfo dbfwInfo = this.getDBFWInfoById(dbfwId);
				NativeCommand command = new FlushDbfwInstanceInfoCommand(dbfwInfo.getAddress(), dbfwInfo.getPort(),
						dbfwInfo.getId());
				commands.add(command);
				NativeExecutor.execute(commands);
			} catch (Exception e) {
				result = "2";
				e.printStackTrace();
				throw new ServiceException("更新安全实例属性：" + name + " 失败 "
						+ (e.getMessage() == null ? "!" : e.getMessage()));
			} finally {
				SystemAuditResultSingelton.getInstance().insertSystemAudit(7, 3, 2, 2, result, "更新安全实例属性：" + name);
			}
		}
	}

	private DBFWInfo getDBFWInfoByAttrId(int id) {
		return dbfwConfigDAO.getDBFWInfoByAttrId(id);
	}

	public void removeDbfwInstance(int dbfwId) {

		String result = "";
		String dbfwName = dbfwConfigDAO.getDbfwInstanceName(dbfwId);
		try {
			result = "1";
			List<NativeCommand> commands = new ArrayList<NativeCommand>();
			dbfwConfigDAO.removeDbfw(dbfwId, commands);
			// NativeExecutor.execute(commands);
		} catch (Exception e) {
			result = "2";
			e.printStackTrace();
			throw new ServiceException("移除安全实例：" + dbfwName + " 失败 " + e.getMessage());
		} finally {
			SystemAuditResultSingelton.getInstance().insertSystemAudit(7, 3, 2, 6, result, "移除安全实例：" + dbfwName);
		}
	}

	public List<DatabaseInfo> getDatabaseByDbfwId(int dbfwId) {
		return dbfwConfigDAO.getDatabaseByDbfwId(dbfwId);
	}

	public List<DatabaseInfo> getDatabaseToProtectByDbfwId(int dbfwId) {
		return dbfwConfigDAO.getDatabaseToProtectByDbfwId(dbfwId);
	}

	public List<DBFWAttr> getDBFWAttrList(int dbfwId) {
		return dbfwConfigDAO.getDBFWAttrList(dbfwId);
	}

	public List<DBFWAttr> getDBFWAttrDefaultList() {
		// 根据License刷新安全实例
		LicenseInfo licenseInfo;
		try {
			// 当第一次添加时重新刷新列表页面，防止恢复出厂设置后页面显示不正确
			if (dbfwConfigDAO.getAllDBFWInstances().size() == 0) {
				licenseInfo = SystemLicenseSrever.getLicenseInfoFinal();
				DBFWConfigDAO.getDAO().reFlushDbfwByLicense(licenseInfo);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return dbfwConfigDAO.getDBFWAttrDefaultList();
	}

	public String getRelatedDbNames(int dbfwId) {
		return dbfwConfigDAO.getRelatedDbNames(dbfwId);
	}

	private DBFWInfo getDBFWInfoById(int id) {
		return dbfwConfigDAO.getDBFWInfoById(id);
	}

	public static void main(String[] args) {
		DBFWConfigService service = new DBFWConfigService();
		// System.out.println(service.getDbfwInstanceAvailable(1));

		System.out.println(service.getDBFWAttrDefaultList());
		/*
		 * DBFWInfo dbfwInfo = new DBFWInfo(); dbfwInfo.setName("dbfwtest1");
		 * dbfwInfo.setAddress("192.168.1.121"); dbfwInfo.setPort(9111);
		 * 
		 * List<DBFWAttr> attrList = new ArrayList<DBFWAttr> (); DBFWAttr attr =
		 * new DBFWAttr(); // attr.setName("S_SMON_WORKTIME_DEADLINE");
		 * attr.setId(232); attr.setValue("10"); attrList.add(attr);
		 * 
		 * service.addDbfwInstance(dbfwInfo, attrList);
		 */
	}
}
