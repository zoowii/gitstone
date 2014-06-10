package com.zoowii.gitstone.git;

import clojure.lang.RT;
import clojure.lang.Var;
import com.google.common.base.Function;
import com.sun.xml.internal.messaging.saaj.util.ByteInputStream;
import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;
import com.zoowii.gitstone.Settings;
import com.zoowii.util.ListUtil;
import com.zoowii.util.StringUtil;
import org.eclipse.jgit.api.ArchiveCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.archive.TarFormat;
import org.eclipse.jgit.archive.ZipFormat;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class GitService {
    private static final Logger logger = Logger.getLogger("GitService");

    private static GitService _instance = new GitService();

    private GitService() {
    }

    public static GitService getInstance() {
        return _instance;
    }

    public InputStream command(String cmd, String dirPath, InputStream inputStream) throws IOException, InterruptedException {
        try {
            RT.load("gitstone/git");
            Var execCmd = RT.var("gitstone.git", "exec-cmd");
            byte[] out = (byte[]) execCmd.invoke(cmd, dirPath, inputStream);
            return new ByteArrayInputStream(out);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void updateServerInfo(String dirPath) throws IOException, InterruptedException {
        String cmd = "update-server-info";
        command("git " + cmd, dirPath, null);
    }

    public Git getGitRepo(String repoPath) throws IOException {
        return Git.open(new File(repoPath));
    }

    private String getBranchNameFromBranchRef(Ref branch) {
        String branchName = branch.getName();
        if (branchName == null) {
            return null;
        }
        if (branchName.startsWith("refs/heads/")) {
            return branchName.substring("refs/heads/".length());
        } else {
            return branchName;
        }
    }

    public Ref getBranch(Git git, final String branchName) throws GitAPIException {
        return ListUtil.first(getBranches(git), new Function<Ref, Boolean>() {
            @Override
            public Boolean apply(Ref branch) {
                return getBranchNameFromBranchRef(branch).equals(branchName);
            }
        });
    }

    public List<Ref> getBranches(Git git) throws GitAPIException {
        if (git == null) {
            return null;
        }
        return git.branchList().call();
    }

    public List<String> getBranchNames(Git git) throws GitAPIException {
        List<Ref> branches = getBranches(git);
        if (branches == null) {
            return null;
        }
        return ListUtil.map(branches, new Function<Ref, String>() {
            @Override
            public String apply(Ref branch) {
                return getBranchNameFromBranchRef(branch);
            }
        });
    }

    public RevCommit getLastCommitOfBranch(Git git, String branchName) throws GitAPIException, IOException {
        if (branchName == null) {
            return null;
        }
        Ref branch = getBranch(git, branchName);
        if (branch == null) {
            return null;
        }
        RevWalk revWalk = new RevWalk(git.getRepository());
        RevCommit commit = revWalk.parseCommit(branch.getObjectId());
        revWalk.dispose();
        return commit;
    }

    /**
     * @param repo      git repository
     * @param tree      目录的tree
     * @param pathItems 目录的路径
     * @return 这个目录下的内容
     * @throws IOException
     */
    public List<GitTreeItem> listTreeWalkInDir(Repository repo, RevWalk revWalk, RevTree tree, List<String> pathItems) throws IOException {
        TreeWalk treeWalk = new TreeWalk(repo);
        treeWalk.addTree(tree);
        treeWalk.setRecursive(false);
        List<GitTreeItem> items = new ArrayList<GitTreeItem>();
        while (treeWalk.next()) {
            GitTreeItem item = new GitTreeItem(revWalk, treeWalk);
            if (pathItems.size() > 0) {
                item.setPath(StringUtil.join(pathItems, "/") + "/" + item.getPath());
            }
            items.add(item);
        }
        return items;
    }

    /**
     * 这段代码很丑
     *
     * @param repo      git repository
     * @param tree      当前目录tree,初始时可以提供某次commit的tree(就是那时的根目录)
     * @param pathItems 目标路径, 要求这个路径是目录的路径
     * @return 最终目标路径中的内容
     * @throws IOException
     */
    public List<GitTreeItem> recListTreeWalkInPath(Repository repo, RevWalk revWalk, RevTree tree, List<String> pathItems) throws IOException {
        String path = StringUtil.join(pathItems, "/");
        GitTreeItem curTree = new GitTreeItem();
        curTree.setPath("");
        curTree.setTree(true);
        RevTree curRevTree = tree;
        boolean ended = false;
        do {
            if (ended) {
                break;
            }
            if (curTree.getPath().equals(path)) {
                ended = true;
            }
            List<GitTreeItem> treeItems = listTreeWalkInDir(repo, revWalk, curRevTree, StringUtil.splitPath(curTree.getPath()));
            String restPath = StringUtil.pathMinus(pathItems, StringUtil.splitPath(curTree.getPath()));
            List<String> restPathItems = StringUtil.splitPath(restPath);
            if (restPathItems.size() < 1 || ended) {
                return treeItems;
            }
            boolean endCurLoop = false;
            for (GitTreeItem item : treeItems) {
                if (path.startsWith(item.getPath()) && item.getRevTree() != null) {
                    curTree = item;
                    curRevTree = item.getRevTree();
                    endCurLoop = true;
                    break;
                }
            }
            if (endCurLoop) {
                continue;
            }
            logger.info("Can't find path " + path);
            return null;
        } while (!ended);
        logger.info("Can't find path " + path);
        return null;
    }

    public GitTreeItem getObjectInPath(Git git, String branchName, String path) throws GitAPIException, IOException {
        if (branchName == null) {
            return null;
        }
        Ref branch = getBranch(git, branchName);
        if (branch == null) {
            return null;
        }
        RevWalk revWalk = new RevWalk(git.getRepository());
        RevCommit commit = revWalk.parseCommit(branch.getObjectId());
        RevTree tree = revWalk.parseTree(commit.getTree().getId());
        TreeWalk treeWalk = new TreeWalk(git.getRepository());
        treeWalk.addTree(tree);
        if (path == null) {
            path = "";
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length());
        }
        List<String> pathItems = StringUtil.splitPath(path);
        if (pathItems.size() > 0) {
            treeWalk.setFilter(PathFilter.create(path));
        }
        treeWalk.setRecursive(true);
        if (!treeWalk.next()) {
            logger.info("Not found in path " + path);
            revWalk.dispose();
            return null;
        }
        GitTreeItem gitTreeItem = new GitTreeItem();
        ObjectId objectId = treeWalk.getObjectId(0);
        gitTreeItem.setObjectId(objectId);
        gitTreeItem.setName(treeWalk.getNameString());
        gitTreeItem.setPath(treeWalk.getPathString());
        boolean isSubTree = treeWalk.isSubtree();
        gitTreeItem.setTree(isSubTree);
        if (treeWalk.getPathString().equals(path) && !treeWalk.isSubtree()) {
            return gitTreeItem;
        } else {
            List<GitTreeItem> items = recListTreeWalkInPath(git.getRepository(), revWalk, tree, pathItems);
            if (items == null) {
                return null;
            }
            gitTreeItem.setTree(true);
            gitTreeItem.setPath(path);
            if (pathItems.size() > 0) {
                gitTreeItem.setName(pathItems.get(pathItems.size() - 1));
            } else {
                gitTreeItem.setName("");
            }
            for (GitTreeItem item : items) {
                gitTreeItem.addItem(item);
            }
            return gitTreeItem;
        }
    }

    public void createRepository(String path) throws IOException {
        File file = new File(path);
        Repository repo = new FileRepository(file);
        try {
            repo.create(true);
        } catch (Exception e) {
            throw new IOException("create repo failed");
        }
    }

    public InputStream getBlob(Git git, ObjectId objectId) throws IOException {
        ObjectLoader loader = git.getRepository().open(objectId);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        loader.copyTo(byteArrayOutputStream);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        return byteArrayInputStream;
    }

    /**
     * 把git repo打包(ZIP)
     * TODO: 目前只支持对默认分支打包
     *
     * @throws IOException
     */
    public InputStream archiveGitRepo(Git git, String branchName) throws IOException, GitAPIException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Ref branch = getBranch(git, branchName);
        if (branch == null) {
            return null;
        }
        ArchiveCommand.registerFormat("zip", new ZipFormat());
        ArchiveCommand cmd = git.archive();
        try {
            RevWalk revWalk = new RevWalk(git.getRepository());
            RevCommit commit = revWalk.parseCommit(branch.getObjectId());
            RevTree tree = revWalk.parseTree(commit.getTree().getId());
            revWalk.dispose();
            cmd.setTree(tree).setFormat("zip").setOutputStream(byteArrayOutputStream).call();
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
            byteArrayOutputStream.close();
            return byteArrayInputStream;
        } finally {
            ArchiveCommand.unregisterFormat("zip");
            git.close();
        }
    }

    public void removeRepo(String ownerName, String repoName) throws IOException {
        String repoPath = Settings.getRepoPath(ownerName, repoName);
        File file = new File(repoPath);
        if (file.exists() && file.isDirectory()) {
            file.renameTo(new File(Settings.getTrashPathForRepo(ownerName, repoName)));
        }
    }

}
