import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * MyHashMap — Zincir Çakışma Yönetimli Hash Tablosu (Separate Chaining)
 *
 * Her hücre bir bağlı zincir başıdır; aynı hash'e düşen anahtarlar
 * o zincire eklenir.
 *
 *   index 0 → [k0|v0] → null
 *   index 1 → [k1|v1] → [k2|v2] → null   (aynı hash)
 *   index 2 → null
 *   ...
 *
 * Karmaşıklık (ortalama):
 *   put / get / remove → O(1)
 *   Yük faktörü > 0.75 ise tablo 2× büyütülür → amortize O(1)
 */
public class MyHashMap<K, V> {

    // ── Giriş düğümü ─────────────────────────────────────────────
    public static class Entry<K, V> {
        public K         key;
        public V         value;
        Entry<K, V> next;   // zincirin sonraki düğümü

        Entry(K key, V value) {
            this.key   = key;
            this.value = value;
        }
    }

    // ── Yardımcı fonksiyonel arayüz ──────────────────────────────
    /** removeValueIf() için yüklem. */
    public interface ValuePredicate<V> {
        boolean test(V value);
    }

    // ── Sabitler ─────────────────────────────────────────────────
    private static final int   DEFAULT_CAPACITY = 16;
    private static final float LOAD_FACTOR      = 0.75f;

    // ── Alanlar ──────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private Entry<K, V>[] table = new Entry[DEFAULT_CAPACITY];
    private int            size  = 0;

    // ── Hash yardımcısı ──────────────────────────────────────────

    private int index(K key) {
        return (key.hashCode() & 0x7FFF_FFFF) % table.length;
    }

    // ── Temel işlemler ───────────────────────────────────────────

    /**
     * Anahtar-değer ekler / günceller → O(1) amortize
     * @return Eski değer (varsa), yoksa null
     */
    public V put(K key, V value) {
        if ((float) size / table.length >= LOAD_FACTOR) resize();

        int i = index(key);
        for (Entry<K, V> e = table[i]; e != null; e = e.next) {
            if (e.key.equals(key)) {
                V old   = e.value;
                e.value = value;
                return old;
            }
        }
        // Zincirin başına ekle
        Entry<K, V> node = new Entry<>(key, value);
        node.next = table[i];
        table[i]  = node;
        size++;
        return null;
    }

    /**
     * Anahtara karşılık gelen değeri döndürür → O(1) amortize
     * Yoksa null döner.
     */
    public V get(K key) {
        int i = index(key);
        for (Entry<K, V> e = table[i]; e != null; e = e.next) {
            if (e.key.equals(key)) return e.value;
        }
        return null;
    }

    /**
     * Anahtarı siler → O(1) amortize
     * @return Silinen değer, yoksa null
     */
    public V remove(K key) {
        int         i    = index(key);
        Entry<K, V> prev = null;
        Entry<K, V> cur  = table[i];

        while (cur != null) {
            if (cur.key.equals(key)) {
                if (prev == null) table[i]  = cur.next;
                else              prev.next = cur.next;
                size--;
                return cur.value;
            }
            prev = cur;
            cur  = cur.next;
        }
        return null;
    }

    /** Anahtar var mı? → O(1) amortize */
    public boolean containsKey(K key) {
        return get(key) != null;
    }

    public int     size()    { return size; }
    public boolean isEmpty() { return size == 0; }

    public void clear() {
        for (int i = 0; i < table.length; i++) table[i] = null;
        size = 0;
    }

    // ── Değer bazlı işlemler ─────────────────────────────────────

    /**
     * Verilen değere sahip ilk kaydı siler → O(n)
     */
    public boolean removeValue(V value) {
        for (int i = 0; i < table.length; i++) {
            Entry<K, V> prev = null, cur = table[i];
            while (cur != null) {
                if (cur.value.equals(value)) {
                    if (prev == null) table[i]  = cur.next;
                    else              prev.next  = cur.next;
                    size--;
                    return true;
                }
                prev = cur;
                cur  = cur.next;
            }
        }
        return false;
    }

    /**
     * Koşulu sağlayan tüm value'lara ait kayıtları siler → O(n)
     */
    public void removeValueIf(ValuePredicate<V> pred) {
        for (int i = 0; i < table.length; i++) {
            Entry<K, V> prev = null, cur = table[i];
            while (cur != null) {
                if (pred.test(cur.value)) {
                    if (prev == null) table[i]  = cur.next;
                    else              prev.next  = cur.next;
                    size--;
                    cur = (prev == null) ? table[i] : prev.next;
                } else {
                    prev = cur;
                    cur  = cur.next;
                }
            }
        }
    }

    // ── Gezme ────────────────────────────────────────────────────

    /**
     * Tüm key-value çiftlerini MyArrayList olarak döndürür.
     * (java.util.Map#entrySet() karşılığı)
     */
    public MyArrayList<Entry<K, V>> entries() {
        MyArrayList<Entry<K, V>> list = new MyArrayList<>();
        for (int i = 0; i < table.length; i++) {
            for (Entry<K, V> e = table[i]; e != null; e = e.next) {
                list.add(e);
            }
        }
        return list;
    }

    // ── Yeniden boyutlandırma ────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void resize() {
        Entry<K, V>[] old = table;
        table = new Entry[old.length * 2];
        size  = 0;
        for (Entry<K, V> head : old) {
            for (Entry<K, V> e = head; e != null; e = e.next) {
                put(e.key, e.value);
            }
        }
    }
}