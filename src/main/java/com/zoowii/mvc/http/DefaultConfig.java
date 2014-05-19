package com.zoowii.mvc.http;

import com.zoowii.mvc.handlers.NotFoundRequestHandler;

public class DefaultConfig {
    public static HttpRouter.RouterHandlerInfo getDefaultWeb404HandlerInfo() {
        HttpRouter.RouterHandlerInfo handlerInfo = new HttpRouter.RouterHandlerInfo();
        handlerInfo.handlerClass = NotFoundRequestHandler.class;
        try {
            handlerInfo.handlerMethod = handlerInfo.handlerClass.getMethod("page404", HttpRequest.class, HttpResponse.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return handlerInfo;
    }
}
