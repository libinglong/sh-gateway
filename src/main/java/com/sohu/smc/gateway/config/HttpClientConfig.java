package com.sohu.smc.gateway.config;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.Dsl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.asynchttpclient.Dsl.config;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/11/17
 */
@Configuration
public class HttpClientConfig {

    @Bean
    public AsyncHttpClient asyncHttpClient() {
        DefaultAsyncHttpClientConfig config = config()
//                .setProxyServer(proxyServer("127.0.0.1",8888))
                .build();
        return Dsl.asyncHttpClient(config);
    }


}
