package com.zoowii.util;

import java.io.InputStream;

public class ResourceUtil {
    public static InputStream readResourceInWar(String path) {
        return getCurrentClassLoader().getResourceAsStream(path);
    }

    public static ClassLoader getCurrentClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    public static String getPropertyOrEnv(String key, String defaultValue) {
        String val = System.getProperty(key, null);
        if (val != null) {
            return val;
        }
        val = System.getenv(key);
        return val != null ? val : defaultValue;
    }
}
