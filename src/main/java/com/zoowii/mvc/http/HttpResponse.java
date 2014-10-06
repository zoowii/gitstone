package com.zoowii.mvc.http;

import com.alibaba.fastjson.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

public class HttpResponse {
    private HttpServletResponse httpServletResponse;
    private boolean contentTypeSettled = false;

    public HttpResponse(HttpServletResponse httpServletResponse) {
        this.httpServletResponse = httpServletResponse;
    }

    public HttpServletResponse getHttpServletResponse() {
        return httpServletResponse;
    }

    public OutputStream getOutputStream() throws IOException {
        return getHttpServletResponse().getOutputStream();
    }

    public void append(CharSequence content) throws IOException {
        PrintWriter out = httpServletResponse.getWriter();
        out.append(content);
    }

    public void safeAppend(CharSequence content) {
        try {
            append(content);
        } catch (IOException e) {

        }
    }

    public void setContentType(String contentType) {
        this.getHttpServletResponse().setContentType(contentType);
        this.contentTypeSettled = true;
    }

    public boolean isContentTypeSettled() {
        return contentTypeSettled;
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

    public void redirect(String url) throws IOException {
        getHttpServletResponse().sendRedirect(url);
    }

    public void ajaxResponse(boolean success, Object data) throws IOException {
        this.setContentType("application/json; charset=UTF-8");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("success", success);
        jsonObject.put("data", data);
        this.append(jsonObject.toJSONString());
    }

    public void ajaxSuccess(Object data) throws IOException {
        ajaxResponse(true, data);
    }

    public void ajaxFail(Object data) throws IOException {
        ajaxResponse(false, data);
    }

}
