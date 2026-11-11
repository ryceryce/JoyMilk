# Mockup Desain UI & Wireframe — JoyMilk Android App

> **Spesifikasi Mockup Resmi**  
> Framework: Jetpack Compose · Material 3 (Material You)  
> Bahasa UI: 100% Bahasa Indonesia  
> Brand Theme: Natural Soy Milk (Hijau `#4CAF50`, Krim `#FFF8E1`, Cokelat `#795548`)  
> Target: Android Phone Portrait (min-width: 320dp)

---

## 🎨 Ringkasan Design Tokens

| Token | Nilai Hex | Penggunaan di Mockup |
|---|---|---|
| **Primary** | `#4CAF50` | Top App Bar, Filled Buttons, Floating Action Button (FAB), Selected Tab |
| **Primary Container** | `#C8E6C9` | Container chip aktif, badge sukses, highlight card |
| **Secondary** | `#FFF8E1` | Background aksen, banner card khusus |
| **Secondary Container** | `#FFE082` | Badge perhatian, counter |
| **Tertiary** | `#795548` | Elemen aksen earthy, avatar, border subtle |
| **Background** | `#FAFAFA` | Canvas latar belakang aplikasi |
| **Surface** | `#FFFFFF` | Latar belakang Card, Bottom Sheet, Dialog |
| **Error** | `#D32F2F` | Pesan error validasi, tombol batalkan/hapus |
| **Status: Pending** | `#FF9800` | Chip status Pesanan Baru / Menunggu |
| **Status: Diproses** | `#2196F3` | Chip status Pesanan Sedang Diproses |
| **Status: Selesai** | `#4CAF50` | Chip status Pesanan Telah Selesai/Diantar |
| **Status: Dibatalkan** | `#9E9E9E` | Chip status Pesanan Dibatalkan |

---

## 📱 Daftar Mockup Layar Lengkap (13 Halaman)

```
[1. Login] ──► [2. Dashboard] ──┬──► [3. Order List] ──┬──► [4. Order Form]
                                │                     └──► [5. Order Detail]
                                ├──► [9. Laporan]
                                └──► [12. Lainnya] ───┬──► [6. Product List] ──► [7. Product Form]
                                                      │                       └──► [8. Price History]
                                                      └──► [10. User List] ───► [11. User Form]
```

---

### 1. 🔐 Login Screen (`login`)

*Akses: Publik / Unauthenticated User*

