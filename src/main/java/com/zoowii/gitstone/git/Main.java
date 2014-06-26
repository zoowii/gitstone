package com.zoowii.gitstone.git;

import com.zoowii.mvc.DispatcherServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class Main {
    public static void main(String[] args) {
        Server server = new Server(8080);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        server.setHandler(context);
        context.addServlet(new ServletHolder(new DispatcherServlet()), "/*");
        try {
            server.start();
            System.out.println("server at http://localhost:8080/");
            server.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
