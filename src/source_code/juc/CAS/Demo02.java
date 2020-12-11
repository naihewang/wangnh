package source_code.juc.CAS;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * ClassName: Demo
 * Description:
 * date: 2020/2/22 20:24
 *
 * @author 暴躁小刘讲师，微信：vv517956494
 * 想要购买本套JDK 1.8 ConcurrentHashMap 源码讲解课程的同学，可以加我微信！
 * 我摊牌了，我就是来卖课的...（家里的孩子还等着我赚钱买奶粉呢...）
 * <p>
 * 小刘讲师决定站着把钱挣了，如果购买后感觉课程不硬核并且指出问题所在，
 * 小刘讲师立刻返还所有课程费用，一分钱不收！
 * @since 1.0.0
 */
public class Demo02 {
    //总访问量
    static int count = 0;


    /**
     * Q：耗时太长的原因是什么呢？
     * A：程序中的request方法使用synchronized关键字修饰，保证了并发情况下，request方法同一时刻
     * 只允许一个线程进入，request加锁相当于串行执行了，count的结果和我们预期的一致，知识耗时太长了..
     *
     * Q：如何解决耗时长的问题？
     * A：count ++ 操作实际上是由3步来完成！（jvm执行引擎）
     *    1.获取count的值，记做A : A=count
     *    2.将A值+1，得到B ：B=A+1
     *    3.将B值赋值给count
     *    升级第3步的实现：
     *       1.获取锁
     *       2.获取以下count最新的值，记做LV
     *       3.判断LV是否等于A，如果相等，则将B的值赋值给count，并返回true，否则返回false
     *       4.释放锁
     *
     */
    //模拟访问的方法
    public synchronized static void request() throws InterruptedException {
        //模拟耗时5毫秒
        TimeUnit.MILLISECONDS.sleep(5);
        /**
         * Q：分析一下问题出在哪呢？
         * A：count ++ 操作实际上是由3步来完成！（jvm执行引擎）
         *    1.获取count的值，记做A : A=count
         *    2.将A值+1，得到B ：B=A+1
         *    3.将B值赋值给count
         *
         *    如果有A.B两个线程同时执行count++,他们通知执行到上面步骤的第一步，得到的
         *    count是一样的，3步操作结束后，count只加1，导致count结果不正确！
         * Q：怎么解决结果不正确问题？
         * A：对count++操作的时候，我们让多个线程排队处理，多个线程同时到达request()方法的时候，
         * 只能允许一个线程可以进去操作，其它的线程在外面等着，等里面的处理完毕出来之后，外面等着的
         * 再进去一个，这样操作的count++就是排队进行的，结果一定是正确的。
         *
         * Q:怎么实现排队效果？？
         * A：java中synchronized关键字和ReentrantLock都可以实现对资源枷锁，保证并发正确性，
         * 多线程的情况下可以保证被锁住的资源被“串行”访问。
         */
        count ++;
    }

    public static void main(String[] args) throws InterruptedException {
        //开始时间
        long startTime = System.currentTimeMillis();
        int threadSize = 100;
        CountDownLatch countDownLatch = new CountDownLatch(threadSize);

        for(int i = 0; i < threadSize; i++) {

            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    //模拟用户行为，每个用户访问10次网站
                    try {
                        for(int j = 0; j < 10; j++) {
                            request();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        countDownLatch.countDown();
                    }
                }
            });

            thread.start();
        }
        //怎么保证100个线程 结束之后，再执行后面代码？
        countDownLatch.await();
        long endTime = System.currentTimeMillis();

        System.out.println(Thread.currentThread().getName() + ",耗时：" + (endTime - startTime) + ", count = " + count);
    }
}
