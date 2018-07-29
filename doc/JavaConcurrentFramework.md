## Java 并发容器与框架

Java 程序员进行并发编程时，相比其它语言的程序员要倍感幸福，因为并发编程大师 `Doug Lea` 不遗余力的为 Java 开发者提供了非常多的并发容器和框架。

### ConcurrentHashMap 的实现原理及使用（JDK 1.7为准）

`ConcurrentHashMap` 是线程安全且高效的 `HashMap` 。

#### 为什么使用 ConcurrentHashMap

在并发编程中使用 `HashMap` 可能导致程序死循环。而使用线程安全的 `HashTable` 效率又非常低下，基于两个原因，`ConcurrentHashMap` 登场了。

1）线程不安全的 `HashMap`

在多线程环境下，使用 `HashMap` 进行 `put` 操作会引起死循环，导致 `CPU` 利用率接近 100%，所以在并发情况下，不能使用 `HashMap`。因为多线程会导致 `HashMap` 的 `Entry` 链表形成环形数据结构，一旦形成了环形数据结构，`Entry` 的 `next` 节点永远不为 `null`，就会产生死循环获取 `Entry`。

2）效率低下的 `HashTable`

`Hashtable` 容器使用 `synchronized` 来保证线程安全，但在线程竞争激烈的情况下，`HashTable` 的效率非常低下，因为当一个线程访问 `HashTable` 的同步方法时，其它线程访问 `HashTable` 的同步方法会进入阻塞或轮询状态。

3）`ConcurrentHashMap` 的锁分段技术可以有效提升并发访问率

`HashTable` 容器在竞争激烈的并发环境下表现出效率低下的原因是所有访问 `HashTable` 的线程都必须竞争同一把锁，假如容器有多把锁，每一把锁用于容器其中一部分数据，那么多线程访问容器里不同数据段的数据时，线程间就不会存在锁竞争，从而可以有效提高并发访问效率，这就是 `ConcurrentHashMap` 所使用的锁分段技术。首先将数据分成一段一段的存储，然后给每一段数据配一把锁，当一个线程占用锁访问其中一段数据时，其它段的数据也能被其它线程访问。

#### ConcurrentHashMap 的初始化

`ConcurrentHashMap` 的初始化方法是通过 `initialCapacity`、`loadFactory` 、`concurrencyLevel` 等几个参数来初始化 `segment` 数段偏移量 `segmentShift`、段掩码 `segmentMask` 和每个 `segment` 里的 `HashEntry` 数组来实现的。

1）初始化 `segments` 数组

```java
if(concurrencyLevel > MAX_SEGMENTS) {
    concurrencyLevel = MAX_SEGMENT;
    int sshift = 0;
    int ssize = 1; 
    while (ssize < concurrencyLevel) { 
        ++ sshift; 
        ssize <<= 1; 
    } 
    segmentShift = 32 - sshift; 
    segmentMask = ssize - 1; 
    this. segments = Segment. newArray( ssize);
}
```

由代码可知，`segments` 数组的长度 `ssize` 是通过 `concurrencyLevel` 计算出来的，为了能通过按位与的散列算法来定位 `segments` 数组的索引，必须保证 `segments` 数组的长度是 2 的 N 此房，所以必须计算出一个大于等于 `concurrencyLevel` 的最小的 2 的 N 此房值来作为 `segments` 数组的长度。假设 `concurrencyLevel` 等于 14、15或16，那么容器锁的个数就是 16。concurrencyLevel 的最大值为 2^16 - 1，即65535。

2）初始化 `segmentShift` 和 `segmentMask`

这两个全局变量需要在定位 `segment` 时的散列算法里使用，`sshift` 等于 `ssize` 从 1 向左移位的次数，在默认情况下 `concurrencyLevel` 等于 16， 1 需要向左移位移动 4 次。`segmentSift` 用于定位参与散列运算的位数，`segmentShift` 等于 32 减 `sshift`，所以等于 28，这里之所以用 32 是因为 `concurrentHashMap` 里的 `hash()` 方法输出的最大数是 32 位的。`segmentMask` 是散列运算的掩码，等于 `ssize` 减 1，即 15，掩码的二进制各个位的值都是 1。因为 `ssize` 的最大长度是 65536，所以 `segmentShift` 最大值是 16，`segmentMask` 最大值是 65535，每个位都是 1。

3）初始化每个 `segment`

输入参数 `initialCapacity` 是 `concurrentHashMap` 的初始化容量，`loadfactory` 是每个 `segment` 的负载因子，在构造方法里需要通过这两个参数来初始化数组中的每个 `segment`。

