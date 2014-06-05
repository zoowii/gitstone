package git;

import com.zoowii.mvc.http.HttpRequest;
import com.zoowii.mvc.http.HttpResponse;
import com.zoowii.mvc.util.FileUtil;
import com.zoowii.mvc.util.StringUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import javax.servlet.AsyncContext;
import java.io.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

public class GitHandler {
    private static final Logger logger = Logger.getLogger("GitHandler");
    private static final String baseGitRepoPath = "/Users/zoowii/test/git";
    private static final String plainTextMimeType = "text/plain";
    private static final String UPLOAD_PACK = "upload-pack";
    private static final String RECEIVE_PACK = "receive_pack";
    private static final String PACKET_FLUSH = "0000";
    private static GitService gitService = new GitService();

    private static String getGitRepoPath(HttpRequest request) {
        String userName = request.getStringParam("user");
        String repoName = request.getStringParam("repo");
        return baseGitRepoPath + "/" + userName + "/" + repoName;
    }

    private static void sendNoAccess(HttpRequest request, HttpResponse response) throws IOException {
        response.setContentType(plainTextMimeType);
        response.sendError(403, "Forbidden");
    }

    private static void sendNotFound(HttpRequest request, HttpResponse response) throws IOException {
        response.setContentType(plainTextMimeType);
        response.sendError(404, "Not Found");
    }

    private static class GitRpcExecutor implements Runnable {
        private AsyncContext asyncContext;
        private String repoPath;
        private String gitCmd;

        public GitRpcExecutor(AsyncContext asyncContext, String repoPath, String gitCmd) {
            this.asyncContext = asyncContext;
            this.repoPath = repoPath;
            this.gitCmd = gitCmd;
        }

