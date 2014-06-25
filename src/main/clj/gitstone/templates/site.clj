(ns gitstone.templates.site
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
            [gitstone.util :as util]))

(defn index-tmpl
  [req res repos]
  (website-layout
    (web/current-user req) "GitStone"
    (html
      [:div {:class "row main-content"}
       [:div {:class "col-sm-8 blog-main"}
        [:ul {:class "nav nav-tabs"}
         [:li {:class "active"}
          [:a {:href "#"}
           "Activities"]]
         [:li
          [:a {:href "#"}
           "Pull Requests"]]
         [:li
          [:a {:href "#"}
           "Issues"]]]]
       [:div {:class "col-sm-3 col-sm-offset-1 blog-sidebar"}
        [:div {:class "sidebar-module sidebar-module-inset"}
         [:h4 "About"]
         [:p "GitStone, another clone of Github"]]
        [:div {:class "sidebar-module"}
         [:h4
          [:a {:class "btn btn-xs btn-success pull-right"
               :href  (web/url-for "new-repo")}
           "New Repo"]
          "Your Repositories"]
         [:ol {:class "list-unstyled"}
          (if (empty? repos)
            [:li "Empty Repositories"]
            (for [repo repos]
              [:li
               [:a {:href (web/url-for "git_view_index" (:owner_name repo) (:name repo))}
                (:name repo)]]))
          [:li
           [:a {:href "#"}
            "..."]]]]
        [:div {:class "sidebar-module"}
         [:h4 "Elsewhere"]
         [:ol {:class "list-unstyled"}
          [:li
           [:a {:href "https://github.com/zoowii/gitstone"}
            "Github"]]
          [:li
           [:a {:href "http://zoowii.com"}
            "Author: @zoowii"]]]]]])
    nil))