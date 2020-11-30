package cn.schina.dbfw.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang.SystemUtils;

import net.ServiceNativeGmon;
import cn.schina.dbfw.common.lang.ServiceException;
import cn.schina.dbfw.config.Globals;
import cn.schina.dbfw.pojo.AuditLog;
import cn.schina.dbfw.pojo.LicenseInfo;
import cn.schina.dbfw.util.SystemAuditResultSingelton;

public class SystemLicenseSrever {
	/**
	 * 上传licence文件
	 * 
	 * @param is
	 * @return
	 * @throws Exception
	 */
	public LicenseInfo uploadLicenseInfo(InputStream is) throws Exception {
		if (!System.getProperty("os.name").equals("Linux")) {
			return getLicenseInfoFinal();
		}
		String path = "/home/dbfw/dbfw/etc/";
		String name = "License.swk";
		path = path + "/" + name;
		boolean success = true;
		FileOutputStream fos = null;
		try {
			File file = new File(path);// 文件存在则删除
			if (file.exists()) {
				file.delete();
			}
			fos = new FileOutputStream(path);
			byte[] bytes = new byte[1024];
			int len = 0;
			while ((len = is.read(bytes)) != -1) {
				fos.write(bytes, 0, len);
			}
			fos.flush();// 保存文件

		} catch (Exception e) {
			success = false;
			e.printStackTrace();
			throw new ServiceException("文件上传失败！写入文件发生异常：\r\n" + e.getLocalizedMessage(), e);
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			AuditLog auditLog = new AuditLog();
			auditLog.setEventDesc("上传License证书 ");
			auditLog.setDesigner("系统->证书管理");
			auditLog.setMovement("上传");
			auditLog.setEventResult(success == true ? "1" : "2");
			auditLog.setAlterBefore("上传证书：" + name);
			SystemAuditResultSingelton.getInstance().insertAuditLog(auditLog);
		}
		return getLicenseInfoFinal();
	}

	/**
	 * 取到license信息
	 * 
	 * @return
	 * @throws ParseException
	 */
	public static LicenseInfo getLicenseInfo() throws Exception {
		LicenseInfo info = new LicenseInfo();
		String runtime = "";
		if ("Linux".equals(info.getSystemType())) {
			String cmdShell = "/bin/sh";
			String cmdOption = "-c";
			String cmdParamPrefix = " cat /proc/uptime| awk -F. " + "'{run_days=$1 / 86400;run_hour=($1 % 86400)/3600;"
					+ "run_minute=($1 % 3600)/60;run_second=$1 % 60;"
					+ "printf(\"已运行：%d天%d时%d分\",run_days,run_hour,run_minute)}'";
			// System.out.println("命令：：" + cmdParamPrefix);
			String[] cmd = { cmdShell, cmdOption, cmdParamPrefix };
			runtime = runCmd(cmd);
		} else {
			System.out.println("系统类型：" + info.getSystemType());
		}
		info.setRunTime(runtime);

		Globals.reloadConfigs();
		String ret = "";
		if ("Linux".equals(info.getSystemType())) {
			ret = ServiceNativeGmon.ODC_ED_GetLicenseInfo(Globals.GMON_IP, Globals.GMON_PORT);
			System.out.println(ret);
		}
		String[] licVal = ret.split("\\|");
		if (licVal.length != 6) {
			info.setDBSum(0);
			info.setEndTime("未授权");
			// info.setRunTime("未知");
			info.setSeriesNumber(getSeriesNumber());
			info.setUnitType("未知");
			info.setHelp_version_info("未授权");
		} else {
			/**
			 * 函数说明：获取license信息 参数说明： 返 回
			 * 值：返回值为字符串类型，如果为正常值，则用竖线分割（audtype|dbcount
			 * |devtype|time|diskid），否则为错误号信息。 其中audtype，为授权类型，T为测试版本，R为正式版本
			 * dbcount，为受保护数据库的个数 devtype，为设备型号，为最长为32字节的字符串
			 * time，为授权有效时间，格式为YYYYMMDD，如果为00000000则为永久有效 diskid，为设备序列号，crc加密结果
			 */
			SimpleDateFormat formatDate = new SimpleDateFormat("yyyy年MM月dd日");
			SimpleDateFormat formatDate1 = new SimpleDateFormat("yyyyMMdd");
			String licType = "未知版";
			if (licVal[0].equals("T")) {
				licType = "测试版";
			} else if (licVal[0].equals("S")) {
				licType = "试用版";
			} else if (licVal[0].equals("R")) {
				licType = "正式版";
			}
			info.setHelp_version_info(licType + "(" + licVal[1] + "个数据库实例)");
			info.setDBSum(Integer.parseInt(licVal[1]));
			info.setUnitType(licVal[2]);

			info.setEndTime(licVal[3].equals("00000000") ? "永久有效" : ("截至 ：" + formatDate.format(formatDate1
					.parse(licVal[3]))));
			info.setSeriesNumber(getSeriesNumber());
			if (!licVal[3].equals("00000000")) {
				Date now = formatDate1.parse(formatDate1.format(new Date()));// 当前时间
				if (formatDate1.parse(licVal[3]).getTime() - now.getTime() < 0) {
					info.setEndTime("已过期(过期时间：" + formatDate.format(formatDate1.parse(licVal[3])) + ")");
				}
			}

			if (licVal[5].equals("0")) {
				info.setHelp_version_info("false");
			}

		}

		return info;
	}