```
┌──────────────────────────────────────────────┐
│  09:41                   📶  🔋 100%         │
├──────────────────────────────────────────────┤
│                                              │
│                                              │
│                 🥛 JoyMilk                   │
│         Susu Kedelai Alami & Segar           │
│        ────────────────────────────          │
│            Internal Order System             │
│                                              │
│                                              │
│       ┌──────────────────────────────┐       │
│       │ Email                        │       │
│       │ admin@joymilk.com            │       │
│       └──────────────────────────────┘       │
│                                              │
│       ┌──────────────────────────────┐       │
│       │ Password                   👁│       │
│       │ ••••••••••••                 │       │
│       └──────────────────────────────┘       │
│                                              │
│       ┌──────────────────────────────┐       │
│       │            MASUK             │       │  ← Green `#4CAF50`
│       └──────────────────────────────┘       │
│                                              │
│                                              │
│          v1.0.0 · Khusus Tim Internal        │
│                                              │
└──────────────────────────────────────────────┘
```

**Komponen & Spesifikasi:**
- **Branding:** Logo susu kedelai + judul aplikasi terpusat (Center Aligned).
- **Email Field:** `OutlinedTextField`, single-line, `KeyboardType.Email`.
- **Password Field:** `OutlinedTextField`, `PasswordVisualTransformation` dengan `IconButton` toggle visibilitas password (`visibility` / `visibility_off`).
- **Tombol Masuk:** `Button` (Filled) full-width (padding horizontal 24dp), height 50dp. Saat loading: menampilkan `CircularProgressIndicator` ukuran 24dp putih.

---

### 2. 📊 Dashboard Screen (`dashboard`)

*Akses: Admin & Staff (Tab "Beranda")*

```
┌──────────────────────────────────────────────┐
│  09:41                   📶  🔋 100%         │
├──────────────────────────────────────────────┤
│ 🥛 JoyMilk                             🚪    │  ← TopAppBar: Logout action
│ Halo, Andi Prasetyo!                         │  ← Large Headline (Admin)
│ Selasa, 10 November 2026                     │
├──────────────────────────────────────────────┤
│                                              │
│  ┌────────────────────┐┌───────────────────┐ │
│  │ Order Hari Ini     ││ Penjualan Hari Ini│ │  ← 2 Summary Cards
│  │ 📋 12 Pesanan      ││ 💰 Rp 2.450.000   │ │    (Elevated 2dp)
│  └────────────────────┘└───────────────────┘ │
│                                              │
│  ┌─────────────────────────────────────────┐ │
│  │  ➕   BUAT ORDER BARU                   │ │  ← Quick Action Utama
│  └─────────────────────────────────────────┘ │
│                                              │
│  Order Terbaru                        Lihat  │
│  ──────────────────────────────────────────  │
│  ┌─────────────────────────────────────────┐ │
│  │ #ORD-001 · Budi Santoso                 │ │
│  │ 3 item · 10 Nov, 14:30      [ Pending ] │ │
│  │ Rp 35.000                     🟠 Orange │ │
│  └─────────────────────────────────────────┘ │
│  ┌─────────────────────────────────────────┐ │
│  │ #ORD-002 · Siti Rahayu                  │ │
│  │ 1 item · 10 Nov, 13:15     [ Diproses ] │ │
│  │ Rp 12.000                      🔵 Blue  │ │
│  └─────────────────────────────────────────┘ │
│  ┌─────────────────────────────────────────┐ │
│  │ #ORD-003 · Agus Wijaya                  │ │
│  │ 5 item · 10 Nov, 11:00      [ Selesai ] │ │
│  │ Rp 58.000                     🟢 Green  │ │
│  └─────────────────────────────────────────┘ │
│                                              │
├──────────────────────────────────────────────┤
│  [🏠 Beranda]   [📋 Order]   [📈 Laporan]   [⚙️ Lainnya] │ ← Bottom Nav
└──────────────────────────────────────────────┘
```

**Komponen & Spesifikasi:**
- **TopAppBar:** Menampilkan salam dinamis dan tanggal hari ini.
- **Summary Metric Cards:** Menggunakan `ElevatedCard` berdampingan (`Row` dengan `weight(1f)`). Format nominal rupiah standar `Rp 2.450.000`.
- **Quick Action:** Tombol besar yang langsung mengarahkan ke formulir pesanan baru.
- **Recent Orders List:** 3–5 kartu pesanan terbaru dengan status chip berwarna.

---

### 3. 📋 Order List Screen (`orders`)

*Akses: Admin & Staff (Tab "Order")*

```
┌──────────────────────────────────────────────┐
│  09:41                   📶  🔋 100%         │
├──────────────────────────────────────────────┤
│ 📋 Daftar Order                 🔍 Cari      │  ← TopAppBar
├──────────────────────────────────────────────┤
│ [Semua] (Hari Ini) [Minggu Ini] [Bulan Ini]  │  ← Filter Tanggal Chip
│ [Semua] [🟠 Pending] [🔵 Proses] [🟢 Selesai]│  ← Filter Status Chip
├──────────────────────────────────────────────┤
│                                              │
│  ┌─────────────────────────────────────────┐ │
│  │ #ORD-001                  10 Nov, 14:30 │ │
│  │ Budi Santoso                [ Pending ] │ │
│  │ 2x Susu Original, 1x Susu Coklat        │ │
│  │ Total: Rp 35.000                 Tunai  │ │
│  └─────────────────────────────────────────┘ │
│  ┌─────────────────────────────────────────┐ │
│  │ #ORD-002                  10 Nov, 13:15 │ │
│  │ Siti Rahayu                [ Diproses ] │ │
│  │ 1x Susu Coklat                          │ │
│  │ Total: Rp 12.000              Transfer  │ │
│  └─────────────────────────────────────────┘ │
│  ┌─────────────────────────────────────────┐ │
│  │ #ORD-003                  10 Nov, 11:00 │ │
│  │ Agus Wijaya                 [ Selesai ] │ │
│  │ 3x Susu Matcha, 2x Tahu Susu            │ │
│  │ Total: Rp 58.000               E-Wallet │ │
│  └─────────────────────────────────────────┘ │
│                                              │
│                               ┌────────────┐ │
│                               │ ➕ Order   │ │  ← Extended FAB
│                               └────────────┘ │
├──────────────────────────────────────────────┤
│  [🏠 Beranda]   [📋 Order]   [📈 Laporan]   [⚙️ Lainnya] │
└──────────────────────────────────────────────┘
```

**Komponen & Spesifikasi:**
- **Horizontal Filter Chips:** Menggunakan `FilterChip` dengan 2 baris filter (Waktu transaksi & Status pemesanan).
- **Card Item:** Menampilkan nomor order snapshot, waktu, rincian ringkas nama produk, metode pembayaran, dan subtotal.
- **Floating Action Button:** `ExtendedFloatingActionButton` dengan teks "+ Order" di pojok kanan bawah.

---

### 4. 📝 Order Form Screen (`orders/new` / `orders/{id}/edit`)

*Akses: Admin & Staff*

```
┌──────────────────────────────────────────────┐
│  09:41                   📶  🔋 100%         │
├──────────────────────────────────────────────┤
│ ← Buat Order Baru                            │
├──────────────────────────────────────────────┤
│ ▼ INFORMASI PELANGGAN                        │
│ ┌──────────────────────────────────────────┐ │
│ │ Nama Pelanggan *                         │ │
│ │ Budi Santoso                             │ │
│ └──────────────────────────────────────────┘ │
│ ┌──────────────────────────────────────────┐ │
│ │ Nomor Telepon (Opsional)                 │ │
│ │ 081234567890                             │ │
│ └──────────────────────────────────────────┘ │
│ ┌──────────────────────────────────────────┐ │
│ │ Alamat Pengiriman (Opsional)             │ │
│ │ Jl. Melati No. 12, RT 02/04              │ │
│ └──────────────────────────────────────────┘ │
│                                              │
│ ▼ ITEM PESANAN                               │
│ ┌──────────────────────────────────────────┐ │
│ │ Produk 1: Susu Kedelai Original        ▼ │ │  ← Dropdown Produk Master
│ │ Manis: [Tanpa] [Less] (Normal) [Extra]   │ │  ← Sweet Level Chip Selector
│ │ Jumlah:  [ - ]   2   [ + ]               │ │  ← Stepper Qty
│ │ Harga Snapshot : Rp 10.000               │ │
│ │ Subtotal       : Rp 20.000           [🗑]│ │  ← Hapus Item
│ └──────────────────────────────────────────┘ │
│ ┌──────────────────────────────────────────┐ │
│ │ Produk 2: Susu Kedelai Coklat          ▼ │ │
│ │ Manis: [Tanpa] (Less Sugar) [Norm] [Ext] │ │
│ │ Jumlah:  [ - ]   1   [ + ]               │ │
│ │ Harga Snapshot : Rp 12.000               │ │
│ │ Subtotal       : Rp 12.000           [🗑]│ │
│ └──────────────────────────────────────────┘ │
│                                              │
│ ┌──────────────────────────────────────────┐ │
│ │  ➕  Tambah Produk Lainnya               │ │  ← Tombol Tambah Baris Item
│ └──────────────────────────────────────────┘ │
│                                              │
│ ▼ PEMBAYARAN & CATATAN                       │
│ ┌──────────────────────────────────────────┐ │
│ │ Metode Pembayaran                        │ │
│ │ (•) Tunai (Cash)  ( ) Transfer  ( ) QRIS │ │  ← Radio Button / Dropdown
│ └──────────────────────────────────────────┘ │
│ ┌──────────────────────────────────────────┐ │
│ │ Catatan Pesanan                          │ │
│ │ Minta sedotan 2 & es dipisah             │ │
│ └──────────────────────────────────────────┘ │
│                                              │
├──────────────────────────────────────────────┤
│ Total: Rp 32.000        ┌──────────────────┐ │  ← Sticky Bottom Bar
│ (2 Item Terpilih)       │   SIMPAN ORDER   │ │
│                         └──────────────────┘ │
└──────────────────────────────────────────────┘
```

**Komponen & Spesifikasi:**
- **Product Selector:** Mengambil daftar aktif dari koleksi `products`. Begitu dipilih, harga langsung di-snapshot ke field read-only.
- **Sweet Level Preference:** 4 pilihan filter chip horizontal: *Tanpa Gula*, *Less Sugar*, *Normal*, *Extra Sweet*. Tidak mempengaruhi harga.
- **Sticky Bottom Summary:** Total terhitung real-time `sum(subtotal)` tanpa reload halaman.

---

### 5. 📄 Order Detail Screen (`orders/{id}`)

*Akses: Admin & Staff*

```
┌──────────────────────────────────────────────┐
│  09:41                   📶  🔋 100%         │
├──────────────────────────────────────────────┤
│ ← Detail Order #ORD-001                      │
├──────────────────────────────────────────────┤
│ Status Saat Ini:                             │
│ ┌──────────────────────────────────────────┐ │
│ │ 🟠 PENDING (Menunggu Diproses)           │ │
│ │ Waktu: 10 Nov 2026, 14:30:15 WIB         │ │
│ │ Dibuat oleh: Maya Sari (Staff)           │ │
│ └──────────────────────────────────────────┘ │
│                                              │
│ Informasi Pemesan:                           │
│ • Pelanggan : Budi Santoso                   │
│ • Telepon   : 081234567890                   │
│ • Alamat    : Jl. Melati No. 12, RT 02/04    │
│                                              │
│ Rincian Pesanan (Price Snapshot):            │
│ ──────────────────────────────────────────   │
│ 1. Susu Kedelai Original                     │
│    • Kemanisan : Normal                      │
│    • 2 Botol x Rp 10.000      Rp 20.000      │
│                                              │
│ 2. Susu Kedelai Coklat                       │
│    • Kemanisan : Less Sugar                  │
│    • 1 Botol x Rp 12.000      Rp 12.000      │
│ ──────────────────────────────────────────   │
│ Metode Pembayaran            Tunai (Cash)    │
│ Catatan                      Es dipisah      │
│                                              │
│ ┌──────────────────────────────────────────┐ │
│ │ TOTAL PEMBAYARAN             Rp 32.000   │ │  ← High-contrast Container
│ └──────────────────────────────────────────┘ │
│                                              │
│ ┌─────────────────────┐┌───────────────────┐ │
│ │  ✏️ EDIT PESANAN    ││ 🔄 UPDATE STATUS  │ │  ← Edit aktif jika pending
│ └─────────────────────┘└───────────────────┘ │
│ ┌──────────────────────────────────────────┐ │
│ │  ❌ BATALKAN PESANAN                     │ │
│ └──────────────────────────────────────────┘ │
└──────────────────────────────────────────────┘
```

**Komponen & Spesifikasi:**
- **Status Update:** Membuka `ModalBottomSheet` yang menampilkan alur status: `Pending` ➔ `Diproses` ➔ `Selesai` atau `Dibatalkan`.
- **Price Integrity Notice:** Menampilkan label harga snapshot yang terkunci dan tidak dapat diubah oleh perubahan harga di master produk.

---

### 6. 📦 Product List Screen (`products`)

*Akses: Admin Only (Staff tidak dapat mengakses menu ini)*

```
┌──────────────────────────────────────────────┐
│  09:41                   📶  🔋 100%         │
├──────────────────────────────────────────────┤
│ ← Master Data Produk                🔍       │
├──────────────────────────────────────────────┤
│ [Semua] [Susu Kedelai] [Susu Rasa] [Snack]   │  ← Kategori
├──────────────────────────────────────────────┤
│                                              │
│  ┌─────────────────────────────────────────┐ │
│  │ Susu Kedelai Original        [ AKTIF ]  │ │
│  │ Kategori: Susu Kedelai      Satuan: Btl │ │
│  │ Rp 10.000 / Botol                       │ │
│  │ 📈 Riwayat Harga (3x)              ✏️    │ │
│  └─────────────────────────────────────────┘ │
│  ┌─────────────────────────────────────────┐ │
│  │ Susu Kedelai Coklat          [ AKTIF ]  │ │
│  │ Kategori: Susu Rasa         Satuan: Btl │ │
│  │ Rp 12.000 / Botol                       │ │
│  │ 📈 Riwayat Harga (1x)              ✏️    │ │
│  └─────────────────────────────────────────┘ │
│  ┌─────────────────────────────────────────┐ │
│  │ Tahu Susu Kedelai            [ NONAKTIF]│ │
│  │ Kategori: Snack             Satuan: Pck │ │
│  │ Rp 5.000 / Pack                         │ │
│  │ 📈 Riwayat Harga (1x)              ✏️    │ │
│  └─────────────────────────────────────────┘ │
│                                              │
│                               ┌────────────┐ │
│                               │  ➕        │ │  ← FAB Tambah Produk
│                               └────────────┘ │
└──────────────────────────────────────────────┘
```

**Komponen & Spesifikasi:**
- **Product Card:** Menampilkan status ketersediaan (*Aktif* / *Nonaktif*), satuan kemasan (*Botol*, *Gelas*, *Pack*), dan shortcut menuju riwayat harga.
- **Floating Action Button:** Membuka form penambahan master produk baru.

---

### 7. 📝 Product Form Screen (`products/new` / `products/{id}/edit`)

*Akses: Admin Only*

```
┌──────────────────────────────────────────────┐
│  09:41                   📶  🔋 100%         │
├──────────────────────────────────────────────┤
│ ← Edit Produk                                │
├──────────────────────────────────────────────┤
│ ┌──────────────────────────────────────────┐ │
│ │ Nama Produk *                            │ │
│ │ Susu Kedelai Original                    │ │
│ └──────────────────────────────────────────┘ │
│ ┌──────────────────────────────────────────┐ │
│ │ Kategori *                             ▼ │ │
│ │ Susu Kedelai                             │ │
│ └──────────────────────────────────────────┘ │
│ ┌──────────────────────────────────────────┐ │
│ │ Harga Saat Ini (Rp) *                    │ │
│ │ Rp 12.000                                │ │  ← Diubah dari 10.000
│ └──────────────────────────────────────────┘ │
│                                              │
│  ⚠️ PERHATIAN PERUBAHAN HARGA:               │
│  Harga sebelumnya adalah Rp 10.000.          │
│  Perubahan harga akan otomatis dicatat ke    │
│  Riwayat Harga dan tidak mengubah pesanan    │
│  yang sudah lewat.                           │
│                                              │
│ ┌──────────────────────────────────────────┐ │
│ │ Satuan Kemasan *                       ▼ │ │
│ │ Botol                                    │ │
│ └──────────────────────────────────────────┘ │
│ ┌──────────────────────────────────────────┐ │
│ │ Deskripsi Produk (Opsional)              │ │
│ │ Susu kedelai murni tanpa bahan pengawet  │ │
│ └──────────────────────────────────────────┘ │
│                                              │
│ Status Produk:                               │
│ [🔘 Switch] Produk Aktif Dijual              │
│                                              │
│ ┌──────────────────────────────────────────┐ │
│ │              SIMPAN PRODUK               │ │
│ └──────────────────────────────────────────┘ │
└──────────────────────────────────────────────┘
```

**Komponen & Spesifikasi:**
- **Price Change Detection:** Jika user mengubah nilai `currentPrice`, aplikasi memunculkan banner alert peringatan otomatis bahwa perubahan harga akan diabadikan di `priceHistory` Firestore transaction.

---

### 8. 📊 Price History Screen (`products/{id}/price-history`)

*Akses: Admin Only*

```
┌──────────────────────────────────────────────┐
│  09:41                   📶  🔋 100%         │
├──────────────────────────────────────────────┤
│ ← Riwayat Harga — Susu Original              │
├──────────────────────────────────────────────┤
│ Produk: Susu Kedelai Original                │
│ Harga Saat Ini: Rp 12.000 / Botol            │
│ ──────────────────────────────────────────   │
│                                              │
│  ● 10 Nov 2026, 14:00                        │
│  │ Rp 12.000                                 │
│  │ Naik dari Rp 10.000                       │
│  │ Diubah oleh: Andi Prasetyo (Admin)        │
│  │                                           │
│  ● 01 Okt 2026, 08:30                        │
│  │ Rp 10.000                                 │
│  │ Naik dari Rp 8.000                        │
│  │ Diubah oleh: Andi Prasetyo (Admin)        │
│  │                                           │
│  ● 15 Agu 2026, 10:00                        │
│    Rp 8.000                                  │
│    Harga Awal Peluncuran                     │
│    Dibuat oleh: Andi Prasetyo (Admin)        │
│                                              │
└──────────────────────────────────────────────┘
```

**Komponen & Spesifikasi:**
- **Timeline Vertical:** Garis waktu visual perubahan harga bertahap dari dokumen sub-koleksi `/products/{id}/priceHistory`.

---

### 9. 📈 Report Screen (`reports`)

*Akses: Admin & Staff (Tab "Laporan")*

```
┌──────────────────────────────────────────────┐
│  09:41                   📶  🔋 100%         │
├──────────────────────────────────────────────┤
│ 📈 Laporan Penjualan                         │
├──────────────────────────────────────────────┤
│  [ Harian ]    [ Mingguan ]   [( Bulanan )]  │  ← Tab Row
├──────────────────────────────────────────────┤
│  📅 Periode: [ November 2026            ▼ ]  │  ← Period Picker
├──────────────────────────────────────────────┤
│                                              │
│  ┌────────────────────┐┌───────────────────┐ │
│  │ Total Pesanan      ││ Total Omset       │ │
│  │ 245 Transaksi      ││ Rp 4.900.000      │ │
│  └────────────────────┘└───────────────────┘ │
│                                              │
│  Status Transaksi:                           │
│  • 🟢 Selesai    : 230 pesanan (Rp 4.650.000)│
│  • 🟠 Pending    : 10 pesanan  (Rp 180.000)  │
│  • ❌ Dibatalkan : 5 pesanan   (Rp 70.000)   │
│                                              │
│  🏆 5 Produk Terlaris (Qty Terbanyak):       │
│  ──────────────────────────────────────────  │
│  #1 Susu Kedelai Original                    │
│     Terjual: 140 Botol   · Omset: Rp 1.40jt  │
│  #2 Susu Kedelai Coklat                      │
│     Terjual: 65 Botol    · Omset: Rp 780rb   │
│  #3 Susu Kedelai Matcha                      │
│     Terjual: 40 Gelas    · Omset: Rp 520rb   │
│  #4 Tahu Susu Kedelai                        │
│     Terjual: 25 Pack     · Omset: Rp 125rb   │
│  #5 Susu Kedelai Strawberry                  │
│     Terjual: 15 Gelas    · Omset: Rp 180rb   │
│                                              │
├──────────────────────────────────────────────┤
│  [🏠 Beranda]   [📋 Order]   [📈 Laporan]   [⚙️ Lainnya] │
└──────────────────────────────────────────────┘
```

**Komponen & Spesifikasi:**
- **Period Tab Selector:** Tab bar navigasi waktu (Harian/Mingguan/Bulanan).
- **Ranking Terlaris:** Menghitung agregasi dari snapshot produk di setiap transaksi terverifikasi.

---

### 10. 👤 User List Screen (`users`)

*Akses: Admin Only*

```
┌──────────────────────────────────────────────┐
│  09:41                   📶  🔋 100%         │
├──────────────────────────────────────────────┤
│ ← Kelola Pengguna                            │
├──────────────────────────────────────────────┤
│ Daftar Tim Internal (4 Akun):                │
│                                              │
│  ┌─────────────────────────────────────────┐ │
│  │ Andi Prasetyo                  [ADMIN]  │ │
│  │ admin@joymilk.com              🟢 Aktif │ │
│  │ Dibuat: 01 Agu 2026                     │ │
│  └─────────────────────────────────────────┘ │
│  ┌─────────────────────────────────────────┐ │
│  │ Maya Sari                      [STAFF]  │ │
│  │ maya@joymilk.com               🟢 Aktif │ │
│  │ Dibuat: 15 Agu 2026                     │ │
│  └─────────────────────────────────────────┘ │
│  ┌─────────────────────────────────────────┐ │
│  │ Doni Kusuma                    [STAFF]  │ │
│  │ doni@joymilk.com               🟢 Aktif │ │
│  │ Dibuat: 01 Sep 2026                     │ │
│  └─────────────────────────────────────────┘ │
│  ┌─────────────────────────────────────────┐ │
│  │ Lina Wijaya                    [STAFF]  │ │
│  │ lina@joymilk.com            🔴 Nonaktif │ │
│  │ Dibuat: 10 Sep 2026                     │ │
│  └─────────────────────────────────────────┘ │
│                                              │
│                               ┌────────────┐ │
│                               │  ➕        │ │  ← Tambah User FAB
│                               └────────────┘ │
└──────────────────────────────────────────────┘
```

---

### 11. 📝 User Form Screen (`users/new`)

*Akses: Admin Only*

```
┌──────────────────────────────────────────────┐
│  09:41                   📶  🔋 100%         │
├──────────────────────────────────────────────┤
│ ← Tambah Pengguna Baru                       │
├──────────────────────────────────────────────┤
│ ┌──────────────────────────────────────────┐ │
│ │ Nama Lengkap *                           │ │
│ │ Rian Hidayat                             │ │
│ └──────────────────────────────────────────┘ │
│ ┌──────────────────────────────────────────┐ │
│ │ Alamat Email *                           │ │
│ │ rian@joymilk.com                         │ │
│ └──────────────────────────────────────────┘ │
│ ┌──────────────────────────────────────────┐ │
│ │ Password Akun *                        👁│ │
│ │ ••••••••••••                             │ │
│ └──────────────────────────────────────────┘ │
│                                              │
│ Peran Akun (Role) *:                         │
│ [ (•) Staff Operasional ]   [ ( ) Administrator ] │
│ • Staff: Akses input pesanan & lihat laporan │
│ • Admin: Akses penuh data master & user      │
│                                              │
│ ┌──────────────────────────────────────────┐ │
│ │              SIMPAN USER                 │ │
│ └──────────────────────────────────────────┘ │
└──────────────────────────────────────────────┘
```

---

### 12. ⚙️ More / Settings Screen (`more`)

*Akses: Admin View vs Staff View*

#### Varian A: Tampilan Admin
```
┌──────────────────────────────────────────────┐
│  09:41                   📶  🔋 100%         │
├──────────────────────────────────────────────┤
│ ⚙️ Menu Lainnya                              │
├──────────────────────────────────────────────┤
│ Profil Pengguna:                             │
│ 👤 Andi Prasetyo (Administrator)             │
│ ✉️ admin@joymilk.com                         │
│ ──────────────────────────────────────────   │
│ MANAJEMEN MASTER DATA (ADMIN ONLY):          │
│                                              │
│ [📦 Master Produk & Harga             ➔ ]    │
│    Kelola daftar menu, harga & satuan        │
│                                              │
│ [👤 Kelola Akun Karyawan              ➔ ]    │
│    Tambah staf, hak akses & akun aktif       │
│                                              │
│ ──────────────────────────────────────────   │
│ APLIKASI:                                    │
│ [🚪 Keluar dari Aplikasi              ➔ ]    │
│                                              │
│ JoyMilk App v1.0.0 (Build 1)                 │
│ Cloud Database: Connected 🟢                 │
├──────────────────────────────────────────────┤
│  [🏠 Beranda]   [📋 Order]   [📈 Laporan]   [(⚙️ Lainnya)]│
└──────────────────────────────────────────────┘
```

#### Varian B: Tampilan Staff
```
┌──────────────────────────────────────────────┐
│  09:41                   📶  🔋 100%         │
├──────────────────────────────────────────────┤
│ ⚙️ Menu Lainnya                              │
├──────────────────────────────────────────────┤
│ Profil Pengguna:                             │
│ 👤 Maya Sari (Staff Operasional)             │
│ ✉️ maya@joymilk.com                          │
│ ──────────────────────────────────────────   │
│ APLIKASI:                                    │
│                                              │
│ [ℹ️ Panduan Singkat Pencatatan        ➔ ]    │
│                                              │
│ [🚪 Keluar dari Aplikasi              ➔ ]    │
│                                              │
│ JoyMilk App v1.0.0 (Build 1)                 │
│ Role: Staff (Order & Laporan Saja)           │
├──────────────────────────────────────────────┤
│  [🏠 Beranda]   [📋 Order]   [📈 Laporan]   [(⚙️ Lainnya)]│
└──────────────────────────────────────────────┘
```

---

### 13. 🧭 Bottom Navigation Bar Component Specs

```
┌───────────────────────────────────────────────────────────┐
│     🏠               📋               📈             ⚙️     │
│   Beranda          Order           Laporan        Lainnya │
│  (Dashboard)    (Order List)       (Reports)      (More)  │
└───────────────────────────────────────────────────────────┘
```
- Tinggi tetap: **80dp** (`NavigationBar` Material 3).
- Warna tab aktif: Container kapsul Pill hijau muda `#C8E6C9` dengan ikon hijau `#4CAF50`.
- Warna tab non-aktif: Ikon outline abu-abu gelap `#757575`.

