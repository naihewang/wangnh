package source_code.juc.cas;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * ClassName: CasABADemo
 * Description:
 * date: 2020/2/23 14:31
 *
 * @author 暴躁小刘讲师，微信：vv517956494
 * 想要购买本套JDK 1.8 ConcurrentHashMap 源码讲解课程的同学，可以加我微信！
 * 我摊牌了，我就是来卖课的...（家里的孩子还等着我赚钱买奶粉呢...）
 * <p>
 * 小刘讲师决定站着把钱挣了，如果购买后感觉课程不硬核并且指出问题所在，
 * 小刘讲师立刻返还所有课程费用，一分钱不收！
 * @since 1.0.0
 */
public class CasABADemo {
    public static AtomicInteger a = new AtomicInteger(1);

    public static void main(String[] args) {
        Thread main = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("操作线程" + Thread.currentThread().getName() + ", 初始值：" + a.get());
                try {

                    int expectNum = a.get();
                    int newNum = expectNum + 1;
                    Thread.sleep(1000);//主线程休眠一秒钟，让出cpu

                    boolean isCASSccuess = a.compareAndSet(expectNum, newNum);
                    System.out.println("操作线程" + Thread.currentThread().getName() + "，CAS操作：" + isCASSccuess);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "主线程");

        Thread other = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(20);//确保Thread-main线程优先执行

                    a.incrementAndGet();//a + 1,a=2
                    System.out.println("操作线程" + Thread.currentThread().getName() + "，【increment】,值=" +a.get());
                    a.decrementAndGet();//a - 1,a=1
                    System.out.println("操作线程" + Thread.currentThread().getName() + "，【decrement】,值=" +a.get());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "干扰线程");

        main.start();
        other.start();
    }
}
