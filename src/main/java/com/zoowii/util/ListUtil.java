package com.zoowii.util;

import com.google.common.base.Function;

import java.util.ArrayList;
import java.util.List;

public class ListUtil {
    public static <T1, T2> List<T2> map(List<T1> source, Function<T1, T2> fn) {
        List<T2> result = new ArrayList<T2>();
        for (T1 item : source) {
            result.add(fn.apply(item));
        }
        return result;
    }

    public static <T> T first(List<T> source, Function<T, Boolean> fn) {
        for (T item : source) {
            Boolean predResult = fn.apply(item);
            if (predResult != null && predResult) {
                return item;
            }
        }
        return null;
    }

    public static <T> Function<T, Boolean> eq(final T val) {
        return new Function<T, Boolean>() {
            @Override
            public Boolean apply(T t) {
                if (t == null) {
                    return val == null;
                }
                return t.equals(val);
            }
        };
    }

    public static Function<String, Boolean> notEmptyString = new Function<String, Boolean>() {
        @Override
        public Boolean apply(String o) {
            if (o == null) {
                return false;
            }
            return o.length() > 0;
        }
    };

    public static <T> List<T> list(T[] source) {
        List<T> result = new ArrayList<T>();
        for (T item : source) {
            result.add(item);
        }
        return result;
    }

    public static <T> List<T> filter(List<T> source, Function<T, Boolean> fn) {
        List<T> result = new ArrayList<T>();
        for (T item : source) {
            Boolean predResult = fn.apply(item);
            if (predResult != null && predResult) {
                result.add(item);
            }
        }
        return result;
    }

}
