package com.zoowii.gitstone.git;

import com.zoowii.mvc.http.HttpRequest;
import com.zoowii.mvc.http.HttpResponse;
import com.zoowii.util.FileUtil;
import com.zoowii.util.StringUtil;
import com.zoowii.gitstone.git.executors.GitRpcExecutor;
import com.zoowii.gitstone.git.executors.SendFileExecutor;
import com.zoowii.gitstone.git.executors.SendInfoRefsExecutor;

import javax.servlet.AsyncContext;
import java.io.*;
import java.util.logging.Logger;

public class GitHandler extends GitBaseHandler {
    private static final Logger logger = Logger.getLogger("GitHandler");

    private static final String UPLOAD_PACK = "upload-pack";
    private static final String RECEIVE_PACK = "receive-pack";
    private static final String PACKET_FLUSH = "0000";

    public static void cloneDummy(HttpRequest request, HttpResponse response) throws IOException {
        response.append("you should clone git repo using git client");
    }

    private static void sendFile(HttpRequest request, HttpResponse response, String repoPath, String path, String mimeType) throws IOException {
        final String absPath = repoPath + path;
        if (!FileUtil.isSubPath(absPath, repoPath)) {
            sendNoAccess(request, response);
            return;
        }
        if (!FileUtil.isReadable(absPath)) {
            sendNotFound(request, response);
            return;
        }
        if (!checkAuth(request, response, repoPath, READ_TYPE)) {
            sendAuthRequiredOrFailed(request, response);
            return;
        }
        response.setStatus(200);
        response.setContentType(mimeType);
        response.setHeader("Last-Modified", FileUtil.getLastModifiedDate(absPath).toString());
        AsyncContext asyncContext = request.startAsync();
        new Thread(new SendFileExecutor(asyncContext, absPath)).start();
        response.flushBuffer();
    }

    private static void sendTextFile(HttpRequest request, HttpResponse response, String repoPath, String path) throws IOException {
        sendFile(request, response, repoPath, path, PLAIN_TEXT_MIME_TYPE);
    }

    private static String getServiceType(HttpRequest request, HttpResponse response) throws IOException {
        String serviceType = request.getParameter("service");
        if (serviceType == null) {
            return null;
        }
        if (!serviceType.startsWith("git-")) {
            return null;
        }
        return serviceType.substring("git-".length());
    }

    public static void head(HttpRequest request, HttpResponse response) throws IOException {
        String repoPath = getGitRepoPath(request);
        if (!checkAuth(request, response, repoPath, READ_TYPE)) {
            sendAuthRequiredOrFailed(request, response);
            return;
        }
        logger.info("get head of " + repoPath);
        String path = "/HEAD";
        sendTextFile(request, response, repoPath, path);
    }

    /**
     * 当获取inf-refs方式不合法时,比如直接浏览器获取,则返回dummy消息
     */
    private static void dumbInfoRefs(HttpRequest request, HttpResponse response) throws IOException {
        //gitService.updateServerInfo();
        response.append("dumb client detected");
    }

    private static String packetWrite(String str) {
        int len = str.length();
        int l4 = len + 4;
        String l4Str = Integer.toHexString(l4);
        String l4StrRjust = StringUtil.rjust(l4Str, 4, "0");
        return l4StrRjust + str;
    }

