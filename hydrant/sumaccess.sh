#!/bin/sh

#
# set environment
#
SET_ENV="env LANG=ja_JP.eucJP"
JAVA_EXE="java"
JVM_OPTION="-Xms256M -Xmx512M -server"
XPROXY_CLASSPATH="./*:./lib/*:./conf"

XPROXY_EXE_CLASS="com.kikisoftware.hydrant.log.AccessLogSummary"

ARGS=300 UTF-8 accessSum.tsv

#
# wirte LogFileName
#
LOG_FILE=./logs/console.log
#
# write PID
#
PID_FILE=./hydrant_sumaccess.pid

if [ -f $PID_FILE ]
then
	echo "It has already started."
	exit 1;
else
	#
	# execute
	#
	echo -e "\n" >> $LOG_FILE
	echo ------------------------------------------------------------------------ >> $LOG_FILE
	date +"[%Y/%m/%d %T]" >> $LOG_FILE
	$SET_ENV $JAVA_EXE $JVM_OPTION -classpath $XPROXY_CLASSPATH $XPROXY_EXE_CLASS $ARGS >> $LOG_FILE 2>&1 &

	echo $! > $PID_FILE

	wait `cat $PID_FILE`
	rm -f $PID_FILE
fi

#
# EOF
#
