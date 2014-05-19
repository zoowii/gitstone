package com.zoowii.mvc.http;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class HttpResponse {
    private HttpServletResponse httpServletResponse;

    public HttpResponse(HttpServletResponse httpServletResponse) {
        this.httpServletResponse = httpServletResponse;
    }

    public HttpServletResponse getHttpServletResponse() {
        return httpServletResponse;
    }

    public void append(CharSequence content) throws IOException {
        PrintWriter out = httpServletResponse.getWriter();
        out.append(content);
    }
}
