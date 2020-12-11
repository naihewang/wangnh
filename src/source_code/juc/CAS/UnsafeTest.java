package source_code.juc.CAS;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * ClassName: UnsafeTest
 * Description:
 * date: 2020/2/23 21:35
 *
 * @author 暴躁小刘讲师，微信：vv517956494
 * 想要购买本套JDK 1.8 ConcurrentHashMap 源码讲解课程的同学，可以加我微信！
 * 我摊牌了，我就是来卖课的...（家里的孩子还等着我赚钱买奶粉呢...）
 * <p>
 * 小刘讲师决定站着把钱挣了，如果购买后感觉课程不硬核并且指出问题所在，
 * 小刘讲师立刻返还所有课程费用，一分钱不收！
 * @since 1.0.0
 */
public class UnsafeTest {
    public static void main(String[] args) throws NoSuchFieldException, IllegalAccessException {
        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        Unsafe unsafe = (Unsafe) f.get(null);
        System.out.println(unsafe);
    }
}
