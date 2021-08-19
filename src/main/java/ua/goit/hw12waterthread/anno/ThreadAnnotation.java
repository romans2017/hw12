package ua.goit.hw12waterthread.anno;

import java.util.concurrent.*;

public class ThreadAnnotation extends ThreadPoolExecutor {

    private final static int DEFAULT_MAXIMUM_POOL_SIZE = 100;
    private final static int KEEP_ALIVE_TIME_IN_MS = 1000;
    private final static BlockingQueue<Runnable> WORK_QUEUE = new ArrayBlockingQueue<>(1);

    public ThreadAnnotation(int corePoolSize) {
        super(corePoolSize, DEFAULT_MAXIMUM_POOL_SIZE, KEEP_ALIVE_TIME_IN_MS, TimeUnit.MILLISECONDS, WORK_QUEUE);
    }

    @Override
    public void execute(Runnable command) {
        Repeat annoClass = command.getClass().getAnnotation(Repeat.class);
        for (int i = 0; i < annoClass.value(); i++) {
            super.execute(command);
        }
    }

}
