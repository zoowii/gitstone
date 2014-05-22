(ns any-route.http
  (:use any-route.core))

(defn- str-http-method
  [method]
  (if (keyword? method)
    (str-http-method (subs (str method) 1))
    (.toUpperCase method)))

(defn- rm-http-method-prefix
  [s]
  (subs s (inc (.indexOf s "_"))))

(def http-methods [:GET :POST :HEAD :OPTION :PUT :DELETE])

(defn http-route
  "建立http的route表. 如果method使用:ANY,则映射到所有已知http method"
  ([method route-str handler]
   (http-route method route-str handler nil))
  ([method route-str handler route-name]
   (if (= (str-http-method method) "ANY")
     (context-route
       ""
       (map #(http-route % route-str handler route-name)
            http-methods))
     (route route-str
            handler
            (if route-name
              (str (str-http-method method)
                   "_"
                   route-name)
              nil)
            (str (str-http-method method)
                 "_")))))

(defn http-route-table-match
  [route-table method source]
  (if-let [find-result (route-table-match route-table
                                          (str (str-http-method method)
                                               "_"
                                               source))]
    (assoc find-result
      :name (rm-http-method-prefix (:name find-result))
      :route (rm-http-method-prefix (:route find-result))
      :method method
      :source source)))

(defn find-route-in-http-route-table
  [route-table name]
  (first-not-empty
    (map (fn [method]
           (let [mname (str (str-http-method method)
                            "_" name)]
             (find-route-in-route-table route-table mname)))
         http-methods)))

(defn reverse-in-http-route-table
  "获取Http的路由表的路由反射"
  [route-table name & params]
  (if-let [reverse-result (apply reverse-in-route-table-using-fn
                                 (apply merge
                                        [route-table name find-route-in-http-route-table]
                                        params))]
    (rm-http-method-prefix reverse-result)))