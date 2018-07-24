package com.yl.learn.concurrent;

import com.yl.learn.concurrent.util.ThreadUtils;
import junit.framework.TestCase;

public class ThreadMXBeanTest extends TestCase {

    public void testThreadInfo() {
        ThreadUtils.dumpThreadsInfo();
    }
}
