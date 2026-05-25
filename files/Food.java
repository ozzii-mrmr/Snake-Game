import java.util.HashMap;
import java.util.Random;

/**
 * Food.java
 *
 * Oyundaki yiyecekleri yönetir.
 *
 * VERİ YAPISI: HashMap<Point, FoodType>
 * - Anahtar (key)  : Point  → Yiyeceğin tahtadaki konumu
 * - Değer  (value) : FoodType → Yiyeceğin türü (puan & renk bilgisi)
 *
 * Neden HashMap?
 *   "Bu hücrede yiyecek var mı?" sorusu O(1) sürede yanıtlanır.
 *   Array/List'te arama O(n) olurdu.
 *   Birden fazla yiyecek aynı anda bulunabilir (bonuslar için).
 */
public class Food {

    // ── İç sınıf: Yiyecek türleri ────────────────────────────────
    public enum FoodType {
        NORMAL (10, "Normal"),   // Yeşil elma
        BONUS  (30, "Bonus"),    // Altın yıldız (kısa süreliğine çıkar)
        SUPER  (50, "Süper");    // Mor elmas (çok nadir)

        public final int score;
        public final String label;

        FoodType(int score, String label) {
            this.score = score;
            this.label = label;
        }
    }

    // ── Alanlar ──────────────────────────────────────────────────
    private HashMap<Point, FoodType> foodMap;  // Konum → Tür
    private final int gridWidth;
    private final int gridHeight;
    private final Random random;
    private ObstacleManager obstacles;

    // Bonus yiyeceğin tahtada kalma süresi (tick sayısı)
    private int bonusTimer = 0;
    private static final int BONUS_DURATION = 100;

    // ── Kurucu ───────────────────────────────────────────────────
    public Food(int gridWidth, int gridHeight) {
        this.gridWidth  = gridWidth;
        this.gridHeight = gridHeight;
        this.foodMap    = new HashMap<>();
        this.random     = new Random();
    }

    // ── Yiyecek ekleme ───────────────────────────────────────────

    /**
     * Tahtada rastgele boş bir konuma normal yiyecek ekler.
     * @param snake Yılanın olduğu alanlardan kaçınmak için
     */
    public void spawnNormal(Snake snake) {
        Point pos = findEmptyCell(snake);
        if (pos != null) {
            foodMap.put(pos, FoodType.NORMAL); // HashMap.put → O(1)
        }
    }

    /**
     * Rastgele bir konuma bonus yiyecek ekler (zamanlayıcıyla).
     */
    public void spawnBonus(Snake snake) {
        Point pos = findEmptyCell(snake);
        if (pos != null) {
            foodMap.put(pos, FoodType.BONUS);
            bonusTimer = BONUS_DURATION;
        }
    }

    /**
     * Süper nadir yiyecek ekler.
     */
    public void spawnSuper(Snake snake) {
        Point pos = findEmptyCell(snake);
        if (pos != null) {
            foodMap.put(pos, FoodType.SUPER);
        }
    }

    // ── Yiyecek kontrolü ─────────────────────────────────────────

    /**
     * Verilen konumda yiyecek var mı? → O(1)
     * @return null yoksa, FoodType varsa
     */
    public FoodType getFoodAt(Point p) {
        return foodMap.get(p); // HashMap.get → O(1)
    }

    /**
     * Yiyeceği konumdan sil (yenildi).
     */
    public void removeFood(Point p) {
        foodMap.remove(p); // HashMap.remove → O(1)
    }

    /**
     * Her oyun adımında çağrılır.
     * Bonus yiyeceğin süresini yönetir.
     */
    public void tick(Snake snake, int step) {
        // Bonus yiyecek zamanlayıcısını azalt
        if (bonusTimer > 0) {
            bonusTimer--;
            if (bonusTimer == 0) {
                // Süresi dolmuş bonus yiyecekleri kaldır
                foodMap.values().remove(FoodType.BONUS);
            }
        }

        // Her 50 adımda bir bonus çıkarma şansı (%20)
        if (step % 50 == 0 && bonusTimer == 0 && random.nextInt(5) == 0) {
            spawnBonus(snake);
        }

        // Her 200 adımda bir süper yiyecek şansı (%10)
        if (step % 200 == 0 && random.nextInt(10) == 0) {
            spawnSuper(snake);
        }
    }

    // ── Yardımcı metodlar ────────────────────────────────────────

    /** Yılanın ve mevcut yiyeceklerin olmadığı rastgele bir hücre bulur. */
    private Point findEmptyCell(Snake snake) {
        int maxAttempts = 200;
        for (int i = 0; i < maxAttempts; i++) {
            int x = random.nextInt(gridWidth);
            int y = random.nextInt(gridHeight);
            Point candidate = new Point(x, y);

            // Yılanda yok ve başka yiyecek de yok
            if (!snake.contains(candidate) && !foodMap.containsKey(candidate)
                    && (obstacles == null || !obstacles.isObstacle(candidate))) {
                return candidate;
            }
        }
        return null; // Tahta dolu
    }

    /** Tüm yiyecekleri temizler (yeni oyun için). */
    public void clear() {
        foodMap.clear();
        bonusTimer = 0;
    }
    public void setObstacles(ObstacleManager obs) { this.obstacles = obs; }

    // ── Erişimciler ──────────────────────────────────────────────

    public HashMap<Point, FoodType> getFoodMap() { return foodMap; }
    public int getBonusTimer()                   { return bonusTimer; }
    public int getBonusDuration()                { return BONUS_DURATION; }
}
