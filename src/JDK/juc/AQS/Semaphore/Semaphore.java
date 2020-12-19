/*
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

/*
 *
 *
 *
 *
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package JDK.juc.AQS.Semaphore;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import JDK.juc.AQS.Condition.AbstractQueuedSynchronizer;

/**
 * A counting semaphore.  Conceptually, a semaphore maintains a set of
 * permits.  Each {@link #acquire} blocks if necessary until a permit is
 * available, and then takes it.  Each {@link #release} adds a permit,
 * potentially releasing a blocking acquirer.
 * However, no actual permit objects are used; the {@code Semaphore} just
 * keeps a count of the number available and acts accordingly.
 *
 * <p>Semaphores are often used to restrict the number of threads than can
 * access some (physical or logical) resource. For example, here is
 * a class that uses a semaphore to control access to a pool of items:
 *  <pre> {@code
 * class Pool {
 *   private static final int MAX_AVAILABLE = 100;
 *   private final Semaphore available = new Semaphore(MAX_AVAILABLE, true);
 *
 *   public Object getItem() throws InterruptedException {
 *     available.acquire();
 *     return getNextAvailableItem();
 *   }
 *
 *   public void putItem(Object x) {
 *     if (markAsUnused(x))
 *       available.release();
 *   }
 *
 *   // Not a particularly efficient data structure; just for demo
 *
 *   protected Object[] items = ... whatever kinds of items being managed
 *   protected boolean[] used = new boolean[MAX_AVAILABLE];
 *
 *   protected synchronized Object getNextAvailableItem() {
 *     for (int i = 0; i < MAX_AVAILABLE; ++i) {
 *       if (!used[i]) {
 *          used[i] = true;
 *          return items[i];
 *       }
 *     }
 *     return null; // not reached
 *   }
 *
 *   protected synchronized boolean markAsUnused(Object item) {
 *     for (int i = 0; i < MAX_AVAILABLE; ++i) {
 *       if (item == items[i]) {
 *          if (used[i]) {
 *            used[i] = false;
 *            return true;
 *          } else
 *            return false;
 *       }
 *     }
 *     return false;
 *   }
 * }}</pre>
 *
 * <p>Before obtaining an item each thread must acquire a permit from
 * the semaphore, guaranteeing that an item is available for use. When
 * the thread has finished with the item it is returned back to the
 * pool and a permit is returned to the semaphore, allowing another
 * thread to acquire that item.  Note that no synchronization lock is
 * held when {@link #acquire} is called as that would prevent an item
 * from being returned to the pool.  The semaphore encapsulates the
 * synchronization needed to restrict access to the pool, separately
 * from any synchronization needed to maintain the consistency of the
 * pool itself.
 *
 * <p>A semaphore initialized to one, and which is used such that it
 * only has at most one permit available, can serve as a mutual
 * exclusion lock.  This is more commonly known as a <em>binary
 * semaphore</em>, because it only has two states: one permit
 * available, or zero permits available.  When used in this way, the
 * binary semaphore has the property (unlike many {@link java.util.concurrent.locks.Lock}
 * implementations), that the &quot;lock&quot; can be released by a
 * thread other than the owner (as semaphores have no notion of
 * ownership).  This can be useful in some specialized contexts, such
 * as deadlock recovery.
 *
 * <p> The constructor for this class optionally accepts a
 * <em>fairness</em> parameter. When set false, this class makes no
 * guarantees about the order in which threads acquire permits. In
 * particular, <em>barging</em> is permitted, that is, a thread
 * invoking {@link #acquire} can be allocated a permit ahead of a
 * thread that has been waiting - logically the new thread places itself at
 * the head of the queue of waiting threads. When fairness is set true, the
 * semaphore guarantees that threads invoking any of the {@link
 * #acquire() acquire} methods are selected to obtain permits in the order in
 * which their invocation of those methods was processed
 * (first-in-first-out; FIFO). Note that FIFO ordering necessarily
 * applies to specific internal points of execution within these
 * methods.  So, it is possible for one thread to invoke
 * {@code acquire} before another, but reach the ordering point after
 * the other, and similarly upon return from the method.
 * Also note that the untimed {@link #tryAcquire() tryAcquire} methods do not
 * honor the fairness setting, but will take any permits that are
 * available.
 *
 * <p>Generally, semaphores used to control resource access should be
 * initialized as fair, to ensure that no thread is starved out from
 * accessing a resource. When using semaphores for other kinds of
 * synchronization control, the throughput advantages of non-fair
 * ordering often outweigh fairness considerations.
 *
 * <p>This class also provides convenience methods to {@link
 * #acquire(int) acquire} and {@link #release(int) release} multiple
 * permits at a time.  Beware of the increased risk of indefinite
 * postponement when these methods are used without fairness set true.
 *
 * <p>Memory consistency effects: Actions in a thread prior to calling
 * a "release" method such as {@code release()}
 * <a href="package-summary.html#MemoryVisibility"><i>happen-before</i></a>
 * actions following a successful "acquire" method such as {@code acquire()}
 * in another thread.
 *
 * @since 1.5
 * @author Doug Lea
 */
