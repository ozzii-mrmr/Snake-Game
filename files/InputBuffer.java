import java.util.ArrayDeque;

/**
 * InputBuffer.java
 *
 * VERİ YAPISI: ArrayDeque<Integer>
 *   - Her eleman: bir yön sabiti (Snake.UP / DOWN / LEFT / RIGHT)
 *
 * Problem:
 *   Oyun 125 ms'de bir güncellenir. Bu sürede oyuncu birden fazla
 *   tuşa basabilir. Basit "son yön" yaklaşımında, hızlı basılan
 *   ara tuşlar kaybolur.
 *
 *   Örnek: Yılan SAĞA gidiyor.
 *   Oyuncu hızla YUKARI + SOL'a basıyor.
 *   Tek güncelleme adımına sığmaz → klasik yılan oyununda
 *   yılan kendi üstüne gelir ve ölür.
 *
 * Çözüm: ArrayDeque (çift uçlu kuyruk) ile FIFO buffer
 *   - Tuş basılınca: offerLast() → kuyruğun sonuna ekle O(1)
 *   - Her oyun adımında: pollFirst() → kuyruktan al O(1)
 *
 * Neden ArrayDeque, LinkedList değil?
 *   - ArrayDeque dizi tabanlı → cache-friendly, LinkedList'ten hızlı
 *   - Amortize O(1) her iki uçta da
 *   - Stack ve Queue olarak kullanılabilen en verimli Java yapısı
 */
public class InputBuffer {

    private static final int MAX_BUFFER = 3; // Fazla birikmeyi önler

    private final ArrayDeque<Integer> queue;
    private int lastDir;

    // ── Kurucu ───────────────────────────────────────────────────
    public InputBuffer(int initialDir) {
        queue   = new ArrayDeque<>();
        lastDir = initialDir;
    }

    // ── Tuş ekleme ───────────────────────────────────────────────

    /**
     * Oyuncu bir tuşa basınca çağrılır.
     * Zıt yön ve aşırı doluluk kontrolü burada yapılır.
     */
    public void push(int newDir) {
        if (queue.size() >= MAX_BUFFER) return;

        // Zıt yön kontrolü: en son eklenen yöne göre filtrele
        int compareDir = queue.isEmpty() ? lastDir : queue.peekLast();
        if (isOpposite(compareDir, newDir)) return;

        // Aynı yönü tekrar ekleme
        if (!queue.isEmpty() && queue.peekLast() == newDir) return;

        queue.offerLast(newDir); // O(1)
    }

    /**
     * Her oyun adımında çağrılır.
     * Kuyruktaki bir sonraki yönü döndürür.
     * Kuyruk boşsa son yönü döndürür.
     */
    public int poll() {
        if (!queue.isEmpty()) {
            lastDir = queue.pollFirst(); // O(1)
        }
        return lastDir;
    }

    /** Kuyruk boş mu? */
    public boolean isEmpty() { return queue.isEmpty(); }

    /** Tamponu sıfırla (yeni oyun). */
    public void reset(int dir) {
        queue.clear();
        lastDir = dir;
    }

    // ── Yardımcı ─────────────────────────────────────────────────
    private boolean isOpposite(int a, int b) {
        return (a == Snake.UP   && b == Snake.DOWN)
            || (a == Snake.DOWN && b == Snake.UP)
            || (a == Snake.LEFT && b == Snake.RIGHT)
            || (a == Snake.RIGHT&& b == Snake.LEFT);
    }
}
