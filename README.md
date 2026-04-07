# 🔥 Override Alarm

**Seni gerçekten uyandıran alarm uygulaması.**

Override Alarm, standart alarm uygulamalarının yetersiz kaldığı durumlarda devreye giren, gelişmiş özelliklere sahip bir alarm uygulamasıdır. Kotlin Multiplatform & Compose Multiplatform ile geliştirilmiştir.

---

## ✨ Özellikler

### 🧮 Susturma Görevleri (Challenge)
Alarmı susturmak için **görev çözme** zorunluluğu. 3 farklı mod:
- **Matematik** — Rastgele matematik sorusu çöz
- **Sallama** — Telefonu belirli süre salla
- **Yazma** — Ekranda gösterilen metni doğru yaz

### 💤 Erteleme (Snooze)
Alarm erteleme özelliği, **süre özelleştirilebilir** (varsayılan 5 dk). İsteğe bağlı olarak tamamen kapatılabilir.

### 🔁 Tekrarlayan Alarm
Haftanın **belirli günlerini** seçerek tekrarlayan alarm kurulabilir (Pzt-Paz).

### 📳 Titreşim
Alarm çalarken titreşim açılıp kapatılabilir.

### 🔔 Zil Sesi Seçimi
Cihazın zil sesi kütüphanesinden **özel alarm sesi** seçilebilir. Seçilmezse sistem varsayılanı kullanılır.

### 🏷️ Etiket
Her alarma **özel etiket** eklenebilir (ör: "İşe Git", "İlaç Saati").

### 🔊 Kulaklık Bypass (Zorunlu Hoparlör)
Kulaklık takılı olsa bile alarm sesini **doğrudan cihaz hoparlöründen** çalar. Gece kulaklıkla müzik dinlerken uyuyakalanlar için tasarlandı.

### 📸 Flaş Strobe
Alarm çaldığında cihazın flaşı ~4Hz hızında **yanıp söner**. Karanlık odada görsel uyarı sağlar.

### 🗣️ Sesli Uyarı (TTS)
Alarm tetiklendiğinde **saati ve özel mesajı sesli olarak okur**. Text-to-Speech motoru ile `STREAM_ALARM` kanalından çalar.

### ⏰ Uyanıklık Doğrulaması
Alarmı susturduktan sonra **5 dakika sessiz zamanlayıcı** başlar. "Uyanık mısın?" sorusuna 30 saniye içinde yanıt verilmezse alarm **en yüksek sesle tekrar çalar** — erteleme hakkı olmadan.

### 🎨 Tema Desteği
Ayarlar sayfasından **Sistem / Açık / Koyu** tema seçimi yapılabilir. Uygulama anında tema değiştirir.

### ⚙️ Ayarlar Sayfası
- **Genel:** 24 saat formatı, varsayılan titreşim
- **Override Varsayılanlar:** Kulaklık bypass, flaş strobe, TTS, uyanıklık doğrulaması
- **Alarm Davranışı:** Otomatik susturma süresi (1-30 dk)
- **Hakkında:** Uygulama sürümü, geliştirici bilgileri

### 🗑️ Animasyonlu Silme
Alarm silinirken **onay dialogu** çıkar, onaylandıktan sonra kart **sola kayarak + solarak** animasyonla kaybolur.

### 🎯 Material 3 UI
- Native **ripple efektleri** tüm buton ve kartlarda
- **Snackbar** bildirimleri (kaydetme/güncelleme)
- Modern ve premium tasarım

---

## 🛠️ Teknoloji

| Katman | Teknoloji |
|---|---|
| **Framework** | Kotlin Multiplatform + Compose Multiplatform |
| **UI** | Material 3, Jetpack Compose |
| **Alarm** | AlarmManager, Foreground Service |
| **Ses** | AudioManager, MediaPlayer, TextToSpeech |
| **Kamera** | CameraManager (Flaş Strobe) |
| **Optimizasyon** | R8 minification, ProGuard |

---

## 📦 Derleme

### Android

```shell
# Debug
./gradlew :composeApp:assembleDebug

# Release (R8 minified)
./gradlew :composeApp:assembleRelease
```

### iOS

`/iosApp` klasörünü Xcode'da açıp çalıştırın.

---

## 📁 Proje Yapısı

```
composeApp/
├── src/
│   ├── commonMain/          # Paylaşılan kod
│   │   └── kotlin/
│   │       ├── model/       # Alarm veri modeli
│   │       ├── repository/  # Repository & Scheduler
│   │       ├── ui/
│   │       │   ├── screens/ # AlarmList, AddAlarm, Settings
│   │       │   ├── components/ # AlarmCard
│   │       │   └── theme/   # Renk, tema (Light/Dark)
│   │       └── App.kt       # Ana navigasyon
│   └── androidMain/         # Android-specific
│       └── alarm/           # AlarmService, Receiver, Scheduler
```

---

## 📄 Lisans

MIT License

---

**Geliştirici:** Ali Yasir Naç