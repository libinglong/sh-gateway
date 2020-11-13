package com.sohu.smc.gateway.filter;

import com.sohu.smc.gateway.util.ByteArrayUtils;
import lombok.Data;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/11/11
 */
@Component
public class AntiTamperGatewayFilterFactory extends AbstractGatewayFilterFactory<AntiTamperGatewayFilterFactory.Config> {


    public AntiTamperGatewayFilterFactory(){
        super(Config.class);
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("headerName", "salt");
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
            return validate(replayHeader, exchange.getRequest(), config)
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
     * @param tamperHeader 防止重放唯一id
     * @param request request
     * @return mono
     */
    private Mono<Void> validate(String tamperHeader, ServerHttpRequest request, Config config) {
        if (HttpMethod.GET.equals(request.getMethod())){
            String query = request.getURI()
                    .getQuery();
            String md5 = DigestUtils.md5DigestAsHex((query + config.salt).getBytes());
            if (!tamperHeader.equals(md5)){
                return Mono.error(new RuntimeException("illegal tamper header"));
            }
            return Mono.empty();
        }
        return DataBufferUtils.join(request.getBody(), 256 * 1024)
                .map(dataBuffer -> {
                    byte[] combine = ByteArrayUtils.combine(dataBuffer.asByteBuffer().array(), config.salt.getBytes());
                    return DigestUtils.md5DigestAsHex(combine);
                })
                .map(md5 -> {
                    if (!tamperHeader.equals(md5)){
                        throw new RuntimeException("illegal tamper header");
                    }
                    return "OK";
                })
                .then();
    }



    @Data
    public static class Config {

        private String headerName;
        private String salt;

    }

}
