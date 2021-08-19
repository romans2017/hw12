import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

public class ElementaryInputThread {
    public static final int DEFAULT_NUMBER_THREADS = 100;

    private final int numberThreads;

    private static class Molecule {

        public static final String OXYGEN = "O";
        public static final String HYDROGEN = "H";

        public static final int ATOMS_OXYGEN = 1;
        public static final int ATOMS_HYDROGEN = 2;

        private int hydrogen;
        private int oxygen;
        private final List<Task> taskList = new ArrayList<>();
        private final List<String> unused = new ArrayList<>();
        private int numberOf;

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
            numberOf++;
        }

        public void addUnused(String atom) {
            unused.add(atom);
        }

        public List<String> getUnused() {
            return unused;
        }

        public int getNumberOf() {
            return numberOf;
        }
    }

    private class Task implements Callable<String> {

        private final CyclicBarrier barrier;
        private final Lock locker;
        private final Molecule molecule;
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
            int atomsIn = atom.equals(Molecule.HYDROGEN) ? Molecule.ATOMS_HYDROGEN : Molecule.ATOMS_OXYGEN;

            try {
                Thread.sleep(numberThreads < 100 ? (long) numberThreads * 50 : numberThreads);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            try {
                int counter = numberThreads;
                int numberLastMolecule = 0;
                while (counter > 0) {
                    Thread.sleep((long) (Math.random() * 10));
                    locker.lockInterruptibly();
                    if (!molecule.isCreated() && !molecule.hasTask(this) && molecule.getAtom(atom) < atomsIn) {
                        //molecule wasn't created yet; try to complete molecule with this atom;
                        //wait at the barrier
                        molecule.setAtom(atom, molecule.getAtom(atom) + 1);
                        molecule.addTask(this);
                        locker.unlock();
                        barrier.await(2L, TimeUnit.SECONDS);
                        break;
                    } else if (molecule.getNumberOf() == numberLastMolecule) {
                        //number of last molecule equals to saved number - decrement counter
                        counter--;
                    } else if (molecule.getNumberOf() != numberLastMolecule) {
                        //number of last molecule not equals to saved number - reset counter
                        counter = numberThreads - numberLastMolecule * (Molecule.ATOMS_HYDROGEN + Molecule.ATOMS_OXYGEN);
                        numberLastMolecule = molecule.getNumberOf();
                    }
                    locker.unlock();
                }
                if (counter == 0) {
                    locker.lock();
                    molecule.addUnused(atom);
                    locker.unlock();
                }
            } catch (BrokenBarrierException | TimeoutException | InterruptedException e) {
                if (Thread.currentThread().isInterrupted()) {
                    Thread.interrupted();
                }
                locker.lock();
                molecule.addUnused(atom);
                locker.unlock();
            }
            return atom;
        }
    }

    public ElementaryInputThread(int numberThreads) {
        this.numberThreads = numberThreads;
    }

    public ElementaryInputThread() {
        this(DEFAULT_NUMBER_THREADS);
    }

    @SuppressWarnings("unchecked")
    public void start() {

        Lock locker = new ReentrantLock();
        Molecule molecule = new Molecule();
        ExecutorService executorService = Executors.newCachedThreadPool();

        Runnable barrierAction = () -> {
            while (true) {
                try {
                    if (locker.tryLock(100, TimeUnit.MILLISECONDS)) {
                        //molecule was created, print atoms, reset molecule
                        String composition = molecule.getComposition();
                        molecule.reset();
                        System.out.println("Molecule " + molecule.getNumberOf() + ":" + composition);
                        locker.unlock();
                        break;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        };
        CyclicBarrier barrier = new CyclicBarrier(3, barrierAction);

        System.out.println("Number atoms - " + numberThreads + ":");

        Set<Task> tasks = new HashSet<>();
        for (int i = 0; i < numberThreads; i++) {
            tasks.add(new Task(barrier, locker, molecule));
        }

        try {
            executorService.invokeAll(tasks);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(numberThreads < 100 ? (long) numberThreads * 100 : numberThreads, TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        List<String> unused = molecule.getUnused();
        System.out.print("Unused atoms total - " + unused.size() + ": " + unused);
    }
}
