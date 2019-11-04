#!/bin/bash
################################################
## auther:liuguanghui
## create date:20181218
## update date:20191102
## work:DBA nfw/npp/tla/ftm performance testing
################################################

#*此区间内参数需进行配置********************************************************************
##配置文件情况名称
CONFF="DBA_Auto_PT.ini"

##定义要DBA的ip
DBA_ip=$(GetKey $CONFF "Product_Info.DBA_ip")
##定义DBA收包网卡
DBA_eth_R=$(GetKey $CONFF "Product_Info.DBA_eth_R")

##要运行tcpreplay的ip,tcpreplay_ip与DBA_ip可相同
tcpreplay_ip=$(GetKey $CONFF "Packing_Tool.tcpreplay_ip")

##tcpreplay打包参数 即:打包命令
##注:1、pcap包需要定义绝对路径 2、需要把包按路径放于打包设备上 

##tcpreplay发包网卡
tcpreplay_eth_T=$(GetKey $CONFF "Packing_Tool.tcpreplay_eth_T")

##pcap包所在路径及名称
pcap_name=$(GetKey $CONFF "Packing_Tool.pcap_name")
pcappath=`dirname $pcap_name`
pcapname=`basename $pcap_name`
ifpcap=`ssh root@$tcpreplay_ip ls $pcap_name |wc -l 2>/dev/null`
if [ "$ifpcap" -eq 0 ];then
	ssh root@$tcpreplay_ip wget -q -t 3 -P $pcappath ftp://192.168.0.5/autotest/$pcapname --user autotest --password autotest
fi

##tcpreplay打包速率模式: M (given Mbps), p (given packets/sec)
tcpreplay_given="$1"

##tcpreplay打包速率
tcpreplay_rate="$2"

##tcpreplay打包loop数
tcpreplay_loop="$3"

##所打的pcap包内预期包数
expect_pcap=$(GetKey $CONFF "Packing_Tool.expect_pcap")

##所打的pcap包内预期sql数
expect_sql=$(GetKey $CONFF "Packing_Tool.expect_sql")

##参上打包参数都设置完后，以下打包命令自动生成
tcpreplay_cmd="tcpreplay -$tcpreplay_given $tcpreplay_rate -l $tcpreplay_loop -i $tcpreplay_eth_T $pcap_name"

##定义取数据的周期，单位:秒 取值范围 60 120 180 ...
Get_info_cycle=$(GetKey $CONFF "PT_Scripts.Get_info_cycle")

##定义此脚本是否在DBA运行 1 是; 2 否 注：建议DBA本机运行,因为在DBA取数据时速度快
PT_RUN_DBA=$(GetKey $CONFF "PT_Scripts.PT_RUN_DBA")

#********************************************************************************************

##程序log输出函数
function printf_log()
{
        if [ $1 -eq 1 ];then
                printf "%s\t%-6s\t%-5s\t%s\n" "[$(date +%Y-%m-%d' '%H:%M:%S)]" "$$" "INFO" "$2"
                printf "%s\t%-6s\t%-5s\t%s\n" "[$(date +%Y-%m-%d' '%H:%M:%S)]" "$$" "INFO" "$2" >> $LOGS_DIR/$PT_log_name
        elif [ $1 -eq 0 ];then
                printf "%s\t%-6s\t%-5s\t%s\n" "[$(date +%Y-%m-%d' '%H:%M:%S)]" "$$" "ERROR" "$2"
                printf "%s\t%-6s\t%-5s\t%s\n" "[$(date +%Y-%m-%d' '%H:%M:%S)]" "$$" "ERROR" "$2" >> $LOGS_DIR/$PT_log_name
	else
                print "function prinf_log pass param error!"
        fi
}

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

##ssh免密登录函数,ssh-keygen login
function ssh_keygen_login()
{

        T_key_ip=`ifconfig eth0|awk '/inet addr/{print $2}'|awk -F : '{print $2}'`
        R_key_ip=$1
        key_yes_or_no=$2

        cmd1="sed -i '/\bRSAAuthentication\b/s/.*/RSAAuthentication yes/' /etc/ssh/sshd_config"
        cmd2="sed -i '/\bPubkeyAuthentication\b/s/.*/PubkeyAuthentication yes/' /etc/ssh/sshd_config"
        cmd3="sed -i '/\bStrictHostKeyChecking\b/s/.*/StrictHostKeyChecking no/' /etc/ssh/ssh_config"
        cmd4="sed -i '/\bAuthorizedKeysFile\b/s/.*/AuthorizedKeysFile .ssh\/authorized_keys/' /etc/ssh/sshd_config"
        cmd5="sed -i '/\bRSAAuthentication\b/s/.*/RSAAuthentication no/' /etc/ssh/sshd_config"
        cmd6="sed -i '/\bPubkeyAuthentication\b/s/.*/PubkeyAuthentication no/' /etc/ssh/sshd_config"
        cmd7="sed -i '/\bStrictHostKeyChecking\b/s/.*/#   StrictHostKeyChecking ask/' /etc/ssh/ssh_config"
        cmd8="rm -rf /root/.ssh/authorized_keys"
        cmd9="service sshd restart >>/dev/null"

        if [[ $2 == y ]];then
                scp /root/.ssh/{authorized_keys,id_rsa}  root@$R_key_ip:/root/.ssh/
                ssh root@$R_key_ip "$cmd1;$cmd2;$cmd3;$cmd4;$cmd9"
        elif [[ $2 == n ]];then
                ssh root@$R_key_ip "$cmd5;$cmd6;$cmd7;$cmd8;$cmd9"
        else
                printf_log 0 "Please pass in parameters correctly."
                exit
        fi
}

##此函数得到发包网卡,所发包及丢包数
function Get_eth_T_info()
{
	tcpreplay_ip="$1"
	tcpreplay_eth_T="$2"

	if [[ $tcpreplay_kernel_version != "el7" ]];then
		tcpreplay_Tx_pck=`ssh root@$tcpreplay_ip ifconfig $tcpreplay_eth_T|grep "TX packets"|awk '{print $2}'|awk -F: '{print $2}'`	
		tcpreplay_T_d_pck=`ssh root@$tcpreplay_ip ifconfig $tcpreplay_eth_T|grep "TX packets"|awk '{print $4}'|awk -F: '{print $2}'`
	else
		tcpreplay_Tx_pck=`ssh root@$tcpreplay_ip "ifconfig $tcpreplay_eth_T|grep 'TX packets'"|awk '{print $3}'`
		tcpreplay_T_d_pck=`ssh root@$tcpreplay_ip "ifconfig $tcpreplay_eth_T|grep 'TX errors'"|awk '{print $5}'`
	fi
	
	##此处这样处理不行，第一次打包时，这个网卡发包数就是0,还需要做标记处理，先搁置。。。
	#if [[ $tcpreplay_Tx_pck -eq 0 ]];then
	#	printf_log 0 "Get $tcpreplay_eth_T tcpreplay_Tx_pck=$tcpreplay_Tx_pck"
	#	exit
	#fi

	tcpreplay_eth_seep=`ssh root@$tcpreplay_ip "ethtool $tcpreplay_eth_T|grep Speed"|awk '{print $2}'|awk -FM '{print $1}'`
	if [[ $tcpreplay_eth_seep -lt 1000 ]];then
		printf_log 0 "Get $tcpreplay_eth_T tcpreplay_eth_seep=$tcpreplay_eth_seep"
		exit
	fi

	printf_log 1 "Get $tcpreplay_eth_T tcpreplay_Tx_pck=$tcpreplay_Tx_pck tcpreplay_T_d_pck=$tcpreplay_T_d_pck"
} 

##此函数得到收包网卡,所收包及丢包数
function Get_eth_R_info()
{
	DBA_ip="$1"
	DBA_eth_R="$2"
	if [[ PT_RUN_DBA -eq 1 ]];then
		if [[ $DBA_kernel_version != "el7" ]];then
			DBA_Rx_pck=`ifconfig $DBA_eth_R|grep 'RX packets'|awk '{print $2}'|awk -F: '{print $2}'`
			DBA_R_d_pck=`ifconfig $DBA_eth_R|grep 'RX packets'|awk '{print $4}'|awk -F: '{print $2}'`
		else
			DBA_Rx_pck=`ifconfig $DBA_eth_R|grep 'RX packets'|awk '{print $3}'`
			DBA_R_d_pck=`ifconfig $DBA_eth_R|grep 'RX packets'|awk '{print $5}'`
		fi
	else
		if [[ $DBA_kernel_version != "el7" ]];then
			DBA_Rx_pck=`ssh root@$DBA_ip "ifconfig $DBA_eth_R|grep 'RX packets'"|awk '{print $2}'|awk -F: '{print $2}'`
			DBA_R_d_pck=`ssh root@$DBA_ip "ifconfig $DBA_eth_R|grep 'RX packets'"|awk '{print $4}'|awk -F: '{print $2}'`
		else
			DBA_Rx_pck=`ssh root@$DBA_ip "ifconfig $DBA_eth_R|grep 'RX packets'"|awk '{print $3}'`
			DBA_R_d_pck=`ssh root@$DBA_ip "ifconfig $DBA_eth_R|grep 'RX packets'"|awk '{print $5}'`
		fi
	fi
	printf_log 1 "Get DBA_Rx_pck=$DBA_Rx_pck DBA_R_d_pck=$DBA_R_d_pck"
} 

