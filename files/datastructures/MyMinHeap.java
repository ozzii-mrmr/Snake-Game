package datastructures;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * MyMinHeap — İkili Min-Heap (Binary Min-Heap)
 *
 * Dizi içine gömülü tam ikili ağaç:
 *   ebeveyn(i) = (i-1)/2
 *   sol  çocuk = 2i+1
 *   sağ  çocuk = 2i+2
 *
 *   Kök (index 0) her zaman en küçük elemandır.
 *
 *        [0]
 *       /    \
 *     [1]    [2]
 *    /  \   /  \
 *   [3] [4][5] [6]
 *
 * Karmaşıklık:
 *   offer (ekle)  → O(log n)  — yukarı süzme (sift-up)
 *   poll  (al)    → O(log n)  — aşağı süzme (sift-down)
 *   peek  (bak)   → O(1)
 */
public class MyMinHeap<T extends Comparable<T>> implements Iterable<T> {

    // ── Alanlar ──────────────────────────────────────────────────
    private Object[] data;
    private int      size;

    // ── Kurucu ───────────────────────────────────────────────────
    public MyMinHeap() {
        data = new Object[16];
        size = 0;
    }

    // ── Ekleme ───────────────────────────────────────────────────

    /**
     * Heap'e eleman ekle → O(log n)
     * Sona ekle, sonra yukarı süz (sift-up).
     */
    public void offer(T item) {
        if (size == data.length) resize();
        data[size] = item;
        siftUp(size);
        size++;
    }

    // ── Çıkarma ──────────────────────────────────────────────────

    /**
     * Kökü (en küçük) çıkar ve döndür → O(log n)
     * Kökü son eleman ile değiş, sonra aşağı süz (sift-down).
     */
    @SuppressWarnings("unchecked")
    public T poll() {
        if (isEmpty()) throw new NoSuchElementException("Heap boş");
        T root     = (T) data[0];
        data[0]    = data[--size];
        data[size] = null;
        if (size > 0) siftDown(0);
        return root;
    }

    // ── Bakma ────────────────────────────────────────────────────

    /** Kökü döndür (silmez) → O(1) */
    @SuppressWarnings("unchecked")
    public T peek() {
        return isEmpty() ? null : (T) data[0];
    }

    // ── Boyut / Temizlik ─────────────────────────────────────────

    public int     size()    { return size; }
    public boolean isEmpty() { return size == 0; }

    public void clear() {
        for (int i = 0; i < size; i++) data[i] = null;
        size = 0;
    }

    // ── Süzme işlemleri ──────────────────────────────────────────

    /** Ebeveynden küçükse yukarı çık. */
    @SuppressWarnings("unchecked")
    private void siftUp(int i) {
        while (i > 0) {
            int parent = (i - 1) / 2;
            if (((T) data[i]).compareTo((T) data[parent]) < 0) {
                swap(i, parent);
                i = parent;
            } else {
                break;
            }
        }
    }

    /** Çocuklardan büyükse aşağı in. */
    @SuppressWarnings("unchecked")
    private void siftDown(int i) {
        while (true) {
            int left     = 2 * i + 1;
            int right    = 2 * i + 2;
            int smallest = i;

            if (left  < size && ((T) data[left ]).compareTo((T) data[smallest]) < 0)
                smallest = left;
            if (right < size && ((T) data[right]).compareTo((T) data[smallest]) < 0)
                smallest = right;

            if (smallest == i) break;
            swap(i, smallest);
            i = smallest;
        }
    }

    private void swap(int a, int b) {
        Object tmp = data[a];
        data[a]    = data[b];
        data[b]    = tmp;
    }

    // ── Iterator (sırasız gezme) ──────────────────────────────────

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