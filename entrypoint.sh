#!/bin/bash
# 这个文件用于启动docker内进程.目前主要做两件事
# 1.exec命令非常关键,其执行后续命令,并替代当前shell进程.由于当前进程pid为1,因此后续的java进程的pid为1.因此可以接收到ctrl+c等信号
# 2.预处理一些操作,同时在shell环境下可以解析使用docker传入的环境变量
set -e
storge_dir=/opt/mrdapp/docker/gateway-preonline
log_home=${storge_dir}/logs
mkdir -p ${log_home}
# shellcheck disable=SC2154
if [[ "${data_center}" == "bx" ]]
then
  opa_server="10.16.13.222:11800,10.16.13.223:11800"
elif [[ "${data_center}" == "yz" ]]
then
  opa_server="10.18.75.162:11800,10.18.74.94:11800"
else
  echo env data_center must be bx or yz
  exit;
fi
JAVA_OPTS="
-Xms2G -Xmx2G -server
-XX:+HeapDumpOnOutOfMemoryError
-XX:+ParallelRefProcEnabled
-XX:+DisableExplicitGC
-XX:+UnlockExperimentalVMOptions -XX:+UseZGC
-Xlog:safepoint,classhisto*=trace,age*,gc*=info:file=${log_home}/gc-%t.log:time,tid,tags:filecount=5,filesize=50m
-javaagent:/skywalking-agent-for-docker/skywalking-agent.jar
-Dskywalking.agent.service_name=smc-gateway
-Dskywalking.collector.backend_service=${opa_server}
"
# shellcheck disable=SC2086
# tee表示往文件中存一份日志的同时也输出至stdout
# https://unix.stackexchange.com/questions/145651/using-exec-and-tee-to-redirect-logs-to-stdout-and-a-log-file-in-the-same-time
# 这里如果用exec java ${JAVA_OPTS} -jar app.jar | tee ${log_home}/stdout.log,无法接受docker stop发出的信号
exec java ${JAVA_OPTS} -jar app.jar &> >(tee ${log_home}/stdout.log)