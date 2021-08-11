import java.util.Collections;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ElementaryInputThread {
    public static int DEFAULT_NUMBER_THREADS = 100;
    private int numberThreads;
    private ExecutorService executorService;

    public ElementaryInputThread(int numberThreads) {
        this.numberThreads = numberThreads;
        executorService = Executors.newCachedThreadPool();
    }

    public ElementaryInputThread() {
        this(DEFAULT_NUMBER_THREADS);
    }

    private static String createInputThread() {
        double flag = Math.random();
        return flag <= 0.5 ? "H" : "O";
    }

    private void

    private void startThreads() {
        Collections.synchronizedList()
        CyclicBarrier cyclicBarrier = new CyclicBarrier(3, );
        for (int i = 0; i < numberThreads; i++) {
            executorService.e(ElementaryInputThread::createInputThread);
            future.
        }
    }
}
