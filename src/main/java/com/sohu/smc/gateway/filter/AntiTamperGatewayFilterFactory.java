package com.sohu.smc.gateway.filter;

import lombok.Data;
import org.apache.commons.io.IOUtils;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
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
            Mono<Void> validate;
            ServerWebExchange exchangeDecorate = exchange;
            if (HttpMethod.GET.equals(exchange.getRequest().getMethod())){
                validate = validateGet(replayHeader, exchange.getRequest(), config);
            } else {
                ServerHttpRequest request = exchange.getRequest();
                Mono<DataBuffer> cacheBody = DataBufferUtils.join(request.getBody(), 256 * 1024)
                        .map(dataBuffer -> {
                            try {
                                InputStream inputStream = dataBuffer.asInputStream();
                                return IOUtils.toByteArray(inputStream);
                            } catch (Exception e) {
                                throw new RuntimeException("io error", e);
                            } finally {
                                DataBufferUtils.release(dataBuffer);
                            }
                        })
                        .cache()
                        .map(bytes -> exchange.getResponse().bufferFactory().wrap(bytes));
                exchangeDecorate = exchange.mutate().request(decorate(exchange, cacheBody)).build();
                validate = validateWithBody(replayHeader, cacheBody, config);
            }
            return validate.onErrorResume(throwable -> {
                        ServerHttpResponse response = exchange.getResponse();
                        response.setStatusCode(HttpStatus.FORBIDDEN);
                        DataBuffer wrap = exchange.getResponse().bufferFactory()
                                .wrap(throwable.getMessage().getBytes());
                        return exchange.getResponse()
                                .writeWith(Mono.just(wrap));
                    })
                    .then(chain.filter(exchangeDecorate));
        };
    }



    /**
     * replayId的格式如下
     * 时间戳#随机数#随机数的加盐Md5
     * @param tamperHeader 防止重放唯一id
     * @param request request
     * @return mono
     */
    private Mono<Void> validateGet(String tamperHeader, ServerHttpRequest request, Config config) {
        String query = request.getURI()
                .getQuery();
        String md5 = DigestUtils.md5DigestAsHex((query + config.salt).getBytes());
        if (!tamperHeader.equals(md5)){
            return Mono.error(new RuntimeException("illegal tamper header"));
        }
        return Mono.empty();
    }

    private Mono<Void> validateWithBody(String tamperHeader, Mono<DataBuffer> body, Config config) {
        return body
                .map(dataBuffer -> getMd5(dataBuffer, config))
                .map(md5 -> {
                    if (!tamperHeader.equals(md5)){
                        throw new RuntimeException("illegal tamper header");
                    }
                    return "OK";
                })
                .then();
    }

    ServerHttpRequestDecorator decorate(ServerWebExchange exchange, Mono<DataBuffer> body) {
        return new ServerHttpRequestDecorator(exchange.getRequest()) {
            @Override
            public Flux<DataBuffer> getBody() {
                return body.flux();
            }
        };
    }

    private String getMd5(DataBuffer dataBuffer, Config config){
        try{
            InputStream bodyInputStream = dataBuffer.asInputStream();
            ByteArrayInputStream saltInputStream = new ByteArrayInputStream(config.salt.getBytes());
            return DigestUtils.md5DigestAsHex(new SequenceInputStream(bodyInputStream, saltInputStream));
        } catch (Exception e){
            throw new RuntimeException("get md5 error", e);
        } finally {
            DataBufferUtils.release(dataBuffer);
        }
    }



    @Data
    public static class Config {

        private String headerName;
        private String salt;

    }

}
