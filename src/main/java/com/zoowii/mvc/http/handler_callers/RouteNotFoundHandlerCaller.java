package com.zoowii.mvc.http.handler_callers;

import com.zoowii.mvc.handlers.RouterNotFoundException;
import com.zoowii.mvc.http.HttpContext;
import com.zoowii.mvc.http.HttpHandlerChain;
import com.zoowii.mvc.http.IHttpHandler;

/**
 * Created by zoowii on 14/10/6.
 */
public class RouteNotFoundHandlerCaller extends HandlerCaller {
    @Override
    protected IHttpHandler makeHandler() throws Exception {
        return new IHttpHandler() {
            @Override
            public void process(HttpContext ctx, Object handlerObj, HttpHandlerChain chain) {
                throw new RuntimeException(new RouterNotFoundException("Can't find handler for route " + ctx.getRequest().getPathInfo()));
            }
        };
    }
}