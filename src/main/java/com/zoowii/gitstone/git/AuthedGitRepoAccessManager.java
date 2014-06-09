package com.zoowii.gitstone.git;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import clojure.lang.RT;

import java.io.IOException;

/**
 * 实际使用的git repo权限管理器
 */
public class AuthedGitRepoAccessManager extends AbstractGitRepoAccessManager {
    @Override
    public boolean hasReadAccess(String repoPath, String username, String password) {
        try {
            RT.load("gitstone/user_dao");
            IFn fn = Clojure.var("gitstone.user-dao", "auth-access-read-repo");
            if (fn != null) {
                Object res = fn.invoke(username, password, repoPath);
                return res != null && !res.equals(false);
            } else {
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean hasWriteAccess(String repoPath, String username, String password) {
        try {
            RT.load("gitstone/user_dao");
            IFn fn = Clojure.var("gitstone.user-dao", "auth-access-write-repo");
            if (fn != null) {
                Object res = fn.invoke(username, password, repoPath);
                return res != null && !res.equals(false);
            } else {
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean hasAdminAccess(String repoPath, String username, String password) {
        try {
            RT.load("gitstone/user_dao");
            IFn fn = Clojure.var("gitstone.user-dao", "auth-access-admin-repo");
            if (fn != null) {
                Object res = fn.invoke(username, password, repoPath);
                return res != null && !res.equals(false);
            } else {
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }
}
