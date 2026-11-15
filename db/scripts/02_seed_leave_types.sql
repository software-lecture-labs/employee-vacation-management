-- =====================================================================
-- 02_seed_leave_types.sql
-- Amaç  : Uygulamanın hazır geldiği 4 izin türünü ekler (FR-13).
-- Sıra  : 01_schema'dan sonra çalışır.
-- Not   : code alanı değişmez (ANNUAL hak edişte koduyla bulunur).
--         Tekrar çalıştırılırsa var olan türler atlanır (on conflict).
--         Yıllık iznin sınırı kıdem ve yaşa göre hesaplanır (FR-33), bu
--         yüzden annual_limit_days boştur. Mazeret sınırı 5 gündür (BR-04).
--         Hastalık ve ücretsiz izin bakiyeden düşmez (BR-03).
-- Sahip : Zehra (veritabanı sorumlusu)
-- =====================================================================

begin;

insert into leave_mgmt.leave_types (code, name, deducts_from_balance, annual_limit_days, active)
values
    ('ANNUAL', 'Yıllık izin',   true,  null, true),
    ('EXCUSE', 'Mazeret izni',  true,  5.0,  true),
    ('SICK',   'Hastalık izni', false, null, true),
    ('UNPAID', 'Ücretsiz izin', false, null, true)
on conflict (code) do nothing;

commit;
