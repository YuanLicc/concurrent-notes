package com.yl.learn.concurrent;

import com.yl.learn.concurrent.util.ThreadUtils;
import junit.framework.TestCase;

public class DaemonThreadTest extends TestCase {

    static class FinallyRunnable implements Runnable {
        @Override
        public void run() {
            try {
                ThreadUtils.sleepSecond(100);
            }
            finally {
                System.out.println("FinallyRunnable finally block");
            }
        }
    }

    public void testDaemon() {
        Thread thread = new Thread(new FinallyRunnable(), "finallyRunnable");
        thread.setDaemon(true);
        thread.start();
    }
}
