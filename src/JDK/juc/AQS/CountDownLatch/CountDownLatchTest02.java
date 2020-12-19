package JDK.juc.AQS.CountDownLatch;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * ClassName: CountDownLatchTest02
 * Description:
 * date: 2020/4/13 10:21
 *
 * @author 小刘讲师，微信：vv517956494
 * 本课程属于 小刘讲师 VIP 源码特训班课程
 * 严禁非法盗用（如有发现非法盗取行为，必将追究法律责任）
 * <p>
 * 如有同学发现非 小刘讲源码 官方号传播本视频资源，请联系我！
 * @since 1.0.0
 */
public class CountDownLatchTest02 {

    public static void main(String[] args) throws InterruptedException {
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(10);

        for(int i = 0; i < 10; i++) {
            new Thread(new Worker(i, startSignal, doneSignal)).start();
        }

        //这里让主线程休眠500毫秒，确保所有子线程已经启动，并且阻塞在startSignal栅栏处
        TimeUnit.MILLISECONDS.sleep(500);


        //因为startSignal 栅栏值为1，所以主线程只要调用一次，
        //那么所有调用startSignal.await()阻塞的子线程，就都可以通过栅栏了
        System.out.println("子任务栅栏已开启...");
        startSignal.countDown();


        System.out.println("等待子任务结束...");
        long startTime = System.currentTimeMillis();
        //等待所有子任务结束..
        doneSignal.await();
        long endTime = System.currentTimeMillis();
        System.out.println("所有子任务已经运行结束，耗时：" + (endTime - startTime));
    }


    static class Worker implements Runnable {
        private final CountDownLatch startSignal;
        private final CountDownLatch doneSignal;
        private int id;

        @Override
        public void run() {
            try {
                // 为了让所有线程同时开始任务，我们让所有线程先阻塞在这里
                // 等大家都准备好了，再打开这个门栓
                startSignal.await();
                System.out.println("子任务-" + id + "，开启时间：" + System.currentTimeMillis());
                doWork();
            } catch (InterruptedException e) {
            }finally {
                doneSignal.countDown();
            }
        }


        private void doWork() throws InterruptedException {
            TimeUnit.SECONDS.sleep(5);
        }

        public Worker(int id, CountDownLatch startSignal, CountDownLatch doneSignal) {
            this.id = id;
            this.startSignal = startSignal;
            this.doneSignal = doneSignal;
        }
    }
}
