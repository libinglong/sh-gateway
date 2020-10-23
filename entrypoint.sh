#!/bin/bash
# 这个文件用于启动docker内进程.目前主要做两件事
# 1.exec命令非常关键,其执行后续命令,并替代当前shell进程.由于当前进程pid为1,因此后续的java进程的pid为1.因此可以接收到ctrl+c等信号
# 2.预处理一些操作,同时在shell环境下可以解析使用docker传入的环境变量
set -e
log_home=/opt/logs
mkdir -p ${log_home}
JAVA_OPTS="
-Xms2G -Xmx2G -server
-XX:+HeapDumpOnOutOfMemoryError
-XX:+ParallelRefProcEnabled
-XX:+DisableExplicitGC
-XX:+UnlockExperimentalVMOptions -XX:+UseZGC
-Xlog:safepoint,classhisto*=trace,age*,gc*=info:file=${log_home}/gc-%t.log:time,tid,tags:filecount=5,filesize=50m
"
# shellcheck disable=SC2086
exec java ${JAVA_OPTS} -jar app.jar