    public static void infoRefs(HttpRequest request, HttpResponse response) throws IOException {
        String repoPath = getGitRepoPath(request);
        if (!checkAuth(request, response, repoPath, READ_TYPE)) {
            sendAuthRequiredOrFailed(request, response);
            return;
        }
        String serviceName = getServiceType(request, response);
        if (serviceName == null || !hasAccess(request, response, repoPath, serviceName, false)) {
            dumbInfoRefs(request, response);
            return;
        }
        String cmd = "git " + serviceName + " --stateless-rpc --advertise-refs " + repoPath;
        String prelude = "# service=git-" + serviceName;
        prelude = packetWrite(prelude) + "" + PACKET_FLUSH;
        try {
            InputStream processResultIn = gitService.command(cmd, repoPath, null);
            response.setStatus(200);
            response.setContentType("application/x-git-" + serviceName + "-advertisement");
            sendNoCache(request, response);
            AsyncContext asyncContext = request.startAsync();
            new Thread(new SendInfoRefsExecutor(asyncContext, processResultIn, prelude)).start();
            response.flushBuffer();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void idxFile(HttpRequest request, HttpResponse response) throws IOException {
        String repoPath = getGitRepoPath(request);
        if (!checkAuth(request, response, repoPath, READ_TYPE)) {
            sendAuthRequiredOrFailed(request, response);
            return;
        }
        String path = "/objects/pack/pack-" + request.getParam("path") + ".idx";
        sendCacheForever(request, response);
        sendFile(request, response, repoPath, path, "application/x-git-packed-objects-toc");
    }

    public static void packFile(HttpRequest request, HttpResponse response) throws IOException {
        String repoPath = getGitRepoPath(request);
        if (!checkAuth(request, response, repoPath, READ_TYPE)) {
            sendAuthRequiredOrFailed(request, response);
            return;
        }
        String path = "/objects/pack/pack-" + request.getParam("path") + ".pack";
        sendCacheForever(request, response);
        sendFile(request, response, repoPath, path, "application/x-git-packed-objects");
    }

    public static void infoPacks(HttpRequest request, HttpResponse response) throws IOException {
        String repoPath = getGitRepoPath(request);
        if (!checkAuth(request, response, repoPath, READ_TYPE)) {
            sendAuthRequiredOrFailed(request, response);
            return;
        }
        String path = "/objects/info/packs";
        sendNoCache(request, response);
        sendFile(request, response, repoPath, path, "text/plain; charset=utf-8");
    }

    public static void textInfo(HttpRequest request, HttpResponse response) throws IOException {
        String repoPath = getGitRepoPath(request);
        if (!checkAuth(request, response, repoPath, READ_TYPE)) {
            sendAuthRequiredOrFailed(request, response);
            return;
        }
        String path = "/objects/info/" + request.getParam("path");
        sendNoAccess(request, response);
        sendFile(request, response, repoPath, path, PLAIN_TEXT_MIME_TYPE);
    }

    public static void looseObject(HttpRequest request, HttpResponse response) throws IOException {
        String repoPath = getGitRepoPath(request);
        if (!checkAuth(request, response, repoPath, READ_TYPE)) {
            sendAuthRequiredOrFailed(request, response);
            return;
        }
        String path = "/objects/info/" + request.getParam("path");
        sendCacheForever(request, response);
        sendFile(request, response, repoPath, path, "application/x-git-loose-object");
    }

    private static void gitRpcService(HttpRequest request, HttpResponse response, String serviceName) throws IOException {
        String repoPath = getGitRepoPath(request);
        response.setStatus(200);
        response.setContentType("application/x-git-" + serviceName + "-result");
        AsyncContext asyncContext = request.startAsync();
        new Thread(new GitRpcExecutor(gitService, asyncContext, repoPath, serviceName)).start();
        response.flushBuffer();
    }

    public static void uploadPack(HttpRequest request, HttpResponse response) throws IOException {
        String repoPath = getGitRepoPath(request);
        if (!checkAuth(request, response, repoPath, READ_TYPE)) {
            sendAuthRequiredOrFailed(request, response);
            return;
        }
        gitRpcService(request, response, UPLOAD_PACK);
    }

    public static void receivePack(HttpRequest request, HttpResponse response) throws IOException {
        String repoPath = getGitRepoPath(request);
        if (!checkAuth(request, response, repoPath, WRITE_TYPE)) {
            sendAuthRequiredOrFailed(request, response);
            return;
        }
        gitRpcService(request, response, RECEIVE_PACK);
    }
}
