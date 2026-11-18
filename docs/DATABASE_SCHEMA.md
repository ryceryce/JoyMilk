# Skema Database JoyMilk — Supabase Postgres

Migrasi SQL di `supabase/migrations/` mendefinisikan skema yang dipakai backend. Firestore lama tetap menjadi arsip setelah cutover. Baca [rencana implementasi](IMPLEMENTATION_PLAN.md) untuk status dan cutover.

## Data bisnis

Tabel berada di schema privat `joymilk`, di luar schema yang diekspos Supabase Data API. Vercel memakai role database khusus runtime. Role migrasi terpisah menjalankan DDL. Role `anon` dan `authenticated` tidak memiliki grant atas tabel bisnis. Frontend hanya menerima URL Supabase dan publishable key untuk berlangganan Realtime.

| Tabel | Kolom dan relasi utama |
|---|---|
| `users` | `firebase_uid text` PK, `name`, `email`, `role`, `is_active`, `created_at`. UID sama dengan Firebase Auth. Email unik tanpa membedakan kapitalisasi. |
| `categories` | `id uuid` PK, `name` unik tanpa membedakan kapitalisasi. |
| `products` | `id uuid` PK, `category_id` FK, `name`, `size`, `description`, `unit`, `current_price bigint`, `is_active`, `created_at`, `updated_at`. |
| `price_history` | `id uuid` PK, `product_id` FK, `price bigint`, `effective_at`, `changed_by` FK ke `users`. Entri bersifat append-only. |
| `orders` | `id uuid` PK, `customer_name`, `customer_phone` opsional, `delivery_address` opsional, `payment_method`, `status`, `total_amount bigint`, `created_by` FK ke `users`, `notes` opsional, `created_at`, `updated_at`. |
| `order_items` | `id uuid` PK, `order_id` FK, `product_id` FK, `product_name`, `price_at_order bigint`, `quantity integer`, `subtotal bigint`, `sweet_level`. |

Gunakan `timestamptz` untuk waktu. Nominal IDR adalah bilangan bulat nonnegatif. Kuantitas harus positif dan subtotal harus sama dengan harga snapshot dikali kuantitas. Validasi domain dengan constraints:

- `role` adalah `admin` atau `staff`.
- `payment_method` adalah `tunai`, `transfer`, atau `qris`.
- `status` adalah `pending`, `diproses`, `selesai`, atau `dibatalkan`. Order baru dimulai sebagai `pending`. Alur Android mengizinkan `pending → diproses → selesai` dan `pending → dibatalkan`.
- `sweet_level` adalah `TANPA_GULA`, `LESS_SUGAR`, `NORMAL`, atau `EXTRA_SWEET`. Pilihan ini tidak mengubah harga.

Indeks awal mencakup `orders(created_at DESC, id)`, `orders(status, created_at DESC, id)`, `order_items(order_id)`, dan `price_history(product_id, effective_at DESC)`. Nonaktifkan pengguna atau produk yang sudah dirujuk histori untuk menjaga referensi laporan.

## Data integrasi

| Tabel internal | Tujuan |
|---|---|
| `telegram_identities` | ID pengguna Telegram unik ditautkan oleh admin ke Firebase UID. Gunakan ID numerik, bukan username. |
| `telegram_updates` | Simpan satu `update_id` per bot dan lease pemrosesan. Simpan hasil sampai jawaban terkirim, lalu pertahankan penanda pengiriman untuk deduplikasi. |
| `telegram_context` | Simpan riwayat pesan singkat dengan waktu kedaluwarsa untuk jawaban lanjutan. Hapus konteks saat draf selesai atau tautan dicabut. |
| `mcp_tokens` | Simpan hash token klien acak, Firebase UID pemilik, izin tool, dan status pencabutan. |
| `order_drafts` | Simpan pemilik, kanal, data order terstruktur, snapshot pratinjau, versi ringkasan, status draf, waktu kedaluwarsa, serta ID order hasil konfirmasi. |
| `order_idempotency` | Kunci unik per aktor dan kanal dengan fingerprint permintaan dan ID order hasil. Berlaku untuk pembuatan order dari Android maupun konfirmasi draf. |

## Transaksi dan otorisasi

1. Pembuatan order mengautentikasi pemanggil, memeriksa pengguna aktif, dan mengklaim kunci idempotensi. Input sama mengembalikan hasil terdahulu. Input berbeda pada kunci yang sama ditolak.
2. Dalam transaksi yang sama, kunci produk dalam urutan ID yang konsisten, baca nama dan harga aktif, hitung subtotal serta total, lalu tulis order dan seluruh item. Simpan hasil idempotensi dan, jika ada, status konfirmasi draf dalam transaksi tersebut.
3. Snapshot nama dan harga pada item yang dipertahankan bersifat immutable. Untuk edit order `pending`, item yang ditambahkan mendapat snapshot baru. Total dihitung ulang dari item hasil edit.
4. Perubahan harga menulis produk dan satu entri harga baru dalam transaksi yang sama. Laporan menghitung nominal dari snapshot dan mengeluarkan order `dibatalkan`. Agregasi periode bisnis memakai Asia/Jakarta.
5. Backend membatasi pengelolaan pengguna, produk, kategori, dan riwayat harga ke admin. Staf aktif dapat membaca katalog dan laporan, membuat order, serta mengubah order sesuai status. Realtime memakai kebijakan baca untuk topik privat dan pengguna aktif. Event hanya berisi jenis perubahan. Gunakan fungsi pemeriksa akses yang terbatas untuk membaca status profil dari kebijakan Realtime, tanpa memberikan hak baca langsung ke tabel profil.

## Bootstrap

Mulai dengan tabel bisnis kosong. Buat profil admin awal dengan UID akun Firebase Auth yang sudah diverifikasi, daftarkan profil staf aktif yang diperlukan, lalu isi kategori dan produk awal. Order dan histori Firestore tetap di arsip. Lihat [kontrak teknis](TRD.md) dan [panduan cutover](IMPLEMENTATION_PLAN.md).
