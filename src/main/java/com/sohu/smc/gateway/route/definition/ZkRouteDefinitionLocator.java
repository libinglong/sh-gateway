package com.sohu.smc.gateway.route.definition;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/10/22
 */
@Component
public class ZkRouteDefinitionLocator implements RouteDefinitionLocator, ApplicationContextAware {

    private ApplicationContext applicationContext;

    private static final String ROUTE_CONFIG_KEY = "${smc.gateway.route.config}" ;

    private YAMLMapper yamlMapper = new YAMLMapper();

    private volatile List<RouteDefinition> routeDefinitions;

    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        return Flux.fromIterable(routeDefinitions);
    }

    @Value(ROUTE_CONFIG_KEY)
    public void setConfig(String config) throws JsonProcessingException {
        routeDefinitions = yamlMapper.readValue(config, new TypeReference<>(){});
        if (applicationContext != null){
            applicationContext.publishEvent(new RefreshRoutesEvent(config));
        }

    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
}
