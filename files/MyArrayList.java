import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * MyArrayList — Dinamik Dizi (Dynamic Array)
 *
 * Dahili dizi dolunca kapasitesi 2× büyütülür.
 *
 *   [ e0 | e1 | e2 | e3 | __ | __ ]
 *                    ↑ size        ↑ capacity
 *
 * Karmaşıklık:
 *   add (sona)   → amortize O(1)
 *   get          → O(1)
 *   remove(idx)  → O(n)  (kaydırma gerektirir)
 *   removeIf     → O(n)
 */
public class MyArrayList<T> implements Iterable<T> {

    // ── Yardımcı fonksiyonel arayüz ──────────────────────────────
    /** removeIf() için yüklem. */
    public interface Predicate<T> {
        boolean test(T item);
    }

    // ── Alanlar ──────────────────────────────────────────────────
    private Object[] data;
    private int      size;

    // ── Kurucu ───────────────────────────────────────────────────
    public MyArrayList() {
        data = new Object[8];
        size = 0;
    }

    // ── Ekleme ───────────────────────────────────────────────────

    /** Sona ekle → amortize O(1) */
    public void add(T item) {
        if (size == data.length) resize();
        data[size++] = item;
    }

    // ── Erişim ───────────────────────────────────────────────────

    /** İndekse göre eriş → O(1) */
    @SuppressWarnings("unchecked")
    public T get(int index) {
        if (index < 0 || index >= size)
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        return (T) data[index];
    }

    // ── Silme ────────────────────────────────────────────────────

    /** İndekse göre sil, kaydır → O(n) */
    @SuppressWarnings("unchecked")
    public T remove(int index) {
        if (index < 0 || index >= size)
            throw new IndexOutOfBoundsException("Index: " + index);
        T item = (T) data[index];
        System.arraycopy(data, index + 1, data, index, size - index - 1);
        data[--size] = null;
        return item;
    }

    /**
     * Koşulu sağlayan tüm elemanları tek geçişte sil → O(n)
     * java.util.Collection#removeIf() karşılığı
     */
    @SuppressWarnings("unchecked")
    public void removeIf(Predicate<T> pred) {
        int write = 0;
        for (int read = 0; read < size; read++) {
            T item = (T) data[read];
            if (!pred.test(item)) {
                data[write++] = item;
            }
        }
        // Artık kullanılmayan hücreleri temizle
        for (int i = write; i < size; i++) data[i] = null;
        size = write;
    }

    // ── Boyut / Temizlik ─────────────────────────────────────────

    public int     size()    { return size; }
    public boolean isEmpty() { return size == 0; }

    public void clear() {
        for (int i = 0; i < size; i++) data[i] = null;
        size = 0;
    }

    // ── Iterator ─────────────────────────────────────────────────

    @Override
    @SuppressWarnings("unchecked")
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            int i = 0;

            @Override
            public boolean hasNext() { return i < size; }

            @Override
            public T next() {
                if (!hasNext()) throw new NoSuchElementException();
                return (T) data[i++];
            }
        };
    }

    // ── Yeniden boyutlandırma ────────────────────────────────────

    private void resize() {
        Object[] newData = new Object[data.length * 2];
        System.arraycopy(data, 0, newData, 0, size);
        data = newData;
    }
}