```java
if (initialCapacity > MAXIMUM_ CAPACITY) 
    initialCapacity = MAXIMUM_ CAPACITY; 
int c = initialCapacity / ssize; 
if (c * ssize < initialCapacity) 
    ++ c; int cap = 1; 
while (cap < c) 
    cap <<= 1; 
for (int i = 0; i < this.segments. length; ++ i) 
    this.segments[ i] = new Segment< K, V>( cap, loadFactor);
```

#### 定位 Segment

既然 `ConcurrentHashMap` 使用分段锁 `Segment` 来保护不同段的数据，那么插入和获取元素的时候，必须通过散列算法定位到 `Segment`。可以看到 `ConcurrentHashMap` 会首先使用算法对元素的 `hashCode` 进行一次再散列。

```java
private static int hash( int h) { 
    h += (h << 15) ^ 0xffffcd7d; 
    h ^= (h >>> 10); 
    h += (h << 3); 
    h ^= (h >>> 6); 
    h += (h << 2) + (h << 14); 
    return h ^ (h >>> 16); 
}
```

之所以进行再散列，目的是减少散列的冲突，是元素能够均匀的分布在不同的 `Segment` 上，从而提高容器的存取效率。

#### ConcurrentHashMap 的操作

1）`get` 操作

`Segment` 的 `get` 操作实现非常简单高效。先经过一次再散列，然后使用这个散列值通过散列运算定位到 `Segment`，再通过散列算法定位到元素：

```java
public V get(Object key) {
    int hash = hash(key.hashCode());
    return segmentFor(hash).get(key, hash);
}
```

`get` 操作的高效之处在于整个 `get` 过程不需要加锁，除非读到的值是空才会加锁重读。我们知道 `HashTable` 容器的 `get` 方法是需要加锁的，那么 `ConcurrentHashMap` 的 `get` 操作是如何做到不加锁的呢，原因是它的 `get` 方法里将使用的共享变量都定义成 `volatile` 类型，如用于统计当前 `Segment` 大小的 `count` 字段和用于存储值得 `HashEntry` 的 `value`。定义成 `volatile` 的变量，能够在线程智简保持可见性，能够被多线程同时读，并且保证不会读到过期的值，但是只能被单线程读（当写入值不依赖与原值时，可以被多线程写），在 `get` 操作里只需要读不需要写共享变量 `count` 和 `value`，所以可以不加锁。之所以不会读到过期的值，是因为对 `volatile` 变量的写入先于读取操作，即使两个线程同时修改和获取 `volatile` 变量，`get` 操作也能拿到最新的值。

```java
transient volatile int count;
volatile V value;
```

2）`put` 操作

由于 `put` 方法里需要对共享变量进行写入操作，所以为了线程安全，在操作共享变量时必须加锁。`put` 方法首先定位到 `Segment`，然后在 `Segment` 里进行插入操作，插入操作需要经历两个步骤：判断是否需要对 `Segment` 里的 `HashEntry` 数组进行扩容；定位添加元素的位置，然后将其放在 `HashEntry` 数组里。

- 是否需要扩容

  在插入元素前会先判断 `Segment` 里的 `HashEntry` 数组是否超过容量，如果超过阈值，则对数组进行扩容。`Segment` 的扩容判断比 `HashMap` 更恰当，因为 `HashMap` 是插入元素后判断元素是否已经达到容量的，如果到达了进行扩容，但是很有可能扩容后没有新元素插入。

- 如何扩容

  在扩容时，首先创建一个容量是原来容量两倍的数组，然后将原素组的元素进行再散列后插入到新的数组里。为了搞笑， `ConcurrentHashMap` 不会对整个容器进行扩容，而只对某个 `Segment` 进行扩容。

3）`size` 操作

如果要统计整个 `ConcurrentHashMap` 里元素的大小，就必须统计所有 `Segment` 里元素的大小后求和。 `Segment` 里的全局变量 `count` 是一个 `volatile` 变量，那么多线程场景下，是不是直接把所有 `Segment` 的 `count` 相加就可以得到整个 `ConcurrentHashMap` 的大小了？不是的，虽然相加就时可以获取每个 `Segment` 的 `count` 的最新值，但是可能累加前使用的 `count` 发生了变化，那么统计结果就不准了。所以，最安全的做法是在统计 `size` 的时候把所有 `Segment` 的 `put`、`remove`和`clean` 方法全部锁住，但是这种做法非常低效。

因为在累加 `count` 操作的过程中，之前累加过的 `count` 发生变化的概率非常小，所以 `ConcurrentHashMap` 的做法是先尝试 2 次通过不锁住 `Segment` 的方式来统计各个 `Segment` 大小，如果统计的过程中，容器 `count` 发生了变化，则再采用加锁的方式来尝试统计所有 `Segment` 的大小。

### ConcurrentLinkedQueue

在并发编程中，有时候需要使用线程安全的队列。如果要实现一个线程安全的队列有两种方式：

- 阻塞算法

  可以用一个锁，入列出列用同一把锁，或者使用两把锁，入列一把出列一把。

