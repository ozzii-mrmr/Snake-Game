import java.util.LinkedList;

/**
 * Snake.java
 *
 * VERİ YAPISI: LinkedList<Point>
 *   - Baş (head) → listenin ilk elemanı  (index 0)
 *   - Kuyruk (tail) → listenin son elemanı
 *
 * Yeni özellikler:
 *   - wrapAround: duvardan geçince karşı taraftan çıkış
 *   - shrink()  : kuyruğu kısaltan güç-up için
 */
public class Snake {

    public static final int UP    = 0;
    public static final int DOWN  = 1;
    public static final int LEFT  = 2;
    public static final int RIGHT = 3;

    private LinkedList<Point> body;
    private int direction;
    private int nextDirection;

    // Wrap-around modu (GamePanel tarafından set edilir)
    private boolean wrapAround = false;
    private int gridW, gridH;

    // ── Kurucu ───────────────────────────────────────────────────
    public Snake(int startX, int startY) {
        body = new LinkedList<>();
        body.addLast(new Point(startX,     startY));
        body.addLast(new Point(startX - 1, startY));
        body.addLast(new Point(startX - 2, startY));
        direction     = RIGHT;
        nextDirection = RIGHT;
    }

    // ── Wrap-around yapılandırma ──────────────────────────────────
    public void setWrapAround(boolean enabled, int gridW, int gridH) {
        this.wrapAround = enabled;
        this.gridW      = gridW;
        this.gridH      = gridH;
    }

    // ── Hareket ──────────────────────────────────────────────────
    public Point move(boolean grow) {
        direction = nextDirection;
        Point head    = body.getFirst();
        Point newHead = calculateNewHead(head);

        // Wrap-around: sınır dışına çıkarsa karşı taraftan gir
        if (wrapAround) {
            newHead.x = (newHead.x + gridW) % gridW;
            newHead.y = (newHead.y + gridH) % gridH;
        }

        body.addFirst(newHead);
        if (!grow) body.removeLast();
        return newHead;
    }

    private Point calculateNewHead(Point current) {
        switch (direction) {
            case UP:    return new Point(current.x,     current.y - 1);
            case DOWN:  return new Point(current.x,     current.y + 1);
            case LEFT:  return new Point(current.x - 1, current.y);
            case RIGHT: return new Point(current.x + 1, current.y);
            default:    return new Point(current.x + 1, current.y);
        }
    }

    // ── Yön ──────────────────────────────────────────────────────
    public void setDirection(int newDir) {
        if (isOpposite(direction, newDir)) return;
        nextDirection = newDir;
    }

    public void applyDirection(int dir) {
        nextDirection = dir;
    }

    private boolean isOpposite(int d1, int d2) {
        return (d1 == UP    && d2 == DOWN)
                || (d1 == DOWN  && d2 == UP)
                || (d1 == LEFT  && d2 == RIGHT)
                || (d1 == RIGHT && d2 == LEFT);
    }

    // ── Güç-up: SHRINK ───────────────────────────────────────────
    /**
     * Kuyruğu `amount` kadar kısaltır.
     * Minimum uzunluk 3'te tutulur.
     */
    public void shrink(int amount) {
        int target = Math.max(3, body.size() - amount);
        while (body.size() > target) {
            body.removeLast(); // LinkedList → O(1)
        }
    }

    // ── Çarpışma ─────────────────────────────────────────────────
    public boolean collidesWithSelf() {
        Point head = body.getFirst();
        for (int i = 1; i < body.size(); i++) {
            if (head.equals(body.get(i))) return true;
        }
        return false;
    }

    public boolean contains(Point p) { return body.contains(p); }

    // ── Erişimciler ──────────────────────────────────────────────
    public Point             getHead()          { return body.getFirst(); }
    public LinkedList<Point> getBody()          { return body; }
    public int               getLength()        { return body.size(); }
    public int               getDirection()     { return direction; }
    public int               getNextDirection() { return nextDirection; }
    public boolean           isWrapAround()     { return wrapAround; }
}