	/**
	 * 取到license信息
	 * 
	 * @return
	 * @throws ParseException
	 */
	public static LicenseInfo getLicenseInfoFinal() throws Exception {
		if (ifNsfocus() == 1) {
			return getNsfocusLicenseInfoFinal();
		}
		LicenseInfo info = new LicenseInfo();
		String runtime = "";
		if (info.getSystemType().equals("Linux")) {
			String cmdShell = "/bin/sh";
			String cmdOption = "-c";
			String cmdParamPrefix = " cat /proc/uptime| awk -F. " + "'{run_days=$1 / 86400;run_hour=($1 % 86400)/3600;"
					+ "run_minute=($1 % 3600)/60;run_second=$1 % 60;"
					+ "printf(\"已运行：%d天%d时%d分\",run_days,run_hour,run_minute)}'";
			// System.out.println("命令：：" + cmdParamPrefix);
			String[] cmd = { cmdShell, cmdOption, cmdParamPrefix };
			runtime = runCmd(cmd);
		} else {
			System.out.println("系统类型：" + info.getSystemType());
		}
		info.setRunTime(runtime);

		Globals.reloadConfigs();
		String ret = "";
		if ("Linux".equals(info.getSystemType())) {
			ret = ServiceNativeGmon.ODC_ED_GetLicenseInfo(Globals.GMON_IP, Globals.GMON_PORT);
			System.out.println(ret);
		} else {
			ret = "T|100|TEST-500-3|20200101|2FF1-8024-078C-A560|1|20141210|通用|2";
		}
		String[] licVal = ret.split("\\|");
		if (licVal.length != 9) {

			info.setStartTime("");
			info.setEndTime("");
			info.setUser("");
			info.setAudtype("未授权");
			info.setCk_lic_ret("未导入证书");
		} else {
			/**
			 * 函数说明：获取license信息 参数说明： 返 回
			 * 值：返回值为字符串类型，如果为正常值，则用竖线分割（audtype|dbcount
			 * |devtype|time|diskid），否则为错误号信息。 其中audtype，为授权类型，T为测试版本，R为正式版本
			 * dbcount，为受保护数据库的个数 devtype，为设备型号，为最长为32字节的字符串
			 * time，为授权有效时间，格式为YYYYMMDD，如果为00000000则为永久有效 diskid，为设备序列号，crc加密结果
			 * 
			 */
			// 绿盟
			/**
			 * * audtype：授权类型，正版，测试 dbcount：受保护数据库个数 devtype：设备型号
			 * starttime：本期服务起始时间 endtime：本期服务终止时间 diskid：磁盘id user：本证书颁发对象
			 * mod：功能模块 ck_lic_ret:校验结果
			 * 
			 * audtype|dbcount|devtype|endtime|diskid|ck_lic_ret|starttime|user|
			 * mod
			 */
			SimpleDateFormat formatDate = new SimpleDateFormat("yyyy年MM月dd日");
			SimpleDateFormat formatDate1 = new SimpleDateFormat("yyyyMMdd");
			String licType = "未知";
			if (licVal[0].equals("T")) {
				licType = "测试版";
			} else if (licVal[0].equals("S")) {
				licType = "试用版";
			} else if (licVal[0].equals("R")) {
				licType = "正式版";
			}
			info.setAudtype(licType);
			info.setDbcount(Integer.parseInt(licVal[1]));

			info.setDevtype(licVal[2]);

			info.setEndTime(licVal[3].equals("00000000") ? "永久有效" : (formatDate.format(formatDate1.parse(licVal[3]))));

			info.setDiskid(getSeriesNumber());
			info.setCk_lic_ret(Integer.parseInt(licVal[5]) == 1 ? "正常" : "无效");
			info.setStartTime(licVal[6].equals("00000000") ? "永久有效" : formatDate.format(formatDate1.parse(licVal[6])));
			info.setUser(licVal[7]);
			// 数据库地址数
			int dbAddressCount = info.getDbcount() == 1 ? 64 : info.getDbcount();
			info.setMod_help(Integer.parseInt(licVal[8]) == 1 ? ("数据库实例数(" + dbAddressCount + ")") : ("数据库防火墙("
					+ info.getDbcount() + "实例)"));

			if (!licVal[3].equals("00000000")) {
				Date now = formatDate1.parse(formatDate1.format(new Date()));// 当前时间
				if (formatDate1.parse(licVal[3]).getTime() - now.getTime() < 0) {
					info.setCk_lic_ret("已过期");
					info.setEndTime("已过期(过期时间：" + formatDate.format(formatDate1.parse(licVal[3])) + ")");
				}
			}

			if (licVal[5].equals("0")) {
				info.setHelp_version_info("false");
			}

		}

		return info;
	}