##此函数定义dpdk状态文件,收包网卡收包数，丢包数，错误包数
function Get_dpdk_stats()
{
	DBA_ip="$1"
	DBA_eth_R="$2"
	if [[ PT_RUN_DBA -eq 1 ]];then
		eth_R_flag=`cat /tmp/dpdk/eth_pci_map|grep "${DBA_eth_R}=" |awk -F = '{print $2}'`
		str_row=`grep -n "$eth_R_flag" /dev/shm/dpdk/dpdk_nic_stats|cut -d ':' -f 1`
		eth_drive_map=`cat /tmp/dpdk/drive_map |grep "$eth_R_flag"`
		dpdk_rx_pkt_tot=`sed -n "$((str_row+2))p" /dev/shm/dpdk/dpdk_nic_stats |awk '{print $3}'`
		dpdk_rx_pkt_err=`sed -n "$((str_row+4))p" /dev/shm/dpdk/dpdk_nic_stats |awk '{print $3}'`
		dpdk_drop_pkt_hw=`sed -n "$((str_row+6))p" /dev/shm/dpdk/dpdk_nic_stats |awk '{print $3}'`
		dpdk_drop_pkt_tot=`sed -n "$((str_row+7))p" /dev/shm/dpdk/dpdk_nic_stats |awk '{print $3}'`
	else
		eth_R_flag=`ssh root@$DBA_ip cat /tmp/dpdk/eth_pci_map|grep "${DBA_eth_R}" |awk -F = '{print $2}'`
		str_row=`ssh root@$DBA_ip grep -n "$eth_R_flag" /dev/shm/dpdk/dpdk_nic_stats|cut -d ':' -f 1`
		dpdk_rx_pkt_tot=`ssh root@$DBA_ip sed -n "$((str_row+2))p" /dev/shm/dpdk/dpdk_nic_stats |awk '{print $3}'`
		dpdk_rx_pkt_err=`ssh root@$DBA_ip sed -n "$((str_row+4))p" /dev/shm/dpdk/dpdk_nic_stats |awk '{print $3}'`
		dpdk_drop_pkt_hw=`ssh root@$DBA_ip sed -n "$((str_row+6))p" /dev/shm/dpdk/dpdk_nic_stats |awk '{print $3}'`
		dpdk_drop_pkt_tot=`ssh root@$DBA_ip sed -n "$((str_row+7))p" /dev/shm/dpdk/dpdk_nic_stats |awk '{print $3}'`
	fi
	printf_log 1 "Get dpdk rx_pkt_tot=$dpdk_rx_pkt_tot rx_pkt_err=$dpdk_rx_pkt_err drop_pkt_hw=$dpdk_drop_pkt_hw drop_pkt_tot=$dpdk_drop_pkt_tot"	
}

##此函数定义npc状态文件,收包网卡收包数，丢包数，错误包数
function Get_npc_stats()
{
	if [[ PT_RUN_DBA -eq 1 ]];then
		npc_rx_all_pkt_tot=`cat /dev/shm/npc/npc_packet_stats|grep 'rx_all_pkt_tot' |awk '{print $3}'`
	else
		npc_rx_all_pkt_tot=`ssh root@$DBA_ip "cat /dev/shm/npc/npc_packet_stats|grep 'rx_all_pkt_tot'"|awk '{print $3}'`
	fi
	printf_log 1 "Get npc_packet_stats npc_rx_all_pkt_tot=$npc_rx_all_pkt_tot}"	
}

##此函数定义往共享内存内得到已解析sql数
function Get_sga_sql()
{
	DBA_ip="$1"

	if [[ PT_RUN_DBA -eq 1 ]];then
		sga_sql=`$DBFW_HOME/bin/dumpsess 0 |awk '{print $7}'`
		sga_count="$sga_sql"
	else
		sga_sql=`ssh root@$DBA_ip $DBFW_HOME/bin/dumpsess 0 |awk '{print $7}'`
		sga_count="$sga_sql"
	fi
	printf_log 1 "Get sga_sql=$sga_sql"
}

##此函数定义从inst_db_count表得sql数
function Get_db_count()
{
        DBA_ip="$1"
        if [[ PT_RUN_DBA -eq 1 ]];then
                db_count_sql=`$DBCDataView_dc dbfw -N -e 'SELECT ifnull(sum(count),0) FROM inst_db_count where is_deleted=0;'`
        else
                db_count_sql=`ssh root@$DBA_ip "$DBCDataView_dc dbfw -N -e 'SELECT ifnull(sum(count),0) FROM inst_db_count where is_deleted=0;'"`
        fi
        printf_log 1 "Get inst_db_count table sql=$db_count_sql"
}

##此函数定义从trace_logs_detail_part表得到数据条数
function Get_trace_count()
{
        DBA_ip="$1"
        if [[ PT_RUN_DBA -eq 1 ]];then
		##按是否为双数据中心取trace_logs_detail_part表中的数据条数
		if [[ $DBC_count -eq 1 ]];then
			trace_count=`$DBCDataView_dc dbfw -N -e 'select count(tlogid) from trace_logs_detail_part;'`
			if [[ -z $trace_count ]];then
				trace_count=0
			fi

		elif [[ $DBC_count -eq 2 ]];then
			trace_count=`$DBCDataView_tc dbfw -N -e 'select count(tlogid) from trace_logs_detail_part;'`
			if [[ -z $trace_count ]];then
				trace_count=0
			fi
			for t_num in `seq 1 3`
				do
					trace_count_tmp=`$DBCDataView_tc dbfw -N -e "select count(tlogid) from trace_logs_detail_part_${t_num};"`
					if [[ -z $trace_count ]];then
						trace_count_tmp=0
					fi
					((trace_count=trace_count+trace_count_tmp))
				done
		fi

        else

		##按是否为双数据中心取trace_logs_detail_part表中的数据条数
		if [[ $DBC_count -eq 1 ]];then
			trace_count=`ssh root@$DBA_ip "$DBCDataView_dc dbfw -N -e 'select count(tlogid) from trace_logs_detail_part;'"`
			if [[ -z $trace_count ]];then
				trace_count=0
			fi

		elif [[ $DBC_count -eq 2 ]];then
			trace_count=`ssh root@$DBA_ip "$DBCDataView_tc dbfw -N -e 'select count(tlogid) from trace_logs_detail_part;'"`
			if [[ -z $trace_count ]];then
				trace_count=0
			fi
			for t_num in `seq 1 3`
				do
					trace_count_tmp=`ssh root@$DBA_ip "$DBCDataView_tc dbfw -N -e 'select count(tlogid) from trace_logs_detail_part_${t_num};'"`
					if [[ -z $trace_count ]];then
						trace_count_tmp=0
					fi
					((trace_count=trace_count+trace_count_tmp))
				done
		fi

        fi
        printf_log 1 "Get trace_logs_detail_part table count=$trace_count"
}

##此函数定义从summary_xml_history表查看索引创建条数
function Get_index_count()
{
        DBA_ip="$1"

        if [[ PT_RUN_DBA -eq 1 ]];then
		index_count=`$DBCDataView_dc dbfw -N -e 'SELECT sum(record_count) FROM summary_xml_history where isdelete=1;'`
        else
		index_count=`ssh root@$DBA_ip "$DBCDataView_dc dbfw -N -e 'SELECT sum(record_count) FROM summary_xml_history where isdelete=1;'"`
	fi


	if [[ -z $index_count ]];then
       		index_count=0
	fi

        printf_log 1 "Get summary_xml_history table index_count=$index_count"

}


##此函数定义从system_param表查看统计进度
function Get_summary_count()
{
        DBA_ip="$1"

        if [[ PT_RUN_DBA -eq 1 ]];then
                ##按是否为双数据中心取表system_param中的统计进度
                if [[ $DBC_count -eq 1 ]];then
                        summary_count=`$DBCDataView_dc dbfw -N -e 'SELECT intvalue FROM system_param where id=1;'`
                        ((summary_count=summary_count-3000000000))
                        if [[ -z $summary_count ]];then
                                summary_count=0
                        fi

                elif [[ $DBC_count -eq 2 ]];then
                        summary_count=`$DBCDataView_dc dbfw -N -e 'SELECT intvalue FROM system_param where id in (1,4);'`
			summary_count=`echo $summary_count|awk '{print $1+$2-6000000000}'`
                        if [[ -z $summary_count ]];then
                                summary_count=0
                        fi
                fi
        else
                ##按是否为双数据中心取表system_param中的统计进度
                if [[ $DBC_count -eq 1 ]];then
                        summary_count=`ssh root@$DBA_ip "$DBCDataView_dc dbfw -N -e 'SELECT intvalue FROM system_param where id=1;'"`
                        ((summary_count=summary_count-3000000000))
                        if [[ -z $summary_count ]];then
                                summary_count=0
                        fi

                elif [[ $DBC_count -eq 2 ]];then
                        summary_count=`ssh root@$DBA_ip "$DBCDataView_dc dbfw -N -e 'SELECT intvalue FROM system_param where id in (1,4);'"`
                        summary_count=`echo $summary_count|awk '{print $1+$2-6000000000}'`
                        if [[ -z $summary_count ]];then
                                summary_count=0
                        fi
                fi
        fi
        printf_log 1 "Get system_param table summary_count=$summary_count"

}