---

## ⚡ Mockup Variasi State

### A. State Loading (Semua Layar)
```
┌──────────────────────────────────────────────┐
│                                              │
│                    ⏳                        │
│                 (  🌀  )                     │
│               Memuat data…                   │
│                                              │
└──────────────────────────────────────────────┘
```

### B. State Kosong (Empty State - Order List / Produk)
```
┌──────────────────────────────────────────────┐
│                                              │
│                    📦                        │
│             Belum Ada Pesanan                │
│   Belum ada transaksi pada periode ini.      │
│   Klik tombol di bawah untuk mencatat.       │
│                                              │
│            ┌─────────────────┐               │
│            │   BUAT ORDER    │               │
│            └─────────────────┘               │
│                                              │
└──────────────────────────────────────────────┘
```

### C. State Error Jaringan (Network Error)
```
┌──────────────────────────────────────────────┐
│                                              │
│                    ⚠️                        │
│             Koneksi Terputus                 │
│      Gagal terhubung ke Cloud Firestore.     │
│      Data tersimpan secara offline.          │
│                                              │
│            ┌─────────────────┐               │
│            │    COBA LAGI    │               │
│            └─────────────────┘               │
│                                              │
└──────────────────────────────────────────────┘
```

### D. State Validasi Error Form (Form Error)
```
┌──────────────────────────────────────────────┐
│ ┌──────────────────────────────────────────┐ │
│ │ Nama Pelanggan *                         │ │
│ │                                          │ │
│ └──────────────────────────────────────────┘ │
│ ❗ Nama pelanggan wajib diisi                │  ← Teks Merah `#D32F2F`
│                                              │
│ ┌──────────────────────────────────────────┐ │
│ │ Harga Produk *                           │ │
│ │ Rp -100                                  │ │
│ └──────────────────────────────────────────┘ │
│ ❗ Harga harus lebih dari 0                  │  ← Teks Merah `#D32F2F`
└──────────────────────────────────────────────┘
```

