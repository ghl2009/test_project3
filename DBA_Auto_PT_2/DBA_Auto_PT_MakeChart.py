#!/usr/bin/python
#_*_ coding: utf-8 _*_

########################################################
## auther:liuguanghui
## date:20181222
## work:DBA performance testing(to xlsx and MakeChart)
########################################################

import sys
import os
import glob
import csv
import datetime

##设置默认字符集
reload(sys)
sys.setdefaultencoding('utf-8')

##此函数功能:得到当前路径
def GetCwdPath():
        return os.path.split(os.path.realpath(sys.argv[0]))[0]

##此函数功能:由csv生成xlsx并生成统计图(线图）
def CSV_to_XLSXandCHART(reportfile,Data_type_list):

	##把后缀名去掉
	xlsxfile_name = os.path.splitext(reportfile)[0]

	##新建一个xlsx文件
	workbook = xlsxwriter.Workbook(CHART_REPORTS_DIR + '/' + xlsxfile_name + '.xlsx')

	##建第一个sheet页,为测试过程中的配置或结果信息
	worksheet = workbook.add_worksheet('CONFIG')

	##列宽
	worksheet.set_column('A:A', 2)
	worksheet.set_column('B:B', 18)
	worksheet.set_column('C:C', 22)
	worksheet.set_column('D:H', 18)

	csvfile_name = report_to_csv(reportfile,'CONFIG')

	##数据入第一个sheet
	with open(CHART_REPORTS_DIR + '/' + csvfile_name + '.csv','rb') as f:
		reader = csv.reader(f)
		for r, row in enumerate(reader):
			for c, col in enumerate(row):
				#print col
				worksheet.write(r+1, c+1, col)
	

	##按数据类型建多个sheet页
	for Data_type in Data_type_list:
		
		##根据数据类型得到要建sheet页的名称
		if Data_type[:3] != 'CPU':
			sheet_name = Data_type_sheet_name_dict['%s'%Data_type]	

		else:
			sheet_name = Data_type

		##xlsx文件建sheet
		worksheet = workbook.add_worksheet(sheet_name)

		##列宽
		worksheet.set_column('A:A', 21)
		worksheet.set_column('B:B', 18)
		worksheet.set_column('C:C', 10)
		worksheet.set_column('D:L', 20)
		##根据数据类型生成csv文件
		csvfile_name = report_to_csv(reportfile,Data_type)

		#print Data_type

		with open(CHART_REPORTS_DIR + '/' + csvfile_name+'.csv','rb') as f:
			reader = csv.reader(f)
			for r, row in enumerate(reader):
				for c, col in enumerate(row):
					if r > 0 and c >1:
						#print col
						col=float(col)
					worksheet.write(r+31, c, col)

		##新建线图
		chart1 = workbook.add_chart({'type': 'line'})

		##哪一列加入线图
		#print Data_type

		if Data_type[:3] != 'CPU':
			column_str = Data_type_column_dict['%s'%Data_type]	
		else:
			column_str = Data_type_column_dict['%s'%Data_type[:3]]

		# Configure the first series.

		##字符串切成列表
		column_list = column_str.split(',')

		##一图多线
		for num in column_list:

			num = int(num)

			chart1.add_series({
			    'name':[sheet_name,31,num],
			    'categories':[sheet_name,32,2,r+31,2],
			    'values':[sheet_name,32,num,r+31,num],
			    })

		if Data_type[:3] != 'CPU':
			chart_title = Data_type_chart_title_dict['%s'%Data_type]	
			chart1.set_title({'name': chart_title})
		else:
			chart_title = Data_type_chart_title_dict['%s'%Data_type[:3]]
			chart1.set_title({'name': Data_type+chart_title})
		chart1.height=600
		chart1.width=1180
		chart1.set_x_axis({'position_axis': 'on_tick'})
		#chart1.set_x_axis({'name':'s'})	
		worksheet.insert_chart('A1',chart1,{'x_offset': 5 ,'y_offset': 5})
	
	workbook.close()

##此函数功能:打包过程由shell script生成的原始报告数据按数据类型生成csv文件
##返回csv文件名称,DBA版本
def report_to_csv(reportfile,Data_type):
	reportfile_name = os.path.splitext(reportfile)[0]
	#print reportfile
	csvfile='%s_%s.csv'%(reportfile_name,Data_type)
	os.system('cat %s/%s |grep %s > %s/%s'%(CHART_REPORTS_DIR,reportfile,Data_type,CHART_REPORTS_DIR,csvfile))
	csvfile_name = os.path.splitext(csvfile)[0]
	
	return csvfile_name

