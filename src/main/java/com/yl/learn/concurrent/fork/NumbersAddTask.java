package com.yl.learn.concurrent.fork;

import java.util.concurrent.RecursiveTask;

/**
 * 连续数字相加（fork/join）
 * @author YuanLi
 */
public class NumbersAddTask extends RecursiveTask<Integer> {

    private static final int THRESHOLD = 2;
    private int start, end;

    public NumbersAddTask(int start, int end) {
        this.start = start;
        this.end = end;
    }

    @Override
    protected Integer compute() {

        int sum = 0;
        boolean canCompute = end - start < THRESHOLD;

        if(canCompute) {
            if(start == end) {
                sum = start;
            }
            else {
                sum = start + end;
            }
        }
        else {
            int middle = (start + end) / 2;

            NumbersAddTask leftTask = new NumbersAddTask(start, middle);
            NumbersAddTask rightTask = new NumbersAddTask(middle + 1, end);

            leftTask.fork();
            rightTask.fork();

            int leftResult = leftTask.join();
            int rightResult = rightTask.join();

            sum = leftResult + rightResult;
        }
        return sum;
    }

}
