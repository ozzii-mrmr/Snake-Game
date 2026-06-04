import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * MyLinkedList — Çift Yönlü Bağlı Liste (Doubly Linked List)
 *
 * Her düğüm önceki ve sonraki düğümü gösterir;
 * head ve tail sentinel'ları sayesinde kenar koşulları basitleşir.
 *
 *   head ↔ [e0] ↔ [e1] ↔ [e2] ↔ tail
 *
 * Karmaşıklık:
 *   addFirst / addLast / removeFirst / removeLast → O(1)
 *   get(index) / contains                        → O(n)
 *   size                                         → O(1)
 *
 * Snake.java'nın ihtiyaç duyduğu tüm işlemleri karşılar:
 *   addFirst, addLast, getFirst, getLast,
 *   removeLast, contains, get, size, iterator
 */
public class MyLinkedList<T> implements Iterable<T> {

    // ══════════════════════════════════════════════════════════════
    //  İÇ SINIF: Node
    // ══════════════════════════════════════════════════════════════

    /**
     * Çift yönlü bağlı listenin her hücresi.
     *   data  → saklanan değer
     *   prev  → önceki düğüm
     *   next  → sonraki düğüm
     */
    public static class Node<T> {
        public T       data;
        public Node<T> prev;
        public Node<T> next;

        public Node(T data) {
            this.data = data;
            this.prev = null;
            this.next = null;
        }

        @Override
        public String toString() {
            return "Node(" + data + ")";
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  ALANLAR
    // ══════════════════════════════════════════════════════════════

    private final Node<T> head;   // Sahte baş sentinel (data = null)
    private final Node<T> tail;   // Sahte kuyruk sentinel (data = null)
    private int           size;

    // ══════════════════════════════════════════════════════════════
    //  KURUCU
    // ══════════════════════════════════════════════════════════════

    public MyLinkedList() {
        head = new Node<>(null);  // sentinel — veri taşımaz
        tail = new Node<>(null);  // sentinel — veri taşımaz
        head.next = tail;
        tail.prev = head;
        size = 0;
    }

    // ══════════════════════════════════════════════════════════════
    //  EKLEME
    // ══════════════════════════════════════════════════════════════

    /**
     * Listenin BAŞINA ekle → O(1)
     *
     *  head ↔ [new] ↔ [eski ilk] ↔ ... ↔ tail
     */
    public void addFirst(T data) {
        insertAfter(head, new Node<>(data));
    }

    /**
     * Listenin SONUNA ekle → O(1)
     *
     *  head ↔ ... ↔ [eski son] ↔ [new] ↔ tail
     */
    public void addLast(T data) {
        insertBefore(tail, new Node<>(data));
    }

    // ══════════════════════════════════════════════════════════════
    //  ÇIKARMA
    // ══════════════════════════════════════════════════════════════

    /**
     * Listenin BAŞINDAKI elemanı çıkar ve döndür → O(1)
     * Liste boşsa NoSuchElementException fırlatır.
     */
    public T removeFirst() {
        if (isEmpty()) throw new NoSuchElementException("Liste boş");
        return unlink(head.next);
    }

    /**
     * Listenin SONUNDAKI elemanı çıkar ve döndür → O(1)
     * Liste boşsa NoSuchElementException fırlatır.
     */
    public T removeLast() {
        if (isEmpty()) throw new NoSuchElementException("Liste boş");
        return unlink(tail.prev);
    }

    // ══════════════════════════════════════════════════════════════
    //  BAKMA
    // ══════════════════════════════════════════════════════════════

    /**
     * İlk elemanı döndür (silmez) → O(1)
     */
    public T getFirst() {
        if (isEmpty()) throw new NoSuchElementException("Liste boş");
        return head.next.data;
    }

    /**
     * Son elemanı döndür (silmez) → O(1)
     */
    public T getLast() {
        if (isEmpty()) throw new NoSuchElementException("Liste boş");
        return tail.prev.data;
    }

    /**
     * İndekse göre eleman döndür → O(n)
     * Snake.java gövde çizimi için gereklidir.
     */
    public T get(int index) {
        if (index < 0 || index >= size)
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        Node<T> cur = head.next;
        for (int i = 0; i < index; i++) cur = cur.next;
        return cur.data;
    }

    // ══════════════════════════════════════════════════════════════
    //  ARAMA
    // ══════════════════════════════════════════════════════════════

    /**
     * Eleman listede var mı? → O(n)
     * equals() kullanır — Point.equals() override edilmiş olmalı.
     */
    public boolean contains(T data) {
        Node<T> cur = head.next;
        while (cur != tail) {
            if (cur.data.equals(data)) return true;
            cur = cur.next;
        }
        return false;
    }

    // ══════════════════════════════════════════════════════════════
    //  BOYUT / TEMİZLİK
    // ══════════════════════════════════════════════════════════════

    public int     size()    { return size; }
    public boolean isEmpty() { return size == 0; }

    public void clear() {
        head.next = tail;
        tail.prev = head;
        size = 0;
    }

    // ══════════════════════════════════════════════════════════════
    //  ITERATOR
    // ══════════════════════════════════════════════════════════════

    /**
     * Baştan sona sıralı gezme — for-each döngüsü için.
     */
    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            Node<T> cur = head.next;

            @Override
            public boolean hasNext() { return cur != tail; }

            @Override
            public T next() {
                if (!hasNext()) throw new NoSuchElementException();
                T data = cur.data;
                cur    = cur.next;
                return data;
            }
        };
    }

    // ══════════════════════════════════════════════════════════════
    //  ÖZEL YARDIMCILAR
    // ══════════════════════════════════════════════════════════════

    /**
     * `node`'u `after`'ın hemen ardına ekle.
     *
     *  ... ↔ after ↔ [node] ↔ oldNext ↔ ...
     */
    private void insertAfter(Node<T> after, Node<T> node) {
        Node<T> oldNext = after.next;
        after.next   = node;
        node.prev    = after;
        node.next    = oldNext;
        oldNext.prev = node;
        size++;
    }

    /**
     * `node`'u `before`'un hemen önüne ekle.
     *
     *  ... ↔ oldPrev ↔ [node] ↔ before ↔ ...
     */
    private void insertBefore(Node<T> before, Node<T> node) {
        insertAfter(before.prev, node);
    }

    /**
     * `node`'u listeden çıkar ve datasını döndür.
     *
     *  ... ↔ prev ↔ [node] ↔ next ↔ ...
     *  →  ... ↔ prev ↔ next ↔ ...
     */
    private T unlink(Node<T> node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
        node.prev = null;   // GC yardımı
        node.next = null;
        size--;
        return node.data;
    }
}