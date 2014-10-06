package com.zoowii.gitstone.git;

import clojure.lang.RT;
import clojure.lang.Var;
import com.zoowii.mvc.http.HttpContext;
import com.zoowii.mvc.http.HttpRequest;
import com.zoowii.mvc.http.HttpResponse;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

public class TestHandler {
    static class BusinessExecutor implements Runnable {
        private AsyncContext ctx;

        BusinessExecutor(AsyncContext ctx) {
            this.ctx = ctx;
        }

        public void run() {
            // 等待 3 秒钟，以模拟业务的执行
            try {
                Thread.sleep(3000);
                PrintWriter out = ctx.getResponse().getWriter();
                out.println("业务处理完成的时间：" + new Date() + "<br/>");
                out.flush();
                ctx.complete();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void async(HttpContext context) throws IOException {
        HttpRequest request = context.getRequest();
        HttpResponse response = context.getResponse();
        response.getHttpServletResponse().setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getHttpServletResponse().getWriter();
        out.println("进入 Servlet 的时间：" + new Date() + ".<br/>");
        out.flush();

        // 在子线程中执行业务调用，并由其负责输出响应，主线程退出
        AsyncContext ctx = request.startAsync();
        new Thread(new BusinessExecutor(ctx)).start();

        out.println("结束 Servlet 的时间：" + new Date() + "<br/>");
        out.flush();
    }

    public void clojureHi(HttpContext ctx) {
        try {
            RT.load("test/hello");
            Var reload = RT.var("test.hello", "reload-dummy");
            reload.invoke();
            Var sayHi = RT.var("test.hello", "say-hi");
            Object result = sayHi.invoke("zoowii");
            ctx.getResponse().append(result.toString());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void hello(HttpContext ctx) throws IOException {
        ctx.getResponse().getHttpServletResponse().setContentType("text/html;charset=utf-8");
        ctx.getResponse().getHttpServletResponse().setStatus(HttpServletResponse.SC_OK);
        ctx.getResponse().getHttpServletResponse().getWriter().println("<h1>Hello World ！" + ctx.getRequest().getParam("name") + ", I'm a java servlet </h1>");
    }
}
