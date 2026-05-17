import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

/**
 * GamePanel.java — Neon Cyberpunk Teması
 *
 * Görsel özellikler:
 *  - Koyu gradient arka plan + nokta ızgarası
 *  - Glow (parıltı) efektli yılan ve yiyecekler
 *  - Gradient yılan gövdesi (baş parlak → kuyruk koyu)
 *  - Bağlantılı yılan segmentleri (boşluksuz)
 *  - Animasyonlu yiyecekler (sinüs dalgası ile nabız)
 *  - Floating score popup (+10/+30/+50)
 *  - Şık HUD paneli
 *  - Seviye (hız) sistemi
 */
public class GamePanel extends JPanel implements ActionListener, KeyListener {

    // ── Izgara & piksel ───────────────────────────────────────────
    public static final int CELL_SIZE   = 24;
    public static final int GRID_WIDTH  = 25;
    public static final int GRID_HEIGHT = 25;
    public static final int PANEL_W     = CELL_SIZE * GRID_WIDTH;
    public static final int PANEL_H     = CELL_SIZE * GRID_HEIGHT;

    // ── Renk paleti — Neon Cyberpunk ─────────────────────────────
    private static final Color BG_DARK        = new Color(8,  10, 24);
    private static final Color BG_LIGHT       = new Color(12, 16, 36);
    private static final Color GRID_DOT       = new Color(30, 35, 65, 180);
    private static final Color BORDER_COLOR   = new Color(0, 220, 130, 60);

    private static final Color SNAKE_HEAD_CLR = new Color(0, 255, 140);
    private static final Color SNAKE_MID_CLR  = new Color(0, 200, 100);
    private static final Color SNAKE_TAIL_CLR = new Color(0, 100, 55);
    private static final Color SNAKE_GLOW_CLR = new Color(0, 255, 140, 50);

    private static final Color FOOD_NORMAL_CLR = new Color(255, 65,  90);
    private static final Color FOOD_BONUS_CLR  = new Color(255, 210,  0);
    private static final Color FOOD_SUPER_CLR  = new Color(170,  60, 255);

    private static final Color HUD_BG         = new Color(8, 10, 24, 210);
    private static final Color HUD_BORDER     = new Color(0, 180, 100, 80);
    private static final Color TEXT_PRIMARY   = new Color(220, 235, 255);
    private static final Color TEXT_MUTED     = new Color(120, 140, 180);
    private static final Color ACCENT_CYAN    = new Color(0, 200, 255);

    // ── Hız (seviye) ─────────────────────────────────────────────
    private static final int BASE_SPEED = 130;
    private int currentSpeed = BASE_SPEED;

    // ── Oyun nesneleri ────────────────────────────────────────────
    private Snake snake;
    private Food  food;
    private javax.swing.Timer timer;

    // ── Durum ─────────────────────────────────────────────────────
    private enum State { MENU, RUNNING, PAUSED, GAME_OVER }
    private State state = State.MENU;

    private int score     = 0;
    private int highScore = 0;
    private int steps     = 0;
    private int level     = 1;

    // ── Animasyon tick ────────────────────────────────────────────
    private int animTick = 0;
    private javax.swing.Timer animTimer;

    // ── Score popup sistemi ───────────────────────────────────────
    private static class ScorePopup {
        String text;
        float x, y, alpha = 1.0f;
        Color color;
        ScorePopup(String t, float x, float y, Color c) {
            text = t; this.x = x; this.y = y; color = c;
        }
        boolean tick() { y -= 0.8f; alpha -= 0.022f; return alpha > 0; }
    }
    private List<ScorePopup> popups = new ArrayList<>();

    // ── Kurucu ───────────────────────────────────────────────────
    public GamePanel() {
        setPreferredSize(new Dimension(PANEL_W, PANEL_H));
        setBackground(BG_DARK);
        setFocusable(true);
        addKeyListener(this);

        timer = new javax.swing.Timer(currentSpeed, this);

        animTimer = new javax.swing.Timer(30, e -> {
            animTick++;
            if (state != State.RUNNING) repaint();
        });
        animTimer.start();

        initGame();
    }

