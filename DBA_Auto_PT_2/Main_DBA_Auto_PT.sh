#!/bin/bash
################################################
## auther:liuguanghui
## create date:20190601
## update date:20191102
## work:Main_DBA_Auto_PT
################################################

##配置文件内读取配置，$1：配置文件名称 $2:配置名称(格式section.key)
function GetKey(){    
    section=$(echo $2 | cut -d '.' -f 1)
    key=$(echo $2 | cut -d '.' -f 2)
    sed -n "/\[$section\]/,/\[.*\]/{
     /^\[.*\]/d
     /^[ \t]*$/d
     /^$/d
     /^#.*$/d
     s/^[ \t]*$key[ \t]*=[ \t]*\(.*\)[ \t]*/\1/p
    }" $1
}

function modify_checksys()
{
	str=$1
	if [ "${str}" = "service sshd restart" ];then
		num=`grep -n "KillAllSSHConnection()" /home/dbfw/dbfw/bin/checksys.sh|awk -F":" '{print$1}'`
	else
		num=`grep -n "${str}()" /home/dbfw/dbfw/bin/checksys.sh|awk -F":" '{print$1}'`
	fi
	echo $num
	num=$(($num+1))
	if_checksys=`ssh root@$tcpreplay_ip "ls /home/dbfw/dbfw/bin/checksys.sh|wc -l"`
	echo $if_checksys
	if [ "${str}" = "CloseSshConnect" ];then
		/bin/sed -i "$num,$ s/^.*${str}/        : #${str}/" /home/dbfw/dbfw/bin/checksys.sh
		if [ $if_checksys -eq 1 ];then
			ssh root@$tcpreplay_ip "/bin/sed -i \"$num,$ s/^.*${str}/        : #${str}/\" /home/dbfw/dbfw/bin/checksys.sh"
		fi
	else
		/bin/sed -i "$num,$ s/^.*${str}/        #${str}/" /home/dbfw/dbfw/bin/checksys.sh
		if [ $if_checksys -eq 1 ];then
		ssh root@$tcpreplay_ip "/bin/sed -i \"$num,$ s/^.*${str}/        #${str}/\" /home/dbfw/dbfw/bin/checksys.sh"
		fi
	fi
}

##要运行tcpreplay的ip,tcpreplay_ip与DBA_ip可相同
CONFF="DBA_Auto_PT.ini"
tcpreplay_ip=$(GetKey $CONFF "Packing_Tool.tcpreplay_ip")
#注释ssh关闭
modify_checksys CloseSshConnect
modify_checksys INS_Close_SSH_Key_Login
modify_checksys INS_Clear_Login_User
modify_checksys KillAllSSHConnection
modify_checksys "service sshd restart"

