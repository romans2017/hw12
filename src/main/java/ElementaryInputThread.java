import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

        public boolean hasTask(Task task) {
            return taskList.contains(task);
        }

        public String getComposition() {
            StringBuilder stringBuilder = new StringBuilder();
            for (Task task : taskList) {
                stringBuilder.append(task.getAtom());
            }
            return stringBuilder.toString();
        }

        public void reset() {
            hydrogen = 0;
            oxygen = 0;
            taskList.clear();
        }
    }

    private class Task implements Callable {

        private CyclicBarrier barrier;
        private Lock locker;
        private Molecule molecule;
        private String atom;

        public Task(CyclicBarrier barrier, Lock locker, Molecule molecule) {
            this.barrier = barrier;
            this.locker = locker;
            this.molecule = molecule;
        }

        public String getAtom() {
            return atom;
        }

        @Override
        public String call() {
            //get random atom
            atom = Math.random() <= 0.6667 ? Molecule.HYDROGEN : Molecule.OXYGEN;
            System.out.println(atom);

            try {
                Thread.sleep((long) (getCoefficientTimeout() * numberThreads / 3));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            try {
                while (true) {
                    Thread.sleep((long) (Math.random() * 10));
                    if (Thread.currentThread().isInterrupted()) {
                        barrier.reset();
                        return atom;
                    }
                    if (locker.tryLock(100, TimeUnit.MILLISECONDS)) {
                        if (!molecule.isCreated() && !molecule.hasTask(this)) {
                            //molecule wasn't created yet; try to complete molecule with this atom;
                            //wait at the barrier
                            int atomsIn = atom.equals(Molecule.HYDROGEN) ? Molecule.ATOMS_HYDROGEN : Molecule.ATOMS_OXYGEN;
                            if (molecule.getAtom(atom) < atomsIn) {
                                molecule.setAtom(atom, molecule.getAtom(atom) + 1);
                                molecule.addTask(this);
                                locker.unlock();
                                barrier.await(10L, TimeUnit.SECONDS);
                                break;
                            } else {
                                locker.unlock();
                            }
                        } else {
                            locker.unlock();
                        }
                    }
                }
            } catch (InterruptedException e) {
                locker.unlock();
                e.printStackTrace();
            } catch (BrokenBarrierException | TimeoutException e) {
                locker.unlock();
            }
            return atom;
        }
    }

    private int getCoefficientTimeout() {
        if (numberThreads <= 50) {
            return 300;
        } else if (numberThreads <= 500) {
            return 100;
        } else if (numberThreads <= 1000) {
            return 30;
        } else if (numberThreads <= 10000) {
            return 6;
        } else if (numberThreads <= 100000) {
            return 3;
        } else {
            return 2;
        }
    }

    public ElementaryInputThread(int numberThreads) {

        this.numberThreads = numberThreads;

        Lock locker = new ReentrantLock();
        Molecule molecule = new Molecule();
        ExecutorService executorService = Executors.newCachedThreadPool();

        Runnable barrierAction = () -> {
            while (true) {
                try {
                    if (locker.tryLock(100, TimeUnit.MILLISECONDS)) {
                        //molecule was created, print atoms,
                        //reset molecule
                        System.out.println(molecule.getComposition());
                        molecule.reset();
                        locker.unlock();
                        break;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        };
        CyclicBarrier barrier = new CyclicBarrier(3, barrierAction);

        List<Future<String>> results = new ArrayList<>();

        for (int i = 0; i < numberThreads; i++) {
            results.add(executorService.submit(new Task(barrier, locker, molecule)));
        }

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(getCoefficientTimeout() * numberThreads, TimeUnit.MILLISECONDS)) {
                barrier.reset();
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for (Future<String> future : results) {
            try {
                System.out.println(future.get());
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    public ElementaryInputThread() {
        this(DEFAULT_NUMBER_THREADS);
    }
}
