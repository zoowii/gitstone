package com.zoowii.gitstone.handlers;

import com.zoowii.gitstone.models.Account;
import com.zoowii.mvc.handlers.AbstractHandler;
import com.zoowii.mvc.http.HttpRequest;

public abstract class BaseHandler extends AbstractHandler {
    public static String currentUsername(HttpRequest request) {
        return (String) request.session("username");
    }

    public static void loginAsUser(HttpRequest request, Account user) {
        if (user != null) {
            request.session("username", user.getUsername());
        }
    }

    public static void logoutOfAll(HttpRequest request) {
        request.clearSession();
    }
}
