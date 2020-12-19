package JDK.juc.AQS.Semaphore;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * ClassName: SemaphoreTest02
 * Description:
 * date: 2020/4/16 15:57
 *
 * @author 小刘讲师，微信：vv517956494
 * 本课程属于 小刘讲师 VIP 源码特训班课程
 * 严禁非法盗用（如有发现非法盗取行为，必将追究法律责任）
 * <p>
 * 如有同学发现非 小刘讲源码 官方号传播本视频资源，请联系我！
 * @since 1.0.0
 */
public class SemaphoreTest02 {
    public static void main(String[] args) throws InterruptedException {
        final Semaphore semaphore = new Semaphore(2, true);

        Thread tA = new Thread(() ->{
            try {
                semaphore.acquire();
                System.out.println("线程A获取通行证成功");
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException e) {
            }finally {
                semaphore.release();
            }
        });
        tA.start();
        //确保线程A已经执行
        TimeUnit.MILLISECONDS.sleep(200);

        Thread tB = new Thread(() ->{
            try {
                semaphore.acquire(2);
                System.out.println("线程B获取通行证成功");
            } catch (InterruptedException e) {
            }finally {
                semaphore.release(2);
            }
        });
        tB.start();
        //确保线程B已经执行
        TimeUnit.MILLISECONDS.sleep(200);

        Thread tC = new Thread(() ->{
            try {
                semaphore.acquire();
                System.out.println("线程C获取通行证成功");
            } catch (InterruptedException e) {
            }finally {
                semaphore.release();
            }
        });
        tC.start();
    }

}
