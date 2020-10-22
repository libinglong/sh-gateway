FROM java:11
ENV LANG C.UTF-8
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
COPY target/app.jar app.jar
# 之所以使用sh命令而不是直接使用java命令，就是因为sh命令可以解析环境变量$JAVA_OPTS
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app.jar $PROFILES"]