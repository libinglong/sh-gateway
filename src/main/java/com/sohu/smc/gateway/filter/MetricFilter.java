package com.sohu.smc.gateway.filter;


import com.sohu.mrd.framework.monitor.metric.MrdMetricClient;
import com.sohu.mrd.framework.monitor.metric.metric.MrdMetric;
import com.sohu.mrd.framework.zk.propertysource.util.ConfigUtils;
import com.sohu.smc.gateway.util.NetUtils;
import io.dropwizard.metrics.Timer;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/11/16
 */
@Component
public class MetricFilter implements GlobalFilter {

    private static MrdMetricClient metricClient = MrdMetricClient.create().influxdbHost("http://influx2.mrd.sohuno.com:8086")
            .database("httpserver2").measurement("meter_measurement").name("httpserver").build();


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String url = request.getURI().getPath();
        Timer.Context context = null;
        try{
            MrdMetric metric = metricClient.getEmptyTagMetric("http_server_api")
                    .withTag("url", url)
                    .withTag("server_ip", NetUtils.getIp())
                    .withTag("server_name", "gateway")
                    .withTag("server_mode", ConfigUtils.getEnv().getPath())
                    .get();
            context = metric.time();
            return chain.filter(exchange);
        } catch (Exception e){
            return Mono.error(e);
        } finally {
            if (context != null) {
                context.stop();
            }
        }
    }
}
