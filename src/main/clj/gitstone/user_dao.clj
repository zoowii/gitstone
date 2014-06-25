(ns gitstone.user-dao
  (:import (com.zoowii.util StringUtil))
  (:require [gitstone.db :as db]
            [gitstone.repo-dao :as repo-dao]
            [gitstone.web :as web]
            [gitstone.util :as util]))

(defn check-user-password
  [user password]
  (and user
       (= (:password user)
          (StringUtil/encryptPassword password (:salt user)))))

(defn check-username-password
  [username password]
  (and username
       password
       (let [user (db/find-user-by-username username)]
         (= (:password user)
            (StringUtil/encryptPassword password (:salt user))))))

(defn- get-repo-from-path
  "从repo的路径中获取到repo的数据库记录"
  [repo-path]
  (when repo-path
    (let [splited (vec (StringUtil/splitPath repo-path))]
      (when (> (count splited) 2)
        (let [owner-name (nth splited (- (count splited) 2))
              repo-name (nth splited (dec (count splited)))
              repo (db/find-repo-by-name-and-user repo-name owner-name)]
          repo)))))

(defn find-user-by-username-and-password
  [username password]
  (when (and username password)
    (let [user (db/find-user-by-username username)]
      (when (check-user-password user password)
        user))))

(defn can-read-repo?
  [cur-username repo-owner-name repo-name]
  (when-let [repo (db/find-repo-by-name-and-user repo-name repo-owner-name)]
    (let [user (db/find-user-by-username cur-username)]
      (repo-dao/can-access-repo repo user))))

(defn auth-access-read-repo
  [username password repo-path]
  (when-let [repo (get-repo-from-path repo-path)]
    (let [user (find-user-by-username-and-password username password)]
      (repo-dao/can-access-repo repo user))))

(defn auth-access-write-repo
  [username password repo-path]
  (when-let [repo (get-repo-from-path repo-path)]
    (let [user (find-user-by-username-and-password username password)]
      (and user
           (repo-dao/can-admin-repo repo user)))))

(defn auth-access-admin-repo
  [username password repo-path]
  (when-let [repo (get-repo-from-path repo-path)]
    (let [user (find-user-by-username-and-password username password)]
      (and user
           (repo-dao/can-admin-repo repo user)))))

(defn is-admin-user
  [user]
  (and user
       (= "admin" (:role user))))

(defn is-admin-user-req
  [req]
  (-> req
      web/current-user
      is-admin-user))

(defn is-group-account
  [user]
  (and user
       (or (= true (:is_group_account user))
           (pos? (:is_group_account user)))))

(defn encrypt-password
  [password salt]
  (StringUtil/encryptPassword password salt))

(defn add-user-change-log!
  "增加一条用户变化记录(表示用户信息变化了)"
  [user]
  (when user
    (db/update-user-by-id! {:last_updated_time (util/now-timestamp)
                            :version           (inc (:version user))}
                           (:id user))))

(defn add-user-action-log!
  "增加一条用户活跃记录"
  [user]
  (when user
    (db/update-user-by-id! {:last_active_time (util/now-timestamp)
                            :version          (inc (:version user))}
                           (:id user))))

(defn add-user-login-log!
  "增加一条用户登录记录"
  [user]
  (when user
    (db/update-user-by-id! {:last_login_time (util/now-timestamp)
                            :version         (inc (:version user))}
                           (:id user))))

(defn check-username-available
  [username]
  (and username
       (db/check-username username)
       (nil? (db/find-user-by-username username))))

(defn check-email-available
  [email]
  (and email
       (db/check-email-format email)
       (nil? (db/find-user-by-email email))))
