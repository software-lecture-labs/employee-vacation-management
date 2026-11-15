-- =====================================================================
-- 01_schema.sql
-- Amaç  : leave_mgmt şemasını, 9 tabloyu, kısıtları ve indeksleri kurar.
-- Sıra  : 01_schema → 02_seed_leave_types → 03_security
-- Ortam : Supabase SQL Editor. Testcontainers ve local-db aynı dosyayı kullanır.
-- Kural : Çalıştırıldıktan sonra bu dosya değiştirilmez; her şema
--         değişikliği yeni numaralı bir script olur (04_...sql).
-- Sahip : Zehra (veritabanı sorumlusu)
-- =====================================================================

begin;

create schema if not exists leave_mgmt;

-- 1) departments ------------------------------------------------------
create table if not exists leave_mgmt.departments (
    id          bigserial    primary key,
    name        varchar(100) not null,
    created_at  timestamptz  not null default now(),
    updated_at  timestamptz  not null default now(),
    constraint uq_departments_name unique (name)
);

-- 2) employees --------------------------------------------------------
create table if not exists leave_mgmt.employees (
    id             bigserial    primary key,
    first_name     varchar(50)  not null,
    last_name      varchar(50)  not null,
    email          varchar(120) not null,
    password_hash  varchar(100) not null,
    hire_date      date         not null,
    birth_date     date         not null,
    role           varchar(20)  not null,
    active         boolean      not null default true,
    department_id  bigint       not null,
    manager_id     bigint,
    version        bigint       not null default 0,
    created_at     timestamptz  not null default now(),
    updated_at     timestamptz  not null default now(),
    constraint ck_employees_role check (role in ('EMPLOYEE', 'MANAGER', 'HR')),
    constraint ck_employees_not_own_manager check (manager_id <> id),
    constraint fk_employees_department foreign key (department_id)
        references leave_mgmt.departments (id),
    constraint fk_employees_manager foreign key (manager_id)
        references leave_mgmt.employees (id)
);
create unique index if not exists uq_employees_email_lower
    on leave_mgmt.employees (lower(email));

-- 3) leave_types ------------------------------------------------------
create table if not exists leave_mgmt.leave_types (
    id                    bigserial    primary key,
    code                  varchar(30)  not null,
    name                  varchar(50)  not null,
    deducts_from_balance  boolean      not null,
    annual_limit_days     numeric(4,1),
    active                boolean      not null default true,
    created_at            timestamptz  not null default now(),
    updated_at            timestamptz  not null default now(),
    constraint uq_leave_types_code unique (code),
    constraint uq_leave_types_name unique (name)
);

-- 4) leave_balances ---------------------------------------------------
create table if not exists leave_mgmt.leave_balances (
    id             bigserial    primary key,
    employee_id    bigint       not null,
    leave_type_id  bigint       not null,
    year           smallint     not null,
    entitled_days  numeric(4,1) not null,
    used_days      numeric(4,1) not null default 0,
    version        bigint       not null default 0,
    created_at     timestamptz  not null default now(),
    updated_at     timestamptz  not null default now(),
    constraint uq_leave_balances_employee_type_year
        unique (employee_id, leave_type_id, year),
    constraint ck_leave_balances_used_days
        check (used_days >= 0 and used_days <= entitled_days),
    constraint fk_leave_balances_employee foreign key (employee_id)
        references leave_mgmt.employees (id),
    constraint fk_leave_balances_leave_type foreign key (leave_type_id)
        references leave_mgmt.leave_types (id)
);

