## Executor 框架

在 `Java` 中，使用线程来异步执行任务。`Java` 线程的创建与销毁需要一定的开销，如果我们为每一个任务创建一个新线程来执行，这些线程的创建与销毁将消耗大量的资源。

`Java` 线程即使工作单元，也是执行机制。从 `JDK 1.5` 开始，把工作单元与执行机制分离开来。工作单元包括 `Runnable` 和 `Callable`，而执行机制由 `Executor` 框架提供。

### Executor 框架简介

#### Executor 框架的两级调度模型

在 `HotSpot VM` 的线程模型中，`Java` 线程被一对一的映射为本地操作系统的线程。`Java` 线程启动时会创建一个本地操作系统线程，当该 `Java` 线程终止时，这个操作系统线程也会被回收。操作系统会调度所有线程并将它们分配给可用的 `CPU`。

在上层，`Java` 多线程程序通常把应用分解为若干个任务，然后使用用户级的调度器（`Executor` 框架） 将这些任务映射为固定数量的线程，在底层，操作系统内核将这些映射到硬件处理器上。

#### Executor 框架的结构与成员

1）Executor 框架的结构

- 任务

  包括被执行任务需要实现的接口：`Runnable`、`Callable`。

- 任务的执行

  包括任务执行机制的核心接口 `Executor`，以及继承自 `Executor` 的 `ExecutorService` 接口。`Executor` 框架有两个关键类实现了 `ExecutorService`接口（`ThreadPoolExecutor`和`ScheduledThreadPoolExecutor`）。

- 异步计算的结果

  包括接口 `Future` 和实现 `Future` 接口的 `FutureTask` 类。

2）类接口简介

- `Executor` 接口

  它是 `Executor` 框架的基础，它将任务的提交与任务的执行分离开来。

- `ThreadPoolExecutor`

  是线程池的核心实现类，用来执行被提交的任务。

- `ScheduledThreadPoolExecutor`

  可以在给定的延迟后运行命令，或者定期执行命令。`ScheduledThreadPoolExecutor` 比 `Timer` 更加灵活，功能更加强大。

- `Future` 接口及其实现类 `FutureTask`

  代表异步运算的结果。

- `Runnable` 接口及 `Callable` 的实现类

  都可以被 `ThreadPoolExecutor`、`ScheduledPoolExecutor` 执行。

主线程首先创建实现 `Runnable` 或者 `Callable` 接口的任务对象。工具类 `Executors` 可以把一个 `Runnable` 对象封装为 `Callable` 对象。然后可以把 `Runnable` 对象直接交给 `ExecutorService` 执行（`ExecutorService.execute(Runnable command)`），或者也可以把 `Runnable` 对象或 `Callable` 对象提交给 `ExecutorService` 执行（`ExecutorService.submit(Runnable task)`）或 `ExecutorService.submit(Callable<T> task)`。

如果执行 `ExecutorService.submit(...)`，`ExecutorService` 将返回一个实现 `Future` 接口的对象。由于 `FutureTask` 实现了`Runnable` ，程序员也可以创建 `FutureTask`，然后交给`ExecutorService` 执行。

最后，主线程可以执行 `FutureTask.get()` 方法来等待任务执行完成。主线程也可以执行 `FutureTask.cancel(boolean mayInterruptIfRunning)` 来取消此任务。

2）`Executor`  框架的成员

主要成员：`ThreadPoolExecutor`、`ScheduledThreadPoolExecutor`、`Future`接口、`Runnable` 接口、`Callable` 接口和 `Executors`。

