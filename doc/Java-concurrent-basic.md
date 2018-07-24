## Java 并发编程基础

`Java` 从诞生开始就明智的选择了内置对多线程的支持，这使得 `Java` 语言相比同一时期的其它语言具有明显的优势。线程作为操作系统调度的最小单元，多个线程能够同时执行，这将显著的提升程序的性能，在多核环境中表现得更加明显。但是，过多的创建线程和对线程的不当管理也容易造成问题。

#### 什么是线程

现代操作系统在运行一个程序时，会为其创建一个进程。现代操作系统系统调度的最小单元是线程，也叫轻量级进程，在一个进程里可以创建多个线程，这些线程拥有各自的计数器、堆栈和局部变量等属性，并且能够访问共享的内存变量。处理器在这些线程上高速切换，让使用者感觉线程是同时执行的。

`Java` 程序从 `main` 方法开始执行，然后按照既定的代码逻辑执行，看似没有其它线程参与，但实际上 `Java` 程序天生就是多线程程序，因为执行 `main` 方法的是一个名为 `main` 的线程。下面通过线程管理 MXBean 获取线程信息：

```java
public static void main(String[] args) {
    ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    Arrays.stream(threadMXBean.getAllThreadIds()).forEach(
        id -> {
            ThreadInfo threadInfo = threadMXBean.getThreadInfo(id);
            System.out.println("Thread Id: " + id + ", Thread Name: " + threadInfo.getThreadName());
        }
    );
}
```

```java
Thread Id: 6, Thread Name: Monitor Ctrl-Break      // 
Thread Id: 5, Thread Name: Attach Listener         // 
Thread Id: 4, Thread Name: Signal Dispatcher       // 分发处理发送 JVM 信号的线程
Thread Id: 3, Thread Name: Finalizer               // 调用对象 finalize 方法的线程
Thread Id: 2, Thread Name: Reference Handler       // 清除 Reference 的线程
Thread Id: 1, Thread Name: `main`                    // `main` 线程，用户程序入口
```

#### 为什么使用多线程

1）更多的处理器核心

随着处理器上的核心数量的增加，以及超线程技术的广泛应用，现在大多数计算机都比以往更加擅长并行计算，而处理器性能的提升方式也从更高的主频向更多的核心发展。线程是大多数操作系统调度的基本单元，一个程序作为一个进程来运行，程序运行过程能够创建多个线程，而一个线程在一个时刻只能运行在一个处理器核心上。若程序使用多线程技术，将计算逻辑分配到多个处理器核心上，就会显著的减少程序的处理时间。

2）更快的响应时间

使用多线程技术将数据一致性不强的操作派发给其它线程处理，可缩短响应时间，提升用户体验。

3）更好的编程模型

`Java` 为多线程编程提供了良好、考究并且一致的编程模型，使开发人员能够更加关注于问题的解决。一旦开发人员建立好了模型，稍作修改总是能够方便的映射到 `Java` 提供的多线程模型上。

#### 线程优先级

现代操作系统基本采用时分的形式调度运行的线程，操作系统分出一个个的时间片，线程会分配到若干时间片，当线程的时间片用完了就会发生线程调度，并等待着下次分配。线程分配到的时间片多少也决定了线程使用处理器资源的多少，而线程优先级就是决定线程需要多或者少分配一些处理器资源的线程属性。

在 `Java` 线程中，通过一个整型成员变量 `priority` 来控制优先级，优先级的范围从 1—10，在线程构建的时候可以通过 `setPriority `方法来修改优先级，默认优先级为 5，优先级高的线程分配时间片的数量要对于优先级低的线程。设置线程优先级时，针对频繁阻塞（休眠或者I/O操作）的线程需要设置较高优先级，而偏重计算（需要较多 CPU 时间或者偏运算）的线程则设置较低的优先级，确保处理器不会被独占。在不同的 JVM 以及操作系统上，线程规划存在差异，有些操作系统甚至会忽略线程优先级的设定。

#### 线程的状态

`Java` 线程在运行的声明周期中可能处于几种不同的状态，在给定的一个时刻，线程只能处于其中一个状态：