    // ── Oyun başlatma ─────────────────────────────────────────────
    private void initGame() {
        snake = new Snake(GRID_WIDTH / 2, GRID_HEIGHT / 2);
        food  = new Food(GRID_WIDTH, GRID_HEIGHT);
        food.spawnNormal(snake);
        score = 0; steps = 0; level = 1;
        currentSpeed = BASE_SPEED;
        popups.clear();
        timer.setDelay(currentSpeed);
    }

    // ── Oyun döngüsü ──────────────────────────────────────────────
    @Override
    public void actionPerformed(ActionEvent e) {
        if (state != State.RUNNING) return;
        update();
        repaint();
    }

    private void update() {
        steps++;
        animTick++;

        Point nextHead = peekNextHead();
        Food.FoodType eaten = food.getFoodAt(nextHead);
        boolean grow = (eaten != null);

        Point newHead = snake.move(grow);

        if (newHead.x < 0 || newHead.x >= GRID_WIDTH ||
                newHead.y < 0 || newHead.y >= GRID_HEIGHT) { endGame(); return; }
        if (snake.collidesWithSelf()) { endGame(); return; }

        if (eaten != null) {
            score += eaten.score;
            Color pc = eaten == Food.FoodType.NORMAL ? FOOD_NORMAL_CLR
                    : eaten == Food.FoodType.BONUS  ? FOOD_BONUS_CLR : FOOD_SUPER_CLR;
            popups.add(new ScorePopup("+" + eaten.score,
                    newHead.x * CELL_SIZE + CELL_SIZE / 2f,
                    newHead.y * CELL_SIZE - 2f, pc));
            food.removeFood(newHead);
            food.spawnNormal(snake);
            updateLevel();
        }

        food.tick(snake, steps);
        popups.removeIf(p -> !p.tick());
    }

    private void updateLevel() {
        int newLevel = score / 100 + 1;
        if (newLevel > level && newLevel <= 10) {
            level = newLevel;
            currentSpeed = Math.max(60, BASE_SPEED - (level - 1) * 10);
            timer.setDelay(currentSpeed);
        }
    }

    private Point peekNextHead() {
        Point head = snake.getHead();
        switch (snake.getNextDirection()) {
            case Snake.UP:   return new Point(head.x,     head.y - 1);
            case Snake.DOWN: return new Point(head.x,     head.y + 1);
            case Snake.LEFT: return new Point(head.x - 1, head.y);
            default:         return new Point(head.x + 1, head.y);
        }
    }

    private void endGame() {
        state = State.GAME_OVER;
        timer.stop();
        if (score > highScore) highScore = score;
    }