- `ThreadPoolExecutor`

  `ThreadPoolExecutor` 通常使用工厂类 `Executors` 来创建。`Executors` 可以创建 3 种类型的 `ThreadPoolExecutor` ：`SingleThreadExecutor`、`FixedThreadPool`、`CachedThreadPool`。

  - `FixedThreadPool`

    下面是 `Executors` 提供的创建固定数量线程数的 `FixedThreadPool`。

    ```java
    public static ExecutorService newFixedThreadPool(int nThreads){...}
    public static ExecutorService newFixedThreadPool(int nThreads, ThreadFactory threadFactory) {...}
    ```

    `FixedTThreadPool` 适用于为了满足资源管理的需求，而需要限制当前线程数量的应用场景，它适用于负载比较重的服务器。

  - `SingleThreadExecutor`

    下面是 `Executors` 提供的创建单个线程的`SingleThreadExecutor`。

    ```java
    public static ExecutorService newSingleThreadExecutor() {...}
    public static ExecutorService newSingleThreadExecutor(ThreadFactory threadFactory) {...}
    ```

    `SingleThreadExecutor` 适用于需要保证顺序执行各个人物的场景，并且在任意时间点，不会有多个线程是活动的应用场景。

  - `CachedThreadPool`

    下面是 `Executors` 提供的创建一个根据需要创建新线程的 `CachedThreadPool`。

    ```java
    public static ExecutorService newCachedThreadPool() {...}
    public static ExecutorService newCachedThreadPool(ThreadFactory threadFactory) {...}
    ```

    `CachedThreadPool` 是大小无界的线程池，适用于执行很多的短期异步任务的小程序，或者是负载较轻的服务器。

- `ScheduledThreadPoolExecutor`

  `ScheduledThreadPoolExecutor` 通常使用工厂类 `Executors` 来创建。`Executors` 可以创建两种此类型的实例：

  - `ScheduledThreadPoolExecutor`

    包含若干个线程的 `ScheduledThreadPoolExecutor`。

    ```java
    public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize) {...}
    public static ScheduledExecutorService newScheduledThreadPool(
                int corePoolSize, ThreadFactory threadFactory)  {...}
    ```

    `ScheduledThreadPoolExecutor` 适用于需要多个后台线程执行周期任务，同时为了满足资源管理的需求限制后台线程的数量的应用场景。

  - `SingleThreadScheduledExecutor`

    只包含一个线程的 `ScheduledThreadPoolExecutor`。

    ```java
    public static ScheduledExecutorService newSingleThreadScheduledExecutor() {...}
    public static ScheduledExecutorService newSingleThreadScheduledExecutor(ThreadFactory threadFactory) {...}
    ```

    `SingleThreadScheduledExecutor` 适用于需要单个后台线程执行周期任务，同时 需要保证顺序的执行各个人物的应用场景。

- `Future` 接口

  `Future` 接口和实现 `Future` 接口的 `FutureTask` 类用来表示异步计算的结果。当我们把 `Runable` 接口或 `Callable` 接口的实现类提交给 `ThreadPoolExecutor` 或 `ScheduledThreadPoolExecutor` 时，`ThreadPoolExecutor` 或 `ScheduledThreadPoolExecutor` 会向我们返回一个 `FutureTask` 对象：

  ```java
  <T> Future<T> submit(Callable<T> task)
  <T> Future<T> submit(Runnable task, T result)
  Funture<> submit(Runnable task)
  ```

  到 `JDK 8` 为止，`Java` 通过上诉 `API` 返回的是一个 `FutureTask` 对象。但从 `API` 可以看到，`Java` 仅仅保证返回的是一个实现了 `Future` 接口的对象。

- `Runnable` 接口和 `Callable` 接口

  `Runnable` 接口和 `Calalble` 接口的实现类，都可以被 `ThreadPoolExecutor` 或 `ScheduledThreadPoolExecutor` 执行。它们之间的区别是 `Runnable` 不会返回结果，而 `Callable` 可以返回结果。还可以使用 `Executors` 把 `Runnable` 包装成一个 `Callable`。

  ```java
  public static <T> Callable<T> callable(Runnable task, T result)
  public static Callable<Object> callable(Runnable task)
  ...
  ```

### ThreadPoolExecutor 详解

`Executor` 框架的最核心的类是 `ThreadPoolExecutor`，它是线程池实现类：

- `corePool`

  核心线程池的大小。

- `maximumPool`

  最大线程池的大小。

- `BlockingQUeue`

  用来暂时保存任务的工作队列。

- `RejectedExecutionHandler`

  当`ThreadPoolExecutos` 已经关闭或 `ThreadPoolExecutor` 已经饱和时（达到了最大线程池大小且工作队列已满），`execute` 方法将要调用的 `Handler`。

