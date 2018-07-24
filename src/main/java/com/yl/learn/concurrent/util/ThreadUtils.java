package com.yl.learn.concurrent.util;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class ThreadUtils {

    public static void sleepSecond(long sleepSecond) {
        try {
            TimeUnit.SECONDS.sleep(sleepSecond);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void dumpThreadsInfo() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

        Arrays.stream(threadMXBean.getAllThreadIds()).forEach(
            id -> {
                ThreadInfo threadInfo = threadMXBean.getThreadInfo(id);
                System.out.print("Thread Info: " + threadInfo);
            }
        );
    }

}
