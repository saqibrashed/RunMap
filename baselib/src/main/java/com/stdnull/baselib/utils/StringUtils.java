package com.stdnull.baselib.utils;

/**
 * 字符串处理工具类
 * Created by chen on 2017/1/20.
 */

public final class StringUtils {
    public static boolean isEmpty(String string){
        return string == null || string.length() < 1;
    }

    public static String splitDate(String date){
        int time = Integer.valueOf(date);
        StringBuilder sb = new StringBuilder(time/10000+"年");
        time %=10000;
        sb.append(time/100+"月");
        sb.append(time%100+"日");
        return sb.toString();
    }
}
