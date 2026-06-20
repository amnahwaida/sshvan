Berikut adalah file `prd.md` yang sudah saya susun secara detail dan terstruktur. Silakan langsung di-copy-paste ke file `prd.md` di repository proyek Anda.

```markdown
# 📱 Product Requirements Document (PRD)
## Aplikasi: Netvan for Android

| Metadata | Detail |
|----------|--------|
| **Nama Proyek** | Netvan |
| **Versi Dokumen** | 1.0.0 |
| **Tanggal** | 18 Juni 2026 |
| **Platform** | Android (Minimum API 26 / Android 8.0) |
| **Bahasa** | Kotlin |
| **UI Framework** | Jetpack Compose |
| **Arsitektur** | MVVM + Clean Architecture |
| **Status** | Draft / Brainstorming |

---

## 1. 📋 Ringkasan Eksekutif

**Netvan** adalah aplikasi Android yang memungkinkan pengguna membuat *SSH Local Port Forwarding* (tunnel) secara aman langsung dari perangkat mobile mereka. Aplikasi ini berfungsi sebagai jembatan untuk mengakses layanan internal (server development, database, IoT, aplikasi web) yang tidak dapat diakses langsung dari jaringan publik.

Aplikasi ini juga dilengkapi dengan fitur integrasi **Hotspot** dan **Copy Link** untuk memudahkan pembagian akses ke perangkat lain (laptop/PC) yang terhubung ke Hotspot HP.

---

## 2. 🎯 Latar Belakang & Masalah

### Masalah yang Dipecahkan:
1. Developer/IT sering perlu mengakses layanan internal (misal: `localhost:3000` di server kantor) dari HP atau laptop lain, tetapi tidak bisa langsung karena berada di jaringan berbeda.
2. Perintah SSH seperti `ssh -f -N -L 8080:localhost:3000 admin@192.168.1.50` hanya bisa dijalankan di terminal Linux/PC, tidak praktis di mobile.
3. Membagikan akses ke laptop via Hotspot membutuhkan konfigurasi IP yang manual dan membingungkan bagi user awam.

### Solusi:
Aplikasi mobile yang mengotomasi seluruh proses SSH tunneling dengan UI yang ramah pengguna, termasuk fitur *one-click copy link* untuk berbagai akses ke perangkat lain.

---

## 3. 👥 Target Pengguna

| Persona | Deskripsi | Kebutuhan Utama |
|---------|-----------|-----------------|
| **Developer** | Programmer yang perlu akses server dev/testing | Tunnel cepat, multi-profile |
| **Sysadmin** | Administrator sistem yang monitoring server | Koneksi stabil, auto-reconnect |
| **IoT Enthusiast** | Pengguna perangkat IoT/Smart Home | Akses device internal dari luar |
| **Remote Worker** | Karyawan yang WFH dan butuh akses jaringan kantor | Kemudahan penggunaan, sharing ke laptop |

---

## 4. 🚀 Ruang Lingkup Produk

### ✅ In Scope (Versi 1.0 / MVP)
- SSH Local Port Forwarding dengan konfigurasi penuh
- Autentikasi via Password & Private Key
- Penyimpanan multi-profile koneksi
- Foreground Service untuk background running
- Integrasi Hotspot (buka pengaturan)
- Copy Link (localhost & IP Hotspot)
- Notifikasi status koneksi

### ❌ Out of Scope (Versi Selanjutnya)
- Remote Port Forwarding (`-R`)
- Dynamic Port Forwarding / SOCKS Proxy (`-D`)
- SFTP File Transfer
- SSH Shell Interaktif
- Multi-hop / Jump Host berantai
- Root-only features

---

## 5. 📦 Fitur Utama (MVP)

### 5.1 🔐 Konfigurasi SSH Tunnel (Fully Customizable)

Setiap bagian dari perintah SSH dapat di-custom oleh pengguna:

| Variabel SSH | Field UI | Default Value | Contoh |
|--------------|----------|---------------|--------|
| SSH Host/IP | `sshHost` | - | `192.168.1.50` / `server.com` |
| SSH Port | `sshPort` | `22` | `22`, `2222` |
| Username | `username` | - | `admin` |
| Auth Method | `authType` | `PASSWORD` | `PASSWORD` / `PRIVATE_KEY` |
| Password / Key | `credential` | - | (input atau file picker) |
| Local Port | `localPort` | `8080` | `8080`, `3000` |
| Remote Host | `remoteHost` | `localhost` | `localhost`, `192.168.1.253` |
| Remote Port | `remotePort` | `3000` | `3000`, `5432` |

### 5.2 🗂️ Manajemen Profil Koneksi
- **Save Profile:** Simpan konfigurasi dengan nama kustom (misal: "Server Kantor", "VPS DO").
- **List Profile:** Tampilan daftar profil yang sudah disimpan.
- **Edit & Delete:** Ubah atau hapus profil yang ada.
- **Quick Connect:** Satu tap untuk connect dari profil yang tersimpan.

### 5.3 📡 Integrasi Hotspot
- **Tombol "Setup Hotspot":** Membuka halaman pengaturan Hotspot Android (`Settings.ACTION_TETHER_SETTINGS`).
- **Deteksi Status Hotspot:** Mendeteksi apakah Hotspot sedang aktif.
- **Catatan Teknis:** Karena Android 8.0+ membatasi API tethering, aplikasi **tidak** menyalakan hotspot otomatis, hanya mengarahkan ke settings.

### 5.4 📋 Copy Link (Quick Share)
Dua opsi link yang dapat disalin ke clipboard:

| Tipe Link | Format | Kapan Digunakan |
|-----------|--------|-----------------|
| **Local Link** | `http://localhost:<localPort>` | Akses dari HP Android itu sendiri |
| **Hotspot Link** | `http://<hotspot_ip>:<localPort>` | Akses dari laptop/HP lain via Hotspot |

