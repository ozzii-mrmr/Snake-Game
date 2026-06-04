/**
 * Snake.java
 *
 * VERİ YAPISI: MyLinkedList<Point>  (java.util.LinkedList yerine)
 *   - Baş (head) → listenin ilk elemanı  (index 0)
 *   - Kuyruk (tail) → listenin son elemanı
 *
 * Neden MyLinkedList?
 *   addFirst / removeLast → O(1)  (ArrayList'te O(n) olurdu)
 */
public class Snake {

    public static final int UP    = 0;
    public static final int DOWN  = 1;
    public static final int LEFT  = 2;
    public static final int RIGHT = 3;

    private MyLinkedList<Point> body;
    private int direction;
    private int nextDirection;

    private boolean wrapAround = false;
    private int gridW, gridH;

    public Snake(int startX, int startY) {
        body = new MyLinkedList<>();
        body.addLast(new Point(startX,     startY));
        body.addLast(new Point(startX - 1, startY));
        body.addLast(new Point(startX - 2, startY));
        direction     = RIGHT;
        nextDirection = RIGHT;
    }

    public void setWrapAround(boolean enabled, int gridW, int gridH) {
        this.wrapAround = enabled;
        this.gridW      = gridW;
        this.gridH      = gridH;
    }

    public Point move(boolean grow) {
        direction = nextDirection;
        Point head    = body.getFirst();
        Point newHead = calculateNewHead(head);

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

    public void shrink(int amount) {
        int target = Math.max(3, body.size() - amount);
        while (body.size() > target) {
            body.removeLast();
        }
    }

    public boolean collidesWithSelf() {
        Point head = body.getFirst();
        java.util.Iterator<Point> it = body.iterator();
        it.next(); // başı atla
        while (it.hasNext()) {
            if (head.equals(it.next())) return true;
        }
        return false;
    }

    public boolean contains(Point p) { return body.contains(p); }

    public Point              getHead()          { return body.getFirst(); }
    public MyLinkedList<Point> getBody()          { return body; }
    public int                getLength()        { return body.size(); }
    public int                getDirection()     { return direction; }
    public int                getNextDirection() { return nextDirection; }
    public boolean            isWrapAround()     { return wrapAround; }
}