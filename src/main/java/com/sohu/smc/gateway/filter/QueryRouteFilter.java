package com.sohu.smc.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

/**
 * 由于路由方式可能是较为复杂的,因此,使用filter来完成路由的查询功能
 * 当参数中含有{@link QueryRouteFilter#PARAM_NAME},那么则直接返回路由id,而不会转发请求.
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/10/27
 */
@Component
public class QueryRouteFilter implements GlobalFilter, Ordered {

    private static final String PARAM_NAME = "queryForRoute";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        boolean queryForRoute = exchange.getRequest()
                .getQueryParams()
                .containsKey(PARAM_NAME);
        if (queryForRoute){
            Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
            byte[] bytes = route.getId().getBytes();
            DataBuffer wrap = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse()
                    .writeWith(Mono.just(wrap));
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE;
    }
}
