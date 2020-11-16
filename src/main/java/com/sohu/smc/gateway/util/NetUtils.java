package com.sohu.smc.gateway.util;


import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/8/8
 */
@Slf4j
public class NetUtils {

    private static String ip;

    private static volatile boolean init = false;

    public static String getIp() {
        if (!init) {
            ip = doGetIp();
        }
        return ip;
    }

    private synchronized static String doGetIp() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()){
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                while (inetAddresses.hasMoreElements()){
                    InetAddress inetAddress = inetAddresses.nextElement();
                    byte[] address = inetAddress.getAddress();
                    if (address.length == 4 && !inetAddress.isLoopbackAddress() && inetAddress.isSiteLocalAddress()){
                        return inetAddress.getHostAddress();
                    }
                }
            }
            return null;
        } catch (SocketException e) {
            log.info("获取ip失败",e);
            return null;
        } finally {
            init = true;
        }
    }

}
