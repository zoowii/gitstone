(ns gitstone.git-views
  (:import (com.zoowii.gitstone.git GitService GitTreeItem)
           (com.zoowii.mvc.http HttpRequest HttpResponse HttpRouter)
           (org.eclipse.jgit.api Git)
           (java.util ArrayList)
           (com.zoowii.util FileUtil StringUtil ClojureUtil)
           (org.apache.commons.lang StringUtils))
  (:require [hiccup.core :refer [html]]
            [hiccup.util :refer [escape-html]]
            [hiccup.element :refer [link-to ordered-list unordered-list image]]
            [gitstone.ui :as ui]
            [gitstone.git :as ggit]
            [clojure.tools.logging :as logging]
            [hiccup.page :refer [html5 include-js include-css]]
            [gitstone.layouts :refer :all]
            [gitstone.web :as web]
            [gitstone.util :as util]
            [gitstone.db :as db]
            [gitstone.partials :as partials]
            [gitstone.repo-dao :as repo-dao]))

(defn- no-access-to-view-repo
  [req res username repo-name]
  (html
    [:pre
     (str "You have no access to view repo " repo-name)]))

(defn- view-path-content
  [req res username repo-name path
   ^Git git cur-branch-name branch-names
   last-commit tree]
  (view-repo-layout
    req res username repo-name (str repo-name " " path)
    "code"
    (html
      [:div
       (partials/view-git-nav2-partial req username repo-name cur-branch-name branch-names path)
       [:div
        (partials/view-git-breadcrumb-partial req username repo-name cur-branch-name path)
        [:div
         [:div {:class "panel panel-default"}
          [:div {:class "panel-heading"}
           (partials/view-git-last-commit-partial req username repo-name last-commit)]
          [:div {:class "panel-body"}
           (when-not (.isTree tree)
             (let [stream (.getBlob (GitService/getInstance) git (.getObjectId tree))
                   preview-text (FileUtil/tryParseStreamToString stream)]
               [:div (if preview-text
                       {:id "code-preview-area" :style (str "width: 100%; min-height: " (int (* 1.5 (StringUtils/countMatches preview-text "\n"))) "em")}
                       {:id "code-preview-area"})
                (if preview-text
                  (escape-html preview-text)
                  "Can't preview this file yet")]))]
          (if (.isTree tree)
            [:ul {:class "list-group"}
             (for [item (.getItems tree)]
               [:li {:class "list-group-item"}
                [:span {:class (if (.isTree item)
                                 "glyphicon glyphicon-book"
                                 "glyphicon glyphicon-list-alt")}]
                [:a {:href (web/url-for "git_view_path" username repo-name cur-branch-name (.getPath item))}
                 (.getName item)]])])]]]])
    (when-not (.isTree tree)
      (include-js (web/js-url "src-min/ace.js")))
    (when-not (.isTree tree)
      (include-js (web/js-url "preview_git_file.js")))))

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
        (str "git clone " (partials/git-clone-url req username repo-name))]])))

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
      (view-path-content req res username repo-name path git cur-branch-name branch-names last-commit tree)
      (display-empty-repo req res username repo-name))))

(defn view-repo-settings-options
  [req res username repo-name branch-names]
  (let [cur-user (web/current-user req)
        repo (db/find-repo-by-name-and-user repo-name cur-user)
        branch-names (vec branch-names)]
    (view-repo-settings-layout
      req res username repo-name "options"
      (html
        [:div {:class "panel panel-primary"}
         [:div {:class "panel-heading"}
          [:h3 {:class "panel-title"}
           "Settings"]]
         [:div {:class "panel-body"}
          [:form {:role   "form"
                  :method "POST"
                  :action ""}
           (ui/form-group
             (html
               [:label "Repository Name:"]
               (ui/input-field {:type     "text"
                                :value    repo-name
                                :disabled "disabled"})))
           (ui/form-group
             (html
               [:label "Description:"]
               (ui/input-field {:type  "text"
                                :value (:description repo)})))
           (ui/form-group
             (html
               [:label "Default Branch:"]
               (ui/select-control
                 (for [b branch-names]
                   {:content b
                    :value   b})
                 (:default_branch repo))))
           (ui/form-group
             (html
               (ui/radio-group
                 "select_for_access"
                 [{:value   "public"
                   :content "Public"}
                  {:value   "private"
                   :content "Private"}]
                 (if (:is_private repo)
                   "private"
                   "public"))))
           [:button {:class "btn btn-default"
                     :type  "submit"}
            "Apply Changes"]]]]))))