通过 `Executor` 框架的工具类 `Executors` 可以创建 3 种类型的 `ThreadPoolExecutor`：

- `FixedThreadPool`
- `SingleThreadExecutor`
- `CachedThreadPool`

#### FixedThreadPool

`FixedThreadPool` 被称为可重用固定线程数的线程池。`FixedThreadPool` 的 `corePoolSize` 和 `maximumPoolSize` 都被设置为创建 `FixedThreadPool` 时指定的参数 `nThreads`。

当线程池中的线程数大于 `corePoolSize` 时，`keepAliveTime` 为多余的空闲线程等待新任务的最长时间，超过这个时间后多余的线程将被终止。

`FixedThreadPool` 的 `execute` 方法的运行：

1）如果当前运行的线程数少于 `corePoolSize`，则创建新线程来执行任务。

2）在线程池完成预热之后（当前运行的线程数等于 `corePoolSize`），将任务加入 `LinkedBlockingQueue`。

3）线程执行完任务后会循环反复从 `LinkedBlockingQueue` 中获取任务来执行。

`FixedThreadPool` 使用无界队列 `LinkedBlockingQueue` 作为线程池的工作队列，有如下影响：

1）当线程池的线程数达到 `corePoolSize` 后，新任务将在无界队列中等待，因此线程池中的线程数不会超过 `corePoolSize`。

2）`maxumimPoolSize` 是个无效参数。

3）`keepAliveTime` 是个无效参数。

4）运行中 `FixedThreadPool` 不会拒绝任务。

#### SingleThreadExecutor

`SingleThreadExecutor` 是使用单个 `worker` 线程的 `Executor`。`SingleThreadExecutor` 的 `corePoolSize` 和 `maximumPoolSize` 被设置为 1。 其他参数与 `FixedThreadPool` 相同。 `SingleThreadExecutor` 使用无界队列 `LinkedBlockingQueue` 作为线程池的工作队列（ 队列的容量为 `Integer.MAX_ VALUE`）。

1）如果当前运行的线程数少于 `corePoolSize`（ 即线程池中无运行的线程），则创建一个新线程来执行任务。 

2）在线程池完成预热之后（当前线程池中有一个运行的线程），将任务加入 `Linked-BlockingQueue`。

3）线程执行完 1 中的任务后，会在一个无限循环中反复从 `LinkedBlockingQueue` 获取任务来执行。

#### CachedThreadPool

`CachedThreadPool` 是一个会根据需要创建新线程的线程池。`CachedThreadPool` 的 `corePoolSize` 被设置为 0， 即 `corePool` 为空；`maximumPoolSize` 被设置为 `Integer.MAX_ VALUE`，即 `maximumPool` 是无界的。 这里把 `keepAliveTime` 设置为 60L， 意味着 `CachedThreadPool` 中的空闲线程等待新任务的最长时间为 60 秒，空闲线程超过 60 秒后将会被终止。`FixedThreadPool` 和 `SingleThreadExecutor` 使用无界队列 `LinkedBlockingQueue` 作为线程池的工作队列。`CachedThreadPool` 使用没有容量的 `SynchronousQueue` 作为现成吃的工作队列，但 `CachedThreadPool` 的 `maximumPool` 是无界的。这意味着，如果主线程提交任务的速度高于 `maximumPool` 中线程处理任务的速度时，`CachedThreadPool` 会不断创建新线程。极端情况下，`CachedThreadPool` 会因为创建过多线程而耗尽 `CPU` 和内存资源。

1）首先执行 `SynchronousQueue.offer(Runnable task)`。 如果当前 `maximumPool` 中有空闲线程正在执行`SynchronousQueue.poll(keepAliveTime，TimeUnit.NANOSECONDS)`，那么主线程执行 `offer` 操作与空闲线程执行 的 `poll` 操作配对成功，主线程把任务交给空闲线程执行，`execute()` 方法执行完成；否则执行下面的步骤 2）。

2）当初始 `maximumPool` 为空，或者`maximumPool` 中当前没有空闲线程时，将没有线程执行` SynchronousQueue.poll(keepAliveTime，TimeUnit.NANOSECONDS)`。 这种情况下，步骤 1）将失败。此时 `CachedThreadPool` 会创建一个新线程执行任务，`execute()`方法执行完成。 

