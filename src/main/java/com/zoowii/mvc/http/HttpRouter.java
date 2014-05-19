package com.zoowii.mvc.http;

import com.zoowii.mvc.handlers.RouterNotFoundException;
import com.zoowii.mvc.util.RouterLoader;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class HttpRouter {
    private static String contextPath = null;  // the app context

    private static RouterHandlerInfo web404Handler = DefaultConfig.getDefaultWeb404HandlerInfo();

    public static void setWeb404Handler(Class handlerClass, Method handlerMethod) {
        web404Handler.handlerClass = handlerClass;
        web404Handler.handlerMethod = handlerMethod;
    }

    public static String getContextPath() {
        return contextPath;
    }

    public static class RouterHandlerInfo {
        public Pattern urlPattern;
        public String url;
        public String requestMethod;
        public Class handlerClass;
        public Method handlerMethod;

        public boolean matchUrl(String urlPathInfo) {
            if (url.equals(urlPathInfo)) {
                return true;
            }
            if (url.endsWith("*")) {
                if (urlPathInfo.indexOf(url.substring(0, url.length() - 1)) >= 0) {
                    return true;
                }
            }
            System.out.println(url + ";" + urlPathInfo);
            return urlPattern.matcher(urlPathInfo).find();
        }
    }

    static {
        try {
            RouterLoader.loadRouterFromFile("routes");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<RouterHandlerInfo> routeTable = null;

    /**
     * 清空路由表，主要调试时使用，从而每次请求都重新加载路由表
     */
    public static void clearRouteTableCache() {
        routeTable = null;
    }

    public static boolean isRouteTableInited() {
        return routeTable != null;
    }

    public static void addRouter(String httpMethod, String urlPattern, Class handlerClass, String handlerMethodName) throws NoSuchMethodException {
        RouterHandlerInfo handlerInfo = new RouterHandlerInfo();
        if (httpMethod == null) {
            httpMethod = "*";
        }
        handlerInfo.requestMethod = httpMethod.trim().toUpperCase();
        String url = urlPattern.trim();
        String regexStr = "^" + urlPattern.trim() + "$";
        handlerInfo.urlPattern = Pattern.compile(regexStr);
        handlerInfo.url = url;
        handlerInfo.handlerClass = handlerClass;
        handlerInfo.handlerMethod = handlerInfo.handlerClass.getDeclaredMethod(handlerMethodName, HttpRequest.class, HttpResponse.class);
        getRouteTable().add(handlerInfo);
    }

    public static List<RouterHandlerInfo> getRouteTable() {
        if (routeTable == null) {
//            loadDemoRouteTable();
            routeTable = new ArrayList<RouterHandlerInfo>();
        }
        return routeTable;
    }

    public static void submitRequest(HttpRequest request, HttpResponse response) {
        if (contextPath == null) {
            contextPath = request.getHttpServletRequest().getContextPath();
        }
        String pathInfo = request.getHttpServletRequest().getPathInfo();
        pathInfo = pathInfo.substring(pathInfo.indexOf(getContextPath()), pathInfo.length());
        String requestMethod = request.getHttpServletRequest().getMethod().toUpperCase();
        for (int i = 0; i < getRouteTable().size(); ++i) {
            RouterHandlerInfo handlerInfo = getRouteTable().get(i);
            // TODO: use regex pattern
            if (handlerInfo.matchUrl(pathInfo) && (requestMethod.equals(handlerInfo.requestMethod) || handlerInfo.requestMethod.equals("*"))) {
                try {
                    handlerInfo.handlerMethod.invoke(handlerInfo.handlerClass, request, response);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
                return;
            }
        }
        try {
            web404Handler.handlerMethod.invoke(web404Handler.handlerClass, request, response);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
//        throw new RouterNotFoundException(pathInfo);
    }
}
