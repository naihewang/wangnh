package source_code.juc.AQS.CyclicBarrier;

import java.util.Random;
import java.util.concurrent.*;

/**
 * ClassName: CyclicBarrierTest01
 * Description:
 * date: 2020/4/15 14:35
 *
 * @author 小刘讲师，微信：vv517956494
 * 本课程属于 小刘讲师 VIP 源码特训班课程
 * 严禁非法盗用（如有发现非法盗取行为，必将追究法律责任）
 * <p>
 * 如有同学发现非 小刘讲源码 官方号传播本视频资源，请联系我！
 * @since 1.0.0
 */
public class CyclicBarrierTest01 {
    /**
     * 案例：
     * 模拟过气游戏 “王者荣耀” 游戏开始逻辑
     */
    public static void main(String[] args) {
        //第一步：定义玩家，定义5个
        String[] heros = {"安琪拉","亚瑟","马超","张飞", "刘备"};

        //第二步：创建固定线程数量的线程池，线程数量为5
        ExecutorService service = Executors.newFixedThreadPool(5);

        //第三步：创建barrier，parties 设置为5
        CyclicBarrier barrier = new CyclicBarrier(5);

        //第四步：通过for循环开启5任务，模拟开始游戏，传递给每个任务 英雄名称和barrier
        for(int i = 0; i < 5; i++) {
            service.execute(new Player(heros[i], barrier));
        }

        service.shutdown();
    }


    static class Player implements Runnable {
        private String hero;
        private CyclicBarrier barrier;

        public Player(String hero, CyclicBarrier barrier) {
            this.hero = hero;
            this.barrier = barrier;
        }

        @Override
        public void run() {
            try {
                //每个玩家加载进度不一样，这里使用随机数来模拟！
                TimeUnit.SECONDS.sleep(new Random().nextInt(10));
                System.out.println(hero + "：加载进度100%，等待其他玩家加载完成中...");
                barrier.await();
                System.out.println(hero + "：发现所有英雄加载完成，开始战斗吧！");
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (BrokenBarrierException e) {
                e.printStackTrace();
            }
        }
    }
}
