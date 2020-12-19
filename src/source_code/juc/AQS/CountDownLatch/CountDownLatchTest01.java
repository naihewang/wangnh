package source_code.juc.AQS.CountDownLatch;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * ClassName: CountDownLatchTest01
 * Description:
 * date: 2020/4/13 10:01
 *
 * @author 小刘讲师，微信：vv517956494
 * 本课程属于 小刘讲师 VIP 源码特训班课程
 * 严禁非法盗用（如有发现非法盗取行为，必将追究法律责任）
 * <p>
 * 如有同学发现非 小刘讲源码 官方号传播本视频资源，请联系我！
 * @since 1.0.0
 */
public class CountDownLatchTest01 {
    private static final int TASK_COUNT = 8;
    private static final int THREAD_CORE_SIZE = 10;

    public static void main(String[] args) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(TASK_COUNT);

        Executor executor = Executors.newFixedThreadPool(THREAD_CORE_SIZE);
        for(int i = 0; i < 8; i++) {
            executor.execute(new WorkerRunnable(i, latch));
        }

        System.out.println("主线程等待所有子任务完成....");
        long mainWaitStartTimeMillis = System.currentTimeMillis();
        latch.await();
        long mainWaitEndTimeMillis = System.currentTimeMillis();
        System.out.println("主线程等待时长：" + (mainWaitEndTimeMillis - mainWaitStartTimeMillis));
    }


    static class WorkerRunnable implements Runnable {
        private int taskId;
        private CountDownLatch latch;

        @Override
        public void run() {
            doWorker();
        }

        public void doWorker() {
            System.out.println("任务ID：" + taskId + "，正在执行任务中....");
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
            } finally {
                latch.countDown();
            }
            System.out.println("任务ID：" + taskId + "，任务执行结束！");
        }

        public WorkerRunnable(int taskId, CountDownLatch latch) {
            this.taskId = taskId;
            this.latch = latch;
        }
    }
}
