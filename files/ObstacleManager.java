import java.util.*;

/**
 * ObstacleManager.java
 *
 * VERİ YAPISI 1: HashSet<Point>     → aktif engeller (O(1) contains)
 * VERİ YAPISI 2: List<SpawnAnim>    → spawn animasyonu kuyruğu
 *
 * Yenilik: Engeller artık anında çıkmaz.
 * spawnForLevel() çağrılınca engeller SpawnAnim listesine girer,
 * her tick'te animasyonu ilerler; tamamlanınca HashSet'e eklenir.
 */
public class ObstacleManager {

    private static final int ENGEL_PER_LEVEL = 3;
    private static final int MIN_LEVEL       = 3;
    private static final int SAFE_RADIUS     = 4;
    private static final int ANIM_TICKS      = 20; // kaç tick'te tamamlanır

    // ── Spawn animasyonu ──────────────────────────────────────────
    public static class SpawnAnim {
        public final Point pos;
        public int tick;                   // 0 → ANIM_TICKS arası ilerler
        SpawnAnim(Point p) { pos = p; tick = 0; }
        /** @return true → animasyon bitti */
        boolean advance() { return ++tick >= ANIM_TICKS; }
        /** 0.0 → 1.0 arası ilerleme */
        public float progress() { return (float) tick / ANIM_TICKS; }
    }

    private final HashSet<Point>    obstacles;   // aktif (tam) engeller
    private final List<SpawnAnim>   spawning;    // animasyondaki engeller
    private final int gridW, gridH;
    private final Random random;

    // ── Kurucu ───────────────────────────────────────────────────
    public ObstacleManager(int gridW, int gridH) {
        this.gridW     = gridW;
        this.gridH     = gridH;
        this.random    = new Random();
        this.obstacles = new HashSet<>();
        this.spawning  = new ArrayList<>();
    }

    // ── Seviye spawn ─────────────────────────────────────────────
    public void spawnForLevel(int level, Snake snake, Food food) {
        if (level < MIN_LEVEL) return;

        int toAdd = ENGEL_PER_LEVEL;
        int attempts = 0;
        int cx = snake.getHead().x, cy = snake.getHead().y;

        while (toAdd > 0 && attempts < 300) {
            attempts++;
            int x = random.nextInt(gridW);
            int y = random.nextInt(gridH);
            Point candidate = new Point(x, y);

            if (isTooClose(x, y, cx, cy, SAFE_RADIUS)) continue;
            if (snake.contains(candidate))              continue;
            if (food.getFoodAt(candidate) != null)      continue;
            if (obstacles.contains(candidate))          continue;
            if (isSpawning(candidate))                  continue;

            spawning.add(new SpawnAnim(candidate)); // animasyon kuyruğuna ekle
            toAdd--;
        }
    }

    // ── Her oyun adımında çağrılır ───────────────────────────────
    /**
     * Animasyonları ilerletir.
     * Tamamlanan animasyonları HashSet'e taşır.
     */
    public void tick() {
        Iterator<SpawnAnim> it = spawning.iterator();
        while (it.hasNext()) {
            SpawnAnim anim = it.next();
            if (anim.advance()) {
                obstacles.add(anim.pos); // HashSet.add → O(1)
                it.remove();
            }
        }
    }

    // ── Sorgulama ────────────────────────────────────────────────
    /** Verilen nokta aktif engel mi? → O(1) */
    public boolean isObstacle(Point p) {
        return obstacles.contains(p);
    }

    /** Verilen nokta animasyonda mı? (çarpışmayı henüz engellemez) */
    public boolean isSpawning(Point p) {
        for (SpawnAnim a : spawning)
            if (a.pos.equals(p)) return true;
        return false;
    }

    public void clear() {
        obstacles.clear();
        spawning.clear();
    }

    public HashSet<Point>  getObstacles() { return obstacles; }
    public List<SpawnAnim> getSpawning()  { return spawning;  }

    // ── Yardımcı ─────────────────────────────────────────────────
    private boolean isTooClose(int x, int y, int cx, int cy, int r) {
        int dx = x - cx, dy = y - cy;
        return (dx * dx + dy * dy) < (r * r);
    }
}