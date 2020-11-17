package com.sohu.smc.gateway.filter;

import io.lettuce.core.RedisException;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import lombok.Data;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/11/11
 */
@Component
public class AntiReplayGatewayFilterFactory extends AbstractGatewayFilterFactory<AntiReplayGatewayFilterFactory.Config> {

    private volatile RedisAdvancedClusterAsyncCommands<String, String> currentCmd;

    private final AtomicInteger atomicInteger = new AtomicInteger();

    @SuppressWarnings("unchecked")
    private final RedisAdvancedClusterAsyncCommands<String, String>[] cmds = new RedisAdvancedClusterAsyncCommands[2];

    private static final String PREFIX = "anti_replay::";

    private static final Set<Long> record = ConcurrentHashMap.newKeySet();

    public AntiReplayGatewayFilterFactory(RedisClusterClient primaryRedisClient, RedisClusterClient secondaryRedisClient){
        super(Config.class);
        cmds[0] = primaryRedisClient.connect()
                .async();
        cmds[1] = secondaryRedisClient.connect()
                .async();
        currentCmd = cmds[0];
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("headerName", "salt", "expireTimeInMilliSecond");
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            List<String> v = exchange.getRequest()
                    .getHeaders()
                    .get(config.headerName);
            if (CollectionUtils.isEmpty(v)){
                ServerHttpResponse response = exchange.getResponse();
                response.setStatusCode(HttpStatus.FORBIDDEN);
                DataBuffer wrap = response.bufferFactory()
                        .wrap((config.headerName + " header is required").getBytes());
                return exchange.getResponse()
                        .writeWith(Mono.just(wrap));
            }
            String replayHeader = v.get(0);
            return validate(replayHeader, config)
                    .onErrorResume(throwable -> {
                            ServerHttpResponse response = exchange.getResponse();
                            response.setStatusCode(HttpStatus.FORBIDDEN);
                            DataBuffer wrap = exchange.getResponse().bufferFactory()
                                    .wrap(throwable.getMessage().getBytes());
                            return exchange.getResponse()
                                    .writeWith(Mono.just(wrap));
                    })
                    .then(chain.filter(exchange));
        };
    }



    /**
     * replayId的格式如下
     * 时间戳#随机数#随机数的加盐Md5
     * @param replayHeader 防止重放唯一id
     * @return mono
     */
    private Mono<Void> validate(String replayHeader, Config config) {
        String[] split = replayHeader.split("#");
        long timestamp = Long.parseLong(split[0]);
        String random = split[1];
        String md5FromHttp = split[2];

        if (split.length != 3){
            return Mono.error(new Exception("wrong length replayId"));
        }
        if (timestamp + config.expireTimeInMilliSecond < System.currentTimeMillis()){
            return Mono.error(new Exception("expired replayId"));
        }
        String md5 = DigestUtils.md5DigestAsHex((timestamp + random + config.salt).getBytes());
        if (!md5.equals(md5FromHttp)){
            return Mono.error(new Exception("illegal replayId"));
        }
        return Mono.fromCompletionStage(exist(replayHeader))
                .flatMap(ret -> {
                    if (ret.equals(1L)) {
                        return Mono.error(new RuntimeException("you can not replay the request"));
                    }
                    return Mono.fromCompletionStage(psetex(replayHeader, config))
                            .map(setRet -> {
                                if (!"OK".equals(setRet)) {
                                    throw new RuntimeException("replay store failed");
                                }
                                return setRet;
                            });
                })
                .then();
    }

    @Data
    public static class Config {

        private String headerName;
        private String salt;
        private Long expireTimeInMilliSecond;

    }

    private CompletionStage<?> exist(String replayHeader){
        try {
            return currentCmd.exists(PREFIX + replayHeader);
        } catch (RedisException e){
            return process(e);
        }
    }

    private CompletionStage<?> psetex(String replayHeader, Config config){
        try {
            return currentCmd.psetex(PREFIX + replayHeader, config.expireTimeInMilliSecond, "");
        } catch (RedisException e){
            return process(e);
        }
    }

    /**
     * 如果10s内出现20次异常,则切换redis
     * @param e RedisException
     * @return CompletionStage
     */
    private CompletionStage<Object> process(RedisException e){
        long now = System.currentTimeMillis();
        record.add(now);
        if (record.size() == 20){
            boolean anyMatch = record.stream()
                    .anyMatch(time -> time + 10_000 > now);
            if (anyMatch){
                currentCmd = cmds[atomicInteger.incrementAndGet() % 2];
            }
            record.clear();
        }
        return CompletableFuture.failedStage(e);
    }




}
