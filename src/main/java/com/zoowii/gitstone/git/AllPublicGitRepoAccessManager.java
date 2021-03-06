package com.zoowii.gitstone.git;

public class AllPublicGitRepoAccessManager extends AbstractGitRepoAccessManager {
    @Override
    public boolean hasReadAccess(String repoPath, String username, String password) {
        return true;
    }

    @Override
    public boolean hasWriteAccess(String repoPath, String username, String password) {
        return true;
    }

    @Override
    public boolean hasAdminAccess(String repoPath, String username, String password) {
        return true;
    }
}
