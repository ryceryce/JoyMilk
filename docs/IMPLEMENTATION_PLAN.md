# Rencana Implementasi JoyMilk

## Status

| Bagian | Status |
|---|---|
| Android saat ini | Alur login, dashboard, order, produk, laporan, dan pengguna tersedia. Repository Android sudah memakai API Vercel. Firebase Auth tetap menangani login. |
| Desain target | Terdokumentasi di [PRD](PRD.md), [TRD](TRD.md), [skema](DATABASE_SCHEMA.md), dan [workflow](WORKFLOW.md). |
| Supabase, Vercel, Realtime, MCP, Telegram | Kode dan migrasi tersedia. Deployment, provisioning lingkungan, dan verifikasi integrasi hosted masih menunggu. |

## 1. Setup schema, backend, dan Android

1. Buat lingkungan staging Firebase Auth, Supabase, dan Vercel. Definisikan migrasi SQL, role backend, grant, constraints, dan indeks. Siapkan bootstrap admin dari UID Firebase Auth.
2. Terapkan migrasi dan deploy backend dari direktori `backend`. Isi variabel deployment yang tercantum di [README](../README.md). Jalankan smoke test API profil, katalog, order, dan laporan.
3. Bangun Android dengan URL staging dan kredensial publik Realtime. Verifikasi Firebase Auth, role, pembuatan order, serta isian yang dipertahankan saat koneksi putus.

## 2. Realtime

1. Hubungkan Firebase Auth sebagai penyedia token Supabase. Tambahkan claim `role: authenticated` pada akun yang diizinkan, termasuk saat provisioning akun baru.
2. Konfigurasikan Broadcast privat, pemeriksaan akses, dan sinyal perubahan tanpa isi record. Android memuat ulang data dari API pada event dan reconnect.
3. Uji perubahan dari dua sesi Android, refresh token, akun nonaktif, dan pemulihan setelah kehilangan koneksi.

## 3. MCP dan Telegram

1. Uji endpoint MCP staging. Coba pencarian, draf, dan konfirmasi dengan token yang berizin, dicabut, atau dibatasi.
2. Daftarkan webhook staging dan tautkan akun Telegram uji. Verifikasi penerjemahan chat, tampilan ringkasan, dan penyimpanan setelah konfirmasi.
3. Uji pesan ambigu, produk nonaktif, perubahan harga, draf kedaluwarsa, update ganda, dan klik dari akun lain. Verifikasi order bot terlihat di Android.

## Verifikasi sebelum cutover

Build backend dan Android serta pengujian transaksi Postgres sudah dijalankan secara lokal. Pengujian webhook memakai respons Telegram dan model yang disimulasikan. Jalankan pemeriksaan di bawah dengan layanan staging sebelum membuka penulisan produksi.

- Jalankan pengujian integrasi dengan Postgres nyata. Gunakan dua request paralel dengan kunci yang sama, simulasikan kegagalan saat menulis item, lalu cek satu order atau rollback penuh.
- Uji penolakan pengguna nonaktif, hak staff versus admin, akses langsung database dari klien, replay draf, dan perubahan harga bersamaan dengan pembuatan order.
- Uji laporan dengan perubahan harga produk dan order dibatalkan. Cocokkan hitungan dengan harga snapshot dan batas hari Asia/Jakarta.
- Bangun APK staging. Uji login, peran, seluruh alur order, katalog, laporan, kegagalan jaringan, dan reconnect.

## Cutover dan rollback

1. Siapkan produksi Supabase dengan tabel bisnis kosong. Buat profil admin dan staf aktif dari UID Firebase Auth yang tetap digunakan, lalu masukkan kategori dan produk awal. Order serta riwayat harga Firestore tetap menjadi arsip.
2. Setelah staging lolos, tentukan waktu penghentian klien lama dan bekukan penulisan Firestore. Uji login dan pembacaan produksi sebelum membuka penulisan di Supabase. Catat build Android terakhir yang memakai Firestore untuk rollback sebelum penulisan baru dibuka.
3. Setelah Supabase menerima order, pertahankan record produksi saat menangani insiden. Jika perlu kembali ke Firestore, hentikan kedua jalur tulis, ekspor order baru dari Supabase, rekonsiliasi dengan arsip, dan verifikasi hasil sebelum membuka jalur tulis kembali.
4. Aktifkan MCP dan webhook produksi setelah Android stabil. Pantau error API, webhook, serta Realtime dan cocokkan jumlah order tiap kanal.
