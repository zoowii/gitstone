package git;

import com.zoowii.mvc.http.HttpRequest;
import com.zoowii.mvc.http.HttpResponse;

import java.io.IOException;

public class GitHandler {
    private static final String baseGitRepoPath = "~/test/git";

    private static String getGitRepoPath(HttpRequest request) {
        String userName = request.getStringParam("user");
        String repoName = request.getStringParam("repo");
        return baseGitRepoPath + "/" + userName + "/" + repoName;
    }

    public static void head(HttpRequest request, HttpResponse response) throws IOException {
        String repoPath = getGitRepoPath(request);
        response.append("git HEAD of " + repoPath);
    }

    public static void infoRefs(HttpRequest request, HttpResponse response) throws IOException {
        String repoPath = getGitRepoPath(request);
        response.append("git info-refs of " + repoPath);
    }
}
