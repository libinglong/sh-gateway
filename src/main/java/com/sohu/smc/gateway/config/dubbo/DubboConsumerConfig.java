package com.sohu.smc.gateway.config.dubbo;

import com.sohu.smc.usercenter.ds.model.userinfo.UserBaseInfoDTO;
import com.sohu.smc.usercenter.ds.service.userinfo.UserBaseService;
import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.Collections;
import java.util.List;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/11/9
 */
@Configuration
@EnableDubbo(scanBasePackages = "com.sohu.smc.usercenter.ds.service.userinfo")
@PropertySource("classpath:dubbo-consumer.properties")
public class DubboConsumerConfig implements CommandLineRunner {

    @Reference(group = "bx-smc-usercenter-ds-server-test")
    private UserBaseService userBaseService;

    @Override
    public void run(String... args) throws Exception {
        List<UserBaseInfoDTO> userBaseInfoDTOS = userBaseService.listBaseInfo(Collections.singletonList(6731775212905742118L));
        System.out.println(userBaseInfoDTOS);
    }
}
