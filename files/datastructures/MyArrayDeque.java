package datastructures;

import java.util.NoSuchElementException;

/**
 * MyArrayDeque — Dairesel Dizi Tabanlı Çift Uçlu Kuyruk (Circular Array Deque)
 *
 * head ve tail göstericileri dairesel olarak ilerler;
 * kaydırma işlemi gerekmez.
 *
 *   [ __ | e2 | e3 | e4 | __ | __ | __ | e0 | e1 ]
 *                             ↑tail  head↑
 *
 * Karmaşıklık:
 *   offerLast / pollFirst / peekLast / peekFirst → O(1)
 *   Kapasite dolunca 2× büyütülür → amortize O(1)
 */
public class MyArrayDeque<T> {

    // ── Alanlar ──────────────────────────────────────────────────
    private Object[] data;
    private int      head;   // ilk elemanın indeksi
    private int      tail;   // bir sonraki yazma indeksi
    private int      size;

    // ── Kurucu ───────────────────────────────────────────────────
    public MyArrayDeque() {
        data = new Object[8];
        head = 0;
        tail = 0;
        size = 0;
    }

    // ── Ekleme ───────────────────────────────────────────────────

    /** Kuyruğun sonuna ekle → O(1) amortize */
    public void offerLast(T item) {
        if (size == data.length) resize();
        data[tail] = item;
        tail       = (tail + 1) % data.length;
        size++;
    }

    // ── Çıkarma ──────────────────────────────────────────────────

    /** Kuyruğun başından al → O(1) */
    @SuppressWarnings("unchecked")
    public T pollFirst() {
        if (isEmpty()) throw new NoSuchElementException("Kuyruk boş");
        T item     = (T) data[head];
        data[head] = null;
        head       = (head + 1) % data.length;
        size--;
        return item;
    }

    // ── Bakma ────────────────────────────────────────────────────

    /** Kuyruğun son elemanına bak (silme) → O(1) */
    @SuppressWarnings("unchecked")
    public T peekLast() {
        if (isEmpty()) return null;
        return (T) data[(tail - 1 + data.length) % data.length];
    }

    /** Kuyruğun ilk elemanına bak (silme) → O(1) */
    @SuppressWarnings("unchecked")
    public T peekFirst() {
        if (isEmpty()) return null;
        return (T) data[head];
    }

    // ── Boyut / Temizlik ─────────────────────────────────────────

    public int     size()    { return size; }
    public boolean isEmpty() { return size == 0; }

    public void clear() {
        for (int i = 0; i < data.length; i++) data[i] = null;
        head = tail = size = 0;
    }

    // ── Yeniden boyutlandırma ────────────────────────────────────

    private void resize() {
        Object[] newData = new Object[data.length * 2];
        for (int i = 0; i < size; i++) {
            newData[i] = data[(head + i) % data.length];
        }
        data = newData;
        head = 0;
        tail = size;
    }
}