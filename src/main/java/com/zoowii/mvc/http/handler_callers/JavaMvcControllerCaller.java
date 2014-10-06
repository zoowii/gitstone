package com.zoowii.mvc.http.handler_callers;

import com.zoowii.mvc.handlers.AbstractHandler;
import com.zoowii.mvc.handlers.RouterNotFoundException;
import com.zoowii.mvc.http.*;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Created by zoowii on 14/10/6.
 */
public class JavaMvcControllerCaller extends HandlerCaller {
    @Override
    protected IHttpHandler makeHandler() throws Exception {
        return new IHttpHandler() {
            @Override
            public void process(HttpContext ctx, Object handlerObj, HttpHandlerChain chain) {
                try {
                    // 如果是一个seq,则代表是[类, 静态方法]的形式,把请求调度过去
                    List handlerArray = (List) handlerObj;
                    if (handlerArray.size() < 2) {
                        throw new RouterNotFoundException("If you use Java method as handler, you need to specify the class and static method");
                    }
                    Class handlerClass = (Class) handlerArray.get(0);
                    Object handler = handlerClass.newInstance();
                    Object[] methodParams;
                    // TODO: 根据对象的属性,方法的参数,是否static等信息注入属性和参数
                    if (handler instanceof AbstractHandler) {
                        AbstractHandler handler1 = (AbstractHandler) handler;
                        handler1.setContext(ctx);
                        methodParams = new Object[0];
                    } else {
                        methodParams = new Object[1];
                    }
                    String methodName = (String) handlerArray.get(1);
                    Method handlerMethod = handlerClass.getDeclaredMethod(methodName);
                    handlerMethod.invoke(handler, methodParams);
                    chain.processNext(ctx, handlerObj);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
}
