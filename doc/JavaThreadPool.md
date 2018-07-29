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

线程池中的线程执行任务分为两种情况，如下：

1）在 `execute` 方法中创建一个线程时，会让这个线程执行当前任务。

2）这个线程执行完任务后，会反复从 `BlockingQueue` 获取任务来执行。

### 线程池的作用

#### 线程池的创建

可通过构造方法创建一个线程池：

```java
public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue,
                              ThreadFactory threadFactory,
                              RejectedExecutionHandler handler) {
    ...
}
```

参数如下：

- `corePoolSize`

  线程池的基本大小，当提交一个任务到线程池时，线程池会创建一个线程来执行任务，即使其他空闲的基本线程能够执行新任务也会创建线程，等到需要执行的任务数大于线程池基本大小时就不再创建。如果调用了线程池的 `prestartAllCoreThreads()` 方法，线程池会提前创建并启动所有基本线程。

- `runnableTaskQueue`

  任务队列，用于保存等待执行的任务的阻塞队列，可以选择以下几个阻塞队列：

  - `ArrayBlockingQueue`

    是一个基于数组结构的有界阻塞队列，此队列先进先出原则对元素排序。

  - `LinkedBlockingQueue`

    一个基于链表结构的阻塞队列，此队列按先进先出排序元素，吞吐量通常高于 `ArrayBlickingQueue`。静态工厂方法 `Executors.newFixedThreadPool` 使用了这个队列。

  - `SynchronousQueue`

    一个不存储元素的阻塞队列，每个插入操作必须等到另一个线程调用移除操作，否则插入操作一直处于阻塞状态，吞吐量通常高于 `LinkedBlockingQueue`，静态工厂方法 `Executors.newCachedThreadPool` 使用了次队列。

  - `PriorityBlockingQueue`

    一个具有优先级的无限阻塞队列。

- `maximumPoolSize`

  线程池最大数量，线程池允许创建的最大线程数，如果队列满了，并且已创建的线程数小于最大线程数，则线程池会再创建新的线程执行任务。如果使用了无界的任务队列，这个参数将无效。

- `ThreadFactory`

  用于设置创建线程的工厂，可以通过线程工厂给每个创建出来的线程设置更有意义的名字。

- `RejectedExecutionHandler`

  饱和策略，当队列和线程池都满了，说明线程池处于饱和状态，那么必须采取一种策略处理提交的新任务。这个策略默认情况下是 `AbortPolicy`，表示无法处理新任务时跑出异常。在 `JDK 1.5` 中提供了一下 4 中策略：

  - `AbortPolicy`

    直接跑出异常。

  - `CallerRunsPolicy`

    只用调用者所在的线程来运行任务。

  - `DiscardOldestPolicy`

    丢弃队列里最近的一个任务，并执行当前任务。

  - `DiscardPolicy`

    不处理，丢弃掉。

  当然，也可以根据应用场景需要来实现 `RejectedExecutionHandler` 接口自定义策略。

- `keepAliveTime`

  线程活动保持时间，线程池的工作线程空闲后，保持存活的时间，所以任务很多，并且每个任务执行的时间比较短，可以调大时间，提高线程利用率。

- `TimeUnit`

  线程活动保持时间的单位。

#### 向线程池提交任务

可以使用两个方法向线程提交任务，分别为 `execute` 和 `submit` 方法。`execute` 方法用于提交不需要返回值的任务，所以无法判断任务是否被线程池执行成功。

```java
threadPool.execute(() -> {
    // ...
})
```

`submit` 方法用于提交需要返回值的任务，线程池会返回一个 `future` 类型的对象，通过这个 `future` 对象可以判断任务是否执行成功，并且可以通过 `future` 的 `get` 方法获取返回值， `get` 方法会阻塞当前线程直到任务完成，而使用 `get(long timeout, TimeUnit unit)` 方法则会阻塞当前线程一段时间后立即返回，这时候可能任务没有执行完。

