package com.zoowii.mvc.http.handler_callers;

import clojure.lang.IPersistentMap;
import clojure.lang.Keyword;
import clojure.lang.MapEntry;
import com.zoowii.mvc.http.HttpContext;
import com.zoowii.mvc.http.HttpHandlerChain;
import com.zoowii.mvc.http.HttpResponse;
import com.zoowii.mvc.http.IHttpHandler;
import com.zoowii.util.ClojureUtil;
import com.zoowii.util.FileUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by zoowii on 14/10/6.
 */
public class RingResponseCaller extends HandlerCaller {

    @Override
    protected IHttpHandler makeHandler() throws Exception {
        return new IHttpHandler() {
            @Override
            public void process(HttpContext ctx, Object handlerObj, HttpHandlerChain chain) {
                try {
                    // 如果是一个Clojure的map,这个就表示返回的内容
                    // 格式类似Ring, 为{:status: ..., :headers: ..., :body: str or byte[] or InputStream, :content-type: ...}
                    // :headers的格式为{:header-name or "header-name": value, ... }
                    IPersistentMap map = (IPersistentMap) handlerObj;
                    Object statusObj = map.valAt(Keyword.intern("status"));
                    HttpResponse response = ctx.getResponse();
                    if (statusObj != null) {
                        if (statusObj instanceof Long) {
                            response.setStatus(((Long) statusObj).intValue());
                        } else if (statusObj instanceof Integer) {
                            response.setStatus((Integer) statusObj);
                        }
                    }
                    Object contentTypeObj = map.valAt(Keyword.intern("content-type"));
                    if (contentTypeObj != null && contentTypeObj instanceof String) {
                        response.setContentType((String) contentTypeObj);
                    }
                    Object headersObj = map.valAt(Keyword.intern("headers"));
                    if (headersObj != null && headersObj instanceof IPersistentMap) {
                        IPersistentMap headersMap = (IPersistentMap) headersObj;
                        for (Object entryObj : headersMap) {
                            MapEntry entry = (MapEntry) entryObj;
                            Object keyObj = entry.getKey();
                            Object valueObj = entry.getValue();
                            String key = ClojureUtil.name(keyObj);
                            String value = ClojureUtil.name(valueObj);
                            response.setHeader(key, value);
                        }
                    }
                    Object bodyObj = map.valAt(Keyword.intern("body"));
                    if (bodyObj != null) {
                        if (bodyObj instanceof String) {
                            response.append((String) bodyObj);
                        } else if (bodyObj instanceof InputStream) {
                            FileUtil.writeFullyStream((InputStream) bodyObj, response.getOutputStream());
                        } else if (bodyObj instanceof byte[]) {
                            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream((byte[]) bodyObj);
                            FileUtil.writeFullyStream(byteArrayInputStream, response.getOutputStream());
                        }
                    }
                    chain.processNext(ctx, handlerObj);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
}
