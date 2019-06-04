#!/bin/bash

./DBA_Auto_PT.sh M 20 1 >/tmp/M20.log

sleep 300

./DBA_Auto_PT.sh M 30 1 >/tmp/M30.log

sleep 300

./DBA_Auto_PT.sh M 40 2 >/tmp/M40.log

sleep 300

./DBA_Auto_PT.sh M 50 2 >/tmp/M50.log

sleep 300

./DBA_Auto_PT.sh p 10000 1 >/tmp/p10000.log

sleep 300

./DBA_Auto_PT.sh p 13000 1 >/tmp/p13000.log

sleep 300

./DBA_Auto_PT.sh p 15000 1 >/tmp/p15000.log

sleep 300

./DBA_Auto_PT.sh p 20000 2 >/tmp/p20000.log


