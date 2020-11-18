#### 项目用途

网关，路由，负载均衡，安全等等

#### 项目结构

docker + jdk11 +  spring gateway + zk propertysource

#### 技术介绍

* 该项目将采用docker部署方式,主要目的是进一步解耦研发和运维的关注点。

  * 目前devops平台提供的打包方式，入侵了maven的打包过程，例如打包方式为jar时要求maven pom必须提供finnalName和packaging标签。这显然属于规范外的事情，是“部门内部的特殊情况”。这些额外的要求导致后续维护困难。比如不了解该特殊情况的人在本地构建项目且运行良好，但在devops却构建失败。并因此浪费大量的时间排查问题。
  * devops的相关配置不受git管理，因此也无法在配置出错后回滚。

* gc选择
    
    为了达到更低的延迟，选择G1GC或ZGC。追求吞吐量则选择CMS。目前选择了ZGC

* [zk propertysource](http://ops.mrd.sohuno.com/gitlab/binglongli217932/zk-propertysource)

    使用zk作为配置中心

#### 扩展功能

##### 路由动态修改

路由从zk配置中心加载，修改后路由立即生效，无需重启应用程序。配置键为smc.gateway.route.config，格式和spring gateway原生配置方式相同。例如：

在application.yml中的配置如果是

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: rewritepath_route
          uri: https://tieba.baidu.com
          predicates:
            - Path=/a/p/**
          filters:
            - RewritePath=/a(?<segment>/?.*), $\{segment}
        - id: anti_tamper
          uri: http://localhost:8081
          predicates:
            - Path=/a/b/**
          filters:
            - RewritePath=/a(?<segment>/?.*), $\{segment}
            - AntiTamper=x-tamper,adbfzfa
```

则在zk中的配置为

```yaml
- id: rewritepath_route
  uri: https://tieba.baidu.com
  predicates:
    - Path=/a/p/**
  filters:
    - RewritePath=/a(?<segment>/?.*), $\{segment}
- id: anti_tamper
  uri: http://localhost:8081
  predicates:
    - Path=/a/b/**
  filters:
    - RewritePath=/a(?<segment>/?.*), $\{segment}
    - AntiTamper=x-tamper,adbfzfa
```

只需要把下述前缀去掉即可

```yaml
spring:
  cloud:
    gateway:
      routes:
```

##### 过滤器

防重放过滤器和防篡改过滤器增加的安全性不如数据库的约束，重要的安全保证不应该依赖于这两个过滤器。

* 防重放过滤器 AntiReplayGatewayFilterFactory

    在filters中添加`- AntiReplay=x-replay,adbfzfa,100000`，可实现防重放功能，例如

      ```yaml
      - id: rewritepath_route
        uri: https://tieba.baidu.com
        predicates:
          - Path=/a/p/**
        filters:
          - RewritePath=/a(?<segment>/?.*), $\{segment}
          - AntiReplay=x-replay,foo,100000
      ```

    x-replay是header的名称，foo是md5的盐，100000是过期时间。调用端需要在请求头中添加键x-replay，值为timestamp + 随机数 + md5(timestamp + 随机数 + foo)，该随机数在过期时间内不可重复请求，否则返回403。

* 防篡改过滤器 AntiTamperGatewayFilterFactory

    在filters中添加`- AntiTamper=x-tamper,foo`，可实现防篡改功能，例如

      ```yaml
      - id: anti_tamper
        uri: http://localhost:8081
        predicates:
          - Path=/a/b/**
        filters:
          - RewritePath=/a(?<segment>/?.*), $\{segment}
          - AntiTamper=x-tamper,foo
      ```

    x-tamper是header的名称，foo是md5的盐，调用端需要在请求头中添加键x-tamper，如果是get请求，值为md5(?后面的所有字符 + foo)。如果不是get请求，值为md5(请求体 + foo)

* 日志过滤器 LogFilter 全局过滤器 无需配置

* 监控过滤器 MetricFilter 全局过滤器 无需配置

    收集一些信息，在grafana展示

* 查询路由过滤器 QueryRouteFilter 全局过滤器 无需配置

    在url中传递queryForRoute参数，那么本次请求不会路由到任何服务，并且将匹配到的路由规则作为json返回。

* 认证过滤器(插件需求专用) AuthGatewayFilterFactory

      在filters中添加`- Auth=`，可实现认证功能

      ```yaml
      - id: authxxx
        uri: http://localhost:8081
        predicates:
          - Path=/a/b/**
        filters:
          - RewritePath=/a(?<segment>/?.*), $\{segment}
          - Auth=x-auth,http://foo/bar
      ```

    //todo 等前端确定参数位置，完成剩余文档。







