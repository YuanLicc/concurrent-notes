package com.yl.learn.concurrent.state;

import com.yl.learn.concurrent.util.ThreadUtils;

public class TimeWaitingRunnable implements Runnable {

    private long sleepSecond;

    public TimeWaitingRunnable(long sleepTime) {
        this.sleepSecond = sleepTime;
    }

    @Override
    public void run() {
        // 休眠指定时间
        ThreadUtils.sleepSecond(sleepSecond);
    }
}
