package git;

import com.zoowii.mvc.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class GitService {
    public InputStream command(String cmd, String dirPath, InputStream input) throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec(cmd, new String[]{}, new File(dirPath));
        if (input != null && input.available() > 0) {
            new Thread(new FileUtil.PipeWriteExecutor(input, process.getOutputStream())).start();
        }
        process.waitFor();
        return process.getInputStream();
    }

    public void updateServerInfo(String dirPath) throws IOException, InterruptedException {
        String cmd = "update-server-info";
        command("git " + cmd, dirPath, null);
    }
}
