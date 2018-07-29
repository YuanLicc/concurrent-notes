## Java 中的并发工具类

在 `JDK` 并发包中提供了几个非常有用的并发工具类。`CountDownLatch`、`CyclicBarrier` 、`Semaphone` 工具类提供了一种并发流程控制手段，`Exchanger` 工具类则提供了在线程间交换数据的一种手段。

### 等待多线程完成的 CountDownLatch

`CountDoownLatch` 允许一个或多个线程等待其他线程完成操作。假设我们需要使得等待所有线程都执行完毕后，提示执行完毕的功能，最简单的操作就是 `join`：

```java
thread2.join(thread1);
thread3.join(thread2);
```

`join` 用于让当前执行线程等待 `join` 线程执行结束。其实现原理就是不停检查 `join` 线程是否存活，如果 `join` 线程存活则让当前线程永远等待。直到 `join` 线程中止后，线程的 `this.notifyAll()` 方法会被调用，调用 `notifyAll` 方法是在 `JVM` 里实现的，所以在 `JDK` 里看不到，可以查看 `JVM` 源码。

在 `JDK 1.5` 之后的并发包中提供的 `CountDownLatch` 也可以实现 `join` 的功能，并且比 `join` 的功能更多。`CountDownLatch` 的构造函数接收一个 `int` 类型的参数作为计数器，如果你想等待 `N` 个点完成，这里就传入 `N`。当我们调用 `CountDownLatch` 的 `countDowm` 方法时，`N` 就会减 1，`DountDownLatch` 的 `await` 方法会阻塞当前线程，直到 `N` 变成零。由于 `countDown` 方法可以用在任何地方，所以这里说的 `N` 个点，可以是 `N` 个线程，也可以是 1 个线程里的 `N` 个步骤。

```java
public void testCountDownLatch() throws Exception {
    CountDownLatch countDownLatch = new CountDownLatch(20);

    for(int i = 0; i < 20; i++) {
        new Thread(new CountdownRunnable(countDownLatch), i +" thread").start();
    }
    countDownLatch.await();
    System.out.println("OK");
}

static class CountdownRunnable implements Runnable {

    private CountDownLatch countDownLatch;

    CountdownRunnable(CountDownLatch countDownLatch) {
        this.countDownLatch = countDownLatch;
    }

    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName());
        this.countDownLatch.countDown();
    }
}
```

### 同步屏障 CyclicBarrier

`CyclicBarrier` 的字面意思是可循环使用的屏障。让一组线程到达一个屏障时被阻塞，直到最后一个线程到达屏障时，屏障才会开门，所有被屏障拦截的线程才会继续运行。

#### CyclicBarrier 简介

`CyclicBarrier` 默认的构造方法是 `CyclicBarrier(int parties)`，七参数表示屏障拦截的线程数量，每个线程调用 `await` 方法告诉 `CyclicBarrier` 已经到达了屏障，然后当前线程被阻塞。

```java
public void testCyclicBarrier() {
    int count = 3;
    CyclicBarrier cyclicBarrier = new CyclicBarrier(count);

    for(int i = 0; i < count; i++) {
        new Thread(() -> {
            try {
                cyclicBarrier.await();
                System.out.println(Thread.currentThread().getName());
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        },"thread " + i).start();
    }
}
```

因为主线程和子线程的调度由 `CPU` 决定的，两个线程都有可能先执行，所以会产生多种输出。若拦截线程未达到指定线程数量，那么被拦截的线程将永远等待。`CyclicBarrier` 还提供了更加高级的构造函数，用于在线程到达屏障时优先执行 `barrierAction`：`CyclicBarrier(int parties, Runnable barrierAction)`。

#### CyclicBarrier 的应用场景

`CyclicBarrier` 可以用于多线程计算数据，最后合并计算结果的场景。

#### CyclicBarrier 和 CountDownLatch 的区别

`CountDownLatch` 的计数器只能使用一次，而 `CyclicBarrier` 的计数器可以使用 `reset` 方法重置。所以 `CyclicBarrier` 能处理更为复杂的业务场景。

### 控制并发线程数的 Semaphore

`Semaphore` 信号量是用来控制同时访问特定资源的线程数量，它通过协调各个线程，以保证合理的使用公共资源。

#### 应用场景

`Semaphore` 可以用于做流量控制，特别是公用资源有限的应用场景，比如数据库连接。`Semaphore` 的构造方法 `Semaohore` 的构造方法 `Semaphore(int permits)` 接受一个整型的数字，表示可用的许可证数量。`Semaphore(10)` 表示允许 10 个线程获取许可证，也就是最大并发数是 10。`Semaphore` 的用法很简单，首先线程使用 `Semaphore` 的 `acquire` 方法获取一个许可证，使用完之后调用 `release` 方法归还许可证。

#### 其它方法

- `int availablePermits`

  返回此信号量中当前可用的许可证数。

- `int getQueueLength`

  返回正在等待获取许可证的线程数。

- `boolean hasQueuedThreads`

  是否有线程正在等待获取许可证。

- `void reducePermits(int reduction)`

  减少 `reduction` 个许可证。

- `Collection getQueuedThreads`

  返回所有等待获取许可证的线程集合。

### 线程间交换数据的 Exchanger

`Exchanger` 交换者是一个用于线程间协作的工具类。`Exchanger` 用于进行线程间的数据交换。它提供一个同步点，在这个同步点，两个线程可以交换彼此的数据。这两个线程通过 `exchange` 方法交换数据，如果第一个线程先执行 `exchange` 方法，它会一直等待第二个线程也执行 `exchange` 方法，当两个线程都到达同步点时，这两个线程就可以交换数据，将本县城生产出来的数据传递给对方。

#### 应用场景

1）`Exchanger` 可以用于遗传算法

遗传算法里需要选取两个人作为交配对象，这时候交换两个人的数据，并使用交叉规则得出两个交配结果。

2）`Exchanger` 可用于校对工作

比如需要将纸质银行流水通过人工方式录入成电子银行流水，为了避免错误，采用 `AB` 岗两人进行录入，录入后系统对着两条数据进行校对。



