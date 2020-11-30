package cn.schina.dbfw.config;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

public class Configures {

	private static Properties p = new Properties();

	static {
		String configPath = Configures.class.getProtectionDomain().getCodeSource().getLocation().toString();
		try {
			
			if(System.getProperty("jdbc.properties") != null){
				// 指定配置文件路径
				configPath = System.getProperty("jdbc.properties");
			}else  if (configPath.contains("WEB-INF/classes")) {
				// Web系统
				configPath = Configures.class.getResource("/").toURI().getPath() + "../configures.properties";
			} else {
				// 焦点报告
				File jarFile = new File(configPath.replace("file:", ""));
				configPath = (jarFile.getParent() == null ? "" : jarFile.getParent()) + "/config/configures.properties";
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
	 * 重新加载配置文件
	 */
	public static void reLoad() {
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
			System.exit(1);
		}
	}
}
