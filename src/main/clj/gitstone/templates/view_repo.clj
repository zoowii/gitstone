(ns gitstone.templates.view-repo
  (:import (org.eclipse.jgit.api Git)
           (org.apache.commons.lang StringUtils)
           (com.zoowii.util FileUtil)
           (com.zoowii.gitstone.git GitService GitTreeItem))
  (:require [hiccup.core :refer [html]]
            [hiccup.util :refer [escape-html]]
            [hiccup.element :refer [link-to ordered-list unordered-list image]]
            [gitstone.ui :as ui]
            [gitstone.git :as ggit]
            [clojure.tools.logging :as logging]
            [hiccup.page :refer [html5 include-js include-css]]
            [gitstone.layouts :refer :all]
            [gitstone.partials :as partials]
            [gitstone.db :as db]
            [gitstone.web :as web]))

(defn view-file-tmpl
  [req res username repo-name path
   ^Git git cur-branch-name branch-names
   last-commit ^GitTreeItem tree]
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
