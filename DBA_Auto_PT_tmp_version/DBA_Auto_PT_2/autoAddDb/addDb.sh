#!/bin/bash

export DBFW_HOME=/home/dbfw/dbfw
export DBFW_INSTANCE_FILE_PATH=$DBFW_HOME/etc
export PATH=$PATH:$DBFW_HOME/bin
export LD_LIBRARY_PATH=$DBFW_HOME/lib:$LD_LIBRARY_PATH

if [ ! -s "/usr/local/tomcat/webapps/ROOT/WEB-INF/classes/cn/schina/dbfw/test/AutoAddDatabase.class" ];then
	basepath=$(cd "$(dirname "$0")"; pwd)
	cp -raf $basepath/AutoAddDatabase.class /usr/local/tomcat/webapps/ROOT/WEB-INF/classes/cn/schina/dbfw/test/AutoAddDatabase.class
fi

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

JDK_PATH="/usr/lib/jdk18/bin/java"

if [ ! -e $JDK_PATH ];then
	JDK_PATH="/usr/lib/jdk17/bin/java"
	if [ ! -e $JDK_PATH ];then
		JDK_PATH="/usr/lib/jre164/jre1.6.0_45/bin/java"
	fi
fi

#echo "begin addDb";

cd /usr/local/tomcat/webapps/ROOT/WEB-INF/classes

$JDK_PATH -classpath .:$JAVA_HOME/lib/dt.jar:$JAVA_HOME/lib/tools.jar:/usr/local/tomcat/webapps/ROOT/WEB-INF/lib/* -Djdbc.properties=/usr/local/tomcat/webapps/ROOT/WEB-INF/configures.properties -Xms256m -Xmx512m -XX:PermSize=64M -XX:MaxPermSize=128m  cn.schina.dbfw.test.AutoAddDatabase $str

#echo "end addDb";
