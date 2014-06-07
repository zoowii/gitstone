package com.zoowii.gitstone.git;

/**
 * git仓库的权限认证管理器,通过这个管理哪些项目哪个用户是有权限访问的
 */
public abstract class AbstractGitRepoAccessManager {
    public abstract boolean hasReadAccess(String repoPath, String username, String password);

    public abstract boolean hasWriteAccess(String repoPath, String username, String password);

    public boolean hasAllAccess(String repoPath, String username, String password) {
        return hasReadAccess(repoPath, username, password) && hasWriteAccess(repoPath, username, password);
    }
}
