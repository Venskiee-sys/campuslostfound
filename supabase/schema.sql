-- ============================================================
-- UniFind — Supabase schema (replaces the old Firebase backend)
--
-- Run this ONCE in:  Supabase Dashboard → SQL Editor → New query → Run
-- It creates all tables, row-level security policies, realtime
-- publication entries, the profile trigger and the storage bucket.
-- ============================================================

-- ------------------------------------------------------------
-- 1. TABLES
-- ------------------------------------------------------------

-- Profiles (was Firestore "users")
create table if not exists public.users (
  uid          text primary key,
  full_name    text not null default '',
  student_id   text not null default '',
  email        text not null default '',
  course       text not null default '',
  year_level   text not null default '',
  phone_number text not null default ''
);

-- Lost & found reports (was Firestore "items")
create table if not exists public.items (
  id            uuid primary key default gen_random_uuid(),
  name          text not null default '',
  category      text not null default '',
  location      text not null default '',
  date          text not null default '',
  description   text not null default '',
  type          text not null default '',          -- 'Lost' | 'Found'
  reported_by   text not null default '',
  reporter_name text not null default '',
  created_at    bigint not null default (extract(epoch from now()) * 1000)::bigint,
  image_url     text,
  status        text not null default 'Open'       -- 'Open' | 'Resolved'
);

-- Conversations (was Firestore "chats")
create table if not exists public.chats (
  id                uuid primary key default gen_random_uuid(),
  participant_ids   text[] not null default '{}',
  participant_names jsonb  not null default '{}'::jsonb,  -- { uid: "Name" }
  last_message      text   not null default '',
  last_timestamp    bigint not null default (extract(epoch from now()) * 1000)::bigint
);

-- Chat messages (was the "messages" subcollection under each chat)
create table if not exists public.messages (
  id         uuid primary key default gen_random_uuid(),
  chat_id    uuid not null references public.chats(id) on delete cascade,
  sender_id  text not null default '',
  content    text not null default '',
  created_at bigint not null default (extract(epoch from now()) * 1000)::bigint
);

-- In-app notifications (was Firestore "notifications")
create table if not exists public.notifications (
  id          uuid primary key default gen_random_uuid(),
  user_id     text   not null default '',
  title       text   not null default '',
  message     text   not null default '',
  type        text   not null default 'Info',      -- 'Message' | 'Resolved' | 'System' | ...
  created_at  bigint not null default (extract(epoch from now()) * 1000)::bigint,
  seen        boolean not null default false,
  sender_name text,
  related_id  text
);

-- Indexes for the app's hot paths
create index if not exists items_status_idx        on public.items (status);
create index if not exists items_reported_by_idx   on public.items (reported_by);
create index if not exists items_type_idx          on public.items (type);
create index if not exists chats_participant_idx   on public.chats using gin (participant_ids);
create index if not exists messages_chat_idx       on public.messages (chat_id);
create index if not exists notifications_user_idx  on public.notifications (user_id);

-- ------------------------------------------------------------
-- 2. ROW LEVEL SECURITY
-- ------------------------------------------------------------

alter table public.users        enable row level security;
alter table public.items        enable row level security;
alter table public.chats        enable row level security;
alter table public.messages     enable row level security;
alter table public.notifications enable row level security;

-- users: you only ever see and touch your own profile
drop policy if exists "users_select_own" on public.users;
create policy "users_select_own" on public.users
  for select to authenticated
  using (uid = auth.uid()::text);

drop policy if exists "users_insert_own" on public.users;
create policy "users_insert_own" on public.users
  for insert to authenticated
  with check (uid = auth.uid()::text);

drop policy if exists "users_update_own" on public.users;
create policy "users_update_own" on public.users
  for update to authenticated
  using (uid = auth.uid()::text)
  with check (uid = auth.uid()::text);

-- items: any signed-in student can browse; you can only report as yourself
drop policy if exists "items_select_all" on public.items;
create policy "items_select_all" on public.items
  for select to authenticated
  using (true);

drop policy if exists "items_insert_own" on public.items;
create policy "items_insert_own" on public.items
  for insert to authenticated
  with check (reported_by = auth.uid()::text);

