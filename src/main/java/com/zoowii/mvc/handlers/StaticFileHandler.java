package com.zoowii.mvc.handlers;

import com.zoowii.mvc.http.HttpRequest;
import com.zoowii.mvc.http.HttpResponse;
import com.zoowii.mvc.util.ResourceUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class StaticFileHandler extends AbstractHandler {
    public static void handleStaticFile(HttpRequest request, HttpResponse response) throws RouterNotFoundException, IOException {
        InputStream inputStream = ResourceUtil.readResourceInWar(request.getHttpServletRequest().getPathInfo());
        if (inputStream == null) {
            throw new RouterNotFoundException(request.getHttpServletRequest().getPathInfo());
        }
        // TODO: change to async or NIO, or just send by 1024bytes/4096bytes buffer
        byte[] bytes = new byte[inputStream.available()];
        inputStream.read(bytes, 0, inputStream.available());
        response.getHttpServletResponse().getOutputStream().write(bytes);
    }
}
