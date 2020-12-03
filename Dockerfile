FROM openjdk:11
ENV LANG C.UTF-8
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
RUN apt-get update
RUN apt-get install net-tools
COPY target/app.jar app.jar
COPY entrypoint.sh /usr/local/bin/
COPY skywalking-agent-for-docker /skywalking-agent-for-docker
ENTRYPOINT ["entrypoint.sh"]
HEALTHCHECK --interval=1s --timeout=1s --start-period=60s \
              CMD curl -f http://localhost:${SERVER_PORT}/health/check || exit 1