##此函数定义从nmon执行结果里取cpu信息
function Get_cpu_info()
{
	##cpu_name表示是CPU_All、CPU001等等
	cpu_name="$1"
	##cpu_tpye值为 3 User%， 4 Sys%
	cpu_type="$2"
	##取nmon最新几条数据
	nmon_num="$3"
	if [[ PT_RUN_DBA -eq 1 ]];then
		cpu_max=`cat $nmon_file_name |grep "$cpu_name" |grep -v "Wait%" |tail -n$nmon_num \
			|awk -v i=$cpu_type -F, 'BEGIN {max = 0} {if ($i+0>max+0) max=$i fi} END {print max}'`
		cpu_avg=`cat $nmon_file_name |grep "$cpu_name" |grep -v "Wait%" |tail -n$nmon_num \
			|awk -v i=$cpu_type -F, '{sum+=$i} END {printf "%.1f\n", sum/NR}'`
		cpu_min=`cat $nmon_file_name |grep "$cpu_name" |grep -v "Wait%" |tail -n$nmon_num \
			|awk -v i=$cpu_type -F, 'BEGIN {min = 1000} {if ($i+0<min+0) min=$i fi} END {print min}'`
	else
		cpu_max=`ssh root@$DBA_ip cat $nmon_file_name |grep "$cpu_name" |grep -v "Wait%" |tail -n$nmon_num \
			|awk -v i=$cpu_type -F, 'BEGIN {max = 0} {if ($i+0>max+0) max=$i fi} END {print max}'`
                cpu_avg=`ssh root@$DBA_ip cat $nmon_file_name |grep "$cpu_name" |grep -v "Wait%" |tail -n$nmon_num \
			|awk -v i=$cpu_type -F, '{sum+=$i} END {printf "%.1f\n", sum/NR}'`
                cpu_min=`ssh root@$DBA_ip cat $nmon_file_name |grep "$cpu_name" |grep -v "Wait%" |tail -n$nmon_num \
			|awk -v i=$cpu_type -F, 'BEGIN {min = 1000} {if ($i+0<min+0) min=$i fi} END {print min}'`
	fi
	if [[ $cpu_type -eq 3 ]];then
		cpu_type='%User'
	elif [[ $cpu_type -eq 4 ]];then
		cpu_type='%Sys'		
	fi
	printf_log 1 "Get ${cpu_name} ${cpu_type} MAX=$cpu_max AVG=$cpu_avg MIN=$cpu_min"
}

##此函数定义从nmon执行结果里取MEM信息
function Get_mem_info()
{
	##取nmon最新几条数据
	nmon_num="$1"
	if [[ PT_RUN_DBA -eq 1 ]];then
		mem_total=`cat $nmon_file_name |grep "MEM" |grep -v "memtotal" |tail -n1 |awk -F, '{print $3}'` 
		mem_free=`cat $nmon_file_name |grep "MEM" |grep -v "memtotal" |tail -n1 |tail -n$nmon_num \
			|awk -F, '{sum+=$7} END {printf "%.1f\n", sum/NR}'`
		mem_used=`echo "$mem_total $mem_free"|awk '{printf ("%0.1f\n",$1-$2)}'`
		mem_used_per=`echo "$mem_used $mem_total"|awk '{printf ("%0.1f\n",$1*100/$2)}'`
	else
                mem_total=`ssh root@$DBA_ip cat $nmon_file_name |grep "MEM" |grep -v "memtotal" |tail -n1 |awk -F, '{print $3}'`
                mem_free=`ssh root@$DBA_ip  cat $nmon_file_name |grep "MEM" |grep -v "memtotal" |tail -n1 |tail -n$nmon_num \
                        |awk -F, '{sum+=$7} END {printf "%.1f\n", sum/NR}'`
		mem_used=`echo "$mem_total $mem_free"|awk '{printf ("%0.1f\n",$1-$2)}'`
                mem_used_per=`echo "$mem_used $mem_total"|awk '{printf ("%0.1f\n",$1*100/$2)}'`
	fi
	printf_log 1 "Get MEM mem_total=$mem_total mem_free=$mem_free mem_used=$mem_used mem_used_per=$mem_used_per"
}

##tla error日志中看是否存在Begin_to_loose
function Get_Begin_to_loose()
{
	Begin_to_loose_count=`cat /dbfw_capbuf/pdump/tla/error/tla_error |grep 'Begin to loose'|wc -l`
}

##取系统cpu mem信息
function Get_Sys_Info()
{
	local cpu mem disk

	cpu=`ssh root@$DBA_ip cat /proc/cpuinfo|grep processor|tail -n 1|awk '{print $3+1}'`
	mem=`ssh root@$DBA_ip /usr/bin/free -g|grep 'Mem'|awk '{print $2}'`
	disk=`ssh root@$DBA_ip fdisk -l 2>/dev/null|grep -E 'Disk /dev/sd|Disk /dev/xvd'|awk '{sum+=$5} END {print sum/1000/1000/1000}'`

	if [ "$mem" -le 32 ];then
		((mem=$mem+1))
	elif [ "$mem" -gt 32 -a "$mem" -le 64 ];then
		((mem=$mem+2))
	elif [ "$mem" -gt 64 -a "$mem" -le 128 ];then
		((mem=$mem+3))
	else
		((mem=$mem+4))
	fi
	export SYS_CPU=${cpu}
        export SYS_MEM=${mem}
        export SYS_DISK=${disk}

}

##此函数定义得到全部需要数据
function Get_info()
{
	Get_info_before_time=`date +%s.%N`
	printf_log 1 "Get_info_before_time=`date -d @$Get_info_before_time '+%Y-%m-%d %H:%M:%S'`"

	##获取统计进度
	Get_summary_count "$DBA_ip"

	##从索引中查询本次创建索引数
	Get_index_count "$DBA_ip" "$log_secquence_old"

	##获取trace_logs_detail_part table 数据条数
	Get_trace_count "$DBA_ip" 

	##获取inst_db_count table sql数
	Get_db_count "$DBA_ip"

	##获取共享内存内sql数
	Get_sga_sql "$DBA_ip"


	##根据采集包模式运行相应的取包方法
	if [ $DBA_RUN_PROCESS_MODE -eq 2 ];then 
		##获取nfw状态文件信息
		Get_dpdk_stats "$DBA_ip" "$DBA_eth_R"
	elif [ $DBA_RUN_PROCESS_MODE -eq 0 ];then 
		##获取收包网卡信息	
		Get_eth_R_info "$DBA_ip" "$DBA_eth_R"	
		##获取npc_stats信息
		Get_npc_stats
	elif [ $DBA_RUN_PROCESS_MODE -eq 1 ];then
		:
	fi

	##获取发包网卡信息
	Get_eth_T_info "$tcpreplay_ip" "$tcpreplay_eth_T"

	##tla error日志中看是否存在Begin_to_loose
	Get_Begin_to_loose

	##拿到数据的时间
	Get_info_after_time=`date +%s.%N`
	printf_log 1 "Get_info_after_time=`date -d @$Get_info_after_time '+%Y-%m-%d %H:%M:%S'`"
	Get_info_time=`awk -v a=$Get_info_before_time -v b=$Get_info_after_time 'BEGIN{print b-a}'`
	printf_log 1 "Get_info_time=$Get_info_time"

	for num in `seq 0 $SYS_CPU`
		do
			if [[ $num -eq 0 ]];then
				##获取CPU_ALL %User
				Get_cpu_info "CPU_ALL" "3" "$nmon_num"

				cpu_max_old=$cpu_max
				cpu_avg_old=$cpu_avg
				cpu_min_old=$cpu_min

				##把cpu信息输出到报告文件
				echo -n "[$Get_info_Time_b],CPU_ALL,$Total_Time,$cpu_max,$cpu_avg,$cpu_min" >> $REPORTS_DIR/$PT_report_name

				##获取CPU_ALL %Sys
				Get_cpu_info "CPU_ALL" "4" "$nmon_num"

				echo -n ",$cpu_max,$cpu_avg,$cpu_min" >> $REPORTS_DIR/$PT_report_name

				##获取CPU_ALL %CPU
				cpu_max=`awk -v a=$cpu_max_old -v b=$cpu_max 'BEGIN{print a+b}'`
				cpu_avg=`awk -v a=$cpu_avg_old -v b=$cpu_avg 'BEGIN{print a+b}'`
				cpu_min=`awk -v a=$cpu_min_old -v b=$cpu_min 'BEGIN{print a+b}'`
				printf_log 1 "Get CPU_ALL %CPU MAX=$cpu_max AVG=$cpu_avg MIN=$cpu_min"

				echo ",$cpu_max,$cpu_avg,$cpu_min" >> $REPORTS_DIR/$PT_report_name

			else
				num=`printf "%03d\n" $num`
				##获取单个CPU %User
				Get_cpu_info "CPU${num}" "3" "$nmon_num"

				echo -n "[$Get_info_Time_b],CPU$num,$Total_Time,$cpu_max,$cpu_avg,$cpu_min"\
				 >> $REPORTS_DIR/$PT_report_name

				cpu_max_old=$cpu_max
				cpu_avg_old=$cpu_avg
				cpu_min_old=$cpu_min

				echo -n ",$cpu_max,$cpu_avg,$cpu_min" >> $REPORTS_DIR/$PT_report_name

				##获取单个CPU %Sys
				Get_cpu_info "CPU${num}" "4" "$nmon_num"

				##获取CPU_ALL %CPU
				cpu_max=`awk -v a=$cpu_max_old -v b=$cpu_max 'BEGIN{print a+b}'`
				cpu_avg=`awk -v a=$cpu_avg_old -v b=$cpu_avg 'BEGIN{print a+b}'`
				cpu_min=`awk -v a=$cpu_min_old -v b=$cpu_min 'BEGIN{print a+b}'`
				printf_log 1 "Get CPU${num} %CPU MAX=$cpu_max AVG=$cpu_avg MIN=$cpu_min"

				echo ",$cpu_max,$cpu_avg,$cpu_min" >> $REPORTS_DIR/$PT_report_name
			fi
		done

	##从nmon执行结果里取MEM信息
	Get_mem_info "$nmon_num"

	echo "[$Get_info_Time_b],MEMORY_MB,$Total_Time,$mem_total,$mem_free,$mem_used,$mem_used_per"\
	>> $REPORTS_DIR/$PT_report_name

	#ps -ef|grep nmon |grep -v grep |awk '{print $2}'|xargs -i kill -9 {}
	Get_info_after_time=`date +%s.%N`
	printf_log 1 "Get_info_after_time=`date -d @$Get_info_after_time '+%Y-%m-%d %H:%M:%S'`"
	Get_info_time=`awk -v a=$Get_info_before_time -v b=$Get_info_after_time 'BEGIN{print b-a}'`
	printf_log 1 "Get_info_time=$Get_info_time"
}

