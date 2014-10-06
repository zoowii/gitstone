package com.zoowii.mvc.http;

import clojure.lang.*;

import java.io.IOException;

public class RouterLoader {

    public static void loadRouter(String path) throws IOException {
        if (HttpRouter.isRouteTableInited()) {
            return;
        }
        try {
            RT.load(path);
            String cljNs = path.replaceAll("/", "\\.");
            ISeq routeTable = (ISeq) RT.var(cljNs, "routes").get();
            HttpRouter.setRouteTable(routeTable);
            Var findRouteFn = RT.var("any-route.http", "find-route");
            HttpRouter.setFindRouteFn(findRouteFn);
            Var urlForFn = RT.var("any-route.http", "url-for");
            HttpRouter.setUrlForFn(urlForFn);
            // add interceptors and middlewares
            Var interceptorsVar = RT.var(cljNs, "interceptors");
            if (interceptorsVar != null && interceptorsVar.get() instanceof IPersistentCollection) {
                ISeq interceptorsSeq = ((IPersistentCollection) interceptorsVar.get()).seq();
                while (interceptorsSeq != null && interceptorsSeq.count() > 0) {
                    try {
                        if (interceptorsSeq.first() instanceof IInterceptor) {
                            HttpRouter.addInterceptor((IInterceptor) interceptorsSeq.first());
                        }
                    } finally {
                        interceptorsSeq = interceptorsSeq.next();
                    }
                }
            }
            Var middleWaresVar = RT.var(cljNs, "middlewares");
            if (middleWaresVar != null && middleWaresVar.get() instanceof IPersistentCollection) {
                ISeq middleWaresSeq = ((IPersistentCollection) middleWaresVar.get()).seq();
                while (middleWaresSeq != null && middleWaresSeq.count() > 0) {
                    try {
                        if (middleWaresSeq.first() instanceof IMiddleWare) {
                            HttpRouter.addMiddleWare((IMiddleWare) middleWaresSeq.first());
                        }
                    } finally {
                        middleWaresSeq = middleWaresSeq.next();
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

}
