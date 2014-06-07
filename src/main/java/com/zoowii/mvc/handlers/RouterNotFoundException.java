package com.zoowii.mvc.handlers;

public class RouterNotFoundException extends Exception {
    private String pathInfo;

    public RouterNotFoundException(String pathInfo) {
        this.pathInfo = pathInfo;
    }

    public String toString() {
        return "route path of " + pathInfo + " can't be found";
    }
}