```java
Future<Object> future = executor.submit(task);
try{
    Object s = future.get();
}
catch(InterruptedException e) {
    // 处理中断异常
}
catch(ExecutionException e) {
    // 处理无法执行任务异常
}
finnaly {
    executor.shutdown();
}
```

#### 关闭线程池

可以通过调用线程池的 `shutdown` 或 `shutdownNow` 方法来关闭线程池。它们的原理是遍历线程池中的工作线程，然后逐个调用线程的 `interrupt` 方法来中断线程，所以无法响应中断的任务可能永远无法终止。但是它们存在一定的区别，`shutdownNow` 首先将线程池的状态设置成 `STOP`，然后尝试停止所有的正在执行或暂停任务的线程，并返回等待执行任务的列表，而 `shutdown` 只是将线程池的状态设置成 `SHUTDOWM` 状态，然后中断所有没有正在执行任务的线程。只要调用了这两个关闭方法的任意一个，`isShutdown` 方法返回 `true`。当所有的任务都已经关闭后，才表示线程池关闭成功，这是调用 `isTerminaed` 方法会返回 `true`。至于应该调用哪一种方法来关闭线程池，应该由提交到线程池的任务特性决定，通常调用 `shutdowm` 方法来关闭线程池，如果任务不一定要执行完，则可以调用 `shutdownNow` 方法。

#### 合理的配置线程池

要想合理的配置线程池，就必须首先分析任务特性，可以从以下几个角度分析：

1）任务的性质

`CPU` 密集型任务、`IO` 密集型任务和混合型任务。

2）任务的优先级

高、中、低。

3）任务的执行时间

长、短、中。

4）任务的依赖性

是否依赖其它系统资源，如数据库连接。

性质不同的任务可以用不同规模的线程池分开处理。`CPU` 密集型任务应配置尽可能小的线程。由于 `IO` 密集型任务线程并不是一直在执行任务，则应该配置尽可能多的线程。混合型任务，如果可以拆分，将其拆分成一个 `CPU` 密集型和`IO` 密集型任务，只要两个任务执行的时间差不是太大，那么分解后执行的吞吐量将高于串行执行的吞吐量。如果这两个任务执行时间相差很大，则没必要进行分解，可以通过 `Runtime.getRuntime().availableProcessors()` 方法获得当前设备的 `CPU` 个数。

优先级不同的任务可以使用优先级队列 `PriorityBlockingQUeue` 来处理。如果一直有优先级高的任务提交到队列，那么优先级低的任务可能永远不能执行。

执行时间不同的任务可以交给不同规模的线程池来处理，或者可以使用优先级队列，让执行时间短的任务先执行。

依赖数据库连接池的任务，因为线程提交 `SQL` 后需要等待数据库返回结果，等待的时间越长，则 `CPU` 空闲时间就越长，那么线程数应该设置的越大，这样才能更好的利用 `CPU`。

建议使用有界队列：

有界队列能增加系统的稳定性和预警能力，可以根据需要设大一点，比如几千。

#### 线程池的监控

如果系统中大量使用线程池，则有必要对线程池进行监控，方便在出现问题时，可以根据线程池的使用状况快速定位问题。可以通过线程池提供的参数进行监控，在监控线程池的时候可以使用以下属性：

- `taskCount`

  线程池需要执行的任务数量。

- `completedTaskCount` 

  线程池在运行过程中已完成的任务数量，小于或等于 `taskCount`。

- `largestPoolSize`

  线程池里曾经创建过的最大线程数量，通过这个数据可以知道线程池是否曾经满过。如该数值等于线程池的最大大小，则表示线程池曾经满过。

- `getPoolSize`

  线程池的线程数量。如果线程池不销毁的话，线程池里的线程不会自动销毁，所以这个大小只增不减。

- `getActiveCount`

  获取活动的线程数。

通过扩展线程池进行监控，可以通过继承线程池来自定义线程池，重写线程池的 `beforeExecute`、`afterExecute`、`terminated` 方法，也可以在任务执行前、执行后和线程池关闭前执行一些代码来进行监控。