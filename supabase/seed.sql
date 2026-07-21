-- Supabase seed for Serein Stay hotel management demo
-- Safe to rerun after running supabase/database.sql once.

begin;

create extension if not exists "pgcrypto";

alter table public.amenities add column if not exists category varchar default 'General';
alter table public.users drop constraint if exists users_role_check;
update public.users set role = 'manager' where role in ('cabinOwner', 'admin');
alter table public.users add constraint users_role_check check (role in ('customer', 'manager'));
alter table public.users enable row level security;
drop policy if exists "users_public_read" on public.users;
create policy "users_public_read"
on public.users for select
to anon, authenticated
using (true);
drop policy if exists "users_create_own_customer_profile" on public.users;
create policy "users_create_own_customer_profile"
on public.users for insert
to authenticated
with check (_id = auth.uid() and role = 'customer');
drop policy if exists "users_update_own_profile" on public.users;
create policy "users_update_own_profile"
on public.users for update
to authenticated
using (_id = auth.uid())
with check (_id = auth.uid());

alter table public.cabins add column if not exists latitude numeric;
alter table public.cabins add column if not exists longitude numeric;
alter table public.cabins add column if not exists "mapPlaceId" text;
alter table public.cabins add column if not exists address text;
alter table public.cabins add column if not exists district varchar;
alter table public.cabins add column if not exists "propertyType" varchar default 'Hotel';
alter table public.cabins add column if not exists "starRating" integer default 3;
alter table public.cabins add column if not exists "reviewScore" numeric default 8.6;
alter table public.cabins add column if not exists "reviewCount" integer default 0;
alter table public.cabins add column if not exists "googleMapsUrl" text;
alter table public.cabins add column if not exists "isActive" boolean default true;

create table if not exists public.room_types (
  _id uuid primary key default gen_random_uuid(),
  "cabinId" uuid not null references public.cabins(_id) on delete cascade,
  name varchar not null,
  category varchar not null default 'Standard',
  description text,
  "maxGuests" integer not null default 2,
  "maxAdults" integer not null default 2,
  "totalRooms" integer not null default 1,
  "basePrice" numeric not null,
  beds varchar,
  "bedType" varchar default 'Queen',
  "bedCount" integer not null default 1,
  "sleepingCapacity" integer not null default 2,
  "bedSummary" varchar,
  "bedConfig" jsonb default '[]'::jsonb,
  "bedWidthM" numeric default 1.6,
  "bedLengthM" numeric default 2.0,
  size varchar,
  "sizeM2" integer default 24,
  "hasLivingRoom" boolean default false,
  amenities text,
  image text,
  "isActive" boolean default true,
  "createdAt" timestamp without time zone default now(),
  "updatedAt" timestamp without time zone default now(),
  constraint room_types_valid_guests check ("maxGuests" > 0),
  constraint room_types_valid_adults check ("maxAdults" > 0),
  constraint room_types_valid_bed_count check ("bedCount" > 0),
  constraint room_types_valid_sleeping_capacity check ("sleepingCapacity" >= "maxAdults"),
  constraint room_types_valid_total check ("totalRooms" >= 0),
  unique ("cabinId", name)
);
do $$
begin
  if exists (select 1 from information_schema.columns where table_schema = 'public' and table_name = 'room_types' and column_name = 'cabinid')
     and not exists (select 1 from information_schema.columns where table_schema = 'public' and table_name = 'room_types' and column_name = 'cabinId') then
    alter table public.room_types rename column cabinid to "cabinId";
  end if;
  if exists (select 1 from information_schema.columns where table_schema = 'public' and table_name = 'room_types' and column_name = 'maxguests') then
    alter table public.room_types rename column maxguests to "maxGuests";
  end if;
  if exists (select 1 from information_schema.columns where table_schema = 'public' and table_name = 'room_types' and column_name = 'maxadults') then
    alter table public.room_types rename column maxadults to "maxAdults";
  end if;
  if exists (select 1 from information_schema.columns where table_schema = 'public' and table_name = 'room_types' and column_name = 'totalrooms') then
    alter table public.room_types rename column totalrooms to "totalRooms";
  end if;
  if exists (select 1 from information_schema.columns where table_schema = 'public' and table_name = 'room_types' and column_name = 'baseprice') then
    alter table public.room_types rename column baseprice to "basePrice";
  end if;
  if exists (select 1 from information_schema.columns where table_schema = 'public' and table_name = 'room_types' and column_name = 'bedtype') then
    alter table public.room_types rename column bedtype to "bedType";
  end if;
  if exists (select 1 from information_schema.columns where table_schema = 'public' and table_name = 'room_types' and column_name = 'bedcount') then
    alter table public.room_types rename column bedcount to "bedCount";
  end if;
  if exists (select 1 from information_schema.columns where table_schema = 'public' and table_name = 'room_types' and column_name = 'sleepingcapacity') then
    alter table public.room_types rename column sleepingcapacity to "sleepingCapacity";
  end if;
  if exists (select 1 from information_schema.columns where table_schema = 'public' and table_name = 'room_types' and column_name = 'bedsummary') then
    alter table public.room_types rename column bedsummary to "bedSummary";
  end if;
  if exists (select 1 from information_schema.columns where table_schema = 'public' and table_name = 'room_types' and column_name = 'bedconfig') then
    alter table public.room_types rename column bedconfig to "bedConfig";
  end if;
  if exists (select 1 from information_schema.columns where table_schema = 'public' and table_name = 'room_types' and column_name = 'bedwidthm') then
    alter table public.room_types rename column bedwidthm to "bedWidthM";
  end if;
  if exists (select 1 from information_schema.columns where table_schema = 'public' and table_name = 'room_types' and column_name = 'bedlengthm') then
    alter table public.room_types rename column bedlengthm to "bedLengthM";
  end if;
  if exists (select 1 from information_schema.columns where table_schema = 'public' and table_name = 'room_types' and column_name = 'sizem2') then
    alter table public.room_types rename column sizem2 to "sizeM2";
  end if;
  if exists (select 1 from information_schema.columns where table_schema = 'public' and table_name = 'room_types' and column_name = 'haslivingroom') then
    alter table public.room_types rename column haslivingroom to "hasLivingRoom";
  end if;
  if exists (select 1 from information_schema.columns where table_schema = 'public' and table_name = 'room_types' and column_name = 'isactive') then
    alter table public.room_types rename column isactive to "isActive";
  end if;
  if exists (select 1 from information_schema.columns where table_schema = 'public' and table_name = 'room_types' and column_name = 'createdat') then
    alter table public.room_types rename column createdat to "createdAt";
  end if;
  if exists (select 1 from information_schema.columns where table_schema = 'public' and table_name = 'room_types' and column_name = 'updatedat') then
    alter table public.room_types rename column updatedat to "updatedAt";
  end if;
end $$;
alter table public.room_types enable row level security;
drop policy if exists "room_types_demo_access" on public.room_types;
create policy "room_types_demo_access"
on public.room_types for all
to anon, authenticated
using (true)
with check (true);
grant select, insert, update, delete on table public.room_types to anon, authenticated;
alter table public.room_types add column if not exists category varchar not null default 'Standard';
alter table public.room_types add column if not exists "maxAdults" integer not null default 2;
alter table public.room_types add column if not exists "bedType" varchar default 'Queen';
alter table public.room_types add column if not exists "bedCount" integer not null default 1;
alter table public.room_types add column if not exists "sleepingCapacity" integer not null default 2;
alter table public.room_types add column if not exists "bedSummary" varchar;
alter table public.room_types add column if not exists "bedConfig" jsonb default '[]'::jsonb;
alter table public.room_types add column if not exists "bedWidthM" numeric default 1.6;
alter table public.room_types add column if not exists "bedLengthM" numeric default 2.0;
alter table public.room_types add column if not exists "sizeM2" integer default 24;
alter table public.room_types add column if not exists "hasLivingRoom" boolean default false;
alter table public.room_types drop constraint if exists room_types_valid_adults;
alter table public.room_types add constraint room_types_valid_adults check ("maxAdults" > 0);
alter table public.room_types drop constraint if exists room_types_valid_bed_count;
alter table public.room_types add constraint room_types_valid_bed_count check ("bedCount" > 0);
alter table public.room_types drop constraint if exists room_types_valid_sleeping_capacity;
alter table public.room_types add constraint room_types_valid_sleeping_capacity check ("sleepingCapacity" >= "maxAdults");

