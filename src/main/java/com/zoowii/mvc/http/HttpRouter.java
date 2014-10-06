package com.zoowii.mvc.http;

import clojure.lang.*;
import com.google.common.base.Function;
import com.zoowii.mvc.handlers.RouterNotFoundException;
import com.zoowii.util.*;
import com.zoowii.mvc.http.handler_callers.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HttpRouter {
    private static String contextPath = null;  // the app context
    private static Logger logger = Logger.getLogger("HttpRouter");
    private static List<IMiddleWare> middleWares = new ArrayList<IMiddleWare>();
    private static List<IInterceptor> interceptors = new ArrayList<IInterceptor>();

    public static List<IInterceptor> getInterceptors() {
        return interceptors;
    }

    public static String getContextPath() {
        return contextPath;
    }

    private static ISeq routesTable = null;
    private static Var findRouteFn = null;
    private static Var urlForFn = null;

    static {
        // FIXME: add demo interceptors and middlewares
        interceptors.add(new IInterceptor() {
            @Override
            public boolean beforeHandler(HttpContext ctx, Object handlerObj) {
                logger.info("before handler");
                return true;
            }

            @Override
            public void afterHandler(HttpContext ctx, Object handlerObj) {
                logger.info("after handler");
            }
        });
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

    public static IMiddleWare mergeMiddleWares() {
        return new IMiddleWare() {
            @Override
            public void call(final HttpContext ctx, final Object handlerObj, Callable callable) {
                Callable finalCallable = callable;
                for (final IMiddleWare middleWare : middleWares) {
                    finalCallable = new HolderCallable<Callable>(finalCallable) {
                        @Override
                        public Object call() throws Exception {
                            middleWare.call(ctx, handlerObj, holder);
                            return null;
                        }
                    };
                }
                try {
                    finalCallable.call();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    public static void submitRequest(HttpRequest request, HttpResponse response) {
        HttpContext ctx = new HttpContext(request, response);
        if (contextPath == null) {
            contextPath = request.getHttpServletRequest().getContextPath();
        }
        String pathInfo = request.getPathInfo();
        pathInfo = pathInfo.substring(pathInfo.indexOf(getContextPath()), pathInfo.length());
        String requestMethod = request.getHttpServletRequest().getMethod().toUpperCase();
        Keyword reqMethodKeyword = Keyword.intern(requestMethod);
        try {
            IPersistentMap routeResult = (IPersistentMap) findRouteFn.invoke(routesTable, reqMethodKeyword, pathInfo);
            if (routeResult == null) {
                new RouteNotFoundHandlerCaller().process(ctx, null);
                return;
            }
            logger.info(routeResult.toString());
            ISeq bindings = (ISeq) routeResult.valAt(Keyword.intern("binding"));
            while (bindings != null && bindings.count() > 0) {
                PersistentVector binding = (PersistentVector) bindings.first();
                bindings = bindings.next();
                String paramName = (String) binding.get(0);
                Object paramValue = binding.get(1);
                request.getParams().add(new Pair<String, Object>(paramName, paramValue));
            }
            Object handlerObj = routeResult.valAt(Keyword.intern("handler"));
            HandlerCaller handlerCaller;
            if (handlerObj == null) {
                handlerCaller = new RouteNotFoundHandlerCaller();
            } else if (handlerObj instanceof List) {
                handlerCaller = new JavaMvcControllerCaller(); // TODO: cache it
            } else if (handlerObj instanceof IPersistentMap) {
                handlerCaller = new RingResponseCaller();
            } else if (handlerObj instanceof String) {
                handlerCaller = new StringResponseCaller();
            } else if (handlerObj instanceof IFn) {
                handlerCaller = new ClojureFnCaller();
            } else {
                handlerCaller = new UnsupportedHandlerCaller();
            }
            handlerCaller.process(ctx, handlerObj);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "routes table not found", e);
            try {
                new RouteNotFoundHandlerCaller().process(ctx, null);
            } catch (Exception e2) {

            }
        }
    }
}
