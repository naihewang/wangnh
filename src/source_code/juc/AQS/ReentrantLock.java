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

package source_code.juc.AQS;
import java.util.concurrent.TimeUnit;
import java.util.Collection;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;

/**
 * A reentrant mutual exclusion {@link Lock} with the same basic
 * behavior and semantics as the implicit monitor lock accessed using
 * {@code synchronized} methods and statements, but with extended
 * capabilities.
 *
 * <p>A {@code ReentrantLock} is <em>owned</em> by the thread last
 * successfully locking, but not yet unlocking it. A thread invoking
 * {@code lock} will return, successfully acquiring the lock, when
 * the lock is not owned by another thread. The method will return
 * immediately if the current thread already owns the lock. This can
 * be checked using methods {@link #isHeldByCurrentThread}, and {@link
 * #getHoldCount}.
 *
 * <p>The constructor for this class accepts an optional
 * <em>fairness</em> parameter.  When set {@code true}, under
 * contention, locks favor granting access to the longest-waiting
 * thread.  Otherwise this lock does not guarantee any particular
 * access order.  Programs using fair locks accessed by many threads
 * may display lower overall throughput (i.e., are slower; often much
 * slower) than those using the default setting, but have smaller
 * variances in times to obtain locks and guarantee lack of
 * starvation. Note however, that fairness of locks does not guarantee
 * fairness of thread scheduling. Thus, one of many threads using a
 * fair lock may obtain it multiple times in succession while other
 * active threads are not progressing and not currently holding the
 * lock.
 * Also note that the untimed {@link #tryLock()} method does not
 * honor the fairness setting. It will succeed if the lock
 * is available even if other threads are waiting.
 *
 * <p>It is recommended practice to <em>always</em> immediately
 * follow a call to {@code lock} with a {@code try} block, most
 * typically in a before/after construction such as:
 *
 *  <pre> {@code
 * class X {
 *   private final ReentrantLock lock = new ReentrantLock();
 *   // ...
 *
 *   public void m() {
 *     lock.lock();  // block until condition holds
 *     try {
 *       // ... method body
 *     } finally {
 *       lock.unlock()
 *     }
 *   }
 * }}</pre>
 *
 * <p>In addition to implementing the {@link Lock} interface, this
 * class defines a number of {@code public} and {@code protected}
 * methods for inspecting the state of the lock.  Some of these
 * methods are only useful for instrumentation and monitoring.
 *
 * <p>Serialization of this class behaves in the same way as built-in
 * locks: a deserialized lock is in the unlocked state, regardless of
 * its state when serialized.
 *
 * <p>This lock supports a maximum of 2147483647 recursive locks by
 * the same thread. Attempts to exceed this limit result in
 * {@link Error} throws from locking methods.
 *
 * @since 1.5
 * @author Doug Lea
 */
public class ReentrantLock implements Lock, java.io.Serializable {
    private static final long serialVersionUID = 7373984872572414699L;
    /** Synchronizer providing all implementation mechanics */
    private final Sync sync;

    /**
     * Base of synchronization control for this lock. Subclassed
     * into fair and nonfair versions below. Uses AQS state to
     * represent the number of holds on the lock.
     */
    abstract static class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = -5179523762034025860L;

        /**
         * Performs {@link Lock#lock}. The main reason for subclassing
         * is to allow fast path for nonfair version.
         */
        abstract void lock();

        /**
         * Performs non-fair tryLock.  tryAcquire is implemented in
         * subclasses, but both need nonfair try for trylock method.
         */
        final boolean nonfairTryAcquire(int acquires) {
            final Thread current = Thread.currentThread();
            int c = getState();
            if (c == 0) {
                if (compareAndSetState(0, acquires)) {
                    setExclusiveOwnerThread(current);
                    return true;
                }
            }
            else if (current == getExclusiveOwnerThread()) {
                int nextc = c + acquires;
                if (nextc < 0) // overflow
                    throw new Error("Maximum lock count exceeded");
                setState(nextc);
                return true;
            }
            return false;
        }

        protected final boolean tryRelease(int releases) {
            int c = getState() - releases;
            if (Thread.currentThread() != getExclusiveOwnerThread())
                throw new IllegalMonitorStateException();
            boolean free = false;
            if (c == 0) {
                free = true;
                setExclusiveOwnerThread(null);
            }
            setState(c);
            return free;
        }

        protected final boolean isHeldExclusively() {
            // While we must in general read state before owner,
            // we don't need to do so to check if current thread is owner
            return getExclusiveOwnerThread() == Thread.currentThread();
        }

        final ConditionObject newCondition() {
            return new ConditionObject();
        }

        // Methods relayed from outer class

        final Thread getOwner() {
            return getState() == 0 ? null : getExclusiveOwnerThread();
        }

        final int getHoldCount() {
            return isHeldExclusively() ? getState() : 0;
        }

        final boolean isLocked() {
            return getState() != 0;
        }

