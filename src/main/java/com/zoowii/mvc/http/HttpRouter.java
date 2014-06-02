package com.zoowii.mvc.http;

import clojure.lang.*;
import com.zoowii.mvc.handlers.RouterNotFoundException;
import com.zoowii.mvc.util.Pair;
import com.zoowii.mvc.util.RouterLoader;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class HttpRouter {
    private static String contextPath = null;  // the app context
    private static Logger logger = Logger.getLogger("HttpRouter");

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
    private static ISeq routesTable = null;
    private static Var findRouteFn = null;

    public static boolean isAnyRouteTableInited() {
        return routesTable != null;
    }

    public static void setAnyRouteTable(ISeq _routesTable) {
        routesTable = _routesTable;
    }

    public static void setFindAnyRouteFn(Var _findRouteFn) {
        findRouteFn = _findRouteFn;
    }

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

    public static void submitRequestToAnyRoute(HttpRequest request, HttpResponse response) {
        if (contextPath == null) {
            contextPath = request.getHttpServletRequest().getContextPath();
        }
        String pathInfo = request.getHttpServletRequest().getPathInfo();
        pathInfo = pathInfo.substring(pathInfo.indexOf(getContextPath()), pathInfo.length());
        String requestMethod = request.getHttpServletRequest().getMethod().toUpperCase();
        Keyword reqMethodKeyword = Keyword.intern(requestMethod);
        try {
            PersistentArrayMap routeResult = (PersistentArrayMap) findRouteFn.invoke(routesTable, reqMethodKeyword, pathInfo);
            if (routeResult == null) {
                response.append("404");
                return;
            }
            logger.info(routeResult.toString());
            ISeq bindings = (ISeq) routeResult.get(Keyword.intern("binding"));
            while (bindings != null && bindings.count() > 0) {
                PersistentVector binding = (PersistentVector) bindings.first();
                bindings = bindings.next();
                String paramName = (String) binding.get(0);
                Object paramValue = binding.get(1);
                request.getParams().add(new Pair<String, Object>(paramName, paramValue));
            }
            PersistentVector handlerArray = (PersistentVector) routeResult.get(Keyword.intern("handler"));
            Class handlerClass = (Class) handlerArray.get(0);
            String methodName = (String) handlerArray.get(1);
            Method handlerMethod = handlerClass.getDeclaredMethod(methodName, HttpRequest.class, HttpResponse.class);
            handlerMethod.invoke(handlerClass, request, response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
