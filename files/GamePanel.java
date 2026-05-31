import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class GamePanel extends JPanel implements ActionListener, KeyListener, MouseListener, MouseMotionListener {

    // ── Boyutlar ─────────────────────────────────────────────────
    static final int CELL    = 26;
    static final int COLS    = 24;
    static final int ROWS    = 24;
    static final int GRID_W  = CELL * COLS;
    static final int GRID_H  = CELL * ROWS;
    static final int HUD_H   = 64;
    static final int PANEL_W = GRID_W;
    static final int PANEL_H = GRID_H + HUD_H;

    // ── Renkler ──────────────────────────────────────────────────
    private static final Color C_BG         = new Color(13,  15,  23);
    private static final Color C_GRID       = new Color(255, 255, 255,  9);
    private static final Color C_HUD_BG     = new Color(10,  12,  20);
    private static final Color C_HUD_DIV    = new Color(255, 255, 255, 18);
    private static final Color C_TEXT       = new Color(241, 245, 249);
    private static final Color C_TEXT_MUTED = new Color(100, 116, 139);
    private static final Color C_ACCENT     = new Color(52,  211, 153);
    private static final Color C_DANGER     = new Color(248, 113, 113);
    private static final Color C_WARN       = new Color(251, 191,  36);
    private static final Color C_INDIGO     = new Color(129, 140, 248);

    private static final Color C_SNAKE_HEAD = new Color(52,  211, 153);
    private static final Color C_SNAKE_BODY = new Color(16,  185, 129);
    private static final Color C_SNAKE_TAIL = new Color(6,    95,  70);
    private static final Color C_SNAKE_GLOW = new Color(52,  211, 153, 40);

    private static final Color C_FOOD_N     = new Color(251, 113, 133);
    private static final Color C_FOOD_B     = new Color(251, 191,  36);
    private static final Color C_FOOD_S     = new Color(167, 139, 250);

    private static final Color C_OBS        = new Color(71,   85, 105);
    private static final Color C_OBS_EDGE   = new Color(148, 163, 184);

    // Güç-up renkleri
    private static final Color C_PU_SLOW    = new Color(56,  189, 248);  // sky-400
    private static final Color C_PU_SHIELD  = new Color(250, 204,  21);  // yellow-400
    private static final Color C_PU_SHRINK  = new Color(251, 113, 133);  // rose-400
    private static final Color C_PU_MAGNET  = new Color(192,  132, 252); // purple-400

    // ── Fontlar ──────────────────────────────────────────────────
    private static final String FF = "SansSerif";
    private final Font fLabel = new Font(FF, Font.BOLD,  15);
    private final Font fNum   = new Font(FF, Font.BOLD,  26);
    private final Font fTitle = new Font(FF, Font.BOLD,  48);
    private final Font fBody  = new Font(FF, Font.PLAIN, 15);
    private final Font fSmall = new Font(FF, Font.PLAIN, 13);
    private final Font fBtn   = new Font(FF, Font.BOLD,  16);
    private final Font fPopup = new Font(FF, Font.BOLD,  18);
    private final Font fTag   = new Font(FF, Font.BOLD,  11);

    // ── Hız ──────────────────────────────────────────────────────
    private static final int BASE_SPEED = 125;
    private int currentSpeed = BASE_SPEED;

    // ── Oyun sistemleri ──────────────────────────────────────────
    private Snake           snake;
    private Food            food;
    private ObstacleManager obstacles;
    private PowerUpManager  powerUps;
    private InputBuffer     inputBuffer;
    private ScoreManager    scoreManager;

    private javax.swing.Timer gameTimer;
    private javax.swing.Timer animTimer;

    // ── Seçenekler (menüden toggle edilir) ───────────────────────
    private boolean wrapMode = false;

    // ── Durum ────────────────────────────────────────────────────
    private enum State { MENU, RUNNING, PAUSED, GAME_OVER, SETTINGS }
    private State state = State.MENU;

    private int score    = 0;
    private int steps    = 0;
    private int level    = 1;
    private int animTick = 0;

    // Menu navigation (0=Başla, 1=Duvar Geçişi, 2=Ayarlar, 3=Çıkış)
    private int menuBtn   = 0;
    private int menuHover = -1;
    private static final int MENU_BTN_COUNT = 4;

    private final Rectangle[] menuRects = new Rectangle[MENU_BTN_COUNT];
    private final Rectangle[] goRects   = new Rectangle[2];

    // Death shake
    private int shakeFrames = 0, shakeX = 0, shakeY = 0;
    private final Random rng = new Random();

    // Level-up banner
    private int levelUpTimer = 0;
    private static final int LU_DURATION = 90;

    // ── Smooth interpolasyon ─────────────────────────────────────
    private float tweenT     = 1.0f;   // 0 = tick başı, 1 = tick sonu
    private Point prevTailPos = null;   // son adımda silinen kuyruk hücresi

    // ── Score popup ──────────────────────────────────────────────
    private static class Popup {
        String text; float x, y, alpha; Color color;
        Popup(String t, float x, float y, Color c) {
            text=t; this.x=x; this.y=y+HUD_H; color=c; alpha=1f;
        }
        boolean tick() { y-=0.9f; alpha-=0.020f; return alpha>0; }
    }
    private final List<Popup> popups = new ArrayList<>();

    // ── Efekt göstergesi (aktif güç-uplar) ───────────────────────
    private static Color puColor(PowerUpManager.PowerUpType t) {
        switch(t) {
            case SLOW:   return new Color(56,  189, 248);
            case SHIELD: return new Color(250, 204,  21);
            case SHRINK: return new Color(251, 113, 133);
            default:     return new Color(192, 132, 252);
        }
    }

    // ── Kurucu ───────────────────────────────────────────────────
    public GamePanel() {
        setPreferredSize(new Dimension(PANEL_W, PANEL_H));
        setBackground(C_BG);
        setFocusable(true);
        addKeyListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);

        scoreManager = new ScoreManager();

        gameTimer = new javax.swing.Timer(currentSpeed, this);
        animTimer = new javax.swing.Timer(16, e -> {
            animTick++;
            if (state == State.RUNNING && tweenT < 1.0f) {
                tweenT = Math.min(1.0f, tweenT + 16.0f / currentSpeed);
            }
            if (shakeFrames > 0) {
                shakeFrames--;
                shakeX = (shakeFrames > 0) ? rng.nextInt(7) - 3 : 0;
                shakeY = (shakeFrames > 0) ? rng.nextInt(7) - 3 : 0;
            }
            if (levelUpTimer > 0) levelUpTimer--;
            repaint();
        });
        animTimer.start();
        initGame();
    }

    // ── Init ─────────────────────────────────────────────────────
    private void initGame() {
        snake       = new Snake(COLS / 2, ROWS / 2);
        snake.setWrapAround(wrapMode, COLS, ROWS);
        food        = new Food(COLS, ROWS);
        obstacles   = new ObstacleManager(COLS, ROWS);
        powerUps    = new PowerUpManager(COLS, ROWS);
        food.setObstacles(obstacles);
        powerUps.setObstacles(obstacles);
        inputBuffer = new InputBuffer(Snake.RIGHT);

        food.spawnNormal(snake);
        score = 0; steps = 0; level = 1;
        currentSpeed = BASE_SPEED;
        popups.clear();
        shakeFrames = 0; shakeX = 0; shakeY = 0;
        levelUpTimer = 0;
        gameTimer.setDelay(currentSpeed);
    }

    // ── Oyun döngüsü ─────────────────────────────────────────────
    @Override
    public void actionPerformed(ActionEvent e) {
        if (state != State.RUNNING) return;
        update();
    }

    private void update() {
        steps++; animTick++;

        // Önce yönü uygula, sonra peek yap
        snake.applyDirection(inputBuffer.poll());

        Point next  = peekNext();
        Food.FoodType eaten = food.getFoodAt(next);
        Point newHead = snake.move(eaten != null);

        // Çarpışma kontrolleri
        boolean hitWall = !wrapMode &&
                (newHead.x < 0 || newHead.x >= COLS || newHead.y < 0 || newHead.y >= ROWS);
        boolean hitSelf = snake.collidesWithSelf();
        boolean hitObs  = obstacles.isObstacle(newHead)
                && !powerUps.isActive(PowerUpManager.PowerUpType.SHIELD);

        if (hitWall || hitSelf || hitObs) { endGame(); return; }

        // Yiyecek
        if (eaten != null) {
            int gain = eaten.score * (powerUps.isActive(PowerUpManager.PowerUpType.MAGNET) ? 2 : 1);
            score += gain;
            Color pc = eaten == Food.FoodType.NORMAL ? C_FOOD_N
                    : eaten == Food.FoodType.BONUS  ? C_FOOD_B : C_FOOD_S;
            addPopup("+" + gain, newHead, pc);
            food.removeFood(newHead);
            food.spawnNormal(snake);
            checkLevelUp();
        }

        // Güç-up toplama
        PowerUpManager.PowerUpType collected = powerUps.collect(newHead);
        if (collected != null) {
            applyPowerUp(collected, newHead);
        }

        food.tick(snake, steps);
        obstacles.tick();
        powerUps.tick(snake, food, steps);
        popups.removeIf(p -> !p.tick());

        // SLOW efekti bittiyse hızı geri yükle
        int targetDelay = powerUps.isActive(PowerUpManager.PowerUpType.SLOW)
                ? Math.max(180, currentSpeed * 2) : currentSpeed;
        if (gameTimer.getDelay() != targetDelay) {
            gameTimer.setDelay(targetDelay);
        }
    }

    private void applyPowerUp(PowerUpManager.PowerUpType type, Point pos) {

        Color c = puColor(type);
        switch (type) {
            case SLOW:
                int slowSpeed = Math.max(180, currentSpeed * 2);
                gameTimer.setDelay(slowSpeed);
                addPopup("YAVAŞ!", pos, c);
                // Efekt bitince hızı geri yükle (animTimer ile kontrol edilir)
                break;
            case SHIELD:
                addPopup("KALKAN!", pos, c);
                break;
            case SHRINK:
                snake.shrink(3);
                addPopup("KÜÇÜLDÜ!", pos, c);
                break;
            case MAGNET:
                addPopup("2× PUAN!", pos, c);
                break;
        }
    }

    private void addPopup(String text, Point cell, Color c) {
        popups.add(new Popup(text, cell.x * CELL + CELL / 2f - 14, cell.y * CELL, c));
    }

    private void checkLevelUp() {
        int nl = score / 80 + 1;
        if (nl > level && nl <= 12) {
            level = nl;
            currentSpeed = Math.max(55, BASE_SPEED - (level - 1) * 8);
            if (!powerUps.isActive(PowerUpManager.PowerUpType.SLOW))
                gameTimer.setDelay(currentSpeed);
            obstacles.spawnForLevel(level, snake, food);

            levelUpTimer = LU_DURATION;
        }
    }

    private Point peekNext() {
        Point h = snake.getHead();
        int d = snake.getNextDirection();
        int nx, ny;
        switch (d) {
            case Snake.UP:    nx=h.x;   ny=h.y-1; break;
            case Snake.DOWN:  nx=h.x;   ny=h.y+1; break;
            case Snake.LEFT:  nx=h.x-1; ny=h.y;   break;
            default:          nx=h.x+1; ny=h.y;   break;
        }
        if (wrapMode) { nx=(nx+COLS)%COLS; ny=(ny+ROWS)%ROWS; }
        return new Point(nx, ny);
    }

    private void endGame() {
        state = State.GAME_OVER;
        gameTimer.stop();
        scoreManager.add(score);

        shakeFrames = 18;
    }

    // ══════════════════════════════════════════════════════════════
    //  ÇİZİM
    // ══════════════════════════════════════════════════════════════

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = setup(g);
        g2.translate(shakeX, shakeY);

        if (state == State.MENU)     { drawMenuOverlay(g2); g2.dispose(); return; }
        if (state == State.SETTINGS) { drawSettingsOverlay(g2); g2.dispose(); return; }

        drawHUD(g2);
        g2.translate(0, HUD_H);
        drawGrid(g2);

        drawObstacles(g2);
        drawFood(g2);
        drawPowerUps(g2);
        drawSnake(g2);
        drawPopups(g2);
        if (levelUpTimer > 0) drawLevelUpBanner(g2);
        drawActiveEffects(g2);

        if (state == State.PAUSED)    drawPauseOverlay(g2);
        if (state == State.GAME_OVER) drawGameOverOverlay(g2);
        //if (state == State.RUNNING)   drawInGameScore(g2);

        g2.dispose();
    }

    private Graphics2D setup(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,         RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,    RenderingHints.VALUE_STROKE_PURE);
        return g2;
    }

    // ── HUD ──────────────────────────────────────────────────────
    private void drawHUD(Graphics2D g2) {
        // Arka plan
        g2.setColor(new Color(10, 13, 22));
        g2.fillRect(0, 0, PANEL_W, HUD_H);

        // Alt ayırıcı çizgi
        g2.setPaint(new GradientPaint(
                0, HUD_H - 1, new Color(52, 211, 153, 0),
                PANEL_W / 2, HUD_H - 1, new Color(52, 211, 153, 60),
                true
        ));
        g2.fillRect(0, HUD_H - 1, PANEL_W, 1);

        int midX = PANEL_W / 2;
        int topY = 18, botY = 46;

        // ── Sol blok: PUAN ────────────────────────────────────────
        int lx = 28;
        g2.setFont(new Font(FF, Font.PLAIN, 11));
        g2.setColor(C_TEXT_MUTED);
        g2.drawString("PUAN", lx, topY);

        g2.setFont(new Font(FF, Font.BOLD, 28));
        g2.setColor(C_ACCENT);
        g2.drawString(String.valueOf(score), lx, botY);

        // ── Orta blok: SEVİYE + ilerleme ─────────────────────────
        String lvlLabel = "SEVİYE  " + level;
        g2.setFont(new Font(FF, Font.BOLD, 13));
        FontMetrics fmL = g2.getFontMetrics();
        int lvlW = fmL.stringWidth(lvlLabel);
        g2.setColor(C_TEXT_MUTED);
        g2.drawString(lvlLabel, midX - lvlW / 2, topY);

        // İlerleme çubuğu (orta altında)
        int barW = 120, barH = 5, barX = midX - barW / 2, barY = topY + 8;
        float progress = Math.min(1f, (float)(score - (level - 1) * 80) / 80f);
        g2.setColor(new Color(255, 255, 255, 12));
        g2.fillRoundRect(barX, barY, barW, barH, barH, barH);
        if (progress > 0) {
            g2.setColor(C_ACCENT);
            g2.fillRoundRect(barX, barY, (int)(barW * progress), barH, barH, barH);
            // İlerleme parıltısı
            g2.setColor(new Color(255, 255, 255, 60));
            g2.fillRoundRect(barX, barY, (int)(barW * progress), barH / 2, barH, barH);
        }

        // Wrap badge (orta alt)
        if (wrapMode) {
            String wlbl = "WRAP";
            g2.setFont(new Font(FF, Font.BOLD, 10));
            FontMetrics fmW = g2.getFontMetrics();
            int ww = fmW.stringWidth(wlbl) + 10;
            int wx = midX - ww / 2, wy = barY + 12;
            g2.setColor(new Color(C_INDIGO.getRed(), C_INDIGO.getGreen(), C_INDIGO.getBlue(), 30));
            g2.fillRoundRect(wx, wy, ww, 14, 7, 7);
            g2.setColor(C_INDIGO);
            g2.drawString(wlbl, wx + 5, wy + 10);
        }

        // ── Sağ blok: EN YÜKSEK ───────────────────────────────────
        int rx = PANEL_W - 28;
        g2.setFont(new Font(FF, Font.PLAIN, 11));
        FontMetrics fmHi = g2.getFontMetrics();
        String hiLabel = "EN YÜKSEK";
        g2.setColor(C_TEXT_MUTED);
        g2.drawString(hiLabel, rx - fmHi.stringWidth(hiLabel), topY);

        g2.setFont(new Font(FF, Font.BOLD, 28));
        g2.setColor(C_WARN);
        String hiStr = String.valueOf(scoreManager.getHighScore());
        FontMetrics fmHiNum = g2.getFontMetrics();
        g2.drawString(hiStr, rx - fmHiNum.stringWidth(hiStr), botY);

        // Aktif kalkan göstergesi (sağ köşe üst)
        if (state == State.RUNNING && powerUps != null
                && powerUps.isActive(PowerUpManager.PowerUpType.SHIELD)) {
            g2.setFont(new Font(FF, Font.BOLD, 10));
            g2.setColor(C_PU_SHIELD);
            g2.drawString("▲ KALKAN", rx - 52, topY);
        }
    }

    // ── Grid (arka plan + ızgara çizgileri) ──────────────────────
    private void drawGrid(Graphics2D g2) {
        // Arka plan
        g2.setPaint(new GradientPaint(0, 0, new Color(11, 14, 24),
                0, GRID_H, new Color(13, 17, 28)));
        g2.fillRect(0, 0, GRID_W, GRID_H);

        // Izgara çizgileri
        g2.setColor(C_GRID);
        g2.setStroke(new BasicStroke(0.5f));
        for (int c = 1; c < COLS; c++) g2.drawLine(c * CELL, 0, c * CELL, GRID_H);
        for (int r = 1; r < ROWS; r++) g2.drawLine(0, r * CELL, GRID_W, r * CELL);

        // Wrap modunda kenar vurgusu
        if (wrapMode) {
            g2.setColor(new Color(C_INDIGO.getRed(), C_INDIGO.getGreen(), C_INDIGO.getBlue(), 50));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRect(1, 1, GRID_W - 2, GRID_H - 2);
        }
    }

    // ── Engeller ─────────────────────────────────────────────────
    private void drawObstacles(Graphics2D g2) {
        // Tam engeller
        for (Point p : obstacles.getObstacles()) {
            drawObstacleAt(g2, p, 1.0f);
        }
        // Animasyonlu engeller (ölçek 0→1)
        for (ObstacleManager.SpawnAnim anim : obstacles.getSpawning()) {
            float prog = easeOutBack(anim.progress()); // overshoot efekti
            drawObstacleAt(g2, anim.pos, prog);
        }
    }

    private void drawObstacleAt(Graphics2D g2, Point p, float scale) {
        int full = CELL - 4;
        int sz   = Math.max(2, (int)(full * scale));
        int off  = (CELL - sz) / 2;
        int px   = p.x * CELL + off;
        int py   = p.y * CELL + off;

        Composite orig = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f, scale)));

        g2.setColor(new Color(0, 0, 0, 60));
        g2.fillRoundRect(px+1, py+2, sz, sz, 5, 5);
        g2.setColor(C_OBS);
        g2.fillRoundRect(px, py, sz, sz, 5, 5);
        if (scale > 0.5f) {
            g2.setColor(C_OBS_EDGE);
            g2.setStroke(new BasicStroke(1f));
            g2.drawLine(px+3, py+1, px+sz-3, py+1);
            g2.setColor(new Color(148,163,184,80));
            g2.drawRoundRect(px, py, sz, sz, 5, 5);
        }
        g2.setComposite(orig);
    }

    /** Ease-out-back: hafif aşım yapıp yerleşir. */
    private float easeOutBack(float t) {
        float c1 = 1.70158f, c3 = c1 + 1;
        return 1 + c3 * (float)Math.pow(t - 1, 3) + c1 * (float)Math.pow(t - 1, 2);
    }

    // ── Yılan ────────────────────────────────────────────────────
    private void drawSnake(Graphics2D g2) {
        java.util.LinkedList<Point> body = snake.getBody();
        int n = body.size();
        if (n == 0) return;

        float sw = CELL - 5f;
        Composite orig = g2.getComposite();

        boolean shielded = powerUps.isActive(PowerUpManager.PowerUpType.SHIELD);
        Color headColor  = shielded ? blendColor(C_SNAKE_HEAD, C_PU_SHIELD, 0.5f) : C_SNAKE_HEAD;
        Color glowColor  = shielded ? new Color(250, 204, 21, 50) : C_SNAKE_GLOW;

        // Baş görsel konumu (iki game tick arası interpolasyon)
        float[] hv = headVisualPos();
        float hvx = hv[0], hvy = hv[1];

        // Görsel pozisyon dizisi: [0] = interpolated baş, [1..n-1] = grid merkezi
        float[] vx = new float[n];
        float[] vy = new float[n];
        vx[0] = hvx; vy[0] = hvy;
        for (int i = 1; i < n; i++) {
            vx[i] = cx(body.get(i).x);
            vy[i] = cy(body.get(i).y);
        }

        // Glow
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.18f));
        g2.setColor(glowColor);
        g2.setStroke(new BasicStroke(sw + 10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(buildSnakePath(vx, vy, body));
        g2.setComposite(orig);

        // Gövde segmentleri
        g2.setStroke(new BasicStroke(sw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = n - 1; i >= 1; i--) {
            Point a = body.get(i), b = body.get(i - 1);
            if (Math.abs(a.x - b.x) > 1 || Math.abs(a.y - b.y) > 1) continue;
            float t = (float) i / Math.max(1, n - 1);
            g2.setColor(blendColor(C_SNAKE_BODY, C_SNAKE_TAIL, t));
            g2.drawLine((int) vx[i], (int) vy[i], (int) vx[i - 1], (int) vy[i - 1]);
        }

        // Kaybolan kuyruk segmenti (tweenT ile söner)
        if (prevTailPos != null && tweenT < 1.0f && n >= 2) {
            Point last = body.get(n - 1);
            if (Math.abs(last.x - prevTailPos.x) <= 1 && Math.abs(last.y - prevTailPos.y) <= 1) {
                float alpha = (1.0f - tweenT) * 0.95f;
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                g2.setColor(C_SNAKE_TAIL);
                g2.setStroke(new BasicStroke(sw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine((int) vx[n - 1], (int) vy[n - 1],
                        cx(prevTailPos.x), cy(prevTailPos.y));
                g2.setComposite(orig);
            }
        }

        // Baş dairesi
        float hr = sw / 2f + 1f;
        g2.setColor(headColor);
        g2.fill(new Ellipse2D.Float(hvx - hr, hvy - hr, hr * 2, hr * 2));
        g2.setColor(new Color(255, 255, 255, 80));
        float gr = hr * 0.35f;
        g2.fill(new Ellipse2D.Float(hvx - hr * 0.5f, hvy - hr * 0.55f, gr * 2, gr * 2));
        drawEyesAt(g2, hvx, hvy, sw);

        // Kalkan halkası
        if (shielded) {
            g2.setColor(new Color(250, 204, 21, 120));
            g2.setStroke(new BasicStroke(2f));
            g2.draw(new Ellipse2D.Float(hvx - hr - 3, hvy - hr - 3, (hr + 3) * 2, (hr + 3) * 2));
        }
    }

    /**
     * İki game tick arasında baş görsel konumunu interpolate eder.
     * tweenT=0 → body[1] konumu (yeni tick henüz başlamadı)
     * tweenT=1 → body[0] konumu (tam hücre kaydı tamamlandı)
     */
    private float[] headVisualPos() {
        java.util.LinkedList<Point> body = snake.getBody();
        if (tweenT >= 1.0f || body.size() < 2) {
            Point h = body.getFirst();
            return new float[]{ cx(h.x), cy(h.y) };
        }
        Point h0 = body.get(0); // mevcut baş (son game tick'te konumlandı)
        Point h1 = body.get(1); // bir önceki segment (= eski baş)
        float x0 = cx(h0.x), y0 = cy(h0.y);
        float x1 = cx(h1.x), y1 = cy(h1.y);
        // Wrap-around geçişini düzelt
        float dx = x0 - x1, dy = y0 - y1;
        if (Math.abs(dx) > CELL * 2) dx = dx > 0 ? dx - GRID_W : dx + GRID_W;
        if (Math.abs(dy) > CELL * 2) dy = dy > 0 ? dy - GRID_H : dy + GRID_H;
        return new float[]{ x1 + dx * tweenT, y1 + dy * tweenT };
    }

    /** Görsel pozisyon dizisinden glow path'i oluşturur. */
    private Path2D.Float buildSnakePath(float[] vx, float[] vy,
                                        java.util.LinkedList<Point> body) {
        Path2D.Float path = new Path2D.Float();
        path.moveTo(vx[0], vy[0]);
        for (int i = 1; i < body.size(); i++) {
            Point prev = body.get(i - 1), curr = body.get(i);
            if (Math.abs(prev.x - curr.x) > 1 || Math.abs(prev.y - curr.y) > 1)
                path.moveTo(vx[i], vy[i]);
            else
                path.lineTo(vx[i], vy[i]);
        }
        return path;
    }

    /** Float koordinatlı göz çizimi (interpolated baş için). */
    private void drawEyesAt(Graphics2D g2, float hcx, float hcy, float sw) {
        int dir = snake.getDirection();
        float er = 2.8f, dist = sw * 0.28f;
        float ex1, ey1, ex2, ey2;
        switch (dir) {
            case Snake.RIGHT: ex1=hcx+dist*.3f;ey1=hcy-dist;ex2=hcx+dist*.3f;ey2=hcy+dist;break;
            case Snake.LEFT:  ex1=hcx-dist*.3f;ey1=hcy-dist;ex2=hcx-dist*.3f;ey2=hcy+dist;break;
            case Snake.UP:    ex1=hcx-dist;ey1=hcy-dist*.3f;ex2=hcx+dist;ey2=hcy-dist*.3f;break;
            default:          ex1=hcx-dist;ey1=hcy+dist*.3f;ex2=hcx+dist;ey2=hcy+dist*.3f;break;
        }
        g2.setColor(new Color(10, 16, 30));
        g2.fill(new Ellipse2D.Float(ex1-er, ey1-er, er*2, er*2));
        g2.fill(new Ellipse2D.Float(ex2-er, ey2-er, er*2, er*2));
        g2.setColor(new Color(255, 255, 255, 200));
        float pr = er * 0.45f;
        g2.fill(new Ellipse2D.Float(ex1-pr, ey1-pr*1.2f, pr*2, pr*2));
        g2.fill(new Ellipse2D.Float(ex2-pr, ey2-pr*1.2f, pr*2, pr*2));
    }

    // ── Yiyecekler ───────────────────────────────────────────────
    private void drawFood(Graphics2D g2) {
        double pulse = Math.sin(animTick * 0.11) * 0.5 + 0.5;
        for (Map.Entry<Point, Food.FoodType> e : food.getFoodMap().entrySet()) {
            Point p = e.getKey(); int x=cx(p.x), y=cy(p.y);
            switch (e.getValue()) {
                case NORMAL: drawFoodNormal(g2,x,y,pulse);    break;
                case BONUS:  drawFoodBonus (g2,x,y,pulse,p);  break;
                case SUPER:  drawFoodSuper (g2,x,y,pulse);    break;
            }
        }
    }

    private void drawFoodNormal(Graphics2D g2, int x, int y, double pulse) {
        float r = (float)(8.5 + pulse * 1.5);
        glow(g2,x,y,C_FOOD_N,r+7,3);
        g2.setColor(C_FOOD_N); g2.fill(new Ellipse2D.Float(x-r,y-r,r*2,r*2));
        g2.setColor(new Color(255,200,210,140));
        float gr=r*.45f; g2.fill(new Ellipse2D.Float(x-r*.5f,y-r*.6f,gr*2,gr*2));
    }

    private void drawFoodBonus(Graphics2D g2, int x, int y, double pulse, Point p) {
        float r = (float)(8 + pulse * 2);
        glow(g2,x,y,C_FOOD_B,r+8,4);
        Graphics2D gs=(Graphics2D)g2.create();
        gs.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        gs.translate(x,y); gs.rotate(animTick*0.05);
        gs.setColor(C_FOOD_B); starShape(gs,0,0,(int)r,(int)(r*.42f));
        gs.setColor(new Color(255,255,200,180)); gs.fill(new Ellipse2D.Float(-3f,-3f,6f,6f));
        gs.dispose();
        if (food.getBonusTimer()>0) {
            float ratio=(float)food.getBonusTimer()/food.getBonusDuration();
            int bw=CELL-6,bx=p.x*CELL+3,by2=p.y*CELL+CELL-5;
            g2.setColor(new Color(255,255,255,20)); g2.fillRoundRect(bx,by2,bw,3,2,2);
            g2.setColor(ratio>.35f?C_FOOD_B:C_DANGER); g2.fillRoundRect(bx,by2,(int)(bw*ratio),3,2,2);
        }
    }

    private void drawFoodSuper(Graphics2D g2, int x, int y, double pulse) {
        float r=(float)(9+pulse*2.5f); glow(g2,x,y,C_FOOD_S,r+9,5);
        Graphics2D gd=(Graphics2D)g2.create();
        gd.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        gd.translate(x,y); gd.rotate(animTick*0.035);
        gd.setColor(new Color(167,139,250,80)); gd.fill(new Ellipse2D.Float(-r-4,-r-4,(r+4)*2,(r+4)*2));
        gd.setPaint(new GradientPaint(-r,-r,new Color(216,180,254),r,r,C_FOOD_S));
        gd.fillPolygon(new int[]{0,(int)r,0,-(int)r},new int[]{-(int)r,0,(int)r,0},4);
        gd.setColor(new Color(240,230,255,180)); gd.fill(new Ellipse2D.Float(-3,-3,6,6));
        gd.dispose();
    }

    // ── Güç-uplar ─────────────────────────────────────────────────
    private void drawPowerUps(Graphics2D g2) {
        double pulse = Math.sin(animTick * 0.13) * 0.5 + 0.5;
        for (Map.Entry<Point, PowerUpManager.PowerUp> e : powerUps.getBoardMap().entrySet()) {
            Point p = e.getKey();
            PowerUpManager.PowerUp pu = e.getValue();
            drawPowerUpAt(g2, cx(p.x), cy(p.y), pu, pulse);
        }
    }

    private void drawPowerUpAt(Graphics2D g2, int x, int y, PowerUpManager.PowerUp pu, double pulse) {
        Color c = puColor(pu.type);
        float r = (float)(9 + pulse * 2);
        glow(g2, x, y, c, r + 8, 4);

        // Dönen kare
        Graphics2D gt = (Graphics2D) g2.create();
        gt.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gt.translate(x, y);
        gt.rotate(animTick * 0.04 + pu.type.ordinal() * Math.PI / 2);
        gt.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 50));
        gt.fill(new RoundRectangle2D.Float(-r - 2, -r - 2, (r+2)*2, (r+2)*2, 6, 6));
        gt.setColor(c);
        gt.fill(new RoundRectangle2D.Float(-r, -r, r * 2, r * 2, 5, 5));
        gt.setColor(new Color(255, 255, 255, 160));
        gt.fill(new Ellipse2D.Float(-3, -3, 6, 6));
        gt.dispose();

        // Etiket
        g2.setFont(fTag);
        g2.setColor(new Color(255, 255, 255, 200));
        FontMetrics fm = g2.getFontMetrics();
        String lbl = pu.type.label;
        g2.drawString(lbl, x - fm.stringWidth(lbl)/2, y + (int)r + 12);

        // Kalan ömür çubuğu
        float lifeRatio = pu.lifetime / 180f;
        int bw = CELL - 6;
        int bx = (x - CELL/2) + 3, by = y + (int)r + 14;
        g2.setColor(new Color(255, 255, 255, 18));
        g2.fillRoundRect(bx, by, bw, 2, 2, 2);
        g2.setColor(c);
        g2.fillRoundRect(bx, by, (int)(bw * lifeRatio), 2, 2, 2);
    }

    // ── Aktif efektler (sağ alt köşe) ────────────────────────────
    private void drawActiveEffects(Graphics2D g2) {
        List<PowerUpManager.ActiveEffect> effects = powerUps.getActiveEffects();
        if (effects.isEmpty()) return;

        int ex = GRID_W - 8, ey = GRID_H - 8;
        int chipW = 72, chipH = 22, gap = 6;

        for (int i = 0; i < effects.size(); i++) {
            PowerUpManager.ActiveEffect ef = effects.get(i);
            Color c = puColor(ef.type);
            long remaining = ef.expiresAt - System.currentTimeMillis();
            float ratio = Math.max(0, Math.min(1, (float) remaining / ef.type.durationMs));

            int cy2 = ey - i * (chipH + gap);
            int cx2 = ex - chipW;

            // Chip arka plan
            g2.setColor(new Color(10, 12, 20, 210));
            g2.fillRoundRect(cx2, cy2 - chipH + 4, chipW, chipH, 8, 8);
            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 100));
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(cx2, cy2 - chipH + 4, chipW, chipH, 8, 8);

            // Etiket
            g2.setFont(fTag);
            g2.setColor(c);
            g2.drawString(ef.type.label, cx2 + 7, cy2 - 1);

            // Kalan süre çubuğu
            g2.setColor(new Color(255, 255, 255, 15));
            g2.fillRect(cx2 + 6, cy2 + 3, chipW - 12, 2);
            g2.setColor(c);
            g2.fillRect(cx2 + 6, cy2 + 3, (int)((chipW - 12) * ratio), 2);
        }
    }

    // ── Popuplar ─────────────────────────────────────────────────
    private void drawPopups(Graphics2D g2) {
        Composite orig = g2.getComposite();
        g2.setFont(fPopup);
        for (Popup p : popups) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, p.alpha*.9f));
            g2.setColor(new Color(0,0,0,120));
            g2.drawString(p.text,(int)p.x+1,(int)p.y+1);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, p.alpha));
            g2.setColor(p.color); g2.drawString(p.text,(int)p.x,(int)p.y);
        }
        g2.setComposite(orig);
    }

    // ── Level-up banner ──────────────────────────────────────────
    private void drawLevelUpBanner(Graphics2D g2) {
        float alpha = levelUpTimer < 20 ? levelUpTimer/20f : 1f;
        Composite orig = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha*.95f));
        int bw=260,bh=44,bx=GRID_W/2-bw/2,by=20;
        g2.setColor(new Color(17,24,39,230)); g2.fillRoundRect(bx,by,bw,bh,12,12);
        g2.setColor(new Color(52,211,153,120)); g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(bx,by,bw,bh,12,12);
        g2.setFont(new Font(FF,Font.BOLD,15)); g2.setColor(C_ACCENT);
        drawCenteredStr(g2,new Font(FF,Font.BOLD,15),"⬆  SEVİYE "+level+"  —  Hız Arttı!",GRID_W/2,by+27,C_ACCENT);
        g2.setComposite(orig);
    }

    // ══════════════════════════════════════════════════════════════
    //  OVERLAY'LAR
    // ══════════════════════════════════════════════════════════════

    private void drawMenuOverlay(Graphics2D g2) {
        int W = GRID_W, H = GRID_H, cx = W / 2;
        Composite orig = g2.getComposite();

        // ── Arka plan ────────────────────────────────────────────
        g2.setPaint(new GradientPaint(0, 0, new Color(7, 9, 18),
                0, H, new Color(12, 16, 28)));
        g2.fillRect(0, 0, W, H);

        // Sadece sağ-alt köşe glow (sol üstte glow yok)
        drawCornerGlow(g2, W, H, new Color(99, 102, 241, 18));
        g2.setComposite(orig);

        // ── SNAKE başlığı ────────────────────────────────────────
        int logoY = 104;
        Font fBig = new Font(FF, Font.BOLD, 68);
        g2.setFont(fBig);
        FontMetrics fmT = g2.getFontMetrics();
        String title = "SNAKE";
        int tW = fmT.stringWidth(title);
        int tX = cx - tW / 2;

        // Glow katmanı
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.12f));
        g2.setColor(C_ACCENT);
        g2.drawString(title, tX + 3, logoY + 3);
        g2.setComposite(orig);
        g2.setColor(C_ACCENT);
        g2.drawString(title, tX, logoY);

        // Başlık altı ince çizgi
        int decY = logoY + 12;
        for (int i = 0; i < 3; i++) {
            g2.setColor(new Color(52, 211, 153, 70 - i * 22));
            g2.fillRect(cx - (24 - i * 7), decY + i * 3, (24 - i * 7) * 2, 1);
        }

        // ── Buton kartları ───────────────────────────────────────
        int btnW = 280, btnH = 52, btnGap = 10;
        int totalBtnH = MENU_BTN_COUNT * btnH + (MENU_BTN_COUNT - 1) * btnGap;
        int btnX = cx - btnW / 2;
        int btnStartY = logoY + 38;

        String[] labels = {
                "Başla",
                "Duvar Geçişi  —  " + (wrapMode ? "Açık" : "Kapalı"),
                "Ayarlar",
                "Çıkış"
        };
        Color[]  accents = { C_ACCENT, C_INDIGO, new Color(251, 191, 36), C_DANGER };
        String[] icons   = { "▶", "⟳", "⚙", "×" };

        for (int i = 0; i < MENU_BTN_COUNT; i++) {
            int by = btnStartY + i * (btnH + btnGap);
            menuRects[i] = new Rectangle(btnX, by, btnW, btnH);
            drawMenuBtn(g2, menuRects[i], icons[i], labels[i], accents[i],
                    menuBtn == i, menuHover == i);
        }

        // ── Klavye ipucu ─────────────────────────────────────────
        int hintY = btnStartY + totalBtnH + 24;
        g2.setFont(new Font(FF, Font.PLAIN, 13));
        g2.setColor(new Color(71, 85, 105));
        String hint = "↑ ↓  gezin   ·   Enter  onayla";
        FontMetrics fmH = g2.getFontMetrics();
        g2.drawString(hint, cx - fmH.stringWidth(hint)/2, hintY);

        // ── En iyi skorlar ───────────────────────────────────────
        if (scoreManager.size() > 0) {
            int[] tops = scoreManager.getTopScores();
            int scY = hintY + 26;
            int scW = 260, scH = 42;
            int scX = cx - scW / 2;

            g2.setColor(new Color(15, 20, 35, 220));
            g2.fillRoundRect(scX, scY, scW, scH, 12, 12);
            g2.setColor(new Color(255, 255, 255, 10));
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(scX, scY, scW, scH, 12, 12);

            int cols = Math.min(tops.length, 3);
            int colW = scW / cols;
            for (int i = 0; i < cols; i++) {
                boolean gold = (i == 0);
                g2.setFont(gold ? new Font(FF, Font.BOLD, 15) : new Font(FF, Font.PLAIN, 13));
                g2.setColor(gold ? C_WARN : new Color(71, 85, 105));
                FontMetrics fmC = g2.getFontMetrics();
                String s = (i+1) + ".  " + tops[i];
                int sx = scX + i * colW + colW/2 - fmC.stringWidth(s)/2;
                g2.drawString(s, sx, scY + scH/2 + fmC.getAscent()/2 - 2);
            }
        }
    }

    /** Köşe glow efekti */
    private void drawCornerGlow(Graphics2D g2, int x, int y, Color c) {
        int r = 200;
        RadialGradientPaint rg = new RadialGradientPaint(
                new java.awt.geom.Point2D.Float(x, y), r,
                new float[]{ 0f, 1f },
                new Color[]{ c, new Color(c.getRed(), c.getGreen(), c.getBlue(), 0) }
        );
        Composite prev = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        g2.setPaint(rg);
        g2.fillOval(x - r, y - r, r * 2, r * 2);
        g2.setComposite(prev);
    }

    /** Her durumda tam kart görünümlü buton */
    private void drawMenuBtn(Graphics2D g2, Rectangle r, String icon,
                             String label, Color accent, boolean sel, boolean hover) {
        RoundRectangle2D rr = new RoundRectangle2D.Float(r.x, r.y, r.width, r.height, 12, 12);
        Composite orig = g2.getComposite();

        // Kart arka planı — her zaman görünür koyu dolgu
        Color bgBase = sel
                ? new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 28)
                : hover
                ? new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 16)
                : new Color(18, 24, 40);
        g2.setColor(bgBase);
        g2.fill(rr);

        // Kenarlık
        float stroke = sel ? 1.5f : 1f;
        int borderAlpha = sel ? 160 : hover ? 90 : 30;
        g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), borderAlpha));
        g2.setStroke(new BasicStroke(stroke));
        g2.draw(rr);
        g2.setComposite(orig);

        // Sol vurgu şeridi (sadece seçili)
        if (sel) {
            g2.setColor(accent);
            g2.fill(new RoundRectangle2D.Float(r.x, r.y + 10, 3, r.height - 20, 3, 3));
        }

        // İkon
        int midY = r.y + r.height / 2;
        g2.setFont(new Font(FF, Font.BOLD, 14));
        FontMetrics fmI = g2.getFontMetrics();
        int iconAlpha = sel ? 255 : hover ? 200 : 130;
        g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), iconAlpha));
        g2.drawString(icon, r.x + 18, midY + fmI.getAscent() / 2 - 2);

        // Etiket
        g2.setFont(sel ? new Font(FF, Font.BOLD, 15) : new Font(FF, Font.PLAIN, 14));
        FontMetrics fmL = g2.getFontMetrics();
        Color txtColor = sel ? accent : hover ? new Color(210, 218, 230) : new Color(150, 163, 180);
        g2.setColor(txtColor);
        g2.drawString(label, r.x + 42, midY + fmL.getAscent() / 2 - 2);

        // Sağ ok (seçili)
        if (sel) {
            g2.setFont(new Font(FF, Font.PLAIN, 16));
            g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 100));
            g2.drawString("›", r.x + r.width - 22, midY + 6);
        }
    }

    // ── Oyun içi skor kartı (sol üst köşe) ───────────────────────
    private void drawInGameScore(Graphics2D g2) {
        int pad = 12, cW = 130, cH = 58;
        int cX = pad, cY = pad;

        g2.setColor(new Color(10, 13, 22, 200));
        g2.fillRoundRect(cX, cY, cW, cH, 12, 12);
        g2.setColor(new Color(255, 255, 255, 12));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(cX, cY, cW, cH, 12, 12);

        g2.setFont(new Font(FF, Font.PLAIN, 11));
        g2.setColor(C_TEXT_MUTED);
        g2.drawString("PUAN", cX + 14, cY + 18);

        g2.setFont(new Font(FF, Font.BOLD, 26));
        g2.setColor(C_ACCENT);
        g2.drawString(String.valueOf(score), cX + 14, cY + 44);

        // Seviye badge
        String lvl = "Sv." + level;
        g2.setFont(new Font(FF, Font.BOLD, 11));
        g2.setColor(C_INDIGO);
        FontMetrics fm = g2.getFontMetrics();
        int lw = fm.stringWidth(lvl);
        g2.setColor(new Color(129, 140, 248, 30));
        g2.fillRoundRect(cX + cW - lw - 16, cY + 6, lw + 10, 16, 8, 8);
        g2.setColor(C_INDIGO);
        g2.drawString(lvl, cX + cW - lw - 11, cY + 17);

        // Seviye ilerleme çubuğu
        float progress = Math.min(1f, (float)(score - (level - 1) * 80) / 80f);
        int barX = cX + 14, barY = cY + cH - 5, barW = cW - 28;
        g2.setColor(new Color(255, 255, 255, 10));
        g2.fillRoundRect(barX, barY, barW, 3, 2, 2);
        if (progress > 0) {
            g2.setColor(C_ACCENT);
            g2.fillRoundRect(barX, barY, (int)(barW * progress), 3, 2, 2);
        }
    }

    private void drawPauseOverlay(Graphics2D g2) {
        // Koyu örtü
        g2.setColor(new Color(7, 9, 18, 210));
        g2.fillRect(0, 0, GRID_W, GRID_H);

        int cW = 320, cH = 160, cX = GRID_W/2 - cW/2, cY = GRID_H/2 - cH/2;

        // Kart
        g2.setColor(new Color(15, 20, 35));
        g2.fillRoundRect(cX, cY, cW, cH, 18, 18);
        g2.setColor(new Color(129, 140, 248, 50));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(cX, cY, cW, cH, 18, 18);

        // Başlık
        g2.setFont(new Font(FF, Font.BOLD, 26));
        g2.setColor(C_TEXT);
        FontMetrics fm = g2.getFontMetrics();
        String t = "Duraklatıldı";
        g2.drawString(t, GRID_W/2 - fm.stringWidth(t)/2, cY + 52);

        // Ayırıcı
        g2.setColor(new Color(255, 255, 255, 12));
        g2.fillRect(cX + 30, cY + 62, cW - 60, 1);

        // İpuçları
        g2.setFont(new Font(FF, Font.PLAIN, 14));
        g2.setColor(C_TEXT_MUTED);
        FontMetrics fm2 = g2.getFontMetrics();
        String h1 = "P  —  Devam Et", h2 = "ESC  —  Ana Menü";
        g2.drawString(h1, GRID_W/2 - fm2.stringWidth(h1)/2, cY + 96);
        g2.drawString(h2, GRID_W/2 - fm2.stringWidth(h2)/2, cY + 120);
    }

    private void drawGameOverOverlay(Graphics2D g2) {
        g2.setColor(new Color(7, 9, 18, 215));
        g2.fillRect(0, 0, GRID_W, GRID_H);

        int cW = 340, cH = 380, cX = GRID_W/2 - cW/2, cY = GRID_H/2 - cH/2;

        // Ana kart
        g2.setColor(new Color(15, 20, 35));
        g2.fillRoundRect(cX, cY, cW, cH, 20, 20);
        g2.setColor(new Color(248, 113, 113, 40));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(cX, cY, cW, cH, 20, 20);
        // Üst kırmızı şerit
        g2.setColor(new Color(248, 113, 113, 25));
        g2.fillRoundRect(cX, cY, cW, 60, 20, 20);
        g2.fillRect(cX, cY + 40, cW, 20);

        // Başlık
        g2.setFont(new Font(FF, Font.BOLD, 30));
        g2.setColor(C_DANGER);
        FontMetrics fmT = g2.getFontMetrics();
        String title = "Oyun Bitti";
        g2.drawString(title, GRID_W/2 - fmT.stringWidth(title)/2, cY + 42);

        // Ayırıcı
        g2.setColor(new Color(255, 255, 255, 10));
        g2.fillRect(cX + 30, cY + 58, cW - 60, 1);

        // Stat kartları
        int sY = cY + 80, sGap = 56;
        drawStatCard(g2, cX + 24, sY,        cW - 48, "PUAN",    String.valueOf(score),             C_ACCENT);
        drawStatCard(g2, cX + 24, sY + sGap, cW - 48, "SEVİYE",  String.valueOf(level),             C_INDIGO);
        drawStatCard(g2, cX + 24, sY+sGap*2, cW - 48, "UZUNLUK", String.valueOf(snake.getLength()), C_TEXT_MUTED);

        // En iyi skorlar
        int[] tops = scoreManager.getTopScores();
        if (tops.length > 0) {
            int divY = sY + sGap * 3 + 4;
            g2.setColor(new Color(255, 255, 255, 10));
            g2.fillRect(cX + 30, divY, cW - 60, 1);

            g2.setFont(new Font(FF, Font.PLAIN, 12));
            g2.setColor(C_TEXT_MUTED);
            FontMetrics fmS = g2.getFontMetrics();
            String lbl = "EN İYİ SKORLAR";
            g2.drawString(lbl, GRID_W/2 - fmS.stringWidth(lbl)/2, divY + 18);

            int cols = Math.min(tops.length, 3), colW = (cW - 60) / cols;
            for (int i = 0; i < cols; i++) {
                boolean gold = (i == 0);
                g2.setFont(gold ? new Font(FF, Font.BOLD, 15) : new Font(FF, Font.PLAIN, 13));
                g2.setColor(gold ? C_WARN : C_TEXT_MUTED);
                FontMetrics fmC = g2.getFontMetrics();
                String s = (i+1) + ".  " + tops[i];
                int sx = cX + 30 + i * colW + colW/2 - fmC.stringWidth(s)/2;
                g2.drawString(s, sx, divY + 38);
            }
        }

        // Butonlar
        int bY = cY + cH - 92, bW = cW - 60, bX = cX + 30;
        goRects[0] = new Rectangle(bX, bY,      bW, 40);
        goRects[1] = new Rectangle(bX, bY + 48, bW, 36);
        drawMenuBtn(g2, goRects[0], "▶", "Tekrar Oyna", C_ACCENT,  false, false);
        drawMenuBtn(g2, goRects[1], "‹", "Ana Menü",    C_INDIGO, false, false);
    }

    /** İki sütunlu stat satırı — kart içinde */
    private void drawStatCard(Graphics2D g2, int x, int y, int w, String lbl, String val, Color vc) {
        g2.setColor(new Color(255, 255, 255, 5));
        g2.fillRoundRect(x, y, w, 46, 10, 10);
        g2.setColor(new Color(255, 255, 255, 8));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(x, y, w, 46, 10, 10);

        g2.setFont(new Font(FF, Font.PLAIN, 12));
        g2.setColor(C_TEXT_MUTED);
        g2.drawString(lbl, x + 16, y + 18);

        g2.setFont(new Font(FF, Font.BOLD, 22));
        g2.setColor(vc);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(val, x + w - fm.stringWidth(val) - 16, y + 36);
    }

    private void drawSettingsOverlay(Graphics2D g2) {
        int W = GRID_W, H = GRID_H, cx = W / 2;

        // Arka plan
        g2.setPaint(new GradientPaint(0, 0, new Color(7, 9, 18), 0, H, new Color(12, 16, 28)));
        g2.fillRect(0, 0, W, H);
        Composite orig = g2.getComposite();
        drawCornerGlow(g2, 0, 0, new Color(99, 102, 241, 14));
        drawCornerGlow(g2, W, H, new Color(52, 211, 153, 12));
        g2.setComposite(orig);

        // Üst başlık
        int titleY = 70;
        g2.setFont(new Font(FF, Font.BOLD, 28));
        g2.setColor(new Color(255, 255, 255, 220));
        FontMetrics fmT = g2.getFontMetrics();
        String t = "Ayarlar";
        g2.drawString(t, cx - fmT.stringWidth(t)/2, titleY);
        g2.setColor(new Color(52, 211, 153, 60));
        g2.fillRect(cx - 28, titleY + 8, 56, 1);

        // Kart
        int cardW = 310, cardH = 320;
        int cardX = cx - cardW / 2, cardY = titleY + 24;
        g2.setColor(new Color(15, 20, 35, 200));
        g2.fillRoundRect(cardX, cardY, cardW, cardH, 16, 16);
        g2.setColor(new Color(255, 255, 255, 10));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(cardX, cardY, cardW, cardH, 16, 16);

        // Yakında etiketi
        g2.setFont(new Font(FF, Font.PLAIN, 12));
        g2.setColor(new Color(100, 116, 139));
        String soon = "Ayarlar sayfası çok yakında...";
        FontMetrics fmS = g2.getFontMetrics();
        g2.drawString(soon, cx - fmS.stringWidth(soon)/2, cardY + cardH/2);

        // Geri butonu
        int bW = 160, bH = 40;
        int bX = cx - bW/2, bY = cardY + cardH + 20;
        Rectangle backRect = new Rectangle(bX, bY, bW, bH);
        drawMenuBtn(g2, backRect, "‹", "Geri Dön", new Color(99, 102, 241),
                false, false);

        // ESC hint
        g2.setFont(new Font(FF, Font.PLAIN, 10));
        g2.setColor(new Color(71, 85, 105));
        String hint = "ESC  →  Geri Dön";
        FontMetrics fmH = g2.getFontMetrics();
        g2.drawString(hint, cx - fmH.stringWidth(hint)/2, bY + bH + 18);
    }

    // ── Ortak çizim yardımcıları ─────────────────────────────────
    private void drawCard(Graphics2D g2, int x, int y, int w, int h, Color bg, Color border) {
        RoundRectangle2D rr = new RoundRectangle2D.Float(x,y,w,h,16,16);
        g2.setColor(bg); g2.fill(rr);
        g2.setColor(border); g2.setStroke(new BasicStroke(1f)); g2.draw(rr);
    }

    private void drawRoundedButton(Graphics2D g2, int x, int y, int w, int h, String lbl, Color accent) {
        RoundRectangle2D rr = new RoundRectangle2D.Float(x,y,w,h,10,10);
        g2.setColor(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),28)); g2.fill(rr);
        g2.setColor(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),160));
        g2.setStroke(new BasicStroke(1f)); g2.draw(rr);
        g2.setFont(fBtn); g2.setColor(accent);
        FontMetrics fm=g2.getFontMetrics();
        g2.drawString(lbl, x+w/2-fm.stringWidth(lbl)/2, y+h/2+5);
    }

    private void statRow(Graphics2D g2, int cx, int y, int cw, String lbl, String val, Color vc) {
        g2.setFont(fBody); g2.setColor(C_TEXT_MUTED); g2.drawString(lbl,cx+28,y+16);
        g2.setFont(fLabel); g2.setColor(vc);
        FontMetrics fm=g2.getFontMetrics();
        g2.drawString(val,cx+cw-28-fm.stringWidth(val),y+16);
        g2.setColor(new Color(255,255,255,8)); g2.fillRect(cx+28,y+22,cw-56,1);
    }

    private void drawKeyRow(Graphics2D g2, String key, String desc, int x, int y) {
        g2.setFont(new Font(FF,Font.BOLD,11));
        int kw=g2.getFontMetrics().stringWidth(key)+14;
        g2.setColor(new Color(255,255,255,18)); g2.fillRoundRect(x,y-13,kw,18,6,6);
        g2.setColor(new Color(255,255,255,40)); g2.setStroke(new BasicStroke(.5f));
        g2.drawRoundRect(x,y-13,kw,18,6,6);
        g2.setColor(C_TEXT); g2.drawString(key,x+7,y);
        g2.setFont(fBody); g2.setColor(C_TEXT_MUTED); g2.drawString(desc,x+kw+12,y);
    }

    private void foodChip(Graphics2D g2, int x, int y, Color dot, String lbl) {
        g2.setColor(dot); g2.fill(new Ellipse2D.Float(x,y-7,9,9));
        g2.setFont(new Font(FF,Font.PLAIN,11)); g2.setColor(C_TEXT_MUTED);
        g2.drawString(lbl,x+13,y);
    }

    private void glow(Graphics2D g2, int x, int y, Color c, float r, int layers) {
        Composite orig=g2.getComposite();
        for (int i=layers;i>=1;i--) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,.055f*i));
            g2.setColor(c); float rr=r+(layers-i)*4f;
            g2.fill(new Ellipse2D.Float(x-rr,y-rr,rr*2,rr*2));
        }
        g2.setComposite(orig);
    }

    private void starShape(Graphics2D g2, int cx, int cy, int outer, int inner) {
        int n=5; int[] xs=new int[n*2],ys=new int[n*2];
        for (int i=0;i<n*2;i++) {
            double a=Math.PI/n*i-Math.PI/2; int r=(i%2==0)?outer:inner;
            xs[i]=(int)(cx+r*Math.cos(a)); ys[i]=(int)(cy+r*Math.sin(a));
        }
        g2.fillPolygon(xs,ys,n*2);
    }

    private void drawCenteredStr(Graphics2D g2, Font f, String s, int cx, int y, Color c) {
        g2.setFont(f); g2.setColor(c);
        FontMetrics fm=g2.getFontMetrics();
        g2.drawString(s,cx-fm.stringWidth(s)/2,y);
    }

    // Overload — font önceden set edilmişse
    private void drawCenteredStr(Graphics2D g2, String s, int cx, int y) {
        FontMetrics fm=g2.getFontMetrics(); g2.drawString(s,cx-fm.stringWidth(s)/2,y);
    }

    private int cx(int col) { return col*CELL+CELL/2; }
    private int cy(int row) { return row*CELL+CELL/2; }

    private Color blendColor(Color a, Color b, float t) {
        t=Math.max(0,Math.min(1,t));
        return new Color(
                (int)(a.getRed()  +t*(b.getRed()  -a.getRed())),
                (int)(a.getGreen()+t*(b.getGreen()-a.getGreen())),
                (int)(a.getBlue() +t*(b.getBlue() -a.getBlue()))
        );
    }

    // ── Klavye ───────────────────────────────────────────────────
    @Override
    public void keyPressed(KeyEvent e) {
        int k=e.getKeyCode();
        switch (state) {
            case SETTINGS:
                if (k == KeyEvent.VK_ESCAPE) { state = State.MENU; repaint(); }
                break;
            case MENU:
                if (k == KeyEvent.VK_UP || k == KeyEvent.VK_W) {
                    menuBtn = (menuBtn - 1 + MENU_BTN_COUNT) % MENU_BTN_COUNT;
                    repaint();
                } else if (k == KeyEvent.VK_DOWN || k == KeyEvent.VK_S) {
                    menuBtn = (menuBtn + 1) % MENU_BTN_COUNT;
                    repaint();
                } else if (k == KeyEvent.VK_ENTER || k == KeyEvent.VK_SPACE) {
                    activateMenuBtn();
                } else if (k == KeyEvent.VK_M) {
                    repaint();
                } else if (k == KeyEvent.VK_T) {
                    wrapMode = !wrapMode; repaint();
                } else if (k == KeyEvent.VK_ESCAPE) {
                    System.exit(0);
                }
                break;
            case RUNNING:
                if      (k==KeyEvent.VK_UP   ||k==KeyEvent.VK_W) inputBuffer.push(Snake.UP);
                else if (k==KeyEvent.VK_DOWN ||k==KeyEvent.VK_S) inputBuffer.push(Snake.DOWN);
                else if (k==KeyEvent.VK_LEFT ||k==KeyEvent.VK_A) inputBuffer.push(Snake.LEFT);
                else if (k==KeyEvent.VK_RIGHT||k==KeyEvent.VK_D) inputBuffer.push(Snake.RIGHT);
                else if (k==KeyEvent.VK_P) { state=State.PAUSED; gameTimer.stop(); repaint(); }
                else if (k==KeyEvent.VK_M) {  repaint(); }
                else if (k==KeyEvent.VK_T) { wrapMode=!wrapMode; snake.setWrapAround(wrapMode,COLS,ROWS); repaint(); }
                break;
            case PAUSED:
                if      (k==KeyEvent.VK_P) { state=State.RUNNING; gameTimer.start(); }
                else if (k==KeyEvent.VK_M) {  repaint(); }
                break;
            case GAME_OVER:
                if (k==KeyEvent.VK_SPACE) { initGame(); state=State.RUNNING; gameTimer.start(); }
                else if (k==KeyEvent.VK_ESCAPE) { initGame(); state=State.MENU; repaint(); }
                break;
        }
    }
    private void activateMenuBtn() {
        switch (menuBtn) {
            case 0: initGame(); state = State.RUNNING; gameTimer.start(); break;
            case 1: wrapMode = !wrapMode;  repaint(); break;
            case 2: state = State.SETTINGS; repaint(); break;
            case 3:  System.exit(0); break;
        }
    }

    // ── Mouse ────────────────────────────────────────────────────
    @Override
    public void mouseClicked(MouseEvent e) {
        if (state == State.MENU) {
            int mx = e.getX(), my = e.getY();
            for (int i = 0; i < MENU_BTN_COUNT; i++) {
                if (menuRects[i] != null && menuRects[i].contains(mx, my)) {
                    menuBtn = i;
                    activateMenuBtn();
                    return;
                }
            }
        }
        if (state == State.GAME_OVER) {
            int mx = e.getX(), my = e.getY() - HUD_H;
            if (goRects[0] != null && goRects[0].contains(mx, my)) {
                initGame(); state = State.RUNNING; gameTimer.start();
            } else if (goRects[1] != null && goRects[1].contains(mx, my)) {
                initGame(); state = State.MENU; repaint();
            }
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (state == State.MENU) {
            int mx = e.getX(), my = e.getY();
            int prev = menuHover;
            menuHover = -1;
            for (int i = 0; i < MENU_BTN_COUNT; i++) {
                if (menuRects[i] != null && menuRects[i].contains(mx, my)) {
                    menuHover = i;
                    break;
                }
            }
            setCursor(menuHover >= 0
                    ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    : Cursor.getDefaultCursor());
            if (menuHover != prev) repaint();
        } else if (state == State.SETTINGS) {
            setCursor(Cursor.getDefaultCursor());
            int mx = e.getX(), my = e.getY() - HUD_H;
            boolean onBtn = (goRects[0] != null && goRects[0].contains(mx, my))
                    || (goRects[1] != null && goRects[1].contains(mx, my));
            setCursor(onBtn ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    : Cursor.getDefaultCursor());
        } else {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    @Override public void mousePressed(MouseEvent e)  {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e)  {}
    @Override public void mouseExited(MouseEvent e)   { menuHover = -1; repaint(); }
    @Override public void mouseDragged(MouseEvent e)  {}

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e)    {}
}