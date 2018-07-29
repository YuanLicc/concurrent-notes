package com.yl.learn.concurrent;

import com.yl.learn.concurrent.connect.database.ConnectionPool;
import com.yl.learn.concurrent.util.ThreadUtils;
import junit.framework.TestCase;

public class ConnectionPoolTest extends TestCase {

    static class SleepRunnable implements Runnable {

        private ConnectionPool pool;

        public SleepRunnable(ConnectionPool pool) {
            this.pool = pool;
        }

        @Override
        public void run() {
            try {
                Timer timer = Timer.start();
                ConnectionPool.Connection connection = pool.get(1);

                if(connection != null) {
                    pool.release(connection);
                    System.out.println("Thread Name: " + Thread.currentThread().getName() + ", time: " + timer.end());
                }
                else {
                    System.out.println("Thread Name: " + Thread.currentThread().getName() + ", get null");
                }
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void testConnectionPool() {

        ConnectionPool pool = new ConnectionPool(10);

        for(int i = 0; i < 100; i++) {
            Thread thread = new Thread(new SleepRunnable(pool), i + "");
            thread.start();
        }

    }

}
