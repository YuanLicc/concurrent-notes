## Java 中的锁

#### Lock 接口

锁是用来控制多个线程访问共享资源的方式，一般来说，一个锁能够防止多个线程同时访问共享资源（有些锁可以允许多个线程并发的访问共享资源，比如读写锁）。在 `Lock` 接口出现之前，`Java` 程序是靠 `synchronized` 关键字实现锁功能的，在 `Java SE 5` 之后，并发包中新增了 `Lock` 接口及其实现类用来实现锁功能，它提供了与 `synchronized` 关键字类似的同步功能，只是使用时需要显示的获取和释放锁。虽然它缺少了隐式获取释放锁的便捷性，但是却拥有了锁获取与释放的可操作性、可中断的获取锁以及超时获取锁等多种 `synchronized` 关键字所不具备的同步特性。

使用 `synchronized` 关键字将会隐式地获取锁，但是它将锁的获取和释放固化了，也就是先获取再释放。当然，这种方式简化了同步的管理，可是扩展性没有显示的锁获取和释放来的好，如：我们需要在释放锁之前获取另一个锁。

`Lock` 的使用：

```java
Lock lock = new ReetrantLock();

lock.lock();

try{
    //
}
finally{
    lock.unlock();
}
```

不要将获取锁的过程写在 `try` 块内，因为如果在获取锁（自定义锁）时发生异常，异常抛出的同时，也会导致锁无故释放。`Lock` 接口提供了一下特性：

| 特性             | 描述                                                         |
| ---------------- | ------------------------------------------------------------ |
| 尝试非阻塞获取锁 | 当前线程尝试获取锁，如果这一时刻锁没有被其它线程获取到，则成功获取并持有锁 |
| 能被中断的获取锁 | 与`synchronized` 不同，获取锁的线程能够响应中断，当获取到锁的线程被中断时，中断异常将会被抛出，同时锁会被释放 |
| 超时获取锁       | 在指定的截止时间之前获取锁，如果截止时间到了仍旧未获取锁，则返回 |

`Lock` 接口：

| 方法               | 描述                                                         |
| ------------------ | ------------------------------------------------------------ |
| lock               | 获取锁，调用该方法当前线程会获取锁，当锁获得后，从该方法返回 |
| lockInterruptibly  | 可中断的获取锁，和 `lock` 方法的不同之处在于该方法会响应中断，即在锁的获取中可以中断当前线程 |
| tryLock            | 尝试非阻塞的获取锁，调用该方法后立即返回，如果能够获取则返回 `true`，否则返回`false` |
| tryLock(time,unit) | 超时的获取锁，当前线程在以下三种情况下会返回：<br />1）当前线程在超时时间内获得了锁<br />2）当前线程在超时时间内被中断<br />3）超时时间结束，返回 `false` |
| unlock             | 释放锁                                                       |
| newCondition       | 获取等待通知组件，该组件和当前的锁绑定，当前线程只有获得了锁，才能调用该组件的 `wait` 方法，调用后，当前线程将释放锁 |

### 队列同步器

队列同步器 `AbstractQueuedSynchronized`，是用来构建锁或者其它同步组件的基础框架，它使用了一个 `int` 成员变量表示同步状态，通过内置的 `FIFO` 队列来完成资源获取线程的排队工作。同步器的主要使用方式是继承，子类通过集成同步器并实现它的抽象方法来管理同步状态，在抽象方法的实现过程中免不了要对同步状态进行更改，这时就需要使用同步器提供的三个方法：`getState`、`setState`、`compareAndSetState` 来进行操作，因为他们能够保证状态的改变是安全的，同步器的子类应该定义为自定义同步组件的静态内部类，同步器自身没有实现任何同步接口，它仅仅是定义了若干同步状态获取和释放的方法来供自定义同步组件使用，同步器既可以支持独占式地获取同步状态，也可以支持共享式的获取同步状态，这样可以方便实现不同类型的同步组件。

同步器是实现锁的关键，在锁的实现中聚合同步器，利用同步器实现锁的语义。二者的关系：锁是面向使用者的，它定义了使用者与锁交互的接口，隐藏了实现细节。同步器面向的是锁的实现者，它简化了锁的实现方式，屏蔽了同步状态管理、线程的排队、等待与唤醒等底层操作。锁和同步器很好的隔离了使用者和实现者所需关注的领域。

#### 队列同步器接口

同步器的设计时基于模板方法模式的，也就是说需要集成同步器并重写指定的方法，随后将同步器组合在自定义同步组件的视线中，并调用同步器提供的模板方法，而这些模板方法将会调用使用者重写的方法。

重写同步器指定的方法时，需要使用同步器提供的三个方法来访问或修改同步状态：

1）`getState()` ：获取当前同步状态。

2）`setState(int newState)`：设置当前同步状态。

3）`compareAndSetState(int expect, int update)`：使用 `CAS` 设置当前状态，该方法能够保证状态设置的原子性。

同步器可重写的方法：

