package JDK.juc.SynchronousQueue;

import java.util.concurrent.SynchronousQueue;

/**
 * ClassName: TransferQueueDemo03
 * Description:
 * date: 2020/5/16 15:35
 *
 * @author 小刘讲师，微信：vv517956494
 * 本课程属于 小刘讲师 VIP 源码特训班课程
 * 严禁非法盗用（如有发现非法盗取行为，必将追究法律责任）
 * <p>
 * 如有同学发现非 小刘讲源码 官方号传播本视频资源，请联系我！
 * @since 1.0.0
 */
public class TransferQueueCancelDemo02 {
    public static void main(String[] args) throws InterruptedException {
        SynchronousQueue<Integer> queue = new SynchronousQueue<>(true);

        Thread thread1 = new Thread(() -> {
            try {
                queue.put(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        thread1.start();

        Thread.sleep(200);
        Thread thread2 = new Thread(() -> {
            try {
                queue.put(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        thread2.start();

        Thread.sleep(200);
        Thread thread3 = new Thread(() -> {
            try {
                queue.put(3);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        thread3.start();

        Thread.sleep(200);
        thread2.interrupt();
    }
}
