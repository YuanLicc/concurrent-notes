package com.yl.learn.concurrent;

import com.yl.learn.concurrent.fork.NumbersAddTask;
import junit.framework.TestCase;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

public class NumbersAddTaskTest extends TestCase {
    public void testNumbersAddTask() {
        ForkJoinPool forkJoinPool = new ForkJoinPool();

        NumbersAddTask task = new NumbersAddTask(1, 10);

        Future<Integer> res = forkJoinPool.submit(task);

        if(task.isCompletedAbnormally()) {
            task.getException().printStackTrace();
        }

        try {
            System.out.println(res.get());
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        catch (ExecutionException e) {
            e.printStackTrace();
        }
    }
}