drop policy if exists "items_update_auth" on public.items;
create policy "items_update_auth" on public.items
  for update to authenticated
  using (true)
  with check (true);

-- chats: only the two participants
drop policy if exists "chats_select" on public.chats;
create policy "chats_select" on public.chats
  for select to authenticated
  using (participant_ids @> array[auth.uid()::text]);

drop policy if exists "chats_insert" on public.chats;
create policy "chats_insert" on public.chats
  for insert to authenticated
  with check (participant_ids @> array[auth.uid()::text]);

drop policy if exists "chats_update" on public.chats;
create policy "chats_update" on public.chats
  for update to authenticated
  using (participant_ids @> array[auth.uid()::text])
  with check (participant_ids @> array[auth.uid()::text]);

-- messages: readable/writable only inside chats you participate in
drop policy if exists "messages_select" on public.messages;
create policy "messages_select" on public.messages
  for select to authenticated
  using (exists (
    select 1 from public.chats c
    where c.id = chat_id and c.participant_ids @> array[auth.uid()::text]
  ));

drop policy if exists "messages_insert" on public.messages;
create policy "messages_insert" on public.messages
  for insert to authenticated
  with check (
    sender_id = auth.uid()::text
    and exists (
      select 1 from public.chats c
      where c.id = chat_id and c.participant_ids @> array[auth.uid()::text]
    )
  );

-- notifications: you receive them and mark them read; any signed-in user may
-- create one (that's how a chat partner notifies you)
drop policy if exists "notifications_select" on public.notifications;
create policy "notifications_select" on public.notifications
  for select to authenticated
  using (user_id = auth.uid()::text);

drop policy if exists "notifications_insert" on public.notifications;
create policy "notifications_insert" on public.notifications
  for insert to authenticated
  with check (true);

drop policy if exists "notifications_update" on public.notifications;
create policy "notifications_update" on public.notifications
  for update to authenticated
  using (user_id = auth.uid()::text)
  with check (user_id = auth.uid()::text);

-- ------------------------------------------------------------
-- 3. PROFILE ROW AUTO-CREATION
--    The app also upserts the profile itself; this trigger makes sure a row
--    exists even when someone signs up with email confirmation enabled.
-- ------------------------------------------------------------

create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  insert into public.users (uid, email)
  values (new.id, new.email)
  on conflict (uid) do nothing;
  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

-- ------------------------------------------------------------
-- 4. REALTIME (live feeds, chats, notifications — the Firestore listeners)
-- ------------------------------------------------------------

do $$ begin
  alter publication supabase_realtime add table public.items;
exception when duplicate_object then null; end $$;

do $$ begin
  alter publication supabase_realtime add table public.chats;
exception when duplicate_object then null; end $$;

do $$ begin
  alter publication supabase_realtime add table public.messages;
exception when duplicate_object then null; end $$;

do $$ begin
  alter publication supabase_realtime add table public.notifications;
exception when duplicate_object then null; end $$;

-- ------------------------------------------------------------
-- 5. STORAGE BUCKET (item photos — was ImgBB / Firebase Storage)
-- ------------------------------------------------------------

insert into storage.buckets (id, name, public)
values ('item-images', 'item-images', true)
on conflict (id) do nothing;

-- storage.buckets ships with row-level security ON and no policies, so the
-- Storage API (which resolves the bucket as the caller's role, not as
-- service_role) sees nothing for anon/authenticated and answers
-- "Bucket not found" — which fails every upload. Let those roles resolve
-- exactly this one bucket; no other bucket becomes visible.
drop policy if exists "item_images_bucket_visible" on storage.buckets;
create policy "item_images_bucket_visible" on storage.buckets
  for select to anon, authenticated
  using (id = 'item-images');

-- Read is `to public`, not just authenticated: the app renders photos from
-- publicUrl(...) with a plain image loader that carries no auth header, so
-- the fetch arrives as anon.
drop policy if exists "item_images_read" on storage.objects;
create policy "item_images_read" on storage.objects
  for select to public
  using (bucket_id = 'item-images');

drop policy if exists "item_images_upload" on storage.objects;
create policy "item_images_upload" on storage.objects
  for insert to authenticated
  with check (bucket_id = 'item-images');
