package com.sohu.smc.gateway.endpoint;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/12/2
 */
@RestController
public class HealthCheck {

    @GetMapping("health/check")
    public String health(){
        return "ok";
    }

}