| 状态名称     | 说明                                                         |
| ------------ | ------------------------------------------------------------ |
| NEW          | 初识状态，线程被构建，但是还没有调用 `start` 方法            |
| RUNNABLE     | 运行状态，`Java` 线程将操作系统中的就绪和运行两种状态笼统地称作“运行中” |
| BLOCKED      | 阻塞状态，表示线程阻塞于锁                                   |
| WAITING      | 等待状态，表示线程进入等待状态，进入该状态表示当前线程需要等待其他线程做一些特定动作（通知或中断） |
| TIME_WAITING | 超时等待状态，该状态不同于 WAITING，它是可以再指定时间自行返回的 |
| TERMINATED   | 终止状态，表示当前线程已经执行完毕                           |

测试示例：

```java
public class BlockedRunnable implements Runnable {
    @Override
    public void run() {
        // 竞争 BlockedRunnable.class 的锁
        synchronized (BlockedRunnable.class) {
            // 无限循环造成一直持有 BlockedRunnable.class 锁，
            // 使得需要 BlockedRunnable.class 锁的线程阻塞
            while (true) {
                ThreadUtils.sleepSecond(100);
            }
        }
    }
}

public class TimeWaitingRunnable implements Runnable {

    private long sleepSecond;

    public TimeWaitingRunnable(long sleepTime) {
        this.sleepSecond = sleepTime;
    }

    @Override
    public void run() {
        // 休眠指定时间
        ThreadUtils.sleepSecond(sleepSecond);
    }
}

public class WaitingClassRunnable implements Runnable {
    @Override
    public void run() {
        synchronized (WaitingClassRunnable.class) {
            try {
                // 使得线程在 WaitingClassRunnable.class 上等待
                WaitingClassRunnable.class.wait();
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

public class ThreadUtils {

    public static void sleepSecond(long sleepSecond) {
        try {
            TimeUnit.SECONDS.sleep(sleepSecond);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void dumpThreadsInfo() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

        Arrays.stream(threadMXBean.getAllThreadIds()).forEach(
            id -> {
                ThreadInfo threadInfo = threadMXBean.getThreadInfo(id);
                System.out.print("Thread Info: " + threadInfo);
            }
        );
    }
}
```

测试代码：

```java
public class ThreadStateTest extends TestCase {

    public void testThreadState() {
        new Thread(new TimeWaitingRunnable(100), "Time waiting").`start`();
        new Thread(new WaitingClassRunnable(), "Waiting class").`start`();
        // 下面两个线程将造成一个阻塞，一个超时等待，
        // 因为其中一个线程获取到锁后另一个线程将不能获取锁，造成阻塞
        new Thread(new BlockedRunnable(), "Block one").`start`();
        new Thread(new BlockedRunnable(), "Block two").`start`();
        ThreadUtils.dumpThreadsInfo();
    }
}
```

打印结果：

```java
Thread Info: "Block two" Id=16 BLOCKED on `Java`.lang.Class@66a29884 owned by "Block one" Id=15

Thread Info: "Block one" Id=15 TIMED_WAITING

Thread Info: "Waiting class" Id=14 WAITING on `Java`.lang.Class@4769b07b

Thread Info: "Time waiting" Id=13 TIMED_WAITING
...
```

线程在自身的声明周期中，并不是固定地处于某个状态，而是随着代码的执行在不同的状态之间进行切换，下面是 `Java` 线程状态转变图：

