package com.sohu.smc.gateway.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sohu.smc.gateway.model.ClusterConfig;
import org.asynchttpclient.AsyncHttpClient;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/11/17
 */
@Service
public class PantherRedisService {


    private final AsyncHttpClient asyncHttpClient;
    private final ObjectMapper mapper;

    public PantherRedisService(AsyncHttpClient asyncHttpClient, ObjectMapper mapper){
        this.asyncHttpClient = asyncHttpClient;
        this.mapper = mapper;
    }

    private final String url = "https://panther.sohurdc.com/api/redis/cluster/release?uid=";

    public List<ClusterConfig> getConfig(String uid) {
        try {
            String responseBody = asyncHttpClient.prepareGet(url + uid)
                    .execute()
                    .get()
                    .getResponseBody();
            return mapper.readValue(responseBody, new TypeReference<>() {});
        } catch (Exception e){
            throw new RuntimeException("get config from remote failed", e);
        }


    }

}
