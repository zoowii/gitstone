(ns gitstone.repo-dao
  (:import (com.zoowii.gitstone.git GitService)
           (com.zoowii.util ClojureUtil))
  (:require [gitstone.db :as db]
            [clojure.tools.logging :as logging]))

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

(defn can-access-repo
  "判断用户是否可以访问某个repo
  TODO: 如果repo所有者是一个group, 或者用户是这个repo的协作者,那么即使是私有repo也是可以访问的"
  [repo user]
  (let [username (if (or (string? user) (nil? user))
                   user
                   (:username user))]
    (when repo
      (or (not (is-repo-private repo))
          (= username
             (:owner_name repo))))))

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