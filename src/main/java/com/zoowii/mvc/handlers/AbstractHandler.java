package com.zoowii.mvc.handlers;

import com.zoowii.mvc.http.HttpRequest;
import com.zoowii.mvc.http.HttpResponse;

import java.io.IOException;

public abstract class AbstractHandler {
    public static void redirect(String url, HttpRequest request, HttpResponse response) throws IOException {
        response.redirect(url);
    }
}
