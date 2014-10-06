package com.zoowii.gitstone.interceptors;

import com.zoowii.mvc.http.HttpContext;
import com.zoowii.mvc.http.IInterceptor;

import java.util.logging.Logger;

/**
 * Created by zoowii on 14/10/6.
 */
public class RequestLogInterceptor implements IInterceptor {
    private static final Logger logger = Logger.getLogger(RequestLogInterceptor.class.getName());

    @Override
    public boolean beforeHandler(HttpContext ctx, Object handlerObj) {
        logger.info("before handler");
        return true;
    }

    @Override
    public void afterHandler(HttpContext ctx, Object handlerObj) {
        logger.info("after handler");
    }
}
