(ns gitstone.repo-dao
  (:require [gitstone.db :as db]))

(defn can-access-repo
  "判断用户是否可以访问某个repo
  TODO: 如果repo所有者是一个group, 或者用户是这个repo的协作者,那么即使是私有repo也是可以访问的"
  [repo user]
  (let [username (if (or (string? user) (nil? user))
                   user
                   (:username user))]
    (when repo
      (or (not (:is_private repo))
          (= username
             (:owner_name repo))))))