| 方法名称                    | 描述                                                         |
| --------------------------- | ------------------------------------------------------------ |
| `tryAcquire(int arg)`       | 独占式获取同步状态，实现该方法需要查询当前状态并判断同步状态是否符合预期，然后再进行 `CAS` 设置同步状态。 |
| `tryRelease(int arg`)`      | 独占式释放同步状态，等待获取同步状态的线程将有机会获取同步状态。 |
| `tryAcquireShared(int arg)` | 共享式获取同步状态，返回大于等于 0 的值，表示获取成功，反之表示获取失败。 |
| `tryReleaseShared(int arg)` | 共享式释放同步状态。                                         |
| `isHeldExclusively()`       | 当前同步器是否在独占墨水下被线程占用，一般该方法表示是否被当前线程所独占。 |

实现自定义同步组件时，将会调用同步器提供的模板方法：

| 方法名称                                     | 描述                                                         |
| -------------------------------------------- | ------------------------------------------------------------ |
| `acquire(int arg)`                           | 独占式获取同步状态，如果当前线程获取同步状态成功，则由该方法返回，否则，将会进入同步队列等待，该方法将会调用重写的 `tryAcquire` 方法。 |
| `acquireInterruptibly(int arg)`              | 与 `acquire(int arg)` 相同，但是该方法响应中断，当前线程未获取到同步状态而进入同步队列中，如果当前线程被中断，则该方法会抛出`InterruptedException` 并返回。 |
| `tryAcquireNanos(int arg, long nanos)`       | 在 `acquireInterruptibly(int arg)` 基础上增加了超时限制，如果当前线程在超时时间内没有获取到同步状态，那么将会返回 `false`，反之返回 `true`。 |
| `acquireShared(int arg)`                     | 共享式获取同步状态，如果当前线程未获取到同步状态，将会进入同步队列等待，与独占锁获取的主要区别是同一时刻可以有多个线程获取到同步状态。 |
| `acquireSharedInterruptibly(int arg)`        | 与 `acquireShared(int arg)` 相同，该方法响应中断。           |
| `tryAcquireSharedNanos(int arg, long nanos)` | 在 `acquireSharedInterruptibly(int arg)` 基础上增加了超时限制。 |
| `release(int arg)`                           | 独占式的释放同步状态，该方法会在释放同步状态之后，将同步队列中第一个节点包含的线程唤醒。 |
| `releaseShared(int arg)`                     | 共享式的释放同步状态。                                       |
| `getQueuedThreads()`                         | 获取等待在同步队列上的线程集合。                             |

同步器提供的模板方法基本上分为三类：独占式获取与释放同步状态、共享式获取与释放同步状态和查询同步队列中的等待线程情况。自定义同步组件将使用同步器提供的模板方法来实现自己的同步语义。下面介绍独占锁来加强对同步器工作原理的理解：

独占锁是同一时刻只能一个线程获取到锁，而其他获取锁的线程只能同步队列中等待，只有获取锁的线程释放了锁，后继的线程才能够获取锁。

```java
public class Mutex implements Lock {

    private static class Sync extends AbstractQueuedSynchronizer {

        // 是否处于占用状态
        protected boolean isHeldExclusively() {
            return getState() == 1;
        }

        // 当状态为 0 的时候获取锁
        public boolean tryAcquire(int acquires) {
            if (compareAndSetState(0, 1)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }

        // 释放锁，将状态设置为 0
        protected boolean tryRelease(int releases) {
            if (getState() == 0)
                throw new IllegalMonitorStateException();
            setExclusiveOwnerThread(null);
            setState(0);
            return true;
        }

        // 返回一个 Condition，每个condition 都包含了一个 condition 队列
        Condition newCondition() {
            return new ConditionObject();
        }
    }

    // 仅需要将操作代理到 Sync 上即可
    private final Sync sync = new Sync();

    public void lock() {
        sync.acquire(1);
    }

    public boolean tryLock() {
        return sync.tryAcquire(1);
    }

    public void unlock() {
        sync.release(1);
    }

    public Condition newCondition() {
        return sync.newCondition();
    }

    public boolean isLocked() {
        return sync.isHeldExclusively();
    }

    public boolean hasQueuedThreads() {
        return sync.hasQueuedThreads();
    }

    public void lockInterruptibly() throws InterruptedException {
        sync.acquireInterruptibly(1);
    }

