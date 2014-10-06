package com.zoowii.mvc.http;

/**
 * Created by zoowii on 14/10/6.
 */
public interface IHttpHandler {
    public void process(HttpContext ctx, Object handlerObj, HttpHandlerChain chain);
}
