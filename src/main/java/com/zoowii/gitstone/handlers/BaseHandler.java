package com.zoowii.gitstone.handlers;

import com.alibaba.fastjson.JSONObject;
import com.zoowii.gitstone.models.Account;
import com.zoowii.mvc.handlers.AbstractHandler;
import com.zoowii.mvc.http.HttpRequest;
import com.zoowii.mvc.http.HttpResponse;
import com.zoowii.mvc.http.HttpRouter;

import java.io.IOException;

public abstract class BaseHandler extends AbstractHandler {
    protected static String currentUsername(HttpRequest request) {
        return (String) request.session("username");
    }

    protected static void loginAsUser(HttpRequest request, Account user) {
        if (user != null) {
            request.session("username", user.getUsername());
        }
    }

    protected static void logoutOfAll(HttpRequest request) {
        request.clearSession();
    }

    protected static void redirectToLogin(HttpRequest request, HttpResponse response) throws IOException {
        response.redirect(HttpRouter.reverseUrl("login_page"));
    }

    /**
     * TODO: 处理调用Clojure函数处理请求的输出,按照{status: ..., content: ..., content-type: ... headers: ...}来处理请求
     *
     * @throws IOException
     */
    protected static void handleClojureOutput(HttpRequest request, HttpResponse response, Object out) throws IOException {
        if (out != null) {
            response.append(out.toString());
        } else {
            response.append("error");
        }
    }

}
