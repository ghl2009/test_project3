package cn.schina.dbfw.config;

import java.io.File;
import java.io.FileInputStream;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;

import cn.schina.dbfw.pojo.database.DatabaseInfo;

public class Configures {

	private static Properties p = new Properties();

	static {
		String configPath = "";
		try {
			configPath = Configures.class.getProtectionDomain().getCodeSource().getLocation().toString();
		} catch (Exception e) {
			configPath = System.getProperty("java.class.path");
		}
		System.out.println(configPath);
		try {

			if (System.getProperty("jdbc.properties") != null) {
				// 指定配置文件路径
				configPath = System.getProperty("jdbc.properties");
			} else if (configPath.contains("WEB-INF/classes")) {
				// Web系统
				configPath = Configures.class.getResource("/").toURI().getPath() + "../configures.properties";
			} else {
				// 焦点报告
				File jarFile = new File(configPath.replace("file:", ""));
				System.out.println(configPath);
				configPath = (jarFile.getParent() == null ? "" : jarFile.getParent())
						+ "/WEB-INF/configures.properties";
				if (!(new File(configPath).exists())) {
					String filePath = System.getProperty("java.class.path");
					String pathSplit = System.getProperty("path.separator");// 得到当前操作系统的分隔符，windows下是";",linux下是":"
					if (filePath.contains(pathSplit)) {
						filePath = filePath.substring(0, filePath.indexOf(pathSplit));
					} else if (filePath.endsWith(".jar")) {// 截取路径中的jar包名,可执行jar包运行的结果里包含".jar"
						filePath = filePath.substring(0, filePath.lastIndexOf(File.separator) + 1);
					}
					configPath = filePath + File.separator + "config" + File.separator + "configures.properties";
					System.out.println(configPath);
				}
			}
			p.load(new FileInputStream(configPath));
		} catch (Exception e) {
			new Exception("Can not read the configures : " + configPath, e).printStackTrace();
			System.exit(1);
		}
	}

	private Configures() {
	}

	public static String getString(String key) {
		return (p.getProperty(key) == null ? "" : p.getProperty(key));
	}

	/**
	 * 返回指定的Key对应的配置项
	 * 
	 * @param key
	 * @return
	 */
	public static int getInteger(String key) {
		String value = p.getProperty(key);
		if (StringUtils.isEmpty(value)) {
			throw new NullPointerException("Config item is not exist in configures.properties. The key is " + key);
		}
		try {
			return Integer.parseInt(value.trim());
		} catch (Exception e) {
			throw new InvalidParameterException("Invalid config item in configures.properties. The key is " + key);
		}
	}

	/**
	 * 获取Boolean类型的配置项
	 * 
	 * @param key
	 * @return 如果是1或true，怎返回true，如果是0或false，则返回false
	 */
	public static boolean getBoolean(String key) {
		String value = p.getProperty(key);
		if (value == null) {
			return false;
		}
		value = value.trim();
		if ("false".equalsIgnoreCase(value) || "0".equals(value)) {
			return false;
		}
		if ("true".equalsIgnoreCase(value) || "1".equals(value)) {
			return true;
		}
		throw new InvalidParameterException("Invalid boolean configure :" + key + "=" + value);
	}

	/**
	 * 获取configures.properties配置中支持的数据库
	 * 
	 * @return
	 */
	public static List<DatabaseInfo> getSupportDatabaseConfigure() {
		List<DatabaseInfo> list = new ArrayList<DatabaseInfo>();
		String dbConfig = Configures.getString("SupportDatabaseConfigure");
		String[] tempArry = dbConfig.split(";");
		int dialect = 0;
		for (int i = 0; i < tempArry.length; i++) {
			String[] arry = tempArry[i].split(",");
			DatabaseInfo db = new DatabaseInfo();
			dialect = Integer.parseInt(arry[1]);
			db.setDialect(dialect);
			db.setType(dialect);
			db.setName(arry[0]);
			db.setDesc("" + dialect);
			list.add(db);
		}
		return list;
	}

	/**
	 * 重新加载配置文件
	 */
	public static void reload() {
		p = new Properties();
		String configPath = Configures.class.getProtectionDomain().getCodeSource().getLocation().toString();
		try {
			if (configPath.contains("WEB-INF/classes")) {
				// Web系统
				configPath = Configures.class.getResource("/").toURI().getPath() + "../configures.properties";
			}
			p.load(new FileInputStream(configPath));
		} catch (Exception e) {
			new Exception("Can not read the configures : " + configPath, e).printStackTrace();
		}
	}
}
