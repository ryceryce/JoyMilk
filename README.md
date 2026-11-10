# 🥛 JoyMilk

**Aplikasi Android internal untuk pencatatan orderan bisnis susu kedelai.**

---

## 📋 Tentang

JoyMilk adalah aplikasi Android native yang dirancang khusus untuk operasional internal bisnis susu kedelai. Aplikasi ini membantu tim (2–5 orang) dalam:

- 📝 **Pencatatan order** dengan harga otomatis dari master data
- 📦 **Manajemen produk** dengan riwayat perubahan harga
- 📈 **Laporan penjualan** harian, mingguan, dan bulanan
- 👤 **Manajemen user** dengan pembagian role (admin/staff)
- ☁️ **Sinkronisasi cloud** real-time antar perangkat

---

## 🏗️ Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM (single-activity) |
| DI | Hilt |
| Backend | Firebase Firestore + Auth |
| Build | Gradle Kotlin DSL |

---

## 📁 Project Structure

```
com.joymilk.app/
├── data/
│   ├── model/          # Data classes (User, Product, Order, OrderItem, PriceHistory, SweetLevel)
│   └── repository/     # Firebase operations (Auth, Order, Product, Report, User)
├── di/                 # Hilt modules
├── ui/
│   ├── auth/           # Login
│   ├── dashboard/      # Dashboard
│   ├── order/          # Order (List, Form, Detail)
│   ├── product/        # Product (List, Form, PriceHistory)
│   ├── report/         # Report
│   ├── user/           # User Management
│   ├── components/     # Reusable composables
│   ├── navigation/     # NavGraph
│   └── theme/          # Material 3 theme
└── util/               # DateUtils, CurrencyUtils, Constants
```

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug (2024.2) atau lebih baru
- JDK 17+
- Akun Google (untuk Firebase)

### 1. Clone Repository
```bash
git clone <repo-url>
cd JoyMilk
```

### 2. Setup Firebase
1. Buka [Firebase Console](https://console.firebase.google.com/)
2. Buat project baru bernama "JoyMilk"
3. Tambahkan Android app:
   - Package name: `com.joymilk.app`
   - App nickname: JoyMilk
4. Download `google-services.json`
5. Letakkan file di folder `app/`

### 3. Enable Firebase Services
Di Firebase Console:
- **Authentication** → Sign-in method → Enable **Email/Password**
- **Cloud Firestore** → Create database → Start in **production mode**

### 4. Setup Initial Admin User
1. Di Firebase Console → Authentication → Add user:
   - Email: `admin@joymilk.com` (atau email admin Anda)
   - Password: (buat password yang kuat)
2. Copy UID dari user yang baru dibuat
3. Di Firestore → Buat document manual:
   ```
   Collection: users
   Document ID: (paste UID dari step 2)
   Fields:
     - name: "Admin" (string)
     - email: "admin@joymilk.com" (string)
     - role: "admin" (string)
     - isActive: true (boolean)
     - createdAt: (timestamp, now)
   ```

### 5. Deploy Firestore Security Rules
Copy rules dari `docs/DATABASE_SCHEMA.md` bagian Security Rules ke Firebase Console → Firestore → Rules.

### 6. Create Firestore Indexes
Di Firebase Console → Firestore → Indexes, buat composite indexes:
- Collection: `orders` → Fields: `status` (ASC), `createdAt` (DESC)
- Collection: `orders` → Fields: `createdBy` (ASC), `createdAt` (DESC)

### 7. Build & Run
```bash
./gradlew assembleDebug
```
Atau buka project di Android Studio dan klik ▶️ Run.

---

## 📖 Dokumentasi

| Dokumen | Deskripsi |
|---------|-----------|
| [PRD](docs/PRD.md) | Product Requirements Document |
| [TRD](docs/TRD.md) | Technical Requirements Document |
| [Database Schema](docs/DATABASE_SCHEMA.md) | Skema database Firestore |
| [Workflow](docs/WORKFLOW.md) | Alur kerja aplikasi |
| [UI/UX Rules](docs/UI_UX_RULES.md) | Aturan desain dan komponen UI |
| [Implementation Plan](docs/IMPLEMENTATION_PLAN.md) | Rencana implementasi per fase |

---

## 🔑 Fitur Utama

### Price Snapshot
Setiap order menyimpan harga produk pada saat order dibuat. Jika harga produk berubah, order lama tetap menggunakan harga saat transaksi. Ini memastikan laporan keuangan selalu akurat.

### Sweet Level
Setiap item order memiliki preferensi tingkat kemanisan:
- Tanpa Gula
- Less Sugar
- Normal
- Extra Sweet

Sweet level hanya preferensi — tidak mempengaruhi harga.

### Role-Based Access
| Fitur | Admin | Staff |
|-------|:-----:|:-----:|
| Input order | ✅ | ✅ |
| Lihat order | ✅ | ✅ |
| Update status | ✅ | ✅ |
| Kelola produk | ✅ | ❌ |
| Kelola user | ✅ | ❌ |
| Lihat laporan | ✅ | ✅ |

---

## 🤖 Agent Instructions

File instruksi untuk AI coding agents:
- [`AGENTS.md`](AGENTS.md) — Universal agent rules (Gemini, Copilot, Codex, dll.)
- [`CLAUDE.md`](CLAUDE.md) — Claude-specific instructions

Kedua file mengadaptasi filosofi [Ponytail](https://github.com/dietrichgebert/ponytail) — "lazy senior dev" yang mengutamakan solusi minimal dan tepat.

---

## 📄 License

Private — Internal use only.
