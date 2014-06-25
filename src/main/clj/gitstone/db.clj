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

(defn- try-create-table!
  [tbl-name & specs]
  (try (execute!
         db-spec
         [(apply create-table-ddl
                 (cons tbl-name specs))])
       (catch Exception e (logging/error e))))

(defn- create-account-table!
  "创建账户表,账户包括用户和用户组,不过目前先只支持用户,不提供用户组的功能"
  []
  (try-create-table!
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
    [:deleted :bool]))

(defn- create-repository-table!
  "创建git repo表"
  []
  (try-create-table!
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
    [:parent_repository_name "varchar(50)"]))

(def repository-collaborator-table-name :repository_collaborator)
(defn- create-repository-collaborator-table!
  "创建git repo协作者表"
  []
  (try-create-table!
    repository-collaborator-table-name
    [:id "varchar(50)"]
    [:version "integer"]
    [:created_time "integer"]
    [:repo_id "varchar(50)"]
    [:user_id "varchar(50)"]
    [:role "varchar(50)"]                                   ;; role表示协作者身份, 包括admin/developer/viewer,目前所有协作者都当做admin role
    ))

(defn- create-milestone-table!
  []
  (try-create-table!
    :milestone
    [:id "varchar(50)"]
    [:version "integer"]
    [:created_time "integer"]
    [:creator_id "varchar(50)"]
    [:title "varchar(50)"]
    [:repo_id "varchar(50)"]
    [:description "text"]
    [:due_date "integer"]
    [:status "varchart(50)"]                                ;; open | closed
    [:closed_time "integer"]
    [:close_user_id "varchar(50)"]))

(defn- create-issue-table!
  []
  (try-create-table!
    :issue
    [:id "varchar(50)"]
    [:version "integer"]
    [:created_time "integer"]
    [:creator_id "varchar(50)"]
    [:title "varchar(50)"]
    [:content "text"]
    [:repo_id "varchar(50)"]
    [:assigned_to_user_id "varchar(50)"]
    [:milestone_id "varchar(50) null"]
    [:label_id "varchar(50)"]
    [:status "varchar(50)"]                                 ;; open | closed
    [:last_updated_time "integer"]
    [:closed_time "integer"]
    [:close_user_id "varchar(50) null"]))

(defn- create-issue-label-table!
  []
  (try-create-table!
    :issue_label
    [:id "varchar(50)"]
    [:version "integer"]
    [:created_time "integer"]
    [:name "varchar(50)"]
    [:color "varchar(50)"]))

(defn get-issue-labels
  []
  (query db-spec
         ["select * from issue_label"]))

(defn- init-issue-labels!
  []
  (when (empty? (get-issue-labels))
    (for [[name color] [["bug" "#fc2929"]
                        ["duplicate" "#cccccc"]
                        ["enhancement" "#84b6eb"]
                        ["invalid" "#e6e6e6"]
                        ["question" "#cc317c"]
                        ["wontfix" "#ffffff"]]]
      (let [label {:id           (uuid)
                   :version      1
                   :created_time (now-timestamp)
                   :name         name
                   :color        color}]
        (insert! db-spec :issue_label label)))))

(defn has-any-users
  []
  (let [rs (query
             db-spec
             ["select count(*) as count from account limit 1"]
             :row-fn :count)
        count (first rs)]
    (> count 0)))

(defn find-issues-by-repo
  [repo]
  (when repo
    (query
      db-spec
      ["select * from issue where repo_id = ? order by created_time desc" (:id repo)])))

(defn find-user-by-id
  [id]
  (when id
    (-> (query db-spec
               ["select * from account where id = ?" id])
        first)))

(defn get-assignee-of-issue
  "获取Issue被assign给了谁"
  [issue]
  (when issue
    (-> (:assigned_to_user_id issue)
        find-user-by-id)))

(defn find-user-by-username
  [username]
  (first
    (query
      db-spec
      ["select * from account where username = ?" username])))

(defn find-user-by-email
  [email]
  (first
    (query
      db-spec
      ["select * from account where email = ?" email])))

(defn find-repo-by-name-and-user
  "repo名称和用户名称不能包括特殊字符,避免URL有问题"
  [name user]
  (if (and name
           (pos? (.length name))
           (not (.contains name "/"))
           user)
    (let [rs (query
               db-spec
               ["select * from repository where name = ? and owner_name = ?" name (if (string? user)
                                                                                    user
                                                                                    (:username user))])]
      (first rs))))

