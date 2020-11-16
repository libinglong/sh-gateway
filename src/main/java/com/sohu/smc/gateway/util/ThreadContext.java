package com.sohu.smc.gateway.util;

import java.util.ArrayList;

/**
 * @author binglongli217932
 * <a href="mailto:libinglong9@gmail.com">libinglong:libinglong9@gmail.com</a>
 * @since 2020/11/16
 */
public class ThreadContext {

    private static ThreadLocal<ArrayList<Long>> threadLocal = ThreadLocal.withInitial(ArrayList::new);

    public static ArrayList<Long> getList(){
        return threadLocal.get();
    }

}
