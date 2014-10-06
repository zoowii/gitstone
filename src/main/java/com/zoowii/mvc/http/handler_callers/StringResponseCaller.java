package com.zoowii.mvc.http.handler_callers;

import com.zoowii.mvc.http.HttpContext;
import com.zoowii.mvc.http.HttpHandlerChain;
import com.zoowii.mvc.http.IHttpHandler;

/**
 * Created by zoowii on 14/10/6.
 */
public class StringResponseCaller extends HandlerCaller {
    @Override
    protected IHttpHandler makeHandler() throws Exception {
        return new IHttpHandler() {
            @Override
            public void process(HttpContext ctx, Object handlerObj, HttpHandlerChain chain) {
                try {
                    String str = (String) handlerObj;
                    ctx.getResponse().append(str);
                    chain.processNext(ctx, handlerObj);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
}
