#!/bin/bash

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

##得到DBA_Server_PT.sh脚本所在目录
DBA_PT_HOME_DIR="$( cd "$( dirname "$0"  )" && pwd  )"
CONFF="DBA_Auto_PT.ini"
DBA_ip=$(GetKey $CONFF "Product_Info.DBA_ip")
tcpreplay_ip=$(GetKey $CONFF "Packing_Tool.tcpreplay_ip")

##把DBA、打包机设置免密登录
mkdir -p /root/.ssh
#rm -rf /root/.ssh/{id_rsa,id_rsa.pub,authorized_keys}
#ssh-keygen  -t rsa -P '' -f /root/.ssh/id_rsa
chmod 0600 $DBA_PT_HOME_DIR/sshkey/{id_rsa,id_rsa.pub}
\cp -f $DBA_PT_HOME_DIR/sshkey/{id_rsa,id_rsa.pub} /root/.ssh/
mv /root/.ssh/{id_rsa.pub,authorized_keys}
ssh_keygen_login "${DBA_ip}" "y"
RET=$?
if [ $RET -ne 0 ];then
	printf "%s\t%-6s\t%-5s\t%s\n" "[$(date +%Y-%m-%d' '%H:%M:%S)]" "$$" "INFO" "$DBA_ip ssh_keygen_login failed!"
	exit
fi
printf "%s\t%-6s\t%-5s\t%s\n" "[$(date +%Y-%m-%d' '%H:%M:%S)]" "$$" "INFO" "$DBA_ip ssh_keygen_login success!"
ssh_keygen_login "${tcpreplay_ip}" "y"
printf "%s\t%-6s\t%-5s\t%s\n" "[$(date +%Y-%m-%d' '%H:%M:%S)]" "$$" "INFO" "$tcpreplay_ip ssh_keygen_login success!"