-- 5) leave_requests ---------------------------------------------------
create table if not exists leave_mgmt.leave_requests (
    id                     bigserial    primary key,
    employee_id            bigint       not null,
    leave_type_id          bigint       not null,
    start_date             date         not null,
    end_date               date         not null,
    working_days           numeric(4,1) not null,
    reason                 varchar(500),
    status                 varchar(30)  not null,
    approver_id            bigint,
    decision_note          varchar(500),
    decided_at             timestamptz,
    cancel_request_reason  varchar(500),
    cancel_requested_at    timestamptz,
    cancelled_by           bigint,
    cancel_reason          varchar(500),
    cancelled_at           timestamptz,
    version                bigint       not null default 0,
    created_at             timestamptz  not null default now(),
    updated_at             timestamptz  not null default now(),
    constraint ck_leave_requests_dates check (end_date >= start_date),
    constraint ck_leave_requests_working_days check (working_days > 0),
    constraint ck_leave_requests_status check (status in (
        'PENDING', 'APPROVED', 'REJECTED', 'WITHDRAWN',
        'CANCEL_REQUESTED', 'CANCELLED', 'CANCEL_REQUEST_EXPIRED')),
    constraint fk_leave_requests_employee foreign key (employee_id)
        references leave_mgmt.employees (id),
    constraint fk_leave_requests_leave_type foreign key (leave_type_id)
        references leave_mgmt.leave_types (id),
    constraint fk_leave_requests_approver foreign key (approver_id)
        references leave_mgmt.employees (id),
    constraint fk_leave_requests_cancelled_by foreign key (cancelled_by)
        references leave_mgmt.employees (id)
);
create index if not exists ix_leave_requests_employee_status
    on leave_mgmt.leave_requests (employee_id, status);
create index if not exists ix_leave_requests_employee_dates
    on leave_mgmt.leave_requests (employee_id, start_date, end_date);
create index if not exists ix_leave_requests_approver_status
    on leave_mgmt.leave_requests (approver_id, status);

-- 6) leave_request_history (sadece eklenir) ---------------------------
create table if not exists leave_mgmt.leave_request_history (
    id                bigserial    primary key,
    leave_request_id  bigint       not null,
    from_status       varchar(30),
    to_status         varchar(30)  not null,
    actor_id          bigint,
    note              varchar(500),
    occurred_at       timestamptz  not null default now(),
    created_at        timestamptz  not null default now(),
    updated_at        timestamptz  not null default now(),
    constraint fk_leave_request_history_request foreign key (leave_request_id)
        references leave_mgmt.leave_requests (id),
    constraint fk_leave_request_history_actor foreign key (actor_id)
        references leave_mgmt.employees (id)
);
create index if not exists ix_leave_request_history_request
    on leave_mgmt.leave_request_history (leave_request_id);
create index if not exists ix_leave_request_history_occurred_at
    on leave_mgmt.leave_request_history (occurred_at);

-- 7) balance_transactions (sadece eklenir) ----------------------------
create table if not exists leave_mgmt.balance_transactions (
    id                bigserial    primary key,
    leave_balance_id  bigint       not null,
    leave_request_id  bigint,
    type              varchar(20)  not null,
    days              numeric(4,1) not null,
    note              varchar(500),
    created_by        bigint,
    created_at        timestamptz  not null default now(),
    updated_at        timestamptz  not null default now(),
    constraint ck_balance_transactions_type
        check (type in ('ACCRUAL', 'DEDUCTION', 'REFUND')),
    constraint ck_balance_transactions_days check (days > 0),
    constraint fk_balance_transactions_balance foreign key (leave_balance_id)
        references leave_mgmt.leave_balances (id),
    constraint fk_balance_transactions_request foreign key (leave_request_id)
        references leave_mgmt.leave_requests (id),
    constraint fk_balance_transactions_created_by foreign key (created_by)
        references leave_mgmt.employees (id)
);

-- 8) public_holidays (bonus, FR-38) -----------------------------------
create table if not exists leave_mgmt.public_holidays (
    id            bigserial    primary key,
    holiday_date  date         not null,
    name          varchar(100) not null,
    created_at    timestamptz  not null default now(),
    updated_at    timestamptz  not null default now(),
    constraint uq_public_holidays_date unique (holiday_date)
);

-- 9) annual_accrual_runs (BR-11) --------------------------------------
create table if not exists leave_mgmt.annual_accrual_runs (
    id               bigserial    primary key,
    year             smallint     not null,
    run_at           timestamptz  not null default now(),
    run_by           bigint,
    processed_count  integer      not null,
    created_at       timestamptz  not null default now(),
    updated_at       timestamptz  not null default now(),
    constraint uq_annual_accrual_runs_year unique (year),
    constraint fk_annual_accrual_runs_run_by foreign key (run_by)
        references leave_mgmt.employees (id)
);

commit;