public class Semaphore implements java.io.Serializable {
    private static final long serialVersionUID = -3222578661600680210L;
    /** All mechanics via AbstractQueuedSynchronizer subclass */
    private final Sync sync;

    /**
     * Synchronization implementation for semaphore.  Uses AQS state
     * to represent permits. Subclassed into fair and nonfair
     * versions.
     */
    abstract static class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = 1192457210091910933L;

        Sync(int permits) {
            setState(permits);
        }

        final int getPermits() {
            return getState();
        }

        final int nonfairTryAcquireShared(int acquires) {
            for (;;) {
                int available = getState();
                int remaining = available - acquires;
                if (remaining < 0 ||
                    compareAndSetState(available, remaining))
                    return remaining;
            }
        }

        protected final boolean tryReleaseShared(int releases) {
            for (;;) {
                int current = getState();
                int next = current + releases;
                if (next < current) // overflow
                    throw new Error("Maximum permit count exceeded");
                if (compareAndSetState(current, next))
                    return true;
            }
        }

        final void reducePermits(int reductions) {
            for (;;) {
                int current = getState();
                int next = current - reductions;
                if (next > current) // underflow
                    throw new Error("Permit count underflow");
                if (compareAndSetState(current, next))
                    return;
            }
        }

        final int drainPermits() {
            for (;;) {
                int current = getState();
                if (current == 0 || compareAndSetState(current, 0))
                    return current;
            }
        }
    }

    /**
     * NonFair version
     */
    static final class NonfairSync extends Sync {
        private static final long serialVersionUID = -2694183684443567898L;

        NonfairSync(int permits) {
            super(permits);
        }

        protected int tryAcquireShared(int acquires) {
            return nonfairTryAcquireShared(acquires);
        }
    }

    /**
     * Fair version
     */
    static final class FairSync extends Sync {
        private static final long serialVersionUID = 2014338818796000944L;

        FairSync(int permits) {
            super(permits);
        }

        /**
         * 尝试获取通行证，获取成功返回 >= 0的值;
         * 获取失败 返回 < 0 值
         */
        protected int   tryAcquireShared(int acquires) {
            for (;;) {
                //判断当前 AQS 阻塞队列内 是否有等待者线程，如果有直接返回-1，表示当前aquire操作的线程需要进入到队列等待..
                if (hasQueuedPredecessors())
                    return -1;
                //执行到这里，有哪几种情况？
                //1.调用aquire时 AQS阻塞队列内没有其它等待者
                //2.当前节点 在阻塞队列内是headNext节点

                //获取state ，state这里表示 通行证
                int available = getState();
                //remaining 表示当前线程 获取通行证完成之后，semaphore还剩余数量
                int remaining = available - acquires;

                //条件一：remaining < 0 成立，说明线程获取通行证失败..
                //条件二：前置条件，remaning >= 0, CAS更新state 成功，说明线程获取通行证成功，CAS失败，则自旋。
                if (remaining < 0 ||
                    compareAndSetState(available, remaining))
                    return remaining;
            }
        }


        public final void acquireSharedInterruptibly(int arg)
                throws InterruptedException {
            //条件成立：说明当前调用acquire方法的线程 已经是 中断状态了,直接抛出异常..
            if (Thread.interrupted())
                throw new InterruptedException();


            //对应业务层面 执行任务的线程已经将latch打破了。然后其他再调用latch.await的线程，就不会在这里阻塞了
            if (tryAcquireShared(arg) < 0)
                doAcquireSharedInterruptibly(arg);
        }

        private void doAcquireSharedInterruptibly(int arg)
                throws InterruptedException {
            //将调用Semaphore.aquire方法的线程 包装成node加入到 AQS的阻塞队列当中。
            final Node node = addWaiter(Node.SHARED);
            boolean failed = true;
            try {
                for (;;) {
                    //获取当前线程节点的前驱节点
                    final Node p = node.predecessor();
                    //条件成立，说明当前线程对应的节点 为 head.next节点
                    if (p == head) {
                        //head.next节点就有权利获取 共享锁了..
                        int r = tryAcquireShared(arg);


                        //站在Semaphore角度：r 表示还剩余的通行证数量
                        if (r >= 0) {
                            setHeadAndPropagate(node, r);
                            p.next = null; // help GC
                            failed = false;
                            return;
                        }
                    }
                    //shouldParkAfterFailedAcquire  会给当前线程找一个好爸爸，最终给爸爸节点设置状态为 signal（-1），返回true
                    //parkAndCheckInterrupt 挂起当前节点对应的线程...
                    if (shouldParkAfterFailedAcquire(p, node) &&
                            parkAndCheckInterrupt())
                        throw new InterruptedException();
                }
            } finally {
                if (failed)
                    cancelAcquire(node);
            }
        }


        /**
         * 设置当前节点为 head节点，并且向后传播！（依次唤醒！）
         */
        //AQS.setHeadAndPropagate
        private void setHeadAndPropagate(Node node, int propagate) {
            Node h = head; // Record old head for check below
            //将当前节点设置为 新的 head节点。
            setHead(node);
            //调用setHeadAndPropagete 时  propagate  == 1 一定成立
            if (propagate > 0 || h == null || h.waitStatus < 0 ||
                    (h = head) == null || h.waitStatus < 0) {
                //获取当前节点的后继节点..
                Node s = node.next;
                //条件一：s == null  什么时候成立呢？  当前node节点已经是 tail了，条件一会成立。 doReleaseShared() 里面会处理这种情况..
                //条件二：前置条件，s != null ， 要求s节点的模式必须是 共享模式。 latch.await() -> addWaiter(Node.SHARED)
                if (s == null || s.isShared())
                    //基本上所有情况都会执行到 doReleasseShared() 方法。
                    doReleaseShared();
            }
        }


        //AQS.releaseShared
        public final boolean releaseShared(int arg) {
            //条件成立：表示当前线程释放资源成功，释放资源成功后，去唤醒获取资源失败的线程..
            if (tryReleaseShared(arg)) {
                //唤醒获取资源失败的线程...
                doReleaseShared();
                return true;
            }
            return false;
        }

        /**
         * 唤醒获取资源失败的线程
         *
         * CountDownLatch版本
         * 都有哪几种路径会调用到doReleaseShared方法呢？
         * 1.latch.countDown() -> AQS.state == 0 -> doReleaseShared() 唤醒当前阻塞队列内的 head.next 对应的线程。
         * 2.被唤醒的线程 -> doAcquireSharedInterruptibly parkAndCheckInterrupt() 唤醒 -> setHeadAndPropagate() -> doReleaseShared()
         *
         * Semaphore版本
         * 都有哪几种路径会调用到doReleaseShared方法呢？
         *
         */
        //AQS.doReleaseShared
        private void doReleaseShared() {
            for (;;) {
                //获取当前AQS 内的 头结点
                Node h = head;
                //条件一：h != null 成立，说明阻塞队列不为空..
                //不成立：h == null 什么时候会是这样呢？
                //latch创建出来后，没有任何线程调用过 await() 方法之前，有线程调用latch.countDown()操作 且触发了 唤醒阻塞节点的逻辑..

                //条件二：h != tail 成立，说明当前阻塞队列内，除了head节点以外  还有其他节点。
                //h == tail  -> head 和 tail 指向的是同一个node对象。 什么时候会有这种情况呢？
                //1. 正常唤醒情况下，依次获取到 共享锁，当前线程执行到这里时 （这个线程就是 tail 节点。）
                //2. 第一个调用await()方法的线程 与 调用countDown()且触发唤醒阻塞节点的线程 出现并发了..
                //   因为await()线程是第一个调用 latch.await()的线程，此时队列内什么也没有，它需要补充创建一个Head节点，然后再次自旋时入队
                //   在await()线程入队完成之前，假设当前队列内 只有 刚刚补充创建的空元素 head 。
                //   同期，外部有一个调用countDown()的线程，将state 值从1，修改为0了，那么这个线程需要做 唤醒 阻塞队列内元素的逻辑..
                //   注意：调用await()的线程 因为完全入队完成之后，再次回到上层方法 doAcquireSharedInterruptibly 会进入到自旋中，
                //   获取当前元素的前驱，判断自己是head.next， 所以接下来该线程又会将自己设置为 head，然后该线程就从await()方法返回了...
                if (h != null && h != tail) {
                    //执行到if里面，说明当前head 一定有 后继节点!


                    int ws = h.waitStatus;
                    //当前head状态 为 signal 说明 后继节点并没有被唤醒过呢...
                    if (ws == Node.SIGNAL) {
                        //唤醒后继节点前 将head节点的状态改为 0
                        //这里为什么，使用CAS呢？ 回头说...
                        //当doReleaseShared方法 存在多个线程 唤醒 head.next 逻辑时，
                        //CAS 可能会失败...
                        //案例：
                        //t3 线程在if(h == head) 返回false时，t3 会继续自旋. 参与到 唤醒下一个head.next的逻辑..
                        //t3 此时执行到 CAS WaitStatus(h,Node.SIGNAL, 0) 成功.. t4 在t3修改成功之前，也进入到 if (ws == Node.SIGNAL) 里面了，
                        //但是t4 修改 CAS WaitStatus(h,Node.SIGNAL, 0) 会失败，因为 t3 改过了...
                        if (!compareAndSetWaitStatus(h, Node.SIGNAL, 0))
                            continue;            // loop to recheck cases
                        //唤醒后继节点
                        unparkSuccessor(h);
                    }

                    else if (ws == 0 &&
                            !compareAndSetWaitStatus(h, 0, Node.PROPAGATE))
                        continue;                // loop on failed CAS
                }

                //条件成立：
                //1.说明刚刚唤醒的 后继节点，还没执行到 setHeadAndPropagate方法里面的 设置当前唤醒节点为head的逻辑。
                //这个时候，当前线程 直接跳出去...结束了..
                //此时用不用担心，唤醒逻辑 在这里断掉呢？、
                //不需要担心，因为被唤醒的线程 早晚会执行到doReleaseShared方法。

                //2.h == null  latch创建出来后，没有任何线程调用过 await() 方法之前，
                //有线程调用latch.countDown()操作 且触发了 唤醒阻塞节点的逻辑..
                //3.h == tail  -> head 和 tail 指向的是同一个node对象

                //条件不成立：
                //被唤醒的节点 非常积极，直接将自己设置为了新的head，此时 唤醒它的节点（前驱），执行h == head 条件会不成立..
                //此时 head节点的前驱，不会跳出 doReleaseShared 方法，会继续唤醒 新head 节点的后继...
                if (h == head)                   // loop if head changed
                    break;
            }
        }
    }



    /**
     * Creates a {@code Semaphore} with the given number of
     * permits and nonfair fairness setting.
     *
     * @param permits the initial number of permits available.
     *        This value may be negative, in which case releases
     *        must occur before any acquires will be granted.
     */
    public Semaphore(int permits) {
        sync = new NonfairSync(permits);
    }

    /**
     * Creates a {@code Semaphore} with the given number of
     * permits and the given fairness setting.
     *
     * @param permits the initial number of permits available.
     *        This value may be negative, in which case releases
     *        must occur before any acquires will be granted.
     * @param fair {@code true} if this semaphore will guarantee
     *        first-in first-out granting of permits under contention,
     *        else {@code false}
     */
    public Semaphore(int permits, boolean fair) {
        sync = fair ? new FairSync(permits) : new NonfairSync(permits);
    }

    /**
     * Acquires a permit from this semaphore, blocking until one is
     * available, or the thread is {@linkplain Thread#interrupt interrupted}.
     *
     * <p>Acquires a permit, if one is available and returns immediately,
     * reducing the number of available permits by one.
     *
     * <p>If no permit is available then the current thread becomes
     * disabled for thread scheduling purposes and lies dormant until
     * one of two things happens:
     * <ul>
     * <li>Some other thread invokes the {@link #release} method for this
     * semaphore and the current thread is next to be assigned a permit; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread.
     * </ul>
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * for a permit,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * @throws InterruptedException if the current thread is interrupted
     */
    public void acquire() throws InterruptedException {
        sync.acquireSharedInterruptibly(1);
    }

    /**
     * Acquires a permit from this semaphore, blocking until one is
     * available.
     *
     * <p>Acquires a permit, if one is available and returns immediately,
     * reducing the number of available permits by one.
     *
     * <p>If no permit is available then the current thread becomes
     * disabled for thread scheduling purposes and lies dormant until
     * some other thread invokes the {@link #release} method for this
     * semaphore and the current thread is next to be assigned a permit.
     *
     * <p>If the current thread is {@linkplain Thread#interrupt interrupted}
     * while waiting for a permit then it will continue to wait, but the
     * time at which the thread is assigned a permit may change compared to
     * the time it would have received the permit had no interruption
     * occurred.  When the thread does return from this method its interrupt
     * status will be set.
     */
    public void acquireUninterruptibly() {
        sync.acquireShared(1);
    }

    /**
     * Acquires a permit from this semaphore, only if one is available at the
     * time of invocation.
     *
     * <p>Acquires a permit, if one is available and returns immediately,
     * with the value {@code true},
     * reducing the number of available permits by one.
     *
     * <p>If no permit is available then this method will return
     * immediately with the value {@code false}.
     *
     * <p>Even when this semaphore has been set to use a
     * fair ordering policy, a call to {@code tryAcquire()} <em>will</em>
     * immediately acquire a permit if one is available, whether or not
     * other threads are currently waiting.
     * This &quot;barging&quot; behavior can be useful in certain
     * circumstances, even though it breaks fairness. If you want to honor
     * the fairness setting, then use
     * {@link #tryAcquire(long, TimeUnit) tryAcquire(0, TimeUnit.SECONDS) }
     * which is almost equivalent (it also detects interruption).
     *
     * @return {@code true} if a permit was acquired and {@code false}
     *         otherwise
     */
    public boolean tryAcquire() {
        return sync.nonfairTryAcquireShared(1) >= 0;
    }

    /**
     * Acquires a permit from this semaphore, if one becomes available
     * within the given waiting time and the current thread has not
     * been {@linkplain Thread#interrupt interrupted}.
     *
     * <p>Acquires a permit, if one is available and returns immediately,
     * with the value {@code true},
     * reducing the number of available permits by one.
     *
     * <p>If no permit is available then the current thread becomes
     * disabled for thread scheduling purposes and lies dormant until
     * one of three things happens:
     * <ul>
     * <li>Some other thread invokes the {@link #release} method for this
     * semaphore and the current thread is next to be assigned a permit; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     * <li>The specified waiting time elapses.
     * </ul>
     *
     * <p>If a permit is acquired then the value {@code true} is returned.
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * to acquire a permit,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * <p>If the specified waiting time elapses then the value {@code false}
     * is returned.  If the time is less than or equal to zero, the method
     * will not wait at all.
     *
     * @param timeout the maximum time to wait for a permit
     * @param unit the time unit of the {@code timeout} argument
     * @return {@code true} if a permit was acquired and {@code false}
     *         if the waiting time elapsed before a permit was acquired
     * @throws InterruptedException if the current thread is interrupted
     */
    public boolean tryAcquire(long timeout, TimeUnit unit)
        throws InterruptedException {
        return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
    }

    /**
     * Releases a permit, returning it to the semaphore.
     *
     * <p>Releases a permit, increasing the number of available permits by
     * one.  If any threads are trying to acquire a permit, then one is
     * selected and given the permit that was just released.  That thread
     * is (re)enabled for thread scheduling purposes.
     *
     * <p>There is no requirement that a thread that releases a permit must
     * have acquired that permit by calling {@link #acquire}.
     * Correct usage of a semaphore is established by programming convention
     * in the application.
     */
    public void release() {
        sync.releaseShared(1);
    }

    /**
     * Acquires the given number of permits from this semaphore,
     * blocking until all are available,
     * or the thread is {@linkplain Thread#interrupt interrupted}.
     *
     * <p>Acquires the given number of permits, if they are available,
     * and returns immediately, reducing the number of available permits
     * by the given amount.
     *
     * <p>If insufficient permits are available then the current thread becomes
     * disabled for thread scheduling purposes and lies dormant until
     * one of two things happens:
     * <ul>
     * <li>Some other thread invokes one of the {@link #release() release}
     * methods for this semaphore, the current thread is next to be assigned
     * permits and the number of available permits satisfies this request; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread.
     * </ul>
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * for a permit,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     * Any permits that were to be assigned to this thread are instead
     * assigned to other threads trying to acquire permits, as if
     * permits had been made available by a call to {@link #release()}.
     *
     * @param permits the number of permits to acquire
     * @throws InterruptedException if the current thread is interrupted
     * @throws IllegalArgumentException if {@code permits} is negative
     */
    public void acquire(int permits) throws InterruptedException {
        if (permits < 0) throw new IllegalArgumentException();
        sync.acquireSharedInterruptibly(permits);
    }

    /**
     * Acquires the given number of permits from this semaphore,
     * blocking until all are available.
     *
     * <p>Acquires the given number of permits, if they are available,
     * and returns immediately, reducing the number of available permits
     * by the given amount.
     *
     * <p>If insufficient permits are available then the current thread becomes
     * disabled for thread scheduling purposes and lies dormant until
     * some other thread invokes one of the {@link #release() release}
     * methods for this semaphore, the current thread is next to be assigned
     * permits and the number of available permits satisfies this request.
     *
     * <p>If the current thread is {@linkplain Thread#interrupt interrupted}
     * while waiting for permits then it will continue to wait and its
     * position in the queue is not affected.  When the thread does return
     * from this method its interrupt status will be set.
     *
     * @param permits the number of permits to acquire
     * @throws IllegalArgumentException if {@code permits} is negative
     */
    public void acquireUninterruptibly(int permits) {
        if (permits < 0) throw new IllegalArgumentException();
        sync.acquireShared(permits);
    }

    /**
     * Acquires the given number of permits from this semaphore, only
     * if all are available at the time of invocation.
     *
     * <p>Acquires the given number of permits, if they are available, and
     * returns immediately, with the value {@code true},
     * reducing the number of available permits by the given amount.
     *
     * <p>If insufficient permits are available then this method will return
     * immediately with the value {@code false} and the number of available
     * permits is unchanged.
     *
     * <p>Even when this semaphore has been set to use a fair ordering
     * policy, a call to {@code tryAcquire} <em>will</em>
     * immediately acquire a permit if one is available, whether or
     * not other threads are currently waiting.  This
     * &quot;barging&quot; behavior can be useful in certain
     * circumstances, even though it breaks fairness. If you want to
     * honor the fairness setting, then use {@link #tryAcquire(int,
     * long, TimeUnit) tryAcquire(permits, 0, TimeUnit.SECONDS) }
     * which is almost equivalent (it also detects interruption).
     *
     * @param permits the number of permits to acquire
     * @return {@code true} if the permits were acquired and
     *         {@code false} otherwise
     * @throws IllegalArgumentException if {@code permits} is negative
     */
    public boolean tryAcquire(int permits) {
        if (permits < 0) throw new IllegalArgumentException();
        return sync.nonfairTryAcquireShared(permits) >= 0;
    }

    /**
     * Acquires the given number of permits from this semaphore, if all
     * become available within the given waiting time and the current
     * thread has not been {@linkplain Thread#interrupt interrupted}.
     *
     * <p>Acquires the given number of permits, if they are available and
     * returns immediately, with the value {@code true},
     * reducing the number of available permits by the given amount.
     *
     * <p>If insufficient permits are available then
     * the current thread becomes disabled for thread scheduling
     * purposes and lies dormant until one of three things happens:
     * <ul>
     * <li>Some other thread invokes one of the {@link #release() release}
     * methods for this semaphore, the current thread is next to be assigned
     * permits and the number of available permits satisfies this request; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     * <li>The specified waiting time elapses.
     * </ul>
     *
     * <p>If the permits are acquired then the value {@code true} is returned.
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * to acquire the permits,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     * Any permits that were to be assigned to this thread, are instead
     * assigned to other threads trying to acquire permits, as if
     * the permits had been made available by a call to {@link #release()}.
     *
     * <p>If the specified waiting time elapses then the value {@code false}
     * is returned.  If the time is less than or equal to zero, the method
     * will not wait at all.  Any permits that were to be assigned to this
     * thread, are instead assigned to other threads trying to acquire
     * permits, as if the permits had been made available by a call to
     * {@link #release()}.
     *
     * @param permits the number of permits to acquire
     * @param timeout the maximum time to wait for the permits
     * @param unit the time unit of the {@code timeout} argument
     * @return {@code true} if all permits were acquired and {@code false}
     *         if the waiting time elapsed before all permits were acquired
     * @throws InterruptedException if the current thread is interrupted
     * @throws IllegalArgumentException if {@code permits} is negative
     */
    public boolean tryAcquire(int permits, long timeout, TimeUnit unit)
        throws InterruptedException {
        if (permits < 0) throw new IllegalArgumentException();
        return sync.tryAcquireSharedNanos(permits, unit.toNanos(timeout));
    }

    /**
     * Releases the given number of permits, returning them to the semaphore.
     *
     * <p>Releases the given number of permits, increasing the number of
     * available permits by that amount.
     * If any threads are trying to acquire permits, then one
     * is selected and given the permits that were just released.
     * If the number of available permits satisfies that thread's request
     * then that thread is (re)enabled for thread scheduling purposes;
     * otherwise the thread will wait until sufficient permits are available.
     * If there are still permits available
     * after this thread's request has been satisfied, then those permits
     * are assigned in turn to other threads trying to acquire permits.
     *
     * <p>There is no requirement that a thread that releases a permit must
     * have acquired that permit by calling {@link Semaphore#acquire acquire}.
     * Correct usage of a semaphore is established by programming convention
     * in the application.
     *
     * @param permits the number of permits to release
     * @throws IllegalArgumentException if {@code permits} is negative
     */
    public void release(int permits) {
        if (permits < 0) throw new IllegalArgumentException();
        sync.releaseShared(permits);
    }

    /**
     * Returns the current number of permits available in this semaphore.
     *
     * <p>This method is typically used for debugging and testing purposes.
     *
     * @return the number of permits available in this semaphore
     */
    public int availablePermits() {
        return sync.getPermits();
    }

    /**
     * Acquires and returns all permits that are immediately available.
     *
     * @return the number of permits acquired
     */
    public int drainPermits() {
        return sync.drainPermits();
    }

    /**
     * Shrinks the number of available permits by the indicated
     * reduction. This method can be useful in subclasses that use
     * semaphores to track resources that become unavailable. This
     * method differs from {@code acquire} in that it does not block
     * waiting for permits to become available.
     *
     * @param reduction the number of permits to remove
     * @throws IllegalArgumentException if {@code reduction} is negative
     */
    protected void reducePermits(int reduction) {
        if (reduction < 0) throw new IllegalArgumentException();
        sync.reducePermits(reduction);
    }

    /**
     * Returns {@code true} if this semaphore has fairness set true.
     *
     * @return {@code true} if this semaphore has fairness set true
     */
    public boolean isFair() {
        return sync instanceof FairSync;
    }

    /**
     * Queries whether any threads are waiting to acquire. Note that
     * because cancellations may occur at any time, a {@code true}
     * return does not guarantee that any other thread will ever
     * acquire.  This method is designed primarily for use in
     * monitoring of the system state.
     *
     * @return {@code true} if there may be other threads waiting to
     *         acquire the lock
     */
    public final boolean hasQueuedThreads() {
        return sync.hasQueuedThreads();
    }

    /**
     * Returns an estimate of the number of threads waiting to acquire.
     * The value is only an estimate because the number of threads may
     * change dynamically while this method traverses internal data
     * structures.  This method is designed for use in monitoring of the
     * system state, not for synchronization control.
     *
     * @return the estimated number of threads waiting for this lock
     */
    public final int getQueueLength() {
        return sync.getQueueLength();
    }

    /**
     * Returns a collection containing threads that may be waiting to acquire.
     * Because the actual set of threads may change dynamically while
     * constructing this result, the returned collection is only a best-effort
     * estimate.  The elements of the returned collection are in no particular
     * order.  This method is designed to facilitate construction of
     * subclasses that provide more extensive monitoring facilities.
     *
     * @return the collection of threads
     */
    protected Collection<Thread> getQueuedThreads() {
        return sync.getQueuedThreads();
    }

    /**
     * Returns a string identifying this semaphore, as well as its state.
     * The state, in brackets, includes the String {@code "Permits ="}
     * followed by the number of permits.
     *
     * @return a string identifying this semaphore, as well as its state
     */
    public String toString() {
        return super.toString() + "[Permits = " + sync.getPermits() + "]";
    }
}
