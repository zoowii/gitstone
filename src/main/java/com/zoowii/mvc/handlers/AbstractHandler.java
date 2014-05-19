package com.zoowii.mvc.handlers;

import com.zoowii.mvc.http.HttpRequest;
import com.zoowii.mvc.http.HttpResponse;

public abstract class AbstractHandler {
    public static void redirect(String url, HttpRequest request, HttpResponse response, Object... params) {

    }

    public static void redirect(Class handlerClass, String handlerMethod, HttpRequest request, HttpResponse response, Object... params) {

    }
}
