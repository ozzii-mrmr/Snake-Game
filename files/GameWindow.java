import javax.swing.*;

/**
 * GameWindow.java
 *
 * Oyunun ana penceresini oluşturur ve gösterir.
 * JFrame'i yapılandırıp GamePanel'i içine yerleştirir.
 */
public class GameWindow extends JFrame {

    public GameWindow() {
        GamePanel gamePanel = new GamePanel();

        setTitle("🐍 Yılan Oyunu — Veri Yapıları Projesi");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        add(gamePanel);
        pack(); // Pencereyi GamePanel'in tercih ettiği boyuta ayarla

        setLocationRelativeTo(null); // Ekran ortasında aç
        setVisible(true);

        // Klavye odağını game panel'e ver
        gamePanel.requestFocusInWindow();
    }
}
