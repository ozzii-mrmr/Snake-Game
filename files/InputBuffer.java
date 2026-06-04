/**
 * InputBuffer.java
 *
 * VERİ YAPISI: MyArrayDeque<Integer>  (java.util.ArrayDeque yerine)
 *   - Her eleman: bir yön sabiti (Snake.UP / DOWN / LEFT / RIGHT)
 *
 * Çözüm: Dairesel dizi tabanlı çift uçlu kuyruk ile FIFO buffer
 *   - Tuş basılınca: offerLast() → kuyruğun sonuna ekle O(1)
 *   - Her oyun adımında: pollFirst() → kuyruktan al O(1)
 */
public class InputBuffer {

    private static final int MAX_BUFFER = 3;

    private final MyArrayDeque<Integer> queue;
    private int lastDir;

    public InputBuffer(int initialDir) {
        queue   = new MyArrayDeque<>();
        lastDir = initialDir;
    }

    public void push(int newDir) {
        if (queue.size() >= MAX_BUFFER) return;

        int compareDir = queue.isEmpty() ? lastDir : queue.peekLast();
        if (isOpposite(compareDir, newDir)) return;
        if (!queue.isEmpty() && queue.peekLast() == newDir) return;

        queue.offerLast(newDir);
    }

    public int poll() {
        if (!queue.isEmpty()) {
            lastDir = queue.pollFirst();
        }
        return lastDir;
    }

    public boolean isEmpty() { return queue.isEmpty(); }

    public void reset(int dir) {
        queue.clear();
        lastDir = dir;
    }

    private boolean isOpposite(int a, int b) {
        return (a == Snake.UP    && b == Snake.DOWN)
                || (a == Snake.DOWN  && b == Snake.UP)
                || (a == Snake.LEFT  && b == Snake.RIGHT)
                || (a == Snake.RIGHT && b == Snake.LEFT);
    }
}