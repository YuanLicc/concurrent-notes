package com.yl.learn.concurrent;

import com.yl.learn.concurrent.util.ThreadUtils;
import junit.framework.TestCase;

public class ThreadJoinTest extends TestCase {

    public void testThreadTest() {
        Thread one = new Thread(new JoinThreadRunnable(Thread.currentThread()), "One");

        one.start();

        Thread two = new Thread(new JoinThreadRunnable(one), "two");

        two.start();

        ThreadUtils.sleepSecond(1);

        System.out.println(Thread.currentThread().getName());

        one.interrupt();
    }
}
