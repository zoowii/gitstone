package com.zoowii.mvc.http.handler_callers;

import clojure.lang.IFn;
import com.zoowii.mvc.http.HttpContext;
import com.zoowii.mvc.http.HttpHandlerChain;
import com.zoowii.mvc.http.IHttpHandler;

/**
 * Created by zoowii on 14/10/6.
 */
public class ClojureFnCaller extends HandlerCaller {
    @Override
    protected IHttpHandler makeHandler() throws Exception {
        return new IHttpHandler() {
            @Override
            public void process(HttpContext ctx, Object handlerObj, HttpHandlerChain chain) {
                // 如果是一个Clojure的函数,就直接用这个函数处理请求
                IFn fn = (IFn) handlerObj;
                fn.invoke(ctx.getRequest(), ctx.getResponse());
                chain.processNext(ctx, handlerObj);
            }
        };
    }
}