##main开始
##定义DBA程序目录
DBFW_HOME="/home/dbfw/dbfw"

##得到DBA_PT.sh脚本所在目录
DBA_PT_HOME_DIR="$( cd "$( dirname "$0"  )" && pwd  )"

##创建DBA_PT.sh脚本生成.log的存放目录
LOGS_DIR="$DBA_PT_HOME_DIR/logs_dir"
mkdir -p $LOGS_DIR

##创建DBA_PT.sh脚本生成.report文件存放目录
REPORTS_DIR="$DBA_PT_HOME_DIR/reports_dir"
mkdir -p $REPORTS_DIR

##创建nmon生成的.nmon文件存放目录
NMONS_DIR="$DBA_PT_HOME_DIR/data_nmon"
mkdir -p $NMONS_DIR

##创建chart_report生成文件存放目录
CHART_REPORTS_DIR="$DBA_PT_HOME_DIR/chart_reports_dir"
mkdir -p $CHART_REPORTS_DIR

##创建tcpreplay_log存放目录
TCPREPLAY_LOG_DIR="$DBA_PT_HOME_DIR/tcpreplay_log_dir"
mkdir -p $TCPREPLAY_LOG_DIR

##定义连接trace_logs_detail_part表所在本地数据中心命令 
DBCDataView_dc="$DBFW_HOME/DBCDataCenter/bin/DBCDataView -h127.0.0.1 -P9207 -uroot -p1 --default-character-set=utf8"
DBCDataView_tc="$DBFW_HOME/DBCDataCenter/bin/DBCDataView -h127.0.0.1 -P9208 -uroot -p1 --default-character-set=utf8"

##得到产品版本号
soft_version=`ssh root@$DBA_ip "$DBCDataView_dc dbfwsystem -N -e 'SELECT soft_major_version,soft_minor_version,soft_svn_version,soft_build_version FROM version_main ORDER BY id desc;'"`
soft_version=`echo $soft_version |sed 's/ /./g'`
soft_version="`cat /etc/producttype`$soft_version"

##得到系统cpu信息
Get_Sys_Info

##定义log名称
File_Name_Time=`date +'%Y%m%d%H%M%S'`
PT_log_name="${soft_version}_CPU${SYS_CPU}_MEM${SYS_MEM}_PT_${File_Name_Time}_${tcpreplay_given}${tcpreplay_rate}_l${tcpreplay_loop}_$$.log"

##定义report名称
PT_report_name="${soft_version}_CPU${SYS_CPU}_MEM${SYS_MEM}_PT_${File_Name_Time}_${tcpreplay_given}${tcpreplay_rate}_l${tcpreplay_loop}_$$.report"


##得到产品的包采集模式
DBA_RUN_PROCESS_MODE=`ssh root@$DBA_ip "$DBCDataView_dc dbfw -N -e 'SELECT param_value FROM param_config where param_id=121;'"`
printf_log 1 "DBA_RUN_PROCESS_MODE:$DBA_RUN_PROCESS_MODE"
if [[ $? != 0 ]];then
        printf_log 1 "ssh root@$DBA_ip fail!"
elif [ -n $DBA_RUN_PROCESS_MODE ];then
        printf_log 1 "Get DBA_RUN_PROCESS_MODE Success!"
else
        printf_log 1 "Get DBA_RUN_PROCESS_MODE fail!"
	exit
fi

##系统CPU、MEM信息写入日志
printf_log 1 "Get DBA OS_CPU_Core=$SYS_CPU;OS_MEM=${SYS_MEM}G;OS_SYS_DISK=${SYS_DISK}G"

# ##把DBA、打包机设置免密登录
# mkdir -p /root/.ssh
# chmod 0600 $DBA_PT_HOME_DIR/sshkey/{id_rsa,id_rsa.pub}
# \cp -f $DBA_PT_HOME_DIR/sshkey/{id_rsa,id_rsa.pub} /root/.ssh/
# mv /root/.ssh/{id_rsa.pub,authorized_keys}
# ssh_keygen_login "${DBA_ip}" "y"
# printf_log 1 "$DBA_ip ssh_keygen_login success!"
# ssh_keygen_login "${tcpreplay_ip}" "y"
# printf_log 1 "$tcpreplay_ip ssh_keygen_login success!"


##判断是否为双数据中心
###trace_logs_detail_part表所处数据中心端口
port=`ssh root@$DBA_ip cat ${DBFW_HOME}/etc/dbfw50.ini|grep "__DATASERVER_PORT_FOR_TLS"|awk -F= '{print $2}'`
if [[ $port -eq 9207 ]];then
        DBC_count=1
        printf_log 1 "DBC_count=1"
elif [[ $port -eq 9208 ]];then
        DBC_count=2
        printf_log 1 "DBC_count=2"
fi

##查看打包机及DBA的kernel版本
tcpreplay_kernel_version=`ssh root@$tcpreplay_ip "uname -r"|awk -F. '{print $(NF-1)}'`
printf_log 1 "Get tcpreplay_kernel_version=$tcpreplay_kernel_version"
DBA_kernel_version=`ssh root@$DBA_ip "uname -r"|awk -F. '{print $(NF-1)}'`
printf_log 1 "Get DBA_kernel_version=$DBA_kernel_version"

##打包前启动nmon，Get_info前起码有一次nmon数据
ssh root@$DBA_ip mkdir -p $NMONS_DIR
ssh root@$DBA_ip /usr/bin/nmon -s5 -c5760 -f -m $NMONS_DIR > /dev/null 2>&1
nmon_pid=`ssh root@$DBA_ip ps -ef|grep "/usr/bin/nmon -s"|grep -v "grep"|awk '{print $2}'`
nmon_file_name=`ls -l $NMONS_DIR |tail -n1|awk '{print $NF}'`
nmon_file_name="$NMONS_DIR/$nmon_file_name"
printf_log 1 "nmon Begin run! nmon_pid=$nmon_pid,output file:$nmon_file_name"

##定义取nmon最新数据条数
((nmon_num=Get_info_cycle/5))

##停一秒,为了使nmon第一次运行完成
sleep 1 

##得到第一次取(基础)数据时的时间,取完数据后会马上启动打包
while true
	do
		Get_info_Time_S=`date +%S`
		Get_info_Time_S=`echo $((10#$Get_info_Time_S))`
		if [ $Get_info_Time_S -le 15 ];then
			sleep $((15-Get_info_Time_S))
			break
		elif [ $Get_info_Time_S -le 35 ];then
			sleep $((35-Get_info_Time_S))
			break
		elif [ $Get_info_Time_S -le 55 ];then
			sleep $((55-Get_info_Time_S))
			break
		else	
			sleep $((75-Get_info_Time_S))
			break
		fi
	done

Get_info_Time=`date +%s`
Get_info_Time_b=`date -d @$Get_info_Time '+%Y-%m-%d %H:%M:%S'`

printf_log 1 "tcpreplay run before time=`date -d @$Get_info_Time '+%Y-%m-%d %H:%M:%S'`"

##预期包数和sql数
((expect_pcap_tot=expect_pcap*tcpreplay_loop))
((expect_sql_tot=expect_sql*tcpreplay_loop))

##输出报告内容的头信息
for num in `seq 0 $SYS_CPU`
	do
		if [[ $num -eq 0 ]];then
			echo -e "\
DBA_PT_CONFIG,DBA_SYS_CONFIG,SYS_CPU=$SYS_CPU SYS_MEM=${SYS_MEM}G SYS_DISK=${SYS_DISK}G\n\
DBA_PT_CONFIG,DBA_VSERION,$soft_version\n\
DBA_PT_CONFIG,DBA_ip,$DBA_ip\n\
DBA_PT_CONFIG,DBA_eth_R,$DBA_eth_R\n\
DBA_PT_CONFIG,tcpreplay_ip,$tcpreplay_ip\n\
DBA_PT_CONFIG,tcpreplay_eth_T,$tcpreplay_eth_T\n\
DBA_PT_CONFIG,Get_info_cycle,$Get_info_cycle\n\
DBA_PT_CONFIG,PT_RUN_DBA,$PT_RUN_DBA\n\
DBA_PT_CONFIG,DBA_RUN_PROCESS_MODE,$DBA_RUN_PROCESS_MODE\n\
DBA_PT_CONFIG,eth_drive_map,\n\
DBA_PT_CONFIG,expect_pcap,$expect_pcap_tot\n\
DBA_PT_CONFIG,expect_sql,$expect_sql_tot\n\
DBA_PT_CONFIG,tcpreplay_cmd,$tcpreplay_cmd\n\
DBA_PT_CONFIG,trace_count_base,\n\
DBA_PT_CONFIG,tcpreplay_packing_time,\n\
DBA_PT_CONFIG,npp_analysis_time,\n\
DBA_PT_CONFIG,tla_analysis_time,\n\
DBA_PT_CONFIG,ftm_analysis_time,\n\
DBA_PT_CONFIG,tls_summary_time,"\
				> $REPORTS_DIR/$PT_report_name
			echo "Get_info_time,CPU_ALL,Total_Time,user%_max,user%_avg,user%_min,sys%_max,sys%_avg,sys%_min,cpu%_max,cpu%_avg,cpu%_min"\
				>> $REPORTS_DIR/$PT_report_name
		else
			num=`printf "%03d\n" $num`
			echo "Get_info_time,CPU$num,Total_Time,user%_max,user%_avg,user%_min,sys%_max,sys%_avg,sys%_min,cpu%_max,cpu%_avg,cpu%_min"\
				>> $REPORTS_DIR/$PT_report_name
		fi
	done

