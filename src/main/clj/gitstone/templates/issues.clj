(ns gitstone.templates.issues
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

(defn create-issue-tmpl
  [req res cur-username repo labels collaborators]
  (view-repo-layout
    req res cur-username (:name repo) (str "Create Issue - " (:name repo)) "issues"
    (html
      (ui/horizontal-form
        (html
          (ui/simple-form-group
            (ui/form-label "Title")
            (ui/input-field {:type "text"} "title"))
          (ui/simple-form-group
            (ui/form-label "Content")
            (ui/input-field {:type "text"} "content"))
          (ui/simple-form-group
            (ui/form-label "Assigned To")
            (ui/select-control
              (for [collaborator collaborators]
                {:content (:username collaborator)
                 :value   (:id collaborator)})
              {:value nil}
              {:name "assignee_id"}))
          (ui/simple-form-group
            (ui/form-label "Label")
            (ui/select-control
              (for [label labels]
                {:content (:name label)
                 :value   (:id label)
                 :props   {:style (str "color: " (:color label) ";")}})
              {:value (when (seq labels)
                        (:id (first labels)))}
              {:name "label_id"}))
          (ui/form-whole-div
            (ui/default-btn "Submit"
                            {:type "submit"})))
        (web/url-for "git-create-issues-handler" (:owner_name repo) (:name repo))
        :POST))))

(defn view-repo-issues-tmpl
  [req res cur-user repo]
  (view-repo-layout
    req res (db/username-of-user cur-user) (:name repo) (str "Issues - " (:name repo)) "issues"
    (html
      [:ul {:class "nav nav-tabs"}
       [:li {:class "active"}
        [:a {:href        "#issues-tab"
             :data-toggle "tab"}
         "Browse Issues"]]
       [:li
        [:a {:href        "#milestones-tab"
             :data-toggle "tab"}
         "Milestones"]]]
      [:div {:class "tab-content"}
       [:div {:class "tab-pane active"
              :id    "issues-tab"}
        (partials/issue-list-with-panel req res cur-user repo)]
       [:div {:class "tab-pane"
              :id    "milestones-tab"}
        "Milestones"]])))