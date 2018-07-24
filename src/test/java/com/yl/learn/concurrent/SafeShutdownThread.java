package com.yl.learn.concurrent;

import com.yl.learn.concurrent.util.ThreadUtils;
import junit.framework.TestCase;

public class SafeShutdownThread extends TestCase {

    static class Runner implements Runnable {
        private long count;
        private volatile boolean on = true;

        @Override
        public void run() {
            while(!Thread.currentThread().isInterrupted()
                && on) {
                count++;
            }
            System.out.println("count: " + count + " Thread id: " + Thread.currentThread().getId());
        }

        public void cancel() {
            this.on = false;
        }
    }

    public void testSafeShutdown() {
        Runner one = new Runner();
        Thread countThread = new Thread( one,"count");
        countThread.start();

        ThreadUtils.sleepSecond(1);
        countThread.interrupt();

        Runner two = new Runner();
        Thread cancelThread = new Thread(two, "cancel");
        cancelThread.start();

        ThreadUtils.sleepSecond(1);
        two.cancel();
    }
}
