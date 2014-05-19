package com.zoowii.mvc.handlers;

import com.zoowii.mvc.http.HttpRequest;
import com.zoowii.mvc.http.HttpResponse;

import java.io.IOException;

public class TestHandler extends AbstractHandler {
    public static void index(HttpRequest request, HttpResponse response) throws IOException {
        response.getHttpServletResponse().getWriter().append("hello, test mvc with jrebel");
    }

    public static void page404(HttpRequest request, HttpResponse response) {
        try {
            response.append("test 404 page");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
