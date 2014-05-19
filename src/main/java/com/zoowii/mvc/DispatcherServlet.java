package com.zoowii.mvc;

import com.zoowii.mvc.handlers.NotFoundRequestHandler;
import com.zoowii.mvc.handlers.RouterNotFoundException;
import com.zoowii.mvc.http.HttpRequest;
import com.zoowii.mvc.http.HttpResponse;
import com.zoowii.mvc.http.HttpRouter;
import com.zoowii.mvc.util.RouterLoader;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class DispatcherServlet extends HttpServlet {
    @Override
    public void service(HttpServletRequest request,
                        HttpServletResponse response) throws ServletException, IOException {
        HttpRouter.clearRouteTableCache(); // debug
        RouterLoader.loadRouterFromFile("routes");
        HttpRequest req = new HttpRequest(request);
        HttpResponse res = new HttpResponse(response);
        HttpRouter.submitRequest(req, res);
    }
}