### 5.5 🔔 Notifikasi & Status
- **Foreground Service Notification:** Notifikasi persisten saat tunnel aktif.
- **Status Indikator:** Visual (hijau/merah) + teks status.
- **Error Handling:** Pesan error yang jelas (timeout, auth failed, port in use, dll).

---

## 6. 📖 User Stories

### Epic 1: Koneksi Dasar
> **US-1.1:** Sebagai developer, saya ingin membuat SSH tunnel dengan konfigurasi kustom agar bisa mengakses server internal dari HP saya.
> 
> **US-1.2:** Sebagai user, saya ingin menyimpan konfigurasi koneksi sebagai profil agar tidak perlu mengetik ulang setiap kali connect.
> 
> **US-1.3:** Sebagai user, saya ingin aplikasi tetap berjalan di background saat HP dikunci agar tunnel tidak terputus.

### Epic 2: Sharing Akses
> **US-2.1:** Sebagai remote worker, saya ingin menyalin link akses dengan satu tap agar bisa langsung di-paste ke browser laptop.
> 
> **US-2.2:** Sebagai user, saya ingin diarahkan ke pengaturan Hotspot dengan cepat agar bisa membagikan koneksi ke laptop.
> 
> **US-2.3:** Sebagai user, saya ingin aplikasi mendeteksi IP Hotspot secara otomatis agar link yang disalin selalu akurat.

### Epic 3: Monitoring
> **US-3.1:** Sebagai user, saya ingin melihat status koneksi secara real-time (connected/disconnected/error).
> 
> **US-3.2:** Sebagai user, saya ingin menerima notifikasi jika koneksi tunnel terputus secara tidak terduga.

---

## 7. 🏗️ Spesifikasi Teknis

### 7.1 Tech Stack

| Kategori | Teknologi | Alasan |
|----------|-----------|--------|
| **Bahasa** | Kotlin 1.9+ | Modern, resmi untuk Android |
| **UI** | Jetpack Compose + Material 3 | Declarative, modern |
| **SSH Library** | `com.github.mwiede:jsch:0.2.18+` | Fork modern JSch, stabil untuk port forwarding |
| **Async** | Kotlin Coroutines + Flow | Non-blocking, lifecycle-aware |
| **DI** | Hilt / Dagger | Dependency injection |
| **Local Storage** | Room Database | Simpan profil koneksi |
| **Navigation** | Jetpack Navigation Compose | Navigasi antar screen |
| **Preferences** | DataStore | Simpan setting sederhana |

### 7.2 Arsitektur (MVVM + Clean Architecture)

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

