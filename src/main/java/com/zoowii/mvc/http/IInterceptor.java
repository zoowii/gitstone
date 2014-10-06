package com.zoowii.mvc.http;

/**
 * Created by zoowii on 14/10/6.
 */
public interface IInterceptor {
    public boolean beforeHandler(HttpContext ctx, Object handlerObj);

    public void afterHandler(HttpContext ctx, Object handlerObj);
}
