package com.sohu.smc.gateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

//@SpringBootTest
class GatewayApplicationTests {

    @Test
    void contextLoads() {
        Flux.range(-2,5)
                .map(integer -> integer + 100)
                .map(s -> {
                    System.out.println("first " + s);
                    if (s == 100 || s == 109){
                        throw new RuntimeException("s == 0 error");
                    }
                    return s;
                })
                .map(integer -> integer + 1000)
                .map(integer -> integer + 1000)
                .onErrorContinue((throwable, o) -> {
                    System.out.println("continue" + o);
                })
//                .onErrorResume(throwable -> Flux.range(7,8))
                .subscribe(integer -> System.out.println("sub " + integer));

    }

    @Test
    public void fun() throws JsonProcessingException {

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String,Object> map = new HashMap<>();
        map.put("a", 1);
        String a = objectMapper.writeValueAsString(map);
        String b = objectMapper.writeValueAsString("map");
        System.out.println(a);
        System.out.println(b);

    }

}
