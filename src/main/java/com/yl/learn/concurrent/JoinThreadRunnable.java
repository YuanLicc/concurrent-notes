package com.yl.learn.concurrent;

public class JoinThreadRunnable implements Runnable {

    private Thread joinedThread;

    public JoinThreadRunnable(Thread joinedThread) {
        this.joinedThread = joinedThread;
    }

    @Override
    public void run() {
        try {
            joinedThread.join();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
