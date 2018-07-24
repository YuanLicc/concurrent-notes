package com.yl.learn.concurrent.state;

import com.yl.learn.concurrent.util.ThreadUtils;

public class BlockedRunnable implements Runnable {
    @Override
    public void run() {
        // 竞争 BlockedRunnable.class 的锁
        synchronized (BlockedRunnable.class) {
            // 无限循环造成一直持有 BlockedRunnable.class 锁，使得需要 BlockedRunnable.class 锁的线程阻塞
            while (true) {
                ThreadUtils.sleepSecond(100);
            }
        }
    }
}
