package git.executors;

import com.zoowii.mvc.util.FileUtil;

import javax.servlet.AsyncContext;
import java.io.*;

public class SendFileExecutor implements Runnable {
    private AsyncContext asyncContext;
    private String path;

    public SendFileExecutor(AsyncContext asyncContext, String path) {
        this.asyncContext = asyncContext;
        this.path = path;
    }

    @Override
    public void run() {
        File file = new File(path);
        try {
            OutputStream outputStream = asyncContext.getResponse().getOutputStream();
            InputStream inputStream = new FileInputStream(file);
            long size = FileUtil.writeFullyStream(inputStream, outputStream);
            asyncContext.getResponse().setContentLengthLong(size);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            asyncContext.complete();
        }
    }
}
