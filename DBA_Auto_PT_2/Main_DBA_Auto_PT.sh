#!/bin/bash
################################################
## auther:liuguanghui
## create date:20190601
## update date:20191102
## work:Main_DBA_Auto_PT
################################################

##�����ļ��ڶ�ȡ���ã�$1�������ļ����� $2:��������(��ʽsection.key)
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

##Ҫ����tcpreplay��ip,tcpreplay_ip��DBA_ip����ͬ
CONFF="DBA_Auto_PT.ini"
tcpreplay_ip=$(GetKey $CONFF "Packing_Tool.tcpreplay_ip")
#ע��ssh�ر�
modify_checksys CloseSshConnect
modify_checksys INS_Close_SSH_Key_Login
modify_checksys INS_Clear_Login_User
modify_checksys KillAllSSHConnection
modify_checksys "service sshd restart"

#ɾ��ԭ������Ŀ¼
#rm -rf chart_reports_dir data_nmon logs_dir reports_dir tcpreplay_log_dir Main_script_log
mkdir -p Main_script_log

#�Զ�������ݿ�
cd autoAddDb
./autoAddDb-DBFW.sh oracl_24_1523 1 4 11020001 0 1 eth2 null null 192.168.1.24 1523 orcl2411gbk 0 null null
cd ..

##�����ļ���ȡ���²���ֵ

##pcap���ֽ���
pcap_bytes=$(GetKey $CONFF "Packing_Tool.pcap_bytes")
printf "%s\t%-6s\t%-5s\t%s\n" "[$(date +%Y-%m-%d' '%H:%M:%S)]" "$$" "INFO" "pcap_bytes=$pcap_bytes"

##pcap����Ԥ�ڰ���
expect_pcap=$(GetKey $CONFF "Packing_Tool.expect_pcap")
printf "%s\t%-6s\t%-5s\t%s\n" "[$(date +%Y-%m-%d' '%H:%M:%S)]" "$$" "INFO" "expect_pcap=$expect_pcap"

##tcpreplay�������ģʽ: M (given Mbps), p (given packets/sec)
tcpreplay_given=$(GetKey $CONFF "Packing_Param.tcpreplay_given")
printf "%s\t%-6s\t%-5s\t%s\n" "[$(date +%Y-%m-%d' '%H:%M:%S)]" "$$" "INFO" "tcpreplay_given=$tcpreplay_given"

##��β�ͬ�����µ���ʼ����
initial_rate=$(GetKey $CONFF "Packing_Param.initial_rate")
printf "%s\t%-6s\t%-5s\t%s\n" "[$(date +%Y-%m-%d' '%H:%M:%S)]" "$$" "INFO" "initial_rate=$initial_rate"

##���ʱ仯��down �����µ���up �����ϵ�
rate_change=$(GetKey $CONFF "Packing_Param.rate_change")
printf "%s\t%-6s\t%-5s\t%s\n" "[$(date +%Y-%m-%d' '%H:%M:%S)]" "$$" "INFO" "rate_change=$rate_change"

##�������,ȡֵ��Χ1,2,3,4,5,6
packing_times=$(GetKey $CONFF "Packing_Param.packing_times")
printf "%s\t%-6s\t%-5s\t%s\n" "[$(date +%Y-%m-%d' '%H:%M:%S)]" "$$" "INFO" "packing_times=$packing_times"

##���ʵ���������
packing_step=$(GetKey $CONFF "Packing_Param.packing_step")
printf "%s\t%-6s\t%-5s\t%s\n" "[$(date +%Y-%m-%d' '%H:%M:%S)]" "$$" "INFO" "packing_step=$packing_step"

##ÿ�������´��ʱ������ڶ��������ϣ���ʱ�����ͨ������Ϣ������������ÿ�δ����loop
packing_time=$(GetKey $CONFF "Packing_Param.packing_time")
printf "%s\t%-6s\t%-5s\t%s\n" "[$(date +%Y-%m-%d' '%H:%M:%S)]" "$$" "INFO" "packing_time=$packing_time"


##�������ļ������õĴ����������DBA���ܲ���,��DBA_Server_PT.sh������Ӧ��������־
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
		./DBA_Auto_PT.sh $tcpreplay_given $packing_rate $packing_loop > ./Main_script_log/$tcpreplay_given_$packing_rate_$packing_loop.log
		sleep 300
	done

##��DBA_Server_PT.sh������Ӧ��������־��ͨ���˽ű�����excl Chartͼ,��ѹ��Ϊtar.gz�ļ�
python DBA_Auto_PT_MakeChart.py > ./Main_script_log/DBA_Auto_PT_MakeChart.log
