package game;

import java.util.Random;
import model.Point;
import datastructures.MyHashMap;

/**
 * Food.java
 *
 * VERİ YAPISI: MyHashMap<Point, FoodType>  (java.util.HashMap yerine)
 *   "Bu hücrede yiyecek var mı?" → O(1)
 */
public class Food {

    public enum FoodType {
        NORMAL(10, "Normal"),
        BONUS (30, "Bonus"),
        SUPER (50, "Süper");

        public final int    score;
        public final String label;

        FoodType(int score, String label) {
            this.score = score;
            this.label = label;
        }
    }

    private MyHashMap<Point, FoodType> foodMap;
    private final int     gridWidth;
    private final int     gridHeight;
    private final Random  random;
    private ObstacleManager obstacles;

    private int bonusTimer = 0;
    private static final int BONUS_DURATION = 100;

    public Food(int gridWidth, int gridHeight) {
        this.gridWidth  = gridWidth;
        this.gridHeight = gridHeight;
        this.foodMap    = new MyHashMap<>();
        this.random     = new Random();
    }

    public void spawnNormal(Snake snake) {
        Point pos = findEmptyCell(snake);
        if (pos != null) foodMap.put(pos, FoodType.NORMAL);
    }

    public void spawnBonus(Snake snake) {
        Point pos = findEmptyCell(snake);
        if (pos != null) {
            foodMap.put(pos, FoodType.BONUS);
            bonusTimer = BONUS_DURATION;
        }
    }

    public void spawnSuper(Snake snake) {
        Point pos = findEmptyCell(snake);
        if (pos != null) foodMap.put(pos, FoodType.SUPER);
    }

    public FoodType getFoodAt(Point p) {
        return foodMap.get(p);
    }

    public void removeFood(Point p) {
        foodMap.remove(p);
    }

    public void tick(Snake snake, int step) {
        if (bonusTimer > 0) {
            bonusTimer--;
            if (bonusTimer == 0) {
                foodMap.removeValue(FoodType.BONUS); // ilk BONUS kaydını sil
            }
        }

        if (step % 50 == 0 && bonusTimer == 0 && random.nextInt(5) == 0) {
            spawnBonus(snake);
        }

        if (step % 200 == 0 && random.nextInt(10) == 0) {
            spawnSuper(snake);
        }
    }

    private Point findEmptyCell(Snake snake) {
        for (int i = 0; i < 200; i++) {
            int x = random.nextInt(gridWidth);
            int y = random.nextInt(gridHeight);
            Point candidate = new Point(x, y);
            if (!snake.contains(candidate) && !foodMap.containsKey(candidate)
                    && (obstacles == null || !obstacles.isObstacle(candidate))) {
                return candidate;
            }
        }
        return null;
    }

    public void clear() {
        foodMap.clear();
        bonusTimer = 0;
    }

    public void setObstacles(ObstacleManager obs) { this.obstacles = obs; }

    public MyHashMap<Point, FoodType> getFoodMap()  { return foodMap; }
    public int getBonusTimer()                      { return bonusTimer; }
    public int getBonusDuration()                   { return BONUS_DURATION; }
}