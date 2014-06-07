package com.zoowii.mvc.handlers;

import com.zoowii.mvc.http.HttpRequest;
import com.zoowii.mvc.http.HttpResponse;
import com.zoowii.util.FileUtil;
import com.zoowii.util.ResourceUtil;

import java.io.IOException;
import java.io.InputStream;

public class StaticFileHandler extends AbstractHandler {
    private static final String staticDir = "static"; // TODO: 做成可配置的

    public static void handleStaticFile(HttpRequest request, HttpResponse response) throws RouterNotFoundException, IOException {
        String path = request.getStringParam("path");
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        path = staticDir + "/" + path;
        InputStream inputStream = ResourceUtil.readResourceInWar(path);
        if (inputStream == null) {
            response.append("Can't find static file " + request.getHttpServletRequest().getPathInfo());
            throw new RouterNotFoundException(request.getHttpServletRequest().getPathInfo());
        }
        FileUtil.writeFullyStream(inputStream, response.getOutputStream());
//        TODO: change to async or NIO, or just send by 1024bytes/4096bytes buffer
//        byte[] bytes = new byte[inputStream.available()];
//        inputStream.read(bytes, 0, inputStream.available());
//        response.getHttpServletResponse().getOutputStream().write(bytes);
    }
}
