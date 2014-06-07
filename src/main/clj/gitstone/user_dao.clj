(ns gitstone.user-dao
  (:import (com.zoowii.util StringUtil))
  (:require [gitstone.db :as db]))

(defn check-user-password
  [user password]
  (and user
       (= (:password user)
          (StringUtil/encryptPassword password (:salt user)))))