echo "Get_info_time,MEMORY_MB,Total_Time,mem_total,mem_free,mem_used,mem_used_per"\
	>> $REPORTS_DIR/$PT_report_name
	
if [[ $DBA_RUN_PROCESS_MODE -eq 2 ]];then
	echo "Get_info_time,PCAP_TOT_COUNT,Total_Time,tcpreplay_Tx_pck,tcpreplay_T_d_pck,dpdk_rx_pkt_tot,dpdk_rx_pkt_err,dpdk_drop_pkt_hw,dpdk_drop_pkt_tot"\
	>> $REPORTS_DIR/$PT_report_name
elif [[ $DBA_RUN_PROCESS_MODE -eq 0 ]];then
	echo "Get_info_time,PCAP_TOT_COUNT,Total_Time,tcpreplay_Tx_pck,tcpreplay_T_d_pck,DBA_Rx_pck,DBA_R_d_pck,npc_rx_all_pkt_tot"\
	>> $REPORTS_DIR/$PT_report_name
elif [[ $DBA_RUN_PROCESS_MODE -eq 1 ]];then
	:
fi

echo "Get_info_time,SQL_TOT_COUNT,Total_Time,expect_sql,sga_sql,inst_db_count_sql"\
	>> $REPORTS_DIR/$PT_report_name
echo "Get_info_time,TLOG_TOT_COUNT,Total_Time,expect_count,sga_count,trace_count,index_count,summary_count"\
	>> $REPORTS_DIR/$PT_report_name
echo "Get_info_time,TO_LOOSE_TOT_COUNT,Total_Time,Begin_to_loose_count"\
	>> $REPORTS_DIR/$PT_report_name

echo "Get_info_time,TLOG_LAG_COUNT,Total_Time,pcap2sga_lag_time,sga2trace_lag_time,sga2index_lag_time,sga2summary_lag_time,trace2summary_lag_time"\
	>> $REPORTS_DIR/$PT_report_name

if [[ $DBA_RUN_PROCESS_MODE -eq 2 ]];then
	echo "Get_info_time,PCAP_TOT_PS_COUNT,Total_Time,tcpreplay_Tx_pck/s,tcpreplay_T_d_pck/s,dpdk_rx_pkt_tot/s,dpdk_rx_pkt_err/s,dpdk_drop_pkt_hw/s,dpdk_drop_pkt_tot/s"\
	>> $REPORTS_DIR/$PT_report_name
elif [[ $DBA_RUN_PROCESS_MODE -eq 0 ]];then
	echo "Get_info_time,PCAP_TOT_PS_COUNT,Total_Time,tcpreplay_Tx_pck/s,tcpreplay_T_d_pck/s,DBA_Rx_pck/s,DBA_R_d_pck/s,npc_rx_all_pkt_tot/s"\
	>> $REPORTS_DIR/$PT_report_name
elif [[ $DBA_RUN_PROCESS_MODE -eq 1 ]];then
	:
fi	
	
	

echo "Get_info_time,SQL_TOT_PS_COUNT,Total_Time,expect_sql/s,sga_sql/s,inst_db_count_sql/s"\
	>> $REPORTS_DIR/$PT_report_name
echo "Get_info_time,TLOG_TOT_PS_COUNT,Total_Time,expect_count/s,sga_count/s,trace_count/s,index_count/s,summary_count/s"\
	>> $REPORTS_DIR/$PT_report_name

if [[ $DBA_RUN_PROCESS_MODE -eq 2 ]];then
	echo "Get_info_time,PCAP_CYC_COUNT,Total_Time,tcpreplay_Tx_pck,tcpreplay_T_d_pck,dpdk_rx_pkt_tot,dpdk_rx_pkt_err,dpdk_drop_pkt_hw,dpdk_drop_pkt_tot"\
	>> $REPORTS_DIR/$PT_report_name
elif [[ $DBA_RUN_PROCESS_MODE -eq 0 ]];then
	echo "Get_info_time,PCAP_CYC_COUNT,Total_Time,tcpreplay_Tx_pck,tcpreplay_T_d_pck,DBA_Rx_pck,DBA_R_d_pck,npc_rx_all_pkt_tot"\
	>> $REPORTS_DIR/$PT_report_name
elif [[ $DBA_RUN_PROCESS_MODE -eq 1 ]];then
	:
fi
	
echo "Get_info_time,SQL_CYC_COUNT,Total_Time,expect_sql,sga_sql,db_count_sql"\
	>> $REPORTS_DIR/$PT_report_name
echo "Get_info_time,TLOG_CYC_COUNT,Total_Time,expect_count,sga_count,trace_count,index_count,summary_count"\
	>> $REPORTS_DIR/$PT_report_name
echo "Get_info_time,TO_LOOSE_CYC_COUNT,Total_Time,Begin_to_loose_count"\
	>> $REPORTS_DIR/$PT_report_name

if [[ $DBA_RUN_PROCESS_MODE -eq 2 ]];then
	echo "Get_info_time,PCAP_CYC_PS_COUNT,Total_Time,tcpreplay_Tx_pck/s,tcpreplay_T_d_pck/s,dpdk_rx_pkt_tot/s,dpdk_rx_pkt_err/s,dpdk_drop_pkt_hw/s,dpdk_drop_pkt_tot/s"\
	>> $REPORTS_DIR/$PT_report_name
elif [[ $DBA_RUN_PROCESS_MODE -eq 0 ]];then
	echo "Get_info_time,PCAP_CYC_PS_COUNT,Total_Time,tcpreplay_Tx_pck/s,tcpreplay_T_d_pck/s,DBA_Rx_pck/s,DBA_R_d_pck/s,npc_rx_all_pkt_tot/s"\
	>> $REPORTS_DIR/$PT_report_name
elif [[ $DBA_RUN_PROCESS_MODE -eq 1 ]];then
	:
fi

echo "Get_info_time,SQL_CYC_PS_COUNT,Total_Time,expect_sql/s,sga_sql/s,db_count_sql/s"\
	>> $REPORTS_DIR/$PT_report_name
echo "Get_info_time,TLOG_CYC_PS_COUNT,Total_Time,expect_count/s,sga_count/s,trace_count/s,index_count/s,summary_count/s"\
	>> $REPORTS_DIR/$PT_report_name

##初始化取数据次数,及数据总时间
Get_Date_num=0
((Total_Time=Get_info_cycle*Get_Date_num))

##tcpreplay运行前获取一次数据
Get_info

##因为打包还没有开始,所以输出为空值
echo "[$Get_info_Time_b],PCAP_TOT_COUNT,$Total_Time,0,0,0,0,0,0" >> $REPORTS_DIR/$PT_report_name
echo "[$Get_info_Time_b],SQL_TOT_COUNT,$Total_Time,0,0,0" >> $REPORTS_DIR/$PT_report_name
echo "[$Get_info_Time_b],TLOG_TOT_COUNT,$Total_Time,0,0,0,0,0" >> $REPORTS_DIR/$PT_report_name
echo "[$Get_info_Time_b],TO_LOOSE_TOT_COUNT,$Total_Time,0" >> $REPORTS_DIR/$PT_report_name

echo "[$Get_info_Time_b],TLOG_LAG_COUNT,$Total_Time,0,0,0,0,0" >> $REPORTS_DIR/$PT_report_name

echo "[$Get_info_Time_b],PCAP_TOT_PS_COUNT,$Total_Time,0,0,0,0,0,0" >> $REPORTS_DIR/$PT_report_name
echo "[$Get_info_Time_b],SQL_TOT_PS_COUNT,$Total_Time,0,0,0" >> $REPORTS_DIR/$PT_report_name
echo "[$Get_info_Time_b],TLOG_TOT_PS_COUNT,$Total_Time,0,0,0,0,0" >> $REPORTS_DIR/$PT_report_name

echo "[$Get_info_Time_b],PCAP_CYC_COUNT,$Total_Time,0,0,0,0,0,0" >> $REPORTS_DIR/$PT_report_name
echo "[$Get_info_Time_b],SQL_CYC_COUNT,$Total_Time,0,0,0" >> $REPORTS_DIR/$PT_report_name
echo "[$Get_info_Time_b],TLOG_CYC_COUNT,$Total_Time,0,0,0,0" >> $REPORTS_DIR/$PT_report_name
echo "[$Get_info_Time_b],TO_LOOSE_CYC_COUNT,$Total_Time,0" >> $REPORTS_DIR/$PT_report_name

echo "[$Get_info_Time_b],PCAP_CYC_PS_COUNT,$Total_Time,0,0,0,0,0,0" >> $REPORTS_DIR/$PT_report_name
echo "[$Get_info_Time_b],SQL_CYC_PS_COUNT,$Total_Time,0,0,0" >> $REPORTS_DIR/$PT_report_name
echo "[$Get_info_Time_b],TLOG_CYC_PS_COUNT,$Total_Time,0,0,0,0" >> $REPORTS_DIR/$PT_report_name

