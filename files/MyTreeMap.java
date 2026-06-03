import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * MyTreeMap — İkili Arama Ağacı Tabanlı Sıralı Map (BST Map)
 *
 * Anahtarlar her zaman sıralı tutulur (sol < kök < sağ).
 *
 *          50
 *         /  \
 *        20   80
 *       /  \
 *      10  30
 *
 * Karmaşıklık (dengeli durumda):
 *   put / get / remove   → O(log n)
 *   firstKey / lastKey   → O(log n)
 *   pollFirstEntry       → O(log n)
 *   Sıralı gezme         → O(n)
 */
public class MyTreeMap<K extends Comparable<K>, V> {

    // ── İç sınıf: BST düğümü ─────────────────────────────────────
    public static class Entry<K, V> {
        public K key;
        public V value;
        Entry<K, V> left;
        Entry<K, V> right;

        Entry(K key, V value) {
            this.key   = key;
            this.value = value;
        }
    }

    // ── Alanlar ──────────────────────────────────────────────────
    private Entry<K, V> root;
    private int         size;

    // ── Kurucu ───────────────────────────────────────────────────
    public MyTreeMap() {
        root = null;
        size = 0;
    }

    // ── Temel işlemler ───────────────────────────────────────────

    /**
     * Anahtar-değer ekle / güncelle → O(log n)
     * @return Eski değer (varsa), yoksa null
     */
    public V put(K key, V value) {
        // Eski değeri tutmak için tek elemanlı dizi hile
        @SuppressWarnings("unchecked")
        V[] old = (V[]) new Object[1];
        root = putRec(root, key, value, old);
        return old[0];
    }

    @SuppressWarnings("unchecked")
    private Entry<K, V> putRec(Entry<K, V> node, K key, V value, V[] old) {
        if (node == null) {
            size++;
            return new Entry<>(key, value);
        }
        int cmp = key.compareTo(node.key);
        if      (cmp < 0) node.left  = putRec(node.left,  key, value, old);
        else if (cmp > 0) node.right = putRec(node.right, key, value, old);
        else {
            old[0]     = node.value;
            node.value = value;
        }
        return node;
    }

    /**
     * Anahtara göre değer al → O(log n)
     */
    public V get(K key) {
        Entry<K, V> node = root;
        while (node != null) {
            int cmp = key.compareTo(node.key);
            if      (cmp < 0) node = node.left;
            else if (cmp > 0) node = node.right;
            else              return node.value;
        }
        return null;
    }

    // ── Min / Max ────────────────────────────────────────────────

    /** En küçük anahtarı döndür → O(log n) */
    public K firstKey() {
        if (root == null) throw new NoSuchElementException("Harita boş");
        Entry<K, V> cur = root;
        while (cur.left != null) cur = cur.left;
        return cur.key;
    }

    /** En büyük anahtarı döndür → O(log n) */
    public K lastKey() {
        if (root == null) throw new NoSuchElementException("Harita boş");
        Entry<K, V> cur = root;
        while (cur.right != null) cur = cur.right;
        return cur.key;
    }

    /**
     * En küçük girişi çıkar ve döndür → O(log n)
     * java.util.TreeMap#pollFirstEntry() karşılığı
     */
    public Entry<K, V> pollFirstEntry() {
        if (root == null) return null;
        @SuppressWarnings("unchecked")
        Entry<K, V>[] min = new Entry[1];
        root = removeMin(root, min);
        if (min[0] != null) size--;
        return min[0];
    }

    @SuppressWarnings("unchecked")
    private Entry<K, V> removeMin(Entry<K, V> node, Entry<K, V>[] min) {
        if (node.left == null) {
            min[0] = node;
            return node.right;    // sol çocuk yok → sağ çocuğu bağ
        }
        node.left = removeMin(node.left, min);
        return node;
    }

    // ── Boyut / Temizlik ─────────────────────────────────────────

    public boolean isEmpty() { return size == 0; }
    public int     size()    { return size; }

    public void clear() {
        root = null;
        size = 0;
    }

    // ── Sıralı gezme ─────────────────────────────────────────────

    /**
     * Artan sırada tüm entry'leri döndürür (in-order gezme).
     * java.util.TreeMap#entrySet() karşılığı
     */
    public MyArrayList<Entry<K, V>> entrySet() {
        MyArrayList<Entry<K, V>> list = new MyArrayList<>();
        inOrder(root, list);
        return list;
    }

    /**
     * Azalan sırada key'leri döndürür (reverse in-order).
     * java.util.TreeMap#descendingKeySet() karşılığı
     */
    public MyArrayList<K> descendingKeySet() {
        MyArrayList<K> list = new MyArrayList<>();
        reverseOrder(root, list);
        return list;
    }

    // ── Yardımcı rekürsif gezme ───────────────────────────────────

    private void inOrder(Entry<K, V> node, MyArrayList<Entry<K, V>> list) {
        if (node == null) return;
        inOrder(node.left,  list);
        list.add(node);
        inOrder(node.right, list);
    }

    private void reverseOrder(Entry<K, V> node, MyArrayList<K> list) {
        if (node == null) return;
        reverseOrder(node.right, list);
        list.add(node.key);
        reverseOrder(node.left,  list);
    }
}