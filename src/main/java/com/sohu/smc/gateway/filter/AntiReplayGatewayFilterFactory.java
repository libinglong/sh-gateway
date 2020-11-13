package com.sohu.smc.gateway.filter;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.async.RedisAsyncCommands;
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

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/11/11
 */
@Component
public class AntiReplayGatewayFilterFactory extends AbstractGatewayFilterFactory<AntiReplayGatewayFilterFactory.Config> {

    RedisAsyncCommands<String, String> async;

    private static final String PREFIX = "anti_replay::";

    public AntiReplayGatewayFilterFactory(RedisClient redisClient){
        super(Config.class);
        async = redisClient.connect()
                .async();
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
     * @param replayId 防止重放唯一id
     * @return mono
     */
    private Mono<Void> validate(String replayId, Config config) {
        String[] split = replayId.split("#");
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
        return Mono.fromCompletionStage(async.exists(PREFIX + replayId))
                .flatMap(ret -> {
                    if (ret == 1) {
                        return Mono.error(new RuntimeException("you can not replay the request"));
                    }
                    return Mono.fromCompletionStage(async.psetex(PREFIX + replayId, config.expireTimeInMilliSecond, ""))
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

}