### 7.3 Permission yang Dibutuhkan

```xml
<!-- Koneksi SSH -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- Foreground Service (Wajib untuk tunnel background) -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE" />

<!-- WakeLock agar CPU tidak tidur saat tunnel aktif -->
<uses-permission android:name="android.permission.WAKE_LOCK" />

<!-- Notifikasi (Android 13+) -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<!-- Akses info Hotspot (untuk deteksi IP) -->
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
```

### 7.4 Data Model

```kotlin
@Entity(tableName = "connection_profiles")
data class ConnectionProfile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,                    // "Server Kantor"
    val sshHost: String,                 // "192.168.1.50"
    val sshPort: Int = 22,               // 22
    val username: String,                // "admin"
    val authType: AuthType,              // PASSWORD / PRIVATE_KEY
    val password: String? = null,        // Encrypted
    val privateKeyPath: String? = null,  // File URI
    val localPort: Int = 8080,           // 8080
    val remoteHost: String = "localhost",// "localhost"
    val remotePort: Int = 3000,          // 3000
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsed: Long? = null
)

enum class AuthType { PASSWORD, PRIVATE_KEY }

enum class TunnelStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}
```

---

## 8. 🎨 Desain UI/UX

### 8.1 Struktur Navigasi

```
SplashScreen
    └── MainActivity
        ├── HomeScreen (Daftar Profil + Status Tunnel)
        │   ├── [Tap Profil] → TunnelDetailScreen
        │   └── [FAB +] → EditProfileScreen
        ├── EditProfileScreen (Form Tambah/Edit)
        └── SettingsScreen (Tema, About, dll)
```

### 8.2 Komponen Utama HomeScreen

1. **Status Card (Top)**
   - Icon besar (🟢/🔴)
   - Teks status: "Connected" / "Disconnected"
   - Info tunnel aktif: `localhost:8080 → 192.168.1.253:3000`

2. **Quick Actions (Jika Connected)**
   - 📋 Tombol **Copy Local Link**
   - 📡 Tombol **Copy Hotspot Link** (otomatis deteksi IP)
   - ⚙️ Tombol **Setup Hotspot** (buka settings)

3. **List Profil (Scrollable)**
   - Card per profil dengan nama, host, last used
   - Swipe to delete
   - Tap untuk connect / buka detail

4. **FAB (Floating Action Button)**
   - Tambah profil baru

### 8.3 Form EditProfileScreen

**Section 1: Profil**
- Input: Nama Profil (wajib)

**Section 2: Server SSH**
- Input: Host/IP
- Input: Port (default 22)
- Input: Username
- Dropdown: Auth Method (Password / Private Key)
- Input: Password **atau** Button: Pilih File Key

**Section 3: Port Forwarding**
- Input: Local Port (default 8080)
- Input: Remote Host (default localhost)
- Input: Remote Port (default 3000)

**[ Tombol: TEST CONNECTION ]** (opsional, untuk validasi)
**[ Tombol: SAVE ]**

---

## 9. ⚠️ Tantangan Teknis & Solusi

| Tantangan | Solusi |
|-----------|--------|
| **Android mematikan app di background** | Gunakan `ForegroundService` dengan notifikasi persisten |
| **CPU tidur (Doze Mode) memutus SSH** | Request `PARTIAL_WAKE_LOCK` saat tunnel aktif |
| **Hotspot API diblokir di Android 8+** | Arahkan ke `Settings.ACTION_TETHER_SETTINGS` |
| **Port sudah dipakai (Address in use)** | Validasi sebelum connect, tampilkan error jelas |
| **Koneksi putus saat sinyal hilang** | Implementasi auto-reconnect dengan exponential backoff |
| **Password tersimpan tidak aman** | Encrypt dengan `EncryptedSharedPreferences` / Android Keystore |
| **File key tidak bisa dibaca (scoped storage)** | Gunakan Storage Access Framework (SAF) + persist URI permission |

---

## 10. 🗺️ Roadmap Pengembangan

### 🔵 Fase 1: MVP (Minggu 1-3)
- [ ] Setup proyek + dependency
- [ ] UI HomeScreen & EditProfileScreen
- [ ] Room database untuk profil
- [ ] SSH Manager dengan JSch (connect + port forwarding)
- [ ] Foreground Service + WakeLock
- [ ] Notifikasi status

