package com.yl.learn.concurrent;

public class Timer {

    private static final ThreadLocal<Long> TIME = new ThreadLocal<>();

    private Timer(){}

    public static Timer start() {
        Timer instance = new Timer();
        instance.TIME.set(System.nanoTime());
        return instance;
    }

    public Long end() {
        return System.nanoTime() - TIME.get();
    }

}
