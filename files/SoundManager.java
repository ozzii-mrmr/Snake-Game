import javax.sound.sampled.*;
import java.util.HashMap;

/**
 * SoundManager.java
 *
 * Oyun seslerini ve arkaplan müziğini yönetir.
 * javax.sound.sampled kullanır — dış kütüphane gerektirmez.
 *
 * Ses dosyaları olmadığında programatik olarak
 * PCM dalgaları üretir (basit bip sesleri).
 * → Oyun .wav dosyası olmadan da ses çıkarır.
 *
 * VERİ YAPISI: HashMap<SoundType, Clip>
 *   Ses adına göre O(1) erişim.
 */
public class SoundManager {

    public enum SoundType {
        EAT_NORMAL,   // Normal yiyecek yenildi
        EAT_BONUS,    // Bonus yıldız yenildi
        EAT_SUPER,    // Süper elmas yenildi
        DEATH,        // Yılan öldü
        LEVEL_UP,     // Seviye atlandı
        MENU_TICK     // Menü gezintisi
    }

    private final HashMap<SoundType, Clip> clips = new HashMap<>();
    private boolean muted = false;

    // ── Kurucu ───────────────────────────────────────────────────
    public SoundManager() {
        generate(SoundType.EAT_NORMAL, 880,  60,  WaveType.SINE);
        generate(SoundType.EAT_BONUS,  1200, 90,  WaveType.SINE);
        generate(SoundType.EAT_SUPER,  1500, 130, WaveType.SINE);
        generate(SoundType.DEATH,      200,  300, WaveType.SQUARE);
        generate(SoundType.LEVEL_UP,   0,    400, WaveType.CHORD);
        generate(SoundType.MENU_TICK,  660,  40,  WaveType.SINE);
    }

    // ── Ses çalma ────────────────────────────────────────────────

    public void play(SoundType type) {
        if (muted) return;
        Clip clip = clips.get(type);
        if (clip == null) return;
        clip.setFramePosition(0);
        clip.start();
    }

    public void toggleMute() { muted = !muted; }
    public boolean isMuted()  { return muted; }

    // ── PCM dalga üretici ────────────────────────────────────────

    private enum WaveType { SINE, SQUARE, CHORD }

    /**
     * Verilen frekans ve süreyle PCM bayt dizisi üretir,
     * Clip'e yükler.
     */
    private void generate(SoundType type, int freqHz, int durationMs, WaveType wave) {
        try {
            int sampleRate = 44100;
            int samples    = sampleRate * durationMs / 1000;
            byte[] buf     = new byte[samples * 2]; // 16-bit stereo → mono

            for (int i = 0; i < samples; i++) {
                double t   = (double) i / sampleRate;
                double val = 0;

                switch (wave) {
                    case SINE:
                        val = Math.sin(2 * Math.PI * freqHz * t);
                        break;
                    case SQUARE:
                        val = Math.sin(2 * Math.PI * freqHz * t) >= 0 ? 1 : -1;
                        break;
                    case CHORD:
                        // Do-Mi-Sol akordu (seviye atlama)
                        val = Math.sin(2 * Math.PI * 523 * t)   // Do
                            + Math.sin(2 * Math.PI * 659 * t)   // Mi
                            + Math.sin(2 * Math.PI * 784 * t);  // Sol
                        val /= 3.0;
                        break;
                }

                // Envelope: sona doğru söndür (tıklama önleme)
                double env = 1.0;
                if (i > samples * 0.7) {
                    env = 1.0 - (i - samples * 0.7) / (samples * 0.3);
                }

                short s = (short)(val * env * 28000);
                buf[i * 2]     = (byte)(s & 0xFF);
                buf[i * 2 + 1] = (byte)((s >> 8) & 0xFF);
            }

            AudioFormat fmt = new AudioFormat(sampleRate, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(Clip.class, fmt);

            if (!AudioSystem.isLineSupported(info)) return;

            Clip clip = (Clip) AudioSystem.getLine(info);
            clip.open(fmt, buf, 0, buf.length);
            clips.put(type, clip);

        } catch (Exception ignored) {
            // Ses sistemi desteklenmiyorsa sessizce devam et
        }
    }

    /** Tüm clip'leri kapat (uygulama kapanırken). */
    public void dispose() {
        clips.values().forEach(Clip::close);
        clips.clear();
    }
}
