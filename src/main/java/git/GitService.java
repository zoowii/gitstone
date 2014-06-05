package git;

import clojure.lang.RT;
import clojure.lang.Var;
import com.zoowii.mvc.util.Common;
import com.zoowii.mvc.util.FileUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.List;
import java.util.logging.Logger;

public class GitService {
    private static final Logger logger = Logger.getLogger("GitService");

    public InputStream command(String cmd, String dirPath, InputStream inputStream) throws IOException, InterruptedException {
        if (inputStream != null && inputStream.available() > 0) {
            try {
                RT.load("gitstone/git");
                Var execCmd = RT.var("gitstone.git", "exec-cmd");
                byte[] out = (byte[]) execCmd.invoke(cmd, dirPath, inputStream);
                return new ByteArrayInputStream(out);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.redirectOutput(ProcessBuilder.Redirect.PIPE);
            processBuilder.redirectError(ProcessBuilder.Redirect.PIPE);
            processBuilder.command(cmd.split(" "));
            processBuilder.directory(new File(dirPath));
            Process process = processBuilder.start();
            process.waitFor();
            return process.getInputStream();
        }
//        return process.getInputStream();
    }

    public void updateServerInfo(String dirPath) throws IOException, InterruptedException {
        String cmd = "update-server-info";
        command("git " + cmd, dirPath, null);
    }
}