tcpreplay_Tx_pck_old="$tcpreplay_Tx_pck"
tcpreplay_T_d_pck_old="$tcpreplay_T_d_pck"

if [[ $DBA_RUN_PROCESS_MODE -eq 2 ]];then
	dpdk_rx_pkt_tot_old="$dpdk_rx_pkt_tot"
	dpdk_rx_pkt_err_old="$dpdk_rx_pkt_err"
	dpdk_drop_pkt_hw_old="$dpdk_drop_pkt_hw"
	dpdk_drop_pkt_tot_old="$dpdk_drop_pkt_tot"
elif [[ $DBA_RUN_PROCESS_MODE -eq 0 ]];then
	DBA_Rx_pck_old="$DBA_Rx_pck"
	DBA_R_d_pck_old="$DBA_R_d_pck"
	npc_rx_all_pkt_tot_old="$npc_rx_all_pkt_tot"
elif [[ $DBA_RUN_PROCESS_MODE -eq 1 ]];then
	:
fi

sga_sql_old="$sga_sql"
sga_count_old="$sga_sql"
db_count_sql_old="$db_count_sql"
trace_count_old="$trace_count"
index_count_old="$index_count"
summary_count_old="$summary_count"
Begin_to_loose_count="$Begin_to_loose_count_old"

tcpreplay_Tx_pck_base="$tcpreplay_Tx_pck"
tcpreplay_T_d_pck_base="$tcpreplay_T_d_pck"

if [[ $DBA_RUN_PROCESS_MODE -eq 2 ]];then
	dpdk_rx_pkt_tot_base="$dpdk_rx_pkt_tot"
	dpdk_rx_pkt_err_base="$dpdk_rx_pkt_err"
	dpdk_drop_pkt_hw_base="$dpdk_drop_pkt_hw"
	dpdk_drop_pkt_tot_base="$dpdk_drop_pkt_tot"
elif [[ $DBA_RUN_PROCESS_MODE -eq 0 ]];then
	DBA_Rx_pck_base="$DBA_Rx_pck"
	DBA_R_d_pck_base="$DBA_R_d_pck"
	npc_rx_all_pkt_tot_base="$npc_rx_all_pkt_tot"
elif [[ $DBA_RUN_PROCESS_MODE -eq 1 ]];then
	:
fi

sga_sql_base="$sga_sql"
sga_count_base="$sga_sql"
db_count_sql_base="$db_count_sql"
trace_count_base="$trace_count"
index_count_base="$index_count"
summary_count_base="$summary_count"
Begin_to_loose_count_base="$Begin_to_loose_count_old"

##输出配置参数到日志文件
printf_log 1 "soft_version=$soft_version"
printf_log 1 "DBA_ip=$DBA_ip"
printf_log 1 "DBA_eth_R=$DBA_eth_R"
printf_log 1 "tcpreplay_ip=$tcpreplay_ip"
printf_log 1 "tcpreplay_eth_T=$tcpreplay_eth_T"
printf_log 1 "expect_pcap=$expect_pcap"
printf_log 1 "expect_sql=$expect_sql"
printf_log 1 "Get_info_cycle=$Total_Time"
printf_log 1 "PT_RUN_DBA=$PT_RUN_DBA"

##只有在2模式时才进行此操作
if [ $DBA_RUN_PROCESS_MODE -eq 2 ];then                                  
	printf_log 1 "eth_drive_map=$eth_drive_map"
	sed -i "s/eth_drive_map,/eth_drive_map,$eth_drive_map/" $REPORTS_DIR/$PT_report_name
fi

sed -i "s/trace_count_base,/trace_count_base,$trace_count/" $REPORTS_DIR/$PT_report_name

##启动tcpreplay
ssh root@$tcpreplay_ip "nohup $tcpreplay_cmd >> /tmp/tcpreplay_${File_Name_Time}_${tcpreplay_given}${tcpreplay_rate}_l${tcpreplay_loop}.log 2>&1 &"
printf_log 1 "tcpreplay Begin run! tcpreplay_cmd: $tcpreplay_cmd"

##输出取数据周期和下次取数据的时间
((Get_info_Time=Get_info_Time+Get_info_cycle))
printf_log 1 "Get_info cycle=$Total_Time next time=`date -d @$Get_info_Time '+%Y-%m-%d %H:%M:%S'`"

tcpreplay_cmd_tmp=`echo $tcpreplay_cmd |sed 's/-/\\\-/g'`

