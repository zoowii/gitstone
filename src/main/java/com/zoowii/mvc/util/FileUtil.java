package com.zoowii.mvc.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

public class FileUtil {
    public static boolean isSubPath(String subPath, String parentPath) {
        File subFile = new File(subPath);
        File parentFile = new File(parentPath);
        return subFile.getAbsolutePath().startsWith(parentFile.getAbsolutePath());
    }

    public static boolean exists(String path) {
        File file = new File(path);
        return file.exists();
    }

    public static boolean isExistFile(String path) {
        if (!exists(path)) {
            return false;
        }
        File file = new File(path);
        return file.isFile();
    }

    public static boolean isReadable(String path) {
        if (!isExistFile(path)) {
            return false;
        }
        File file = new File(path);
        return file.canRead();
    }

    public static Date getLastModifiedDate(String path) {
        if (!isReadable(path)) {
            return null;
        }
        File file = new File(path);
        return new Date(file.lastModified());
    }

    public static class PipeWriteExecutor implements Runnable {
        private InputStream inputStream;
        private OutputStream outputStream;

        public PipeWriteExecutor(InputStream inputStream, OutputStream outputStream) {
            this.inputStream = inputStream;
            this.outputStream = outputStream;
        }

        @Override
        public void run() {
            int c;
            try {
                while ((c = inputStream.read()) != -1) {
                    outputStream.write(c);
                }
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
