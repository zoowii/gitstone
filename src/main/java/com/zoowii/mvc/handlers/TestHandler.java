package com.zoowii.mvc.handlers;

import com.zoowii.mvc.http.HttpRequest;
import com.zoowii.mvc.http.HttpResponse;

import java.io.IOException;

public class TestHandler extends AbstractHandler {
    public void index() throws IOException {
        response().getHttpServletResponse().getWriter().append("hello, test zoowii mvc");
    }

    public void page404() {
        try {
            response().append("test 404 page");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
