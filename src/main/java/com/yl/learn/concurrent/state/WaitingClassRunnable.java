package com.yl.learn.concurrent.state;

public class WaitingClassRunnable implements Runnable {
    @Override
    public void run() {
        synchronized (WaitingClassRunnable.class) {
            try {
                // 使得线程在 WaitingClassRunnable.class 上等待
                WaitingClassRunnable.class.wait();
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
