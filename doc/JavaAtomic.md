## Java 中的原子操作类

从 `JAVA 5` 开始提供了 `java.util.concurrent.atomic` 包，这个包中的原子操作类提供了一种用法简单、性能搞笑、线程安全的更新一个变量的方式。

### 原子更新基本类型类

使用原子的方式更新基本类型：

- `AtomicBoolean`

  原子更新布尔类型。

- `AtomicInteger`

  原子更新整型。

- `AtomicLong`

  原子更新长整型。

以上三个类提供的方法几乎一模一样，所以只举例 `AtomicInteger` 。`AtomicInteger` 的常用方法包括：

- `int addAndGet(int delta)`

  以原子方式将输入的数值与实例中的值（`AtomicInteger` 里的 `value`）相加，并返回结果。

- `boolean compareAndSet(int expect, int update)`

  如果输入的数值等于预期值，则以原子方式将该值设置为输入的值。

- `int getAndIncrement()`

  以原子方式将当前值加 1，注意这里返回的是自增的值。

- `void lazySet(int newValue)`

  最终会设置成 `newValue`，使用 `lazySet` 设置值后，可能导致其他线程在之后的一小段时间内可以读到旧的值。

- `int getAndSet(int newValue)`

  以原子方式设置为 `newValue` 的值，并返回旧值。

`atomic` 包只提供了这三种基本类型的原子更新，其它如 `char`、`float`、`double` 等可使用 `Unsafe` 类的方法实现，`Unsafe` 类提供了三种 `CAS` 操作：`compareAndSwapObject`、`compareAndSwapInt` 和 `compareAndWwapLong`，可以先进行类型转换再调用者三个方法之一进行原子更新。

### 原子更新数组

通过原子的方式更新数组的某个元素：

- `AtomicIntegerArray`
- `AtomicLongArray`
- `AtomicReferenceArray`
- `AtomicIntegerArray`

`AtomicIntegerArray` 的常用方法：

- `int addAndSet(int i, int delta)`

  以原子方式将输入值与数组中索引为 i 的元素相加。

- `boolean compareAndSet(int i, int expect, int update)`

  若当前值等于预期值，则以原子方式将数组位置 i 的元素设置成 `update` 值。

```java
public AtomicIntegerArray(int[] array) {
    // Visibility guaranteed by final field guarantees
    this.array = array.clone();
}
```

以此种方式创建的实例，实例对内部数组元素修改时，不印象传入的数组。因为实例持有的复制品。

### 原子更新引用

- `AtomicReference`

  原子更新引用类型。

- `AtomicReferenceFiledUpdater`

  原子更新引用类型里的字段。

- `AtomicMarkableReference`

  原子更新带有标记位的引用类型。

`AtomicReference` 示例：

```java
public class AtomicReferenceTest extends TestCase {
    public void testAtomicReference() {

        AtomicReference<User> userAtomicReference = new AtomicReference<>();

        User userOne = new User("小红", "女");
        userAtomicReference.set(userOne);

        User userTwo = new User("小白", "男");

        userAtomicReference.compareAndSet(userOne, userTwo);

        System.out.println(userAtomicReference.get().getName());

    }
}

public class User : Serializable {

    constructor(name : String, sex : String) {
        this.name = name;
        this.sex = sex;
    }

    var name : String = "用户";

    var sex : String = "男";

}
```

### 原子更新字段类

如果需要原子的更新某个类里的某个字段，就需要使用原子更新字段类：

- `AtomicIntegerFieldUpdater`

  原子更新整型的字段的更新器。

- `AtomicLongFieldUpdater`

  原子更新长整型字段的更新器。

- `AtomicStampedReference`

  原子更新带有版本号的引用类型。

原子更新字段需要两步：

1）因为原子更新类都是抽象类，每次使用的时候必须使用静态方法 `newUpdater` 创建一个更新器，并且需要设置想要更新的类和属性。

2）更新类的字段必须使用 `public volatile` 修饰符。 



