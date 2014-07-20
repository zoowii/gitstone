package com.zoowii.mvc.http;

import clojure.lang.ISeq;
import clojure.lang.RT;
import clojure.lang.Var;

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
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

}
