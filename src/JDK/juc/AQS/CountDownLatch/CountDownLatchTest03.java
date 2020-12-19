package JDK.juc.AQS.CountDownLatch;

import java.util.concurrent.CountDownLatch;

/**
 * ClassName: CountDownLatchTest03
 * Description:
 * date: 2020/4/13 10:47
 *
 * @author 小刘讲师，微信：vv517956494
 * 本课程属于 小刘讲师 VIP 源码特训班课程
 * 严禁非法盗用（如有发现非法盗取行为，必将追究法律责任）
 * <p>
 * 如有同学发现非 小刘讲源码 官方号传播本视频资源，请联系我！
 * @since 1.0.0
 */
public class CountDownLatchTest03 {
    public static void main(String[] args) {
        CountDownLatch latch = new CountDownLatch(2);

        Thread t1 = new Thread(()-> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignore) {
            }
            // 休息 5 秒后(模拟线程工作了 5 秒)，调用 countDown()
            latch.countDown();
        }, "t1");

        Thread t2 = new Thread(()-> {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException ignore) {
            }
            // 休息 10 秒后(模拟线程工作了 10 秒)，调用 countDown()
            latch.countDown();
        }, "t2");

        t1.start();
        t2.start();

        Thread t3 = new Thread(()-> {
            try {
                // 阻塞，等待 state 减为 0
                latch.await();
                System.out.println("线程 t3 从 await 中返回了");
            } catch (InterruptedException e) {
                System.out.println("线程 t3 await 被中断");
                Thread.currentThread().interrupt();
            }
        }, "t3");

        Thread t4 = new Thread(()-> {
            try {
                // 阻塞，等待 state 减为 0
                latch.await();
                System.out.println("线程 t4 从 await 中返回了");
            } catch (InterruptedException e) {
                System.out.println("线程 t4 await 被中断");
                Thread.currentThread().interrupt();
            }
        }, "t4");

        t3.start();
        t4.start();
    }
}
