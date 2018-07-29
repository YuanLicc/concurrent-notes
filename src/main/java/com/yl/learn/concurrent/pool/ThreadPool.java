package com.yl.learn.concurrent.pool;

public interface ThreadPool<T extends Runnable> {

    void execute(T job);

    void shutdown();

    void add(int num);

    void remove(int num);

    int getWaitSize();

}
