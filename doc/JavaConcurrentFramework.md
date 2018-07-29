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