##函数功能拿到所有特定类型文件,返回列表
def file_name(file_dir,file_tpye):
	file_list = [] 
	#for root, dirs, files in os.walk(file_dir):  
	for file in os.listdir(file_dir):  
		if os.path.splitext(file)[1] == file_tpye:  
			file_list.append(file)  
	return file_list 


##定义字典:各数据类型生成的曲线在xlsx的都有哪几列
Data_type_column_dict={\
'PCAP_TOT_COUNT':'3,5',\
'SQL_TOT_COUNT':'3,4,5',\
'TLOG_TOT_COUNT':'3,4,5,6,7',\
'TO_LOOSE_TOT_COUNT':'3',\
'TLOG_LAG_COUNT':'3,4,5,6,7',\
'PCAP_TOT_PS_COUNT':'3,5',\
'SQL_TOT_PS_COUNT':'3,4,5',\
'TLOG_TOT_PS_COUNT':'3,4,5,6,7',\
'PCAP_CYC_COUNT':'3,5',\
'SQL_CYC_COUNT':'3,4,5',\
'TLOG_CYC_COUNT':'3,4,5,6,7',\
'TO_LOOSE_CYC_COUNT':'3',\
'PCAP_CYC_PS_COUNT':'3,5',\
'SQL_CYC_PS_COUNT':'3,4,5',\
'TLOG_CYC_PS_COUNT':'3,4,5,6,7',\
'MEMORY_MB':'3,4,5',\
'CPU':'3,4,6,7'\
}

##定义字典:各数据类型生成的曲线在xlsx的都有哪几列
Data_type_chart_title_dict={\
'PCAP_TOT_COUNT':'tcpreplay发包,nfw收包分析图(总数  单位:个)',\
'SQL_TOT_COUNT':'预期,NPP解析,inst_db_count表中sql分析图(总数  单位:条)',\
'TLOG_TOT_COUNT':'预期,NPP解析,tla入库,FTM索引,统计进度分析图(总数  单位:条)',\
'TO_LOOSE_TOT_COUNT':'tla_error索引延迟标记次数(总数  单位:次)',\
'TLOG_LAG_COUNT':'expect2npp,npp2tla,npp2ftm,npp2tls,tla2tls各延迟情况分析图(单位:秒)',\
'PCAP_TOT_PS_COUNT':'tcpreplay发包,nfw收包分析图(整体速度  单位:个/秒)',\
'SQL_TOT_PS_COUNT':'预期,NPP解析,inst_db_count表中sql分析图(整体速度  单位:条/秒)',\
'TLOG_TOT_PS_COUNT':'预期,NPP解析,tla入库,FTM索引,统计进度分析图(整体速度  单位:条/秒)',\
'PCAP_CYC_COUNT':'tcpreplay发包,nfw收包分析图(周期内数量  单位:次)',\
'SQL_CYC_COUNT':'预期,NPP解析,inst_db_count表中sql分析图(周期内数量  单位:条)',\
'TLOG_CYC_COUNT':'预期,NPP解析,tla入库,FTM索引,统计进度分析图(周期内数量  单位:条)',\
'TO_LOOSE_CYC_COUNT':'tla_error索引延迟标记次数(周期内数量  单位:条)',\
'PCAP_CYC_PS_COUNT':'tcpreplay发包,nfw收包分析图(周期内速度  单位:个/秒)',\
'SQL_CYC_PS_COUNT':'预期,NPP解析,inst_db_count表中sql分析图(周期内速度  单位:条/秒)',\
'TLOG_CYC_PS_COUNT':'预期,NPP解析,tla入库,FTM索引,统计进度分析图(周期内速度  单位:条/秒)',\
'MEMORY_MB':'总内存,内存剩余,内存使用分析图(单位:MB)',\
'CPU':'使用情况分析图(百分比)'\
}

##定义字典:各数据类型生成的曲线在xlsx的都有哪几列
Data_type_sheet_name_dict={\
'PCAP_TOT_COUNT':'PCAP_T',\
'SQL_TOT_COUNT':'SQL_T',\
'TLOG_TOT_COUNT':'TLOG_T',\
'TO_LOOSE_TOT_COUNT':'LOOSE_T',\
'TLOG_LAG_COUNT':'TLOG_L',\
'PCAP_TOT_PS_COUNT':'PCAP_T_P',\
'SQL_TOT_PS_COUNT':'SQL_T_P',\
'TLOG_TOT_PS_COUNT':'TLOG_T_P',\
'PCAP_CYC_COUNT':'PCAP_C',\
'SQL_CYC_COUNT':'SQL_C',\
'TLOG_CYC_COUNT':'TLOG_C',\
'TO_LOOSE_CYC_COUNT':'LOOSE_C',\
'PCAP_CYC_PS_COUNT':'PCAP_C_P',\
'SQL_CYC_PS_COUNT':'SQL_C_P',\
'TLOG_CYC_PS_COUNT':'TLOG_C_P',\
'MEMORY_MB':'MEM',\
'CPU':'CPUxxx'\
}

