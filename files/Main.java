/**
 * Main.java
 *
 * Uygulamanın başlangıç noktası.
 * SwingUtilities.invokeLater ile UI thread'inde pencereyi açar.
 */
public class Main {
    public static void main(String[] args) {
        // Swing uygulamaları EDT (Event Dispatch Thread) üzerinde çalışmalıdır
        javax.swing.SwingUtilities.invokeLater(() -> {
            new GameWindow();
        });
    }
}
