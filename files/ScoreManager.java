import java.io.*;
import java.util.TreeMap;
import java.util.Map;

/**
 * ScoreManager.java
 *
 * VERİ YAPISI: TreeMap<Integer, String>
 *   - Anahtar (key)  : puan (Integer)  → TreeMap otomatik sıralar
 *   - Değer  (value) : etiket ("1. Oyun", "2. Oyun" …)
 *
 * Neden TreeMap?
 *   - Her put() sonrası liste anahtara göre sıralı kalır → O(log n)
 *   - En yüksek skora erişim: lastKey() → O(log n)
 *   - HashMap ile saklasaydık skor tablosunu her seferinde
 *     kendimiz sıralamamız gerekirdi → O(n log n)
 *
 * Dosya formatı (scores.txt):
 *   120
 *   95
 *   80
 *   ...  (her satır bir puan, büyükten küçüğe)
 */
public class ScoreManager {

    private static final int   MAX_ENTRIES  = 5;
    private static final String SAVE_FILE   = "scores.txt";

    /**
     * TreeMap<Integer, String>
     * Puanlar küçükten büyüğe sıralı tutulur.
     * En yüksek = lastKey(), en düşük = firstKey().
     *
     * NOT: Aynı puan iki kez girilirse üzerine yazar (key unique).
     * Bu yüzden value'ya sıra numarası yerine oturum etiketi koyuyoruz.
     */
    private TreeMap<Integer, String> scores;
    private int sessionCount = 0;

    // ── Kurucu ───────────────────────────────────────────────────
    public ScoreManager() {
        scores = new TreeMap<>();
        load();
    }

    // ── Puan ekleme ──────────────────────────────────────────────

    /**
     * Yeni skoru ekler.
     * TreeMap sıralı tuttuğu için ekstra sort() gerekmez.
     * MAX_ENTRIES aşılırsa en düşük skor otomatik silinir.
     */
    public void add(int score) {
        if (score <= 0) return;
        sessionCount++;
        scores.put(score, "Oyun #" + sessionCount);

        // Kapasite aşıldıysa en küçük skoru çıkar
        while (scores.size() > MAX_ENTRIES) {
            scores.pollFirstEntry(); // en küçük → O(log n)
        }
        save();
    }

    /** En yüksek skoru döndürür. */
    public int getHighScore() {
        return scores.isEmpty() ? 0 : scores.lastKey(); // O(log n)
    }

    /** Skoru skor tablosuna girecek mi? */
    public boolean isTopScore(int score) {
        if (scores.size() < MAX_ENTRIES) return true;
        return score > scores.firstKey();
    }

    /** Sıralı skor listesi — en yüksekten en düşüğe */
    public int[] getTopScores() {
        int[] arr = new int[scores.size()];
        int i = scores.size() - 1;
        for (int key : scores.keySet()) {
            arr[i--] = key;
        }
        return arr;
    }

    public int size() { return scores.size(); }

    // ── Dosya I/O ────────────────────────────────────────────────

    private void save() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(SAVE_FILE))) {
            // Büyükten küçüğe yaz (descendingKeySet)
            for (int key : scores.descendingKeySet()) {
                pw.println(key);
            }
        } catch (IOException ignored) {}
    }

    private void load() {
        File f = new File(SAVE_FILE);
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            int idx = 0;
            while ((line = br.readLine()) != null && idx < MAX_ENTRIES) {
                try {
                    int val = Integer.parseInt(line.trim());
                    scores.put(val, "Kayıtlı");
                    idx++;
                } catch (NumberFormatException ignored) {}
            }
        } catch (IOException ignored) {}
    }
}