##main开始

##得到脚本路径
DBA_PT_HOME_DIR = GetCwdPath()
CHART_REPORTS_DIR = DBA_PT_HOME_DIR + '/chart_reports_dir'

##依赖包没有安装,放到了本地路径下
sys.path.append("%s/python-site-packages"%DBA_PT_HOME_DIR)
import xlsxwriter

#os.system('cd %s;touch aaaaa'%CHART_REPORTS_DIR)

##打包过程由shell script生成的原始数据报告文件
reportfile_list = file_name(CHART_REPORTS_DIR,'.report') 
reportfile_list.sort()

#if os.path.isdir(CHART_REPORTS_DIR):
#	os.system('cd %s;rm -rf *.xlsx *.log *.report reportfile data_nmon logfile'%CHART_REPORTS_DIR)

##初始化一个写列头的标志
Write_title_flag = 0



##把多个文件生成xlsx文件及chart_line图
for reportfile in reportfile_list:

	##根据设备不同，算出.report中每个周期内的数据条数(行数）
	One_cyc_Data_count = os.popen("grep 'SYS_CPU' %s/%s |awk -F= '{print $2}'|awk '{print $1}'"%(CHART_REPORTS_DIR,reportfile))
	One_cyc_Data_count = One_cyc_Data_count.read()
	print int(One_cyc_Data_count)
	One_cyc_Data_count = int(One_cyc_Data_count) + 17
	print("One_cyc_Data_count=%s"%One_cyc_Data_count)

	##得到数据类型列表
	Data_type_list_tmp = os.popen("tail -n%s %s/%s |awk -F, '{print $2}'|grep 'COUNT'"%(One_cyc_Data_count,CHART_REPORTS_DIR,reportfile))
	Data_type_list_tmp = Data_type_list_tmp.read()
	Data_type_list = Data_type_list_tmp.split('\n')[:-1]

	Data_type_list_tmp = os.popen("tail -n%s %s/%s |awk -F, '{print $2}'|grep 'MEMORY_MB'"%(One_cyc_Data_count,CHART_REPORTS_DIR,reportfile))
	Data_type_list_tmp = Data_type_list_tmp.read()
	Data_type_list_tmp = Data_type_list_tmp.split('\n')[:-1]
	Data_type_list = Data_type_list + Data_type_list_tmp

	Data_type_list_tmp = os.popen("tail -n%s %s/%s |awk -F, '{print $2}'|grep 'CPU'"%(One_cyc_Data_count,CHART_REPORTS_DIR,reportfile))
	Data_type_list_tmp = Data_type_list_tmp.read()
	Data_type_list_tmp = Data_type_list_tmp.split('\n')[:-1]
	Data_type_list = Data_type_list + Data_type_list_tmp

	print Data_type_list

	##调生成线图的函数
	CSV_to_XLSXandCHART(reportfile,Data_type_list)

	##判断是用的什么速率,Mbps或pps
	rate_param = reportfile.split('_')[5][0]
	if rate_param == "M":
		rate=reportfile.split('_')[5][1:] + 'Mbps'
	elif rate_param == "p":
		rate=reportfile.split('_')[5][1:] + 'pps'
	else:
		print(error)
		exit
	
	DBA_Version = reportfile.split('_')[0]
	print DBA_Version 

	DBA_SYSCPU = reportfile.split('_')[1]
	print DBA_SYSCPU

	DBA_SYSMEM = reportfile.split('_')[2]
	print DBA_SYSMEM 

	##定义不同速度下tla等tps重定向对csv文件的名称
	#Diff_Rate_csvfile = "%s_%s_%s_Diff_Rate_npp_tla_ftm_tls_tps.csv"%(DBA_Version, DBA_SYSCPU, DBA_SYSMEM)
	Diff_Rate_csvfile = "PT_Diff_Rate_npp_tla_ftm_tls_tps.csv"
	Diff_Rate_xlsxfile = "%s_%s_%s_PT_Diff_Rate_npp_tla_ftm_tls_tps.xlsx"%(DBA_Version, DBA_SYSCPU, DBA_SYSMEM)

	##处理第一个文件时把列头名称写入csv文件,后期后根据生成的这个文件,生成不同速度下的纵向性能对比图
	if Write_title_flag == 0:
		if rate_param == "M":
			os.system("echo 'DBA_Version,tcpreplay_rate/Mbps,expect_count/s,sga_count/s,trace_count/s,index_count/s,summary_count/s' > %s/%s"%(CHART_REPORTS_DIR,Diff_Rate_csvfile))	
		elif rate_param == "p":
			os.system("echo 'DBA_Version,tcpreplay_rate/pps,expect_count/s,sga_count/s,trace_count/s,index_count/s,summary_count/s' > %s/%s"%(CHART_REPORTS_DIR,Diff_Rate_csvfile))	
		
		Write_title_flag=Write_title_flag+1
		
	value1 = os.popen("tail -n%s %s/%s |grep 'TLOG_TOT_PS_COUNT' |awk -F, '{print $4,$5,$6,$7,$8}' "%(One_cyc_Data_count,CHART_REPORTS_DIR,reportfile))
        value1 = value1.read()
        value1 = value1.split('\n')[:-1]
	value1 =" ".join(value1)
	value1 = value1.replace(' ',',')
	rate_value=DBA_Version + ',' + rate +  ',' + value1
	print rate_value
	os.system('echo %s >> %s/%s'%(rate_value,CHART_REPORTS_DIR,Diff_Rate_csvfile))

