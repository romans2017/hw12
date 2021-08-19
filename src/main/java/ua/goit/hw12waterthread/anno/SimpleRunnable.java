package ua.goit.hw12waterthread.anno;

@Repeat(10)
public class SimpleRunnable implements Runnable{

    @Override
    public void run() {
        System.out.println("test");
    }
}
