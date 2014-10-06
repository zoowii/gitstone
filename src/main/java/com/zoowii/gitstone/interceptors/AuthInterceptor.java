package com.zoowii.gitstone.interceptors;

import com.zoowii.mvc.http.HttpContext;
import com.zoowii.mvc.http.IInterceptor;

/**
 * Created by zoowii on 14/10/6.
 */
public class AuthInterceptor implements IInterceptor {
    @Override
    public boolean beforeHandler(HttpContext ctx, Object handlerObj) {
        // TODO
        return true;
    }

    @Override
    public void afterHandler(HttpContext ctx, Object handlerObj) {
        // TODO
    }
}
