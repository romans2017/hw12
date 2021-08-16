import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.stream.Stream;

public class ElementaryInputThread {
    public static int DEFAULT_NUMBER_THREADS = 100;

    private int numberThreads;

    private static class Molecule {
        public static final String OXYGEN = "O";
        public static final String HYDROGEN = "H";

        public static final int ATOMS_OXYGEN = 1;
        public static final int ATOMS_HYDROGEN = 2;

        private int hydrogen;
        private int oxygen;
        private List<Task> taskList = new ArrayList<>();

        public int getAtom(String atom) {
            return atom.equals(HYDROGEN) ? getHydrogen() : getOxygen();
        }

        private int getHydrogen() {
            return hydrogen;
        }

        private int getOxygen() {
            return oxygen;
        }

        public void setAtom(String atom, int numberAtoms) {
            if (atom.equals(HYDROGEN)) {
                setHydrogen(numberAtoms);
            } else {
                setOxygen(numberAtoms);
            }
        }

        private void setHydrogen(int hydrogen) {
            this.hydrogen = hydrogen;
        }

        private void setOxygen(int oxygen) {
            this.oxygen = oxygen;
        }

        public boolean isCreated() {
            return hydrogen == 2 && oxygen == 1;
        }

        public void addTask(Task task) {
            taskList.add(task);
        }

        public void removeTask(Task task) {
            taskList.remove(task);
        }

        public boolean hasTask(Task task) {
            return taskList.contains(task);
        }

        public List<Task> getTaskList() {
            return taskList;
        }

        public void reset() {
            hydrogen = 0;
            oxygen = 0;
            taskList.clear();
        }
    }

    private class Task implements Callable {

        //private Phaser barrier;
        private CyclicBarrier barrier;
        private Lock locker;
        private Molecule molecule;

        //public Task(Phaser barrier, Lock locker, Molecule molecule) {
        public Task(CyclicBarrier barrier, Lock locker, Molecule molecule) {
            this.barrier = barrier;
            this.locker = locker;
            this.molecule = molecule;
        }

        @Override
        public String call() {
            try {
                Thread.sleep((long) Math.random() * 10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //get random atom
            String atom = Math.random() <= 0.6667 ? Molecule.HYDROGEN : Molecule.OXYGEN;
            System.out.println(atom);
            try {
                Thread.sleep(numberThreads*2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                //for (int i = 0; i < numberThreads; i++) {
                while(true) {
                    //Thread.sleep(10);
                    if (Thread.currentThread().isInterrupted()) {
                        //barrier.arriveAndDeregister();
                        barrier.reset();
                        return atom;
                    }
                    if (locker.tryLock(100, TimeUnit.MILLISECONDS)) {
                        if (molecule.isCreated() && molecule.hasTask(this)) {
                            //molecule was created with this atom, print atom,
                            //break loop, molecule is reset
                            System.out.print(atom);
                            molecule.removeTask(this);
                            if (molecule.getTaskList().size() == 0) {
                                System.out.println("");
                                molecule.reset();
                            }
                            locker.unlock();
                            break;
                        } else if (!molecule.isCreated() && !molecule.hasTask(this)) {
                            //molecule wasn't created yet; try to complete molecule with this atom;
                            //wait at the barrier
                            int atomsIn = atom.equals(Molecule.HYDROGEN) ? Molecule.ATOMS_HYDROGEN : Molecule.ATOMS_OXYGEN;
                            if (molecule.getAtom(atom) < atomsIn) {
                                molecule.setAtom(atom, molecule.getAtom(atom) + 1);
                                molecule.addTask(this);
                                locker.unlock();
                                barrier.await(5L, TimeUnit.SECONDS);
                                //System.out.print(atom);
                                //break;
                            } else {
                               locker.unlock();
                               //barrier.arriveAndAwaitAdvance();
                            }
                        } else {
                            /*molecule wasn't created yet
                            can't complete molecule with this atom
                            wait at the barrier*/
                            locker.unlock();
                            //barrier.arriveAndAwaitAdvance();
                        }
                    } else {
                        //barrier.arriveAndAwaitAdvance();
                    }
                    //locker.unlock();
                }
                //barrier.arriveAndDeregister();
            } catch (InterruptedException | BrokenBarrierException | TimeoutException e) {
                locker.unlock();
                e.printStackTrace();
            }
            return atom;
        }
    }

    public ElementaryInputThread(int numberThreads) {

        this.numberThreads = numberThreads;

        Lock locker = new ReentrantLock();
        Molecule molecule = new Molecule();
        ExecutorService executorService = Executors.newCachedThreadPool();
        //Phaser barrier = new Phaser(numberThreads);
        CyclicBarrier barrier = new CyclicBarrier(3);

        List<Future<String>> results = new ArrayList<>();

        for (int i = 0; i < numberThreads; i++) {
            results.add(executorService.submit(new Task(barrier, locker, molecule)));
        }

        executorService.shutdown();
        /*long timeout = (long) numberThreads * 30;
        try {
            if (!executorService.awaitTermination(timeout, TimeUnit.MILLISECONDS)) {
                barrier.reset();
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
    }

    public ElementaryInputThread() {
        this(DEFAULT_NUMBER_THREADS);
    }
}
