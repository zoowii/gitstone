package com.zoowii.gitstone.git.executors;

import com.zoowii.util.FileUtil;

import javax.servlet.AsyncContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SendInfoRefsExecutor implements Runnable {
    private AsyncContext asyncContext;
    private String prelude;
    private InputStream inputStream;

    public SendInfoRefsExecutor(AsyncContext asyncContext, InputStream inputStream, String prelude) {
        this.asyncContext = asyncContext;
        this.inputStream = inputStream;
        this.prelude = prelude;
    }

    @Override
    public void run() {
        try {
            OutputStream outputStream = asyncContext.getResponse().getOutputStream();
            outputStream.write(prelude.getBytes());
            long size = FileUtil.writeFullyStream(inputStream, outputStream);
            asyncContext.getResponse().setContentLengthLong(size + prelude.length());
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            asyncContext.complete();
        }
    }
}
