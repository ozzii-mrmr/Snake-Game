# 🐍 Yılan Oyunu — Veri Yapıları Projesi

Java Swing ile geliştirilmiş, **LinkedList** ve **HashMap** veri yapılarını kullanan klasik Yılan oyunu.

---

## 📁 Dosya Yapısı

```
snake-game/
└── src/
    ├── Main.java        → Başlangıç noktası (entry point)
    ├── GameWindow.java  → JFrame penceresi
    ├── GamePanel.java   → Oyun döngüsü, çizim, klavye girişi
    ├── Snake.java       → LinkedList ile yılan gövdesi
    ├── Food.java        → HashMap ile yiyecek yönetimi
    └── Point.java       → Koordinat sınıfı (HashMap key'i)
```

---

## 🏗️ Veri Yapıları

### LinkedList — `Snake.java`
```
LinkedList<Point> body
  [0] → Baş (head)    ← addFirst() ile eklenir  O(1)
  [1] → Gövde
  ...
  [n] → Kuyruk (tail) ← removeLast() ile çıkarılır O(1)
```

Her hareket adımında:
1. `addFirst(newHead)` → Yeni baş öne eklenir
2. `removeLast()` → Eski kuyruk çıkarılır (yiyecek yenilmediyse)

**Neden ArrayList değil?**  
ArrayList'te `add(0, e)` → O(n) (tüm elemanlar kaydırılır)  
LinkedList'te `addFirst(e)` → O(1) (sadece pointer değişir)

---

### HashMap — `Food.java`
```
HashMap<Point, FoodType>
  key   → Point (konum)
  value → FoodType (NORMAL / BONUS / SUPER)
```

Her adımda "bu hücrede yiyecek var mı?" kontrolü:
- `foodMap.get(point)` → **O(1)**
- Array/List araması → O(n) olurdu

**Point için `equals()` ve `hashCode()` neden gerekli?**  
HashMap, anahtarları karşılaştırmak için bu metodları kullanır.  
Aynı koordinatları taşıyan iki farklı `Point` nesnesi eşit sayılabilmeli.

---

## 🎮 Kontroller

| Tuş | Eylem |
|-----|-------|
| ↑ ↓ ← → veya W A S D | Yılanı yönlendir |
| SPACE / ENTER | Oyunu başlat / Yeniden oyna |
| P | Duraklat / Devam et |
| ESC | Menüye dön |

---

## 🍎 Yiyecek Türleri

| Tür | Puan | Açıklama |
|-----|------|----------|
| 🔴 Normal | +10 | Her zaman var |
| ⭐ Bonus  | +30 | 100 adım sonra kaybolur, %20 şans |
| 💜 Süper  | +50 | Çok nadir, %10 şans |

---

## ⚙️ Derleme ve Çalıştırma

### Terminal ile:
```bash
# src klasörüne gir
cd snake-game/src

# Derle
javac *.java

# Çalıştır
java Main
```

### IDE ile (IntelliJ / Eclipse):
1. `src/` klasörünü proje kaynak dizini olarak ayarla
2. `Main.java` dosyasını çalıştır

---

## 🔍 Öğrenilen Kavramlar

- `LinkedList` ile O(1) başa ekleme / kuyruktan silme
- `HashMap` ile O(1) anahtar sorgulama
- `equals()` ve `hashCode()` kontratı
- Swing ile temel GUI ve oyun döngüsü (`javax.swing.Timer`)
- OOP: sınıf sorumluluklarının ayrılması

---

*Veri Yapıları Dersi — Java ile Yılan Oyunu Projesi*
