(ns gitstone.db
  (:import (com.zoowii.gitstone Settings)
           (com.zoowii.util StringUtil)
           (java.io File)
           (com.zoowii.gitstone.git GitService))
  (:require [clojure.java.jdbc :refer :all]
            [clojure.tools.logging :as logging]
            [clojure.string :as str]
            [gitstone.util :refer [uuid now-timestamp]]))

(def db-spec
  {:classname   "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname     (Settings/getDbFilePath)})

(logging/info db-spec)

(defn- create-account-table!
  []
  (try (execute!
         db-spec
         [(create-table-ddl
            :account
            [:id "varchar(50)"]
            [:version "integer"]
            [:created_time "integer"]
            [:username "varchar(50)"]
            [:password "varchar(255)"]
            [:salt "varchar(50)"]
            [:full_name "varchar(50)"]
            [:email "varchar(50)"]
            [:role "varchar(50)"]
            [:url "varchar(255)"]
            [:last_updated_time "integer"]
            [:last_login_time "integer"]
            [:last_active_time "integer"]
            [:image "varchar(255)"]
            [:is_group_account :bool]
            [:deleted :bool])])
       (catch Exception e (logging/info e))))

(defn- create-repository-table!
  []
  (try (execute!
         db-spec
         [(create-table-ddl
            :repository
            [:id "varchar(50)"]
            [:version "integer"]
            [:created_time "integer"]
            [:name "varchar(50)"]
            [:is_private :bool]
            [:description "text"]
            [:default_branch "varchar(50)"]
            [:last_updated_time "integer"]
            [:owner_name "varchar(50)"]
            [:last_active_time "integer"]
            [:parent_user_name "varchar(50)"]
            [:parent_repository_name "varchar(50)"])])
       (catch Exception e (logging/info e))))

(defn has-any-users
  []
  (let [rs (query
             db-spec
             ["select count(*) as count from account limit 1"]
             :row-fn :count)
        count (first rs)]
    (> count 0)))

(defn find-user-by-username
  [username]
  (let [rs (query
             db-spec
             ["select * from account where username = ?" username])]
    (first rs)))

(defn find-repo-by-name-and-user
  "repo名称和用户名称不能包括特殊字符,避免URL有问题"
  [name user]
  (if (and name
           (pos? (.length name))
           (not (.contains name "/"))
           user)
    (let [rs (query
               db-spec
               ["select * from repository where name = ? and owner_name = ?" name (:username user)])]
      (first rs))))

(defn check-username
  "用户名只能包括字母,数字,还有'_', '-'"
  [name]
  (and name
       (re-matches #"[a-zA-Z_][\d\w_-]{3,}" name)))

(defn check-repo-name
  "repo名称只能包括字母,数字,还有'_', '-'"
  [name]
  (and name
       (re-matches #"[a-zA-Z_][\d\w_-]{3,}" name)))

(defn create-user!
  "TODO: 检查用户名是否有特殊字符,比如'/'"
  [user]
  (when-not (find-user-by-username (:username user))
    (insert!
      db-spec
      :account
      user)
    (let [user-dir (Settings/getUserGitRootPath (:username user))
          file (File. user-dir)]
      (.mkdirs file))))

(defn find-repos-of-user
  [user offset limit]
  (let [rs (query db-spec
                  ["select * from repository where owner_name = ? limit ? offset ?" (:username user) limit offset])]
    rs))

(defn create-repo!
  [repo]
  (logging/info "create-repo: " repo)
  (insert! db-spec
           :repository
           repo)
  (let [git-service (GitService/getInstance)
        path (Settings/getRepoPath (:owner_name repo) (:name repo))]
    (.createRepository git-service path)))

(defn- init-users
  []
  (let [username "root"
        password "root"
        salt (StringUtil/randomString 10)
        password (StringUtil/encryptPassword password salt)
        user {:id                (uuid)
              :version           1
              :created_time      (now-timestamp)
              :username          username
              :password          password
              :email             ""
              :salt              salt
              :role              "admin"
              :full_name         username
              :url               ""
              :last_updated_time (now-timestamp)
              :last_login_time   (now-timestamp)
              :last_active_time  (now-timestamp)
              :image             ""
              :is_group_account  false
              :deleted           false}]
    (when-not (has-any-users)
      (create-user! user))))

(defn create-db!
  []
  (create-account-table!)
  (create-repository-table!)
  (init-users))

(create-db!)

(logging/info (has-any-users))