![Alt `Java`线程状态转变图](https://github.com/YuanLicc/concurrent-notes/blob/master/doc/images/thread-stage.svg)

线程创建后，调用 `start` 方法开始运行。当线程执行 wait 方法之后，线程进入等待状态。进入等待状态的线程需要其它线程的通知才能返回运行态，而超时等待状态相当于在等待状态的基础上增加了超时限制，也就是超时时间到达时将会返回到运行态。当线程调用同步方法时，在没有获取锁的情况下，线程将会进入阻塞状态。线程在执行过 run 方法后将会进入终止状态。

`Java` 将操作系统中的运行和就绪状态合并为运行状态。阻塞状态是线程阻塞在进入 synchronized 关键字修饰的方法或代码块（获取锁）时的状态，但是阻塞在 `Java.concurrent` 包中 Lock 接口的线程状态却是等待状态，因为 `Java.concurrent` 包中 Lock 接口对于阻塞的实现均使用了 LockSupport 类中的方法。

#### Daemon 线程

Daemon 线程是一种支持型线程，因为它主要被用作程序中后台调度以及支持性工作。当一个虚拟机中不存在非 Daemon 线程的时候，`Java` 虚拟机将会退出。可以通过调用 Thread.setDaemon(true) 将线程设置为 Daemon 线程。Daemon 属性需要在启动线程之前设置，不能再启动县城之后设置。

Daemon 线程被用作完成支持性工作，但是在 `Java` 虚拟机退出时 Daemon 线程中 finally 块并不一定会执行。测试代码：

```java
public class DaemonThreadTest extends TestCase {

    static class FinallyRunnable implements Runnable {
        @Override
        public void run() {
            try {
                ThreadUtils.sleepSecond(100);
            }
            finally {
                System.out.println("FinallyRunnable finally block");
            }
        }
    }

    public void testDaemon() {
        Thread thread = new Thread(new FinallyRunnable(), "finallyRunnable");
        thread.setDaemon(true);
        thread.`start`();
    }
}
```

测试无任何输出。所以在构建 Daemon 线程时，不能依靠 finally 块中的内容来确保执行关闭或清理资源的逻辑。

### 启动和终止线程

通过调用线程的 `start` 方法进行启动，随着 run 方法的执行完毕，线程也随之终止。

#### 构造线程

在运行线程之前首先构造一个线程对象，线程对象在构造的时候需要提供线程所需要的属性，如线程所属的线程组、线程优先级、是否是 Daemon 线程等信息。当设置好这些信息，那么一个能够运行的线程对象就初始化好了，在堆内存中等待运行。下面截取 Thread 内的初始化方法作为示例：

```java
private void init(ThreadGroup g, Runnable target, String name,
                      long stackSize, AccessControlContext acc,
                      boolean inheritThreadLocals) {
        if (name == null) {
            throw new NullPointerException("name cannot be null");
        }

        this.name = name;

        Thread parent = currentThread();
        SecurityManager security = System.getSecurityManager();
        if (g == null) {
            /* Determine if it's an applet or not */

            /* If there is a security manager, ask the security manager
               what to do. */
            if (security != null) {
                g = security.getThreadGroup();
            }

            /* If the security doesn't have a strong opinion of the matter
               use the parent thread group. */
            if (g == null) {
                g = parent.getThreadGroup();
            }
        }

        /* checkAccess regardless of whether or not threadgroup is
           explicitly passed in. */
        g.checkAccess();

        /*
         * Do we have the required permissions?
         */
        if (security != null) {
            if (isCCLOverridden(getClass())) {
                security.checkPermission(SUBCLASS_IMPLEMENTATION_PERMISSION);
            }
        }

        g.addUn`start`ed();

        this.group = g;
        this.daemon = parent.isDaemon();
        this.priority = parent.getPriority();
        if (security == null || isCCLOverridden(parent.getClass()))
            this.contextClassLoader = parent.getContextClassLoader();
        else
            this.contextClassLoader = parent.contextClassLoader;
        this.inheritedAccessControlContext =
                acc != null ? acc : AccessController.getContext();
        this.target = target;
        setPriority(priority);
        if (inheritThreadLocals && parent.inheritableThreadLocals != null)
            this.inheritableThreadLocals =
                ThreadLocal.createInheritedMap(parent.inheritableThreadLocals);
        /* Stash the specified stack size in case the VM cares */
        this.stackSize = stackSize;

        /* Set thread ID */
        tid = nextThreadID();
    }
```

####启动线程

线程对象在初始化完成后，调用 `start` 方法就可以启动这个线程。线程 `start` 方法的含义是：当前线程（即 `parent` 线程）同步告知 `Java`虚拟机，只要线程规划器空闲，应立即启动调用 `start` 方法的线程。

#### 理解中断

中断可以理解为线程的一个标识位属性，它表示一个运行中的线程是否被其他线程进行了中断操作。中断好比其它线程对该线程打了个招呼，其它线程通过调用该线程的 `interrupt ` 方法对其进行中断操作。

线程通过检查自身是否被中断来进行响应，线程通过方法 `isInterrupted` 方法来进行判断是否被中断，也可以调用静态方法 `Thread.interrupted` 方法对当前线程的中断标识位进行复位。如果该线程已经处于终结状态，即使该线程被中断过，在调用该线程对象的 `isInterrupted` 方法时依旧会返回 `false`。

`Java API` 中可以看到很多声明抛出 `InterruptedException` 的方法，这些方法在抛出 `InterruptedException` 之前，Java 虚拟机会先将该线程的中断标识位清除，然后抛出 `InterruptedException`，此时调用 `isInterrupted` 方法将会返回 `false`。

#### 安全地终止线程

中断操作是一种简便的线程间交互方式，而这种交互方式最适合用来取消或停止任务。除了中断以外，还可以利用一个 `boolean` 变量来控制是否需要停止任务并终止该线程。

```java
public class SafeShutdownThread extends TestCase {

    static class Runner implements Runnable {
        private long count;
        private volatile boolean on = true;

        @Override
        public void run() {
            while(!Thread.currentThread().isInterrupted()
                && on) {
                count++;
            }
            System.out.println("count: " + count + " Thread id: " + Thread.currentThread().getId());
        }

        public void cancel() {
            this.on = false;
        }
    }

    public void testSafeShutdown() {
        Runner one = new Runner();
        Thread countThread = new Thread( one,"count");
        countThread.start();

        ThreadUtils.sleepSecond(1);
        countThread.interrupt();

        Runner two = new Runner();
        Thread cancelThread = new Thread(two, "cancel");
        cancelThread.start();

        ThreadUtils.sleepSecond(1);
        two.cancel();
    }
}
```

### 线程间通信

线程开始运行，拥有自己的栈空间，就如同一个脚本一样，按照既定的代码一步一步地执行，直到终止。但是，每个运行中的线程，如果仅仅是孤立的运行，那么没有一点儿价值，或者说价值很少，如果多个线程能够相互配合完成工作，这将带来巨大的价值。

#### volatile 和 synchronized 关键字

Java 支持多个线程同时访问一个对象或者对象的成员变量，由于每个线程可以拥有这个变量的拷贝（虽然对象以及成员变量分配的内存是在共享内存上的，但是每个执行的线程还是可以拥有一份拷贝，这样做的目的是加速程序的执行，这是现代多核处理器的一个显著特性），所以程序在执行过程中，一个线程看到的变量并不一定是最新的。

关键字 `volatile ` 可以用来修饰字段（成员变量），就是告知程序任何对该变量的访问均需要从共享内存中获取，而对它的改变必须同步刷新回共享内存，它能保证所有线程对变量访问的可见性。

关键字 `synchronized` 可以修饰方法或者以同步块的形式来进行使用，它主要确保多个线程在同一个时刻，只能有一个线程处于方法或者同步块中，它保证了线程对变量访问的可见性和排他性。任意线程访问同步代码块都需要首先获取锁，若失败，线程进入同步队列，线程状态变为`BLOCKED`。当获得了锁的线程释放了锁，则该释放操作唤醒阻塞在同步队列中的线程，使其重新尝试对监视器的获取。

#### 等待/ 通知机制

一个线程修改了一个对象的值，而另一个线程感知到了变化，然后进行相应的操作，整个过程开始于一个线程，而最终执行又是另一个线程。前者是生产者，后者就是消费者，这种模式隔离了 “做什么” 和 “怎么做”，在功能层面上实现了解耦，体系结构上具备良好的伸缩性，但是在 Java 语言中如何实现类似的功能呢？简单的办法是让消费者线程不断地循环检查变量是否符合预期：

```java
while(value != desire) {
    Thread.sleep(1000);
}
doSomething();
```

代码中当条件不满足时就睡眠一段时间，这样做的目的是防止过快的 “无效” 尝试，这种方式看似能够实现所需的功能，但存在如下问题：

1）难以确保及时性

在睡眠时，基本不消耗处理器资源，但是如果睡得过久，就不能及时发现条件已经变化，也就是及时性难以保证。

2）难以降低开销

如果降低睡眠的时间，会消耗更多的处理器资源。

Java 通过内置的等待/ 通知机制能够很好的解决这个矛盾并实现所需的功能。等待/ 通知的相关方法是任意 Java 对象都具备的，因为这些方法被定义在所有对象的超类 `Object` 上。

| 方法名称        | 描述                                                         |
| --------------- | ------------------------------------------------------------ |
| notify          | 通知一个再对象上等待的线程，使其从 wait 方法返回，而返回的前提是该线程获取到了对象的锁 |
| notifyAll       | 通知所有等待在该对象上的线程                                 |
| wait            | 调用该方法的线程进入 WAITING 状态，只有等待另外线程的通知或被中断才会返回，调用 wait 方法后，会释放对象的锁 |
| wait(long)      | 超时等待一段时间，这里的参数时间是毫秒，即等待多少毫秒，如果没有通知就超时返回 |
| wait(long, int) | 对于超时时间更细粒度的控制，可以达到纳秒                     |

等待/ 通知机制，是指一个线程 A 调用了对象 O 的 wait 方法进入等待状态，而另一个线程 B 调用了对象 O 的 notify 或者 notifyAll 方法，线程 A 收到通知后从对象 O 的 wait 方法返回，进而执行后续操作。