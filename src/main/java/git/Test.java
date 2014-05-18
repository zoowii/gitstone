package git;

import clojure.lang.RT;
import clojure.lang.Var;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

public class Test {
    static class DefaultHandler extends AbstractHandler {
        @Override
        public void handle(String s, Request baseRequest,
                           HttpServletRequest request,
                           HttpServletResponse response) throws IOException {
            response.setContentType("text/html;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_OK);
            baseRequest.setHandled(true);
            response.getWriter().println("<h1>Hello World!</h1>");
        }
    }

    static class HelloServlet extends HttpServlet {
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
            response.setContentType("text/html;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().println("<h1>Hello World ！i'm groovy servlet </h1>");
        }
    }

    static class AsyncTestServlet extends HttpServlet {
        protected void doGet(HttpServletRequest request, HttpServletResponse response)
                throws ServletException, IOException {
            response.setContentType("text/html;charset=UTF-8");
            PrintWriter out = response.getWriter();
            out.println("进入 Servlet 的时间：" + new Date() + ".<br/>");
            out.flush();

            // 在子线程中执行业务调用，并由其负责输出响应，主线程退出
            AsyncContext ctx = request.startAsync();
            new Thread(new BusinessExecutor(ctx)).start();

            out.println("结束 Servlet 的时间：${new Date()}.<br/>");
            out.flush();
        }
    }

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
                out.println("业务处理完成的时间：${new Date()}.<br/>");
                out.flush();
                ctx.complete();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static class ClojureTestHandler extends HttpServlet {
        protected void doGet(HttpServletRequest request, HttpServletResponse response)
                throws ServletException, IOException {
            try {
                RT.load("test/hello");
                Var sayHi = RT.var("test.hello", "say-hi");
                Object result = sayHi.invoke("zoowii");
                response.getWriter().append(result.toString());
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }


    public static void main(String[] args) {
        Server server = new Server(8080);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        server.setHandler(context);
        context.addServlet(new ServletHolder(new AsyncTestServlet()), "/async");
        context.addServlet(new ServletHolder(new HelloServlet()), "/hello");
        context.addServlet(new ServletHolder(new ClojureTestHandler()), "/clojure");
        context.addServlet(new ServletHolder(new GitDispatcher()), "/git/.*");
        context.addServlet(new ServletHolder(new GitHeaderHandler()), "/git/(.+?)/(.+?)/HEAD");
        try {
            server.start();
            server.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
