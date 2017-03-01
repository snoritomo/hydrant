@echo off
rem 
rem set environment
rem
set JAVA_EXE="java"
set JVM_OPTION=-Xms256M -Xmx512M
set XPROXY_CLASSPATH=./;./*;./conf;./lib/*
set XPROXY_EXE_CLASS=com.kikisoftware.hydrant.log.AccessLogSummary
set LOG=./logs/console.log
set ARGS=300 UTF-8 accessSum.tsv

rem
rem execute
rem
echo ------------------------------------------------------------------------ >> %LOG%
date +"[%Y/%m/%d %T]" >> %LOG%
%JAVA_EXE% %JVM_OPTION% -classpath %XPROXY_CLASSPATH% %XPROXY_EXE_CLASS% %ARGS% >> %LOG% 2>&1
