FROM openjdk:11
ENV LANG C.UTF-8
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
COPY target/app.jar app.jar
COPY entrypoint.sh /usr/local/bin/
ADD skywalking-agent-for-docker /skywalking-agent-for-docker
ENTRYPOINT ["entrypoint.sh"]