##文件名去后缀
Diff_Rate_csvfilename = os.path.splitext(Diff_Rate_csvfile)[0]

##新建一个xlsx文件
workbook = xlsxwriter.Workbook(CHART_REPORTS_DIR + '/' + Diff_Rate_csvfilename + '.xlsx')

##sheetname
Diff_Rate_sheetname = Diff_Rate_csvfilename.split('_npp')[0]

##xlsx文件建sheet
worksheet = workbook.add_worksheet(Diff_Rate_sheetname)

##列宽
worksheet.set_column('A:A', 21)
worksheet.set_column('B:B', 20)
worksheet.set_column('C:L', 22)

with open(CHART_REPORTS_DIR + '/' + Diff_Rate_csvfile,'rb') as f:
	reader = csv.reader(f)
	for r, row in enumerate(reader):
		for c, col in enumerate(row):
			if r > 0 and c >1:
				#print col
				col=float(col)
			worksheet.write(r+31, c, col)

##新建线图
chart1 = workbook.add_chart({'type': 'line'})

##字符串切成列表
column_list = [3,4,5,6]

##一图多线
for num in column_list:
	int(num)
	
	chart1.add_series({
	    'name':[Diff_Rate_sheetname,31,num],
	    'categories':[Diff_Rate_sheetname,32,1,r+31,1],
	    'values':[Diff_Rate_sheetname,32,num,r+31,num],
	    })

chart1.set_title({'name': '%s 不同压力场景下npp、tla、ftm、tls性能分析图'%DBA_Version})
chart1.height=600
chart1.width=1180
chart1.set_x_axis({'position_axis': 'on_tick'})
#chart1.set_x_axis({'name':'s'})	
worksheet.insert_chart('A1',chart1,{'x_offset': 5 ,'y_offset': 5})
	
workbook.close()

##获取当前时间,按特定格式赋值给变量
nowTime=datetime.datetime.now().strftime('%Y%m%d%H%M%S')

os.system('cd %s;mv %s.xlsx %s'%(CHART_REPORTS_DIR,Diff_Rate_csvfilename,Diff_Rate_xlsxfile))
os.system('cd %s;rm -rf *.csv'%CHART_REPORTS_DIR)
os.system('cd %s;mkdir -p reportfile;mv *.report reportfile'%CHART_REPORTS_DIR)
os.system('cd %s;mkdir -p data_nmon;mv *.nmon data_nmon'%CHART_REPORTS_DIR)
os.system('cd %s;mkdir -p logfile;mv *.log logfile'%CHART_REPORTS_DIR)
os.system('cd %s;tar zcvf %s_%s_%s_PT_Report_%s.tar.gz *.xlsx *.docx reportfile data_nmon logfile'%(CHART_REPORTS_DIR,DBA_Version,DBA_SYSCPU,DBA_SYSMEM,nowTime))
os.system('cd %s;rm -rf *.xlsx reportfile data_nmon logfile'%CHART_REPORTS_DIR)
