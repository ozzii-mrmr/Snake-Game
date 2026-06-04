package game;

import javax.sound.sampled.*;
import datastructures.MyHashMap;

/**
 * SoundManager.java
 *
 * VERİ YAPISI: MyHashMap<SoundType, Clip>  (java.util.HashMap yerine)
 *   Ses adına göre O(1) erişim.
 */
public class SoundManager {

    public enum SoundType {
        EAT_NORMAL, EAT_BONUS, EAT_SUPER,
        DEATH, LEVEL_UP, MENU_TICK
    }

    private final MyHashMap<SoundType, Clip> clips = new MyHashMap<>();
    private boolean muted = false;

    public SoundManager() {
        generate(SoundType.EAT_NORMAL, 880,  60,  WaveType.SINE);
        generate(SoundType.EAT_BONUS,  1200, 90,  WaveType.SINE);
        generate(SoundType.EAT_SUPER,  1500, 130, WaveType.SINE);
        generate(SoundType.DEATH,      200,  300, WaveType.SQUARE);
        generate(SoundType.LEVEL_UP,   0,    400, WaveType.CHORD);
        generate(SoundType.MENU_TICK,  660,  40,  WaveType.SINE);
    }

    public void play(SoundType type) {
        if (muted) return;
        Clip clip = clips.get(type);
        if (clip == null) return;
        clip.setFramePosition(0);
        clip.start();
    }

    public void toggleMute() { muted = !muted; }
    public boolean isMuted()  { return muted; }

    private enum WaveType { SINE, SQUARE, CHORD }

    private void generate(SoundType type, int freqHz, int durationMs, WaveType wave) {
        try {
            int sampleRate = 44100;
            int samples    = sampleRate * durationMs / 1000;
            byte[] buf     = new byte[samples * 2];

            for (int i = 0; i < samples; i++) {
                double t = (double) i / sampleRate, val = 0;
                switch (wave) {
                    case SINE:   val = Math.sin(2*Math.PI*freqHz*t); break;
                    case SQUARE: val = Math.sin(2*Math.PI*freqHz*t) >= 0 ? 1 : -1; break;
                    case CHORD:
                        val = (Math.sin(2*Math.PI*523*t)
                                + Math.sin(2*Math.PI*659*t)
                                + Math.sin(2*Math.PI*784*t)) / 3.0;
                        break;
                }
                double env = (i > samples*0.7) ? 1.0-(i-samples*0.7)/(samples*0.3) : 1.0;
                short s = (short)(val * env * 28000);
                buf[i*2]   = (byte)(s & 0xFF);
                buf[i*2+1] = (byte)((s >> 8) & 0xFF);
            }

            AudioFormat fmt  = new AudioFormat(sampleRate, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(Clip.class, fmt);
            if (!AudioSystem.isLineSupported(info)) return;
            Clip clip = (Clip) AudioSystem.getLine(info);
            clip.open(fmt, buf, 0, buf.length);
            clips.put(type, clip);
        } catch (Exception ignored) {}
    }

    public void dispose() {
        for (MyHashMap.Entry<SoundType, Clip> e : clips.entries()) {
            e.value.close();
        }
        clips.clear();
    }
}