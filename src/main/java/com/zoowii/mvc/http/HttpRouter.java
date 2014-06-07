package com.zoowii.mvc.http;

import clojure.lang.*;
import com.zoowii.util.Pair;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class HttpRouter {
    private static String contextPath = null;  // the app context
    private static Logger logger = Logger.getLogger("HttpRouter");

    public static String getContextPath() {
        return contextPath;
    }

    private static ISeq routesTable = null;
    private static Var findRouteFn = null;
    private static Var urlForFn = null;

    static {
        try {
            RouterLoader.loadRouter("gitstone/routes");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean isRouteTableInited() {
        return routesTable != null;
    }

    public static void setRouteTable(ISeq _routesTable) {
        routesTable = _routesTable;
    }

    public static void setFindRouteFn(Var _findRouteFn) {
        findRouteFn = _findRouteFn;
    }

    public static void setUrlForFn(Var _urlForFn) {
        urlForFn = _urlForFn;
    }

    public static String reverseUrl(String routeName) {
        List<Object> params = new ArrayList<Object>();
        return reverseUrl(routeName, params);
    }

    public static String reverseUrl(String routeName, List<Object> params) {
        Object res = urlForFn.invoke(routesTable, routeName, params);
        return res != null ? res.toString() : null;
    }

    public static void submitRequest(HttpRequest request, HttpResponse response) {
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
