#!/bin/bash
set -e
logfile=gc.log.$(date "+%Y-%m-%d.%H-%M-%S")
JAVA_OPTS="
-Xms2G -Xmx2G -server
-XX:+UnlockExperimentalVMOptions -XX:+UseZGC
-Xloggc:${logfile}
-XX:+PrintGC
-XX:+PrintReferenceGC
-XX:+PrintHeapAtGC
-XX:+PrintGCDetails
-XX:+PrintGCTimeStamps
-XX:+PrintGCDateStamps
-XX:+PrintAdaptiveSizePolicy
-XX:+PrintGCApplicationStoppedTime
-XX:+PrintGCApplicationConcurrentTime
-XX:+PrintTenuringDistribution
-XX:+SafepointTimeout
-XX:SafepointTimeoutDelay=1000
-XX:+PrintSafepointStatistics
-XX:PrintSafepointStatisticsCount=1
"
# shellcheck disable=SC2086
exec java ${JAVA_OPTS} -jar app.jar