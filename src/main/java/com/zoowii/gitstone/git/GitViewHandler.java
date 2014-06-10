package com.zoowii.gitstone.git;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import clojure.lang.RT;
import clojure.lang.Var;
import com.zoowii.mvc.http.HttpRequest;
import com.zoowii.mvc.http.HttpResponse;
import com.zoowii.util.FileUtil;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GitViewHandler extends GitBaseHandler {
    private static final Logger logger = Logger.getLogger(GitViewHandler.class.getName());

    public static void index(HttpRequest request, HttpResponse response) throws IOException {
        String repoPath = getGitRepoPath(request);
        String repoOwnerName = request.getStringParam("user");
        String repoName = request.getStringParam("repo");
        if (!canReadRepo(currentUsername(request), repoOwnerName, repoName)) {
            redirectToLogin(request, response);
            return;
        }
        Git git = gitService.getGitRepo(repoPath);
        if (git == null) {
            response.append("Can't find this git repo " + repoPath);
            return;
        }
        try {
            List<String> branchNames = gitService.getBranchNames(git);
            RT.load("gitstone/repo_dao");
            IFn getDefaultBranchFn = Clojure.var("gitstone.repo-dao", "default-branch-of-repo-by-name");
            if (getDefaultBranchFn == null) {
                response.append("Can't find default-branch-of-repo-by-name");
                return;
            }
            String defaultBranchName = (String) getDefaultBranchFn.invoke(repoOwnerName, repoName); // 默认分支
            if (defaultBranchName == null) {
                defaultBranchName = "master";
            }
            String branchName = defaultBranchName;
            if (branchNames == null || branchNames.size() < 1) {
                branchName = null;
            } else if (!branchNames.contains(defaultBranchName)) {
                branchName = branchNames.get(0);
            }
            String path = "";
            viewPathInGitRepo(request, response, git, branchName, path);
        } catch (GitAPIException e) {
            e.printStackTrace();
            response.append(e.getMessage());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            response.append(e.getMessage());
        }
    }

    private static void viewPathInGitRepo(HttpRequest request, HttpResponse response, Git git, String currentBranchName, String path) throws IOException {
        String username = request.getStringParam("user");
        String repoName = request.getStringParam("repo");
        try {
            RT.load("gitstone/git_views");
            Var viewPathFn = RT.var("gitstone.git-views", "view-path");
            Object res = viewPathFn.invoke(request, response, username, repoName, gitService, git, currentBranchName, path);
            response.setContentType("text/html; charset=UTF-8");
            if (res != null && res instanceof String) {
                response.append(res.toString());
            } else {
                if (res != null) {
                    logger.info(res.toString());
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void archiveRepo(HttpRequest request, HttpResponse response) throws IOException {
        String username = request.getStringParam("user");
        String repoName = request.getStringParam("repo");
        String branchName = request.getStringParam("branch", "master");
        String repoPath = getGitRepoPath(request);
        if (!canReadRepo(currentUsername(request), username, repoName)) {
            redirectToLogin(request, response);
            return;
        }
        Git git = gitService.getGitRepo(repoPath);
        if (git == null) {
            response.append("Can't find this git repo " + repoPath);
            return;
        }
        try {
            InputStream archiveInputStream = gitService.archiveGitRepo(git, branchName); // FIXME: branch name
            FileUtil.writeFullyStream(archiveInputStream, response.getOutputStream());
            archiveInputStream.close();
        } catch (GitAPIException e) {
            e.printStackTrace();
            response.sendError(501);
            response.append("archive failed");
        }
    }

    public static void deleteRepo(HttpRequest request, HttpResponse response) throws IOException {
        String username = request.getStringParam("user");
        String repoName = request.getStringParam("repo");
        if (!isOwnerOfRepo(currentUsername(request), username, repoName)) {
            response.ajaxFail("You are not the owner of this repository");
            return;
        }
        try {
            RT.load("gitstone/repo_dao");
            IFn fn = Clojure.var("gitstone.repo-dao", "del-repo-by-name!");
            if (fn == null) {
                response.ajaxFail("Can't find del-repo-by-name!");
                return;
            }
            fn.invoke(username, repoName);
            response.ajaxSuccess(null);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            response.ajaxFail(e.getMessage());
        }
    }

    public static void updateSettingsOptions(HttpRequest request, HttpResponse response) throws IOException {
        String username = request.getStringParam("user");
        String repoName = request.getStringParam("repo");
        if (!isOwnerOfRepo(currentUsername(request), username, repoName)) {
            response.ajaxFail("You are not the owner of this repository");
            return;
        }
        String description = request.getPostParam("description");
        String defaultBranch = request.getPostParam("default_branch");
        boolean isPrivate = request.getBoolPostParam("is_private", true);
        try {
            RT.load("gitstone/repo_dao");
            IFn fn = Clojure.var("gitstone.repo-dao", "update-repo-settings!");
            if (fn == null) {
                response.ajaxFail("Can't find update-repo-settings!");
                return;
            }
            fn.invoke(username, repoName, description, defaultBranch, isPrivate);
            response.ajaxSuccess("update repo settings successfully!");
        } catch (ClassNotFoundException e) {
            logger.log(Level.SEVERE, e.getMessage());
            e.printStackTrace();
            response.ajaxFail(e.getMessage());
        }
    }

    public static void settingsDangerZone(HttpRequest request, HttpResponse response) throws IOException {
        String username = request.getStringParam("user");
        String repoName = request.getStringParam("repo");
        if (!canReadRepo(currentUsername(request), username, repoName)) {
            redirectToLogin(request, response);
            return;
        }
        try {
            RT.load("gitstone/git_views");
            IFn fn = Clojure.var("gitstone.git-views", "view-repo-settings-danger-zone");
            if (fn == null) {
                response.append("error, load view-repo-settings-danger-zone error");
                return;
            }
            Object res = fn.invoke(request, response, username, repoName);
            response.setContentType("text/html; charset=UTF-8");
            handleClojureOutput(request, response, res);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            response.append("error " + e.getMessage());
        }
    }

    public static void settingsOptions(HttpRequest request, HttpResponse response) throws IOException {
        String username = request.getStringParam("user");
        String repoName = request.getStringParam("repo");
        String repoPath = getGitRepoPath(request);
        if (!canReadRepo(currentUsername(request), username, repoName)) {
            redirectToLogin(request, response);
            return;
        }
        Git git = gitService.getGitRepo(repoPath);
        if (git == null) {
            response.append("Can't find this git repo " + repoPath);
            return;
        }
        try {
            List<String> branchNames = gitService.getBranchNames(git);
            if (branchNames == null) {
                branchNames = new ArrayList<String>();
            }
            if (branchNames.size() < 1) {
                branchNames.add("master");
            }
            RT.load("gitstone/git_views");
            IFn fn = Clojure.var("gitstone.git-views", "view-repo-settings-options");
            if (fn == null) {
                response.append("error, load view-repo-settings-options error");
                return;
            }
            Object res = fn.invoke(request, response, username, repoName, branchNames);
            response.setContentType("text/html; charset=UTF-8");
            handleClojureOutput(request, response, res);
        } catch (GitAPIException e) {
            e.printStackTrace();
            response.append("error " + e.getMessage());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            response.append("error " + e.getMessage());
        }
    }

    public static void viewPath(HttpRequest request, HttpResponse response) throws IOException {
        String repoPath = getGitRepoPath(request);
        String path = request.getStringParam("path");
        if (path == null) {
            path = "";
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        String branchName = request.getStringParam("branch");
        String repoOwnerName = request.getStringParam("user");
        String repoName = request.getStringParam("repo");
        if (!canReadRepo(currentUsername(request), repoOwnerName, repoName)) {
            redirectToLogin(request, response);
            return;
        }
        Git git = gitService.getGitRepo(repoPath);
        if (git == null) {
            response.append("Can't find this git repo " + repoPath);
            return;
        }
        try {
            List<String> branchNames = gitService.getBranchNames(git);
            if (branchNames == null || branchNames.size() < 1) {
                response.append("this repo is empty");
                return;
            }
            if (!branchNames.contains(branchName)) {
                response.append("can't find branch " + branchName + " in repo " + repoPath);
                return;
            }
            viewPathInGitRepo(request, response, git, branchName, path);
        } catch (GitAPIException e) {
            e.printStackTrace();
            response.append("error " + e.getMessage());
        }
    }
}
