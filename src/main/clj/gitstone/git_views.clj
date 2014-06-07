(ns gitstone.git-views
  (:import (com.zoowii.gitstone.git GitService GitTreeItem)
           (com.zoowii.mvc.http HttpRequest HttpResponse HttpRouter)
           (org.eclipse.jgit.api Git)
           (java.util ArrayList)
           (com.zoowii.util FileUtil))
  (:require [hiccup.core :refer [html]]
            [gitstone.ui :as ui]
            [gitstone.git :as ggit]
            [clojure.tools.logging :as logging]
            [gitstone.layouts :refer :all]
            [gitstone.web :as web]))

(defn- git-clone-url
  [req username repo-name]
  (str (.getFinalHostWithPort req) (web/url-for "git-url" username repo-name)))

(defn- view-path-content
  "如果这个git repo不为空,则显示文件夹列表或者文件内容"
  [req res username repo-name
   ^GitService git-service ^Git git
   cur-branch-name path
   branch-names last-commit tree]
  (website-layout
    (web/current-user req) "GitStone"
    (html
      [:div {:class "row main-content"}
       [:div
        [:select
         (for [bn branch-names]
           [:option (if (= bn cur-branch-name)
                      {:selected "selected"}
                      {})
            bn])]]]
      [:div
       [:span "Path: "]
       [:span path]]
      [:div
       [:span "Last Commit: "]
       [:span (if last-commit
                [:div
                 [:span (ggit/commit-id last-commit)]
                 [:span "  By: "]
                 [:span (ggit/commit-author-name last-commit)]
                 "&nbsp;&nbsp;&nbsp;"
                 [:span (ggit/commit-short-msg last-commit)]]
                "no commit now")]]
      [:div {:class (if (.isTree tree) "dir-panel" "file-panel")}
       (if (.isTree tree)
         [:ul
          (for [item (.getItems tree)]
            [:li
             (let [url (HttpRouter/reverseUrl "git_view_path" (jlist [username repo-name cur-branch-name (.getPath item)]))]
               [:a {:href url}
                (.getName item)])])]
         (let [stream (.getBlob git-service git (.getObjectId tree))
               preview-text (FileUtil/tryParseStreamToString stream)]
           (if preview-text
             [:pre
              preview-text]
             "Can't preview this file yet")))])))

(defn- display-empty-repo
  "显示空git repo"
  [req res username repo-name]
  (website-layout
    (web/current-user req) (str "GitStone " username "/" repo-name)
    (html
      [:div {:class "row main-content"}
       [:pre
        (str "This repository is empty. You can start with")
        [:br]
        (str "git clone " (git-clone-url req username repo-name))]])))

(defn view-path
  "浏览git repo某个分支的某个路径"
  [^HttpRequest req ^HttpResponse res
   username repo-name
   ^GitService git-service ^Git git
   cur-branch-name path]
  (let [branch-names (vec (.getBranchNames git-service git))
        last-commit (ggit/last-commit git-service git cur-branch-name)
        ^GitTreeItem tree (ggit/object-in-path git-service git cur-branch-name path)]
    (if cur-branch-name
      (view-path-content req res username repo-name git-service git cur-branch-name path branch-names last-commit tree)
      (display-empty-repo req res username repo-name))))
