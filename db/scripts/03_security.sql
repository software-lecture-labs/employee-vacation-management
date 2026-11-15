-- =====================================================================
-- 03_security.sql
-- Amaç  : leave_mgmt şemasındaki her tabloda Row Level Security'yi açar.
-- Sıra  : 01_schema'dan sonra çalışır.
-- Not   : Politika tanımlanmaz. Politikasız RLS, Data API rollerinin
--         (anon, authenticated) hiçbir satırı görmemesi demektir.
--         Uygulama tabloların sahibi olan kullanıcıyla bağlandığı için
--         RLS'ten etkilenmez. Tekrar çalıştırılması zararsızdır.
-- Sahip : Zehra (veritabanı sorumlusu)
-- =====================================================================

begin;

alter table leave_mgmt.departments           enable row level security;
alter table leave_mgmt.employees             enable row level security;
alter table leave_mgmt.leave_types           enable row level security;
alter table leave_mgmt.leave_balances        enable row level security;
alter table leave_mgmt.leave_requests        enable row level security;
alter table leave_mgmt.leave_request_history enable row level security;
alter table leave_mgmt.balance_transactions  enable row level security;
alter table leave_mgmt.public_holidays       enable row level security;
alter table leave_mgmt.annual_accrual_runs   enable row level security;

commit;
