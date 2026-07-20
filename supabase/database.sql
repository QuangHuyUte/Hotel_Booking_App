create extension if not exists "pgcrypto";

create table if not exists users (
  _id uuid primary key default gen_random_uuid(),
  "fullName" varchar not null,
  email varchar not null unique,
  password text not null,
  phone varchar,
  "nationalId" varchar,
  "dateOfBirth" date,
  gender varchar,
  address text,
  nationality varchar,
  role varchar not null default 'customer',
  "createdAt" timestamp without time zone default now(),
  "updatedAt" timestamp without time zone default now(),
  constraint users_role_check check (role in ('customer', 'manager'))
);

alter table users enable row level security;
drop policy if exists "users_public_read" on users;
create policy "users_public_read"
on users for select
to anon, authenticated
using (true);

drop policy if exists "users_create_own_customer_profile" on users;
create policy "users_create_own_customer_profile"
on users for insert
to authenticated
with check (_id = auth.uid() and role = 'customer');

drop policy if exists "users_update_own_profile" on users;
create policy "users_update_own_profile"
on users for update
to authenticated
using (_id = auth.uid())
with check (_id = auth.uid());

create table if not exists amenities (
  _id uuid primary key default gen_random_uuid(),
  name varchar not null unique,
  icon text,
  category varchar default 'General',
  "createdAt" timestamp without time zone default now(),
  "updatedAt" timestamp without time zone default now()
);

create table if not exists cabins (
  _id uuid primary key default gen_random_uuid(),
  name varchar not null,
  "maxCapacity" integer not null,
  "regularPrice" numeric not null,
  discount numeric default 0,
  image text not null,
  description text,
  location text,
  latitude numeric,
  longitude numeric,
  "mapPlaceId" text,
  address text,
  district varchar,
  "propertyType" varchar default 'Hotel',
  "starRating" integer default 3 check ("starRating" between 0 and 5),
  "reviewScore" numeric default 8.6,
  "reviewCount" integer default 0,
  "googleMapsUrl" text,
  amenities text,
  "hostId" uuid references users(_id),
  "createdAt" timestamp without time zone default now(),
  "updatedAt" timestamp without time zone default now()
);

