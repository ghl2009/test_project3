package cn.schina.dbfw.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.schina.dbfw.base.tools.DesEncrypt;
import cn.schina.dbfw.dao.BaseDAO;
import cn.schina.dbfw.service.strategy.DatabaseConfigService;

public class AutoAddDatabase extends BaseDAO {

	/**
	 * <b> 自动添加数据库</b><br/>
	 * 
	 * @author <b>LCL</b><br/>
	 * @date 2016-3-31
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		if (System.getProperty("os.name").equals("Linux")) {
			if (args.length < 11 && (args.length - 5) % 6 != 0) {
				return;
			}
			List<String> argList = new ArrayList<String>();
			for (int i = 0; i < args.length; i++) {
				argList.add(args[i].equalsIgnoreCase("NULL") ? "" : args[i]);
			}
			addDatabase(argList.toArray());
		} else {
			String[] selfArgs = { "test_oracle", "1", "2", "10020002", "0", "192.168.1.99", "1521", "orcl", "1", "",
					"", "192.168.1.100", "1521", "orcl", "1", "", "" };
			if (selfArgs.length % 5 != 0 && args.length >= 10) {
				return;
			}
			addDatabase(selfArgs);
		}

	}

	/**
	 * 
	 * @author <b>LCL</b><br/>
	 * @date 2016-3-31 <br/>
	 * 
	 *       参数1：name受保护数据库名称
	 * 
	 *       参数2：type数据库类型对应的值 1 Oracle 2 MSSQL 4 Sybase 8 DB2 32 MySql 64
	 *       POSTGRE 128 DM 256 Kingbase 512 GBase 1024 Informix 2048 CacheDb
	 *       4096 Oscar 8192 Gbase8t
	 * 
	 *       参数3：isglba表示服务器的操作系统 对应的值 1～9，0表示未使用(未知) 1-Windows32 2-Windows64
	 *       3-Linux 32 4-Linux64(默认值) 5-AIX32 6-Aix64 7-Solaris32 8-Solaris64
	 *       9-HPUnix64
	 * 
	 *       参数4：version数据库版本 参照dbversion表dbverval字段（dbkind为数据库类型见-参数2）
	 * 
	 *       参数5：isdpa数据库字符集 Oracle数据库 ZHS16GBK=0 ZHT32EUC=1 ZHT16BIG5=2
	 *       AL32UTF8=3 其他数据库 GBK=0 Latin=1 UTF8=4 其他=9
	 * 
	 *       参数6：address数据库IP地址
	 * 
	 *       参数7：port数据库端口
	 * 
	 *       参数8：serviceName数据库实例名（如果没有默认传''）
	 * 
	 *       参数9：userName数据库账户（如果没有默认传''）
	 * 
	 *       参数10：userPwd数据库数据库密码（如果没有默认传''）
	 * 
	 * 
	 *       多地址时参数6-参数10多次传值
	 * 
	 * @param args
	 * @return
	 */
	private synchronized static void addDatabase(Object[] args) {
		if (args.length < 10) {
			return;
		}
		Map<String, String> databaseInfos = new HashMap<String, String>();

		String value = "";

		// 数据库信息
		String name = args[0] + "";// 数据库名称
		String type = args[1] + "";// 数据库类型
		String isglba = args[2] + "";// 服务器的操作系统
		String version = args[3] + "";// 服务器的操作系统
		String isdpa = args[4] + "";// 数据库字符集

		String key = "{'id':'','name':'" + name + "','desc':'','state':0,'type':'" + type + "'," + "'isdpa':'" + isdpa
				+ "','isglba':'" + isglba + "','version':'" + version + "','issox':0,'ispci':0,"
				+ "'ipFilter':0,'sqlFilter':0,'auditMode':'0'}";

		String address = "";// 数据库IP地址
		String port = "";// 数据库端口
		String serviceName = "";// 数据库实例名
		String userName = "";// 数据库账户
		String userPwd = "";// 数据库数据库密码
		String dynaPort = "";// 数据库数据库密码

		// 用户名密码、加密
		DesEncrypt desEncrypt = new DesEncrypt();

		for (int i = 5; i < args.length; i += 6) {
			address = args[i] + "";// 数据库IP地址
			port = args[i + 1] + "";// 数据库端口
			serviceName = args[i + 2] + "";// 数据库实例名
			dynaPort = args[i + 3] + "";// 是否为动态端口
			userName = args[i + 4] + "";// 数据库账户
			userPwd = args[i + 5] + "";// 数据库数据库密码

			value += "{'address':'" + address + "','id':'','mirdbAddressId':'','userName':'"
					+ (userName.equals("") ? "" : desEncrypt.strEnc(userName, "20160101", "1", "1")) + "','userPwd':'"
					+ (userPwd.equals("") ? "" : desEncrypt.strEnc(userPwd, "20160101", "1", "1")) + "','port':'"
					+ port + "','dynaPort':" + dynaPort + ",'serviceName':'" + serviceName + "'},";
		}
		// 去掉最后的逗号
		if (value.length() > 0) {
			value = value.substring(0, value.length() - 1);
		}
		value = "[" + value + "]";

		databaseInfos.put(key, value);

		System.out.println(databaseInfos);
		// 运行添加数据库
		DatabaseConfigService service = new DatabaseConfigService();
		service.saveBasicAuditNoSelfLog(databaseInfos, 0, 1);

	}

}
