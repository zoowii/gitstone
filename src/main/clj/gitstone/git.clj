(ns gitstone.git
  (:import (com.zoowii.gitstone.git GitService GitTreeItem)
           (org.eclipse.jgit.api Git)
           (org.eclipse.jgit.revwalk RevCommit))
  (:require [clojure.java.shell :refer [sh]]))

(defn exec-cmd
  "执行shell命令"
  [cmd dir-path input-stream]
  (let [sh-res (apply sh (merge
                           (vec (.split cmd " "))
                           :out-enc :bytes
                           :in input-stream
                           :dir dir-path))]
    (:out sh-res)))

(defn last-commit
  [^GitService git-service
   ^Git git
   ^String cur-branch-name]
  (.getLastCommitOfBranch git-service git cur-branch-name))

(defn commit-id
  [^RevCommit commit]
  (.getId commit))

(defn commit-author-ident
  [^RevCommit commit]
  (.getAuthorIdent commit))

(defn commit-author-name
  [^RevCommit commit]
  (.getName (commit-author-ident commit)))

(defn commit-short-msg
  [^RevCommit commit]
  (.getShortMessage commit))

(defn commit-full-msg
  [^RevCommit commit]
  (.getFullMessage commit))

(defn object-in-path
  [^GitService git-service
   ^Git git branch-name path]
  (.getObjectInPath git-service git branch-name path))

(defn is-tree
  [^GitTreeItem tree-item]
  (.isTree tree-item))