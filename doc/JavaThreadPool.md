## Java 中的线程池

`Java` 中的线程池是运用场景最多的并发框架，几乎所有需要异步或并发执行任务的程序都可以使用线程池，它有三个好处：

- 降低资源消耗

  通过重复利用已创建的线程降低线程创建和销毁造成的消耗。

- 提高响应速度

  当任务到达时，任务可以不需要等到线程创建就能立即执行。

- 提高线程的可管理型

  秀按成是稀缺资源，如果无限制的创建，不仅会消耗系统资源，还会降低系统的稳定性，使用线程池也可以进行统一分配、调优和监控。

### 线程池的实现原理

当线程池提交一个任务之后，线程池的处理流程：

1）线程池判断核心线程池里的线程是否都在执行任务。如果不是，则创建一个新的工作线程来执行任务。如果核心线程池里的线程都在执行任务，则进入下个流程。

2）线程池判断工作队列是否已经满。如果工作队列没有满，则将新提交的任务存储在工作队列中。如果工作队列满了，进入下个流程。

3）线程池判断线程池的线程是否都处于工作状态。如果没有，则创建一个新的工作线程来执行任务。如果已经满了，则交给饱和策略来处理这个任务。

`ThreadPoolExecutor` 执行 `execute` 方法分为下面 4 中情况：

1）如果当前运行的线程少于 `corePoolSize`，则创建新线程来执行任务（需要获取全局锁）。

2）如果运行的线程等于或多余 `corePoolSize`，则将任务加入 `BlockingQueue`。

3）如果无法将任务加入 `BlockingQueue`（队列已满），则创建新的线程来处理任务（需要获取全局锁）。

4）如果创建新线程将使当前运行的线程超出 `maximumPoolSize`，任务将被拒绝，并调用 `RejectedExecutionHandler.rejectedExecution()`方法。

`ThreadPoolExecutor` 采用上诉步骤的总体设计思路，是为了在执行 `execute`方法时，尽可能地避免获取全局锁。在 `ThreadPoolExecutor` 完成预热之后（当前线程数大于等于 `corePoolSize`），几乎所有的 `execute` 方法调用都是执行步骤 2，而步骤 2 不需要获取全局锁。

```java
public void execute(Runnable command) {
    if (command == null)
        throw new NullPointerException();
    int c = ctl.get();
    if (workerCountOf(c) < corePoolSize) {
        if (addWorker(command, true))
            return;
        c = ctl.get();
    }
    if (isRunning(c) && workQueue.offer(command)) {
        int recheck = ctl.get();
        if (! isRunning(recheck) && remove(command))
            reject(command);
        else if (workerCountOf(recheck) == 0)
            addWorker(null, false);
    }
    else if (!addWorker(command, false))
        reject(command);
}
```

线程池创建线程时，会将线程封装成工作线程 `Worker`，`worker` 在执行完任务后，还会循环获取工作队列里的任务来执行。

![](https://github.com/YuanLicc/concurrent-notes/blob/master/doc/images/threadPoolExecutor.png)

