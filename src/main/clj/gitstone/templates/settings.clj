(ns gitstone.templates.settings
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
            [gitstone.web :as web]
            [gitstone.repo-dao :as repo-dao]))

(defn view-repo-settings-options-tmpl
  [req res repo branch-names]
  (view-repo-settings-layout
    req res (:owner_name repo) (:name repo) "options"
    (html
      (include-js (web/js-url "settings_options.js"))
      [:script
       (str "var updateSettingsOptionsUrl = '"
            (web/url-for "update-git-settings-options" (:owner_name repo) (:name repo))
            "';")]
      [:div {:class "panel panel-primary"}
       [:div {:class "panel-heading"}
        [:h3 {:class "panel-title"}
         "Settings"]]
       [:div {:class "panel-body"}
        [:form {:role   "form"
                :method "POST"
                :name   "settings_options_form"
                :action ""}
         (ui/form-group
           (html
             [:label "Repository Name:"]
             (ui/input-field {:type     "text"
                              :name     "repo_name"
                              :value    (:name repo)
                              :disabled "disabled"})))
         (ui/form-group
           (html
             [:label "Description:"]
             (ui/input-field {:type  "text"
                              :name  "description"
                              :value (:description repo)})))
         (ui/form-group
           (html
             [:label "Default Branch:"]
             (ui/select-control
               (for [b branch-names]
                 {:content b
                  :value   b})
               (:default_branch repo)
               {:name "default_branch"})))
         (ui/form-group
           (html
             (ui/radio-group
               "select_for_access"
               [{:value   "public"
                 :content "Public"}
                {:value   "private"
                 :content "Private"}]
               (if (repo-dao/is-repo-private repo)
                 "private"
                 "public")
               {:name "is_private"})))
         [:button {:class "btn btn-default save-btn"
                   :type  "button"}
          "Apply Changes"]]]])))

(defn view-repo-settings-danger-zone-tmpl
  [req res repo]
  (let [owner-name (:owner_name repo)
        repo-name (:name repo)]
    (view-repo-settings-layout
      req res owner-name repo-name "danger_zone"
      (html
        [:script
         (str "var delRepoUrl = '"
              (web/url-for "git-delete" owner-name repo-name)
              "';
              var indexUrl = '"
              (web/url-for "index")
              "';")]
        (include-js (web/js-url "settings_danger.js"))
        [:div {:class "panel panel-primary"}
         [:div {:class "panel-heading"}
          [:h3 {:class "panel-title"}
           "Danger Zone"]]
         [:div {:class "panel-body"}
          [:div {:class "row"}
           [:div {:class "col-md-4"}
            [:h4 "Transfer Ownership"]
            [:p
             "Transfer this repo to another user or to group."]]
           [:div {:class "col-md-4"}
            (ui/input-field {:type  "text"
                             :class "transfer-to-field"})
            (ui/danger-btn
              "Transfer"
              {:class "transfer-ownership-btn btn btn-danger"})]]
          [:div {:class "row"}
           [:div {:class "col-md-4"}
            [:h4 "Delete repository"]
            [:p
             "Once you delete a repository, there is no going back."]]
           [:div {:class "col-md-4"}
            (ui/danger-btn
              "Delete this repository"
              {:class "delete-repo-btn btn btn-danger"})]]]]))))