### 🟢 Fase 2: Fitur Sharing (Minggu 4)
- [ ] Copy Link (localhost & hotspot IP)
- [ ] Deteksi IP Hotspot
- [ ] Integrasi Settings Hotspot
- [ ] Quick Actions di HomeScreen

### 🟡 Fase 3: Polish (Minggu 5)
- [ ] Error handling yang lebih baik
- [ ] Auto-reconnect
- [ ] Validasi input (port range, format IP)
- [ ] Encrypt credential
- [ ] Dark mode support

### 🔴 Fase 4: Future (Post MVP)
- [ ] Widget layar utama
- [ ] Quick Settings tile
- [ ] Import/Export profil (JSON)
- [ ] Remote Port Forwarding
- [ ] SOCKS Proxy support

---

## 11. ✅ Kriteria Keberhasilan (Success Metrics)

| Metrik | Target |
|--------|--------|
| **Fungsi Inti** | Tunnel berhasil connect 99% dalam kondisi jaringan normal |
| **Stabilitas** | Tidak crash dalam 24 jam penggunaan terus-menerus |
| **Background** | Tunnel tetap aktif minimal 8 jam di background |
| **UX** | User bisa connect dalam < 3 tap dari HomeScreen |
| **Ukuran App** | APK < 10 MB |
| **Kompatibilitas** | Berjalan di Android 8.0 (API 26) hingga Android 15 |

---

## 12. ⚡ Risiko & Mitigasi

| Risiko | Dampak | Mitigasi |
|--------|--------|----------|
| JSch tidak support algoritma SSH modern | Koneksi gagal ke server baru | Pertimbangkan fallback ke SSHJ, atau update JSch berkala |
| OEM Android agresif kill background | Tunnel putus | Dokumentasi whitelist battery untuk user, gunakan WorkManager |
| Play Store reject karena Foreground Service | Tidak bisa publish | Ikuti policy terbaru Google, gunakan `connectedDevice` type |
| Hotspot IP berubah-ubah | Link copy tidak valid | Deteksi IP real-time setiap kali tombol copy ditekan |

---

## 13. 📚 Referensi

- [JSch Fork (mwiede)](https://github.com/mwiede/jsch)
- [Android Foreground Service Docs](https://developer.android.com/develop/background-work/services/foreground-services)
- [Android Tethering Restrictions](https://developer.android.com/reference/android/net/ConnectivityManager)
- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)

---

## 14. 📝 Catatan Pengembangan

> **Catatan untuk Developer:**
> 1. Mulai dengan implementasi `SshManager` terlebih dahulu sebagai proof-of-concept sebelum membangun UI.
> 2. Test Foreground Service di berbagai OEM (Samsung, Xiaomi, Oppo) karena perilaku background-nya berbeda.
> 3. Selalu minta permission `POST_NOTIFICATIONS` secara runtime di Android 13+.
> 4. Gunakan `android:exported="false"` untuk service agar lebih aman.

---

**Dokumen ini adalah living document dan akan di-update seiring perkembangan proyek.**
```

---

### 💡 Cara Menggunakan File Ini:

1. **Copy seluruh isi** di atas ke file baru bernama `prd.md` di root repository Anda.
2. File ini sudah mencakup **semua hasil brainstorming kita**: SSH customizable, hotspot integration, copy link, save profile, foreground service, dll.
3. Anda bisa langsung menggunakan **User Stories** sebagai tiket di Trello/Jira/GitHub Issues.
4. **Roadmap** bisa dijadikan milestone di GitHub Projects.
5. **Data Model** sudah siap diimplementasi langsung ke Room Entity.

### 🚀 Langkah Selanjutnya yang Saya Sarankan:

Setelah Anda punya `prd.md`, kita bisa lanjut ke:
- **Opsi A:** Membuat struktur folder proyek + file `build.gradle.kts` lengkap dengan semua dependency.
- **Opsi B:** Langsung implementasi `SshManager.kt` sebagai proof-of-concept (inti dari aplikasi).
- **Opsi C:** Membuat UI `HomeScreen` dengan Jetpack Compose sebagai mockup visual.

