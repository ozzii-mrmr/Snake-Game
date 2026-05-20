import java.util.*;

/**
 * PowerUpManager.java
 *
 * Oyundaki güç-upları yönetir.
 *
 * VERİ YAPISI 1: HashMap<Point, PowerUp>
 *   Tahtadaki aktif güç-upları konuma göre O(1) sorgular.
 *
 * VERİ YAPISI 2: PriorityQueue<ActiveEffect>
 *   Üst üste binen efektlerin hangisinin önce biteceğini
 *   heap ile yönetir. Her poll() → O(log n).
 *   En erken biten efekt her zaman kuyruğun başında.
 *
 *   Neden PriorityQueue?
 *   Birden fazla efekt aynı anda aktif olabilir (hız yavaşlama +
 *   dokunulmazlık). Her tick'te "süresi dolan var mı?" diye
 *   tüm listeyi taramak O(n); PriorityQueue ile peek() O(1).
 *
 * Güç-up türleri:
 *   SLOW    → 5 saniyeliğine yılanı yavaşlatır (hız ÷2)
 *   SHIELD  → 5 saniyeliğine engel çarpışmasını engeller
 *   SHRINK  → kuyruğu 3 segment kısaltır (anlık)
 *   MAGNET  → 8 saniyeliğine yakındaki yiyecekleri çeker (skoru 2x)
 */
public class PowerUpManager {

    // ── Güç-up türleri ───────────────────────────────────────────
    public enum PowerUpType {
        SLOW   ("YAVAŞ",   150, 5000),   // ms cinsinden süre
        SHIELD ("KALKAN",  200, 5000),
        SHRINK ("KÜÇÜLT",  80,  0),      // anlık → süre yok
        MAGNET ("MANYЕТ",  120, 8000);

        public final String label;
        public final int    spawnWeight;  // yüksek = daha sık çıkar
        public final int    durationMs;

        PowerUpType(String label, int w, int d) {
            this.label = label; spawnWeight = w; durationMs = d;
        }
    }

    // ── Tahtadaki bekleyen güç-up ─────────────────────────────────
    public static class PowerUp {
        public final PowerUpType type;
        public final Point       pos;
        public int               lifetime;   // tick cinsinden

        public PowerUp(PowerUpType type, Point pos) {
            this.type     = type;
            this.pos      = pos;
            this.lifetime = 180; // ~22 saniye (120ms tick × 180)
        }
        public boolean tick() { return --lifetime > 0; }
    }

    // ── Aktif efekt (PriorityQueue elemanı) ──────────────────────
    public static class ActiveEffect implements Comparable<ActiveEffect> {
        public final PowerUpType type;
        public final long        expiresAt;  // System.currentTimeMillis()

        public ActiveEffect(PowerUpType type) {
            this.type      = type;
            this.expiresAt = System.currentTimeMillis() + type.durationMs;
        }

        @Override
        public int compareTo(ActiveEffect o) {
            return Long.compare(this.expiresAt, o.expiresAt); // en erken biten başta
        }
    }

    // ── Alanlar ──────────────────────────────────────────────────
    private final HashMap<Point, PowerUp>       boardMap;    // konum → bekleyen güç-up
    private final PriorityQueue<ActiveEffect>   effectQueue; // aktif efektler, en erken biten başta
    private final int gridW, gridH;
    private final Random random;

    private static final int SPAWN_INTERVAL = 120; // her N adımda bir spawn şansı
    private static final double SPAWN_CHANCE = 0.35;

    // ── Kurucu ───────────────────────────────────────────────────
    public PowerUpManager(int gridW, int gridH) {
        this.gridW      = gridW;
        this.gridH      = gridH;
        this.random     = new Random();
        this.boardMap   = new HashMap<>();
        this.effectQueue= new PriorityQueue<>(); // min-heap: en erken biten başta
    }

    // ── Oyun döngüsü adımı ───────────────────────────────────────

    /**
     * Her oyun adımında çağrılır.
     * Süresi dolan boardMap güç-uplarını kaldırır.
     * Süresi dolan aktif efektleri kaldırır.
     */
    public void tick(Snake snake, Food food, int step) {
        // Süresi dolan boardMap öğelerini kaldır
        boardMap.values().removeIf(pu -> !pu.tick());

        // Süresi dolan aktif efektleri kaldır (PriorityQueue.peek → O(1))
        long now = System.currentTimeMillis();
        while (!effectQueue.isEmpty() && effectQueue.peek().expiresAt <= now) {
            effectQueue.poll(); // O(log n)
        }

        // Spawn şansı
        if (step % SPAWN_INTERVAL == 0 && random.nextDouble() < SPAWN_CHANCE) {
            trySpawn(snake, food);
        }
    }

    // ── Güç-up alma ──────────────────────────────────────────────

    /**
     * Baş hücresinde güç-up varsa al ve uygula.
     * @return Alınan güç-up türü, yoksa null
     */
    public PowerUpType collect(Point head) {
        PowerUp pu = boardMap.remove(head); // HashMap.remove → O(1)
        if (pu == null) return null;

        if (pu.type.durationMs > 0) {
            effectQueue.offer(new ActiveEffect(pu.type)); // PriorityQueue.offer → O(log n)
        }
        return pu.type;
    }

    // ── Efekt sorguları ──────────────────────────────────────────

    public boolean isActive(PowerUpType type) {
        for (ActiveEffect e : effectQueue) {
            if (e.type == type) return true;
        }
        return false;
    }

    /** En erken biten efektin kalan süresi (ms). */
    public long nextExpiry() {
        if (effectQueue.isEmpty()) return -1;
        return effectQueue.peek().expiresAt - System.currentTimeMillis();
    }

    /** Tüm aktif efektler. */
    public List<ActiveEffect> getActiveEffects() {
        return new ArrayList<>(effectQueue);
    }

    // ── Spawn ────────────────────────────────────────────────────
    private void trySpawn(Snake snake, Food food) {
        if (boardMap.size() >= 2) return; // Aynı anda max 2 güç-up

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
                    && !boardMap.containsKey(c)) return c;
        }
        return null;
    }

    // ── Temizlik ─────────────────────────────────────────────────
    public void clear() {
        boardMap.clear();
        effectQueue.clear();
    }

    public HashMap<Point, PowerUp> getBoardMap() { return boardMap; }
}