
参数1：name受保护数据库名称
参数2：type数据库类型对应的值
type
1	Oracle
2	MSSQL
3	DB2
4	MySql
5	POSTGRE
6	Sybase
9	Informix
10	DM
11	Kingbase
12	GBase
15	Oscar
16	CacheDb
18	Gbase8t
#查询数据库type对应值
SELECT DISTINCT dbkind,name FROM  dbversion left JOIN xsec_dictionary on
xsec_dictionary.`value`=dbversion.dialect

参数3：isglba表示服务器的操作系统
对应的值 1～9，0表示未使用(未知) 1-Windows32 2-Windows64 3-Linux 32 4-Linux64(默认值) 5-AIX32 6-Aix64 7-Solaris32 8-Solaris64 9-HPUnix64

参数4：version数据库版本
参照dbversion表dbverval字段（dbkind为数据库类型见-参数2）

参数5：isdpa数据库字符集
Oracle数据库 ZHS16GBK=0 ZHT32EUC=1 ZHT16BIG5=2 AL32UTF8=3
其他数据库 GBK=0 Latin=1 UTF8=4 其他=9

参数6：address数据库IP地址
参数7：port数据库端口
参数8：serviceName数据库实例名（如果没有默认传NULL）
参数9：dynaPort是否为动态端口 0-非动态端口 1-动态端口（Oracle9i版本才支持此选项）
参数10：userName数据库账户（如果没有默认传NULL）
参数11：userPwd数据库数据库密码（如果没有默认传NULL）

多地址时参数6-参数11多次传值

例子
单地址：./autoAddDBForAudit.sh  auto_oracle  1  2  10020002  0 192.168.1.199 1521 orcl 1 null  null
多地址：./autoAddDBForAudit.sh  auto_oracle  1  2  10020002  0 192.168.1.109 1521 orcl 1 null  null 192.168.1.125 1521 orcl 1 null  null