- 使用非阻塞算法

  使用循环 `CAS` 的方式来实现。

`ConcurrentLinkedQueue` 是非阻塞的方式实现的线程安全队列。`ConcurrentLinkedQueue` 是一个基于链接节点的无界线程安全队列，采用先进先出的规则对节点进行排序，当我们添加一个元素的时候，它会添加到队列的尾部，当我们获取一个元素时，会返回队列头部的元素，使用 `CAS` 算法实现。

#### ConcurrentLinkedQueue 的结构

`ConcurrentLinkedQueue` 由 `head` 节点和 `tail` 节点组成，每个节点由节点元素和只想下一个节点的 `next` 的引用组成，节点与节点之间就是通过 `next` 关联起来，从而组成一张链表结构的队列。默认情况下 `head` 节点存储的元素为空，`tail` 节点等于 `head` 节点。

```java
private transient volatile Node<E> tail = head;
```

#### 入队列

1）如队列过程

如队列就是将入队节点添加到队列的尾部：

- 将入队节点设置成当前队列尾节点的下一个节点。
- 更新 `tail` 节点，如果 `tail` 节点的 `next` 不为空，则将入队节点设置成 `tail` 节点，如果 `tail` 节点的 `next` 节点为空，则将入队节点设置成 `tail` 节点的 `next` 节点，所以 `tail` 节点不总是尾节点。

上面从单线程入队的角度理解了入队过程，但是多线程同时进行入队的情况就变得更加复杂了，因为可能出现其他线程插队的情况。如果一个线程正在入队，那么它必须先获取尾节点，然后设置尾节点的下一个节点为入队节点，但这时可能有另一个线程插队了，那么队列的尾节点就会发生变化，这时当前线程要暂停入队操作，然后重新获取尾节点。

```java
public boolean offer(E e) {
    if(e == null) 
        throw new NullpointerException();
    Node<E> n = new Node<>();
    // 死循环，入队不成功，反复入队。
    for (;;) {
        // 创建一个指向 tail 节点的引用
        Node<E> t = tail;
        // p 表示队列的尾节点，默认等于 tail 节点
        Node<E> p = t;
        for(int hops = 0;; hops++) {
            // 获得 p 节点的下一个节点
            Node<E> next = succ(p);
            // next 节点不为空，说明 p 不是尾节点，需要更新 p 后再将它指向 nexit 节点
            if(next != null) {
                // 循环了两次极其以上，并且当前节点还是不等于尾节点
                if(hops > HOPS && t != tail)
                    continue;
                p = next;
            }
            // 如果 p 是尾节点，则设置 p 节点的 next 节点为入队节点
            else if(p.casNext(null, n)) {
                // 如果 tail 节点有大于等于 1 个next 节点，则将入队节点设置成 tail 节点
                // 更新失败没关系，表示其他线程成功更新了 tail 节点
                casTail(t, n);
                return true;
            }
            // p 有 next 节点，表示 p 的 next 节点是尾节点，则重新设置 p 节点
            else {
                p = succ(p);
            }
        }
    }
}
```

从源码分析可把入队过程分为主要的两件事：

- 定位出尾节点。
- 使用 CAS  算法将入队节点设置成尾节点的 `next` 节点，不成功则重试。

2）定位尾节点

`tail` 节点并不总是尾节点，所以每次入队都必须先提高 `tail` 节点来找到尾节点。尾节点可能是 `tail` 节点，也可能是 `tail` 节点的 `next` 节点。源码中判断了 `tail` 的 `next` 节点是否为 `null` ，有则表示 `next` 节点可能是尾节点。获取 `next` 节点需要注意的是 `p` 节点等于 `p` 的 `next` 节点的情况，只有一种可能就是 `p` 节点和 `p` 的 `next` 节点都等于空，表示这个队列刚初始化，正准备添加节点，所以需要返回 `head` 节点。获取 `p` 节点的 `next` 节点：

```java
final Node<E> succ(Node<E> p) {
    return (p == next) ? head : next;
}
```

3）设置入队节点为尾节点

`p.casNext(null, n)` 方法用于将入队节点设置为当前队列尾节点的`next` 节点，如果 `p` 是 `null` ，表示 `p` 是当前队列的尾节点，如果不为 `null`，表示其他线程更新了尾节点，则需要重新获取当前队列的尾节点。

4）`HOPS` 变量的设计意图

是否可把上面的代码简化：

```java
public boolean offer(E e) {
    if(e == null) {
        throw new NullPointerException();
    }
    Node<E> n = new Node(e);
    for(;;) {
        Node<E> t = tail;
        if(t.casNext(null, n) && casTail(t, n)) {
            return true;
        }
    }
}
```