create table if not exists public.room_inventory (
  _id uuid primary key default gen_random_uuid(),
  "roomTypeId" uuid not null references public.room_types(_id) on delete cascade,
  date date not null,
  "availableRooms" integer not null default 0,
  "priceOverride" numeric,
  "isClosed" boolean default false,
  "createdAt" timestamp without time zone default now(),
  "updatedAt" timestamp without time zone default now(),
  constraint room_inventory_available_nonnegative check ("availableRooms" >= 0),
  unique ("roomTypeId", date)
);
do $$
begin
  if exists (select 1 from information_schema.columns where table_schema = 'public' and table_name = 'room_inventory' and column_name = 'roomtypeid')
     and not exists (select 1 from information_schema.columns where table_schema = 'public' and table_name = 'room_inventory' and column_name = 'roomTypeId') then
    alter table public.room_inventory rename column roomtypeid to "roomTypeId";
  end if;
  if exists (select 1 from information_schema.columns where table_schema = 'public' and table_name = 'room_inventory' and column_name = 'availablerooms') then
    alter table public.room_inventory rename column availablerooms to "availableRooms";
  end if;
  if exists (select 1 from information_schema.columns where table_schema = 'public' and table_name = 'room_inventory' and column_name = 'priceoverride') then
    alter table public.room_inventory rename column priceoverride to "priceOverride";
  end if;
  if exists (select 1 from information_schema.columns where table_schema = 'public' and table_name = 'room_inventory' and column_name = 'isclosed') then
    alter table public.room_inventory rename column isclosed to "isClosed";
  end if;
  if exists (select 1 from information_schema.columns where table_schema = 'public' and table_name = 'room_inventory' and column_name = 'createdat') then
    alter table public.room_inventory rename column createdat to "createdAt";
  end if;
  if exists (select 1 from information_schema.columns where table_schema = 'public' and table_name = 'room_inventory' and column_name = 'updatedat') then
    alter table public.room_inventory rename column updatedat to "updatedAt";
  end if;
end $$;
alter table public.room_inventory enable row level security;
drop policy if exists "room_inventory_demo_access" on public.room_inventory;
create policy "room_inventory_demo_access"
on public.room_inventory for all
to anon, authenticated
using (true)
with check (true);
grant select, insert, update, delete on table public.room_inventory to anon, authenticated;
alter table public.bookings add column if not exists "roomTypeId" uuid references public.room_types(_id);
alter table public.bookings add column if not exists "numRooms" integer not null default 1;
alter table public.blocked_dates add column if not exists "roomTypeId" uuid references public.room_types(_id) on delete cascade;
alter table public.blocked_dates add column if not exists "numRooms" integer not null default 1;
alter table public.blocked_dates drop constraint if exists blocked_dates_valid_rooms;
alter table public.blocked_dates add constraint blocked_dates_valid_rooms check ("numRooms" > 0);

create table if not exists public.destinations (
  _id uuid primary key default gen_random_uuid(),
  name varchar not null unique,
  city varchar not null,
  country varchar not null default 'Vietnam',
  "imageUrl" text,
  "stayCount" integer not null default 0,
  latitude numeric,
  longitude numeric,
  "createdAt" timestamp without time zone default now(),
  "updatedAt" timestamp without time zone default now()
);
alter table public.destinations add column if not exists "imageUrl" text;
alter table public.destinations add column if not exists "stayCount" integer not null default 0;
alter table public.destinations add column if not exists latitude numeric;
alter table public.destinations add column if not exists longitude numeric;

create table if not exists public.destination_places (
  _id uuid primary key default gen_random_uuid(),
  "destinationId" uuid references public.destinations(_id) on delete cascade,
  city varchar not null,
  name varchar not null,
  address text,
  image text,
  latitude numeric not null,
  longitude numeric not null,
  "createdAt" timestamp without time zone default now(),
  "updatedAt" timestamp without time zone default now(),
  unique (city, name)
);

drop table if exists public.seed_extra_places;

truncate table
  public.messages,
  public.conversations,
  public.notifications,
  public.rates,
  public.payments,
  public.bookings,
  public.room_inventory,
  public.blocked_dates,
  public.promotions,
  public.wishlists,
  public.otps,
  public.cabin_amenities,
  public.images,
  public.booking_policies,
  public.coupons,
  public.settings,
  public.destination_places,
  public.destinations,
  public.room_types,
  public.cabins,
  public.amenities,
  public.users
restart identity cascade;

