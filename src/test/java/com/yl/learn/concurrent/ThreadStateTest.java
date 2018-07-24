package com.yl.learn.concurrent;

import com.yl.learn.concurrent.state.BlockedRunnable;
import com.yl.learn.concurrent.state.TimeWaitingRunnable;
import com.yl.learn.concurrent.state.WaitingClassRunnable;
import com.yl.learn.concurrent.util.ThreadUtils;
import junit.framework.TestCase;

public class ThreadStateTest extends TestCase {

    public void testThreadState() {
        new Thread(new TimeWaitingRunnable(100), "Time waiting").start();
        new Thread(new WaitingClassRunnable(), "Waiting class").start();
        // 下面两个线程将造成一个阻塞，一个超时等待，因为其中一个线程获取到锁后另一个线程将不能获取锁，造成阻塞
        new Thread(new BlockedRunnable(), "Block one").start();
        new Thread(new BlockedRunnable(), "Block two").start();
        ThreadUtils.dumpThreadsInfo();
    }
}
