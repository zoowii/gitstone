package com.zoowii.util;

import java.io.*;
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

    public static long writeFullyStream(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] bytes = new byte[8092];
        long totalSize = 0;
        while (inputStream != null && inputStream.available() > 0) {
            int size = inputStream.read(bytes);
            totalSize += size;
            outputStream.write(bytes, 0, size);
        }
        outputStream.flush();
        return totalSize;
    }

    public static byte[] readFullyInputStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        writeFullyStream(inputStream, byteArrayOutputStream);
        try {
            return byteArrayOutputStream.toByteArray();
        } finally {
            byteArrayOutputStream.close();
        }
    }

    public static String tryParseStreamToString(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return null;
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        writeFullyStream(inputStream, byteArrayOutputStream);
        if (byteArrayOutputStream.size() > 1024 * 1024) { // now max support 1MB file preview TODO: 改到配置中
            return null;
        }
        try {
            return new String(byteArrayOutputStream.toByteArray(), "UTF-8"); // TODO: utf8 supported only
        } catch (Exception e) {
            return null;
        }
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
