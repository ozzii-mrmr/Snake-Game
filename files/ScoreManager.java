import java.io.*;
import java.util.TreeMap;
import java.util.Map;

/**
 * ScoreManager.java
 *
 * VERİ YAPISI: TreeMap<Integer, String>
 *   - Anahtar (key)  : puan → otomatik sıralı
 *   - Değer  (value) : oyuncu adı
 *
 * Dosya formatı:  puan:ad   (örn. 240:Ali)
 * Eski format (sadece sayı) de okunabilir.
 */
public class ScoreManager {

    public static class ScoreEntry {
        public final int    score;
        public final String name;
        ScoreEntry(int s, String n) { score = s; name = n; }
    }

    private static final int    MAX_ENTRIES = 5;
    private static final String SAVE_FILE   = "scores.txt";

    private TreeMap<Integer, String> scores = new TreeMap<>();

    public ScoreManager() { load(); }

    public void add(int score, String name) {
        if (score <= 0) return;
        String n = (name == null || name.trim().isEmpty()) ? "Oyuncu" : name.trim();
        scores.put(score, n);
        while (scores.size() > MAX_ENTRIES) scores.pollFirstEntry();
        save();
    }

    public int getHighScore() {
        return scores.isEmpty() ? 0 : scores.lastKey();
    }

    public boolean isTopScore(int score) {
        if (scores.size() < MAX_ENTRIES) return true;
        return score > scores.firstKey();
    }

    /** En yüksekten en düşüğe sıralı kayıtlar. */
    public ScoreEntry[] getTopEntries() {
        int n = scores.size();
        ScoreEntry[] arr = new ScoreEntry[n];
        int i = n - 1;
        for (Map.Entry<Integer, String> e : scores.entrySet()) {
            arr[i--] = new ScoreEntry(e.getKey(), e.getValue());
        }
        return arr;
    }

    public int size() { return scores.size(); }

    private void save() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(SAVE_FILE))) {
            for (int key : scores.descendingKeySet()) {
                pw.println(key + ":" + scores.get(key).replace(":", "_"));
            }
        } catch (IOException ignored) {}
    }

    private void load() {
        File f = new File(SAVE_FILE);
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null && scores.size() < MAX_ENTRIES) {
                line = line.trim();
                if (line.isEmpty()) continue;
                int sep = line.indexOf(':');
                try {
                    if (sep > 0) {
                        scores.put(Integer.parseInt(line.substring(0, sep)),
                                   line.substring(sep + 1));
                    } else {
                        scores.put(Integer.parseInt(line), "Oyuncu");
                    }
                } catch (NumberFormatException ignored) {}
            }
        } catch (IOException ignored) {}
    }
}
