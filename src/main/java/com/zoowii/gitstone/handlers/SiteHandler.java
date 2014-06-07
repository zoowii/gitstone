package com.zoowii.gitstone.handlers;

import clojure.lang.RT;
import clojure.lang.Var;
import com.zoowii.mvc.handlers.AbstractHandler;
import com.zoowii.mvc.http.HttpRequest;
import com.zoowii.mvc.http.HttpResponse;
import com.zoowii.mvc.http.HttpRouter;

import java.io.IOException;

public class SiteHandler extends BaseHandler {
    public static void index(HttpRequest request, HttpResponse response) throws IOException {
        if (currentUsername(request) == null) {
            response.redirect(HttpRouter.reverseUrl("login_page"));
            return;
        }
        try {
            RT.load("gitstone/views");
            Var indexPage = RT.var("gitstone.views", "index-page");
            Object res = indexPage.invoke(request, response);
            if (res != null) {
                response.append(res.toString());
            } else {
                response.append("error");
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void createRepoPage(HttpRequest request, HttpResponse response) throws IOException {
        if (currentUsername(request) == null) {
            response.redirect(HttpRouter.reverseUrl("login_page"));
            return;
        }
        try {
            RT.load("gitstone/views");
            Var indexPage = RT.var("gitstone.views", "new-repo-page");
            Object res = indexPage.invoke(request, response);
            if (res != null) {
                response.append(res.toString());
            } else {
                response.append("error");
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void createRepo(HttpRequest request, HttpResponse response) throws IOException {
        if (currentUsername(request) == null) {
            response.redirect(HttpRouter.reverseUrl("login_page"));
            return;
        }
        try {
            RT.load("gitstone/views");
            Var indexPage = RT.var("gitstone.views", "create-repo");
            indexPage.invoke(request, response);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void loginPage(HttpRequest request, HttpResponse response) throws IOException {
        try {
            RT.load("gitstone/views");
            Var loginPage = RT.var("gitstone.views", "login-page");
            Object res = loginPage.invoke(request, response);
            response.setContentType("text/html; charset=UTF-8");
            if (res != null) {
                response.append(res.toString());
            } else {
                response.append("error");
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void login(HttpRequest request, HttpResponse response) throws IOException {
        try {
            RT.load("gitstone/views");
            Var login = RT.var("gitstone.views", "login");
            login.invoke(request, response);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void logout(HttpRequest request, HttpResponse response) throws IOException {
        request.clearSession();
        redirect(HttpRouter.reverseUrl("index"), request, response);
    }

    public static void page404(HttpRequest request, HttpResponse response) throws IOException {
        response.append("404");
    }
}
