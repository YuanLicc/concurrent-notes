package com.yl.learn.concurrent;

import junit.framework.TestCase;

import java.util.concurrent.CyclicBarrier;

public class CyclicBarrierTest extends TestCase {
    public void testCyclicBarrier() {
        int count = 3;
        CyclicBarrier cyclicBarrier = new CyclicBarrier(count);

        for(int i = 0; i < count; i++) {
            new Thread(() -> {
                try {
                    cyclicBarrier.await();
                    System.out.println(Thread.currentThread().getName());
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            },"thread " + i).start();
        }
    }
}