当我们将代码简化成这样时，每次都需要使用循环 `CAS` 更新 `tail` 节点，如果能减少 `CAS` 更新 `tail` 节点的次数，就能提高入队的效率，所以 `doug lea` 使用 `HOPS` 变量控制并减少 `tail` 节点的更新频率，并不是每次节点入队后都将 `tail` 节点更新成尾节点，而是当 `tail` 节点和尾节点的距离大于等于常量 `HOPS` 的值时才更新 `tail` 节点，`tail` 和尾节点的距离越长，使用 `CAS` 更新 `tail` 节点的次数就会越少，但是距离越长带来的负面效果就是每次入队时定位尾节点的时间就越长，因为循环体需要多次循环来定位尾节点。

```java
private static final HOPS = 1;
```

5）注意

入队方法返回永远为 `true` ，所以不要通过返回值判断入队是否成功。

#### 出队列

出队列就是从队列返回一个节点元素，并清空该节点对元素的引用。并不是每次出队列都更新 `head` 节点，当 `head` 节点里的元素时，直接弹出 `head` 里的元素，而不会更新 `head` 节点。只有当 `head` 节点里没有元素时，出队操作才会更新 `head` 节点。这种做法也是通过 `HOPS` 变量来减少使用 `CAS` 更新 `head` 节点的消耗，从而提高出队列效率。

```java
public E poll() { 
    Node< E> h = head; 
    // p 表示头节点，需要出队的节点 
    Node< E> p = h; 
    for (int hops = 0;; hops++) { 
        // 获取 p 节点的元素 
        E item = p. getItem(); 
        // 如果 p 节点 的 元素不为空，使用 CAS 设置 p 节点引用的元素为 null, 
        // 如果成功则返回 p 节点的元素。
        if (item != null && p. casItem( item, null)) { 
            if (hops >= HOPS) { 
                // 将 p 节点下一个节点设置成 head 节点 
                Node< E> q = p. getNext(); 
                updateHead( h, (q != null) q : p); 
            } 
            return item; 
        } 
        // 如果头节点的元素为空或头节点发生了变化，这说明头节点已经被另外 
        // 一个线程修改了。那么获取 p 节点的下一个节点 
        Node< E> next = succ( p); 
        // 如果 p 的下一个节点也为空，说明这个队列已经空了 
        if (next == null) { 
            // 更新头节点。 
            updateHead( h, p); 
            break; 
        } 
        // 如果下一个元素不为空，则将头节点的下一个节点设置成头节点 
        p = next; 
    } 
    return null; 
}
```

首先获取头结点元素，然后判断头节点元素是否为空，如果为空，表示另外一个线程已经进行了一次出队操作将该节点的元素取走，如果不为空，则使用 `CAS` 的方式将头节点的引用设置为 `null`，如果 `CAS` 成功，则直接返回头结点的元素，如果不成功，表示另外一个线程已经进行了一次出队操作更新了 `head` 节点，导致元素发生了变化，需要重新获取头结点。

### Java 中的阻塞队列

#### 什么是阻塞队列

阻塞队列是一个支持两个附加操作的队列。这两个附加的操作支持阻塞的插入和移除方法。

- 支持阻塞的插入方法

  当队列满时，队列会阻塞插入元素的线程，知道队列不满。

- 支持阻塞的移除方法

  在队列为空时，获取元素的线程会等待队列变为非空。

阻塞队列常用于生产者和消费者的场景，生产者是向队列里添加元素的线程，消费者是从队列里取元素的线程。阻塞队列就是生产者用来存放元素、消费者用来获取元素的容器。

| 方法/ 处理方式 | 抛出异常  | 返回特殊值 | 一直阻塞 | 超时退出             |
| -------------- | --------- | ---------- | -------- | -------------------- |
| 插入方法       | add(e)    | offer(e)   | put(e)   | offer(e, time, unit) |
| 移除方法       | remove()  | poll()     | take()   | poll(time, unit)     |
| 检查方法       | element() | peek()     | 不可用   | 不可用               |

- 抛出异常

  当队列满时，如果再往队列插入元素，抛出 `IllegalStateException("Queue full")` 异常。当队列空时，从队列里获取元素会抛出 `NoSuchElementException` 异常。

- 返回特殊值

  当往队列插入元素时，会返回元素是否插入成功，成功返回 `true`。如果是移除方法，则是从队列取出一个元素，如果没有则返回 `null`。

- 一直阻塞

  当阻塞队列满时，如果生产者线程往队列`put` 元素，队列会一直阻塞生产者线程，知道队列可用或者响应中断退出。当队列为空时，如果消费者线程从队列里 `take` 元素，队列会阻塞消费者线程，直到队列不为空。

- 超时退出

  当阻塞队列满时，如果生产者线程往队列插入元素，队列会阻塞生产者线程一段时间，如果超过了指定的时间，生产者线程退出。

#### Java 中的阻塞队列

