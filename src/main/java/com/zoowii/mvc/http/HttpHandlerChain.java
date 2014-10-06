package com.zoowii.mvc.http;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Created by zoowii on 14/10/6.
 */
public class HttpHandlerChain {
    private Iterator<IHttpHandler> handlerIterator;

    public HttpHandlerChain(Iterator<IHttpHandler> handlerIterator) {
        this.handlerIterator = handlerIterator;
    }

    public boolean hasNext() {
        return handlerIterator.hasNext();
    }

    public IHttpHandler next() {
        return handlerIterator.next();
    }

    public void processNext(HttpContext ctx, Object handlerObj) {
        if (hasNext()) {
            IHttpHandler nextHandler = next();
            nextHandler.process(ctx, handlerObj, this);
        }
    }
}
