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

