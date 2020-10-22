#!/bin/bash
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