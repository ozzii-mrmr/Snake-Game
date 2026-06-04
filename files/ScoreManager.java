import java.io.*;

/**
 * ScoreManager.java
 *
 * VERİ YAPISI: MyTreeMap<Integer, String>  (java.util.TreeMap yerine)
 *   Anahtarlar (puanlar) otomatik sıralı tutulur → O(log n)
 *   En yüksek skor: lastKey() → O(log n)
 */
public class ScoreManager {

    public static class ScoreEntry {
        public final int    score;
        public final String name;
        ScoreEntry(int s, String n) { score = s; name = n; }
    }

    private static final int    MAX_ENTRIES = 5;
    private static final String SAVE_FILE   = "scores.txt";

    private MyTreeMap<Integer, String> scores = new MyTreeMap<>();

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

    public ScoreEntry[] getTopEntries() {
        int n = scores.size();
        ScoreEntry[] arr = new ScoreEntry[n];
        int i = n - 1;
        for (MyTreeMap.Entry<Integer, String> e : scores.entrySet()) {
            arr[i--] = new ScoreEntry(e.key, e.value);
        }
        return arr;
    }

    public int size() { return scores.size(); }

    private void save() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(SAVE_FILE))) {
            for (Integer key : scores.descendingKeySet()) {
                pw.println(key + ":" + scores.get(key).replace(":", "_"));
            }
        } catch (IOException ignored) {}
    }

    private void load() {
        java.io.File f = new java.io.File(SAVE_FILE);
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null && scores.size() < MAX_ENTRIES) {
                line = line.trim();
                if (line.isEmpty()) continue;
                int sep = line.indexOf(':');
                try {
                    if (sep > 0) scores.put(Integer.parseInt(line.substring(0, sep)), line.substring(sep+1));
                    else         scores.put(Integer.parseInt(line), "Oyuncu");
                } catch (NumberFormatException ignored) {}
            }
        } catch (IOException ignored) {}
    }
}