package git;

import com.zoowii.mvc.http.HttpRequest;
import com.zoowii.mvc.http.HttpResponse;
import com.zoowii.mvc.util.FileUtil;
import com.zoowii.mvc.util.StringUtil;
import git.executors.GitRpcExecutor;
import git.executors.SendFileExecutor;
import git.executors.SendInfoRefsExecutor;
import org.apache.commons.codec.binary.Base64;

import javax.servlet.AsyncContext;
import java.io.*;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GitHandler {
    private static final Logger logger = Logger.getLogger("GitHandler");
    private static final String baseGitRepoPath = "/Users/zoowii/test/git";
    private static final String PLAIN_TEXT_MIME_TYPE = "text/plain";
    private static final String UPLOAD_PACK = "upload-pack";
    private static final String RECEIVE_PACK = "receive_pack";
    private static final String PACKET_FLUSH = "0000";
    private static final String basicRealm = "gitstone";
    private static GitService gitService = new GitService();
    private static AbstractGitRepoAccessManager gitRepoAccessManager = new AllPublicGitRepoAccessManager();

    private static String getGitRepoPath(HttpRequest request) {
        String userName = request.getStringParam("user");
        String repoName = request.getStringParam("repo");
        return baseGitRepoPath + "/" + userName + "/" + repoName;
    }

    private static void sendNoAccess(HttpRequest request, HttpResponse response) throws IOException {
        response.setContentType(PLAIN_TEXT_MIME_TYPE);
        response.sendError(403, "Forbidden");
    }

    private static void sendNotFound(HttpRequest request, HttpResponse response) throws IOException {
        response.setContentType(PLAIN_TEXT_MIME_TYPE);
        response.sendError(404, "Not Found");
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

    private static final int READ_TYPE = 1;
    private static final int WRITE_TYPE = 2;
    private static final int READ_WRITE_TYPE = 1 | 2;

    private static boolean checkAuth(HttpRequest request, HttpResponse response, String repoPath, int accessType) throws IOException {
        // TODO: 如果是public的项目,直接返回true,否则要验证
        String authorization = request.getHeader("Authorization");
        if (authorization == null) {
            return false;
        }
        logger.info("author: " + authorization);
        try {
            String base64Decoded = new String(Base64.decodeBase64(authorization.split(" ")[1]), "UTF-8");
            String[] userPassArray = base64Decoded.split(":");
            if (userPassArray.length < 2) {
                return false;
            }
            String username = userPassArray[0];
            String password = userPassArray[1];
            if (READ_TYPE == accessType) {
                return gitRepoAccessManager.hasReadAccess(repoPath, username, password);
            } else if (WRITE_TYPE == accessType) {
                return gitRepoAccessManager.hasWriteAccess(repoPath, username, password);
            } else if (READ_WRITE_TYPE == accessType) {
                return gitRepoAccessManager.hasAllAccess(repoPath, username, password);
            } else {
                return false;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * TODO: 记录下认证次数,超过3次直接禁止
     */
    private static void sendAuthRequiredOrFailed(HttpRequest request, HttpResponse response) throws IOException {
        String msg = "Authorization needed to access this repository";
        response.setStatus(401);
        response.setHeader("WWW-Authenticate", "Basic realm=\"" + basicRealm + "\"");
        response.setContentType(PLAIN_TEXT_MIME_TYPE);
        response.append(msg);
        response.flushBuffer();
    }

    private static boolean hasAccess(HttpRequest request, HttpResponse response, String repoPath, String rpc, boolean checkContentType) {
        return true; // TODO, basic-auth check
    }

    private static void sendNoCache(HttpRequest request, HttpResponse response) throws IOException {
        response.setHeader("Expires", "Fri, 01 Jan 1980 00:00:00 GMT");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache, max-age=0, must-revalidate");
    }

    private static void sendCacheForever(HttpRequest request, HttpResponse response) throws IOException {
        Date now = new Date();
        response.setHeader("Date", now.toString());
        int seconds = 31536000;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(now);
        calendar.add(Calendar.SECOND, seconds);
        response.setHeader("Expires", calendar.getTime().toString());
        response.setHeader("Cache-Control", "public, max-age=31536000");
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
