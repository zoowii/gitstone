package com.zoowii.mvc.http;

/**
 * Created by zoowii on 14/10/6.
 */
public class InterceptorHandler implements IHttpHandler {
    private IInterceptor interceptor;

    public InterceptorHandler(IInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    @Override
    public void process(HttpContext ctx, Object handlerObj, HttpHandlerChain chain) {
        boolean beforeProcessResult = interceptor.beforeHandler(ctx, handlerObj);
        if (beforeProcessResult) {
            chain.processNext(ctx, handlerObj);
            interceptor.afterHandler(ctx, handlerObj);
        }
    }
}
