package com.zoowii.mvc.handlers;

import com.zoowii.mvc.http.HttpRequest;
import com.zoowii.mvc.http.HttpResponse;

import java.io.IOException;

public class NotFoundRequestHandler {
    public static void page404(HttpRequest request, HttpResponse response) {
        try {
            response.append("default 404 page");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
