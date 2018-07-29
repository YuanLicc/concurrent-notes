package com.yl.learn.concurrent;

import com.yl.learn.concurrent.pool.DefaultThreadPool;
import com.yl.learn.concurrent.pool.ThreadPool;
import com.yl.learn.concurrent.util.ThreadUtils;
import junit.framework.TestCase;

public class DefaultThreadPoolTest extends TestCase {
    public void testThreadPool() {
        ThreadPool pool = new DefaultThreadPool(100);
        pool.add(1);
        pool.getWaitSize();
        pool.remove(1);
        while (pool.getWaitSize() == 100) {
            pool.execute(() -> {
                System.out.print(Thread.currentThread());
            });
        }
        pool.shutdown();
    }
}