3）在步骤 2）中新创建的线程将任务执行完后，会执行 `SynchronousQueue.poll(keepAliveTime，TimeUnit.NANOSECONDS)`。这个 `poll` 操作会让空闲线程最多在 `SynchronousQueue` 中等待 60 秒钟。如果 60 秒钟内主线程提交了一个新任务（主线程执行步骤 1）），那么这个空闲线程将执行主线程提交的新任务；否则，这个空闲线程将终止。由于空闲 60 秒的空闲线程会被终止，因此长时间保持空闲的 `CachedThreadPool` 不会使用任何资源。

前面提到过，`SynchronousQueue` 是一个没有容量的阻塞队列。 每个插入操作必须等待另一个线程的对应移除操作，反之亦然。`CachedThreadPool` 使用 `SynchronousQueue`，把主线程提交的任务传递给空闲线程执行。

### ScheduledThreadPoolExecutor

`ScheduledThreadPoolExecutor` 继承自 `ThreadPoolExecutor`。它主要用来在给定的延迟之后运行任务，或者定期执行任务。`ScheduledThreadPoolExecutor` 的功能与 `Timer` 类似，但是`ScheduledThreadPoolExecutor` 更加强大、灵活。

#### ScheduledThreadPoolExecutor 的运行机制

`DepayQueue` 是一个无界队列，所有 `ThreadPoolExecutor` 的 `maximumPoolSize` 在 `ScheduledThreadPoolExecutor` 没有什么意义。`ScheduledThreadPoolExecutor` 的执行主要分为两大部分：

1）当调用 `ScheduledThreadPoolExecutor` 的 `scheduleAtFixedRate` 方法或者 `scheduleWithFixedDelay` 方法时，会向 `ScheduledThreadPoolExecutor`  的 `DelayQueue` 添加一个实现了 `RunnableScheduledFuture` 接口的 `ScheduledFutureTask`。

2）线程池中的线程从 `DelayQueue` 中获取 `ScheduledFutureTask`，然后执行任务。

`ScheduledThreadPoolExecutor` 为了实现周期性的执行任务，对 `ThreadPoolExecutor` 做了如下修改：

1）使用 `DelayQueue` 作为任务队列。

2）获取任务的方式不同。

3）执行周期任务后，增加了额外的处理。

#### ScheduledThreadPoolExecutor 的实现

`ScheduledThreadPoolExecutor` 会把待调度的任务放到 `DelayQueue` 中，`ScheduledFutureTask` 主要包含 3 个成员变量：

1）`long` 型成员变量 `time`，表示这个任务将要被执行的具体时间。

2）`long` 型成员变量 `sequenceNumber`，表示这个任务被添加到 `ScheduledThreadPoolExecutor` 中的序号。

3）`long` 型成员变量 `period`，表示任务执行的间隔周期。

`DelayQueue` 封装了一个 `PriorityQueue`，这个 `PriorityQueue` 会对队列中的 `ScheduledFutureTask` 进行排序。排序时，`time` 小的排在前面。如果两个 `ScheduledFutureTask` 的 `time` 相同，就比较 `sequenceNumber`，`sequenceNumber` 小的排在前面。执行周期任务的 4 个步骤：

1）线程从 `DelayQueue` 中获取已到期的 `ScheduledFutureTask`。但其任务是指 `ScheduledFutureTask` 的 `time` 大于等于当前时间。

2）执行这个 `ScheduledFutureTask`。

3）线程修改 `ScheduledFutureTask` 的 `time` 变量为下次将要被执行的时间。

4）线程把这个修改 `time` 之后的 `ScheduledFutureTask` 放回 `DelayQueue` 中。

获取任务的 3 大步骤：

1）获取 `Lock`。

2）获取周期任务。

- 如果 `PriorityQueue` 为空，当前线程到 `Condition` 中等待。否则执行下一步。
- 如果 `PriorityQueue` 的头元素的 `time` 时间比当前时间大，到 `Condition` 中等待到 `time` 时间，否则执行下一步。
- 获取 `PriorityQueue` 的头元素，如果 `PriorityQueue` 不为空，则唤醒在 `Condition` 中等待的所有线程。

