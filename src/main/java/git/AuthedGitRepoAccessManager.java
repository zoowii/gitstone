package git;

/**
 * 实际使用的git repo权限管理器
 */
public class AuthedGitRepoAccessManager extends AbstractGitRepoAccessManager {
    @Override
    public boolean hasReadAccess(String repoPath, String username, String password) {
        return username.equals(password);
    }

    @Override
    public boolean hasWriteAccess(String repoPath, String username, String password) {
        return username.equals(password);
    }
}
