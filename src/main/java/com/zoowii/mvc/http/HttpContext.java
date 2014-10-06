package com.zoowii.mvc.http;

import java.nio.channels.Channel;

public class HttpContext {
    private HttpRequest request;
    private HttpResponse response;

    public HttpContext(HttpRequest request, HttpResponse response) {
        this.request = request;
        this.response = response;
    }

    public HttpRequest getRequest() {
        return request;
    }

    public void setRequest(HttpRequest request) {
        this.request = request;
    }

    public HttpResponse getResponse() {
        return response;
    }

    public void setResponse(HttpResponse response) {
        this.response = response;
    }

    public Channel channel() {
        // TODO
        throw new UnsupportedOperationException();
    }
}
