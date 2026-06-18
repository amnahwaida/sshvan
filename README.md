# 📱 SSH Tunnel Manager

> Aplikasi Android untuk membuat SSH Local Port Forwarding (tunnel) secara aman langsung dari perangkat mobile.

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![API](https://img.shields.io/badge/API-26%2B-brightgreen.svg)](https://developer.android.com/about/versions/oreo)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-blue.svg)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## 🌟 Tentang Aplikasi

**SSH Tunnel Manager** adalah aplikasi Android yang memungkinkan pengguna membuat *SSH Local Port Forwarding* (tunnel) langsung dari perangkat Android. Aplikasi ini berfungsi sebagai jembatan untuk mengakses layanan internal (server development, database, IoT, aplikasi web) yang tidak dapat diakses langsung dari jaringan publik.

Aplikasi ini dilengkapi dengan fitur integrasi **Hotspot** dan **Copy Link** untuk memudahkan pembagian akses ke perangkat lain (laptop/PC) yang terhubung ke Hotspot HP.

---

## ✨ Fitur Utama

- 🔐 **SSH Local Port Forwarding** — Konfigurasi SSH tunnel yang sepenuhnya dapat disesuaikan
- 🗂️ **Manajemen Profil** — Simpan, edit, dan hapus konfigurasi koneksi dengan nama kustom
- 🔑 **Dual Auth Support** — Autentikasi via Password & Private Key
- 📡 **Integrasi Hotspot** — Deteksi IP Hotspot otomatis & buka pengaturan Hotspot
- 📋 **Copy Link** — Salin link localhost atau Hotspot IP dengan satu tap
- 🔔 **Foreground Service** — Tunnel tetap aktif di background dengan notifikasi persisten
- 🌙 **Dark Mode** — Mendukung tema terang dan gelap
- 🔒 **Keamanan** — Kredensial dienkripsi menggunakan Android Keystore

---

## 🏗️ Arsitektur & Tech Stack

| Kategori | Teknologi |
|----------|-----------|
| **Bahasa** | Kotlin |
| **UI Framework** | Jetpack Compose + Material 3 |
| **Arsitektur** | MVVM + Clean Architecture |
| **SSH Library** | [JSch (mwiede fork)](https://github.com/mwiede/jsch) `0.2.18` |
| **DI** | Hilt / Dagger |
| **Database** | Room |
| **Navigation** | Jetpack Navigation Compose |
| **Async** | Kotlin Coroutines + Flow |
| **Preferences** | DataStore |
| **Security** | EncryptedSharedPreferences |

### Struktur Arsitektur

```
┌─────────────────────────────────────────┐
│           Presentation Layer            │
│  (Compose UI + ViewModel + StateFlow)   │
└──────────────────┬──────────────────────┘
                   │
┌──────────────────▼──────────────────────┐
│            Domain Layer                 │
│  (Use Cases + Repository Interfaces)    │
└──────────────────┬──────────────────────┘
                   │
┌──────────────────▼──────────────────────┐
│             Data Layer                  │
│  (Repository Impl + Room DAO + SSH)     │
└──────────────────┬──────────────────────┘
                   │
┌──────────────────▼──────────────────────┐
│         Infrastructure                  │
│  (JSch SSH Manager + Foreground Service)│
└─────────────────────────────────────────┘
```

---

## 📱 Screenshots

### Navigasi Aplikasi

```
SplashScreen
    └── MainActivity
        ├── HomeScreen (Daftar Profil + Status Tunnel)
        │   ├── [Tap Profil] → Detail / Connect
        │   └── [FAB +] → EditProfileScreen
        ├── EditProfileScreen (Form Tambah/Edit)
        └── SettingsScreen (Tema, About, dll)
```

---

## 🚀 Cara Menggunakan

### Prasyarat
- Perangkat Android dengan **API 26+ (Android 8.0 Oreo)** atau lebih tinggi
- Koneksi internet untuk terhubung ke server SSH

### Instalasi
1. Download file **APK** dari [Releases](https://github.com/amnahwaida/sshvan/releases)
2. Buka file APK di perangkat Android
3. Izinkan instalasi dari sumber tidak dikenal jika diminta
4. Buka aplikasi **SSH Tunnel Manager**

### Langkah Penggunaan
1. **Buat Profil Baru** — Tap tombol `+` di HomeScreen
2. **Isi Konfigurasi** — Masukkan detail server SSH:
   - Host/IP server
   - Port SSH (default: 22)
   - Username
   - Password atau Private Key
   - Local Port (default: 8080)
   - Remote Host (default: localhost)
   - Remote Port (default: 3000)
3. **Simpan & Connect** — Simpan profil dan tap untuk menghubungkan
4. **Akses Tunnel** — Buka browser di `http://localhost:<localPort>`
5. **Share via Hotspot** — Aktifkan Hotspot, lalu copy Hotspot Link untuk dibagikan ke laptop/PC lain

---

## 🛠️ Build dari Source

### Prasyarat
- **Android Studio** Hedgehog (2023.1.1) atau lebih baru
- **JDK 17**
- **Android SDK** API 35

### Clone & Build

```bash
# Clone repository
git clone https://github.com/amnahwaida/sshvan.git
cd sshvan

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# APK output berada di:
# Debug:   app/build/outputs/apk/debug/app-debug.apk
# Release: app/build/outputs/apk/release/app-release-unsigned.apk
```

---

## 📂 Struktur Proyek

```
sshvan/
├── app/
│   └── src/main/
│       ├── java/com/sshvan/tunnelmanager/
│       │   ├── TunnelManagerApp.kt          # Application class
│       │   ├── data/                        # Data layer (Repository impl, DAO, etc.)
│       │   ├── di/                          # Hilt dependency injection modules
│       │   ├── domain/                      # Domain layer (models, use cases)
│       │   ├── presentation/                # UI layer
│       │   │   └── ui/
│       │   │       ├── MainActivity.kt
│       │   │       ├── home/                # HomeScreen
│       │   │       ├── profile/             # EditProfileScreen
│       │   │       ├── settings/            # SettingsScreen
│       │   │       ├── components/          # Reusable UI components
│       │   │       ├── navigation/          # Navigation graph
│       │   │       └── theme/               # Material 3 theme
│       │   ├── service/                     # Foreground service untuk tunnel
│       │   └── util/                        # Utility classes
│       ├── res/                             # Android resources
│       └── AndroidManifest.xml
├── gradle/
├── build.gradle.kts                         # Root build config
├── settings.gradle.kts
├── prd.md                                   # Product Requirements Document
└── README.md
```

---

## 📄 Permissions

Aplikasi ini membutuhkan permission berikut:

| Permission | Kegunaan |
|-----------|----------|
| `INTERNET` | Koneksi SSH ke server |
| `ACCESS_NETWORK_STATE` | Monitoring status jaringan |
| `FOREGROUND_SERVICE` | Menjalankan tunnel di background |
| `WAKE_LOCK` | Menjaga CPU aktif saat tunnel berjalan |
| `POST_NOTIFICATIONS` | Notifikasi status koneksi (Android 13+) |
| `ACCESS_WIFI_STATE` | Deteksi IP Hotspot |

---

## 🤝 Kontribusi

Kontribusi sangat diterima! Silakan:

1. Fork repository ini
2. Buat branch fitur (`git checkout -b feature/fitur-baru`)
3. Commit perubahan (`git commit -m 'Tambah fitur baru'`)
4. Push ke branch (`git push origin feature/fitur-baru`)
5. Buat Pull Request

---

## 📜 Lisensi

Proyek ini dilisensikan di bawah [MIT License](LICENSE).

---

## 👨‍💻 Pengembang

Dikembangkan oleh **amnahwaida**

---

<p align="center">
  Made with ❤️ using Kotlin & Jetpack Compose
</p>