PT_complete_flag=0
while true
	do
	Get_info_Time_tmp=`date +%s`
	##到了Get数据的时间	
	if [[ $Get_info_Time_tmp -eq $Get_info_Time ]];then

		Get_info_Time_b=`date -d @$Get_info_Time '+%Y-%m-%d %H:%M:%S'`
		((Total_Time=Get_info_cycle*Get_Date_num))
		Get_info

		##开始算数的时间	
		Count_data_before_time=`date +%s.%N`
		printf_log 1 "Count_data_before_time=`date -d @$Count_data_before_time '+%Y-%m-%d %H:%M:%S'`"

		((cyc_tcpreplay_Tx_pck=tcpreplay_Tx_pck-tcpreplay_Tx_pck_old))	
		((cyc_expect_sql=expect_sql_tot*cyc_tcpreplay_Tx_pck/expect_pcap_tot))
		((cyc_sga_sql=sga_sql-sga_sql_old))
		((cyc_db_count_sql=db_count_sql-db_count_sql_old))
		((cyc_sga_count=sga_count-sga_count_old))
		((cyc_trace_count=trace_count-trace_count_old))
		((cyc_index_count=index_count-index_count_old))
		((cyc_summary_count=summary_count-summary_count_old))

		if [[ $cyc_expect_sql -ne 0 ]] || [[ $Get_Date_num -eq 1 ]];then
			expect2fact_Total_Time=$Total_Time
			printf_log 1 "expect2fact_Total_Time=$expect2fact_Total_Time"	
			sed1_flag=0
		else
			if [[ $sed1_flag -eq 0 ]];then
				((tcpreplay_packing_time=Total_Time-Get_info_cycle))
				sed -i "s/tcpreplay_packing_time,.*$/tcpreplay_packing_time,$tcpreplay_packing_time/" $REPORTS_DIR/$PT_report_name 
				((sed1_flag=sed1_flag+1))
			fi
			printf_log 1 "now cyc_expect_sql=0"	
		fi

		if [[ $cyc_sga_count -ne 0 ]] || [[ $Get_Date_num -eq 1 ]];then
			expect2sga_Total_Time=$Total_Time
			printf_log 1 "expect2sga_Total_Time=$expect2sga_Total_Time"	
			sed2_flag=0
		else
			if [[ $sed2_flag -eq 0 ]];then
				((npp_analysis_time=Total_Time-Get_info_cycle))
				sed -i "s/npp_analysis_time,.*$/npp_analysis_time,$npp_analysis_time/" $REPORTS_DIR/$PT_report_name 
				((sed2_flag=sed2_flag+1))
			fi
			printf_log 1 "now cyc_sga_count=0"	
		fi

		if [[ $cyc_trace_count -ne 0 ]] || [[ $Get_Date_num -eq 1 ]];then 
			sga2trace_Total_Time=$Total_Time
			printf_log 1 "sga2trace_Total_Time=$sga2trace_Total_Time"	
			sed3_flag=0
		else	
			if [[ $sed3_flag -eq 0 ]];then
				((tla_analysis_time=Total_Time-Get_info_cycle))
				sed -i "s/tla_analysis_time,.*$/tla_analysis_time,$tla_analysis_time/" $REPORTS_DIR/$PT_report_name 
				((sed3_flag=sed3_flag+1))
			fi
			printf_log 1 "now cyc_trace_count=0"	
		fi

		if [[ $cyc_index_count -ne 0 ]] || [[ $Get_Date_num -eq 1 ]];then 
			sga2index_Total_Time=$Total_Time
			printf_log 1 "sga2index_Total_Time=$sga2index_Total_Time"	
			sed4_flag=0
		else
			if [[ $sed4_flag -eq 0 ]];then
				((ftm_analysis_time=Total_Time-Get_info_cycle))
				sed -i "s/ftm_analysis_time,.*$/ftm_analysis_time,$ftm_analysis_time/" $REPORTS_DIR/$PT_report_name 
				((sed4_flag=sed4_flag+1))
			fi
			printf_log 1 "now cyc_index_count=0"	
		fi

		if [[ $cyc_summary_count -ne 0 ]] || [[ $Get_Date_num -eq 1 ]];then 
			trace2summary_Total_Time=$Total_Time
			printf_log 1 "trace2summary_Total_Time=$trace2summary_Total_Time"	
			sed5_flag=0
		else
			if [[ $sed5_flag -eq 0 ]];then
				((tls_summary_time=Total_Time-Get_info_cycle))
				sed -i "s/tls_summary_time,.*$/tls_summary_time,$tls_summary_time/" $REPORTS_DIR/$PT_report_name 
				((sed5_flag=sed5_flag+1))
			fi
			printf_log 1 "now cyc_summary_count=0"	
		fi

		((tot_tcpreplay_Tx_pck=tcpreplay_Tx_pck-tcpreplay_Tx_pck_base))
		((tot_tcpreplay_T_d_pck=tcpreplay_T_d_pck-tcpreplay_T_d_pck_base))
		
		if [[ $DBA_RUN_PROCESS_MODE -eq 2 ]];then
			((tot_dpdk_rx_pkt_tot=dpdk_rx_pkt_tot-dpdk_rx_pkt_tot_base))
			((tot_dpdk_rx_pkt_err=dpdk_rx_pkt_err-dpdk_rx_pkt_err_base))
			((tot_dpdk_drop_pkt_hw=dpdk_drop_pkt_hw-dpdk_drop_pkt_hw_base))
			((tot_dpdk_drop_pkt_tot=dpdk_drop_pkt_tot-dpdk_drop_pkt_tot_base))
			echo "[$Get_info_Time_b],PCAP_TOT_COUNT,$Total_Time,\
$tot_tcpreplay_Tx_pck,\
$tot_tcpreplay_T_d_pck,\
$tot_dpdk_rx_pkt_tot,\
$tot_dpdk_rx_pkt_err,\
$tot_dpdk_drop_pkt_hw,\
$tot_dpdk_drop_pkt_tot"\
>> $REPORTS_DIR/$PT_report_name
		elif [[ $DBA_RUN_PROCESS_MODE -eq 0 ]];then
			((tot_DBA_Rx_pck=DBA_Rx_pck-DBA_Rx_pck_base))
			((tot_DBA_R_d_pck=DBA_R_d_pck-DBA_R_d_pck_base))
			((tot_npc_rx_all_pkt_tot=npc_rx_all_pkt_tot-npc_rx_all_pkt_tot_base))
			echo "[$Get_info_Time_b],PCAP_TOT_COUNT,$Total_Time,\
$tot_tcpreplay_Tx_pck,\
$tot_tcpreplay_T_d_pck,\
$tot_DBA_Rx_pck,\
$tot_DBA_R_d_pck,\
$tot_npc_rx_all_pkt_tot"\
>> $REPORTS_DIR/$PT_report_name
		elif [[ $DBA_RUN_PROCESS_MODE -eq 1 ]];then
			:
		fi

		((tot_expect_sql=expect_sql_tot*tot_tcpreplay_Tx_pck/expect_pcap_tot))
		((tot_sga_sql=sga_sql-sga_sql_base))		
		((tot_db_count_sql=db_count_sql-db_count_sql_base))		
		echo "[$Get_info_Time_b],SQL_TOT_COUNT,$Total_Time,\
$tot_expect_sql,\
$tot_sga_sql,\
$tot_db_count_sql"\
>> $REPORTS_DIR/$PT_report_name

		((tot_sga_count=sga_count-sga_count_base))
		((tot_trace_count=trace_count-trace_count_base))
		((tot_index_count=index_count-index_count_base))
		((tot_summary_count=summary_count-summary_count_base))
		echo "[$Get_info_Time_b],TLOG_TOT_COUNT,$Total_Time,\
$tot_expect_sql,\
$tot_sga_count,\
$tot_trace_count,\
$tot_index_count,\
$tot_summary_count"\
>> $REPORTS_DIR/$PT_report_name

		((tot_Begin_to_loose_count=Begin_to_loose_count-Begin_to_loose_count_base))
		echo "[$Get_info_Time_b],TO_LOOSE_TOT_COUNT,$Total_Time,\
$tot_Begin_to_loose_count"\
>> $REPORTS_DIR/$PT_report_name

		((ps_tot_sga_count=tot_sga_count/expect2sga_Total_Time))
		
		##这一块还就有bug有时间了再查（偶尔可能会出现分母为0的情况,但是不知道是哪一个）
		if [[ $tot_sga_count -eq 0 ]] || [[ $tot_trace_count -eq 0 ]] || [[ $tot_summary_count -eq 0 ]];then
			printf 1 "tcpreplay param error or DBA error!"	
			exit

		#elif [[ $tot_index_count -eq 0 ]] && [[ $Get_Date_num -ge 3 ]];then
		#	printf 1 "tcpreplay param error or DBA error!"	

                elif [[ $tot_index_count -eq 0 ]];then
                        echo "[$Get_info_Time_b],TLOG_LAG_COUNT,$Total_Time,\
$(((tot_expect_sql-tot_sga_count)/(tot_sga_count/expect2sga_Total_Time))),\
$(((tot_sga_count-tot_trace_count)/(tot_trace_count/sga2trace_Total_Time))),\
0,\
$(((tot_sga_count-tot_summary_count)/(tot_summary_count/trace2summary_Total_Time))),\
$(((tot_trace_count-tot_summary_count)/(tot_summary_count/trace2summary_Total_Time)))"\
>> $REPORTS_DIR/$PT_report_name
                else
                        echo "[$Get_info_Time_b],TLOG_LAG_COUNT,$Total_Time,\
$(((tot_expect_sql-tot_sga_count)/(tot_sga_count/expect2sga_Total_Time))),\
$(((tot_sga_count-tot_trace_count)/(tot_trace_count/sga2trace_Total_Time))),\
$(((tot_sga_count-tot_index_count)/(tot_index_count/sga2index_Total_Time))),\
$(((tot_sga_count-tot_summary_count)/(tot_summary_count/trace2summary_Total_Time))),\
$(((tot_trace_count-tot_summary_count)/(tot_summary_count/trace2summary_Total_Time)))"\
>> $REPORTS_DIR/$PT_report_name
                fi

		if [[ $DBA_RUN_PROCESS_MODE -eq 2 ]];then
			echo "[$Get_info_Time_b],PCAP_TOT_PS_COUNT,$Total_Time,\
$((tot_tcpreplay_Tx_pck/expect2fact_Total_Time)),\
$((tot_tcpreplay_T_d_pck/expect2fact_Total_Time)),\
$((tot_dpdk_rx_pkt_tot/expect2fact_Total_Time)),\
$((tot_dpdk_rx_pkt_err/expect2fact_Total_Time)),\
$((tot_dpdk_drop_pkt_hw/expect2fact_Total_Time)),\
$((tot_dpdk_drop_pkt_tot/expect2fact_Total_Time))"\
>> $REPORTS_DIR/$PT_report_name
		elif [[ $DBA_RUN_PROCESS_MODE -eq 0 ]];then
			echo "[$Get_info_Time_b],PCAP_TOT_PS_COUNT,$Total_Time,\
$((tot_tcpreplay_Tx_pck/expect2fact_Total_Time)),\
$((tot_tcpreplay_T_d_pck/expect2fact_Total_Time)),\
$((tot_DBA_Rx_pck/expect2fact_Total_Time)),\
$((tot_DBA_R_d_pck/expect2fact_Total_Time)),\
$((tot_npc_rx_all_pkt_tot/expect2fact_Total_Time))"\
>> $REPORTS_DIR/$PT_report_name
		elif [[ $DBA_RUN_PROCESS_MODE -eq 1 ]];then
			:
		fi				

		echo "[$Get_info_Time_b],SQL_TOT_PS_COUNT,$Total_Time,\
$((tot_expect_sql/expect2fact_Total_Time)),\
$((tot_sga_sql/expect2sga_Total_Time)),\
$((tot_db_count_sql/sga2trace_Total_Time))"\
>> $REPORTS_DIR/$PT_report_name

		echo "[$Get_info_Time_b],TLOG_TOT_PS_COUNT,$Total_Time,\
$((tot_expect_sql/expect2fact_Total_Time)),\
$((tot_sga_count/expect2sga_Total_Time)),\
$((tot_trace_count/sga2trace_Total_Time)),\
$((tot_index_count/sga2index_Total_Time)),\
$((tot_summary_count/trace2summary_Total_Time))"\
>> $REPORTS_DIR/$PT_report_name

		if [[ $DBA_RUN_PROCESS_MODE -eq 2 ]];then
			((cyc_tcpreplay_Tx_pck=tcpreplay_Tx_pck-tcpreplay_Tx_pck_old))	
			((cyc_tcpreplay_T_d_pck=tcpreplay_T_d_pck-tcpreplay_T_d_pck_old))
			((cyc_dpdk_rx_pkt_tot=dpdk_rx_pkt_tot-dpdk_rx_pkt_tot_old))
			((cyc_dpdk_rx_pkt_err=dpdk_rx_pkt_err-dpdk_rx_pkt_err_old))
			((cyc_dpdk_drop_pkt_hw=dpdk_drop_pkt_hw-dpdk_drop_pkt_hw_old))
			((cyc_dpdk_drop_pkt_tot=dpdk_drop_pkt_tot-dpdk_drop_pkt_tot_old))
			echo "[$Get_info_Time_b],PCAP_CYC_COUNT,$Total_Time,\
$cyc_tcpreplay_Tx_pck,\
$cyc_tcpreplay_T_d_pck,\
$cyc_dpdk_rx_pkt_tot,\
$cyc_dpdk_rx_pkt_err,\
$cyc_dpdk_drop_pkt_hw,\
$cyc_dpdk_drop_pkt_tot"\
>> $REPORTS_DIR/$PT_report_name
		elif [[ $DBA_RUN_PROCESS_MODE -eq 0 ]];then
			((cyc_tcpreplay_Tx_pck=tcpreplay_Tx_pck-tcpreplay_Tx_pck_old))	
			((cyc_tcpreplay_T_d_pck=tcpreplay_T_d_pck-tcpreplay_T_d_pck_old))
			((cyc_DBA_Rx_pck=DBA_Rx_pck-DBA_Rx_pck_old))
			((cyc_DBA_R_d_pck=DBA_R_d_pck-DBA_R_d_pck_old))
			((cyc_npc_rx_all_pkt_tot=npc_rx_all_pkt_tot-npc_rx_all_pkt_tot_old))
			echo "[$Get_info_Time_b],PCAP_CYC_COUNT,$Total_Time,\
$cyc_tcpreplay_Tx_pck,\
$cyc_tcpreplay_T_d_pck,\
$cyc_DBA_Rx_pck,\
$cyc_DBA_R_d_pck,\
$cyc_npc_rx_all_pkt_tot"\
>> $REPORTS_DIR/$PT_report_name
		elif [[ $DBA_RUN_PROCESS_MODE -eq 1 ]];then
			:
		fi

		echo "[$Get_info_Time_b],SQL_CYC_COUNT,$Total_Time,\
$cyc_expect_sql,\
$cyc_sga_sql,\
$cyc_db_count_sql"\
>> $REPORTS_DIR/$PT_report_name

		echo "[$Get_info_Time_b],TLOG_CYC_COUNT,$Total_Time,\
$cyc_expect_sql,\
$cyc_sga_count,\
$cyc_trace_count,\
$cyc_index_count,\
$cyc_summary_count"\
>> $REPORTS_DIR/$PT_report_name

		((cyc_Begin_to_loose_count=Begin_to_loose_count-Begin_to_loose_count_old))
		echo "[$Get_info_Time_b],TO_LOOSE_CYC_COUNT,$Total_Time,\
$cyc_Begin_to_loose_count"\
>> $REPORTS_DIR/$PT_report_name

		if [[ $DBA_RUN_PROCESS_MODE -eq 2 ]];then
			echo "[$Get_info_Time_b],PCAP_CYC_PS_COUNT,$Total_Time,\
$((cyc_tcpreplay_Tx_pck/Get_info_cycle)),\
$((cyc_tcpreplay_T_d_pck/Get_info_cycle)),\
$((cyc_dpdk_rx_pkt_tot/Get_info_cycle)),\
$((cyc_dpdk_rx_pkt_err/Get_info_cycle)),\
$((cyc_dpdk_drop_pkt_hw/Get_info_cycle)),\
$((cyc_dpdk_drop_pkt_tot/Get_info_cycle))"\
>> $REPORTS_DIR/$PT_report_name
		elif [[ $DBA_RUN_PROCESS_MODE -eq 0 ]];then
			echo "[$Get_info_Time_b],PCAP_CYC_PS_COUNT,$Total_Time,\
$((cyc_tcpreplay_Tx_pck/Get_info_cycle)),\
$((cyc_tcpreplay_T_d_pck/Get_info_cycle)),\
$((cyc_DBA_Rx_pck/Get_info_cycle)),\
$((cyc_DBA_R_d_pck/Get_info_cycle)),\
$((cyc_npc_rx_all_pkt_tot/Get_info_cycle))"\
>> $REPORTS_DIR/$PT_report_name
		elif [[ $DBA_RUN_PROCESS_MODE -eq 1 ]];then
			:
		fi



		echo "[$Get_info_Time_b],SQL_CYC_PS_COUNT,$Total_Time,\
$((cyc_expect_sql/Get_info_cycle)),\
$((cyc_sga_sql/Get_info_cycle)),\
$((cyc_db_count_sql/Get_info_cycle))"\
>> $REPORTS_DIR/$PT_report_name

		echo "[$Get_info_Time_b],TLOG_CYC_PS_COUNT,$Total_Time,\
$((cyc_expect_sql/Get_info_cycle)),\
$((cyc_sga_count/Get_info_cycle)),\
$((cyc_trace_count/Get_info_cycle)),\
$((cyc_index_count/Get_info_cycle)),\
$((cyc_summary_count/Get_info_cycle))"\
>> $REPORTS_DIR/$PT_report_name

		tlog_table_complete_flag="$cyc_trace_count"
		xml_table_complete_flag="$cyc_index_count"
		summary_complete_flag="$cyc_summary_count"
		##算数的时间
		Count_data_after_time=`date +%s.%N`
		printf_log 1 "Count_data_after_time=`date -d @$Count_data_after_time '+%Y-%m-%d %H:%M:%S'`"
		Count_data_time=`awk -v a=$Count_data_after_time -v b=$Count_data_after_time 'BEGIN{print b-a}'`
		printf_log 1 "Count_data_time=$Count_data_time"

		if [[ $DBA_RUN_PROCESS_MODE -eq 2 ]];then
			tcpreplay_Tx_pck_old="$tcpreplay_Tx_pck"
			tcpreplay_T_d_pck_old="$tcpreplay_T_d_pck"
			dpdk_rx_pkt_tot_old="$dpdk_rx_pkt_tot"
			dpdk_rx_pkt_err_old="$dpdk_rx_pkt_err"
			dpdk_drop_pkt_hw_old="$dpdk_drop_pkt_hw"
			dpdk_drop_pkt_tot_old="$dpdk_drop_pkt_tot"
		elif [[ $DBA_RUN_PROCESS_MODE -eq 0 ]];then
			tcpreplay_Tx_pck_old="$tcpreplay_Tx_pck"
			tcpreplay_T_d_pck_old="$tcpreplay_T_d_pck"
			DBA_Rx_pck_old="$DBA_Rx_pck"
			DBA_R_d_pck_old="$DBA_R_d_pck"
			npc_rx_all_pkt_tot_old="$npc_rx_all_pkt_tot"
		elif [[ $DBA_RUN_PROCESS_MODE -eq 1 ]];then
			:
		fi

		sga_sql_old="$sga_sql"
		sga_count_old="$sga_sql"
		db_count_sql_old="$db_count_sql"
		trace_count_old="$trace_count"
		index_count_old="$index_count"
		summary_count_old="$summary_count"
		Begin_to_loose_count_old="$Begin_to_loose_count"

		##获取数据次数
		((Get_Date_num=Get_Date_num+1))
		##下次取数据时间
		((Get_info_Time=Get_info_Time+Get_info_cycle))

		##取数据次数
		printf_log 1 "Get_info num=$Get_Date_num"
		##输出取数据周期和下次取数据的时间

		printf_log 1 "Get_info cycle=$Total_Time next time=`date -d @$Get_info_Time '+%Y-%m-%d %H:%M:%S'`"
		
		tcpreplay_run_flag=`ssh root@$tcpreplay_ip ps -ef |grep "$tcpreplay_cmd_tmp" |grep -v "grep"|wc -l`		
		printf_log 1 "tcpreplay_run_flag=$tcpreplay_run_flag"
		tlog_file_complete_flag=`ssh root@$DBA_ip ls -l /dbfw_tlog |grep -v "total" |wc -l`
		printf_log 1 "tlog_file_complete_flag=$tlog_file_complete_flag"
		printf_log 1 "tlog_table_complete_flag=$cyc_trace_count"
		xml_file_complete_flag=`ssh root@$DBA_ip find /tmp/shm/ |grep -E -v 'shm/$|/0$|/1$|/2$|3$'|wc -l`	
		printf_log 1 "xml_file_complete_flag=$xml_file_complete_flag"
		printf_log 1 "xml_table_complete_flag=$xml_table_complete_flag"
		printf_log 1 "summary_complete_flag=$summary_complete_flag"


		if [[ $tcpreplay_run_flag -eq 0 ]] && \
		   [[ $tlog_file_complete_flag -eq 0 ]] && \
		   [[ $tlog_table_complete_flag -eq 0 ]] && \
		   [[ $xml_file_complete_flag -eq 0 ]] && \
		   [[ $xml_table_complete_flag -eq 0 ]] && \
		   [[ $summary_complete_flag -eq 0 ]];then

			((PT_complete_flag=PT_complete_flag+1))

		fi

		if [[ $PT_complete_flag -eq 1 ]];then
			##把打包机文件拿到本地目录
			scp -r root@$tcpreplay_ip:/tmp/tcpreplay_${File_Name_Time}_${tcpreplay_given}${tcpreplay_rate}_l${tcpreplay_loop}.log $TCPREPLAY_LOG_DIR
			ssh root@$tcpreplay_ip rm -rf /tmp/tcpreplay_${File_Name_Time}_${tcpreplay_given}${tcpreplay_rate}_l${tcpreplay_loop}.log
                        printf_log 1 "scp tcpreplay_${File_Name_Time}_${tcpreplay_given}${tcpreplay_rate}_l${tcpreplay_loop}.log complete!"
			ssh root@$DBA_ip kill -9 $nmon_pid
                        printf_log 1 "kill nmon complete!"
			
			##把成shell脚本生成的.log,.report,.nmon文件拷贝一份放入CHART_REPORTS_DIR目录,把报告处理成xlsx_chart时用
			cp $LOGS_DIR/$PT_log_name $CHART_REPORTS_DIR
			echo "$LOGS_DIR/$PT_log_name"
			echo $CHART_REPORTS_DIR
			cp $REPORTS_DIR/$PT_report_name $CHART_REPORTS_DIR
			cp $nmon_file_name $CHART_REPORTS_DIR
                        printf_log 1 "cp .report .log .nmon file to chart_reports_dir complete!"
                        printf_log 1 "shell script run end!"

			exit
		else 
			##得到周期内循环需要sleep的时间,为了使周期内循环次数尽量减小
			Get_info_Time_tmp=`date +%s`
			((sleep_time=Get_info_Time-Get_info_Time_tmp-1))
			printf_log 1 "sleep_time=$sleep_time"
			sleep $sleep_time
		fi
	elif [[ $Get_Date_num -eq 0 ]];then
		((Get_Date_num=Get_Date_num+1))		
		##得到周期内循环需要sleep的时间,为了使周期内循环次数尽量减小
		Get_info_Time_tmp=`date +%s`
		((sleep_time=Get_info_Time-Get_info_Time_tmp-1))
		printf_log 1 "sleep_time=$sleep_time"
		sleep $sleep_time
	fi

	done

