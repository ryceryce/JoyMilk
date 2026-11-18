# Alur Kerja JoyMilk

Dokumen ini menggambarkan alur yang diterapkan pada Android dan backend. Lihat [rencana implementasi](IMPLEMENTATION_PLAN.md) untuk status rollout.

## Login dan akses

1. Pengguna masuk dengan email dan kata sandi lewat Firebase Auth.
2. Android mengirim Firebase ID token ke API profil. Backend memverifikasi token dan memuat profil Supabase berdasarkan UID.
3. Profil aktif masuk ke dashboard. Admin mendapat menu produk dan pengguna. Jika profil belum dibuat atau dinonaktifkan, tampilkan pesan akses ditolak. Saat sesi kedaluwarsa, Android mencoba memperbarui token dan meminta login ulang bila diperlukan.

## Membuat order

| Kanal | Persiapan | Persetujuan |
|---|---|---|
| Android | Staf mengisi nama pelanggan, metode pembayaran, dan satu atau lebih item. Nomor telepon, alamat, dan catatan opsional. | Staf menekan **Simpan Order**. Android mengirim formulir dan `Idempotency-Key`. |
| Telegram | Staf tertaut mengirim chat privat. Bot memakai MCP untuk mencari produk dan menyiapkan draf, meminta detail yang kurang atau memilih produk yang ambigu, lalu menampilkan ringkasan. | Pemilik draf menekan **Konfirmasi**. Handler callback memeriksa identitas, kemudian memanggil tool konfirmasi. Staf dapat membatalkan draf sebelum menyimpannya. Chat yang masih membutuhkan data tambahan berlaku selama 30 menit. |
| Klien AI internal | Klien berizin mencari produk dan membuat draf lewat MCP. Tampilkan ringkasan, termasuk item, harga, dan total. | Klien meminta persetujuan pengguna lalu memanggil tool konfirmasi dengan ID draf dan versi ringkasan. |

Backend menyimpan order dan item melalui layanan yang sama, lalu mengembalikan ID order. Harga pada ringkasan draf diperiksa ulang saat konfirmasi. Jika nama atau harga yang ditinjau berubah, terbitkan versi ringkasan baru dan minta persetujuan ulang. Produk nonaktif perlu diganti sebelum menyimpan. Draf yang sudah dikonfirmasi mengembalikan hasil yang sama ketika dipanggil ulang.

## Order, katalog, pengguna, dan laporan

- **Order** dapat diedit ketika `pending`. Android menyediakan aksi `pending → diproses → selesai` dan `pending → dibatalkan`. Status akhir bersifat terminal.
- **Katalog** dapat dikelola admin. Harga yang berubah tercatat bersama waktu dan pembuat perubahan. Produk nonaktif tetap terlihat di histori dan dikeluarkan dari pemilihan item baru.
- **Pengguna** dibuat admin melalui backend yang mengelola akun Firebase Auth dan profil Supabase. Status aktif profil diperiksa pada setiap akses. Admin juga menautkan ID Telegram staf dan mencabut akses yang sudah tidak dipakai.
- **Dashboard dan laporan** menampilkan jumlah order, pendapatan, dan produk terlaris per periode Asia/Jakarta. Order `dibatalkan` dikeluarkan. Harga transaksi memakai snapshot item.

## Realtime dan kegagalan

- Saat perubahan berhasil disimpan, Supabase mengirim sinyal privat. Android memuat ulang data terkait lewat API saat menerima sinyal dan setelah tersambung kembali, termasuk perubahan yang berasal dari Telegram atau MCP.
- Jika koneksi putus sebelum pengiriman, tampilkan “Koneksi terputus. Order belum dikirim.” Jika respons terputus setelah pengiriman, tampilkan “Status penyimpanan belum diketahui.” Pertahankan isian layar dan tampilkan **Coba Lagi**. Pakai kunci yang sama untuk permintaan yang hasilnya belum diketahui.
- Bila order sudah berubah sejak layar edit dibuka, tampilkan konflik dan minta pengguna memuat versi terbaru. Bila API menolak produk, kuantitas, atau bidang lain, arahkan pengguna ke isian terkait.
- Bot mengembalikan ID order setelah backend menyimpan. Pengiriman ulang webhook atau klik ganda memakai hasil yang sama. Kegagalan sementara dapat dicoba ulang tanpa membuat order tambahan.
