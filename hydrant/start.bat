@echo off
rem 
rem set environment
rem
set JAVA_EXE="java"
set JVM_OPTION=-Xms256M -Xmx512M
set XPROXY_CLASSPATH=./;./*;./conf;./lib/*
set XPROXY_EXE_CLASS=com.kikisoftware.hydrant.Main
set LOG=./logs/console.log

rem
rem execute
rem
echo ------------------------------------------------------------------------ >> %LOG%
date +"[%Y/%m/%d %T]" >> %LOG%
%JAVA_EXE% %JVM_OPTION% -classpath %XPROXY_CLASSPATH% %XPROXY_EXE_CLASS% >> %LOG% 2>&1
