// ============================================================
// OficJus Drive — Migration: Licenciamento e Pagamentos
// ============================================================
// Adiciona campos de licenciamento à tabela profiles
// Cria tabela de histórico de pagamentos
// ============================================================

-- 1. Campos de licenciamento na profiles (se não existirem)
alter table public.profiles add column if not exists plano text default 'drive'
  check (plano in ('drive', 'enterprise'));

alter table public.profiles add column if not exists status_assinatura text default 'trial'
  check (status_assinatura in ('ativo', 'inadimplente', 'cancelado', 'trial', 'vitalicio'));

alter table public.profiles add column if not exists trial_inicio timestamptz default now();

alter table public.profiles add column if not exists trial_fim timestamptz;

alter table public.profiles add column if not exists assinatura_inicio timestamptz;

alter table public.profiles add column if not exists assinatura_fim timestamptz;

alter table public.profiles add column if not exists ultimo_pagamento timestamptz;

alter table public.profiles add column if not exists gateway_customer_id text;

alter table public.profiles add column if not exists gateway_card_token text;

alter table public.profiles add column if not exists cartao_bandeira text;

alter table public.profiles add column if not exists cartao_ultimos_digitos char(4);


-- 2. Tabela de histórico de pagamentos
create table if not exists public.pagamentos_drive (
  id uuid primary key default gen_random_uuid(),
  user_id uuid references auth.users(id) on delete cascade not null,
  gateway_payment_id text,
  valor numeric not null,
  status text not null check (status in ('pending', 'received', 'overdue', 'refunded', 'canceled')),
  forma_pagamento text,
  data_vencimento date,
  data_pagamento timestamptz,
  created_at timestamptz default now()
);

-- Índices
create index if not exists idx_pagamentos_drive_user on pagamentos_drive(user_id);
create index if not exists idx_pagamentos_drive_status on pagamentos_drive(status);


-- 3. RLS
alter table public.pagamentos_drive enable row level security;

drop policy if exists "Usuário vê próprios pagamentos" on pagamentos_drive;
create policy "Usuário vê próprios pagamentos"
  on pagamentos_drive for select
  using (auth.uid() = user_id);

-- Admin pode ver todos (via service_role)
drop policy if exists "Admin vê todos os pagamentos" on pagamentos_drive;
create policy "Admin vê todos os pagamentos"
  on pagamentos_drive for select
  using (auth.role() = 'service_role');


-- 4. Trigger: ao criar usuário, seta trial
create or replace function public.handle_new_user_drive()
returns trigger as $$
begin
  insert into public.profiles (id, full_name, plano, status_assinatura, trial_inicio, trial_fim)
  values (
    new.id,
    new.raw_user_meta_data ->> 'full_name',
    coalesce(new.raw_user_meta_data ->> 'plano', 'drive'),
    'trial',
    now(),
    now() + interval '7 days'
  );
  return new;
end;
$$ language plpgsql security definer;

-- Drop trigger se já existir (para evitar conflito com o trigger do project-oficjus)
drop trigger if exists on_auth_user_created_drive on auth.users;

-- Só cria se não existir outro trigger de profile
do $$
begin
  if not exists (
    select 1 from pg_trigger
    where tgname = 'on_auth_user_created_drive'
  ) then
    create trigger on_auth_user_created_drive
      after insert on auth.users
      for each row
      execute function public.handle_new_user_drive();
  end if;
end;
$$;