`JDK 7` 提供了 7 个阻塞队列：

1）ArrayBlockingQueue

一个由数组结构组成的有界阻塞队列。此队列按照先进先出的原则对元素进行排序。默认情况下不保证线程公平的访问队列，公平指的是阻塞的线程，可以按照阻塞的先后顺序访问队列，即先阻塞的线程先访问队列。非公平指对先等待的线程是不公平的，当队列可用时，阻塞的线程都可以争夺访问队列的资格，有可能先阻塞的线程最后才访问队列。为了办证公平性，通常会降低吞吐量。创建一个公平的阻塞队列：

```java
ArrayBlockingQueue fairQueue = new ArrayBlockingQueue(1000, true);
```

访问者的公平性是使用可重入锁实现的：

```java
public ArrayBlickingQueue(int capacity, boolean fair) {
    if(capacity <= 0) {
        throw new IllegalArgumentException();
    }
    this.items = new Object[capacity];
    lock = new ReetrantLock(fair);
    notEmpty = lock.newCondition();
    notFull = lock.newCondition;
}
```



2）LinkedBlockingQueue

一个由链表结构组成的有界阻塞队列。此队列的默认和最大长度为 `Integer.MAX_VALUE`。此队列按照先进先出的原则对元素进行排序。



3）PriorityBlockingQueue

一个支持优先级排序的无界阻塞队列。默认情况下元素采取自然顺序升序排列。也可以自定义类实现`compareTo()` 方法来指定元素排序规则，或者初始化 `PriortyBlockingQueue` 时，指定构造参数 `Comparator` 来对元素进行排序。此队列不能保证同优先级元素的顺序。



4）DelayQueue

一个使用优先级队列实现的无界阻塞队列。队列使用 `PriortyQueue` 来实现。队列中的元素必须实现 `Delayed` 接口，在创建元素时可以指定多久才能从队列中获取当前元素。只有在延迟期满时才能从队列中提取元素。运用场景：

- 缓存系统的设计

  可以用 `DelayQueue` 保存缓存元素的有效期，使用一个线程循环查询 `DelayQueue`，一旦能从 `DelayQueue` 中获取元素时，表示缓存有效期到了。

- 定时任务调度

  使用 `DelayQueue` 保存当天将会执行的任务和执行时间，一旦从 `DelayQueue` 中获取到任务就开始执行，比如 `TimerQueue` 就是使用 `DeplayQueue` 实现的。



如何实现 `Delayed` 接口？`DelayQueue` 队列的元素必须实现 `Delayed` 接口。实现此接口分为三步：

- 在对象创建的时候，初始化基本数据。使用 `time` 记录当前对象延迟到什么时候可以使用，使用 `sequenceNumber` 来标识元素在队列中的先后顺序：

  ```java
  private static final AtomicLong sequencer = new AtomicLong( 0); 
  ScheduledFutureTask( Runnable r, V result, long ns, long period) { 
      super( r, result); 
      this.time = ns; 
      this.period = period; 
      this.sequenceNumber = sequencer.getAndIncrement(); 
  }
  ```

- 实现 `getDeplay` 方法，该方法返回当前元素还需要延时多长时间，单位是纳秒：

  ```java
  public long getDelay(TimeUnit unit) {
      return unit.convert(time - now(), TimeUnit.NANOSECONDS);
  }
  ```

- 实现 `compareTo` 方法来指定元素的顺序。



如何实现延时阻塞队列？延时阻塞队列的实现很简单，当消费者从队列里获取元素时，如果元素没有达到延时时间，就阻塞当前线程。

```java
long delay = first.getDelay(TimeUnit. NANOSECONDS); 
if (delay <= 0) 
    return q. poll(); 
else if (leader != null) 
    available. await(); 
else { 
    Thread thisThread = Thread.currentThread(); 
    leader = thisThread; 
    try { 
        available.awaitNanos( delay); 
    } 
    finally { 
        if (leader == thisThread) 
            leader = null; 
    } 
}
```



5）SynchronousQueue

一个不存储元素的阻塞队列。每一个 `put` 操作必须等待一个 `take` 操作，否则不能继续添加元素。它支持公平访问队列，默认情况下线程采用非公平策略访问队列。可以使用下面的构造方法创建公平性访问的 `SynchronousQueue`，如果为 `true`，则等待的线程会采用先进先出的顺序访问队列。

```java
public SynchronousQueue(boolean fair) {
    transferer = fair ? new TransferQueue() : new TransferStack();
}
```

`SynchronousQueue` 可以看成是一个传球手，负责把生产者线程处理的数据直接传递给消费者线程。队列本身并不存储任何元素，非常适合传递性场景。它的吞吐量高于 `LinkedBlockingQueue`、`ArrayBlockingQueue`。



6）LinkedTransferQueue

