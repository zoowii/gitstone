package com.zoowii.gitstone.git;

import com.zoowii.gitstone.Settings;
import com.zoowii.mvc.handlers.AbstractHandler;
import com.zoowii.mvc.http.HttpRequest;
import com.zoowii.mvc.http.HttpResponse;
import org.apache.commons.codec.binary.Base64;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GitBaseHandler extends AbstractHandler {
    private static final Logger logger = Logger.getLogger(GitBaseHandler.class.getName());
    protected static final String baseGitRepoPath = Settings.getGitRootPath(); //"/Users/zoowii/test/git";
    protected static final String PLAIN_TEXT_MIME_TYPE = "text/plain";
    protected static final String basicRealm = Settings.getBasicRealm();
    protected static GitService gitService = GitService.getInstance();
    protected static AbstractGitRepoAccessManager gitRepoAccessManager = new AuthedGitRepoAccessManager();

    protected static void sendNoAccess(HttpRequest request, HttpResponse response) throws IOException {
        response.setContentType(PLAIN_TEXT_MIME_TYPE);
        response.sendError(403, "Forbidden");
    }

    protected static void sendNotFound(HttpRequest request, HttpResponse response) throws IOException {
        response.setContentType(PLAIN_TEXT_MIME_TYPE);
        response.sendError(404, "Not Found");
    }

    protected static String getGitRepoPath(HttpRequest request) {
        String userName = request.getStringParam("user");
        String repoName = request.getStringParam("repo");
        return baseGitRepoPath + "/" + userName + "/" + repoName;
    }

    protected static final int READ_TYPE = 1;
    protected static final int WRITE_TYPE = 2;
    protected static final int READ_WRITE_TYPE = 1 | 2;

    protected static boolean checkAuth(HttpRequest request, HttpResponse response, String repoPath, int accessType) throws IOException {
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
    protected static void sendAuthRequiredOrFailed(HttpRequest request, HttpResponse response) throws IOException {
        String msg = "Authorization needed to access this repository";
        response.setStatus(401);
        response.setHeader("WWW-Authenticate", "Basic realm=\"" + basicRealm + "\"");
        response.setContentType(PLAIN_TEXT_MIME_TYPE);
        response.append(msg);
        response.flushBuffer();
    }

    protected static boolean hasAccess(HttpRequest request, HttpResponse response, String repoPath, String rpc, boolean checkContentType) {
        return true; // TODO: 判断是否禁止某个git rpc操作,无论auth是否成功
    }

    protected static void sendNoCache(HttpRequest request, HttpResponse response) throws IOException {
        response.setHeader("Expires", "Fri, 01 Jan 1980 00:00:00 GMT");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache, max-age=0, must-revalidate");
    }

    protected static void sendCacheForever(HttpRequest request, HttpResponse response) throws IOException {
        Date now = new Date();
        response.setHeader("Date", now.toString());
        int seconds = 31536000;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(now);
        calendar.add(Calendar.SECOND, seconds);
        response.setHeader("Expires", calendar.getTime().toString());
        response.setHeader("Cache-Control", "public, max-age=31536000");
    }
}
