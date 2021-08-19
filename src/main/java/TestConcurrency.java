import anno.SimpleRunnable;
import anno.ThreadAnnotation;
import ua.goit.waterthread.ElementaryInputThread;

public class TestConcurrency {

    private static void testElementaryInputThread() {
        ElementaryInputThread elementaryInputThread = new ElementaryInputThread();
        elementaryInputThread.start();
    }

    private static void testAnno() {
        ThreadAnnotation threadAnnotation = new ThreadAnnotation(10);
        threadAnnotation.execute(new SimpleRunnable());
        threadAnnotation.shutdown();
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("Test water thread");
        testElementaryInputThread();
        Thread.sleep(1000);
        System.out.println();
        System.out.println("Test annotations");
        testAnno();
    }
}
