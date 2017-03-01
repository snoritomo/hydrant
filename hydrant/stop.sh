#!/bin/sh

PID_FILE=./hydrant.pid

if [ !  -f $PID_FILE ] ;then
	echo "$PID_FILE is not exsists."
	exit 1;
fi

if [ !  -r $PID_FILE ] ;then
	echo "$PID_FILE can not read."
	exit 1;
fi

if [ !  -w $PID_FILE ] ;then
	echo "$PID_FILE can not operate."
	exit 1;
fi

PID_NO=`cat $PID_FILE`

kill -KILL $PID_NO
rm -f $PID_FILE

#
# EOF
#