一个由链表结构组成的无界阻塞队列。相对于其它阻塞队列，多了 `tryTransfer` 和 `transfer` 方法：

- `transfer` 方法

  如果当前消费者正在等待接收元素（消费者使用 `take` 方法或带时间限制的 `poll` 方法时），`transfer` 方法可以把生产者传入的元素立刻 `transfer` 给消费者。如果没有消费者在等待接收元素，`transfer` 方法会将元素存放在队列的 `tail` 节点，并等待该元素被消费者消费了才返回。

- `tryTransfer` 方法

  用来试探生产者传入的元素是否能直接传给消费者。如果没有消费者等待接收元素，则返回 `false`。和 `transfer` 方法的区别是 `tryTransfer` 方法无论消费者是否接收，方法立即返回，而 `transfer` 方法必须等到消费者消费了才返回。



7）LinkedBlockingDeque

一个由链表结构组成的双向阻塞队列。可以从队列的两端插入和移除元素。因为双向队列多了一个操作队列的入口，在多线程同时入队时，也就减少了一半的竞争。相比其他阻塞队列，多了 `addFirst`、`addLast`、`offerFirst`、`offerLast`、`peekFirst`、`peekLast` 等方法。插入方法 `add` 等同于 `addLast`，移除方法 `remove` 等同于 `removeFirst`。但是 `take` 方法却等同于 `takeFirst`。



#### 阻塞队列的实现原理

如果队列为空的，消费者会一直等待，当生产者添加元素时，消费者是如何知道当前队列有元素的呢？

1）使用通知模式实现

所谓通知模式，就是当生产者往满的队列里添加元素时会阻塞住生产者，当消费者消费了一个队列中的元素后，会通知生产者当前队列可用。`ArrayBlockingQueue` 的实现：

```java
// 其中一个构造方法
public ArrayBlockingQueue(int capacity, boolean fair) {
    if (capacity <= 0)
        throw new IllegalArgumentException();
    this.items = new Object[capacity];
    lock = new ReentrantLock(fair);
    notEmpty = lock.newCondition();
    notFull =  lock.newCondition();
}

public void put(E e) throws InterruptedException {
    checkNotNull(e);
    final ReentrantLock lock = this.lock;
    lock.lockInterruptibly();
    try {
        while (count == items.length)
            notFull.await();
        enqueue(e);
    } finally {
        lock.unlock();
    }
}
```

可看见使用了 `await` 方法使得线程阻塞住：

```java
public final void await() throws InterruptedException {
    if (Thread.interrupted())
        throw new InterruptedException();
    Node node = addConditionWaiter();
    long savedState = fullyRelease(node);
    int interruptMode = 0;
    while (!isOnSyncQueue(node)) {
        LockSupport.park(this);
        if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
            break;
    }
    if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
        interruptMode = REINTERRUPT;
    if (node.nextWaiter != null) // clean up if cancelled
        unlinkCancelledWaiters();
    if (interruptMode != 0)
        reportInterruptAfterWait(interruptMode);
}
```

通过 `LockSupport.park(this)` 实现阻塞：

```java
public static void park(Object blocker) {
    Thread t = Thread.currentThread();
    setBlocker(t, blocker);
    UNSAFE.park(false, 0L);
    setBlocker(t, null);
}
```

调用了 `setBlocker` 保存将要阻塞的线程，然后调用 `UNSAFE.park` 阻塞当前线程。

```java
public native void park(boolean var1, long var2);
```

`park` 方法会阻塞当前线程，发生以下四种情况的一种，该方法会返回：

1）线程被中断。

2）与 `park` 对应的 `unpark` 执行或已经执行时。“已经执行” 是指 `unpark` 先执行，然后执行 `park` 的情况。

3）等待完 `time` 参数指定的毫秒数时。

4）异常现象发生时，这个异常现象没有任何原因。

`park` 在不同的操作系统中使用不同的方式实现，在 `Linux` 下使用的是系统方法 `pthread_cond_wait` 实现。实现代码在 `JVM` 源码路径 `src/os/linux/vm/os_linux.cpp` 里的 `os:: PlatformEvent:: park` 方法：

```c++
void os:: PlatformEvent:: park() {
    int v;
    for(;;) {
        v = _Event;
        if(Atomic:: cmpchg(v - 1, &_Event, v) == v)
            break;
    }
    guarantee(v >= 0, "invariant");
    if(v == 0) {
        int status = pthread_mutex_lock;
        assert_status(status == 0, status. "mutex_lock");
        guarantee(_nParked == 0, "invariant");
        ++_nparked;
        while(_Event < 0) {
            status = pthread_cond_wait(_cond, _mutex);
            if(status == ETIME) {
                status = EINTR;
            }
            assert_status(status == 0 || status == EINTR, status, "cond_wait");
            --_nParked;
            _Event = 0;
            status = pthread_mutex_unlock(_mutex);
            assert_status(status == 0, status, "mutex_unlock");
        }
        guarantee(_Event >= 0, "invariant");
    }
}
```

