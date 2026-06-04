import java.util.Random;

/**
 * PowerUpManager.java
 *
 * VERİ YAPISI 1: MyHashMap<Point, PowerUp>   (java.util.HashMap yerine)
 *   Tahtadaki güç-upları konuma göre O(1) sorgular.
 *
 * VERİ YAPISI 2: MyMinHeap<ActiveEffect>     (java.util.PriorityQueue yerine)
 *   En erken biten efekt her zaman kökte (O(1) peek).
 *   Ekleme/silme O(log n).
 */
public class PowerUpManager {

    public enum PowerUpType {
        SLOW   ("YAVAŞ",  150, 5000),
        SHIELD ("KALKAN", 200, 5000),
        SHRINK ("KÜÇÜLT",  80,    0),
        MAGNET ("MANYЕТ", 120, 8000);

        public final String label;
        public final int    spawnWeight;
        public final int    durationMs;

        PowerUpType(String label, int w, int d) {
            this.label = label; spawnWeight = w; durationMs = d;
        }
    }

    private ObstacleManager obstacles;
    public void setObstacles(ObstacleManager obs) { this.obstacles = obs; }

    public static class PowerUp {
        public final PowerUpType type;
        public final Point       pos;
        public int               lifetime;

        public PowerUp(PowerUpType type, Point pos) {
            this.type     = type;
            this.pos      = pos;
            this.lifetime = 180;
        }
        public boolean tick() { return --lifetime > 0; }
    }

    public static class ActiveEffect implements Comparable<ActiveEffect> {
        public final PowerUpType type;
        public final long        expiresAt;

        public ActiveEffect(PowerUpType type) {
            this.type      = type;
            this.expiresAt = System.currentTimeMillis() + type.durationMs;
        }

        @Override
        public int compareTo(ActiveEffect o) {
            return Long.compare(this.expiresAt, o.expiresAt);
        }
    }

    private final MyHashMap<Point, PowerUp> boardMap;
    private final MyMinHeap<ActiveEffect>   effectQueue;
    private final int    gridW, gridH;
    private final Random random;

    private static final int    SPAWN_INTERVAL = 120;
    private static final double SPAWN_CHANCE   = 0.35;

    public PowerUpManager(int gridW, int gridH) {
        this.gridW       = gridW;
        this.gridH       = gridH;
        this.random      = new Random();
        this.boardMap    = new MyHashMap<>();
        this.effectQueue = new MyMinHeap<>();
    }

    public void tick(Snake snake, Food food, int step) {
        // Süresi dolan boardMap güç-uplarını kaldır
        boardMap.removeValueIf(pu -> !pu.tick());

        // Süresi dolan aktif efektleri kaldır
        long now = System.currentTimeMillis();
        while (!effectQueue.isEmpty() && effectQueue.peek().expiresAt <= now) {
            effectQueue.poll();
        }

        if (step % SPAWN_INTERVAL == 0 && random.nextDouble() < SPAWN_CHANCE) {
            trySpawn(snake, food);
        }
    }

    public PowerUpType collect(Point head) {
        PowerUp pu = boardMap.remove(head);
        if (pu == null) return null;
        if (pu.type.durationMs > 0) {
            effectQueue.offer(new ActiveEffect(pu.type));
        }
        return pu.type;
    }

    public boolean isActive(PowerUpType type) {
        for (ActiveEffect e : effectQueue) {
            if (e.type == type) return true;
        }
        return false;
    }

    public long nextExpiry() {
        if (effectQueue.isEmpty()) return -1;
        return effectQueue.peek().expiresAt - System.currentTimeMillis();
    }

    public MyArrayList<ActiveEffect> getActiveEffects() {
        MyArrayList<ActiveEffect> list = new MyArrayList<>();
        for (ActiveEffect e : effectQueue) {
            list.add(e);
        }
        return list;
    }

    private void trySpawn(Snake snake, Food food) {
        if (boardMap.size() >= 2) return;
        Point pos = findEmpty(snake, food);
        if (pos == null) return;
        PowerUpType type = randomType();
        boardMap.put(pos, new PowerUp(type, pos));
    }

    private PowerUpType randomType() {
        int total = 0;
        for (PowerUpType t : PowerUpType.values()) total += t.spawnWeight;
        int roll = random.nextInt(total);
        for (PowerUpType t : PowerUpType.values()) {
            roll -= t.spawnWeight;
            if (roll < 0) return t;
        }
        return PowerUpType.SLOW;
    }

    private Point findEmpty(Snake snake, Food food) {
        for (int attempt = 0; attempt < 150; attempt++) {
            int x = random.nextInt(gridW);
            int y = random.nextInt(gridH);
            Point c = new Point(x, y);
            if (!snake.contains(c) && food.getFoodAt(c) == null
                    && !boardMap.containsKey(c)
                    && (obstacles == null || !obstacles.isObstacle(c))) return c;
        }
        return null;
    }

    public void clear() {
        boardMap.clear();
        effectQueue.clear();
    }

    public MyHashMap<Point, PowerUp> getBoardMap() { return boardMap; }
}