	/**
	 * DB数量
	 * 
	 * @return
	 */
	public static int getDBsum() {
		Globals.reloadConfigs();

		int saveDB = 0;
		if (System.getProperty("os.name").equals("Linux")) {
			saveDB = ServiceNativeGmon.ODC_ED_GetLicenseDBCount(Globals.GMON_IP, Globals.GMON_PORT);
			if (saveDB < 0) {
				String errMsg = Globals.ERROR_MSG_MAP.get(saveDB);
				if (errMsg == null)
					throw new ServiceException("[" + saveDB + "]未知错误，请联系厂家");
				else
					throw new ServiceException("[" + saveDB + "]" + errMsg);
			}
		}
		return saveDB;
	}

	/**
	 * 取到系统设备号
	 * 
	 * @return
	 */
	public static String getSeriesNumber() {
		Globals.reloadConfigs();
		String ret = "";
		if (System.getProperty("os.name").equals("Linux")) {
			ret = ServiceNativeGmon.ODC_ED_GetLicenseDiskCRC(Globals.GMON_IP, Globals.GMON_PORT);
		}
		System.out.println(Globals.GMON_IP + "-----" + Globals.GMON_PORT + "---------" + ret);
		return ret;
	}

	private static String runCmd(String[] cmd) throws IOException {
		Process ps = Runtime.getRuntime().exec(cmd);

		BufferedReader br = new BufferedReader(new InputStreamReader(ps.getInputStream()));
		StringBuffer sb = new StringBuffer();
		String line;
		while ((line = br.readLine()) != null) {
			sb.append(line).append("\n");
		}
		String result = sb.toString();
		return result;
	}

