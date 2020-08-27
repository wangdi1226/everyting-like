import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

public class WaitTest {

    /**
     * 等待所有线程执行完毕：
     * 1.CountDownLatch: 初始化一个数值，可以countDown()对数值进行i--操作，也可以await()阻塞等待直到i==0
     * 2.Semaphore: release()进行一定数量许可的颁发。acquire()阻塞并等待一定数量的许可。
     * 相对来说，semaphore功能更强大，也更灵活一点
     * @param args
     */
    private static int COUNT = 5;
    private static CountDownLatch LATCH = new CountDownLatch(COUNT);
    private static Semaphore SEMAPHORE = new Semaphore(0);

    public static void main(String[] args) throws InterruptedException {
        for(int i=0; i<COUNT; i++){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    System.out.println(Thread.currentThread().getName());
//                    LATCH.countDown();//i--
                    SEMAPHORE.release();//颁发一定数量许可证，无参就是颁发一个数量
                }
            }).start();
        }

        //main在所有子线程执行完毕之后，再运行以下代码
//        LATCH.await();// await()会阻塞并一直等待，直到LATCH的值==0
        SEMAPHORE.acquire(5);// 无参代表请求资源数量为1，也可以请求指定数量的资源
        System.out.println(Thread.currentThread().getName());
    }
}
