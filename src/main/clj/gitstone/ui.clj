(ns gitstone.ui
  (:require [hiccup.core :refer [html]]
            [clojure.string :as str]))

(defn input-field
  ([props]
   (input-field props ""))
  ([props name]
   (input-field props name ""))
  ([props name value]
   [:input (into {:name name :value value :class "form-control"} props)]))

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
   [:div (into {:class "form-group"} props)
    content]))

(defn form-label
  ([content]
   (form-label content {}))
  ([content props]
   [:label (into {:class "col-sm-2 control-label"} props)
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
   [:button (into {:class "btn btn-default"} props)
    text]))

(defn success-btn
  ([text]
   (default-btn text {}))
  ([text props]
   [:button (into {:class "btn btn-success"} props)
    text]))

(defn nav-item
  [url text active]
  [:a {:href  url
       :class (if active
                "blog-nav-item active"
                "blog-nav-item")}
   text])

(defn list-group
  "items 是一个列表,里面每一项形如{content: ..., link: ..., active: true/false, props: ...}"
  ([items]
   (list-group items nil))
  ([items list-props]
   [:ul (into {:class "list-group"} list-props)
    (for [item items]
      [:a (into {:class (if (:active item)
                          "list-group-item active"
                          "list-group-item")
                 :href  (if (:link item)
                          (:link item)
                          "#")}
                (:props item))
       (:content item)])]))

(defn select-control
  "items是一个列表,每一项形如{content: ..., value: ..., props: ...)"
  ([items selected]
   (select-control items selected nil))
  ([items selected props]
   [:select (into {:class "form-control"} props)
    (for [item items]
      [:option (into (if (= selected
                            (:value item))
                       {:selected "selected"}
                       {})
                     (:props item))
       (:content item)])]))

(defn radio-group
  "items是一个列表,每一项形如{content: ..., value: ..., props: ...)"
  ([name items selected]
   (radio-group name items selected nil))
  ([name items selected props]
   (html
     (for [item items]
       [:div (into {:class "radio"}
                   props)
        [:label
         [:input (-> {:type  "radio"
                      :name  name
                      :value (str (:value item))}
                     (into (if (= (:value item) selected)
                             {:checked "checked"}))
                     (into (:props item)))]
         (:content item)]]))))