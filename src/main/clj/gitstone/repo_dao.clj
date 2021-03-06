(ns gitstone.repo-dao
  (:import (com.zoowii.gitstone.git GitService)
           (com.zoowii.util ClojureUtil)
           (com.zoowii.mvc.http HttpRequest))
  (:require [gitstone.db :as db]
            [clojure.tools.logging :as logging]
            [gitstone.util :as util]))

(defn update-repo-settings!
  "修改repo的设置(描述,默认分支,public/private)"
  [owner-name repo-name
   description default-branch is-private]
  (when-let [repo (db/find-repo-by-name-and-user repo-name owner-name)]
    (db/update-repo-by-id! {:description    description
                            :default_branch default-branch
                            :is_private     is-private}
                           (:id repo))))

(defn is-repo-private
  [repo]
  (and repo
       (or (= true (:is_private repo))
           (pos? (:is_private repo)))))

(defn repo-public?
  [repo]
  (not (is-repo-private repo)))

(defn can-admin-repo
  "判断用户是否有某个repo的管理权限"
  [repo user]
  (let [username (db/username-of-user user)]
    (when repo
      (util/clj-debug user repo)
      (or (= username (:owner_name repo))
          (db/is-collaborator-of-repo? repo (db/find-user-by-username username))))))

(defn can-access-repo
  "判断用户是否可以访问某个repo
  TODO: 如果repo所有者是一个group,那么即使是私有repo也是可以访问的"
  [repo user]
  (when repo
    (or (repo-public? repo)
        (can-admin-repo repo user))))

(defn can-read-repo?
  [repo user]
  (when repo
    (or (repo-public? repo)
        (can-access-repo repo user))))

(defn default-branch-of-repo-by-name
  [owner-name repo-name]
  (when-let [repo (db/find-repo-by-name-and-user repo-name owner-name)]
    (:default_branch repo)))

(defn del-repo-by-name!
  [owner-name repo-name]
  (when-let [repo (db/find-repo-by-name-and-user repo-name owner-name)]
    (do
      (db/delete-repo-by-id! (:id repo))
      (try
        (.removeRepo (GitService/getInstance) owner-name repo-name)
        (catch Exception e (logging/error e))))))

(defn get-repo-from-req
  [^HttpRequest req]
  (db/find-repo-by-name-and-user (.getParam req "repo") (.getParam req "user")))