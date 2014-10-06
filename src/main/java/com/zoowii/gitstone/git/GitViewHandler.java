package com.zoowii.gitstone.git;

import com.zoowii.mvc.http.HttpRequest;
import com.zoowii.mvc.http.HttpResponse;
import com.zoowii.util.FileUtil;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

public class GitViewHandler extends GitBaseHandler {
    private static final Logger logger = Logger.getLogger(GitViewHandler.class.getName());

    public void archiveRepo() throws IOException {
        String username = request().getStringParam("user");
        String repoName = request().getStringParam("repo");
        String branchName = request().getStringParam("branch", "master");
        String repoPath = getGitRepoPath(request());
        if (!canReadRepo(currentUsername(request()), username, repoName)) {
            redirectToLogin(request(), response());
            return;
        }
        Git git = gitService.getGitRepo(repoPath);
        if (git == null) {
            response().append("Can't find this git repo " + repoPath);
            return;
        }
        try {
            InputStream archiveInputStream = gitService.archiveGitRepo(git, branchName); // FIXME: branch name
            FileUtil.writeFullyStream(archiveInputStream, response().getOutputStream());
            archiveInputStream.close();
        } catch (GitAPIException e) {
            e.printStackTrace();
            response().sendError(501);
            response().append("archive failed");
        }
    }

}