insert into public.users (
  "_id", "fullName", email, password, phone, "nationalId", "dateOfBirth",
  gender, address, nationality, role, "createdAt", "updatedAt"
) values
  ('10000000-0000-4000-8000-000000000001', 'Huy Gia Lai', 'huy.manager@sereinstay.test', '', '+84900000001', 'MGR000001', date '2005-08-20', 'male', 'Ho Chi Minh City, Vietnam', 'Vietnamese', 'manager', timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00'),
  ('10000000-0000-4000-8000-000000000002', 'Cuong Manager', 'cuong.manager@sereinstay.test', '', '+84900000002', 'MGR000002', date '2005-07-20', 'male', 'Ho Chi Minh City, Vietnam', 'Vietnamese', 'manager', timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00'),
  ('10000000-0000-4000-8000-000000000003', 'Tran Tuan Kha', 'kha.manager@sereinstay.test', '', '+84900000003', 'MGR000003', date '2005-02-03', 'male', 'Vung Tau, Vietnam', 'Vietnamese', 'manager', timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00'),
  ('10000000-0000-4000-8000-000000000101', 'Alice Nguyen', 'alice.nguyen@sereinstay.test', '', '+84900000101', 'CUS000101', date '1995-03-20', 'female', 'Thu Duc, Ho Chi Minh City', 'Vietnamese', 'customer', timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00'),
  ('10000000-0000-4000-8000-000000000102', 'Bao Tran', 'bao.tran@sereinstay.test', '', '+84900000102', 'CUS000102', date '1992-08-12', 'male', 'Hai Chau, Da Nang', 'Vietnamese', 'customer', timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00'),
  ('10000000-0000-4000-8000-000000000103', 'Chi Pham', 'chi.pham@sereinstay.test', '', '+84900000103', 'CUS000103', date '1988-11-05', 'female', 'Hoan Kiem, Hanoi', 'Vietnamese', 'customer', timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00'),
  ('10000000-0000-4000-8000-000000000104', 'David Le', 'david.le@sereinstay.test', '', '+84900000104', 'CUS000104', date '1998-06-22', 'male', 'Nha Trang, Khanh Hoa', 'Vietnamese', 'customer', timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00'),
  ('10000000-0000-4000-8000-000000000105', 'Eve Hoang', 'eve.hoang@sereinstay.test', '', '+84900000105', 'CUS000105', date '1996-12-09', 'female', 'Hue City, Vietnam', 'Vietnamese', 'customer', timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00');

insert into public.amenities ("_id", name, icon, category, "createdAt", "updatedAt") values
  ('40000000-0000-4000-8000-000000000001', 'WiFi', 'wifi', 'Comfort', timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00'),
  ('40000000-0000-4000-8000-000000000002', 'Breakfast', 'coffee', 'Food', timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00'),
  ('40000000-0000-4000-8000-000000000003', 'Parking', 'parking', 'Convenience', timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00'),
  ('40000000-0000-4000-8000-000000000004', 'Pool', 'pool', 'Relax', timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00'),
  ('40000000-0000-4000-8000-000000000005', 'Air conditioning', 'air-conditioner', 'Comfort', timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00'),
  ('40000000-0000-4000-8000-000000000006', 'Private bathroom', 'bath', 'Comfort', timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00'),
  ('40000000-0000-4000-8000-000000000007', 'Sea view', 'wave', 'View', timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00'),
  ('40000000-0000-4000-8000-000000000008', 'Balcony', 'balcony', 'View', timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00');

insert into public.settings (
  "_id", "miniBookingLength", "maxBookingLength", "maxNumberOfGuests", "breakfastPrice", "createdAt", "updatedAt"
) values
  ('90000000-0000-4000-8000-000000000001', 1, 21, 8, 12, timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00');

insert into public.destinations (
  "_id", name, city, country, "imageUrl", "stayCount", latitude, longitude, "createdAt", "updatedAt"
) values
  ('91000000-0000-4000-8000-000000000001', 'Ho Chi Minh City', 'Ho Chi Minh City', 'Vietnam', 'https://images.unsplash.com/photo-1583417319070-4a69db38a482?auto=format&fit=crop&w=1200&q=80', 15, 10.7769, 106.7009, timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00'),
  ('91000000-0000-4000-8000-000000000002', 'Vung Tau', 'Vung Tau', 'Vietnam', 'https://images.unsplash.com/photo-1500375592092-40eb2168fd21?auto=format&fit=crop&w=1200&q=80', 15, 10.4114, 107.1362, timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00'),
  ('91000000-0000-4000-8000-000000000003', 'Hanoi', 'Hanoi', 'Vietnam', 'https://images.unsplash.com/photo-1528127269322-539801943592?auto=format&fit=crop&w=1200&q=80', 15, 21.0278, 105.8342, timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00'),
  ('91000000-0000-4000-8000-000000000004', 'Da Nang', 'Da Nang', 'Vietnam', 'https://images.unsplash.com/photo-1559592413-7cec4d0cae2b?auto=format&fit=crop&w=1200&q=80', 15, 16.0471, 108.2068, timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00'),
  ('91000000-0000-4000-8000-000000000005', 'Da Lat', 'Da Lat', 'Vietnam', 'https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=1200&q=80', 15, 11.9404, 108.4583, timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00');

insert into public.cabins (
  "_id", name, "maxCapacity", "regularPrice", discount, image, description, location,
  latitude, longitude, "mapPlaceId", amenities, "hostId", address, district, "propertyType",
  "starRating", "reviewScore", "reviewCount", "googleMapsUrl", "createdAt", "updatedAt"
) values
  ('20000000-0000-4000-8000-000000000001', 'Hotel Majestic Saigon', 4, 118, 8, 'https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=1200&q=80', 'A heritage riverside hotel on Dong Khoi with classic rooms, balcony options, and quick access to Nguyen Hue Walking Street.', 'District 1, Ho Chi Minh City, Vietnam', 10.7756, 106.7039, null, 'WiFi, Breakfast, Parking, Air conditioning, Private bathroom, Balcony', '10000000-0000-4000-8000-000000000001', '01 Dong Khoi Street, Saigon Ward, Ho Chi Minh City, Vietnam', 'District 1', 'Hotel', 5, 8.9, 936, 'https://www.google.com/maps/search/?api=1&query=10.7756,106.7039', timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00'),
  ('20000000-0000-4000-8000-000000000002', 'The Imperial Vung Tau Hotel', 5, 126, 12, 'https://images.unsplash.com/photo-1571003123894-1f0594d2b5d9?auto=format&fit=crop&w=1200&q=80', 'A beachfront Vung Tau stay near Back Beach with larger rooms, pool access, and sea-view choices.', 'Back Beach, Vung Tau, Vietnam', 10.3353, 107.0931, null, 'WiFi, Breakfast, Parking, Pool, Sea view, Balcony', '10000000-0000-4000-8000-000000000003', '159 Thuy Van Street, Vung Tau, Vietnam', 'Vung Tau', 'Hotel', 5, 8.8, 742, 'https://www.google.com/maps/search/?api=1&query=10.3353,107.0931', timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00'),
  ('20000000-0000-4000-8000-000000000003', 'Sofitel Legend Metropole Hanoi', 4, 148, 5, 'https://images.unsplash.com/photo-1598928506311-c55ded91a20c?auto=format&fit=crop&w=1200&q=80', 'A landmark Hoan Kiem hotel near the Opera House with heritage rooms, quiet suites, and walkable Old Quarter access.', 'Hoan Kiem, Hanoi, Vietnam', 21.0256, 105.8561, null, 'WiFi, Breakfast, Air conditioning, Private bathroom, Balcony', '10000000-0000-4000-8000-000000000002', '15 Ngo Quyen Street, Hoan Kiem Ward, Hanoi, Vietnam', 'Hoan Kiem', 'Hotel', 5, 9.2, 4691, 'https://www.google.com/maps/search/?api=1&query=21.0256,105.8561', timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00'),
  ('20000000-0000-4000-8000-000000000004', 'Furama Resort Danang', 5, 142, 10, 'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=1200&q=80', 'A beach resort on Vo Nguyen Giap Street with spacious rooms, family suites, pools, and ocean-facing options.', 'My Khe Beach, Da Nang, Vietnam', 16.0394, 108.2492, null, 'WiFi, Breakfast, Parking, Pool, Sea view, Air conditioning', '10000000-0000-4000-8000-000000000002', '103 - 105 Vo Nguyen Giap Street, Khue My Ward, Da Nang, Vietnam', 'My Khe', 'Resort', 5, 9.0, 1294, 'https://www.google.com/maps/search/?api=1&query=16.0394,108.2492', timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00'),
  ('20000000-0000-4000-8000-000000000005', 'Dalat Palace Heritage Hotel', 4, 112, 6, 'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1200&q=80', 'A heritage Da Lat hotel by Xuan Huong Lake with classic rooms, garden views, and larger suite options.', 'Ward 3, Da Lat, Vietnam', 11.9391, 108.4444, null, 'WiFi, Breakfast, Parking, Air conditioning, Private bathroom, Balcony', '10000000-0000-4000-8000-000000000003', '02 Tran Phu, Ward 3, Da Lat, Lam Dong, Vietnam', 'Ward 3', 'Hotel', 5, 8.7, 531, 'https://www.google.com/maps/search/?api=1&query=11.9391,108.4444', timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00');

-- real-world hotel names: 15 hotels per city including the curated rows above
with city_templates as (
  select *
  from (values
    ('Ho Chi Minh City', 'District 1', 10.7756::numeric, 106.7039::numeric, 'WiFi, Breakfast, Parking, Air conditioning, Private bathroom, Balcony',
      array['Caravelle Saigon','Rex Hotel Saigon','The Reverie Saigon','Liberty Central Saigon Riverside Hotel','Fusion Original Saigon Centre','Park Hyatt Saigon','New World Saigon Hotel','Pullman Saigon Centre','Hotel Nikko Saigon','La Vela Saigon Hotel','Mai House Saigon Hotel','Bay Hotel Ho Chi Minh','Silverland Ben Thanh Hotel','Norfolk Mansion']::text[],
      array['19-23 Lam Son Square, Saigon Ward, Ho Chi Minh City, Vietnam','141 Nguyen Hue Boulevard, Saigon Ward, Ho Chi Minh City, Vietnam','22-36 Nguyen Hue Boulevard, Saigon Ward, Ho Chi Minh City, Vietnam','17 Ton Duc Thang Street, Saigon Ward, Ho Chi Minh City, Vietnam','65 Le Loi Boulevard, Saigon Ward, Ho Chi Minh City, Vietnam','2 Lam Son Square, Saigon Ward, Ho Chi Minh City, Vietnam','76 Le Lai Street, Ben Thanh Ward, Ho Chi Minh City, Vietnam','148 Tran Hung Dao Boulevard, Ho Chi Minh City, Vietnam','235 Nguyen Van Cu Street, District 1, Ho Chi Minh City, Vietnam','280 Nam Ky Khoi Nghia Street, District 3, Ho Chi Minh City, Vietnam','157 Nam Ky Khoi Nghia Street, District 3, Ho Chi Minh City, Vietnam','7 Ngo Van Nam Street, District 1, Ho Chi Minh City, Vietnam','14-16 Le Lai Street, District 1, Ho Chi Minh City, Vietnam','17-19-21 Ly Tu Trong Street, District 1, Ho Chi Minh City, Vietnam']::text[]),
    ('Vung Tau', 'Back Beach', 10.3353::numeric, 107.0931::numeric, 'WiFi, Breakfast, Parking, Pool, Sea view, Balcony',
      array['Pullman Vung Tau','Malibu Hotel','ibis Styles Vung Tau','Mercure Vung Tau','Fusion Suites Vung Tau','Marina Bay Vung Tau Resort & Spa','Vias Hotel Vung Tau','Premier Pearl Hotel Vung Tau','The Cap Hotel','Palace Hotel Vung Tau','Muong Thanh Vung Tau Hotel','Seaside Resort Vung Tau','Petro House Hotel','Grand Hotel Vung Tau']::text[],
      array['15 Thi Sach Street, Vung Tau, Vietnam','263 Le Hong Phong Street, Vung Tau, Vietnam','117 Thuy Van Street, Vung Tau, Vietnam','03 Ha Long Street, Vung Tau, Vietnam','02 Truong Cong Dinh Street, Vung Tau, Vietnam','115 Tran Phu Street, Vung Tau, Vietnam','179 Thuy Van Street, Vung Tau, Vietnam','69-69A Thuy Van Street, Vung Tau, Vietnam','01 Thi Sach Street, Vung Tau, Vietnam','01 Nguyen Trai Street, Vung Tau, Vietnam','09 Thong Nhat Street, Vung Tau, Vietnam','28 Tran Phu Street, Vung Tau, Vietnam','63 Tran Hung Dao Street, Vung Tau, Vietnam','02 Nguyen Du Street, Vung Tau, Vietnam']::text[]),
    ('Hanoi', 'Hoan Kiem', 21.0287::numeric, 105.8523::numeric, 'WiFi, Breakfast, Air conditioning, Private bathroom, Balcony',
      array['Apricot Hotel','Peridot Grand Luxury Boutique Hotel','La Siesta Premium Hang Be','Hotel de l''Opera Hanoi','Melia Hanoi','InterContinental Hanoi Westlake','Lotte Hotel Hanoi','Pan Pacific Hanoi','The Oriental Jade Hotel','Silk Path Boutique Hanoi','Hanoi La Siesta Hotel & Spa','O''Gallery Premier Hotel & Spa','Grand Hotel du Lac Hanoi','JM Marvel Hotel & Spa']::text[],
      array['136 Hang Trong Street, Hoan Kiem, Hanoi, Vietnam','33 Duong Thanh Street, Hoan Kiem, Hanoi, Vietnam','27 Hang Be Street, Hoan Kiem, Hanoi, Vietnam','29 Trang Tien Street, Hoan Kiem, Hanoi, Vietnam','44B Ly Thuong Kiet Street, Hoan Kiem, Hanoi, Vietnam','05 Tu Hoa Street, Tay Ho, Hanoi, Vietnam','54 Lieu Giai Street, Ba Dinh, Hanoi, Vietnam','01 Thanh Nien Road, Ba Dinh, Hanoi, Vietnam','92-94 Hang Trong Street, Hoan Kiem, Hanoi, Vietnam','21 Hang Khay Street, Hoan Kiem, Hanoi, Vietnam','94 Ma May Street, Hoan Kiem, Hanoi, Vietnam','122 Hang Bong Street, Hoan Kiem, Hanoi, Vietnam','18-20 Nha Chung Street, Hoan Kiem, Hanoi, Vietnam','16 Hang Da Street, Hoan Kiem, Hanoi, Vietnam']::text[]),
    ('Da Nang', 'My Khe', 16.0544::numeric, 108.2475::numeric, 'WiFi, Breakfast, Parking, Pool, Sea view, Air conditioning',
      array['Pullman Danang Beach Resort','TMS Hotel Da Nang Beach','Sala Danang Beach Hotel','Novotel Danang Premier Han River','Hilton Da Nang','Melia Vinpearl Danang Riverfront','Naman Retreat','InterContinental Danang Sun Peninsula Resort','Hyatt Regency Danang Resort and Spa','Fusion Suites Da Nang','A La Carte Da Nang Beach','Muong Thanh Luxury Da Nang Hotel','Grand Mercure Danang','Wink Hotel Danang Centre']::text[],
      array['101 Vo Nguyen Giap Street, Da Nang, Vietnam','292 Vo Nguyen Giap Street, Da Nang, Vietnam','36-38 Lam Hoanh Street, Da Nang, Vietnam','36 Bach Dang Street, Da Nang, Vietnam','50 Bach Dang Street, Da Nang, Vietnam','341 Tran Hung Dao Street, Da Nang, Vietnam','Truong Sa Road, Da Nang, Vietnam','Bai Bac, Son Tra Peninsula, Da Nang, Vietnam','05 Truong Sa Street, Da Nang, Vietnam','An Cu 5 Residential, Da Nang, Vietnam','200 Vo Nguyen Giap Street, Da Nang, Vietnam','270 Vo Nguyen Giap Street, Da Nang, Vietnam','Lot A1 Zone of Villas of Green Island, Da Nang, Vietnam','178 Tran Phu Street, Da Nang, Vietnam']::text[]),
    ('Da Lat', 'Ward 3', 11.9365::numeric, 108.4370::numeric, 'WiFi, Breakfast, Parking, Air conditioning, Private bathroom, Balcony',
      array['Ana Mandara Villas Dalat Resort & Spa','Mercure Dalat Resort','Hotel Colline','Swiss-Belresort Tuyen Lam Dalat','Terracotta Hotel & Resort Dalat','Dalat Edensee Lake Resort & Spa','Golf Valley Hotel','Ladalat Hotel','TTC Hotel Premium Ngoc Lan','Kings Hotel Dalat','Sammy Dalat Hotel','Du Parc Hotel Dalat','La Sapinette Hotel Dalat','Zen Valley Dalat']::text[],
      array['Le Lai Street, Ward 5, Da Lat, Lam Dong, Vietnam','03 Nguyen Du Street, Da Lat, Lam Dong, Vietnam','10 Phan Boi Chau Street, Da Lat, Lam Dong, Vietnam','Zone 7 and 8, Tuyen Lam Lake, Da Lat, Vietnam','Tuyen Lam Lake Tourist Area, Da Lat, Vietnam','Tuyen Lam Lake Zone VII.2, Da Lat, Vietnam','94 Bui Thi Xuan Street, Da Lat, Vietnam','106A Mai Anh Dao Street, Da Lat, Vietnam','42 Nguyen Chi Thanh Street, Da Lat, Vietnam','10 Bui Thi Xuan Street, Da Lat, Vietnam','01 Le Hong Phong Street, Da Lat, Vietnam','15 Tran Phu Street, Da Lat, Vietnam','01 Phan Chu Trinh Street, Da Lat, Vietnam','38 Khe Sanh Street, Da Lat, Vietnam']::text[])
  ) as t(city, district, base_lat, base_lng, amenities, hotel_names, hotel_addresses)
),
generated_hotels as (
  select
    gen_random_uuid() as id,
    city,
    district,
    amenities,
    hotel_names[n - 1] as hotel_name,
    hotel_addresses[n - 1] as address_text,
    n,
    base_lat + ((n % 7) - 3) * 0.0035 as latitude,
    base_lng + ((n % 5) - 2) * 0.0038 as longitude,
    case n % 5
      when 0 then 'https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=1200&q=80'
      when 1 then 'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=1200&q=80'
      when 2 then 'https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?auto=format&fit=crop&w=1200&q=80'
      when 3 then 'https://images.unsplash.com/photo-1564501049412-61c2a3083791?auto=format&fit=crop&w=1200&q=80'
      else 'https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?auto=format&fit=crop&w=1200&q=80'
    end as image
  from city_templates
  cross join generate_series(2, 15) as n
)
insert into public.cabins (
  "_id", name, "maxCapacity", "regularPrice", discount, image, description, location,
  latitude, longitude, "mapPlaceId", amenities, "hostId", address, district, "propertyType",
  "starRating", "reviewScore", "reviewCount", "googleMapsUrl", "createdAt", "updatedAt"
)
select
  id,
  hotel_name,
  case when n % 4 = 0 then 5 when n % 3 = 0 then 4 else 3 end,
  74 + (n * 6) + case when city in ('Vung Tau', 'Da Nang') then 18 when city = 'Hanoi' then 12 else 0 end,
  case when n % 5 = 0 then 12 when n % 3 = 0 then 8 else 0 end,
  image,
  'A real-world hotel reference in ' || city || ' with practical room inventory for booking, map, and manager testing.',
  address_text,
  latitude,
  longitude,
  null,
  amenities,
  case
    when city = 'Ho Chi Minh City' then '10000000-0000-4000-8000-000000000001'::uuid
    when city in ('Hanoi', 'Da Nang') then '10000000-0000-4000-8000-000000000002'::uuid
    else '10000000-0000-4000-8000-000000000003'::uuid
  end,
  address_text,
  district,
  case when city in ('Vung Tau', 'Da Nang') and n % 2 = 0 then 'Resort' else 'Hotel' end,
  case when n % 5 = 0 then 5 when n % 2 = 0 then 4 else 3 end,
  round((8.1 + (n % 8) * 0.1)::numeric, 1),
  24 + n * 3,
  'https://www.google.com/maps/search/?api=1&query=' || latitude || ',' || longitude,
  timestamp '2026-07-20 08:00:00',
  timestamp '2026-07-20 08:00:00'
from generated_hotels;

insert into public.destination_places (
  "_id", "destinationId", city, name, address, image, latitude, longitude, "createdAt", "updatedAt"
)
select gen_random_uuid(),
       case
         when c.location ilike '%Ho Chi Minh%' then '91000000-0000-4000-8000-000000000001'::uuid
         when c.location ilike '%Vung Tau%' then '91000000-0000-4000-8000-000000000002'::uuid
         when c.location ilike '%Hanoi%' then '91000000-0000-4000-8000-000000000003'::uuid
         when c.location ilike '%Da Nang%' then '91000000-0000-4000-8000-000000000004'::uuid
         else '91000000-0000-4000-8000-000000000005'::uuid
       end,
       case
         when c.location ilike '%Ho Chi Minh%' then 'Ho Chi Minh City'
         when c.location ilike '%Vung Tau%' then 'Vung Tau'
         when c.location ilike '%Hanoi%' then 'Hanoi'
         when c.location ilike '%Da Nang%' then 'Da Nang'
         else 'Da Lat'
       end,
       c.name, c.location, c.image, c.latitude, c.longitude,
       timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00'
from public.cabins c;

insert into public.room_types (
  "_id", "cabinId", name, category, description, "maxGuests", "maxAdults", "totalRooms", "basePrice",
  beds, "bedType", "bedCount", "sleepingCapacity", "bedSummary", "bedConfig",
  "bedWidthM", "bedLengthM", size, "sizeM2", "hasLivingRoom",
  amenities, image, "isActive", "createdAt", "updatedAt"
) values
  ('30000000-0000-4000-8000-000000000001', '20000000-0000-4000-8000-000000000001', 'Saigon Standard Queen', 'Standard', '20-25 m2 room for short city stays.', 2, 2, 12, 84, '1 Queen bed', 'Queen', 1, 2, '1 Queen bed', '[{"type":"Queen","quantity":1,"adultCapacity":2}]'::jsonb, 1.6, 2.0, '24 m2', 24, false, 'WiFi, Air conditioning, Private bathroom', 'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1200&q=80', true, timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00'),
  ('30000000-0000-4000-8000-000000000002', '20000000-0000-4000-8000-000000000001', 'Saigon Superior Double', 'Superior', '25-30 m2 room with extra desk space.', 2, 2, 8, 96, '1 Double bed', 'Double', 1, 2, '1 Double bed', '[{"type":"Double","quantity":1,"adultCapacity":2}]'::jsonb, 1.5, 2.0, '29 m2', 29, false, 'WiFi, Breakfast, Air conditioning, Private bathroom', 'https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?auto=format&fit=crop&w=1200&q=80', true, timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00'),
  ('30000000-0000-4000-8000-000000000003', '20000000-0000-4000-8000-000000000001', 'Saigon Deluxe Balcony', 'Deluxe', '30-45 m2 room with balcony.', 3, 3, 6, 118, '1 King bed and 1 sofa bed', 'King', 2, 3, '1 King bed and 1 sofa bed', '[{"type":"King","quantity":1,"adultCapacity":2},{"type":"Sofa Single","quantity":1,"adultCapacity":1}]'::jsonb, 1.8, 2.0, '38 m2', 38, false, 'WiFi, Breakfast, Air conditioning, Private bathroom, Balcony', 'https://images.unsplash.com/photo-1564013799919-ab600027ffc6?auto=format&fit=crop&w=1200&q=80', true, timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00'),
  ('30000000-0000-4000-8000-000000000004', '20000000-0000-4000-8000-000000000001', 'Saigon Suite Living Room', 'Suite', '50 m2+ suite with separate living room.', 4, 4, 3, 168, '1 King bed and 1 sofa double', 'King', 2, 4, '1 King bed and 1 sofa double', '[{"type":"King","quantity":1,"adultCapacity":2},{"type":"Sofa Double","quantity":1,"adultCapacity":2}]'::jsonb, 1.8, 2.0, '55 m2', 55, true, 'WiFi, Breakfast, Air conditioning, Private bathroom, Balcony', 'https://images.unsplash.com/photo-1570129477492-45c003edd2be?auto=format&fit=crop&w=1200&q=80', true, timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00'),

  ('30000000-0000-4000-8000-000000000005', '20000000-0000-4000-8000-000000000002', 'Vung Tau Standard Twin', 'Standard', 'Beachside 23 m2 room for two guests.', 2, 2, 10, 92, '2 Single beds', 'Single', 2, 2, '2 Single beds', '[{"type":"Single","quantity":2,"adultCapacity":1}]'::jsonb, 1.0, 2.0, '23 m2', 23, false, 'WiFi, Air conditioning, Private bathroom', 'https://images.unsplash.com/photo-1512918728675-ed5a9ecdebfd?auto=format&fit=crop&w=1200&q=80', true, timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00'),
  ('30000000-0000-4000-8000-000000000006', '20000000-0000-4000-8000-000000000002', 'Vung Tau Deluxe Sea View', 'Deluxe', '39 m2 sea-view room with balcony.', 3, 3, 7, 134, '1 Queen bed and 1 sofa bed', 'Queen', 2, 3, '1 Queen bed and 1 sofa bed', '[{"type":"Queen","quantity":1,"adultCapacity":2},{"type":"Sofa Single","quantity":1,"adultCapacity":1}]'::jsonb, 1.6, 2.0, '39 m2', 39, false, 'WiFi, Breakfast, Pool, Sea view, Balcony', 'https://images.unsplash.com/photo-1560185127-6ed189bf02f4?auto=format&fit=crop&w=1200&q=80', true, timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00'),
  ('30000000-0000-4000-8000-000000000007', '20000000-0000-4000-8000-000000000002', 'Vung Tau Family Suite', 'Suite', '62 m2 suite with living room for families.', 5, 5, 4, 188, '1 King bed, 1 Double bed and 1 sofa bed', 'King', 3, 5, '1 King bed, 1 Double bed and 1 sofa bed', '[{"type":"King","quantity":1,"adultCapacity":2},{"type":"Double","quantity":1,"adultCapacity":2},{"type":"Sofa Single","quantity":1,"adultCapacity":1}]'::jsonb, 1.8, 2.0, '62 m2', 62, true, 'WiFi, Breakfast, Pool, Sea view, Balcony', 'https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=1200&q=80', true, timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00'),

  ('30000000-0000-4000-8000-000000000008', '20000000-0000-4000-8000-000000000003', 'Hanoi Standard Double', 'Standard', '22 m2 Old Quarter room.', 2, 2, 11, 78, '1 Double bed', 'Double', 1, 2, '1 Double bed', '[{"type":"Double","quantity":1,"adultCapacity":2}]'::jsonb, 1.5, 2.0, '22 m2', 22, false, 'WiFi, Air conditioning, Private bathroom', 'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=1200&q=80', true, timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00'),
  ('30000000-0000-4000-8000-000000000009', '20000000-0000-4000-8000-000000000003', 'Hanoi Superior Queen', 'Superior', '28 m2 room with city window.', 2, 2, 8, 92, '1 Queen bed', 'Queen', 1, 2, '1 Queen bed', '[{"type":"Queen","quantity":1,"adultCapacity":2}]'::jsonb, 1.6, 2.0, '28 m2', 28, false, 'WiFi, Breakfast, Air conditioning, Private bathroom', 'https://images.unsplash.com/photo-1600566753190-17f0baa2a6c3?auto=format&fit=crop&w=1200&q=80', true, timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00'),
  ('30000000-0000-4000-8000-000000000010', '20000000-0000-4000-8000-000000000003', 'Hanoi Deluxe Family', 'Deluxe', '42 m2 family room near Hoan Kiem.', 4, 4, 5, 126, '2 Queen beds', 'Queen', 2, 4, '2 Queen beds', '[{"type":"Queen","quantity":2,"adultCapacity":2}]'::jsonb, 1.6, 2.0, '42 m2', 42, false, 'WiFi, Breakfast, Air conditioning, Private bathroom, Balcony', 'https://images.unsplash.com/photo-1494526585095-c41746248156?auto=format&fit=crop&w=1200&q=80', true, timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00'),

  ('30000000-0000-4000-8000-000000000011', '20000000-0000-4000-8000-000000000004', 'Da Nang Superior Ocean', 'Superior', '30 m2 room near My Khe Beach.', 2, 2, 9, 102, '1 Queen bed', 'Queen', 1, 2, '1 Queen bed', '[{"type":"Queen","quantity":1,"adultCapacity":2}]'::jsonb, 1.6, 2.0, '30 m2', 30, false, 'WiFi, Breakfast, Pool, Sea view', 'https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?auto=format&fit=crop&w=1200&q=80', true, timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00'),
  ('30000000-0000-4000-8000-000000000012', '20000000-0000-4000-8000-000000000004', 'Da Nang Deluxe King', 'Deluxe', '43 m2 deluxe room with king bed.', 3, 3, 7, 138, '1 King bed and 1 sofa bed', 'King', 2, 3, '1 King bed and 1 sofa bed', '[{"type":"King","quantity":1,"adultCapacity":2},{"type":"Sofa Single","quantity":1,"adultCapacity":1}]'::jsonb, 1.8, 2.0, '43 m2', 43, false, 'WiFi, Breakfast, Pool, Sea view, Balcony', 'https://images.unsplash.com/photo-1570129477492-45c003edd2be?auto=format&fit=crop&w=1200&q=80', true, timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00'),
  ('30000000-0000-4000-8000-000000000013', '20000000-0000-4000-8000-000000000004', 'Da Nang Ocean Suite', 'Suite', '70 m2 ocean suite with living room.', 5, 5, 3, 210, '1 King bed, 1 Double bed and 1 sofa bed', 'King', 3, 5, '1 King bed, 1 Double bed and 1 sofa bed', '[{"type":"King","quantity":1,"adultCapacity":2},{"type":"Double","quantity":1,"adultCapacity":2},{"type":"Sofa Single","quantity":1,"adultCapacity":1}]'::jsonb, 1.8, 2.0, '70 m2', 70, true, 'WiFi, Breakfast, Pool, Sea view, Balcony', 'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=1200&q=80', true, timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00'),

  ('30000000-0000-4000-8000-000000000014', '20000000-0000-4000-8000-000000000005', 'Da Lat Standard Garden', 'Standard', '21 m2 garden-facing room.', 2, 2, 10, 72, '1 Double bed', 'Double', 1, 2, '1 Double bed', '[{"type":"Double","quantity":1,"adultCapacity":2}]'::jsonb, 1.5, 2.0, '21 m2', 21, false, 'WiFi, Air conditioning, Private bathroom', 'https://images.unsplash.com/photo-1564013799919-ab600027ffc6?auto=format&fit=crop&w=1200&q=80', true, timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00'),
  ('30000000-0000-4000-8000-000000000015', '20000000-0000-4000-8000-000000000005', 'Da Lat Superior Queen', 'Superior', '27 m2 room with balcony.', 2, 2, 6, 86, '1 Queen bed', 'Queen', 1, 2, '1 Queen bed', '[{"type":"Queen","quantity":1,"adultCapacity":2}]'::jsonb, 1.6, 2.0, '27 m2', 27, false, 'WiFi, Breakfast, Private bathroom, Balcony', 'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=1200&q=80', true, timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00'),
  ('30000000-0000-4000-8000-000000000016', '20000000-0000-4000-8000-000000000005', 'Da Lat Deluxe Family', 'Deluxe', '36 m2 room for small families.', 4, 4, 4, 112, '2 Queen beds', 'Queen', 2, 4, '2 Queen beds', '[{"type":"Queen","quantity":2,"adultCapacity":2}]'::jsonb, 1.6, 2.0, '36 m2', 36, false, 'WiFi, Breakfast, Private bathroom, Balcony', 'https://images.unsplash.com/photo-1512918728675-ed5a9ecdebfd?auto=format&fit=crop&w=1200&q=80', true, timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00');

-- generated room types for every generated hotel
insert into public.room_types (
  "_id", "cabinId", name, category, description, "maxGuests", "maxAdults", "totalRooms", "basePrice",
  beds, "bedType", "bedCount", "sleepingCapacity", "bedSummary", "bedConfig",
  "bedWidthM", "bedLengthM", size, "sizeM2", "hasLivingRoom",
  amenities, image, "isActive", "createdAt", "updatedAt"
)
select
  gen_random_uuid(),
  c."_id",
  c.name || ' ' || room.category,
  room.category,
  room.description || ' at ' || c.name || '.',
  room.max_guests,
  room.max_adults,
  greatest(2, room.total_rooms + (length(c.name) % 4)),
  round((c."regularPrice" * room.price_multiplier)::numeric, 2),
  room.beds,
  room.bed_type,
  room.bed_count,
  room.sleeping_capacity,
  room.bed_summary,
  room.bed_config::jsonb,
  room.bed_width,
  2.0,
  room.size_m2 || ' m2',
  room.size_m2,
  room.has_living_room,
  room.amenities,
  c.image,
  true,
  timestamp '2026-07-20 08:00:00',
  timestamp '2026-07-20 08:00:00'
from public.cabins c
cross join lateral (
  select *
  from (
    values
      (1, 'Standard', '20-24 m2 room for quick stays', 2, 2, 5, 0.88, '1 Double bed', 'Double', 1, 2, '1 Double bed', '[{"type":"Double","quantity":1,"adultCapacity":2}]', 1.5, 24, false, 'WiFi, Air conditioning, Private bathroom'),
      (2, 'Superior', '26-30 m2 queen room with extra comfort', 2, 2, 4, 1.02, '1 Queen bed', 'Queen', 1, 2, '1 Queen bed', '[{"type":"Queen","quantity":1,"adultCapacity":2}]', 1.6, 28, false, 'WiFi, Breakfast, Air conditioning, Private bathroom'),
      (3, 'Deluxe', '32-40 m2 room for couples or small groups', 3, 3, 3, 1.18, '1 King bed and 1 sofa bed', 'King', 2, 3, '1 King bed and 1 sofa bed', '[{"type":"King","quantity":1,"adultCapacity":2},{"type":"Sofa Single","quantity":1,"adultCapacity":1}]', 1.8, 36, false, 'WiFi, Breakfast, Air conditioning, Private bathroom, Balcony'),
      (4, 'Suite', '44-55 m2 suite with separate living room', 4, 4, 2, 1.42, '1 King bed and 1 sofa double', 'King', 2, 4, '1 King bed and 1 sofa double', '[{"type":"King","quantity":1,"adultCapacity":2},{"type":"Sofa Double","quantity":1,"adultCapacity":2}]', 1.8, 50, true, 'WiFi, Breakfast, Air conditioning, Private bathroom, Balcony')
  ) as t(room_rank, category, description, max_guests, max_adults, total_rooms, price_multiplier, beds, bed_type, bed_count, sleeping_capacity, bed_summary, bed_config, bed_width, size_m2, has_living_room, amenities)
  where t.room_rank <= case when mod(length(c.name), 2) = 0 then 2 else 3 end
) as room(room_rank, category, description, max_guests, max_adults, total_rooms, price_multiplier, beds, bed_type, bed_count, sleeping_capacity, bed_summary, bed_config, bed_width, size_m2, has_living_room, amenities)
where room.max_guests <= c."maxCapacity";

-- Safety net: every hotel must have room types even if an earlier seed section was edited.
insert into public.room_types (
  "_id", "cabinId", name, category, description, "maxGuests", "maxAdults", "totalRooms", "basePrice",
  beds, "bedType", "bedCount", "sleepingCapacity", "bedSummary", "bedConfig",
  "bedWidthM", "bedLengthM", size, "sizeM2", "hasLivingRoom",
  amenities, image, "isActive", "createdAt", "updatedAt"
)
select
  gen_random_uuid(),
  c."_id",
  c.name || ' Demo ' || room.category,
  room.category,
  room.description || ' at ' || c.name || '.',
  room.max_guests,
  room.max_adults,
  room.total_rooms,
  round((c."regularPrice" * room.price_multiplier)::numeric, 2),
  room.beds,
  room.bed_type,
  room.bed_count,
  room.sleeping_capacity,
  room.bed_summary,
  room.bed_config::jsonb,
  room.bed_width,
  2.0,
  room.size_m2 || ' m2',
  room.size_m2,
  room.has_living_room,
  room.amenities,
  c.image,
  true,
  timestamp '2026-07-20 08:00:00',
  timestamp '2026-07-20 08:00:00'
from public.cabins c
cross join lateral (
  values
    ('Standard', '20-24 m2 room for short stays', 2, 2, 4, 0.88, '1 Double bed', 'Double', 1, 2, '1 Double bed', '[{"type":"Double","quantity":1,"adultCapacity":2}]', 1.5, 24, false, 'WiFi, Air conditioning, Private bathroom'),
    ('Superior', '26-30 m2 queen room with extra comfort', 2, 2, 3, 1.02, '1 Queen bed', 'Queen', 1, 2, '1 Queen bed', '[{"type":"Queen","quantity":1,"adultCapacity":2}]', 1.6, 28, false, 'WiFi, Breakfast, Air conditioning, Private bathroom'),
    ('Deluxe', '32-40 m2 room with more comfort', 3, 3, 2, 1.18, '1 King bed and 1 sofa bed', 'King', 2, 3, '1 King bed and 1 sofa bed', '[{"type":"King","quantity":1,"adultCapacity":2},{"type":"Sofa Single","quantity":1,"adultCapacity":1}]', 1.8, 36, false, 'WiFi, Breakfast, Air conditioning, Private bathroom, Balcony')
) as room(category, description, max_guests, max_adults, total_rooms, price_multiplier, beds, bed_type, bed_count, sleeping_capacity, bed_summary, bed_config, bed_width, size_m2, has_living_room, amenities)
where not exists (
  select 1
  from public.room_types rt
  where rt."cabinId" = c."_id"
);

insert into public.room_inventory (
  "_id", "roomTypeId", date, "availableRooms", "priceOverride", "isClosed", "createdAt", "updatedAt"
)
select gen_random_uuid(), rt."_id", day::date,
       greatest(0, rt."totalRooms" - case when extract(dow from day) in (5, 6) then 1 else 0 end),
       case when extract(dow from day) in (5, 6) then round((rt."basePrice" * 1.12)::numeric, 2) else null end,
       false, timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00'
from public.room_types rt
cross join generate_series(date '2026-07-20', date '2026-10-31', interval '1 day') as dates(day);

update public.room_inventory
set "availableRooms" = 0,
    "updatedAt" = timestamp '2026-07-20 08:00:00'
where "roomTypeId" = '30000000-0000-4000-8000-000000000001'
  and date = date '2026-07-25';

insert into public.booking_policies (
  "_id", "cabinId", "breakfastPrice", "miniBookingLength", "maxBookingLength", "createdAt", "updatedAt"
)
select gen_random_uuid(), c."_id", 12, 1, 21, timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00'
from public.cabins c;

insert into public.cabin_amenities ("_id", "cabinId", "amenityId", "createdAt")
select gen_random_uuid(), c."_id", a."_id", timestamp '2026-07-20 08:00:00'
from public.cabins c
join public.amenities a on c.amenities ilike '%' || a.name || '%';

insert into public.images ("_id", "cabinId", "imageUrl", name, "isCover", "createdAt")
select gen_random_uuid(), c."_id", c.image, 'Cover', true, timestamp '2026-07-20 08:00:00'
from public.cabins c;

insert into public.images ("_id", "cabinId", "imageUrl", name, "isCover", "createdAt")
select gen_random_uuid(), c."_id", gallery."imageUrl", gallery.name, false, timestamp '2026-07-20 08:00:00'
from public.cabins c
cross join lateral (
  values
    ('Lobby', case
      when c.location ilike '%Vung Tau%' or c.location ilike '%Da Nang%' then 'https://images.unsplash.com/photo-1578683010236-d716f9a3f461?auto=format&fit=crop&w=1200&q=80'
      when c.location ilike '%Da Lat%' then 'https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?auto=format&fit=crop&w=1200&q=80'
      else 'https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?auto=format&fit=crop&w=1200&q=80'
    end),
    ('Room detail', case
      when c.location ilike '%Vung Tau%' or c.location ilike '%Da Nang%' then 'https://images.unsplash.com/photo-1590490360182-c33d57733427?auto=format&fit=crop&w=1200&q=80'
      else 'https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?auto=format&fit=crop&w=1200&q=80'
    end),
    ('Bathroom and amenities', 'https://images.unsplash.com/photo-1584622650111-993a426fbf0a?auto=format&fit=crop&w=1200&q=80'),
    ('Restaurant', 'https://images.unsplash.com/photo-1555396273-367ea4eb4db5?auto=format&fit=crop&w=1200&q=80')
) as gallery(name, "imageUrl");

insert into public.coupons (
  "_id", code, description, "discountType", "discountValue", "maxDiscountAmount",
  "minBookingAmount", "startDate", "endDate", "usageLimit", "usedCount", "isActive", "createdAt", "updatedAt"
) values
  ('70000000-0000-4000-8000-000000000001', 'WELCOME10', 'Ten percent off first bookings', 'percent', 10, 50, 120, date '2026-07-01', date '2026-12-31', 100, 8, true, timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00'),
  ('70000000-0000-4000-8000-000000000002', 'ROOM25', 'Flat room discount for bigger stays', 'fixed', 25, 25, 200, date '2026-07-01', date '2026-11-30', 60, 3, true, timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00');

insert into public.promotions (
  "_id", "cabinId", "discountPercent", "startDate", "endDate", "isActive", "createdAt", "updatedAt"
) values
  ('71000000-0000-4000-8000-000000000001', '20000000-0000-4000-8000-000000000002', 12, date '2026-07-20', date '2026-09-30', true, timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00'),
  ('71000000-0000-4000-8000-000000000002', '20000000-0000-4000-8000-000000000004', 10, date '2026-07-20', date '2026-08-31', true, timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00');

insert into public.blocked_dates (
  "_id", "cabinId", "roomTypeId", "hostId", "numRooms", "startDate", "endDate", reason, "createdAt", "updatedAt"
) values
  ('88000000-0000-4000-8000-000000000001', '20000000-0000-4000-8000-000000000002', '30000000-0000-4000-8000-000000000006', '10000000-0000-4000-8000-000000000003', 2, date '2026-08-12', date '2026-08-15', 'External OTA booking hold', timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00'),
  ('88000000-0000-4000-8000-000000000002', '20000000-0000-4000-8000-000000000004', '30000000-0000-4000-8000-000000000013', '10000000-0000-4000-8000-000000000002', 1, date '2026-08-20', date '2026-08-22', 'Suite maintenance', timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00'),
  ('88000000-0000-4000-8000-000000000003', '20000000-0000-4000-8000-000000000001', '30000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', 11, date '2026-07-25', date '2026-07-26', 'Demo booking: Standard Queen nearly sold out for this night', timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00');

insert into public.bookings (
  "_id", "userId", "cabinId", "roomTypeId", "numRooms", "startDate", "endDate", "numNights",
  "numGuests", "cabinPrice", "extrasPrice", "totalPrice", status, "hasBreakfast", "isPaid",
  observations, "couponId", "discountAmount", "createdAt", "updatedAt"
) values
  ('80000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000101', '20000000-0000-4000-8000-000000000001', '30000000-0000-4000-8000-000000000003', 1, date '2026-08-04', date '2026-08-07', 3, 2, 354.00, 72.00, 426.00, 'pending', true, false, 'Anniversary city trip', null, 0.00, timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00'),
  ('80000000-0000-4000-8000-000000000002', '10000000-0000-4000-8000-000000000102', '20000000-0000-4000-8000-000000000002', '30000000-0000-4000-8000-000000000007', 1, date '2026-08-10', date '2026-08-13', 3, 5, 564.00, 0.00, 539.00, 'confirmed', false, false, 'Family beach break', '70000000-0000-4000-8000-000000000002', 25.00, timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00'),
  ('80000000-0000-4000-8000-000000000003', '10000000-0000-4000-8000-000000000103', '20000000-0000-4000-8000-000000000003', '30000000-0000-4000-8000-000000000009', 1, date '2026-07-24', date '2026-07-26', 2, 2, 184.00, 48.00, 232.00, 'checked-in', true, true, 'Weekend in the Old Quarter', null, 0.00, timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00'),
  ('80000000-0000-4000-8000-000000000004', '10000000-0000-4000-8000-000000000104', '20000000-0000-4000-8000-000000000004', '30000000-0000-4000-8000-000000000012', 1, date '2026-07-05', date '2026-07-08', 3, 3, 414.00, 0.00, 414.00, 'checked-out', false, true, 'Ocean room was clean', null, 0.00, timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00'),
  ('80000000-0000-4000-8000-000000000005', '10000000-0000-4000-8000-000000000105', '20000000-0000-4000-8000-000000000001', '30000000-0000-4000-8000-000000000001', 1, date '2026-07-25', date '2026-07-26', 1, 2, 84.00, 0.00, 84.00, 'confirmed', false, true, 'Demo booking for Standard Queen sold-out test', null, 0.00, timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00'),
  ('80000000-0000-4000-8000-000000000006', '10000000-0000-4000-8000-000000000102', '20000000-0000-4000-8000-000000000002', '30000000-0000-4000-8000-000000000006', 1, date '2026-08-18', date '2026-08-20', 2, 3, 268.00, 0.00, 268.00, 'cancelled', false, false, 'Demo decline: manager rejected pending in-app payment', null, 0.00, timestamp '2026-07-20 08:30:00', timestamp '2026-07-20 08:45:00');

with manager_booking_source as (
  select
    c."hostId" as host_id,
    rt."_id" as room_type_id,
    rt."cabinId" as cabin_id,
    rt."basePrice" as base_price,
    rt."maxGuests" as max_guests,
    row_number() over (partition by c."hostId" order by c.name, rt.category, rt.name) as rn
  from public.room_types rt
  join public.cabins c on c."_id" = rt."cabinId"
  where c."hostId" in (
    '10000000-0000-4000-8000-000000000001',
    '10000000-0000-4000-8000-000000000002',
    '10000000-0000-4000-8000-000000000003'
  )
    and rt."isActive" = true
),
generated_manager_bookings as (
  select
    gen_random_uuid() as booking_id,
    case (rn % 5)
      when 0 then '10000000-0000-4000-8000-000000000101'::uuid
      when 1 then '10000000-0000-4000-8000-000000000102'::uuid
      when 2 then '10000000-0000-4000-8000-000000000103'::uuid
      when 3 then '10000000-0000-4000-8000-000000000104'::uuid
      else '10000000-0000-4000-8000-000000000105'::uuid
    end as user_id,
    cabin_id,
    room_type_id,
    rn,
    (date '2026-08-01' + (rn::integer % 24)) as start_date,
    case when rn % 4 = 0 then 2 else 1 end as nights,
    greatest(1, least(max_guests, 1 + (rn::integer % greatest(1, max_guests)))) as guests,
    round((base_price * (case when rn % 4 = 0 then 2 else 1 end))::numeric, 2) as cabin_price,
    case when rn % 3 = 0 then 24.00 else 0.00 end as extras_price,
    case when rn % 5 = 0 then 'pending'
         when rn % 7 = 0 then 'checked-out'
         else 'confirmed'
    end as booking_status,
    case when rn % 3 = 0 then true else false end as has_breakfast,
    case when rn % 5 = 0 then false else true end as is_paid
  from manager_booking_source
  where rn <= case when host_id = '10000000-0000-4000-8000-000000000001' then 32 else 20 end
),
inserted_manager_bookings as (
  insert into public.bookings (
    "_id", "userId", "cabinId", "roomTypeId", "numRooms", "startDate", "endDate", "numNights",
    "numGuests", "cabinPrice", "extrasPrice", "totalPrice", status, "hasBreakfast", "isPaid",
    observations, "couponId", "discountAmount", "createdAt", "updatedAt"
  )
  select
    booking_id,
    user_id,
    cabin_id,
    room_type_id,
    1,
    start_date,
    start_date + nights,
    nights,
    guests,
    cabin_price,
    extras_price,
    cabin_price + extras_price,
    booking_status,
    has_breakfast,
    is_paid,
    'Generated manager dashboard transaction seed',
    null,
    0.00,
    timestamp '2026-07-20 09:00:00' + (rn || ' hours')::interval,
    timestamp '2026-07-20 09:00:00'
  from generated_manager_bookings
  returning "_id", "userId", "totalPrice", "isPaid", "createdAt"
)
select count(*) from inserted_manager_bookings;

insert into public.payments (
  "_id", "bookingId", "userId", amount, method, provider, "transactionId", status, "paidAt", "createdAt", "updatedAt"
)
select
  gen_random_uuid(),
  b."_id",
  b."userId",
  b."totalPrice",
  case when b."isPaid" then 'card' else 'app' end,
  case when b."isPaid" then 'stripe' else 'app' end,
  case when b."isPaid" then 'HCM-MGR-' || lpad(row_number() over (order by b."createdAt", b."_id")::text, 4, '0') else null end,
  case when b."isPaid" then 'paid' else 'pending' end,
  case when b."isPaid" then b."createdAt" + interval '1 hour' else null end,
  b."createdAt",
  b."createdAt"
from public.bookings b;

update public.payments
set status = 'failed',
    method = 'app',
    provider = 'app',
    "transactionId" = 'MGR-DECLINE-SEED-0001',
    "paidAt" = null,
    "updatedAt" = timestamp '2026-07-20 08:45:00'
where "bookingId" = '80000000-0000-4000-8000-000000000006';

insert into public.rates (
  "_id", "userId", "cabinId", "bookingId", rating, comment, "createdAt", "updatedAt"
) values
  ('82000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000104', '20000000-0000-4000-8000-000000000004', '80000000-0000-4000-8000-000000000004', 5, 'Clean deluxe room and easy beach access.', timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00'),
  ('82000000-0000-4000-8000-000000000002', '10000000-0000-4000-8000-000000000103', '20000000-0000-4000-8000-000000000003', '80000000-0000-4000-8000-000000000003', 4, 'Good location and quick check-in.', timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00'),
  ('82000000-0000-4000-8000-000000000003', '10000000-0000-4000-8000-000000000101', '20000000-0000-4000-8000-000000000001', '80000000-0000-4000-8000-000000000001', 5, 'Early stay was smooth, room looked clean, and check-in was quick.', timestamp '2026-07-18 12:00:00', timestamp '2026-07-20 08:00:00'),
  ('82000000-0000-4000-8000-000000000004', '10000000-0000-4000-8000-000000000102', '20000000-0000-4000-8000-000000000002', '80000000-0000-4000-8000-000000000002', 5, 'Nice sea-view room with enough space for the family.', timestamp '2026-07-19 09:00:00', timestamp '2026-07-20 08:00:00'),
  ('82000000-0000-4000-8000-000000000005', '10000000-0000-4000-8000-000000000105', '20000000-0000-4000-8000-000000000005', '80000000-0000-4000-8000-000000000005', 4, 'Quiet room and easy to manage on the admin side.', timestamp '2026-07-19 18:00:00', timestamp '2026-07-20 08:00:00');

with review_targets as (
  select
    c."_id" as cabin_id,
    row_number() over (order by c.name) as hotel_index,
    10 + ((row_number() over (order by c.name))::integer % 6) as target_count,
    coalesce(existing.count_existing, 0) as existing_count
  from public.cabins c
  left join (
    select "cabinId", count(*)::integer as count_existing
    from public.rates
    group by "cabinId"
  ) existing on existing."cabinId" = c."_id"
)
insert into public.rates (
  "_id", "userId", "cabinId", "bookingId", rating, comment, "createdAt", "updatedAt"
)
select
  gen_random_uuid(),
  case ((rt.hotel_index + g.n) % 5)
    when 0 then '10000000-0000-4000-8000-000000000101'::uuid
    when 1 then '10000000-0000-4000-8000-000000000102'::uuid
    when 2 then '10000000-0000-4000-8000-000000000103'::uuid
    when 3 then '10000000-0000-4000-8000-000000000104'::uuid
    else '10000000-0000-4000-8000-000000000105'::uuid
  end,
  rt.cabin_id,
  null,
  case
    when (rt.hotel_index + g.n) % 11 = 0 then 3
    when (rt.hotel_index + g.n) % 4 = 0 then 4
    else 5
  end,
  case (rt.hotel_index + g.n) % 6
    when 0 then 'Room was clean, easy to find, and close to the main area.'
    when 1 then 'Good room layout, comfortable bed, and helpful hotel team.'
    when 2 then 'Amenities matched the listing and check-in was smooth.'
    when 3 then 'Nice stay for the price with reliable WiFi and quiet room.'
    when 4 then 'Convenient location and the room type was exactly as described.'
    else 'Pleasant short stay, good service, and useful room facilities.'
  end,
  timestamp '2026-07-20 08:00:00' + (g.n || ' hours')::interval,
  timestamp '2026-07-20 08:00:00'
from review_targets rt
cross join lateral generate_series(1, greatest(0, rt.target_count - rt.existing_count)) as g(n);

update public.cabins c
set "reviewCount" = stats.review_count,
    "reviewScore" = stats.review_score,
    "updatedAt" = timestamp '2026-07-20 08:00:00'
from (
  select
    "cabinId",
    count(*)::integer as review_count,
    round((avg(rating) * 2)::numeric, 1) as review_score
  from public.rates
  group by "cabinId"
) stats
where stats."cabinId" = c."_id";

insert into public.wishlists ("_id", "userId", "cabinId", "createdAt") values
  ('83000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000101', '20000000-0000-4000-8000-000000000002', timestamp '2026-07-20 08:00:00'),
  ('83000000-0000-4000-8000-000000000002', '10000000-0000-4000-8000-000000000102', '20000000-0000-4000-8000-000000000004', timestamp '2026-07-20 08:00:00');

insert into public.conversations (
  "_id", "guestId", "hostId", "cabinId", "bookingId", "createdAt", "updatedAt"
) values
  ('84000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000101', '10000000-0000-4000-8000-000000000001', '20000000-0000-4000-8000-000000000001', '80000000-0000-4000-8000-000000000001', timestamp '2026-07-20 08:00:00', timestamp '2026-07-20 08:00:00');

insert into public.messages (
  "_id", "conversationId", "senderId", message, "isRead", "createdAt"
) values
  ('85000000-0000-4000-8000-000000000001', '84000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000101', 'Hi, is the Deluxe Balcony room available for August 4?', true, timestamp '2026-07-20 08:00:00'),
  ('85000000-0000-4000-8000-000000000002', '84000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', 'Yes, the room is available and breakfast can be added.', false, timestamp '2026-07-20 08:05:00');

insert into public.conversations (
  "_id", "guestId", "hostId", "cabinId", "bookingId", "createdAt", "updatedAt"
) values
  ('84000000-0000-4000-8000-000000000002', '10000000-0000-4000-8000-000000000102', '10000000-0000-4000-8000-000000000003', '20000000-0000-4000-8000-000000000002', null, timestamp '2026-07-20 08:12:00', timestamp '2026-07-20 08:12:00'),
  ('84000000-0000-4000-8000-000000000003', '10000000-0000-4000-8000-000000000103', '10000000-0000-4000-8000-000000000002', '20000000-0000-4000-8000-000000000004', null, timestamp '2026-07-20 08:15:00', timestamp '2026-07-20 08:15:00'),
  ('84000000-0000-4000-8000-000000000004', '10000000-0000-4000-8000-000000000105', '10000000-0000-4000-8000-000000000001', '20000000-0000-4000-8000-000000000001', null, timestamp '2026-07-20 08:18:00', timestamp '2026-07-20 08:18:00');

insert into public.messages (
  "_id", "conversationId", "senderId", message, "isRead", "createdAt"
) values
  ('85000000-0000-4000-8000-000000000003', '84000000-0000-4000-8000-000000000002', '10000000-0000-4000-8000-000000000102', 'Mình chưa book, muốn hỏi phòng sea view cho 2 người thì còn không?', true, timestamp '2026-07-20 08:13:00'),
  ('85000000-0000-4000-8000-000000000004', '84000000-0000-4000-8000-000000000002', '10000000-0000-4000-8000-000000000003', 'Còn nhé, phòng Deluxe Sea View đang trống và có thể giữ giá ưu đãi.', false, timestamp '2026-07-20 08:16:00'),
  ('85000000-0000-4000-8000-000000000005', '84000000-0000-4000-8000-000000000003', '10000000-0000-4000-8000-000000000103', 'Em đang hỏi trước, phòng Family Suite ở Đà Nẵng có phù hợp 4 người lớn không?', true, timestamp '2026-07-20 08:16:00'),
  ('85000000-0000-4000-8000-000000000006', '84000000-0000-4000-8000-000000000003', '10000000-0000-4000-8000-000000000002', 'Phù hợp ạ. Suite đó cho tối đa 5 khách và có khu sinh hoạt riêng.', false, timestamp '2026-07-20 08:20:00'),
  ('85000000-0000-4000-8000-000000000007', '84000000-0000-4000-8000-000000000004', '10000000-0000-4000-8000-000000000105', 'Chưa đặt phòng mà nhắn trước thì bên mình có hỗ trợ tư vấn thêm bữa sáng không?', true, timestamp '2026-07-20 08:19:00'),
  ('85000000-0000-4000-8000-000000000008', '84000000-0000-4000-8000-000000000004', '10000000-0000-4000-8000-000000000001', 'Có nhé, em có thể xem phòng và thêm breakfast sau khi chốt lịch.', false, timestamp '2026-07-20 08:22:00');

with generated_conversation_source as (
  select
    b."_id" as booking_id,
    b."userId" as guest_id,
    c."hostId" as host_id,
    c."_id" as cabin_id,
    row_number() over (partition by c."hostId" order by b."createdAt", c.name) as rn
  from public.bookings b
  join public.cabins c on c."_id" = b."cabinId"
  where b."_id" <> '80000000-0000-4000-8000-000000000001'
    and c."hostId" in (
      '10000000-0000-4000-8000-000000000001',
      '10000000-0000-4000-8000-000000000002',
      '10000000-0000-4000-8000-000000000003'
    )
),
inserted_conversations as (
  insert into public.conversations (
    "_id", "guestId", "hostId", "cabinId", "bookingId", "createdAt", "updatedAt"
  )
  select
    gen_random_uuid(),
    guest_id,
    host_id,
    cabin_id,
    booking_id,
    timestamp '2026-07-20 10:00:00' + (rn || ' minutes')::interval,
    timestamp '2026-07-20 10:00:00' + (rn || ' minutes')::interval
  from generated_conversation_source
  where rn <= 6
  returning "_id", "guestId", "hostId", "createdAt"
)
insert into public.messages (
  "_id", "conversationId", "senderId", message, "isRead", "createdAt"
)
select gen_random_uuid(), "_id", "guestId", 'Hello, I want to confirm room details and check-in time for my booking.', true, "createdAt" + interval '1 minute'
from inserted_conversations
union all
select gen_random_uuid(), "_id", "hostId", 'Thanks for contacting us. Your room type and check-in note are ready in the hotel system.', false, "createdAt" + interval '4 minutes'
from inserted_conversations;

insert into public.notifications (
  "_id", title, "userId", "isRead", data, "createdAt", type, message
) values
  ('86000000-0000-4000-8000-000000000001', 'Booking pending', '10000000-0000-4000-8000-000000000001', false, '{"bookingId":"80000000-0000-4000-8000-000000000001","status":"pending"}'::jsonb, timestamp '2026-07-20 08:00:00', 'booking', 'A guest booked Saigon Deluxe Balcony.'),
  ('86000000-0000-4000-8000-000000000002', 'Payment reminder', '10000000-0000-4000-8000-000000000102', false, '{"bookingId":"80000000-0000-4000-8000-000000000002","status":"confirmed"}'::jsonb, timestamp '2026-07-20 08:00:00', 'payment', 'Please finish payment for your Vung Tau suite.');

-- OTP rows are intentionally omitted from demo seed data.

do $$
declare
  hotel_count integer;
  room_type_count integer;
  missing_room_hotels integer;
begin
  select count(*) into hotel_count from public.cabins;
  select count(*) into room_type_count from public.room_types;
  select count(*) into missing_room_hotels
  from public.cabins c
  where not exists (
    select 1
    from public.room_types rt
    where rt."cabinId" = c."_id"
  );

  if hotel_count = 0 or room_type_count = 0 or missing_room_hotels > 0 then
    raise exception 'Seed validation failed: hotels=%, room_types=%, hotels_without_rooms=%',
      hotel_count, room_type_count, missing_room_hotels;
  end if;
end $$;

commit;
