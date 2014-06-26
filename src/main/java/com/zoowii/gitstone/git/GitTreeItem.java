package com.zoowii.gitstone.git;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GitTreeItem {
    private boolean isTree = false;
    private ObjectId objectId = null;
    private String name = null;
    private String path = null;
    private RevTree revTree = null;
    private RevWalk revWalk = null;

    public RevTree getRevTree() {
        return revTree;
    }

    public void setRevTree(RevTree revTree) {
        this.revTree = revTree;
    }

    public GitTreeItem() {
    }

    public GitTreeItem(RevWalk revWalk, TreeWalk treeWalk) throws IOException {
        this.revWalk = revWalk;
        this.isTree = treeWalk.isSubtree();
        this.objectId = treeWalk.getObjectId(0);
        this.name = treeWalk.getNameString();
        this.path = treeWalk.getPathString();
        if (isTree) {
            this.revTree = revWalk.parseTree(objectId);
        }
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private List<GitTreeItem> items = new ArrayList<GitTreeItem>();

    public boolean isTree() {
        return isTree;
    }

    public void setTree(boolean isTree) {
        this.isTree = isTree;
    }

    public ObjectId getObjectId() {
        return objectId;
    }

    public void addItem(GitTreeItem item) {
        this.items.add(item);
    }

    public List<GitTreeItem> getItems() {
        return items;
    }

    public Iterator<GitTreeItem> iterator() {
        return items.iterator();
    }

    public void setObjectId(ObjectId objectId) {
        this.objectId = objectId;
    }

    /**
     * TODO: 这个有错
     */
    public RevCommit getCommit() {
        try {
            return revWalk.parseCommit(objectId);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public ObjectId getCommitId() {
        RevCommit commit = getCommit();
        return commit != null ? commit.getId() : null;
    }

}