(defn check-username
  "用户名只能包括字母,数字,还有'_', '-'"
  [name]
  (and name
       (re-matches #"[a-zA-Z_][\d\w_-]{3,}" name)))

(defn check-email-format
  [email]
  (and email
       (re-matches #"[a-zA-Z\d_]+@[a-zA-Z\d_\.]+\.[a-zA-Z][a-zA-Z0-9]*" email)))

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

(defn update-user!
  [set-map where-clause]
  (update! db-spec
           :account
           set-map
           where-clause))

(defn update-user-by-id!
  [set-map id]
  (update-user! set-map ["id = ?" id]))

(defn username-of-user
  [user]
  (when user
    (if (or (string? user) (nil? user))
      user
      (:username user))))

(defn find-repo-by-id
  [id]
  (first
    (query db-spec
           ["select * from repository where id = ?" id])))

(defn find-repos-of-user
  [user offset limit]
  (query db-spec
         ["select * from repository where owner_name = ? limit ? offset ?" (:username user) limit offset]))

(defn find-repos-of-user-as-collaborator
  "获取用户作为协作者可以访问的repos"
  [user offset limit]
  (let [repo-ids (->> (query db-spec
                             [(str "select repo_id from "
                                   (name repository-collaborator-table-name)
                                   " where user_id = ? limit ? offset ?")
                              (:id user) limit offset])
                      (map :repo_id))]
    (map find-repo-by-id repo-ids)))

(defn find-repos-user-can-access
  "获取用户直接拥有或者是协作者或者是同一组(TODO)的git repos"
  [user offset limit]
  (concat (find-repos-of-user user offset limit)
          (find-repos-of-user-as-collaborator user offset limit)))

(defn find-users
  [offset limit]
  (query db-spec
         ["select * from account limit ? offset ?" limit offset]))

(defn find-repo-collaborator-ids
  "获取repo的协作者的ID列表"
  [repo]
  (let [repo-id (if (map? repo) (:id repo) repo)]
    (->> (query db-spec
                [(str "select user_id from "
                      (name repository-collaborator-table-name)
                      " where repo_id = ?") repo-id])
         (map :user_id))))

(defn find-repo-collaborators
  "获取repo的协作者列表"
  [repo]
  (->> (find-repo-collaborator-ids repo)
       (map #(find-user-by-id (:user_id %)))))

(defn find-repo-collaborators-mapping
  "获取repo的协作者的映射关系列表,也就是repo-collaborator表的记录"
  [repo]
  (query db-spec
         [(str "select * from "
               (name repository-collaborator-table-name)
               " where repo_id = ?") (if (map? repo) (:id repo) repo)]))

(defn find-owner-of-repo
  [repo]
  (when repo
    (find-user-by-username (:owner_name repo))))

(defn find-repo-collaborators-with-owner
  [repo]
  (let [owner (find-owner-of-repo repo)]
    (conj (find-repo-collaborators repo) owner)))

(defn is-collaborator-of-repo?
  [repo user]
  (when (and repo user)
    (first (query
             db-spec
             [(str "select * from "
                   (name repository-collaborator-table-name)
                   " where repo_id = ? and user_id = ?") (:id repo) (:id user)]))))

(defn new-repo-collaborator-info
  [user-id repo-id role]
  {:id           (uuid)
   :version      1
   :created_time (now-timestamp)
   :repo_id      repo-id
   :user_id      user-id
   :role         role})

(defn insert-repo-collaborator!
  [info]
  (insert! db-spec
           repository-collaborator-table-name
           info))

(defn create-repo!
  [repo]
  (logging/info "create-repo: " repo)
  (insert! db-spec
           :repository
           repo)
  (let [git-service (GitService/getInstance)
        path (Settings/getRepoPath (:owner_name repo) (:name repo))]
    (.createRepository git-service path)))

(defn update-repo!
  [set-map where-clause]
  (update! db-spec
           :repository
           set-map
           where-clause))

(defn update-repo-by-id!
  [set-map id]
  (update-repo!
    set-map
    ["id = ?" id]))

(defn delete-repo!
  [where-clause]
  (delete! db-spec
           :repository
           where-clause))

(defn delete-repo-by-id!
  [id]
  (delete-repo!
    ["id = ?" id]))

(defn new-user-info
  ([username email password]
   (new-user-info username email password "user"))
  ([username email password role]
   (let [salt (StringUtil/randomString 10)
         password (StringUtil/encryptPassword password salt)]
     {:id                (uuid)
      :version           1
      :created_time      (now-timestamp)
      :username          username
      :password          password
      :email             email
      :salt              salt
      :role              (name role)
      :full_name         username
      :url               ""
      :last_updated_time (now-timestamp)
      :last_login_time   (now-timestamp)
      :last_active_time  (now-timestamp)
      :image             ""
      :is_group_account  false
      :deleted           false})))


(defn new-issue-info
  ([creator-id repo-id title content assignee-id label-id]
   (new-issue-info creator-id repo-id title content assignee-id label-id nil))
  ([creator-id repo-id title content assignee-id label-id milestone-id]
   (new-issue-info creator-id repo-id title content assignee-id label-id milestone-id "open"))
  ([creator-id repo-id title content assignee-id label-id milestone-id status]
   {:id                  (uuid)
    :version             1
    :created_time        (now-timestamp)
    :creator_id          creator-id
    :title               title
    :content             content
    :repo_id             repo-id
    :assigned_to_user_id assignee-id
    :milestone_id        milestone-id
    :label_id            label-id
    :status              status
    :last_updated_time   (now-timestamp)
    :closed_time         0
    :close_user_id       nil}))

(defn insert-issue!
  [issue]
  (insert! db-spec
           :issue
           issue))

(defn- init-users!
  []
  (let [username "root"
        password "root"
        user (new-user-info username "" password :admin)]
    (when-not (has-any-users)
      (create-user! user))))

(defn create-db!
  []
  (create-account-table!)
  (create-repository-table!)
  (create-repository-collaborator-table!)
  (create-milestone-table!)
  (create-issue-label-table!)
  (create-issue-table!)
  (init-users!)
  (init-issue-labels!))

(create-db!)

(logging/info (has-any-users))


