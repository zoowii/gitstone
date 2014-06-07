(ns gitstone.ui
  (:require [hiccup.core :refer [html]]
            [clojure.string :as str]))

(defn input-field
  ([props name]
   (input-field props name ""))
  ([props name value]
   [:input (into props {:name name :value value :class "form-control"})]))

(defn input-field-row
  ([display-text props name]
   (input-field-row display-text props name ""))
  ([display-text props name value]
   [:div
    [:span display-text]
    (input-field props name value)
    [:br]]))


;; TODO: 以下几个改成宏,从而可以省掉(html ...)
(defn horizontal-form
  ([content]
   (horizontal-form content "" "GET"))
  ([content action method]
   (horizontal-form content action method ""))
  ([content action method extra-class]
   (horizontal-form content action method extra-class {}))
  ([content action method extra-class extra-attrs]
   (html
     [:form (merge {:class  (str/join " " ["form-horizontal" extra-class])
                    :method method
                    :action action
                    :role   "form"}
                   extra-attrs)
      content])))

(defn form-group
  ([content]
   (form-group content {}))
  ([content props]
   [:div (into props {:class "form-group"})
    content]))

(defn form-label
  ([content]
   (form-label content {}))
  ([content props]
   [:label (into props {:class "col-sm-2 control-label"})
    content]))

(defn form-div
  [content]
  [:div {:class "col-sm-10"}
   content])

(defn form-whole-div
  [content]
  [:div {:class "col-sm-offset-2 col-sm-10"}
   content])

(defn default-btn
  ([text]
   (default-btn text {}))
  ([text props]
   [:button (into props {:class "btn btn-default"})
    text]))

(defn success-btn
  ([text]
   (default-btn text {}))
  ([text props]
   [:button (into props {:class "btn btn-success"})
    text]))