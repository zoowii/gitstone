package com.zoowii.mvc.util;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ResourceUtil {
    public static InputStream readResourceInWar(String path) {
        return getCurrentClassLoader().getResourceAsStream("../.." + path);
    }

    public static InputStream readResource(String path) {
        return getCurrentClassLoader().getResourceAsStream(path);
    }

    public static ClassLoader getCurrentClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    public static Class getClassOrNone(String classFullName) {
        try {
            return getCurrentClassLoader().loadClass(classFullName);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * 在packages中所有的包路径中查找类className，如果className本身就是full-path-class-name，则直接返回这个类
     */
    public static Class findClassInPackages(List<String> packages, String className) {
        if (packages == null) {
            packages = new ArrayList<String>();
        }
        Class clazz = getClassOrNone(className);
        if (clazz != null) {
            return clazz;
        }
        for (String packageName : packages) {
            String fullClassName = packageName + "." + className;
            clazz = getClassOrNone(fullClassName);
            if (clazz != null) {
                return clazz;
            }
        }
        return null;
    }
}
