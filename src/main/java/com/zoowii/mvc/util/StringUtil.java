package com.zoowii.mvc.util;

public class StringUtil {
    public static String rjust(String str, int n, String fillChar) {
        if (str.length() >= n) {
            return str;
        }
        return rjust(fillChar + str, n, fillChar);
    }
}
