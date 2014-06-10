package com.zoowii.gitstone.git;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import clojure.lang.RT;
import com.zoowii.gitstone.Settings;
import com.zoowii.gitstone.handlers.BaseHandler;
import com.zoowii.mvc.handlers.AbstractHandler;
import com.zoowii.mvc.http.HttpRequest;
import com.zoowii.mvc.http.HttpResponse;
import com.zoowii.util.StringUtil;
import org.apache.commons.codec.binary.Base64;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GitBaseHandler extends BaseHandler {
    private static final Logger logger = Logger.getLogger(GitBaseHandler.class.getName());
    protected static final String baseGitRepoPath = Settings.getGitRootPath();
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
    protected static final int ADMIN_TYPE = 4;
    protected static final int ALL_TYPE = 1 | 2;

    protected static boolean checkBasicAuth(HttpRequest request, HttpResponse response, String repoPath, int accessType) throws IOException {
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
            // FIXME
            if (READ_TYPE == accessType) {
                return gitRepoAccessManager.hasReadAccess(repoPath, username, password);
            } else if (WRITE_TYPE == accessType) {
                return gitRepoAccessManager.hasWriteAccess(repoPath, username, password);
            } else if (ALL_TYPE == accessType) {
                return gitRepoAccessManager.hasAllAccess(repoPath, username, password);
            } else if (ADMIN_TYPE == accessType) {
                return gitRepoAccessManager.hasAdminAccess(repoPath, username, password);
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

    protected static boolean canWriteRepo(String currentUserName, String repoUserName, String repoName) {
        return false; // TODO
    }

    protected static boolean canAdminRepo(String currentUserName, String repoUserName, String repoName) {
        return false; // TODO
    }

    protected static boolean isOwnerOfRepo(String currentUserName, String repoUserName, String repoName) {
        return StringUtil.eq(currentUserName, repoUserName); // TODO: 改成只要是repo的管理员就可以管理,目前是只有拥有者可以管理
    }

    protected static boolean canReadRepo(String currentUserName, String repoUserName, String repoName) {
        try {
            RT.load("gitstone/user_dao");
            IFn fn = Clojure.var("gitstone.user-dao", "can-read-repo?");
            if (fn == null) {
                logger.info("Can't find can-read-repo?");
                return false;
            }
            Object res = fn.invoke(currentUserName, repoUserName, repoName);
            return res != null && res.equals(true);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }
}
