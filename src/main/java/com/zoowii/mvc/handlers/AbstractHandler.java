package com.zoowii.mvc.handlers;

import com.zoowii.mvc.http.HttpContext;
import com.zoowii.mvc.http.HttpRequest;
import com.zoowii.mvc.http.HttpResponse;

import java.io.IOException;

public abstract class AbstractHandler {
    protected HttpContext context;

    public HttpContext getContext() {
        return context;
    }

    public HttpRequest request() {
        return context.getRequest();
    }

    public HttpResponse response() {
        return context.getResponse();
    }

    public void setContext(HttpContext context) {
        this.context = context;
    }

    public static void redirect(String url, HttpRequest request, HttpResponse response) throws IOException {
        response.redirect(url);
    }
}
