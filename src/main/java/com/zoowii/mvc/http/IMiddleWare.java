package com.zoowii.mvc.http;

import java.util.concurrent.Callable;

/**
 * Created by zoowii on 14/10/6.
 */
public interface IMiddleWare {
    public void call(HttpContext ctx, Object handlerObj, Callable callable);
}
