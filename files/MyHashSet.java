import java.util.Iterator;

/**
 * MyHashSet — Hash Kümesi
 *
 * MyHashMap<T, Boolean> üzerine inşa edilir;
 * değer olarak her zaman Boolean.TRUE saklanır.
 *
 * Karmaşıklık:
 *   add / contains / remove → ortalama O(1)
 */
public class MyHashSet<T> implements Iterable<T> {

    // ── İç harita ────────────────────────────────────────────────
    private final MyHashMap<T, Boolean> map;

    // ── Kurucu ───────────────────────────────────────────────────
    public MyHashSet() {
        map = new MyHashMap<>();
    }

    // ── Temel işlemler ───────────────────────────────────────────

    /** Kümeye ekle → O(1) amortize */
    public void add(T item) {
        map.put(item, Boolean.TRUE);
    }

    /** Kümede var mı? → O(1) amortize */
    public boolean contains(T item) {
        return map.containsKey(item);
    }

    /** Kümeden çıkar → O(1) amortize */
    public void remove(T item) {
        map.remove(item);
    }

    public int     size()    { return map.size(); }
    public boolean isEmpty() { return map.isEmpty(); }

    public void clear() { map.clear(); }

    // ── Iterator ─────────────────────────────────────────────────

    @Override
    public Iterator<T> iterator() {
        // map.entries() üzerinden sadece key'leri geziyoruz
        MyArrayList<T> keys = new MyArrayList<>();
        for (MyHashMap.Entry<T, Boolean> e : map.entries()) {
            keys.add(e.key);
        }
        return keys.iterator();
    }
}