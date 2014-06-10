package com.zoowii.mvc.http;

import clojure.lang.*;
import com.zoowii.mvc.handlers.RouterNotFoundException;
import com.zoowii.util.ClojureUtil;
import com.zoowii.util.FileUtil;
import com.zoowii.util.Pair;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
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
            Object handlerObj = routeResult.get(Keyword.intern("handler"));
            // TODO: 增加before和after filter, 从而可以注入其他功能(需要封装新的Stream代替request和response中的流)
            if (handlerObj == null) {
                throw new RouterNotFoundException("Can't find handler for route " + pathInfo);
            }
            if (handlerObj instanceof List) {
                // 如果是一个seq,则代表是[类, 静态方法]的形式,把请求调度过去
                List handlerArray = (List) handlerObj;
                if (handlerArray.size() < 2) {
                    throw new RouterNotFoundException("If you use Java method as handler, you need to specify the class and static method");
                }
                Class handlerClass = (Class) handlerArray.get(0);
                String methodName = (String) handlerArray.get(1);
                Method handlerMethod = handlerClass.getDeclaredMethod(methodName, HttpRequest.class, HttpResponse.class);
                handlerMethod.invoke(handlerClass, request, response);
            } else if (handlerObj instanceof IPersistentMap) {
                // 如果是一个Clojure的map,这个就表示返回的内容
                // 格式类似Ring, 为{:status: ..., :headers: ..., :body: str or byte[] or InputStream, :content-type: ...}
                // :headers的格式为{:header-name or "header-name": value, ... }
                IPersistentMap map = (IPersistentMap) handlerObj;
                Object statusObj = map.valAt(Keyword.intern("status"));
                if (statusObj != null) {
                    if (statusObj instanceof Long) {
                        response.setStatus(((Long) statusObj).intValue());
                    } else if (statusObj instanceof Integer) {
                        response.setStatus((Integer) statusObj);
                    }
                }
                Object contentTypeObj = map.valAt(Keyword.intern("content-type"));
                if (contentTypeObj != null && contentTypeObj instanceof String) {
                    response.setContentType((String) contentTypeObj);
                }
                Object headersObj = map.valAt(Keyword.intern("headers"));
                if (headersObj != null && headersObj instanceof IPersistentMap) {
                    IPersistentMap headersMap = (IPersistentMap) headersObj;
                    for (Object entryObj : headersMap) {
                        MapEntry entry = (MapEntry) entryObj;
                        Object keyObj = entry.getKey();
                        Object valueObj = entry.getValue();
                        String key = ClojureUtil.name(keyObj);
                        String value = ClojureUtil.name(valueObj);
                        response.setHeader(key, value);
                    }
                }
                Object bodyObj = map.valAt(Keyword.intern("body"));
                if (bodyObj != null) {
                    if (bodyObj instanceof String) {
                        response.append((String) bodyObj);
                    } else if (bodyObj instanceof InputStream) {
                        FileUtil.writeFullyStream((InputStream) bodyObj, response.getOutputStream());
                    } else if (bodyObj instanceof byte[]) {
                        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream((byte[]) bodyObj);
                        FileUtil.writeFullyStream(byteArrayInputStream, response.getOutputStream());
                    }
                }
            } else if (handlerObj instanceof String) {
                String str = (String) handlerObj;
                response.append(str);
            } else if (handlerObj instanceof IFn) {
                // 如果是一个Clojure的函数,就直接用这个函数处理请求
                IFn fn = (IFn) handlerObj;
                fn.invoke(request, response);
            } else {
                throw new RouterNotFoundException("Unsupported handler type for path " + pathInfo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
