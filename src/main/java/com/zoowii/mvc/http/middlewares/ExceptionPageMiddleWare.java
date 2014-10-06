package com.zoowii.mvc.http.middlewares;

import com.zoowii.mvc.http.HttpContext;
import com.zoowii.mvc.http.IMiddleWare;

import java.util.concurrent.Callable;

/**
 * Created by zoowii on 14/10/6.
 */
public class ExceptionPageMiddleWare implements IMiddleWare {
    @Override
    public void call(HttpContext ctx, Object handlerObj, Callable callable) {
        try {
            callable.call();
        } catch (Exception e) {
            ctx.getResponse().safeAppend("Page Error: " + e.getMessage());
        }
    }
}
