package com.zoowii.util;

import org.apache.commons.codec.binary.Base64;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Random;

public class StringUtil {
    public static String randomString(int length) {
        String base = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(base.length());
            sb.append(base.charAt(number));
        }
        return sb.toString();
    }

    public static String md5(String str) {
        try {
            byte[] bytesOfMessage = str.getBytes("UTF-8");
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] thedigest = md.digest(bytesOfMessage);
            return new String(thedigest, Charset.forName("UTF-8"));
        } catch (NoSuchAlgorithmException e) {
            return str;
        } catch (UnsupportedEncodingException e2) {
            return str;
        }
    }

    public static String base64encode(String str) {
        try {
            return new String(Base64.encodeBase64(str.getBytes("UTF-8")));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String encryptPassword(String password, String salt) {
        if (salt == null) {
            salt = "";
        }
        if (password == null) {
            password = "";
        }
        // FIXME
        return base64encode(base64encode(password.trim()) + salt.trim());
    }

    public static String rjust(String str, int n, String fillChar) {
        if (str.length() >= n) {
            return str;
        }
        return rjust(fillChar + str, n, fillChar);
    }

    public static String join(List<String> items, String sep) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < items.size(); ++i) {
            if (i > 0) {
                builder.append(sep);
            }
            builder.append(items.get(i));
        }
        return builder.toString();
    }

    public static List<String> splitPath(String path) {
        return ListUtil.filter(ListUtil.list(path.split("/")), ListUtil.notEmptyString);
    }

    public static String pathMinus(List<String> pathItems1, List<String> pathItems2) {
        return pathMinus(join(pathItems1, "/"), join(pathItems2, "/"));
    }

    /**
     * 给出两个文件路径path,计算两者的差(或者是相对路径)
     * path1 - path2
     * eg. a/b/c/d - a/b = c/d
     * TODO: 暂时不考虑路径不包含的情况,还有path1 < path2的情况(这种情况需要'.', '..'等符号), 也不考虑Windows风格的路径'\'
     */
    public static String pathMinus(String path1, String path2) {
        List<String> pathItems1 = splitPath(path1);
        List<String> pathItems2 = splitPath(path2);
        if (pathItems1.size() <= pathItems2.size()) {
            return ""; // TODO
        }
        List<String> resultPathItems = pathItems1.subList(pathItems2.size(), pathItems1.size());
        return join(resultPathItems, "/");
    }

    public static boolean eq(String o1, String o2) {
        if (o1 == null) {
            return o2 == null;
        }
        return o1.equals(o2);
    }

}
