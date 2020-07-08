package cn.dbsec.auto;

/**
 * 
 * <b>数据库名称、方言、类型数值枚举</b><br/>
 * 
 * @author <b>LCL</b><br/>
 * @date 2016-09-26
 * 
 * @param dbtype
 * @return
 */
public enum DBEnum {
	OTEHER(0, 0, "OTHER"), Oracle(1, 1, "Oracle"), MSSQL(2, 2, "Sqlserver"), Sybase(6, 4, "Sybase"), MySql(4, 32,
			"MySql"), DB2(3, 8, "DB2"), DM(10, 128, "DM"), GBase(12, 512, "GBase"), POSTGRE(5, 64, "Postgre"), Kingbase(
			11, 256, "Kingbase"), Informix(9, 1024, "Informix"), CacheDb(16, 2048, "CacheDb"), Oscar(15, 4096, "Oscar"), Gbase8t(
			18, 8192, "Gbase8t");
	public final int dbType;
	public final int dialect;
	public final String typeName;

	public int getDbType() {
		return dbType;
	}

	public int getDialect() {
		return dialect;
	}

	public String getTypeName() {
		return typeName;
	}

	private DBEnum(int dbType, int dialect, String typeName) {
		this.dbType = dbType;
		this.dialect = dialect;
		this.typeName = typeName;
	}

	/**
	 * 
	 * <b>根据类型获取</b><br/>
	 * 
	 * @author <b>LCL</b><br/>
	 * @date 2016-2-19
	 * 
	 * @param dbtype
	 * @return
	 */
	public static DBEnum getDBEnumByDbType(int dbtype) {
		for (DBEnum dbenum : DBEnum.values()) {
			if (dbenum.dbType == dbtype) {
				return dbenum;
			}
		}
		return null;
	}

	/**
	 * 
	 * <b>根据方言获取</b><br/>
	 * 
	 * @author <b>LCL</b><br/>
	 * @date 2016-2-19
	 * 
	 * @param dialect
	 * @return
	 */
	public static DBEnum getDBEnumByDialect(int dialect) {
		for (DBEnum dbenum : DBEnum.values()) {
			if (dbenum.dialect == dialect) {
				return dbenum;
			}
		}
		return null;
	}

}
