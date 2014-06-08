package com.zoowii.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class DateUtil {

    public static String formatDate(Date date, String format, TimeZone timeZone) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        sdf.setTimeZone(timeZone);
        return sdf.format(date);
    }

    public static String formatDate(Date date, String format) {
        return formatDate(date, format, TimeZone.getTimeZone("Asia/Shanghai"));
    }

    /**
     * format: yyyy-MM-dd
     */
    public static String dateStringFormat(Date date) {
        return formatDate(date, "yyyy-MM-dd");
    }
}
