package git;

import com.zoowii.mvc.http.HttpRequest;
import com.zoowii.mvc.http.HttpResponse;
import com.zoowii.mvc.util.FileUtil;
import com.zoowii.mvc.util.StringUtil;
import org.apache.commons.io.FileUtils;

import javax.servlet.AsyncContext;
import java.io.*;
import java.util.Calendar;
import java.util.Date;

public class GitHandler {
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
//                TODO: 分词逐步发送文件内容
//                byte[] bytes = FileUtils.readFileToByteArray(file);
//                outputStream.write(bytes);
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
        String path = "/HEAD";
        sendTextFile(request, response, repoPath, path);
//        response.append("git HEAD of " + repoPath);
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

    private static void sendNoCacheForever(HttpRequest request, HttpResponse response) throws IOException {
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
//        response.append("git info-refs of " + repoPath);
        String serviceName = getServiceType(request, response);
        if (serviceName == null || !hasAccess(serviceName, false)) {
            dumbInfoRefs(request, response);
            return;
        }
        String cmd = "git " + serviceName + " --stateless-rpc --advertise-refs " + repoPath;
        // TODO: send prelude
        try {
            InputStream processResultIn = gitService.command(cmd, repoPath, null);
            response.setStatus(200);
            response.setContentType("application/x-git-" + serviceName + "-advertisement");
            sendNoCache(request, response);
            response.append(packetWrite("\"# service=git-" + serviceName + "\n"));
            response.append(PACKET_FLUSH);
            int c;
            while ((c = processResultIn.read()) != -1) {
                response.write(c);
            }
            response.flushBuffer();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void idxFile(HttpRequest request, HttpResponse response) throws IOException {
        response.append("get idx file " + request.getParam("path"));
    }

    public static void packFile(HttpRequest request, HttpResponse response) throws IOException {
        response.append("get pack file " + request.getParam("path"));
    }

    public static void infoPacks(HttpRequest request, HttpResponse response) throws IOException {
        response.append("get info packs ");
    }

    public static void textInfo(HttpRequest request, HttpResponse response) throws IOException {
        response.append("get text info " + request.getParam("path"));
    }

    public static void looseObject(HttpRequest request, HttpResponse response) throws IOException {
        response.append("get loose object " + request.getParam("path"));
    }

    public static void uploadPack(HttpRequest request, HttpResponse response) throws IOException {
        response.append("git upload pack ");
    }

    public static void receivePack(HttpRequest request, HttpResponse response) throws IOException {
        response.append("git receive pack ");
    }
}
