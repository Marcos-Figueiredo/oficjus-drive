-- ============================================================
-- DRIVE HÍBRIDO — Migration SQL (banco JUSTICA)
-- ============================================================
-- Adiciona campos de plano/assinatura à tabela profiles
-- Cria tabela enderecos_drive para o modo Drive Standalone
-- ============================================================

-- 1. Campos de plano e assinatura na profiles
alter table public.profiles add column if not exists plano text default 'drive'
  check (plano in ('drive', 'enterprise'));

alter table public.profiles add column if not exists status_assinatura text default 'trial'
  check (status_assinatura in ('ativo', 'inadimplente', 'cancelado', 'trial'));

alter table public.profiles add column if not exists trial_inicio timestamptz default now();

alter table public.profiles add column if not exists trial_fim timestamptz;

alter table public.profiles add column if not exists assinatura_inicio timestamptz;

alter table public.profiles add column if not exists assinatura_fim timestamptz;

alter table public.profiles add column if not exists ultimo_pagamento timestamptz;

alter table public.profiles add column if not exists gateway_customer_id text;

alter table public.profiles add column if not exists gateway_card_token text;

alter table public.profiles add column if not exists cartao_bandeira text;

alter table public.profiles add column if not exists cartao_ultimos_digitos char(4);


-- 2. Tabela de endereços do Drive Standalone
create table if not exists public.enderecos_drive (
  id uuid primary key default gen_random_uuid(),
  user_id uuid references auth.users(id) on delete cascade not null,
  logradouro text not null,
  numero text,
  bairro text,
  cidade text,
  estado char(2),
  cep text,
  latitude numeric,
  longitude numeric,
  ordem smallint,
  rota_grupo_id text,
  created_at timestamptz default now()
);

-- Índices
create index if not exists idx_enderecos_drive_user on enderecos_drive(user_id);
create index if not exists idx_enderecos_drive_rota on enderecos_drive(rota_grupo_id);


-- 3. RLS (Row Level Security)
alter table public.enderecos_drive enable row level security;

-- Usuário só vê seus próprios endereços
drop policy if exists "Usuário vê próprios endereços" on enderecos_drive;
create policy "Usuário vê próprios endereços"
  on enderecos_drive for select
  using (auth.uid() = user_id);

-- Usuário pode inserir seus próprios endereços
drop policy if exists "Usuário insere próprios endereços" on enderecos_drive;
create policy "Usuário insere próprios endereços"
  on enderecos_drive for insert
  with check (auth.uid() = user_id);

-- Usuário pode deletar seus próprios endereços
drop policy if exists "Usuário deleta próprios endereços" on enderecos_drive;
create policy "Usuário deleta próprios endereços"
  on enderecos_drive for delete
  using (auth.uid() = user_id);