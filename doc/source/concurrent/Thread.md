## Thread

注释：

- 线程有优先级，Java 中优先级高的线程会比优先级低的线程先执行。
- 线程可以被标记为守护线程。
- 当在某个线程中执行的代码创建一个新的 Thread 对象时，新线程的优先级就等于设置的优先级，且新线程只有被设置为守护线程才是守护线程。
- 当 JVM 启动时，