    public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
        return sync.tryAcquireNanos(1, unit.toNanos(timeout));
    }
}
```

上面的例子中，独占锁 `Mutex` 是一个自定义同步组件，它在同一时刻只允许一个线程占有锁。`Mutex` 中定义了一个静态内部类，该内部类集成了 `AQS` 并实现了独占式获取和释放同步状态。在 `tryAcquire(int acquires)` 方法中，如果经过 `CAS` 设置成功，则表示获取了同步状态，而在 `tryRelease(int release)`方法中只是将同步状态重置为 0。用户使用 `Mutex` 时并不会直接和内部同步器的实现交互，而是调用 `Mutex` 通过的方法，在 `Mutex` 的实现中，以获取锁的 `lock` 方法为例，只需要在方法实现中调用同步器的模板方法 `acquire(int args)` 即可，当前线程调用该方法获取同步状态失败后会被加入到同步队列中等待。

#### 队里同步器的实现分析

同步队列、独占式同步状态获取与释放、共享式同步状态获取与释放以及超时获取同步状态的核心数据结构与模板方法：

1）同步队列

同步器依赖内部的同步队列来完成同步状态的管理，当前线程获取同步状态失败时，同步器会将当前线程以及等待状态等信息构造成一个节点并将其加入到同步队列，同时会则色当前线程，当同步状态释放时，会把首节点中的线程唤醒，使其再次尝试获取同步状态。同步队列中的节点用来保存获取同步状态失败的线程引用、等待状态以及前驱和后继节点。节点属性：

| 属性         | 描述                                                         |
| ------------ | ------------------------------------------------------------ |
| `waitStatus` | 等待状态，包含：<br />1）`CANCELLED`，值为 1，由于在同步队列中等待的线程等待超时或者被中断，需要从同步队列中取消等待，节点进入该状态将不会变化。<br />2）`SINGAL`，值为 -1，后继节点的线程处于等待状态，而当前节点的线程如果释放了同步状态或被取消，将会通知后继节点，使后继节点的线程得以运行。<br />3）`CONDITION`，值为 -2，节点在等待队列中，节点线程等待在 `Condition` 上，当其他线程对 `Condition` 调用了 `signal` 方法后，该节点将会从等待队列中转移到同步队列中，加入到对同步状态的获取中。<br />4）`PROPAGATE`，值为 -3，表示下一次共享式同步状态获取将会无条件的被传播下去。<br />5）`INITIAL`，值为 0，初始状态。 |
| `prev`       | 前驱节点，当节点加入同步队列时被设置。                       |
| `next`       | 后继节点。                                                   |
| `nextWaiter` | 等待队列中的后继节点，如果当前节点是共享的，那么这个字段将是一个 `SHRAED` 常量，也就是说节点类型（独占和共享）和等待队列中的后继节点公用一个字段。 |
| `thread`     | 获取同步状态的线程。                                         |

节点构成同步队列的基础，同步器拥有首节点和尾节点，没有成功获取同步状态的线程将会成为节点加入该队列的尾部。当一个线程成功的获取了同步状态，其它线程将无法获取到同步状态，转而被构造成为节点并加入到同步队列中，而这个加入队列的过程必须要保证线程安全，因为同步器提供了一个基于 `CAS` 的设置尾节点的方法：

`compareAndSetTail(Node expect, Node update)`，它传递当前下次呢很难过的尾节点和当前节点，只有设置成功后，当前节点才正式与之前的尾节点建立联系。

同步队列遵循先进先出，首节点是获取同步状态成功的节点，首节点的线程在释放同步状态时，将会唤醒后继节点，而后继节点将会在获取同步状态成功时将自己设置为首节点。

设置首节点是通过获取同步状态成功的线程来完成的，由于只有一个线程能够成功获取到同步状态，因此设置头节点的方法并不需要使用 `CAS`来保证，它只需要将首节点设置成为原首节点的后继节点并断开原首节点的 `next` 引用即可。

2）独占式同步状态获取与释放

通过调用同步器的 `acquire(int arg)` 方法可以获取同步状态，该方法对中断不敏感，也就是由于线程获取同步状态失败后进入同步队列中，后续对线程进行中断操作时，线程不会从同步队列中移出。

```java
public final void acquire(int arg) {
    if(!tryAcquire(arg) && acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
        selfInterrupt();
}
```

首先调用自定义同步器实现的 `tryAcquire(int arg)` 方法，该方法保证线程安全的获取同步状态，如果同步状态获取失败，则构造同步节点并通过 `addWaiter(..)` 方法将节点加入到同步队列的尾部，最后调用 `acquireQueued` 方法，使得该节点以死循环的方式获取同步状态。如果获取不到则阻塞节点中的线程，而被阻塞线程的唤醒主要依靠前驱节点的出队或阻塞线程被中断来实现。

```java
private Node addWaiter(Node mode) {
    Node node = new Node(Thread.currentThread(), mode);
    // Try the fast path of enq; backup to full enq on failure
    Node pred = tail;
    if (pred != null) {
        node.prev = pred;
        if (compareAndSetTail(pred, node)) {
            pred.next = node;
            return node;
        }
    }
    enq(node);
    return node;
}

private Node enq(final Node node) {
    for (;;) {
        Node t = tail;
        if (t == null) { // Must initialize
            if (compareAndSetHead(new Node()))
                tail = head;
        } else {
            node.prev = t;
            if (compareAndSetTail(t, node)) {
                t.next = node;
                return t;
            }
        }
    }
}
```

在 `enq` 方法中，同步器通过死循环来保证节点的正确添加，在死循环中通过 `CAS` 将节点设置成为尾节点之后，当前线程才能从该方法返回，否则当前线程不断的尝试设置。

节点进入同步队列后，进入了一个自旋的过程，每个节点都在自省的观察，当条件满足时，获取到了同步状态，就可以从自旋中退出，否则依旧留在这个自旋过程中。