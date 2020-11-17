package com.sohu.smc.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.Response;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/11/17
 */
@Slf4j
@Component
public class AuthGatewayFilterFactory extends AbstractGatewayFilterFactory<AuthGatewayFilterFactory.Config> {

    private final AsyncHttpClient asyncHttpClient;
    private final ObjectMapper mapper;

    public AuthGatewayFilterFactory(AsyncHttpClient asyncHttpClient, ObjectMapper mapper) {
        this.asyncHttpClient = asyncHttpClient;
        this.mapper = mapper;
    }


    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            List<String> v = exchange.getRequest()
                    .getHeaders()
                    .get(config.headerName);
            if (CollectionUtils.isEmpty(v)) {
                ServerHttpResponse response = exchange.getResponse();
                response.setStatusCode(HttpStatus.FORBIDDEN);
                DataBuffer wrap = response.bufferFactory()
                        .wrap((config.headerName + " header is required").getBytes());
                return exchange.getResponse()
                        .writeWith(Mono.just(wrap));
            }
            String authHeader = v.get(0);
            return validate(authHeader, config)
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

    private Mono<Void> validate(String authHeader, Config config) {
        Map<String, String> params;
        try {
            //noinspection BlockingMethodInNonBlockingContext
            params = mapper.readValue(Base64.getUrlDecoder().decode(authHeader), new TypeReference<>() {
            });
        } catch (IOException e) {
            return Mono.error(e);
        }
        BoundRequestBuilder builder = asyncHttpClient.preparePost(config.url);
        params.forEach(builder::addFormParam);
        CompletableFuture<Response> future = builder.execute()
                .toCompletableFuture();
        return Mono.fromFuture(future)
                .map(Response::getResponseBody)
                .map(ret -> {
                    try {
                        Map<String, Object> retMap = mapper.readValue(ret, new TypeReference<>() {
                        });
                        if (!retMap.get("status").equals(200)) {
                            throw new RuntimeException(String.valueOf(retMap.get("message")));
                        }
                        return "OK";
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .then();
    }

    @Data
    public static class Config {
        private String url;
        private String headerName;
    }
}
