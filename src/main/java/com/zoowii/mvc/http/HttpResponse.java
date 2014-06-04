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

    public void setContentType(String contentType) {
        this.getHttpServletResponse().setContentType(contentType);
    }

    public void sendError(int code, String msg) throws IOException {
        this.getHttpServletResponse().sendError(code, msg);
    }

    public void sendError(int code) throws IOException {
        this.getHttpServletResponse().sendError(code);
    }

    public void setStatus(int code) throws IOException {
        this.getHttpServletResponse().setStatus(code);
    }

    public void setHeader(String name, String value) {
        this.getHttpServletResponse().setHeader(name, value);
    }

    public void flushBuffer() throws IOException {
        this.getHttpServletResponse().flushBuffer();
    }

    public PrintWriter getWriter() throws IOException {
        return this.getHttpServletResponse().getWriter();
    }

    public void write(int c) throws IOException {
        this.getHttpServletResponse().getWriter().write(c);
    }

}
