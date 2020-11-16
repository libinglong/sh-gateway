package com.sohu.smc.gateway.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/11/13
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisClient primaryRedisClient(){
        RedisURI localhost = RedisURI.builder()
                .withHost("localhost")
                .withPort(6379)
                .build();
        RedisClient redisClient = RedisClient.create(localhost);
        ClientOptions options = ClientOptions.builder()
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                .build();
        redisClient.setOptions(options);
        return redisClient;
    }

    @Bean
    public RedisClient secondaryRedisClient(){
        RedisURI localhost = RedisURI.builder()
                .withHost("localhost")
                .withPort(6380)
                .build();
        RedisClient redisClient = RedisClient.create(localhost);
        ClientOptions options = ClientOptions.builder()
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                .build();
        redisClient.setOptions(options);
        return redisClient;
    }

}
