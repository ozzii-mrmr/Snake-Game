import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class GamePanel extends JPanel implements ActionListener, KeyListener {

    // ── Boyutlar ─────────────────────────────────────────────────
    static final int CELL    = 26;
    static final int COLS    = 24;
    static final int ROWS    = 24;
    static final int GRID_W  = CELL * COLS;
    static final int GRID_H  = CELL * ROWS;
    static final int HUD_H   = 56;
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
    private final Font fLabel = new Font(FF, Font.BOLD,  13);
    private final Font fNum   = new Font(FF, Font.BOLD,  22);
    private final Font fTitle = new Font(FF, Font.BOLD,  42);
    private final Font fBody  = new Font(FF, Font.PLAIN, 13);
    private final Font fSmall = new Font(FF, Font.PLAIN, 11);
    private final Font fBtn   = new Font(FF, Font.BOLD,  14);
    private final Font fPopup = new Font(FF, Font.BOLD,  16);
    private final Font fTag   = new Font(FF, Font.BOLD,  10);

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
    private SoundManager    sound;

    private javax.swing.Timer gameTimer;
    private javax.swing.Timer animTimer;

    // ── Seçenekler (menüden toggle edilir) ───────────────────────
    private boolean wrapMode = false;

    // ── Durum ────────────────────────────────────────────────────
    private enum State { MENU, RUNNING, PAUSED, GAME_OVER }
    private State state = State.MENU;

    private int score    = 0;
    private int steps    = 0;
    private int level    = 1;
    private int animTick = 0;

    // Death shake
    private int shakeFrames = 0, shakeX = 0, shakeY = 0;
    private final Random rng = new Random();

    // Level-up banner
    private int levelUpTimer = 0;
    private static final int LU_DURATION = 90;

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

        scoreManager = new ScoreManager();
        sound        = new SoundManager();

        gameTimer = new javax.swing.Timer(currentSpeed, this);
        animTimer = new javax.swing.Timer(25, e -> {
            animTick++;
            if (shakeFrames > 0) {
                shakeFrames--;
                shakeX = (shakeFrames > 0) ? rng.nextInt(7) - 3 : 0;
                shakeY = (shakeFrames > 0) ? rng.nextInt(7) - 3 : 0;
            }
            if (levelUpTimer > 0) levelUpTimer--;
            if (state != State.RUNNING) repaint();
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
        repaint();
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
            sound.play(eaten == Food.FoodType.NORMAL ? SoundManager.SoundType.EAT_NORMAL
                    : eaten == Food.FoodType.BONUS  ? SoundManager.SoundType.EAT_BONUS
                    : SoundManager.SoundType.EAT_SUPER);
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
    }

    private void applyPowerUp(PowerUpManager.PowerUpType type, Point pos) {
        sound.play(SoundManager.SoundType.EAT_BONUS);
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
            sound.play(SoundManager.SoundType.LEVEL_UP);
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
        sound.play(SoundManager.SoundType.DEATH);
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

        drawHUD(g2);
        g2.translate(0, HUD_H);
        drawGrid(g2);

        if (state == State.MENU) { drawMenuOverlay(g2); g2.dispose(); return; }

        drawObstacles(g2);
        drawFood(g2);
        drawPowerUps(g2);
        drawSnake(g2);
        drawPopups(g2);
        if (levelUpTimer > 0) drawLevelUpBanner(g2);
        drawActiveEffects(g2);

        if (state == State.PAUSED)    drawPauseOverlay(g2);
        if (state == State.GAME_OVER) drawGameOverOverlay(g2);

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
        g2.setColor(C_HUD_BG);
        g2.fillRect(0, 0, PANEL_W, HUD_H);
        g2.setColor(C_HUD_DIV);
        g2.setStroke(new BasicStroke(1f));
        g2.drawLine(0, HUD_H - 1, PANEL_W, HUD_H - 1);

        hudStat     (g2, "PUAN",    String.valueOf(score), C_ACCENT, 24);
        drawCenteredStr(g2, fLabel, "EN YÜKSEK", PANEL_W/2, 22, C_TEXT_MUTED);
        drawCenteredStr(g2, fNum,   String.valueOf(scoreManager.getHighScore()), PANEL_W/2, 44, C_WARN);
        hudStatRight(g2, "SEVİYE", String.valueOf(level), C_INDIGO, PANEL_W - 24);

        // Ses + wrap göstergesi
        g2.setFont(fSmall);
        g2.setColor(sound.isMuted() ? C_TEXT_MUTED : C_ACCENT);
        g2.drawString(sound.isMuted() ? "🔇" : "🔊", PANEL_W - 44, HUD_H - 8);
        if (wrapMode) {
            g2.setColor(C_INDIGO);
            g2.drawString("WRAP", PANEL_W - 80, HUD_H - 8);
        }

        // Kalkan göstergesi (HUD'da belirgin)
        if (state == State.RUNNING && powerUps != null
                && powerUps.isActive(PowerUpManager.PowerUpType.SHIELD)) {
            g2.setFont(fSmall);
            g2.setColor(C_PU_SHIELD);
            g2.drawString("🛡", 4, HUD_H - 8);
        }

        // Seviye ilerleme çubuğu
        float progress = Math.min(1f, (float)(score - (level - 1) * 80) / 80f);
        g2.setColor(new Color(255, 255, 255, 12));
        g2.fillRect(0, HUD_H - 4, PANEL_W, 3);
        if (progress > 0) {
            Color barC = powerUps != null && powerUps.isActive(PowerUpManager.PowerUpType.MAGNET)
                    ? C_PU_MAGNET : C_ACCENT;
            g2.setColor(barC);
            g2.fillRect(0, HUD_H - 4, (int)(PANEL_W * progress), 3);
        }
    }

    private void hudStat(Graphics2D g2, String lbl, String val, Color vc, int x) {
        g2.setFont(fLabel); g2.setColor(C_TEXT_MUTED); g2.drawString(lbl, x, 22);
        g2.setFont(fNum);   g2.setColor(vc);            g2.drawString(val, x, 44);
    }

    private void hudStatRight(Graphics2D g2, String lbl, String val, Color vc, int rx) {
        g2.setFont(fLabel); int lw = g2.getFontMetrics().stringWidth(lbl);
        g2.setColor(C_TEXT_MUTED); g2.drawString(lbl, rx - lw, 22);
        g2.setFont(fNum); int vw = g2.getFontMetrics().stringWidth(val);
        g2.setColor(vc); g2.drawString(val, rx - vw, 44);
    }

    // ── Grid ─────────────────────────────────────────────────────
    private void drawGrid(Graphics2D g2) {
        g2.setColor(C_BG); g2.fillRect(0, 0, GRID_W, GRID_H);
        g2.setColor(C_GRID); g2.setStroke(new BasicStroke(0.5f));
        for (int c = 1; c < COLS; c++) g2.drawLine(c*CELL, 0, c*CELL, GRID_H);
        for (int r = 1; r < ROWS; r++) g2.drawLine(0, r*CELL, GRID_W, r*CELL);

        // Wrap modunda kenar vurgusu
        if (wrapMode) {
            g2.setColor(new Color(C_INDIGO.getRed(), C_INDIGO.getGreen(), C_INDIGO.getBlue(), 60));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRect(0, 0, GRID_W - 1, GRID_H - 1);
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

        // Kalkan renk tonu
        boolean shielded = powerUps.isActive(PowerUpManager.PowerUpType.SHIELD);
        Color headColor = shielded
                ? blendColor(C_SNAKE_HEAD, C_PU_SHIELD, 0.5f) : C_SNAKE_HEAD;
        Color glowColor = shielded
                ? new Color(250, 204, 21, 50) : C_SNAKE_GLOW;

        // Glow
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.18f));
        g2.setColor(glowColor);
        g2.setStroke(new BasicStroke(sw + 10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(snakePath(body));
        g2.setComposite(orig);

        // Gövde
        g2.setStroke(new BasicStroke(sw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = n - 1; i >= 1; i--) {
            float t = (float) i / (n - 1);
            g2.setColor(blendColor(C_SNAKE_BODY, C_SNAKE_TAIL, t));
            Point a = body.get(i), b = body.get(i-1);
            g2.drawLine(cx(a.x), cy(a.y), cx(b.x), cy(b.y));
        }

        // Baş
        Point head = body.get(0);
        float hr = sw / 2f + 1f;
        g2.setColor(headColor);
        g2.fill(new Ellipse2D.Float(cx(head.x)-hr, cy(head.y)-hr, hr*2, hr*2));
        g2.setColor(new Color(255,255,255,80));
        float gr = hr * 0.35f;
        g2.fill(new Ellipse2D.Float(cx(head.x)-hr*0.5f, cy(head.y)-hr*0.55f, gr*2, gr*2));
        drawEyes(g2, head, sw);

        // Kalkan halkası
        if (shielded) {
            g2.setColor(new Color(250, 204, 21, 120));
            g2.setStroke(new BasicStroke(2f));
            g2.draw(new Ellipse2D.Float(cx(head.x)-hr-3, cy(head.y)-hr-3, (hr+3)*2, (hr+3)*2));
        }
    }

    private Path2D.Float snakePath(java.util.LinkedList<Point> body) {
        Path2D.Float path = new Path2D.Float();
        path.moveTo(cx(body.get(0).x), cy(body.get(0).y));
        for (int i = 1; i < body.size(); i++)
            path.lineTo(cx(body.get(i).x), cy(body.get(i).y));
        return path;
    }

    private void drawEyes(Graphics2D g2, Point head, float sw) {
        int dir = snake.getDirection();
        float er = 2.8f, dist = sw * 0.28f;
        float hcx = cx(head.x), hcy = cy(head.y);
        float ex1,ey1,ex2,ey2;
        switch (dir) {
            case Snake.RIGHT: ex1=hcx+dist*.3f;ey1=hcy-dist;ex2=hcx+dist*.3f;ey2=hcy+dist;break;
            case Snake.LEFT:  ex1=hcx-dist*.3f;ey1=hcy-dist;ex2=hcx-dist*.3f;ey2=hcy+dist;break;
            case Snake.UP:    ex1=hcx-dist;ey1=hcy-dist*.3f;ex2=hcx+dist;ey2=hcy-dist*.3f;break;
            default:          ex1=hcx-dist;ey1=hcy+dist*.3f;ex2=hcx+dist;ey2=hcy+dist*.3f;break;
        }
        g2.setColor(new Color(10,16,30));
        g2.fill(new Ellipse2D.Float(ex1-er,ey1-er,er*2,er*2));
        g2.fill(new Ellipse2D.Float(ex2-er,ey2-er,er*2,er*2));
        g2.setColor(new Color(255,255,255,200));
        float pr = er * 0.45f;
        g2.fill(new Ellipse2D.Float(ex1-pr,ey1-pr*1.2f,pr*2,pr*2));
        g2.fill(new Ellipse2D.Float(ex2-pr,ey2-pr*1.2f,pr*2,pr*2));
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
        g2.setColor(new Color(8,10,18,230)); g2.fillRect(0,0,GRID_W,GRID_H);

        int cw=360,ch=380,cardX=GRID_W/2-cw/2,cardY=GRID_H/2-ch/2;
        drawCard(g2,cardX,cardY,cw,ch,new Color(20,24,38),new Color(52,211,153,50));

        g2.setFont(fTitle); g2.setColor(C_ACCENT);
        drawCenteredStr(g2,fTitle,"SNAKE",GRID_W/2,cardY+58,C_ACCENT);
        drawCenteredStr(g2,fSmall,"Veri Yapıları Projesi",GRID_W/2,cardY+76,new Color(100,116,139));

        // Divider
        g2.setColor(new Color(255,255,255,15)); g2.fillRect(cardX+30,cardY+90,cw-60,1);

        // Kontroller
        int ix=cardX+36,iy=cardY+112;
        drawKeyRow(g2,"↑ ↓ ← →  /  WASD","Hareket",  ix,iy);
        drawKeyRow(g2,"P",               "Duraklat",  ix,iy+28);
        drawKeyRow(g2,"M",               "Ses",       ix,iy+56);

        // Wrap-around toggle
        int wrapBtnW=cw-60, wrapBtnX=cardX+30, wrapBtnY=iy+84;
        Color wrapC = wrapMode ? C_INDIGO : new Color(55,65,90);
        drawRoundedButton(g2,wrapBtnX,wrapBtnY,wrapBtnW,30,
                (wrapMode?"✓ ":"  ")+"Duvar Geçişi (T tuşu)  —  "+(wrapMode?"AÇIK":"KAPALI"), wrapC);

        // Divider
        g2.setColor(new Color(255,255,255,15)); g2.fillRect(cardX+30,cardY+250,cw-60,1);

        // Yiyecek & güç-up göstergesi
        int fy=cardY+268;
        foodChip(g2,cardX+36,     fy,C_FOOD_N,"Normal +10");
        foodChip(g2,cardX+140,    fy,C_FOOD_B,"Bonus +30");
        foodChip(g2,cardX+240,    fy,C_FOOD_S,"Süper +50");
        fy+=22;
        foodChip(g2,cardX+36,     fy,C_PU_SLOW,  "Yavaş");
        foodChip(g2,cardX+110,    fy,C_PU_SHIELD,"Kalkan");
        foodChip(g2,cardX+184,    fy,C_PU_SHRINK,"Küçült");
        foodChip(g2,cardX+258,    fy,C_PU_MAGNET,"2×Puan");

        // En iyi skorlar
        if (scoreManager.size()>0) {
            g2.setColor(new Color(255,255,255,15)); g2.fillRect(cardX+30,cardY+318,cw-60,1);
            g2.setFont(fSmall); g2.setColor(C_TEXT_MUTED);
            g2.drawString("En İyi Skorlar",cardX+36,cardY+334);
            int[] tops=scoreManager.getTopScores();
            for (int i=0;i<Math.min(tops.length,3);i++) {
                g2.setColor(i==0?C_WARN:C_TEXT_MUTED);
                g2.drawString((i+1)+".  "+tops[i],cardX+36+i*95,cardY+350);
            }
        }

        // Başla butonu
        double pulse=Math.sin(animTick*0.07)*0.5+0.5;
        Color btnC=blendColor(new Color(30,100,65),C_ACCENT,(float)pulse);
        int btnW=220,btnH=40,btnX=GRID_W/2-btnW/2,btnY=cardY+ch-54;
        drawRoundedButton(g2,btnX,btnY,btnW,btnH,"Başla  —  Space / Enter",btnC);
    }

    private void drawPauseOverlay(Graphics2D g2) {
        g2.setColor(new Color(8,10,18,200)); g2.fillRect(0,0,GRID_W,GRID_H);
        int cw=300,ch=140,cx=GRID_W/2-cw/2,cy=GRID_H/2-ch/2;
        drawCard(g2,cx,cy,cw,ch,new Color(20,24,38),new Color(129,140,248,60));
        drawCenteredStr(g2,new Font(FF,Font.BOLD,22),"Duraklatıldı",GRID_W/2,cy+46,C_INDIGO);
        drawCenteredStr(g2,fBody,"P  →  Devam",GRID_W/2,cy+78,C_TEXT_MUTED);
        drawCenteredStr(g2,fBody,"M  →  Ses Aç/Kapat",GRID_W/2,cy+100,C_TEXT_MUTED);
    }

    private void drawGameOverOverlay(Graphics2D g2) {
        g2.setColor(new Color(8,10,18,215)); g2.fillRect(0,0,GRID_W,GRID_H);
        int cw=320,ch=350,cx=GRID_W/2-cw/2,cy=GRID_H/2-ch/2;
        drawCard(g2,cx,cy,cw,ch,new Color(20,24,38),new Color(248,113,113,55));

        drawCenteredStr(g2,new Font(FF,Font.BOLD,26),"Oyun Bitti",GRID_W/2,cy+46,C_DANGER);
        g2.setColor(new Color(255,255,255,15)); g2.fillRect(cx+28,cy+60,cw-56,1);

        int sy=cy+88,sh=36;
        statRow(g2,cx,sy,      cw,"Puan",      String.valueOf(score),             C_ACCENT);
        statRow(g2,cx,sy+sh,   cw,"Seviye",    String.valueOf(level),             C_INDIGO);
        statRow(g2,cx,sy+sh*2, cw,"Uzunluk",   String.valueOf(snake.getLength()), C_TEXT);

        g2.setColor(new Color(255,255,255,15)); g2.fillRect(cx+28,sy+sh*3+4,cw-56,1);
        g2.setFont(fSmall); g2.setColor(C_TEXT_MUTED);
        g2.drawString("En İyi Skorlar",cx+28,sy+sh*3+22);
        int[] tops=scoreManager.getTopScores();
        for (int i=0;i<Math.min(tops.length,3);i++) {
            boolean isNew=i==0&&tops[i]==score;
            g2.setFont(fLabel); g2.setColor(isNew?C_WARN:i==0?C_WARN:C_TEXT_MUTED);
            g2.drawString((i+1)+".  "+tops[i]+(isNew?"  ✦ YENİ!":""),cx+28+i*90,sy+sh*3+40);
        }

        int bw=cw-56,bx=cx+28;
        drawRoundedButton(g2,bx,cy+ch-88,bw,36,"Yeniden Oyna  —  Space",C_ACCENT);
        drawRoundedButton(g2,bx,cy+ch-44,bw,32,"Ana Menü  —  Esc",new Color(55,65,90));
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
            case MENU:
                if (k==KeyEvent.VK_SPACE||k==KeyEvent.VK_ENTER) { initGame(); state=State.RUNNING; gameTimer.start(); }
                else if (k==KeyEvent.VK_T) { wrapMode=!wrapMode; repaint(); }
                else if (k==KeyEvent.VK_M) { sound.toggleMute(); repaint(); }
                break;
            case RUNNING:
                if      (k==KeyEvent.VK_UP   ||k==KeyEvent.VK_W) inputBuffer.push(Snake.UP);
                else if (k==KeyEvent.VK_DOWN ||k==KeyEvent.VK_S) inputBuffer.push(Snake.DOWN);
                else if (k==KeyEvent.VK_LEFT ||k==KeyEvent.VK_A) inputBuffer.push(Snake.LEFT);
                else if (k==KeyEvent.VK_RIGHT||k==KeyEvent.VK_D) inputBuffer.push(Snake.RIGHT);
                else if (k==KeyEvent.VK_P) { state=State.PAUSED; gameTimer.stop(); repaint(); }
                else if (k==KeyEvent.VK_M) { sound.toggleMute(); repaint(); }
                else if (k==KeyEvent.VK_T) { wrapMode=!wrapMode; snake.setWrapAround(wrapMode,COLS,ROWS); repaint(); }
                break;
            case PAUSED:
                if      (k==KeyEvent.VK_P) { state=State.RUNNING; gameTimer.start(); }
                else if (k==KeyEvent.VK_M) { sound.toggleMute(); repaint(); }
                break;
            case GAME_OVER:
                if (k==KeyEvent.VK_SPACE) { initGame(); state=State.RUNNING; gameTimer.start(); }
                else if (k==KeyEvent.VK_ESCAPE) { initGame(); state=State.MENU; repaint(); }
                break;
        }
    }
    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e)    {}
}