3）释放 `Lock`

`ScheduledThreadPoolExecutor` 在一个循环中执行 2），知道线程从 `PriorityQueue` 获取到一个元素之后，才会退出无限循环。

添加任务的 3 大步骤：

1）获取 `Lock`。

2）添加任务。

- 向 `PriorityQueue` 添加任务。
- 如果上面添加的任务是 `PriorityQueue` 的头元素，唤醒在 `Condition` 中等待的所有线程。

3）释放 `Lock`。

### FutureTask 详解

`Future` 接口和实现 `Future` 接口的 `FutureTask` 类，代表异步计算的结果。

#### FutureTask 简介

`FutureTask` 除了实现 `Future` 接口外，还实现了 `Runnable` 接口。因此，`FutureTask` 可以交给 `Executor` 执行，也可以由调用线程直接执行。根据 `FutureTask.run()` 的执行时机，`FutureTask` 可以处于下面三种状态：

1）未启动

`FutureTask.run()` 方法还没有被执行之前，`FutureTask` 处于未启动状态。当创建一个 `FutureTask`，且没有执行 `FutureTask.run()` 方法之前，这个 `FutureTask` 处于未启动状态。

2）已启动

`FutureTask.run()` 方法被执行的过程中，`FutureTask` 处于已启动状态。

3）已完成

`FutureTask.run()` 方法执行完后正常结束，或被取消（`FutureTask.cancel(..)`），或执行 `FutureTask.run()` 方法时抛出异常而结束，`FutureTask` 处于已完成状态。



当 `FutureTask` 处于未启动或已启动状态时，执行 `FutureTask.get()` 方法将导致调用线程阻塞，当 `FutureTask` 处于已完成状态时，执行 `FutureTask.get()` 方法将导致调用线程立即返回结果或抛出异常。

当 `FutureTask` 处于未启动状态时，执行 `FutureTask.cacel()` 方法将导致此任务永远不会被执行，当 `FutureTask` 处于已启动状态时，执行 `FutureTask.cancel(true)` 方法将以中断执行此任务线程的方式来试图停止任务，当 `FutureTask` 处于已启动状态时，执行 `FutureTask.cancel(false)` 镜怒会对正在执行此任务的线程产生影响，当 `FutureTask` 处于已完成状态时，执行 `FutureTask.cancel(...)` 将会返回 `false`。

#### FutureTask 的使用

可以把 `FutureTask` 交给 `Executor` 执行，也可以通过 `ExecutorService.submit(...)` 方法返回一个 `FutureTask`，然后执行 `FutureTask.get()` 方法或 `FutureTask.cancel(...)` 方法，除此以外还可以单独使用 `FutureTask`。

当一个线程需要等待另一个线程把某个人物执行完成之后它才能继续执行，此时可以使用 `FutureTask`。

#### FutureTask 的实现

`FutureTask` 的实现基于 `AbstractQueuedSynchronizer(AQS)`。`concurrent` 包下很多阻塞类都是基于 `AQS` 来实现的。`AQS` 是一个同步框架，它提供通用机制来原子性管理同步状态、阻塞和唤醒线程，以及维护被阻塞线程的队列。每一个基于 `AQS` 实现的同步器都会包含两种类型的操作：

1）至少一个 `acquire` 操作。这个操作阻塞调用线程，除非/ 直到 `AQS` 的状态允许这个线程继续执行。`FutureTask` 的 `acquire` 操作为 `get()/ get(...)` 方法调用。

2）至少一个 `release` 操作。这个操作改变 `AQS` 的状态，改变后的状态可允许一个或多个阻塞线程被解除阻塞。`FutureTask` 的 `release` 操作包括 `run` 方法和 `cancel(...)` 方法。

基于 “复合优先于继承” 的原则，`FutureTask` 声明了一个内部私有继承于 `AQS` 的子类 `Sync`，对 `FutureTask` 所有共有方法的调用会委托给这个内部子类。

`AQS` 被作为 “模板方法模式” 的基础类提供给 `FutureTask` 的内部子类 `Sync`，这个内部子类只需要实现状态检查 和状态更新的方法即可，这些方法将控制 `FutureTask` 的获取和释放操作。