        /**
         * Reconstitutes the instance from a stream (that is, deserializes it).
         */
        private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {
            s.defaultReadObject();
            setState(0); // reset to unlocked state
        }
    }

    /**
     * Sync object for non-fair locks
     */
    static final class NonfairSync extends Sync {
        private static final long serialVersionUID = 7316153563782823691L;

        /**
         * Performs lock.  Try immediate barge, backing up to normal
         * acquire on failure.
         */
        final void lock() {
            if (compareAndSetState(0, 1))
                setExclusiveOwnerThread(Thread.currentThread());
            else
                acquire(1);
        }

        protected final boolean tryAcquire(int acquires) {
            return nonfairTryAcquire(acquires);
        }
    }

    /**
     * Sync object for fair locks
     * ��ƽ��
     */
    static final class FairSync extends Sync {
        private static final long serialVersionUID = -3000897897090466540L;

        //AQS#cancelAcquire
        /**
         * ȡ��ָ��node���뾺����
         */
        private void cancelAcquire(Node node) {
            //���ж�..
            if (node == null)
                return;

            //��Ϊ�Ѿ�ȡ���Ŷ���..����node�ڲ������ĵ�ǰ�̣߳���ΪNull�ͺ��ˡ���
            node.thread = null;

            //��ȡ��ǰȡ���Ŷ�node��ǰ����
            Node pred = node.prev;

            while (pred.waitStatus > 0)
                node.prev = pred = pred.prev;

            //�õ�ǰ���ĺ�̽ڵ㡣
            //1.��ǰnode
            //2.����Ҳ�� ws > 0 �Ľڵ㡣
            Node predNext = pred.next;

            //����ǰnode״̬����Ϊ ȡ��״̬  1
            node.waitStatus = Node.CANCELLED;



            /**
             * ��ǰȡ���Ŷӵ�node���� ���е�λ�ò�ͬ��ִ�еĳ��Ӳ����ǲ�һ���ģ�һ����Ϊ���������
             * 1.��ǰnode�Ƕ�β  tail -> node
             * 2.��ǰnode ���� head.next �ڵ㣬Ҳ���� tail
             * 3.��ǰnode �� head.next�ڵ㡣
             */


            //����һ��node == tail  ��������ǰnode�Ƕ�β  tail -> node
            //��������compareAndSetTail(node, pred) �ɹ��Ļ���˵���޸�tail��ɡ�
            if (node == tail && compareAndSetTail(node, pred)) {
                //�޸�pred.next -> null. ���node���ӡ�
                compareAndSetNext(pred, predNext, null);

            } else {


                //����ڵ� ״̬..
                int ws;

                //�ڶ����������ǰnode ���� head.next �ڵ㣬Ҳ���� tail
                //����һ��pred != head ������ ˵����ǰnode ���� head.next �ڵ㣬Ҳ���� tail
                //�������� ((ws = pred.waitStatus) == Node.SIGNAL || (ws <= 0 && compareAndSetWaitStatus(pred, ws, Node.SIGNAL)))
                //����2.1��(ws = pred.waitStatus) == Node.SIGNAL   ������˵��node��ǰ��״̬�� Signal ״̬   ��������ǰ��״̬������0 ��
                // ��������£�ǰ��Ҳȡ���Ŷ���..
                //����2.2:(ws <= 0 && compareAndSetWaitStatus(pred, ws, Node.SIGNAL))
                // ����ǰ��״̬�� <= 0 ������ǰ��״̬Ϊ Signal״̬..��ʾҪ���Ѻ�̽ڵ㡣
                //if�����������飬������pred.next -> node.next  ,������Ҫ��֤pred�ڵ�״̬Ϊ Signal״̬��
                if (pred != head &&
                        ((ws = pred.waitStatus) == Node.SIGNAL ||
                                (ws <= 0 && compareAndSetWaitStatus(pred, ws, Node.SIGNAL))) &&
                        pred.thread != null) {
                    //���2����ǰnode ���� head.next �ڵ㣬Ҳ���� tail
                    //���ӣ�pred.next -> node.next �ڵ�󣬵�node.next�ڵ� �����Ѻ�
                    //���� shouldParkAfterFailedAcquire ����node.next �ڵ�Խ��ȡ��״̬�Ľڵ�
                    //����������ӡ�
                    Node next = node.next;
                    if (next != null && next.waitStatus <= 0)
                        compareAndSetNext(pred, predNext, next);

                } else {
                    //��ǰnode �� head.next�ڵ㡣  ������...
                    //�������2����̽ڵ㻽�Ѻ󣬻���� shouldParkAfterFailedAcquire ����node.next �ڵ�Խ��ȡ��״̬�Ľڵ�
                    //���еĵ������ڵ� �� ֱ�� �� head ���� ˫��ָ��Ĺ�ϵ��
                    //head.next -> ������node  �м���Ǳ����ӵ�head.next ������node.prev -> head
                    unparkSuccessor(node);
                }

                node.next = node; // help GC
            }
        }

        //��ƽ�����..
        //����Ӧ�жϵļ���..��Ӧ�жϵ� ����Ϊ��lockinterrupted����
        final void lock() {
            acquire(1);
        }

        //AQS#release����
        //ReentrantLock.unlock() -> sync.release()��AQS�ṩ��release��
        public final boolean release(int arg) {
            //�����ͷ�����tryRelease ����true ��ʾ��ǰ�߳��Ѿ���ȫ�ͷ���
            //����false��˵����ǰ�߳���δ��ȫ�ͷ���..
            if (tryRelease(arg)) {

                //headʲô����»ᱻ����������
                //�������߳�δ�ͷ��߳�ʱ���ҳ����ڼ� �������߳���Ҫ��ȡ��ʱ�������̷߳��ֻ�ȡ�����������Ҷ����ǿն��У���ʱ�����̻߳�Ϊ��ǰ�����е�
                //�߳� ��������һ��head�ڵ㣬Ȼ������߳�  ��׷�ӵ� head �ڵ���档
                Node h = head;

                //����һ:������˵�������е�head�ڵ��Ѿ���ʼ�����ˣ�ReentrantLock ��ʹ���ڼ� ������ ���߳̾�����...
                //������������������˵����ǰhead����һ�������node�ڵ㡣
                if (h != null && h.waitStatus != 0)
                    //���Ѻ�̽ڵ�..
                    unparkSuccessor(h);
                return true;
            }

            return false;
        }

        //AQS#unparkSuccessor
        /**
         * ���ѵ�ǰ�ڵ����һ���ڵ㡣
         */
        private void unparkSuccessor(Node node) {
            //��ȡ��ǰ�ڵ��״̬
            int ws = node.waitStatus;

            if (ws < 0)//-1 Signal  �ĳ����ԭ����Ϊ��ǰ�ڵ��Ѿ���ɺ���̽ڵ��������..
                compareAndSetWaitStatus(node, ws, 0);

            //s�ǵ�ǰ�ڵ� �ĵ�һ����̽ڵ㡣
            Node s = node.next;

            //����һ��
            //s ʲôʱ�����null��
            //1.��ǰ�ڵ����tail�ڵ�ʱ  s == null��
            //2.���½ڵ����δ���ʱ��1.�����½ڵ��prev ָ��pred  2.cas�����½ڵ�Ϊtail   3.��δ��ɣ�pred.next -> �½ڵ� ��
            //��Ҫ�ҵ����Ա����ѵĽڵ�..

            //��������s.waitStatus > 0    ǰ�᣺s ��= null
            //������˵�� ��ǰnode�ڵ�ĺ�̽ڵ��� ȡ��״̬... ��Ҫ��һ�����ʵĿ��Ա����ѵĽڵ�..
            if (s == null || s.waitStatus > 0) {
                //���ҿ��Ա����ѵĽڵ�...
                s = null;
                for (Node t = tail; t != null && t != node; t = t.prev)
                    if (t.waitStatus <= 0)
                        s = t;

              //����ѭ�������ҵ�һ���뵱ǰnode�����һ�����Ա����ѵ�node�� node �����Ҳ���  node �п�����null����
            }


            //����ҵ����ʵĿ��Ա����ѵ�node������.. �Ҳ��� ɶҲ������
            if (s != null)
                LockSupport.unpark(s.thread);
        }


        //Sync#tryRelease()
        protected final boolean tryRelease(int releases) {
            //��ȥ�ͷŵ�ֵ..
            int c = getState() - releases;
            //����������˵����ǰ�̲߳�δ����..ֱ���쳣.,.
            if (Thread.currentThread() != getExclusiveOwnerThread())
                throw new IllegalMonitorStateException();

            //��ǰ�̳߳�����..


            //�Ƿ��Ѿ���ȫ�ͷ���..Ĭ��false
            boolean free = false;
            //����������˵����ǰ�߳��Ѿ��ﵽ��ȫ�ͷ����������� c == 0
            if (c == 0) {
                free = true;
                setExclusiveOwnerThread(null);
            }
            //����AQS.stateֵ
            setState(c);
            return free;
        }


        //AQS#acquire
        public final void acquire(int arg) {
            //����һ��!tryAcquire ���Ի�ȡ�� ��ȡ�ɹ�����true  ��ȡʧ�� ����false��
            //��������2.1��addWaiter ����ǰ�̷߳�װ��node���
            //       2.2��acquireQueued ����ǰ�߳�   ���Ѻ���ص��߼�..
            //      acquireQueued ����true ��ʾ����������̱߳��жϻ��ѹ�..  false ��ʾδ���жϹ�..
            if (!tryAcquire(arg) &&
                    acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
                //�ٴ������жϱ��λ true
                selfInterrupt();
        }

        //acquireQueued ��Ҫ��ʲô�أ�
        //1.��ǰ�ڵ���û�б�park? ���� û��  ==> ����Ĳ���
        //2.����֮����߼������أ�   ==> ����֮����߼���
        //AQS#acquireQueued
        //����һ��node ���ǵ�ǰ�̰߳�װ������node���ҵ�ǰʱ�� �Ѿ���ӳɹ���..
        //����������ǰ�߳���ռ��Դ�ɹ�������stateֵʱ ���õ���
        final boolean acquireQueued(final Node node, int arg) {
            //true ��ʾ��ǰ�߳���ռ���ɹ�����ͨ����¡�lock�� ��ǰ�߳�������õ���..
            //false ��ʾʧ�ܣ���Ҫִ�г��ӵ��߼�... ����ͷ�� ��Ӧ�жϵ�lock����ʱ�ٽ�����
            boolean failed = true;
            try {
                //��ǰ�߳��Ƿ��ж�
                boolean interrupted = false;
                //����..
                for (;;) {


                    //ʲôʱ���ִ�����
                    //1.����forѭ��ʱ ���߳���δparkǰ��ִ��
                    //2.�߳�park֮�� �����Ѻ�Ҳ��ִ������...


                    //��ȡ��ǰ�ڵ��ǰ�ýڵ�..
                    final Node p = node.predecessor();
                    //����һ������p == head  ˵����ǰ�ڵ�Ϊhead.next�ڵ㣬head.next�ڵ����κ�ʱ�� ����Ȩ��ȥ������.
                    //��������tryAcquire(arg)
                    //������˵��head��Ӧ���߳� �Ѿ��ͷ����ˣ�head.next�ڵ��Ӧ���̣߳����û�ȡ������..
                    //��������˵��head��Ӧ���߳�  ��û�ͷ�����...head.next��Ȼ��Ҫ��park����
                    if (p == head && tryAcquire(arg)) {
                        //�õ���֮����Ҫ��ʲô��
                        //�����Լ�Ϊhead�ڵ㡣
                        setHead(node);
                        //���ϸ��̶߳�Ӧ��node��next������Ϊnull��Э���ϵ�head����..
                        p.next = null; // help GC
                        //��ǰ�߳� ��ȡ�� ������..û���쳣
                        failed = false;
                        //���ص�ǰ�̵߳��жϱ��..
                        return interrupted;
                    }

                    //shouldParkAfterFailedAcquire  ��������Ǹ���ģ� ��ǰ�̻߳�ȡ����Դʧ�ܺ��Ƿ���Ҫ�����أ�
                    //����ֵ��true -> ��ǰ�߳���Ҫ ����    false -> ����Ҫ..
                    //parkAndCheckInterrupt()  �������ʲô���ã� ����ǰ�̣߳����һ���֮�� ���� ��ǰ�̵߳� �жϱ��
                    // �����ѣ�1.�������� �����߳� unpark 2.�����̸߳���ǰ������߳� һ���ж��ź�..��
                    if (shouldParkAfterFailedAcquire(p, node) &&
                            parkAndCheckInterrupt())
                        //interrupted == true ��ʾ��ǰnode��Ӧ���߳��Ǳ� �ж��źŻ��ѵ�...
                        interrupted = true;
                }
            } finally {
                if (failed)
                    cancelAcquire(node);
            }
        }

        //AQS#parkAndCheckInterrupt
        //park��ǰ�߳� ����ǰ�߳� ���𣬻��Ѻ󷵻ص�ǰ�߳� �Ƿ�Ϊ �ж��ź� ���ѡ�
        private final boolean parkAndCheckInterrupt() {
            LockSupport.park(this);
            return Thread.interrupted();
        }

        /**
         * �ܽ᣺
         * 1.��ǰ�ڵ��ǰ�ýڵ��� ȡ��״̬ ����һ�������������ʱ ��Խ�� ȡ��״̬�Ľڵ㣬 �ڶ��� �᷵��true Ȼ��park��ǰ�߳�
         * 2.��ǰ�ڵ��ǰ�ýڵ�״̬��0����ǰ�̻߳�����ǰ�ýڵ��״̬Ϊ -1 ���ڶ������������������ʱ  �᷵��true Ȼ��park��ǰ�߳�.
         *
         * ����һ��pred ��ǰ�߳�node��ǰ�ýڵ�
         * ��������node ��ǰ�̶߳�Ӧnode
         * ����ֵ��boolean  true ��ʾ��ǰ�߳���Ҫ����..
         */
        private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
            //��ȡǰ�ýڵ��״̬
            //waitStatus��0 Ĭ��״̬ new Node() �� -1 Signal״̬����ʾ��ǰ�ڵ��ͷ���֮��ỽ�����ĵ�һ����̽ڵ㣻 >0 ��ʾ��ǰ�ڵ���CANCELED״̬
            int ws = pred.waitStatus;
            //������������ʾǰ�ýڵ��Ǹ����Ի��ѵ�ǰ�ڵ�Ľڵ㣬���Է���true ==> parkAndCheckInterrupt() park��ǰ�߳���..
            //��ͨ����£���һ������shouldPark������ ws ������ -1
            if (ws == Node.SIGNAL)
                return true;

            //���������� >0 ��ʾǰ�ýڵ���CANCELED״̬
            if (ws > 0) {
                //�ҰְֵĹ��̣�������ʲô�أ� ǰ�ýڵ�� waitStatus <= 0 �������
                do {
                    node.prev = pred = pred.prev;
                } while (pred.waitStatus > 0);
                //�ҵ��ðְֺ��˳�ѭ��
                //������һ�ֲ�����CANCELED״̬�Ľڵ�ᱻ���ӡ�
                pred.next = node;

            } else {
                //��ǰnodeǰ�ýڵ��״̬���� 0 ����һ�������
                //����ǰ�߳�node��ǰ��node��״̬ǿ������Ϊ SIGNAl����ʾǰ�ýڵ��ͷ���֮����Ҫ ������..
                compareAndSetWaitStatus(pred, ws, Node.SIGNAL);
            }
            return false;
        }



        //AQS#addWaiter
        //���շ��ص�ǰ�̰߳�װ������node
        private Node addWaiter(Node mode) {
            //Node.EXCLUSIVE
            //����Node ���ѵ�ǰ�̷߳�װ������node����
            Node node = new Node(Thread.currentThread(), mode);
            // Try the fast path of enq; backup to full enq on failure
            //�������
            //��ȡ��β�ڵ� ���浽pred������
            Node pred = tail;
            //�����������������Ѿ���node��
            if (pred != null) {
                //��ǰ�ڵ��prev ָ�� pred
                node.prev = pred;
                //cas�ɹ���˵��node��ӳɹ�
                if (compareAndSetTail(pred, node)) {
                    //ǰ�ýڵ�ָ��ǰnode����� ˫��󶨡�
                    pred.next = node;
                    return node;
                }
            }

            //ʲôʱ���ִ�е������أ�
            //1.��ǰ�����ǿն���  tail == null
            //2.CAS�������ʧ��..����������..

            //�������..
            enq(node);

            return node;
        }

        //AQS#enq()
        //����ֵ�����ص�ǰ�ڵ�� ǰ�ýڵ㡣
        private Node enq(final Node node) {
            //������ӣ�ֻ�е�ǰnode��ӳɹ��󣬲Ż�����ѭ����
            for (;;) {
                Node t = tail;
                //1.��ǰ�����ǿն���  tail == null
                //˵����ǰ ����ռ�ã��ҵ�ǰ�߳� �п����ǵ�һ����ȡ��ʧ�ܵ��̣߳���ǰʱ�̿��ܴ���һ����ȡ��ʧ�ܵ��߳�...��
                if (t == null) { // Must initialize
                    //��Ϊ��ǰ�����̵߳� ��һ�� ����̣߳���Ҫ��ʲô�£�
                    //1.��Ϊ��ǰ�������̣߳�����ȡ��ʱ��ֱ��tryAcquire�ɹ��ˣ�û���� �������� ������κ�node��������Ϊ�����ҪΪ����ƨ��..
                    //2.Ϊ�Լ�׷��node

                    //CAS�ɹ���˵����ǰ�߳� ��Ϊhead.next�ڵ㡣
                    //�߳���ҪΪ��ǰ�������߳� ����head��
                    if (compareAndSetHead(new Node()))
                        tail = head;

                    //ע�⣺����û��return,�����for����
                } else {
                    //��ͨ��ӷ�ʽ��ֻ������for�У��ᱣ֤һ����ӳɹ���
                    node.prev = t;
                    if (compareAndSetTail(t, node)) {
                        t.next = node;
                        return t;
                    }
                }
            }
        }


        /**
         * Fair version of tryAcquire.  Don't grant access unless
         * recursive call or no waiters or is first.
         * ��ռ�ɹ�������true  ��������..
         * ��ռʧ�ܣ�����false
         */
        protected final boolean tryAcquire(int acquires) {
            //current ��ǰ�߳�
            final Thread current = Thread.currentThread();
            //AQS state ֵ
            int c = getState();
            //����������c == 0 ��ʾ��ǰAQS��������״̬..
            if (c == 0) {
                //����һ��
                //��ΪfairSync�ǹ�ƽ�����κ�ʱ����Ҫ���һ�� �������Ƿ��ڵ�ǰ�߳�֮ǰ�еȴ���..
                //hasQueuedPredecessors() �������� true ��ʾ��ǰ�߳�ǰ���еȴ��ߣ���ǰ�߳���Ҫ��ӵȴ�
                //hasQueuedPredecessors() �������� false ��ʾ��ǰ�߳�ǰ���޵ȴ��ߣ�ֱ�ӳ��Ի�ȡ��..

                //��������compareAndSetState(0, acquires)
                //�ɹ���˵����ǰ�߳���ռ���ɹ�
                //ʧ�ܣ�˵�����ھ������ҵ�ǰ�߳̾���ʧ��..
                if (!hasQueuedPredecessors() &&
                    compareAndSetState(0, acquires)) {
                    //�ɹ�֮����Ҫ��ʲô��
                    //���õ�ǰ�߳�Ϊ ��ռ�� �̡߳�
                    setExclusiveOwnerThread(current);
                    return true;
                }
            }
            //ִ�е�����м��������
            //c != 0 ����0 ������������������Ҫ���һ�� ��ǰ�߳��ǲ��� ��ռ�����̣߳���ΪReentrantLock�ǿ��������.

            //����������˵����ǰ�߳̾��Ƕ�ռ���߳�..
            else if (current == getExclusiveOwnerThread()) {
                //��������߼�..

                //nextc ����ֵ..
                int nextc = c + acquires;
                //Խ���жϣ����������Ⱥ���ʱ���ᵼ�� nextc < 0 ��intֵ�ﵽ���֮�� �� + 1 ...�为��..
                if (nextc < 0)
                    throw new Error("Maximum lock count exceeded");
                //���µĲ���
                setState(nextc);
                return true;
            }

            //ִ�е����
            //1.CASʧ��  c == 0 ʱ��CAS�޸� state ʱ δ���������߳�...
            //2.c > 0 �� ownerThread != currentThread.
            return false;
        }
    }

    /**
     * Creates an instance of {@code ReentrantLock}.
     * This is equivalent to using {@code ReentrantLock(false)}.
     */
    public ReentrantLock() {
        sync = new NonfairSync();
    }

    /**
     * Creates an instance of {@code ReentrantLock} with the
     * given fairness policy.
     *
     * @param fair {@code true} if this lock should use a fair ordering policy
     */
    public ReentrantLock(boolean fair) {
        sync = fair ? new FairSync() : new NonfairSync();
    }

    /**
     * Acquires the lock.
     *
     * <p>Acquires the lock if it is not held by another thread and returns
     * immediately, setting the lock hold count to one.
     *
     * <p>If the current thread already holds the lock then the hold
     * count is incremented by one and the method returns immediately.
     *
     * <p>If the lock is held by another thread then the
     * current thread becomes disabled for thread scheduling
     * purposes and lies dormant until the lock has been acquired,
     * at which time the lock hold count is set to one.
     */
    public void lock() {
        sync.lock();
    }

    /**
     * Acquires the lock unless the current thread is
     * {@linkplain Thread#interrupt interrupted}.
     *
     * <p>Acquires the lock if it is not held by another thread and returns
     * immediately, setting the lock hold count to one.
     *
     * <p>If the current thread already holds this lock then the hold count
     * is incremented by one and the method returns immediately.
     *
     * <p>If the lock is held by another thread then the
     * current thread becomes disabled for thread scheduling
     * purposes and lies dormant until one of two things happens:
     *
     * <ul>
     *
     * <li>The lock is acquired by the current thread; or
     *
     * <li>Some other thread {@linkplain Thread#interrupt interrupts} the
     * current thread.
     *
     * </ul>
     *
     * <p>If the lock is acquired by the current thread then the lock hold
     * count is set to one.
     *
     * <p>If the current thread:
     *
     * <ul>
     *
     * <li>has its interrupted status set on entry to this method; or
     *
     * <li>is {@linkplain Thread#interrupt interrupted} while acquiring
     * the lock,
     *
     * </ul>
     *
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * <p>In this implementation, as this method is an explicit
     * interruption point, preference is given to responding to the
     * interrupt over normal or reentrant acquisition of the lock.
     *
     * @throws InterruptedException if the current thread is interrupted
     */
    public void lockInterruptibly() throws InterruptedException {
        sync.acquireInterruptibly(1);
    }

    /**
     * Acquires the lock only if it is not held by another thread at the time
     * of invocation.
     *
     * <p>Acquires the lock if it is not held by another thread and
     * returns immediately with the value {@code true}, setting the
     * lock hold count to one. Even when this lock has been set to use a
     * fair ordering policy, a call to {@code tryLock()} <em>will</em>
     * immediately acquire the lock if it is available, whether or not
     * other threads are currently waiting for the lock.
     * This &quot;barging&quot; behavior can be useful in certain
     * circumstances, even though it breaks fairness. If you want to honor
     * the fairness setting for this lock, then use
     * {@link #tryLock(long, TimeUnit) tryLock(0, TimeUnit.SECONDS) }
     * which is almost equivalent (it also detects interruption).
     *
     * <p>If the current thread already holds this lock then the hold
     * count is incremented by one and the method returns {@code true}.
     *
     * <p>If the lock is held by another thread then this method will return
     * immediately with the value {@code false}.
     *
     * @return {@code true} if the lock was free and was acquired by the
     *         current thread, or the lock was already held by the current
     *         thread; and {@code false} otherwise
     */
    public boolean tryLock() {
        return sync.nonfairTryAcquire(1);
    }

    /**
     * Acquires the lock if it is not held by another thread within the given
     * waiting time and the current thread has not been
     * {@linkplain Thread#interrupt interrupted}.
     *
     * <p>Acquires the lock if it is not held by another thread and returns
     * immediately with the value {@code true}, setting the lock hold count
     * to one. If this lock has been set to use a fair ordering policy then
     * an available lock <em>will not</em> be acquired if any other threads
     * are waiting for the lock. This is in contrast to the {@link #tryLock()}
     * method. If you want a timed {@code tryLock} that does permit barging on
     * a fair lock then combine the timed and un-timed forms together:
     *
     *  <pre> {@code
     * if (lock.tryLock() ||
     *     lock.tryLock(timeout, unit)) {
     *   ...
     * }}</pre>
     *
     * <p>If the current thread
     * already holds this lock then the hold count is incremented by one and
     * the method returns {@code true}.
     *
     * <p>If the lock is held by another thread then the
     * current thread becomes disabled for thread scheduling
     * purposes and lies dormant until one of three things happens:
     *
     * <ul>
     *
     * <li>The lock is acquired by the current thread; or
     *
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     *
     * <li>The specified waiting time elapses
     *
     * </ul>
     *
     * <p>If the lock is acquired then the value {@code true} is returned and
     * the lock hold count is set to one.
     *
     * <p>If the current thread:
     *
     * <ul>
     *
     * <li>has its interrupted status set on entry to this method; or
     *
     * <li>is {@linkplain Thread#interrupt interrupted} while
     * acquiring the lock,
     *
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * <p>If the specified waiting time elapses then the value {@code false}
     * is returned.  If the time is less than or equal to zero, the method
     * will not wait at all.
     *
     * <p>In this implementation, as this method is an explicit
     * interruption point, preference is given to responding to the
     * interrupt over normal or reentrant acquisition of the lock, and
     * over reporting the elapse of the waiting time.
     *
     * @param timeout the time to wait for the lock
     * @param unit the time unit of the timeout argument
     * @return {@code true} if the lock was free and was acquired by the
     *         current thread, or the lock was already held by the current
     *         thread; and {@code false} if the waiting time elapsed before
     *         the lock could be acquired
     * @throws InterruptedException if the current thread is interrupted
     * @throws NullPointerException if the time unit is null
     */
    public boolean tryLock(long timeout, TimeUnit unit)
            throws InterruptedException {
        return sync.tryAcquireNanos(1, unit.toNanos(timeout));
    }

    /**
     * Attempts to release this lock.
     *
     * <p>If the current thread is the holder of this lock then the hold
     * count is decremented.  If the hold count is now zero then the lock
     * is released.  If the current thread is not the holder of this
     * lock then {@link IllegalMonitorStateException} is thrown.
     *
     * @throws IllegalMonitorStateException if the current thread does not
     *         hold this lock
     */
    public void unlock() {
        sync.release(1);
    }

    /**
     * Returns a {@link Condition} instance for use with this
     * {@link Lock} instance.
     *
     * <p>The returned {@link Condition} instance supports the same
     * usages as do the {@link Object} monitor methods ({@link
     * Object#wait() wait}, {@link Object#notify notify}, and {@link
     * Object#notifyAll notifyAll}) when used with the built-in
     * monitor lock.
     *
     * <ul>
     *
     * <li>If this lock is not held when any of the {@link Condition}
     * {@linkplain Condition#await() waiting} or {@linkplain
     * Condition#signal signalling} methods are called, then an {@link
     * IllegalMonitorStateException} is thrown.
     *
     * <li>When the condition {@linkplain Condition#await() waiting}
     * methods are called the lock is released and, before they
     * return, the lock is reacquired and the lock hold count restored
     * to what it was when the method was called.
     *
     * <li>If a thread is {@linkplain Thread#interrupt interrupted}
     * while waiting then the wait will terminate, an {@link
     * InterruptedException} will be thrown, and the thread's
     * interrupted status will be cleared.
     *
     * <li> Waiting threads are signalled in FIFO order.
     *
     * <li>The ordering of lock reacquisition for threads returning
     * from waiting methods is the same as for threads initially
     * acquiring the lock, which is in the default case not specified,
     * but for <em>fair</em> locks favors those threads that have been
     * waiting the longest.
     *
     * </ul>
     *
     * @return the Condition object
     */
    public Condition newCondition() {
        return sync.newCondition();
    }

    /**
     * Queries the number of holds on this lock by the current thread.
     *
     * <p>A thread has a hold on a lock for each lock action that is not
     * matched by an unlock action.
     *
     * <p>The hold count information is typically only used for testing and
     * debugging purposes. For example, if a certain section of code should
     * not be entered with the lock already held then we can assert that
     * fact:
     *
     *  <pre> {@code
     * class X {
     *   ReentrantLock lock = new ReentrantLock();
     *   // ...
     *   public void m() {
     *     assert lock.getHoldCount() == 0;
     *     lock.lock();
     *     try {
     *       // ... method body
     *     } finally {
     *       lock.unlock();
     *     }
     *   }
     * }}</pre>
     *
     * @return the number of holds on this lock by the current thread,
     *         or zero if this lock is not held by the current thread
     */
    public int getHoldCount() {
        return sync.getHoldCount();
    }

    /**
     * Queries if this lock is held by the current thread.
     *
     * <p>Analogous to the {@link Thread#holdsLock(Object)} method for
     * built-in monitor locks, this method is typically used for
     * debugging and testing. For example, a method that should only be
     * called while a lock is held can assert that this is the case:
     *
     *  <pre> {@code
     * class X {
     *   ReentrantLock lock = new ReentrantLock();
     *   // ...
     *
     *   public void m() {
     *       assert lock.isHeldByCurrentThread();
     *       // ... method body
     *   }
     * }}</pre>
     *
     * <p>It can also be used to ensure that a reentrant lock is used
     * in a non-reentrant manner, for example:
     *
     *  <pre> {@code
     * class X {
     *   ReentrantLock lock = new ReentrantLock();
     *   // ...
     *
     *   public void m() {
     *       assert !lock.isHeldByCurrentThread();
     *       lock.lock();
     *       try {
     *           // ... method body
     *       } finally {
     *           lock.unlock();
     *       }
     *   }
     * }}</pre>
     *
     * @return {@code true} if current thread holds this lock and
     *         {@code false} otherwise
     */
    public boolean isHeldByCurrentThread() {
        return sync.isHeldExclusively();
    }

    /**
     * Queries if this lock is held by any thread. This method is
     * designed for use in monitoring of the system state,
     * not for synchronization control.
     *
     * @return {@code true} if any thread holds this lock and
     *         {@code false} otherwise
     */
    public boolean isLocked() {
        return sync.isLocked();
    }

    /**
     * Returns {@code true} if this lock has fairness set true.
     *
     * @return {@code true} if this lock has fairness set true
     */
    public final boolean isFair() {
        return sync instanceof FairSync;
    }

    /**
     * Returns the thread that currently owns this lock, or
     * {@code null} if not owned. When this method is called by a
     * thread that is not the owner, the return value reflects a
     * best-effort approximation of current lock status. For example,
     * the owner may be momentarily {@code null} even if there are
     * threads trying to acquire the lock but have not yet done so.
     * This method is designed to facilitate construction of
     * subclasses that provide more extensive lock monitoring
     * facilities.
     *
     * @return the owner, or {@code null} if not owned
     */
    protected Thread getOwner() {
        return sync.getOwner();
    }

    /**
     * Queries whether any threads are waiting to acquire this lock. Note that
     * because cancellations may occur at any time, a {@code true}
     * return does not guarantee that any other thread will ever
     * acquire this lock.  This method is designed primarily for use in
     * monitoring of the system state.
     *
     * @return {@code true} if there may be other threads waiting to
     *         acquire the lock
     */
    public final boolean hasQueuedThreads() {
        return sync.hasQueuedThreads();
    }

    /**
     * Queries whether the given thread is waiting to acquire this
     * lock. Note that because cancellations may occur at any time, a
     * {@code true} return does not guarantee that this thread
     * will ever acquire this lock.  This method is designed primarily for use
     * in monitoring of the system state.
     *
     * @param thread the thread
     * @return {@code true} if the given thread is queued waiting for this lock
     * @throws NullPointerException if the thread is null
     */
    public final boolean hasQueuedThread(Thread thread) {
        return sync.isQueued(thread);
    }

    /**
     * Returns an estimate of the number of threads waiting to
     * acquire this lock.  The value is only an estimate because the number of
     * threads may change dynamically while this method traverses
     * internal data structures.  This method is designed for use in
     * monitoring of the system state, not for synchronization
     * control.
     *
     * @return the estimated number of threads waiting for this lock
     */
    public final int getQueueLength() {
        return sync.getQueueLength();
    }

    /**
     * Returns a collection containing threads that may be waiting to
     * acquire this lock.  Because the actual set of threads may change
     * dynamically while constructing this result, the returned
     * collection is only a best-effort estimate.  The elements of the
     * returned collection are in no particular order.  This method is
     * designed to facilitate construction of subclasses that provide
     * more extensive monitoring facilities.
     *
     * @return the collection of threads
     */
    protected Collection<Thread> getQueuedThreads() {
        return sync.getQueuedThreads();
    }

    /**
     * Queries whether any threads are waiting on the given condition
     * associated with this lock. Note that because timeouts and
     * interrupts may occur at any time, a {@code true} return does
     * not guarantee that a future {@code signal} will awaken any
     * threads.  This method is designed primarily for use in
     * monitoring of the system state.
     *
     * @param condition the condition
     * @return {@code true} if there are any waiting threads
     * @throws IllegalMonitorStateException if this lock is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this lock
     * @throws NullPointerException if the condition is null
     */
    public boolean hasWaiters(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.hasWaiters((AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    /**
     * Returns an estimate of the number of threads waiting on the
     * given condition associated with this lock. Note that because
     * timeouts and interrupts may occur at any time, the estimate
     * serves only as an upper bound on the actual number of waiters.
     * This method is designed for use in monitoring of the system
     * state, not for synchronization control.
     *
     * @param condition the condition
     * @return the estimated number of waiting threads
     * @throws IllegalMonitorStateException if this lock is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this lock
     * @throws NullPointerException if the condition is null
     */
    public int getWaitQueueLength(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.getWaitQueueLength((AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    /**
     * Returns a collection containing those threads that may be
     * waiting on the given condition associated with this lock.
     * Because the actual set of threads may change dynamically while
     * constructing this result, the returned collection is only a
     * best-effort estimate. The elements of the returned collection
     * are in no particular order.  This method is designed to
     * facilitate construction of subclasses that provide more
     * extensive condition monitoring facilities.
     *
     * @param condition the condition
     * @return the collection of threads
     * @throws IllegalMonitorStateException if this lock is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this lock
     * @throws NullPointerException if the condition is null
     */
    protected Collection<Thread> getWaitingThreads(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.getWaitingThreads((AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    /**
     * Returns a string identifying this lock, as well as its lock state.
     * The state, in brackets, includes either the String {@code "Unlocked"}
     * or the String {@code "Locked by"} followed by the
     * {@linkplain Thread#getName name} of the owning thread.
     *
     * @return a string identifying this lock, as well as its lock state
     */
    public String toString() {
        Thread o = sync.getOwner();
        return super.toString() + ((o == null) ?
                                   "[Unlocked]" :
                                   "[Locked by thread " + o.getName() + "]");
    }
}
