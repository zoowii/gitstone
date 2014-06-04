package com.zoowii.mvc.http;

import com.zoowii.mvc.util.Pair;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

public class HttpRequest {
    private HttpServletRequest httpServletRequest;
    private List<Pair<String, Object>> params = new ArrayList<Pair<String, Object>>();

    public List<Pair<String, Object>> getParams() {
        return params;
    }

    public Object getParam(String name) {
        for (Pair<String, Object> pair : params) {
            if (pair.getLeft().equals(name)) {
                return pair.getRight();
            }
        }
        return null;
    }

    public String getStringParam(String name) {
        Object val = getParam(name);
        return val != null ? val.toString() : null;
    }

    public void setParams(List<Pair<String, Object>> params) {
        this.params = params;
    }

    public HttpRequest(HttpServletRequest httpServletRequest) {
        this.httpServletRequest = httpServletRequest;
    }

    public HttpServletRequest getHttpServletRequest() {
        return httpServletRequest;
    }

    public void setHttpServletRequest(HttpServletRequest httpServletRequest) {
        this.httpServletRequest = httpServletRequest;
    }

    public AsyncContext startAsync() {
        return this.getHttpServletRequest().startAsync();
    }

    /**
     * get parameter from query string
     */
    public String getParameter(String name) {
        return getHttpServletRequest().getParameter(name);
    }
}
