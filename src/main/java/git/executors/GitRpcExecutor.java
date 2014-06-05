package git.executors;

import com.zoowii.mvc.util.FileUtil;
import git.GitService;

import javax.servlet.AsyncContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class GitRpcExecutor implements Runnable {
    private AsyncContext asyncContext;
    private String repoPath;
    private String gitCmd;
    private GitService gitService;

    public GitRpcExecutor(GitService gitService, AsyncContext asyncContext, String repoPath, String gitCmd) {
        this.asyncContext = asyncContext;
        this.repoPath = repoPath;
        this.gitCmd = gitCmd;
        this.gitService = gitService;
    }

    @Override
    public void run() {
        String cmd = "git " + gitCmd + " --stateless-rpc " + repoPath;
        try {
            InputStream processResultIn = gitService.command(cmd, repoPath, asyncContext.getRequest().getInputStream());
            OutputStream outputStream = asyncContext.getResponse().getOutputStream();
            long size = FileUtil.writeFullyStream(processResultIn, asyncContext.getResponse().getOutputStream());
            outputStream.flush();
            asyncContext.getResponse().setContentLengthLong(size);
            asyncContext.complete();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
