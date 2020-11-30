#!/bin/bash

export DBFW_HOME=/home/dbfw/dbfw
export DBFW_INSTANCE_FILE_PATH=$DBFW_HOME/etc
export PATH=$PATH:$DBFW_HOME/bin
export LD_LIBRARY_PATH=$DBFW_HOME/lib:$LD_LIBRARY_PATH

#if [ ! -d "/usr/local/tomcat/webapps/ROOT/WEB-INF/classes/cn/schina/dbfw/auto" ];then
#	basepath=$(cd "$(dirname "$0")"; pwd)
#	cp -raf $basepath/auto /usr/local/tomcat/webapps/ROOT/WEB-INF/classes/cn/schina/dbfw
#fi
#rm -f /usr/local/tomcat/webapps/ROOT/WEB-INF/classes/cn/dbsec/dbfw/aspect/ControllerAspect*
#if [ -d "/usr/local/tomcat/webapps/ROOT/WEB-INF/classes/cn/dbsec/dbfw/aspect" ];then
#	basepath=$(cd "$(dirname "$0")"; pwd)
#	cp -raf $basepath/class/* /usr/local/tomcat/webapps/ROOT/WEB-INF/classes/cn/dbsec/dbfw/aspect
#fi
#if [ -d "/usr/local/tomcat/webapps/ROOT/WEB-INF/classes/cn/dbsec/dbfw/modual/auditlog" ];then
#	basepath=$(cd "$(dirname "$0")"; pwd)
#	cp -raf $basepath/SystemAuditResultSingelton.class /usr/local/tomcat/webapps/ROOT/WEB-INF/classes/cn/dbsec/dbfw/modual/auditlog
#fi

## 增加设置环境变量 ##
. /etc/profile

#增加防守，防止同时启动多个
PID_FILE="/home/dbfw/dbfw/bin/addDb.pid"
if [ -e "$PID_FILE" ]
then
	CUR_PROCESS_PID=`cat ${PID_FILE}|awk -F '|' '{print $1}'`
	if [ -d "/proc/$CUR_PROCESS_PID" ];then
		CUR_PROCESS_NUM=`ps -ef |grep -v grep|grep -w $CUR_PROCESS_PID|grep "addDb.sh"|wc -l`
		if [ "$CUR_PROCESS_NUM" -ge 1 ];then
			#echo "abort this for have other same running...$CUR_PROCESS_NUM" ;
			exit;
		fi
	fi
else
	echo "not found $PID_FILE"
fi

echo $$ > $PID_FILE

for args in $@
do
	str="$str $args"
done


#echo "begin addDb";

cd /usr/local/tomcat/webapps/ROOT/WEB-INF/classes

cp -f ../configures.properties ./

sed -i 's/classpath:..\/configures.properties/classpath:configures.properties/' ./applicationContext.xml

sed -i '/uploadTempDir/d' ./applicationContext.xml

java -classpath .:$JAVA_HOME/lib/dt.jar:$JAVA_HOME/lib/tools.jar:/usr/local/tomcat/webapps/ROOT/WEB-INF/lib/* -Djdbc.properties=/usr/local/tomcat/webapps/ROOT/WEB-INF/configures.properties -Xms256m -Xmx512m -XX:PermSize=64M -XX:MaxPermSize=128m  cn.schina.dbfw.auto.AutoAddDbMain $str

#echo "end addDb";
