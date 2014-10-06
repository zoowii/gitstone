package com.zoowii.mvc.http.handler_callers;

import com.google.common.base.Function;
import com.zoowii.mvc.http.*;
import com.zoowii.util.ListUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Created by zoowii on 14/10/6.
 */
public abstract class HandlerCaller {
    public void process(final HttpContext ctx, final Object handlerObj) throws Exception {
        // 构造handler-chain,然后调用
        IHttpHandler mainHandler = makeHandler(); // TODO: cache it for every HandlerCaller class
        List<IHttpHandler> handlers = new ArrayList<IHttpHandler>();
        handlers.add(mainHandler);
        handlers.addAll(ListUtil.map(HttpRouter.getInterceptors(), new Function<IInterceptor, IHttpHandler>() {
            @Override
            public IHttpHandler apply(IInterceptor interceptor) {
                return new InterceptorHandler(interceptor);
            }
        }));
        handlers = ListUtil.reverse(handlers);
        final HttpHandlerChain chain = new HttpHandlerChain(handlers.iterator());
        Callable chainCallable = new Callable() {
            @Override
            public Object call() throws Exception {
                chain.processNext(ctx, handlerObj);
                return null;
            }
        };
        IMiddleWare finalMiddleWare = HttpRouter.mergeMiddleWares();
        finalMiddleWare.call(ctx, handlerObj, chainCallable);
    }

    protected abstract IHttpHandler makeHandler() throws Exception;
}
