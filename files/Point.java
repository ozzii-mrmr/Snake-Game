/**
 * Point.java
 * Oyun tahtasındaki bir hücrenin koordinatını temsil eder.
 * HashMap'te anahtar (key) olarak kullanıldığı için
 * equals() ve hashCode() metodları override edilmiştir.
 */
public class Point {
    public int x;
    public int y;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /** Bir kopyasını döndürür — LinkedList'e eklenmeden önce kullanılır. */
    public Point copy() {
        return new Point(this.x, this.y);
    }

    /**
     * HashMap'te doğru çalışması için equals() zorunludur.
     * İki Point, aynı x ve y değerlerine sahipse eşittir.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Point)) return false;
        Point other = (Point) obj;
        return this.x == other.x && this.y == other.y;
    }

    /**
     * HashMap'te doğru çalışması için hashCode() zorunludur.
     * equals() true döndürüyorsa hashCode() aynı olmalıdır!
     */
    @Override
    public int hashCode() {
        // Yaygın bir hash formülü: 31 * x + y
        return 31 * x + y;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}
