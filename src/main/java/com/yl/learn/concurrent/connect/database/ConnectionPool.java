package com.yl.learn.concurrent.connect.database;

import java.util.LinkedList;

public class ConnectionPool {

    private LinkedList<Connection> pool = new LinkedList<>();

    public ConnectionPool(int initialSize) {
        if(initialSize > 0) {
            pool.addLast(new Connection());
        }
    }

    public void release(Connection connection) {
        if(connection != null) {
            synchronized (pool) {
                pool.addLast(connection);
                pool.notifyAll();
            }
        }
    }

    public Connection get() throws InterruptedException{
        return get(1000);
    }

    public Connection get(long millis) throws InterruptedException{
        synchronized (pool) {
            if(millis <= 0) {
                while (pool.isEmpty()) {
                    pool.wait();
                }
                return pool.removeFirst();
            }
            else {
                long future = System.currentTimeMillis() + millis;
                long remaining = millis;

                while (pool.isEmpty() && remaining > 0) {
                    pool.wait(remaining);
                    remaining = future - System.currentTimeMillis();
                }
                Connection connection = null;

                if(!pool.isEmpty()) {
                    connection = pool.removeFirst();
                }
                return connection;
            }
        }
    }

    public class Connection {
    }

}
