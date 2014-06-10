package com.zoowii.util;

import clojure.java.api.Clojure;
import clojure.lang.IFn;

import java.util.logging.Logger;

public class ClojureUtil {
    private static final Logger logger = Logger.getLogger("ClojureUtil");

    public static void debug(Object obj) {
        logger.info("for clojure debug");
    }

    public static String name(Object cljObj) {
        if (cljObj == null) {
            return null;
        }
        IFn nameFn = Clojure.var("clojure.core", "name");
        if (nameFn == null) {
            return cljObj.toString();
        }
        Object res = nameFn.invoke(cljObj);
        if (res == null) {
            return cljObj.toString();
        }
        return res.toString();
    }
}
