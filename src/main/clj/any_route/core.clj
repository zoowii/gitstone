(ns any-route.core)

;; 定义一组规则,将字符串(比如url)双射到一个名称
;; (比如常见Web MVC框架中的controller-action, params为source str中捕获的值)
;; 同时支持双向映射
;; 规则允许嵌套,比如(context-route "/api" [["/user" "user"] ["/role" "role"]])
;; 为了捕获params, 需要有分隔符的定义,默认分隔符是'/',也可以改为其他(暂时不能改)
;; source中捕获形如:abc, :*abc 的内容, 后者可以跨分隔符捕获参数,前者不可以

;; 目前context的base-path不支持带有参数

;; BUG较多,比如在source中有\/的时候就不对了

(def ^:private re-chars (set "\\.*+|?()[]{}$^"))

(defn- re-escape
  "Escape all special regex chars in a string."
  [s]
  (clojure.string/escape
    s
    #(if (re-chars %) (str \\ %))))

(defn first-not-empty
  "返回第一个非nil/false的元素"
  [col]
  (first
    (filter #(not (or (nil? %)
                      (= false %)))
            col)))

;; 默认的分隔符
(def ^:private default-sep "/")

(defn get-route-params
  [route]
  (->> route
       re-escape
       (re-seq #"((?!\\):(\\\*)?[a-zA-Z_][a-zA-Z_0-9]*)")
       (map second)
       (map #(if (.startsWith % ":\\*")
              (subs % 3)
              (subs % 1)))))

(defn make-route
  "创建一个只有在route规则匹配时才会返回非false/nil的函数(实际返回的应该是dest和params)"
  [route]
  (let [route-params (get-route-params route)
        route (re-escape route)
        route (clojure.string/replace route #"(:[a-zA-Z_][a-zA-Z_0-9]*)" (str "([^" default-sep "]+)"))
        route (clojure.string/replace route #"(:\\\*[a-zA-Z_][a-zA-Z_0-9]*)" "(.+)")
        route (str "^" route "$")
        route (re-pattern route)]
    (fn [source]
      (let [matcher (re-matches route source)]
        (if matcher
          (map vector route-params (next matcher)))))))

(defn make-un-route
  "创建一个用来构造反转路由的函数"
  [route & params]
  (let [route-params (get-route-params route)
        res (reduce #(clojure.string/replace
                      %1
                      (re-pattern
                        (str "(?!\\\\):(\\*)?" (first %2)))
                      (str (second %2)))
                    route
                    (map vector route-params params))]
    res))


(defn route
  "建立路由,route-str可能是一个字符串,也可能是一个context-route"
  ([route-str handler route-name]
   (route route-str handler route-name nil))
  ([route-str handler route-name extra-info]
   {
     :name       route-name
     :route      route-str
     :extra-info extra-info
     :handler    handler
     }))

(defn context-route
  "嵌套路由"
  [ctx routes]
  {
    :context ctx
    :routes  routes
    })

(defn is-context-route
  "判断是否是嵌套路由"
  [route]
  (:context route))

(defn make-route-table
  "建立路由表,这个时候根据路由定义,还有context建立正则"
  ([routes]
   (make-route-table routes ""))
  ([routes context]
   (for [route routes]
     (if (is-context-route route)
       (assoc route
         :routes
         (make-route-table (:routes route)
                           (str context (:context route))))
       (let [route-str (str (if (:extra-info route)
                              (:extra-info route)
                              "")
                            context
                            (:route route))]
         (assoc route
           :route route-str
           :route-fn (make-route route-str)
           :route-params (get-route-params
                           route-str)))))))

(defn route-table-match
  "在一个路由表中找到映射"
  [route-table source]
  (first-not-empty
    (map
      (fn [route]
        (if (is-context-route route)
          (route-table-match (:routes route)
                             source)
          (if-let [route-fn (:route-fn route)]
            (if-let [route-binding (route-fn source)]
              (assoc route
                :binding
                route-binding)))))
      route-table)))

(defn find-route-in-route-table
  "在路由表中根据名字找到路由, 如果路由在context中,则补全完整的路由"
  [route-table name]
  (first-not-empty
    (map
      (fn [route]
        (if (is-context-route route)
          (find-route-in-route-table
            (:routes route)
            name)
          (and (= (:name route) name)
               route)))
      route-table)))

(defn reverse-in-route-table-using-fn
  [route-table name find-fn & params]
  (if-let [route (find-fn route-table name)]
    (apply make-un-route
           (cons (:route route)
                 params))))

(defmacro reverse-in-route-table
  "在路由表中获取路由反射的结果"
  [route-table name & params]
  `(reverse-in-route-table-using-fn
     ~route-table ~name ~find-route-in-route-table ~@params))

