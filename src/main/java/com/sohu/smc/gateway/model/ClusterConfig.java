package com.sohu.smc.gateway.model;

import lombok.Data;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/11/17
 */
@Data
public class ClusterConfig {

    private String ip;
    private int master;
    private int persistence;
    private int port;

}