create table if not exists destinations (
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

create table if not exists destination_places (
  _id uuid primary key default gen_random_uuid(),
  "destinationId" uuid references destinations(_id) on delete cascade,
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

create table if not exists cabin_amenities (
  _id uuid primary key default gen_random_uuid(),
  "cabinId" uuid not null references cabins(_id) on delete cascade,
  "amenityId" uuid not null references amenities(_id) on delete cascade,
  "createdAt" timestamp without time zone default now(),
  unique ("cabinId", "amenityId")
);

create table if not exists images (
  _id uuid primary key default gen_random_uuid(),
  "cabinId" uuid not null references cabins(_id) on delete cascade,
  "imageUrl" text not null,
  name varchar,
  "isCover" boolean default false,
  "createdAt" timestamp without time zone default now()
);

create table if not exists settings (
  _id uuid primary key default gen_random_uuid(),
  "miniBookingLength" integer not null default 1,
  "maxBookingLength" integer not null default 30,
  "maxNumberOfGuests" integer not null default 10,
  "breakfastPrice" numeric not null default 15,
  "createdAt" timestamp without time zone default now(),
  "updatedAt" timestamp without time zone default now()
);

create table if not exists booking_policies (
  _id uuid primary key default gen_random_uuid(),
  "cabinId" uuid not null references cabins(_id) on delete cascade,
  "breakfastPrice" numeric,
  "miniBookingLength" integer,
  "maxBookingLength" integer,
  "createdAt" timestamp without time zone default now(),
  "updatedAt" timestamp without time zone default now(),
  unique ("cabinId")
);

create table if not exists room_types (
  _id uuid primary key default gen_random_uuid(),
  "cabinId" uuid not null references cabins(_id) on delete cascade,
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

create table if not exists room_inventory (
  _id uuid primary key default gen_random_uuid(),
  "roomTypeId" uuid not null references room_types(_id) on delete cascade,
  date date not null,
  "availableRooms" integer not null default 0,
  "priceOverride" numeric,
  "isClosed" boolean default false,
  "createdAt" timestamp without time zone default now(),
  "updatedAt" timestamp without time zone default now(),
  constraint room_inventory_available_nonnegative check ("availableRooms" >= 0),
  unique ("roomTypeId", date)
);

create table if not exists coupons (
  _id uuid primary key default gen_random_uuid(),
  code varchar not null unique,
  description text,
  "discountType" varchar not null,
  "discountValue" numeric not null,
  "maxDiscountAmount" numeric,
  "minBookingAmount" numeric default 0,
  "startDate" date,
  "endDate" date,
  "usageLimit" integer,
  "usedCount" integer not null default 0,
  "isActive" boolean default true,
  "createdAt" timestamp without time zone default now(),
  "updatedAt" timestamp without time zone default now()
);

create table if not exists bookings (
  _id uuid primary key default gen_random_uuid(),
  "userId" uuid not null references users(_id),
  "cabinId" uuid not null references cabins(_id),
  "roomTypeId" uuid references room_types(_id),
  "numRooms" integer not null default 1,
  "startDate" date not null,
  "endDate" date not null,
  "numNights" integer not null,
  "numGuests" integer not null,
  "cabinPrice" numeric not null,
  "extrasPrice" numeric default 0,
  "totalPrice" numeric not null,
  status varchar not null default 'pending',
  "hasBreakfast" boolean default false,
  "isPaid" boolean default false,
  observations text,
  "couponId" uuid references coupons(_id),
  "discountAmount" numeric default 0,
  "createdAt" timestamp without time zone default now(),
  "updatedAt" timestamp without time zone default now(),
  constraint bookings_valid_dates check ("endDate" > "startDate"),
  constraint bookings_valid_guests check ("numGuests" > 0),
  constraint bookings_valid_rooms check ("numRooms" > 0),
  constraint bookings_status_check check (status in ('pending', 'confirmed', 'cancelled', 'checked-in', 'checked-out'))
);

create table if not exists blocked_dates (
  _id uuid primary key default gen_random_uuid(),
  "cabinId" uuid not null references cabins(_id) on delete cascade,
  "roomTypeId" uuid references room_types(_id) on delete cascade,
  "hostId" uuid references users(_id),
  "startDate" date not null,
  "endDate" date not null,
  reason text,
  "createdAt" timestamp without time zone default now(),
  "updatedAt" timestamp without time zone default now(),
  constraint blocked_dates_valid_dates check ("endDate" > "startDate")
);

create table if not exists payments (
  _id uuid primary key default gen_random_uuid(),
  "bookingId" uuid not null references bookings(_id) on delete cascade,
  "userId" uuid not null references users(_id),
  amount numeric not null,
  method varchar,
  provider varchar,
  "transactionId" text,
  status varchar not null default 'pending',
  "paidAt" timestamp without time zone,
  "createdAt" timestamp without time zone default now(),
  "updatedAt" timestamp without time zone default now(),
  constraint payments_status_check check (status in ('pending', 'paid', 'failed', 'refunded'))
);

create table if not exists rates (
  _id uuid primary key default gen_random_uuid(),
  "userId" uuid not null references users(_id),
  "cabinId" uuid not null references cabins(_id) on delete cascade,
  "bookingId" uuid references bookings(_id) on delete set null,
  rating integer not null check (rating between 1 and 5),
  comment text,
  "createdAt" timestamp without time zone default now(),
  "updatedAt" timestamp without time zone default now(),
  unique ("bookingId")
);

create table if not exists wishlists (
  _id uuid primary key default gen_random_uuid(),
  "userId" uuid not null references users(_id) on delete cascade,
  "cabinId" uuid not null references cabins(_id) on delete cascade,
  "createdAt" timestamp without time zone default now(),
  unique ("userId", "cabinId")
);

create table if not exists conversations (
  _id uuid primary key default gen_random_uuid(),
  "guestId" uuid not null references users(_id),
  "hostId" uuid not null references users(_id),
  "cabinId" uuid references cabins(_id) on delete set null,
  "bookingId" uuid references bookings(_id) on delete set null,
  "createdAt" timestamp without time zone default now(),
  "updatedAt" timestamp without time zone default now()
);

create table if not exists messages (
  _id uuid primary key default gen_random_uuid(),
  "conversationId" uuid not null references conversations(_id) on delete cascade,
  "senderId" uuid not null references users(_id),
  message text not null,
  "isRead" boolean default false,
  "createdAt" timestamp without time zone default now()
);

create table if not exists notifications (
  _id uuid primary key default gen_random_uuid(),
  "userId" uuid not null references users(_id) on delete cascade,
  title varchar not null,
  message text not null,
  type varchar,
  "isRead" boolean default false,
  data jsonb,
  "createdAt" timestamp without time zone default now()
);

create table if not exists otps (
  _id uuid primary key default gen_random_uuid(),
  email varchar not null,
  otp varchar not null,
  "expiresAt" timestamp without time zone not null,
  "userId" uuid references users(_id) on delete cascade,
  "createdAt" timestamp without time zone default now()
);

create table if not exists promotions (
  _id uuid primary key default gen_random_uuid(),
  "cabinId" uuid not null references cabins(_id) on delete cascade,
  "discountPercent" numeric not null,
  "startDate" date not null,
  "endDate" date not null,
  "isActive" boolean default true,
  "createdAt" timestamp without time zone default now(),
  "updatedAt" timestamp without time zone default now(),
  constraint promotions_valid_dates check ("endDate" >= "startDate")
);

create index if not exists idx_cabins_location on cabins using gin (to_tsvector('simple', coalesce(location, '') || ' ' || coalesce(name, '') || ' ' || coalesce(amenities, '')));
create index if not exists idx_destinations_city on destinations(city);
create index if not exists idx_destination_places_city on destination_places(city);

create table if not exists seed_extra_places (
  city varchar,
  destination_id uuid,
  name varchar,
  address text,
  image text,
  latitude numeric,
  longitude numeric,
  regular_price numeric,
  max_capacity integer,
  discount numeric
);
create index if not exists idx_cabins_host on cabins("hostId");
create index if not exists idx_bookings_user_created on bookings("userId", "createdAt" desc);
create index if not exists idx_bookings_cabin_dates on bookings("cabinId", "startDate", "endDate");
create index if not exists idx_bookings_room_type_dates on bookings("roomTypeId", "startDate", "endDate");
create index if not exists idx_blocked_dates_cabin_dates on blocked_dates("cabinId", "startDate", "endDate");
create index if not exists idx_blocked_dates_room_type_dates on blocked_dates("roomTypeId", "startDate", "endDate");
create index if not exists idx_room_types_cabin on room_types("cabinId");
create index if not exists idx_room_inventory_room_date on room_inventory("roomTypeId", date);
create index if not exists idx_payments_booking on payments("bookingId");
create index if not exists idx_notifications_user_read on notifications("userId", "isRead", "createdAt" desc);
create index if not exists idx_messages_conversation_created on messages("conversationId", "createdAt" asc);

alter table cabins add column if not exists latitude numeric;
alter table cabins add column if not exists longitude numeric;
alter table cabins add column if not exists "mapPlaceId" text;
alter table cabins add column if not exists address text;
alter table cabins add column if not exists district varchar;
alter table cabins add column if not exists "propertyType" varchar default 'Hotel';
alter table cabins add column if not exists "starRating" integer default 3;
alter table cabins add column if not exists "reviewScore" numeric default 8.6;
alter table cabins add column if not exists "reviewCount" integer default 0;
alter table cabins add column if not exists "googleMapsUrl" text;
alter table amenities add column if not exists category varchar default 'General';
alter table destinations add column if not exists "imageUrl" text;
alter table destinations add column if not exists "stayCount" integer not null default 0;
alter table destinations add column if not exists latitude numeric;
alter table destinations add column if not exists longitude numeric;
alter table destination_places add column if not exists image text;
alter table destination_places add column if not exists latitude numeric;
alter table destination_places add column if not exists longitude numeric;
alter table bookings add column if not exists "roomTypeId" uuid references room_types(_id);
alter table bookings add column if not exists "numRooms" integer not null default 1;
alter table blocked_dates add column if not exists "roomTypeId" uuid references room_types(_id) on delete cascade;
alter table room_types add column if not exists category varchar not null default 'Standard';
alter table room_types add column if not exists "bedType" varchar default 'Queen';
alter table room_types add column if not exists "maxAdults" integer not null default 2;
alter table room_types add column if not exists "bedCount" integer not null default 1;
alter table room_types add column if not exists "sleepingCapacity" integer not null default 2;
alter table room_types add column if not exists "bedSummary" varchar;
alter table room_types add column if not exists "bedConfig" jsonb default '[]'::jsonb;
alter table room_types add column if not exists "bedWidthM" numeric default 1.6;
alter table room_types add column if not exists "bedLengthM" numeric default 2.0;
alter table room_types add column if not exists "sizeM2" integer default 24;
alter table room_types add column if not exists "hasLivingRoom" boolean default false;
update room_types
set "maxAdults" = greatest(coalesce(nullif("maxAdults", 0), 0), coalesce("maxGuests", 2)),
    "bedCount" = greatest(1, coalesce(nullif("bedCount", 0), 1)),
    "sleepingCapacity" = greatest(coalesce(nullif("sleepingCapacity", 0), 0), coalesce("maxGuests", 2), coalesce(nullif("maxAdults", 0), 2)),
    "bedSummary" = coalesce("bedSummary", beds);
alter table room_types drop constraint if exists room_types_valid_adults;
alter table room_types add constraint room_types_valid_adults check ("maxAdults" > 0);
alter table room_types drop constraint if exists room_types_valid_bed_count;
alter table room_types add constraint room_types_valid_bed_count check ("bedCount" > 0);
alter table room_types drop constraint if exists room_types_valid_sleeping_capacity;
alter table room_types add constraint room_types_valid_sleeping_capacity check ("sleepingCapacity" >= "maxAdults");
update users set role = 'manager' where role in ('cabinOwner', 'admin');
alter table users drop constraint if exists users_role_check;
alter table users add constraint users_role_check check (role in ('customer', 'manager'));

insert into settings ("miniBookingLength", "maxBookingLength", "maxNumberOfGuests", "breakfastPrice")
select 1, 30, 10, 15
where not exists (select 1 from settings);

insert into amenities (name, icon, category)
values
  ('WiFi', 'wifi', 'Comfort'),
  ('Breakfast', 'coffee', 'Food'),
  ('Parking', 'parking', 'Convenience'),
  ('Mountain view', 'map', 'Outdoors'),
  ('Private bathroom', 'bath', 'Comfort')
on conflict (name) do nothing;

-- Password reset/email:
-- Prefer Supabase Auth recovery emails. Keep app database for user profile only.
-- Google Maps:
-- For embedded maps, fill cabins.latitude/cabins.longitude and add Android Maps SDK/API key outside git.
