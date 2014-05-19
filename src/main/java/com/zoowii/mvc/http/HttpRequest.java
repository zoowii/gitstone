package com.zoowii.mvc.http;

import javax.servlet.http.HttpServletRequest;

public class HttpRequest {
    private HttpServletRequest httpServletRequest;

    public HttpRequest(HttpServletRequest httpServletRequest) {
        this.httpServletRequest = httpServletRequest;
    }

    public HttpServletRequest getHttpServletRequest() {
        return httpServletRequest;
    }

    public void setHttpServletRequest(HttpServletRequest httpServletRequest) {
        this.httpServletRequest = httpServletRequest;
    }
}
