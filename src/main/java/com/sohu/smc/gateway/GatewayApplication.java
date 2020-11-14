package com.sohu.smc.gateway;

import com.sohu.mrd.framework.zk.propertysource.anno.EnableZkConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.core.publisher.Hooks;

@EnableZkConfig
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
//        调试reactor
//        Hooks.onOperatorDebug();
        SpringApplication.run(GatewayApplication.class, args);
    }

}
