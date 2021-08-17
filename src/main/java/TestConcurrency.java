public class TestConcurrency {
    public static void main(String[] args) {
        ElementaryInputThread elementaryInputThread = new ElementaryInputThread();
        elementaryInputThread.start();
    }
}