    // ══════════════════════════════════════════════════════════════
    //  ÇİZİM
    // ══════════════════════════════════════════════════════════════

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,         RenderingHints.VALUE_RENDER_QUALITY);

        drawBackground(g2);

        if (state == State.MENU) { drawMenu(g2); return; }

        drawFood(g2);
        drawSnake(g2);
        drawPopups(g2);
        drawHUD(g2);
        drawBorder(g2);

        if (state == State.PAUSED)    drawPauseOverlay(g2);
        if (state == State.GAME_OVER) drawGameOverOverlay(g2);
    }

    // ── Arka plan ─────────────────────────────────────────────────
    private void drawBackground(Graphics2D g2) {
        g2.setPaint(new GradientPaint(0, 0, BG_LIGHT, PANEL_W, PANEL_H, BG_DARK));
        g2.fillRect(0, 0, PANEL_W, PANEL_H);
        g2.setColor(GRID_DOT);
        for (int x = 0; x <= GRID_WIDTH; x++)
            for (int y = 0; y <= GRID_HEIGHT; y++)
                g2.fillRect(x * CELL_SIZE - 1, y * CELL_SIZE - 1, 2, 2);
    }

    // ── Çerçeve ───────────────────────────────────────────────────
    private void drawBorder(Graphics2D g2) {
        g2.setStroke(new BasicStroke(1.5f));
        g2.setColor(BORDER_COLOR);
        g2.drawRect(0, 0, PANEL_W - 1, PANEL_H - 1);
        int c = 14;
        g2.setColor(SNAKE_HEAD_CLR);
        g2.setStroke(new BasicStroke(2.5f));
        // Köşe vurguları
        int W = PANEL_W - 1, H = PANEL_H - 1;
        g2.drawLine(0, 0, c, 0);     g2.drawLine(0, 0, 0, c);
        g2.drawLine(W - c, 0, W, 0); g2.drawLine(W, 0, W, c);
        g2.drawLine(0, H - c, 0, H); g2.drawLine(0, H, c, H);
        g2.drawLine(W - c, H, W, H); g2.drawLine(W, H - c, W, H);
    }

    // ── Yılan ─────────────────────────────────────────────────────
    private void drawSnake(Graphics2D g2) {
        java.util.LinkedList<Point> body = snake.getBody();
        int n = body.size();

        // 1. Glow katmanı
        Composite orig = g2.getComposite();
        for (Point p : body) {
            int cx = p.x * CELL_SIZE + CELL_SIZE / 2;
            int cy = p.y * CELL_SIZE + CELL_SIZE / 2;
            for (int r = 3; r >= 1; r--) {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.04f * r));
                g2.setColor(SNAKE_GLOW_CLR);
                int rad = CELL_SIZE / 2 + r * 4;
                g2.fillOval(cx - rad, cy - rad, rad * 2, rad * 2);
            }
        }
        g2.setComposite(orig);

        // 2. Segment bağlantıları
        for (int i = 0; i < n - 1; i++) {
            Point curr = body.get(i);
            Point next = body.get(i + 1);
            float t = (float) i / n;
            g2.setColor(blendColor(SNAKE_MID_CLR, SNAKE_TAIL_CLR, t));
            int m = 3, sz = CELL_SIZE - m * 2;
            if (curr.x == next.x) {
                int topY = Math.min(curr.y, next.y) * CELL_SIZE + CELL_SIZE - m;
                g2.fillRect(curr.x * CELL_SIZE + m, topY, sz, m * 2);
            } else {
                int leftX = Math.min(curr.x, next.x) * CELL_SIZE + CELL_SIZE - m;
                g2.fillRect(leftX, curr.y * CELL_SIZE + m, m * 2, sz);
            }
        }

        // 3. Segmentler (kuyruktan başa)
        for (int i = n - 1; i >= 0; i--) {
            Point p = body.get(i);
            float t = (float) i / n;
            int m = 3, px = p.x * CELL_SIZE + m, py = p.y * CELL_SIZE + m, sz = CELL_SIZE - m * 2;

            if (i == 0) {
                // Baş glow
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
                g2.setColor(SNAKE_HEAD_CLR);
                g2.fill(new RoundRectangle2D.Float(px - 2, py - 2, sz + 4, sz + 4, 14, 14));
                g2.setComposite(orig);
                // Baş gradient
                g2.setPaint(new GradientPaint(px, py, SNAKE_HEAD_CLR, px + sz, py + sz, SNAKE_MID_CLR));
                g2.fill(new RoundRectangle2D.Float(px, py, sz, sz, 10, 10));
                g2.setColor(SNAKE_HEAD_CLR.brighter());
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(px, py, sz, sz, 10, 10));
                drawSnakeEyes(g2, p);

            } else if (i == n - 1) {
                // Kuyruk
                g2.setColor(blendColor(SNAKE_MID_CLR, SNAKE_TAIL_CLR, 0.85f));
                g2.fill(new RoundRectangle2D.Float(px + 1, py + 1, sz - 2, sz - 2, 8, 8));

            } else {
                // Gövde
                Color c = blendColor(SNAKE_MID_CLR, SNAKE_TAIL_CLR, t * 0.7f);
                g2.setColor(c);
                g2.fill(new RoundRectangle2D.Float(px, py, sz, sz, 7, 7));
                g2.setColor(c.brighter());
                g2.setStroke(new BasicStroke(0.8f));
                g2.drawLine(px + 3, py + 1, px + sz - 3, py + 1);
            }
        }
        g2.setComposite(orig);
    }

    private void drawSnakeEyes(Graphics2D g2, Point head) {
        int bx = head.x * CELL_SIZE, by = head.y * CELL_SIZE;
        int dir = snake.getDirection();
        int[] e1, e2;
        switch (dir) {
            case Snake.RIGHT: e1 = new int[]{bx+16,by+6};  e2 = new int[]{bx+16,by+15}; break;
            case Snake.LEFT:  e1 = new int[]{bx+6, by+6};  e2 = new int[]{bx+6, by+15}; break;
            case Snake.UP:    e1 = new int[]{bx+6, by+6};  e2 = new int[]{bx+15,by+6};  break;
            default:          e1 = new int[]{bx+6, by+16}; e2 = new int[]{bx+15,by+16}; break;
        }
        Composite orig = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        for (int[] e : new int[][]{e1, e2}) {
            g2.setColor(Color.WHITE);      g2.fillOval(e[0]-3, e[1]-3, 6, 6);
            g2.setColor(new Color(5,5,20)); g2.fillOval(e[0]-1, e[1]-1, 3, 3);
            g2.setColor(Color.WHITE);       g2.fillRect(e[0],   e[1]-1, 1, 1);
        }
        g2.setComposite(orig);
    }

    // ── Yiyecekler ────────────────────────────────────────────────
    private void drawFood(Graphics2D g2) {
        double pulse = Math.sin(animTick * 0.12) * 0.5 + 0.5;
        for (Map.Entry<Point, Food.FoodType> entry : food.getFoodMap().entrySet()) {
            Point p = entry.getKey();
            int cx = p.x * CELL_SIZE + CELL_SIZE / 2;
            int cy = p.y * CELL_SIZE + CELL_SIZE / 2;
            switch (entry.getValue()) {
                case NORMAL: drawFoodNormal(g2, cx, cy, pulse);      break;
                case BONUS:  drawFoodBonus (g2, cx, cy, pulse, p);   break;
                case SUPER:  drawFoodSuper (g2, cx, cy, pulse);       break;
            }
        }
    }

    private void drawFoodNormal(Graphics2D g2, int cx, int cy, double pulse) {
        int r = (int)(7 + pulse * 2);
        drawGlow(g2, cx, cy, FOOD_NORMAL_CLR, r + 5, 3);
        g2.setColor(FOOD_NORMAL_CLR);
        g2.fillOval(cx - r, cy - r, r * 2, r * 2);
        g2.setColor(new Color(255, 180, 180, 200));
        g2.fillOval(cx - r/2, cy - r/2, r/2, r/2);
        g2.setColor(new Color(80, 200, 80));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(cx + 1, cy - r, cx + 3, cy - r - 4);
    }

    private void drawFoodBonus(Graphics2D g2, int cx, int cy, double pulse, Point p) {
        int r = (int)(8 + pulse * 2);
        drawGlow(g2, cx, cy, FOOD_BONUS_CLR, r + 6, 4);
        Graphics2D gs = (Graphics2D) g2.create();
        gs.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gs.translate(cx, cy);
        gs.rotate(animTick * 0.06);
        gs.setColor(FOOD_BONUS_CLR);
        fillStar(gs, 0, 0, r, (int)(r * 0.45));
        gs.setColor(new Color(255, 255, 200, 220));
        gs.fillOval(-3, -3, 6, 6);
        gs.dispose();
        if (food.getBonusTimer() > 0) {
            float ratio = (float) food.getBonusTimer() / food.getBonusDuration();
            int bw = CELL_SIZE - 4, bx2 = p.x * CELL_SIZE + 2, by2 = p.y * CELL_SIZE + CELL_SIZE - 5;
            g2.setColor(new Color(60, 60, 60, 160));
            g2.fillRoundRect(bx2, by2, bw, 3, 3, 3);
            g2.setColor(ratio > 0.4f ? FOOD_BONUS_CLR : FOOD_NORMAL_CLR);
            g2.fillRoundRect(bx2, by2, (int)(bw * ratio), 3, 3, 3);
        }
    }

    private void drawFoodSuper(Graphics2D g2, int cx, int cy, double pulse) {
        int r = (int)(8 + pulse * 3);
        drawGlow(g2, cx, cy, FOOD_SUPER_CLR, r + 7, 5);
        Graphics2D gd = (Graphics2D) g2.create();
        gd.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gd.translate(cx, cy);
        gd.rotate(animTick * 0.04);
        gd.setColor(new Color(200, 100, 255, 100));
        int rd = r + 3;
        gd.fillPolygon(new int[]{0, rd, 0, -rd}, new int[]{-rd, 0, rd, 0}, 4);
        gd.setPaint(new GradientPaint(-r, -r, new Color(220, 150, 255), r, r, FOOD_SUPER_CLR));
        gd.fillPolygon(new int[]{0, r, 0, -r}, new int[]{-r, 0, r, 0}, 4);
        gd.setColor(new Color(240, 200, 255, 220));
        gd.fillPolygon(new int[]{0, r/2, 0, -r/2}, new int[]{-r/2, 0, 0, 0}, 4);
        gd.dispose();
    }

    private void drawGlow(Graphics2D g2, int cx, int cy, Color c, int radius, int layers) {
        Composite orig = g2.getComposite();
        for (int i = layers; i >= 1; i--) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.06f * i));
            g2.setColor(c);
            int r = radius + (layers - i) * 3;
            g2.fillOval(cx - r, cy - r, r * 2, r * 2);
        }
        g2.setComposite(orig);
    }

    private void fillStar(Graphics2D g2, int cx, int cy, int outer, int inner) {
        int pts = 5;
        int[] xs = new int[pts * 2], ys = new int[pts * 2];
        for (int i = 0; i < pts * 2; i++) {
            double angle = Math.PI / pts * i - Math.PI / 2;
            int r = (i % 2 == 0) ? outer : inner;
            xs[i] = (int)(cx + r * Math.cos(angle));
            ys[i] = (int)(cy + r * Math.sin(angle));
        }
        g2.fillPolygon(xs, ys, pts * 2);
    }

    // ── Score popups ──────────────────────────────────────────────
    private void drawPopups(Graphics2D g2) {
        Composite orig = g2.getComposite();
        g2.setFont(new Font("Monospaced", Font.BOLD, 15));
        for (ScorePopup pop : popups) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, pop.alpha));
            g2.setColor(new Color(0, 0, 0, 160));
            g2.drawString(pop.text, (int) pop.x - 13, (int) pop.y + 1);
            g2.setColor(pop.color);
            g2.drawString(pop.text, (int) pop.x - 14, (int) pop.y);
        }
        g2.setComposite(orig);
    }

    // ── HUD ───────────────────────────────────────────────────────
    private void drawHUD(Graphics2D g2) {
        int H = 32, pad = 10;
        g2.setColor(HUD_BG);
        g2.fillRoundRect(pad, pad, PANEL_W - pad * 2, H, 10, 10);
        g2.setColor(HUD_BORDER);
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(pad, pad, PANEL_W - pad * 2, H, 10, 10);

        g2.setFont(new Font("Monospaced", Font.PLAIN, 11));
        g2.setColor(TEXT_MUTED);
        g2.drawString("PUAN", 22, 21);
        g2.setFont(new Font("Monospaced", Font.BOLD, 14));
        g2.setColor(SNAKE_HEAD_CLR);
        g2.drawString(String.valueOf(score), 60, 22);

        g2.setFont(new Font("Monospaced", Font.PLAIN, 11));
        g2.setColor(TEXT_MUTED);
        g2.drawString("EN YÜKSEK", PANEL_W / 2 - 38, 21);
        g2.setFont(new Font("Monospaced", Font.BOLD, 14));
        g2.setColor(FOOD_BONUS_CLR);
        g2.drawString(String.valueOf(highScore), PANEL_W / 2 + 40, 22);

        g2.setFont(new Font("Monospaced", Font.PLAIN, 11));
        g2.setColor(TEXT_MUTED);
        g2.drawString("LVL", PANEL_W - 90, 21);
        g2.setFont(new Font("Monospaced", Font.BOLD, 14));
        g2.setColor(ACCENT_CYAN);
        g2.drawString(String.valueOf(level), PANEL_W - 62, 22);

        g2.setColor(TEXT_MUTED);
        g2.setFont(new Font("Monospaced", Font.PLAIN, 11));
        g2.drawString("×" + snake.getLength(), PANEL_W - 45, 22);
    }

    // ── MENÜ ──────────────────────────────────────────────────────
    private void drawMenu(Graphics2D g2) {
        // Tarama çizgisi efekti
        Composite orig = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.03f));
        g2.setColor(Color.WHITE);
        for (int y = 0; y < PANEL_H; y += 4) g2.drawLine(0, y, PANEL_W, y);
        g2.setComposite(orig);

        drawGlow(g2, PANEL_W / 2, PANEL_H / 2 - 80, SNAKE_HEAD_CLR, 80, 5);

        g2.setFont(new Font("Monospaced", Font.BOLD, 48));
        g2.setColor(SNAKE_HEAD_CLR);
        drawCentered(g2, "SNAKE", PANEL_H / 2 - 68);
        g2.setFont(new Font("Monospaced", Font.PLAIN, 11));
        g2.setColor(new Color(0, 180, 100, 180));
        drawCentered(g2, "N E O N   E D I T I O N", PANEL_H / 2 - 48);

        g2.setColor(new Color(0, 200, 100, 70));
        g2.setStroke(new BasicStroke(1f));
        g2.drawLine(PANEL_W / 2 - 110, PANEL_H / 2 - 37, PANEL_W / 2 + 110, PANEL_H / 2 - 37);

        g2.setFont(new Font("Monospaced", Font.PLAIN, 13));
        g2.setColor(TEXT_MUTED);
        drawCentered(g2, "WASD  /  OK TUŞLARI     Hareket", PANEL_H / 2 - 15);
        drawCentered(g2, "P                       Duraklat", PANEL_H / 2 + 8);

        // Yiyecek efsanesi
        int[] xs = {PANEL_W/2 - 115, PANEL_W/2 - 28, PANEL_W/2 + 58};
        Color[] fc = {FOOD_NORMAL_CLR, FOOD_BONUS_CLR, FOOD_SUPER_CLR};
        String[] fl = {"Normal +10", "Bonus +30", "Süper +50"};
        g2.setFont(new Font("Monospaced", Font.PLAIN, 11));
        for (int i = 0; i < 3; i++) {
            g2.setColor(fc[i]);  g2.fillOval(xs[i], PANEL_H/2 + 30, 8, 8);
            g2.setColor(TEXT_MUTED); g2.drawString(fl[i], xs[i] + 12, PANEL_H/2 + 39);
        }

        // Nabız butonu
        double p = Math.sin(animTick * 0.08) * 0.5 + 0.5;
        Color btnC = blendColor(new Color(0, 130, 70), SNAKE_HEAD_CLR, (float) p);
        String btn = "[ SPACE / ENTER  —  Başla ]";
        g2.setFont(new Font("Monospaced", Font.BOLD, 15));
        FontMetrics fm = g2.getFontMetrics();
        int bx = (PANEL_W - fm.stringWidth(btn)) / 2;
        int by2 = PANEL_H / 2 + 80;
        g2.setColor(new Color(0, 80, 40, 80));
        g2.fillRoundRect(bx - 10, by2 - 16, fm.stringWidth(btn) + 20, 24, 8, 8);
        g2.setColor(new Color(0, 180, 90, 90));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(bx - 10, by2 - 16, fm.stringWidth(btn) + 20, 24, 8, 8);
        g2.setColor(btnC);
        g2.drawString(btn, bx, by2);

        drawBorder(g2);
    }

    // ── Duraklat overlay ─────────────────────────────────────────
    private void drawPauseOverlay(Graphics2D g2) {
        Composite orig = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f));
        g2.setColor(BG_DARK);
        g2.fillRect(0, 0, PANEL_W, PANEL_H);
        g2.setComposite(orig);
        drawGlow(g2, PANEL_W/2, PANEL_H/2-20, ACCENT_CYAN, 60, 4);
        g2.setFont(new Font("Monospaced", Font.BOLD, 28));
        g2.setColor(ACCENT_CYAN);
        drawCentered(g2, "DURAKLATILDI", PANEL_H / 2 - 10);
        g2.setFont(new Font("Monospaced", Font.PLAIN, 13));
        g2.setColor(TEXT_MUTED);
        drawCentered(g2, "Devam etmek için  P", PANEL_H / 2 + 22);
    }

    // ── Oyun bitti overlay ───────────────────────────────────────
    private void drawGameOverOverlay(Graphics2D g2) {
        Composite orig = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.82f));
        g2.setColor(BG_DARK);
        g2.fillRect(0, 0, PANEL_W, PANEL_H);
        g2.setComposite(orig);

        drawGlow(g2, PANEL_W/2, PANEL_H/2-55, FOOD_NORMAL_CLR, 90, 5);

        g2.setFont(new Font("Monospaced", Font.BOLD, 32));
        g2.setColor(FOOD_NORMAL_CLR);
        drawCentered(g2, "GAME OVER", PANEL_H / 2 - 45);

        // İstatistik kartı
        int cw = 240, ch = 90, cx2 = PANEL_W/2 - 120, cy2 = PANEL_H/2 - 25;
        g2.setColor(new Color(18, 20, 45, 230));
        g2.fillRoundRect(cx2, cy2, cw, ch, 12, 12);
        g2.setColor(new Color(FOOD_NORMAL_CLR.getRed(), 40, 40, 90));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(cx2, cy2, cw, ch, 12, 12);

        g2.setFont(new Font("Monospaced", Font.PLAIN, 12));
        int[] ys2 = {cy2+26, cy2+48, cy2+70};
        String[] labels = {"Puan", "En Yüksek", "Uzunluk"};
        for (int i = 0; i < 3; i++) {
            g2.setColor(TEXT_MUTED);
            g2.drawString(labels[i], cx2 + 18, ys2[i]);
        }
        g2.setFont(new Font("Monospaced", Font.BOLD, 15));
        g2.setColor(TEXT_PRIMARY);
        drawRight(g2, String.valueOf(score),          cx2+cw-18, ys2[0]);
        g2.setColor(FOOD_BONUS_CLR);
        drawRight(g2, String.valueOf(highScore),      cx2+cw-18, ys2[1]);
        g2.setColor(SNAKE_HEAD_CLR);
        drawRight(g2, String.valueOf(snake.getLength()), cx2+cw-18, ys2[2]);

        g2.setFont(new Font("Monospaced", Font.BOLD, 13));
        g2.setColor(SNAKE_HEAD_CLR);
        drawCentered(g2, "[ SPACE  —  Yeniden Oyna ]", PANEL_H/2 + 80);
        g2.setColor(TEXT_MUTED);
        drawCentered(g2, "[ ESC  —  Ana Menü ]",       PANEL_H/2 + 102);
    }

    // ── Yardımcı metodlar ─────────────────────────────────────────
    private void drawCentered(Graphics2D g2, String s, int y) {
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(s, (PANEL_W - fm.stringWidth(s)) / 2, y);
    }
    private void drawRight(Graphics2D g2, String s, int rx, int y) {
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(s, rx - fm.stringWidth(s), y);
    }
    private Color blendColor(Color a, Color b, float t) {
        t = Math.max(0, Math.min(1, t));
        return new Color(
                (int)(a.getRed()   + t * (b.getRed()   - a.getRed())),
                (int)(a.getGreen() + t * (b.getGreen() - a.getGreen())),
                (int)(a.getBlue()  + t * (b.getBlue()  - a.getBlue()))
        );
    }

    // ── Klavye ───────────────────────────────────────────────────
    @Override
    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        switch (state) {
            case MENU:
                if (k == KeyEvent.VK_SPACE || k == KeyEvent.VK_ENTER) {
                    state = State.RUNNING; timer.start();
                }
                break;
            case RUNNING:
                if      (k==KeyEvent.VK_UP    || k==KeyEvent.VK_W) snake.setDirection(Snake.UP);
                else if (k==KeyEvent.VK_DOWN  || k==KeyEvent.VK_S) snake.setDirection(Snake.DOWN);
                else if (k==KeyEvent.VK_LEFT  || k==KeyEvent.VK_A) snake.setDirection(Snake.LEFT);
                else if (k==KeyEvent.VK_RIGHT || k==KeyEvent.VK_D) snake.setDirection(Snake.RIGHT);
                else if (k==KeyEvent.VK_P) { state=State.PAUSED; timer.stop(); repaint(); }
                break;
            case PAUSED:
                if (k == KeyEvent.VK_P) { state = State.RUNNING; timer.start(); }
                break;
            case GAME_OVER:
                if (k == KeyEvent.VK_SPACE) {
                    initGame(); state = State.RUNNING; timer.start();
                } else if (k == KeyEvent.VK_ESCAPE) {
                    initGame(); state = State.MENU; repaint();
                }
                break;
        }
    }
    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e)    {}
}