        @Override
        public void run() {
            String cmd = "git " + gitCmd + " --stateless-rpc " + repoPath;
            try {
                InputStream processResultIn = gitService.command(cmd, repoPath, asyncContext.getRequest().getInputStream());
                OutputStream outputStream = asyncContext.getResponse().getOutputStream();
                // TODO: set content-length
                byte[] bytes = new byte[8192 * 1024];
                while (processResultIn.available() > 0) {
                    int size = processResultIn.read(bytes);
                    asyncContext.getResponse().setContentLength(size);
                    outputStream.write(bytes, 0, size);
                }
                outputStream.flush();
                asyncContext.complete();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static class SendFileExecutor implements Runnable {
        private AsyncContext asyncContext;
        private String path;

        public SendFileExecutor(AsyncContext asyncContext, String path) {
            this.asyncContext = asyncContext;
            this.path = path;
        }

        @Override
        public void run() {
            File file = new File(path);
            OutputStream outputStream = null;
            try {
                outputStream = asyncContext.getResponse().getOutputStream();
                asyncContext.getResponse().setContentLength((int) FileUtils.sizeOf(file));
                InputStream inputStream = new FileInputStream(file);
                byte[] bytes = new byte[8192];
                while (inputStream.available() > 0) {
                    int size = inputStream.read(bytes);
                    outputStream.write(bytes, 0, size);
                }
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                asyncContext.complete();
            }
        }
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
        response.setStatus(200);
        response.setContentType(mimeType);
        response.setHeader("Last-Modified", FileUtil.getLastModifiedDate(absPath).toString());
        AsyncContext asyncContext = request.startAsync();
        new Thread(new SendFileExecutor(asyncContext, absPath)).start();
        response.flushBuffer();
    }

    private static void sendTextFile(HttpRequest request, HttpResponse response, String repoPath, String path) throws IOException {
        sendFile(request, response, repoPath, path, plainTextMimeType);
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

    private static boolean hasAccess(String rpc, boolean checkContentType) {
        return true; // TODO
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

    private static class SendInfoRefsExecutor implements Runnable {
        private AsyncContext asyncContext;
        private String prelude;
        private InputStream inputStream;

        public SendInfoRefsExecutor(AsyncContext asyncContext, InputStream inputStream, String prelude) {
            this.asyncContext = asyncContext;
            this.inputStream = inputStream;
            this.prelude = prelude;
        }

        @Override
        public void run() {
            try {
                // TODO: set content-length
                OutputStream outputStream = asyncContext.getResponse().getOutputStream();
                outputStream.write(prelude.getBytes());
                byte[] bytes = new byte[8192];
                while (inputStream.available() > 0) {
                    int size = inputStream.read(bytes);
                    // FIXME
                    asyncContext.getResponse().setContentLength(size + prelude.length());
                    outputStream.write(bytes, 0, size);
                }
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                asyncContext.complete();
            }
        }
    }

    public static void infoRefs(HttpRequest request, HttpResponse response) throws IOException {
        String repoPath = getGitRepoPath(request);
        logger.info("git info-refs of " + repoPath);
        String serviceName = getServiceType(request, response);
        logger.info("get info refs service name " + serviceName);
        if (serviceName == null || !hasAccess(serviceName, false)) {
            dumbInfoRefs(request, response);
            return;
        }
        String cmd = "git " + serviceName + " --stateless-rpc --advertise-refs " + repoPath;
        String prelude = "# service=git-" + serviceName;
        prelude = packetWrite(prelude) + "" + PACKET_FLUSH;
        logger.info(prelude);
        // TODO: send prelude
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
        logger.info("get idx file " + request.getParam("path"));
        String repoPath = getGitRepoPath(request);
        String path = "/objects/pack/pack-" + request.getParam("path") + ".idx";
        sendCacheForever(request, response);
        sendFile(request, response, repoPath, path, "application/x-git-packed-objects-toc");
    }

    public static void packFile(HttpRequest request, HttpResponse response) throws IOException {
        logger.info("get pack file " + request.getParam("path"));
        String repoPath = getGitRepoPath(request);
        String path = "/objects/pack/pack-" + request.getParam("path") + ".pack";
        sendCacheForever(request, response);
        sendFile(request, response, repoPath, path, "application/x-git-packed-objects");
    }

    public static void infoPacks(HttpRequest request, HttpResponse response) throws IOException {
        logger.info("get info packs ");
        String repoPath = getGitRepoPath(request);
        String path = "/objects/info/packs";
        sendNoCache(request, response);
        sendFile(request, response, repoPath, path, "text/plain; charset=utf-8");
    }

    public static void textInfo(HttpRequest request, HttpResponse response) throws IOException {
        logger.info("get text info " + request.getParam("path"));
        String repoPath = getGitRepoPath(request);
        String path = "/objects/info/" + request.getParam("path");
        sendNoAccess(request, response);
        sendFile(request, response, repoPath, path, plainTextMimeType);
    }

    public static void looseObject(HttpRequest request, HttpResponse response) throws IOException {
        String repoPath = getGitRepoPath(request);
        String path = "/objects/info/" + request.getParam("path");
        logger.info("get loose object " + path);
        sendCacheForever(request, response);
        sendFile(request, response, repoPath, path, "application/x-git-loose-object");
    }

    private static void gitRpcService(HttpRequest request, HttpResponse response, String serviceName) throws IOException {
        if (!hasAccess(serviceName, true)) {
            sendNoAccess(request, response);
            return;
        }
        String repoPath = getGitRepoPath(request);
        response.setStatus(200);
        response.setContentType("application/x-git-" + serviceName + "-result");
        AsyncContext asyncContext = request.startAsync();
        new Thread(new GitRpcExecutor(asyncContext, repoPath, serviceName)).start();
        response.flushBuffer();
    }

    public static void uploadPack(HttpRequest request, HttpResponse response) throws IOException {
        logger.info("git upload pack ");
        gitRpcService(request, response, "upload-pack");
    }

    public static void receivePack(HttpRequest request, HttpResponse response) throws IOException {
        logger.info("git receive pack ");
        gitRpcService(request, response, "receive-pack");
    }
}