`pthread_cond_wait` 是一个多线程的条件变量函数，`cond` 是 `condition` 的缩写，字面意思可以理解为线程在等待一个条件发生，这个条件是一个全局变量。这个方法接收两个参数：一个共享变量`_cond`，一个互斥量`_mutex`。而 `unpark` 方法在 `Linux` 下使用 `pthread_cond_signal` 实现的。`park` 方法在 `Windows` 下则是使用 `WaitForSingleObject` 实现的。当线程被阻塞队列阻塞时，线程会进入 `WAITING(parking)` 状态。

### Fork/ Join 框架

#### 什么是 Fork/ Join 框架

`Fork/ Join` 框架是 `Java 7` 提供的一个用于并行执行任务的框架，是一个把大人物分割成若干个小人物，最终汇总每个小任务结果后得到大人物结果的框架。`Fork` 就是把一个大人物切分为若干个子任务并行的执行，`Join` 就是合并这些子任务的执行结果，最后得到这个大人物的结果。

#### 工作窃取算法

工作窃取算法是指某个线程从其它队列中窃取任务来执行。假设我们需要做一个比较大的任务，可以把这个任务分割为若干互不依赖的子任务，为了减少线程间的竞争，把这些子任务分别放到不同的队列里，并为每个队列创建一个单独的线程来执行队列里的任务，线程和队列一一对应。若出现线程已经将任务执行完毕，而其他线程还有任务未处理，此时无任务的线程可以在其他线程的队列中窃取一个任务来执行。而此时两个线程访问同一个队列，所以为了减少窃取任务线程和被窃取任务线程之间的竞争，通常会使用双端队列，被窃取人物线程永远从双端队列的头部获取任务，而窃取任务的线程永远从队列的尾部拿任务执行。

1）优点

充分利用线程进行并行计算，减少了线程间的竞争。

2）缺点

在某些情况下存在竞争，比如双端队列中只存在一个任务时。该算法消耗更多的系统资源，比如创建多个线程和多个双端队列。

#### Fork/ Join 框架的设计

1）分割任务

首先需要把大任务分割为子任务，继续分割子任务，直到子任务足够小。

2）执行任务合并人物

分割的子任务分别放在双端队列中，然后几个启动线程分别从双端队列中获取任务执行。子任务执行完的结果都统一放在一个队列里，启动一个线程从队列获取数据，合并。

`Fork/ Join` 使用两个类完成上面两件事情：

1）`ForkJoinTask`

首先创建一个 `ForkJoin` 任务，它提供在任务中执行 `fork()` 和 `join()` 操作的机制。我们不需要直接继承 `ForkJoinTask` 类，只需要继承它的子类，`Fork/ Join` 框架提供了一下两个子类：

- `RecursiveAction`

  用于没有返回结果的任务。

- `RecursiveTask`

  用于有返回结果的任务。

2）`ForkJoinPool`

`ForkJoinTask` 需要通过 `ForkJoinPool` 来执行。



任务分割出的子任务会添加到当前工作线程所维护的双端队列中，进入队列的头部。当一个工作线程的队列里暂时没有任务时，它会随机从其他工作线程的队列的尾部获取一个任务。

#### 使用 Fork/ Join 框架

计算 `1+2+3+4` 的结果：

使用 `Fork/ Join` 框架首先要考虑的是如何分割任务，由于是 4 个数字相加，所以把任务分为两个子任务，分别执行 `1 + 2` 和 `3 + 4`，然后 `join` 两个子任务的结果。

```java
/**
 * 连续数字相加（fork/join）
 * @author YuanLi
 */
public class NumbersAddTask extends RecursiveTask<Integer> {

    private static final int THRESHOLD = 2;
    private int start, end;

    public NumbersAddTask(int start, int end) {
        this.start = start;
        this.end = end;
    }

    @Override
    protected Integer compute() {

        int sum = 0;
        boolean canCompute = end - start < THRESHOLD;

        if(canCompute) {
            if(start == end) {
                sum = start;
            }
            else {
                sum = start + end;
            }
        }
        else {
            int middle = (start + end) / 2;

            NumbersAddTask leftTask = new NumbersAddTask(start, middle);
            NumbersAddTask rightTask = new NumbersAddTask(middle + 1, end);

            leftTask.fork();
            rightTask.fork();

            int leftResult = leftTask.join();
            int rightResult = rightTask.join();

            sum = leftResult + rightResult;
        }
        return sum;
    }

}
```

测试：

```java
public void testNumbersAddTask() {
    ForkJoinPool forkJoinPool = new ForkJoinPool();

    NumbersAddTask task = new NumbersAddTask(1, 10);

    Future<Integer> res = forkJoinPool.submit(task);

    try {
        System.out.println(res.get());
    }
    catch (InterruptedException e) {
        e.printStackTrace();
    }
    catch (ExecutionException e) {
        e.printStackTrace();
    }
}
```

