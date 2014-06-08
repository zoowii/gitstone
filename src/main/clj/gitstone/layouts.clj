(ns gitstone.layouts
  (:import (com.zoowii.mvc.http HttpRouter)
           (java.util ArrayList))
  (:require [hiccup.core :refer [html]]
            [hiccup.page :refer [html5 include-js include-css]]
            [hiccup.element :refer [link-to mail-to image]]
            [gitstone.db]
            [gitstone.web :as web]))

(defn jlist
  [col]
  (ArrayList. (vec col)))

(defn jlist*
  [& col]
  (jlist col))

(defn static-url
  [path]
  (HttpRouter/reverseUrl "static-file" (jlist* path)))

(defn js-url
  [path]
  (static-url (str "js/" path)))

(defn css-url
  [path]
  (static-url (str "css/" path)))

(defn img-url
  [path]
  (static-url (str "img/" path)))

(defn nav-item
  [url text active]
  [:a {:href  url
       :class (if active
                "blog-nav-item active"
                "blog-nav-item")}
   text])

(defn website-layout
  ([cur-user title content]
   (website-layout cur-user title content nil))
  ([cur-user title content cur-module]
   (html5
     [:head
      [:title title]
      [:meta {:charset "utf8"}]
      [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge"}]
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
      [:meta {:name "description" :content "GitStone"}]
      [:meta {:name "author" :content "zoowii"}]
      [:link {:ref "shortcut icon" :href (img-url "favicon.jpg")}]
      (include-css
        (css-url "bootstrap.min.css")
        ;(css-url "bootstrap-theme.min.css")
        (css-url "website.css"))
      (include-js
        (js-url "jquery.min.js")
        (js-url "underscore.min.js")
        (js-url "bootstrap.min.js"))]
     [:body
      [:div {:class "blog-masthead"}
       [:div {:class "container"}
        [:nav {:class "blog-nav"}
         (nav-item (web/url-for "index") "Home" (= cur-module nil))
         (nav-item (web/url-for "new-repo-page") "New Repo" (= cur-module "new-repo"))
         (if cur-user
           (nav-item "#" (:username cur-user) (= cur-module "profile")))
         (if (and cur-user (= (:role cur-user "admin")))
           (nav-item "#" "Administration" (= cur-module "admin")))
         (if cur-user
           (nav-item (web/url-for "logout") "Sign Out" (= cur-module "sign-out"))
           (nav-item (web/url-for "login_page" (jlist*)) "Sign In" (= cur-module "sign-in")))]]]
      [:div {:class "container main-container"}
       content]
      [:div {:class "blog-footer"}
       [:p
        "Author: "
        [:a {:href "http://github.com/zoowii"}
         "zoowii"]]]])))