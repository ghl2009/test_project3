
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

参数3：isglba表示服务器的操作系统
对应的值 1～9，0表示未使用(未知) 1-Windows32 2-Windows64 3-Linux 32 4-Linux64(默认值) 5-AIX32 6-Aix64 7-Solaris32 8-Solaris64 9-HPUnix64

参数4：version数据库版本
参照dbversion表dbverval字段（dbkind为数据库类型见-参数2）

参数5：isdpa数据库字符集
Oracle数据库 ZHS16GBK=0 ZHT32EUC=1 ZHT16BIG5=2 AL32UTF8=3
其他数据库 GBK=0 Latin=1 UTF8=4 其他=9
参数6：添加网卡的类型，1-旁路组  2-网桥组  3-代理组
参数7：网卡名称，传入网卡名称，例如：eth0（网桥组传如一个网卡名称即可,但是需要传入的网卡名能够与网桥关联表bypassnc的值对应）
参数8：代理组IP，端口默认为从10000开始,非代理组传入NULL
参数9：代理组掩码

参数10：address数据库IP地址
参数11：port数据库端口
参数12：serviceName数据库实例名（如果没有默认传NULL）
参数13：dynaPort是否为动态端口 0-非动态端口 1-动态端口（Oracle9i版本才支持此选项）
参数14：userName数据库账户（如果没有默认传NULL）
参数15：userPwd数据库数据库密码（如果没有默认传NULL）

多地址时参数10-参数15多次传值

例子
单地址：./autoAddDBForDbfw.sh  auto_oracle  1  2  10020002  0 1 eth1 null null 192.168.1.199 1521 orcl 1 null  null
多地址：./autoAddDBForDbfw.sh  auto_oracle  1  2  10020002  0 2 eth4 null null 192.168.1.109 1521 orcl 1 null  null 192.168.1.125 1521 orcl 1 null  null
代理模式: ./autoAddDBForCoco.sh dorcl_2     1  4  11020001  0 3 eth1 192.168.6.255 255.255.0.0 192.168.1.5  1522  orcl2411gbk 0 null null

代理组多地址：./autoAddDBForCocoDbfw.sh  auto_oracle 1 2 10020002 0 3 eth1 192.168.30.80 255.255.0.0 192.168.1.109 1521 orcl 1 null  null 192.168.1.125 1521 orcl 1 null  null



SELECT
	database_addresses.database_id,
	xsec_databases.`name`,
	database_addresses.address,
	database_addresses.`port`,
	interface_group.group_ip,
	group_port.port_num
FROM
	database_addresses database_addresses
INNER JOIN dbfw_fordb dbfw_fordb ON dbfw_fordb.address_id =
database_addresses.address_id
AND dbfw_fordb.monitor_type = 3
INNER JOIN group_port group_port ON group_port.port_id = dbfw_fordb.port_id
LEFT JOIN interface_group interface_group ON interface_group.group_id =
group_port.group_id
LEFT JOIN xsec_databases xsec_databases ON xsec_databases.database_id =
database_addresses.database_id

