import java.util.Random;

/**
 * ObstacleManager.java
 *
 * VERİ YAPISI 1: MyHashSet<Point>      (java.util.HashSet yerine)
 *   Aktif engeller → O(1) contains
 *
 * VERİ YAPISI 2: MyArrayList<SpawnAnim> (java.util.ArrayList yerine)
 *   Spawn animasyonu kuyruğu
 */
public class ObstacleManager {

    private static final int ENGEL_PER_LEVEL = 3;
    private static final int MIN_LEVEL       = 3;
    private static final int SAFE_RADIUS     = 4;
    private static final int ANIM_TICKS      = 20;

    public static class SpawnAnim {
        public final Point pos;
        public int tick;
        SpawnAnim(Point p) { pos = p; tick = 0; }
        boolean advance()        { return ++tick >= ANIM_TICKS; }
        public float progress()  { return (float) tick / ANIM_TICKS; }
    }

    private final MyHashSet<Point>     obstacles;
    private final MyArrayList<SpawnAnim> spawning;
    private final int gridW, gridH;
    private final Random random;

    public ObstacleManager(int gridW, int gridH) {
        this.gridW     = gridW;
        this.gridH     = gridH;
        this.random    = new Random();
        this.obstacles = new MyHashSet<>();
        this.spawning  = new MyArrayList<>();
    }

    public void spawnForLevel(int level, Snake snake, Food food) {
        if (level < MIN_LEVEL) return;

        int toAdd = ENGEL_PER_LEVEL, attempts = 0;
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

            spawning.add(new SpawnAnim(candidate));
            toAdd--;
        }
    }

    public void tick() {
        // Animasyonları ilerlet; biten animasyonları HashSet'e taşı
        int i = 0;
        while (i < spawning.size()) {
            SpawnAnim anim = spawning.get(i);
            if (anim.advance()) {
                obstacles.add(anim.pos);
                spawning.remove(i); // kaydır, i artırma
            } else {
                i++;
            }
        }
    }

    public boolean isObstacle(Point p) {
        return obstacles.contains(p);
    }

    public boolean isSpawning(Point p) {
        for (SpawnAnim a : spawning)
            if (a.pos.equals(p)) return true;
        return false;
    }

    public void clear() {
        obstacles.clear();
        spawning.clear();
    }

    public MyHashSet<Point>       getObstacles() { return obstacles; }
    public MyArrayList<SpawnAnim> getSpawning()  { return spawning; }

    private boolean isTooClose(int x, int y, int cx, int cy, int r) {
        int dx = x - cx, dy = y - cy;
        return (dx*dx + dy*dy) < (r*r);
    }
}