package com.zoowii.gitstone;

import com.zoowii.util.ResourceUtil;

import java.io.File;
import java.util.Date;
import java.util.logging.Logger;

public class Settings {
    private static final Logger logger = Logger.getLogger(Settings.class.getName());

    public static String getGitRootPath() {
        String path = System.getProperty("user.home") + "/gitstone";
        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }
        if (file.isFile()) {
            logger.info("Path of " + path + " is not a directory");
            return null; // TODO: throw IOException
        }
        return path;
    }

    public static String getUserGitRootPath(String username) {
        return getGitRootPath() + "/" + username;
    }

    public static String getRepoPath(String username, String repoName) {
        return getUserGitRootPath(username) + "/" + repoName;
    }

    public static String getBasicRealm() {
        return "gitstone";
    }

    public static String getDbFilePath() {
        return getGitRootPath() + "/gitstone.db";
    }

    public static String getTrashPath() {
        String path = ResourceUtil.getPropertyOrEnv("TRASH_HOME", getGitRootPath() + "/_trash");
        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }
        return path;
    }

    public static String getTrashPathForRepo(String ownerName, String repoName) {
        String dirName = ownerName + "_" + repoName + "_" + new Date().getTime();
        return getTrashPath() + "/" + dirName;
    }

    public static String getDatabaseUrl() {
        return ResourceUtil.getPropertyOrEnv("DATABASE_URL", "jdbc:sqlite3:" + getDbFilePath());
    }
}