	/**
	 * 
	 * <b>是否为绿盟版本</b><br/>
	 * 
	 * @author <b>LCL</b><br/>
	 * @date 2016-8-3
	 * 
	 * @return
	 */
	public static int ifNsfocus() {
		int returnValure = 0;
		try {
			if (SystemUtils.IS_OS_LINUX) {
				String[] cmd = { "/bin/sh", "-c", " cat /etc/producttype " };
				String value = runCmd(cmd).trim();
				if (value.contains("Nsfocus")) {
					returnValure = 1;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return returnValure;
	}

	/**
	 * 取到license信息
	 * 
	 * @return
	 * @throws ParseException
	 */
	public static LicenseInfo getNsfocusLicenseInfoFinal() throws Exception {
		LicenseInfo info = new LicenseInfo();
		String systemType = System.getProperty("os.name");
		String runtime = "";
		if (systemType.equals("Linux")) {
			String cmdShell = "/bin/sh";
			String cmdOption = "-c";
			String cmdParamPrefix = " cat /proc/uptime| awk -F. " + "'{run_days=$1 / 86400;run_hour=($1 % 86400)/3600;"
					+ "run_minute=($1 % 3600)/60;run_second=$1 % 60;"
					+ "printf(\"已运行：%d天%d时%d分\",run_days,run_hour,run_minute)}'";
			String[] cmd = { cmdShell, cmdOption, cmdParamPrefix };
			runtime = runCmd(cmd);
		} else {
			System.out.println("系统类型：" + systemType);
		}
		info.setRunTime(runtime);

		Globals.reloadConfigs();
		String ret = "";
		if (systemType.equals("Linux")) {
			ret = ServiceNativeGmon.ODC_ED_GetLicenseInfo(Globals.GMON_IP, Globals.GMON_PORT);
		} else if (systemType.indexOf("Windows") > -1) {
			ret = "4|FCB5-5AB6-F481-FABB|1|5|user|20140101|20151231|10|1|1";// 过期
		}
		String[] licVal = ret.split("\\|");
		if (licVal.length != 10) {

			info.setStartTime("");
			info.setEndTime("");
			info.setUser("");
			info.setAudtype("未授权");
		} else {
			/**
			 * audtype|hardw_id|modcode|ewkcount|lssue_usr|starttime|endtime|
			 * dbcount|instcount|ck_lic_ret audtype, 证书类型：1-4
			 * 数字，1.测试证书/2.试用证书/3.销售临时证书/4.正式证书 hardw_id, 硬件特征值 modcode, 1-32
			 * ,1:应用账号关联审计,2:数据库入侵检测 ewkcount, 工作口数量 lssue_usr, 颁发的用户 starttime,
			 * 开始时间 endtime, 结束时间 dbcount 保护的数据库数量 instcount 实例数量 ck_lic_ret
			 * 证书状态，1有效，2无效，3 license永久有效，但升级时间无效，不能支持升级操作。4 无效的硬件特征值
			 */
			SimpleDateFormat formatDate = new SimpleDateFormat("yyyy-MM-dd");
			SimpleDateFormat formatDate1 = new SimpleDateFormat("yyyyMMdd");

			info.setVersionInfo(Integer.parseInt(licVal[0]));
			switch (Integer.parseInt(licVal[0])) {// 证书类型
			case 1:
				info.setAudtype("测试证书");
				break;
			case 2:
				info.setAudtype("试用证书");
				break;
			case 3:
				info.setAudtype("销售临时证书");
				break;
			case 4:
				info.setAudtype("正式证书");
				break;

			default:
				break;
			}

			info.setHardw_id(licVal[1]);// 硬件特征值

			// 1:应用账号关联审计,2:数据库入侵检测
			info.setModcode(Integer.parseInt(licVal[2]));
			info.setEwkcount(Integer.parseInt(licVal[3]));// 工作口数量
			info.setUser(licVal[4]);// 颁发的用户
			info.setStartTime(formatDate.format(formatDate1.parse(licVal[5])));// 开始时间
			info.setEndTime(formatDate.format(formatDate1.parse(licVal[6])));// 结束时间
			info.setDbcount(Integer.parseInt(licVal[7]));// DB数量信息
			info.setInstcount(Integer.parseInt(licVal[8]));// 实例数量

			int mode_info = Integer.parseInt(licVal[2]);
			String mode_help = "";
			for (int i = 0; i <= 31; i++) {
				if ((mode_info & (1 << i)) > 0) {
					switch (i) {
					case 0:
						mode_help = mode_help + "应用账号关联审计、";
						break;
					case 1:
						mode_help = mode_help + "数据库入侵检测、";
						break;
					default:
						break;
					}
				}
			}
			if (mode_help.length() > 1) {
				mode_help = mode_help.substring(0, mode_help.length() - 1);
			}
			info.setMod_help(mode_help + ("(" + info.getDbcount() + "数据库实例)"));

			// 最终结果1 有效，2无效，3 license永久有效，但升级时间无效，不能支持升级操作。
			info.setCk_lic_ret_im(Integer.parseInt(licVal[9]));
			if (Integer.parseInt(licVal[9]) != 0) {
				info.setCk_lic_ret("正常");
			}

			// 过期信息
			if (licVal[9].equals("3") && licVal[0].equals("4")) {
				Date now = formatDate1.parse(formatDate1.format(new Date()));// 当前时间
				if (formatDate1.parse(licVal[6]).getTime() - now.getTime() < 0) {
					info.setEndTime("过期(到期时间：" + formatDate.format(formatDate1.parse(licVal[6])) + ")");
				}
				if (formatDate1.parse(licVal[5]).getTime() - now.getTime() > 0) {
					info.setStartTime(formatDate.format(formatDate1.parse(licVal[5])) + "");
					info.setClientInfo("1");
				}
			} else if (!licVal[0].equals("4") && licVal[9].equals("2")) {
				Date now = formatDate1.parse(formatDate1.format(new Date()));// 当前时间
				if (formatDate1.parse(licVal[6]).getTime() - now.getTime() < 0) {
					info.setEndTime("过期(到期时间：" + formatDate.format(formatDate1.parse(licVal[6])) + ")");
				}
				if (formatDate1.parse(licVal[5]).getTime() - now.getTime() > 0) {
					info.setStartTime(formatDate.format(formatDate1.parse(licVal[5])) + "");
					info.setClientInfo("1");
				}
			} else if (licVal[0].equals("4") && licVal[9].equals("2")) {
				Date now = formatDate1.parse(formatDate1.format(new Date()));// 当前时间
				if (formatDate1.parse(licVal[5]).getTime() - now.getTime() > 0) {
					info.setStartTime(formatDate.format(formatDate1.parse(licVal[5])) + "");
					info.setClientInfo("1");
				}
			}
		}
		return info;
	}
}
