#### gateway

##### 项目用途

网关路由

##### 项目结构

docker + jdk11 +  spring gateway + zk propertysource

##### 技术介绍

* 为什么使用docker?

  该项目将采用docker部署方式,主要目的是进一步解耦研发和运维的关注点。

  * 目前devops平台提供的打包方式，入侵了maven的打包过程，例如打包方式为jar时要求maven pom必须提供finnalName和packaging标签。这显然属于规范外的事情，是“部门内部的特殊情况”。这些额外的要求导致后续维护困难。比如不了解该特殊情况的人在本地构建项目且运行良好，但在devops却构建失败。并因此浪费大量的时间排查问题。
  * devops的相关配置不受git管理，因此也无法在配置出错后回滚。

* gc选择

  为了达到更低的延迟，选择G1GC或ZGC。追求吞吐量则选择CMS。目前选择了ZGC

* [zk propertysource](http://ops.mrd.sohuno.com/gitlab/binglongli217932/zk-propertysource)