mkdir -p chart_reports_histry
\cp chart_reports_dir/* chart_reports_histry

#删除原有数据目录
rm -rf chart_reports_dir data_nmon logs_dir reports_dir tcpreplay_log_dir Main_script_log
mkdir -p Main_script_log

#自动添加数据库
cd autoAddDb
chmod 777 ./autoAddDb-DBFW.sh
./autoAddDb-DBFW.sh oracl_24_1523 1 4 11020001 0 1 eth2 null null 192.168.1.24 1523 orcl2411gbk 0 null null
cd ..

##配置文件读取以下参数值

##pcap包字节数
pcap_bytes=$(GetKey $CONFF "Packing_Tool.pcap_bytes")
printf "%s\t%-6s\t%-5s\t%s\n" "[$(date +%Y-%m-%d' '%H:%M:%S)]" "$$" "INFO" "pcap_bytes=$pcap_bytes"

##pcap包内预期包数
expect_pcap=$(GetKey $CONFF "Packing_Tool.expect_pcap")
printf "%s\t%-6s\t%-5s\t%s\n" "[$(date +%Y-%m-%d' '%H:%M:%S)]" "$$" "INFO" "expect_pcap=$expect_pcap"

##tcpreplay打包速率模式: M (given Mbps), p (given packets/sec)
tcpreplay_given=$(GetKey $CONFF "Packing_Param.tcpreplay_given")
printf "%s\t%-6s\t%-5s\t%s\n" "[$(date +%Y-%m-%d' '%H:%M:%S)]" "$$" "INFO" "tcpreplay_given=$tcpreplay_given"

##多次不同速率下的起始速率
initial_rate=$(GetKey $CONFF "Packing_Param.initial_rate")
printf "%s\t%-6s\t%-5s\t%s\n" "[$(date +%Y-%m-%d' '%H:%M:%S)]" "$$" "INFO" "initial_rate=$initial_rate"

##速率变化：down 速率下调，up 速率上调
rate_change=$(GetKey $CONFF "Packing_Param.rate_change")
printf "%s\t%-6s\t%-5s\t%s\n" "[$(date +%Y-%m-%d' '%H:%M:%S)]" "$$" "INFO" "rate_change=$rate_change"

##打包次数,取值范围1,2,3,4,5,6
packing_times=$(GetKey $CONFF "Packing_Param.packing_times")
printf "%s\t%-6s\t%-5s\t%s\n" "[$(date +%Y-%m-%d' '%H:%M:%S)]" "$$" "INFO" "packing_times=$packing_times"

##速率调整步长，
packing_step=$(GetKey $CONFF "Packing_Param.packing_step")
printf "%s\t%-6s\t%-5s\t%s\n" "[$(date +%Y-%m-%d' '%H:%M:%S)]" "$$" "INFO" "packing_step=$packing_step"

##每种速率下打包时间控制在多少秒以上，此时间可以通过包信息及打包速率算出每次打包的loop
packing_time=$(GetKey $CONFF "Packing_Param.packing_time")
printf "%s\t%-6s\t%-5s\t%s\n" "[$(date +%Y-%m-%d' '%H:%M:%S)]" "$$" "INFO" "packing_time=$packing_time"


##按配置文件内设置的打包参数进行DBA性能测试,调DBA_Server_PT.sh生成相应的数据日志
packing_rate=$initial_rate
for num in `seq $packing_times`
	do
		printf "%s\t%-6s\t%-5s\t%s\n" "[$(date +%Y-%m-%d' '%H:%M:%S)]" "$$" "INFO" "num=$num"	
		if [[ $num -gt 1 ]];then	
			if [[ $rate_change = "down" ]];then
				packing_rate=$((packing_rate-packing_step))
			elif [[ $rate_change = "up" ]];then
				packing_rate=$((packing_rate+packing_step))
			else
				printf "%s\t%-6s\t%-5s\t%s\n" "[$(date +%Y-%m-%d' '%H:%M:%S)]" "$$" "ERROR" "rate_change param error,exit!"
				exit
			fi
		fi
		
		if [[ $packing_rate -le 0 ]];then
			printf "%s\t%-6s\t%-5s\t%s\n" "[$(date +%Y-%m-%d' '%H:%M:%S)]" "$$" "ERROR" "packing_rate<0,exit!"
			exit
		fi
		
		
		if [[ $tcpreplay_given = "p" ]];then
			packing_loop=$((packing_time*packing_rate/expect_pcap))
			printf "%s\t%-6s\t%-5s\t%s\n" "[$(date +%Y-%m-%d' '%H:%M:%S)]" "$$" "INFO" "packing_loop=$packing_loop"
		elif [[ $tcpreplay_given = "M" ]];then
			packing_loop=$((packing_time*packing_rate*1024*1024/pcap_bytes/8))
			printf "%s\t%-6s\t%-5s\t%s\n" "[$(date +%Y-%m-%d' '%H:%M:%S)]" "$$" "INFO" "packing_loop=$packing_loop"
		else
			printf "%s\t%-6s\t%-5s\t%s\n" "[$(date +%Y-%m-%d' '%H:%M:%S)]" "$$" "ERROR" "tcpreplay_given param error,exit!"
			exit
		fi
		
		if [[ $packing_loop -eq 0 ]];then
			packing_loop=1
			printf "%s\t%-6s\t%-5s\t%s\n" "[$(date +%Y-%m-%d' '%H:%M:%S)]" "$$" "INFO" "packing_loop=$packing_loop!"
		fi
			
		printf "%s\t%-6s\t%-5s\t%s\n" "[$(date +%Y-%m-%d' '%H:%M:%S)]" "$$" "INFO" \
			 "PT_CMD:./DBA_Auto_PT.sh ${tcpreplay_given} ${packing_rate} ${packing_loop} > ./Main_script_log/${tcpreplay_given}_${packing_rate}_${packing_loop}.log"	
		./DBA_Auto_PT.sh ${tcpreplay_given} ${packing_rate} ${packing_loop} > ./Main_script_log/${tcpreplay_given}_${packing_rate}_${packing_loop}.log
		if [[ $packing_times -eq 1 ]];then
			sleep 10
		else
			sleep 600
		fi
	done


##把DBA_Server_PT.sh生成相应的数据日志，通过此脚本生成excl Chart图,并压缩为tar.gz文件
\cp ./Readme_DBA_Auto_PT_DES.docx ./chart_reports_dir
python DBA_Auto_PT_MakeChart.py > ./Main_script_log/DBA_Auto_PT_MakeChart.log
