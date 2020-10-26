package com.sohu.smc.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/10/26
 */
@Slf4j
@Component
public class LogFilter implements GlobalFilter {

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Map<String,Object> logMap = new HashMap<>();
        String uri = exchange.getRequest()
                .getURI()
                .toString();
        logMap.put("uri",uri);
        try {
            //noinspection BlockingMethodInNonBlockingContext
            log.info("{}",objectMapper.writeValueAsString(logMap));
        } catch (JsonProcessingException e) {
            log.error("日志打印异常",e);
        }
        return chain.filter(exchange);
    }
}
