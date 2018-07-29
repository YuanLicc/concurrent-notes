package com.yl.learn.concurrent;

import com.yl.learn.concurrent.util.ThreadUtils;
import junit.framework.TestCase;

public class ThreadLocalTest extends TestCase {

    public void testThreadLocal() {
        Timer timer = Timer.start();

        ThreadUtils.sleepSecond(1);

        System.out.println(timer.end());
    }
}
