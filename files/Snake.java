import java.util.LinkedList;

/**
 * Snake.java
 *
 * Yılanın gövdesini ve hareketini yönetir.
 *
 * VERİ YAPISI: LinkedList<Point>
 * - Baş (head) → listenin ilk elemanı  (index 0)
 * - Kuyruk (tail) → listenin son elemanı
 *
 * Neden LinkedList?
 *   Her adımda başa yeni bir segment EKLENİR (addFirst → O(1))
 *   ve kuyruktan bir segment ÇIKARILIR (removeLast → O(1)).
 *   ArrayList'te bu işlemler O(n) olurdu (kaydırma gerekir).
 */
public class Snake {

    // ── Yön sabitleri ────────────────────────────────────────────
    public static final int UP    = 0;
    public static final int DOWN  = 1;
    public static final int LEFT  = 2;
    public static final int RIGHT = 3;

    // ── Alanlar ──────────────────────────────────────────────────
    private LinkedList<Point> body;   // Yılanın gövde segmentleri
    private int direction;             // Mevcut yön
    private int nextDirection;         // Oyuncu tarafından seçilen sonraki yön

    // ── Kurucu ───────────────────────────────────────────────────
    public Snake(int startX, int startY) {
        body = new LinkedList<>();

        // Yılan 3 segment ile başlar (yatay, sağa bakan)
        body.addFirst(new Point(startX,     startY));   // baş
        body.addFirst(new Point(startX - 1, startY));   // orta
        body.addFirst(new Point(startX - 2, startY));   // kuyruk
        // Not: addFirst kullandık, bu yüzden son eklenen = kuyruk (index 0)
        //      Ama mantığı tersine çevirelim: baş = getLast()

        // Açıklama: biz getFirst() = baş olarak kullanacağız.
        // Yukarıdaki ekleme sırasını düzeltelim:
        body.clear();
        body.addLast(new Point(startX,     startY));   // baş (index 0)
        body.addLast(new Point(startX - 1, startY));   // gövde
        body.addLast(new Point(startX - 2, startY));   // kuyruk

        direction     = RIGHT;
        nextDirection = RIGHT;
    }

    // ── Hareket ─────────────────────────────────────────────────

    /**
     * Yılanı bir adım ilerletir.
     * @param grow true ise kuyruk kesilmez (yiyecek yenildi)
     * @return Yeni baş pozisyonu
     */
    public Point move(boolean grow) {
        // Zıt yöne gitmeyi engelle
        direction = nextDirection;

        Point head = body.getFirst(); // mevcut baş
        Point newHead = calculateNewHead(head);

        body.addFirst(newHead); // Yeni başı öne ekle → O(1)

        if (!grow) {
            body.removeLast();   // Kuyruğu çıkar → O(1)
        }

        return newHead;
    }

    /** Mevcut yöne göre yeni baş koordinatını hesaplar. */
    private Point calculateNewHead(Point current) {
        switch (direction) {
            case UP:    return new Point(current.x,     current.y - 1);
            case DOWN:  return new Point(current.x,     current.y + 1);
            case LEFT:  return new Point(current.x - 1, current.y);
            case RIGHT: return new Point(current.x + 1, current.y);
            default:    return new Point(current.x + 1, current.y);
        }
    }

    // ── Yön değiştirme ──────────────────────────────────────────

    public void setDirection(int newDir) {
        // Zıt yönlere geçişi engelle
        if (isOpposite(direction, newDir)) return;
        nextDirection = newDir;
    }

    private boolean isOpposite(int d1, int d2) {
        return (d1 == UP    && d2 == DOWN)
            || (d1 == DOWN  && d2 == UP)
            || (d1 == LEFT  && d2 == RIGHT)
            || (d1 == RIGHT && d2 == LEFT);
    }

    // ── Çarpışma kontrolleri ─────────────────────────────────────

    /** Yılan kendi gövdesine çarptı mı? */
    public boolean collidesWithSelf() {
        Point head = body.getFirst();
        // getFirst() hariç geri kalanlarla karşılaştır
        for (int i = 1; i < body.size(); i++) {
            if (head.equals(body.get(i))) return true;
        }
        return false;
    }

    /** Verilen nokta yılanın gövdesinde mi? */
    public boolean contains(Point p) {
        return body.contains(p); // LinkedList.contains() → O(n)
    }

    // ── Erişimciler ──────────────────────────────────────────────

    public Point getHead()             { return body.getFirst(); }
    public LinkedList<Point> getBody() { return body; }
    public int getLength()             { return body.size(); }
    public int getDirection()          { return direction; }
}
