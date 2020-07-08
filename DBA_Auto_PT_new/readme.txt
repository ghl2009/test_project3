##说明文件

1、请先配置DBA_Server_PT.ini 文件


2、执行 sshkey.sh 先配置互信，如果配置过不需要再次执行，(审计会定期清除ssh配置，隔天需要再次执行)
	
   ./sshkey.sh
   执行时会提示输入对应ip设备的密码(一台设备输入两次），每次输入后回车即可
	
   ssh_keygen_login success! 提示成功即可


3、修改Main_DBA_Auto_PT.sh脚本中执行参数


4、执行Main_DBA_Auto_PT.sh
	
   nohup ./Main_PY.sh &


注：运行前建议把MMS平台运行参数：单独的prepare语句审计的值改为1
    touch /tmp/npc_perform_control;sleep 5;rm -rf /tmp/npc_perform_control