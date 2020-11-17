package com.sohu.smc.gateway.config;

import com.sohu.smc.gateway.service.PantherRedisService;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.RedisClusterClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/11/13
 */
@Configuration
public class RedisConfig {

    private final PantherRedisService pantherRedisService;

    public RedisConfig(PantherRedisService pantherRedisService){
        this.pantherRedisService = pantherRedisService;
    }

    private final String yzUid = "21116";
    private final String yzPassword = "97a30351ebeb4d7aadae767371b4f665";
    private final String bxUid = "21117";
    private final String bxPassword = "38c9156c4ad34cad9477d0421efafa28";

    @Bean
    public RedisClusterClient primaryRedisClient(){
        return getRedisClient(yzUid, yzPassword);
    }

    @Bean
    public RedisClusterClient secondaryRedisClient(){
        return getRedisClient(bxUid, bxPassword);
    }

    private RedisClusterClient getRedisClient(String uid, String pwd){
        List<RedisURI> uris = pantherRedisService.getConfig(uid)
                .stream()
                .map(c -> RedisURI.builder()
                        .withHost(c.getIp())
                        .withPort(c.getPort())
                        .withPassword(pwd)
                        .build())
                .collect(Collectors.toList());
        RedisClusterClient client = RedisClusterClient.create(uris);
        ClusterClientOptions op = ClusterClientOptions.builder()
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                .build();
        client.setOptions(op);
        return client;
    }

}
