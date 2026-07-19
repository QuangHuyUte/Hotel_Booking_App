-- Run this in Supabase SQL Editor when a cabinOwner account must act as the app admin.
-- Your users_role_check currently allows 'cabinOwner', not 'admin'.
-- The Android app therefore treats role = 'cabinOwner' as the admin/system manager role.

create or replace function public.current_app_role()
returns text
language sql
stable
as $$
  select coalesce(
    (
      select u.role
      from public.users u
      where lower(u.email) = lower(auth.jwt() ->> 'email')
      limit 1
    ),
    ''
  );
$$;

create or replace function public.is_app_admin()
returns boolean
language sql
stable
as $$
  select public.current_app_role() in ('cabinOwner', 'admin');
$$;

alter table public.cabins enable row level security;
drop policy if exists cabinowner_admin_select_cabins on public.cabins;
drop policy if exists cabinowner_admin_insert_cabins on public.cabins;
drop policy if exists cabinowner_admin_update_cabins on public.cabins;
drop policy if exists cabinowner_admin_delete_cabins on public.cabins;
create policy cabinowner_admin_select_cabins on public.cabins
for select using (public.is_app_admin());
create policy cabinowner_admin_insert_cabins on public.cabins
for insert with check (public.is_app_admin());
create policy cabinowner_admin_update_cabins on public.cabins
for update using (public.is_app_admin()) with check (public.is_app_admin());
create policy cabinowner_admin_delete_cabins on public.cabins
for delete using (public.is_app_admin());

alter table public.conversations enable row level security;
drop policy if exists cabinowner_admin_select_conversations on public.conversations;
drop policy if exists cabinowner_admin_insert_conversations on public.conversations;
create policy cabinowner_admin_select_conversations on public.conversations
for select using (public.is_app_admin());
create policy cabinowner_admin_insert_conversations on public.conversations
for insert with check (public.is_app_admin());

alter table public.messages enable row level security;
drop policy if exists cabinowner_admin_select_messages on public.messages;
drop policy if exists cabinowner_admin_insert_messages on public.messages;
create policy cabinowner_admin_select_messages on public.messages
for select using (public.is_app_admin());
create policy cabinowner_admin_insert_messages on public.messages
for insert with check (public.is_app_admin());

alter table public.settings enable row level security;
drop policy if exists cabinowner_admin_select_settings on public.settings;
drop policy if exists cabinowner_admin_insert_settings on public.settings;
drop policy if exists cabinowner_admin_update_settings on public.settings;
create policy cabinowner_admin_select_settings on public.settings
for select using (public.is_app_admin());
create policy cabinowner_admin_insert_settings on public.settings
for insert with check (public.is_app_admin());
create policy cabinowner_admin_update_settings on public.settings
for update using (public.is_app_admin()) with check (public.is_app_admin());

alter table public.bookings enable row level security;
drop policy if exists cabinowner_admin_select_bookings on public.bookings;
drop policy if exists cabinowner_admin_update_bookings on public.bookings;
create policy cabinowner_admin_select_bookings on public.bookings
for select using (public.is_app_admin());
create policy cabinowner_admin_update_bookings on public.bookings
for update using (public.is_app_admin()) with check (public.is_app_admin());

alter table public.payments enable row level security;
drop policy if exists cabinowner_admin_select_payments on public.payments;
drop policy if exists cabinowner_admin_insert_payments on public.payments;
drop policy if exists cabinowner_admin_update_payments on public.payments;
create policy cabinowner_admin_select_payments on public.payments
for select using (public.is_app_admin());
create policy cabinowner_admin_insert_payments on public.payments
for insert with check (public.is_app_admin());
create policy cabinowner_admin_update_payments on public.payments
for update using (public.is_app_admin()) with check (public.is_app_admin());

-- Optional checks after running:
-- select public.current_app_role(), public.is_app_admin();
-- select count(*) from public.cabins;
-- If the app status still says "Showing 2 of 2 cabin(s)", Supabase is only returning 2
-- rows to the current session. Log out/in once, then re-check these two queries.