`ForkJoinTask` 与一般任务的主要区别在于它需要实现 `compute` 方法，在这个方法里，首先需要判断任务是否足够小，如果足够小就直接执行任务。如果不足够小，就必须分割成两个子任务，每个任务在调用 `fork` 方法时，又会进入 `compute` 方法，看看当前子任务是否需要继续分割成子任务，如果不需要继续分割，则执行当前子任务并返回结果。使用 `join` 方法会等待子任务执行完并得到其结果。

#### Fork/ Join 框架的异常处理

`ForkJoinTask` 在执行的时候可能会抛出异常，但是没办法在主线程里直接捕获异常，所以 `ForkJoinTask` 提供了 `isCompletedAbnormally()` 方法来检查任务是否已经抛出异常或已经被取消了，并且可以通过 `ForkJoinTask` 的 `getExeception` 方法获取异常：

```java
if(task.isCompleteAbnormally()) {
    System.out.println(task.getExeception())
}
```

`getExeception` 方法返回 `Throwable` 对象，如果任务被取消了则返回 `CancellationExeception`；如果任务没有完成或者没有抛出异常则返回 `null`。



#### Fork/ Join 框架的实现原理

`ForkJoinPool` 由 `ForkJoinTask` 数组和 `ForkJoinWorkerThread` 数组组成，`ForkJoinTask` 数组负责将存放程序提交给 `ForkJoinPool` 的任务，而 `ForkJoinWorkerThread` 数组负责执行这些任务。

1）`ForkJoinTask` 的 `fork` 方法实现原理

当我们调用 `fork` 方法时，程序会调用 `ForkJoinWorkerThread` 的 `workQueue.push` 方法异步的执行这个任务，然后立即返回结果：

```java
public final ForkJoinTask<V> fork() {
        Thread t;
        if ((t = Thread.currentThread()) instanceof ForkJoinWorkerThread)
            ((ForkJoinWorkerThread)t).workQueue.push(this);
        else
            ForkJoinPool.common.externalPush(this);
        return this;
    }
```

`push` 方法把当前任务存放在 `ForkJoinTask` 数组队列里。然后再调用 `ForkJoinPool` 的 `signalWork()` 方法唤醒或创建一个工作线程来执行任务。

```java
final void push(ForkJoinTask<?> task) {
    ForkJoinTask<?>[] a; ForkJoinPool p;
    int b = base, s = top, n;
    if ((a = array) != null) {    // ignore if queue removed
        int m = a.length - 1;     // fenced write for task visibility
        U.putOrderedObject(a, ((m & s) << ASHIFT) + ABASE, task);
        U.putOrderedInt(this, QTOP, s + 1);
        if ((n = s - b) <= 1) {
            if ((p = pool) != null)
                p.signalWork(p.workQueues, this);
        }
        else if (n >= m)
            growArray();
    }
}
```

2）`ForkJoinTask` 的 `join` 方法实现原理

`join` 方法的主要作用是阻塞当前线程并等待获取结果：

```java
public final V join() {
    int s;
    if ((s = doJoin() & DONE_MASK) != NORMAL)
        reportException(s);
    return getRawResult();
}

private void reportException(int s) {
    if (s == CANCELLED)
        throw new CancellationException();
    if (s == EXCEPTIONAL)
        rethrow(getThrowableException());
}
```

调用 `doJoin()` 方法，通过 `doJoin()` 方法得到当前任务的状态来判断返回什么结果，任务状态有 4 种：已完成（NORMAL）、被取消（CANCELLED）、信号（SIGNAL）和出现异常（EXECEPTIONAL）。

- 如果任务状态已完成，直接返回任务结果。
- 如果任务状态是取消，直接抛出此异常。
- 如果为抛出异常，直接抛出对应异常。

```java
private int doJoin() {
    int s; Thread t; ForkJoinWorkerThread wt; ForkJoinPool.WorkQueue w;
    return (s = status) < 0 ? s :
    ((t = Thread.currentThread()) instanceof ForkJoinWorkerThread) ?
        (w = (wt = (ForkJoinWorkerThread)t).workQueue).
        tryUnpush(this) && (s = doExec()) < 0 ? s :
    wt.pool.awaitJoin(w, this, 0L) :
    externalAwaitDone();
}
```

`doJoin` 方法里，首先查看任务的状态，看任务是否已经执行完成，如果执行完成则直接返回任务状态，如果没有执行完，则从任务数组里去除任务并执行。如果任务顺利执行完成，则设置任务状态为 `NORMAL`，如果出现异常，则记录异常，并将任务状态设置为 `EXECEPTIONAL`。