package com.zoowii.util;

import java.util.concurrent.Callable;

/**
 * Created by zoowii on 14/10/6.
 */
public abstract class HolderCallable<T> implements Callable {
    public T holder;

    public HolderCallable(T holder) {
        this.holder = holder;
    }
}
