create extension if not exists "pgcrypto";

create table if not exists users (
  _id uuid primary key default gen_random_uuid(),
  fullName varchar not null,
  email varchar not null unique,
  password text not null,
  phone varchar,
  nationalId varchar,
  dateOfBirth date,
  gender varchar,
  address text,
  nationality varchar,
  role varchar not null default 'customer',
  createdAt timestamp without time zone default now(),
  updatedAt timestamp without time zone default now(),
  constraint users_role_check check (role in ('customer', 'cabinOwner', 'admin'))
);

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

create table if not exists amenities (
  _id uuid primary key default gen_random_uuid(),
  name varchar not null unique,
  icon text,
  category varchar default 'General',
  createdAt timestamp without time zone default now(),
  updatedAt timestamp without time zone default now()
);

create table if not exists cabins (
  _id uuid primary key default gen_random_uuid(),
  name varchar not null,
  maxCapacity integer not null,
  regularPrice numeric not null,
  discount numeric default 0,
  image text not null,
  description text,
  location text,
  latitude numeric,
  longitude numeric,
  mapPlaceId text,
  amenities text,
  hostId uuid references users(_id),
  createdAt timestamp without time zone default now(),
  updatedAt timestamp without time zone default now()
);

create table if not exists cabin_amenities (
  _id uuid primary key default gen_random_uuid(),
  cabinId uuid not null references cabins(_id) on delete cascade,
  amenityId uuid not null references amenities(_id) on delete cascade,
  createdAt timestamp without time zone default now(),
  unique (cabinId, amenityId)
);

create table if not exists images (
  _id uuid primary key default gen_random_uuid(),
  cabinId uuid not null references cabins(_id) on delete cascade,
  imageUrl text not null,
  name varchar,
  isCover boolean default false,
  createdAt timestamp without time zone default now()
);

create table if not exists settings (
  _id uuid primary key default gen_random_uuid(),
  miniBookingLength integer not null default 1,
  maxBookingLength integer not null default 30,
  maxNumberOfGuests integer not null default 10,
  breakfastPrice numeric not null default 15,
  createdAt timestamp without time zone default now(),
  updatedAt timestamp without time zone default now()
);

create table if not exists booking_policies (
  _id uuid primary key default gen_random_uuid(),
  cabinId uuid not null references cabins(_id) on delete cascade,
  breakfastPrice numeric,
  miniBookingLength integer,
  maxBookingLength integer,
  createdAt timestamp without time zone default now(),
  updatedAt timestamp without time zone default now(),
  unique (cabinId)
);

create table if not exists coupons (
  _id uuid primary key default gen_random_uuid(),
  code varchar not null unique,
  description text,
  discountType varchar not null,
  discountValue numeric not null,
  maxDiscountAmount numeric,
  minBookingAmount numeric default 0,
  startDate date,
  endDate date,
  usageLimit integer,
  usedCount integer not null default 0,
  isActive boolean default true,
  createdAt timestamp without time zone default now(),
  updatedAt timestamp without time zone default now()
);

create table if not exists bookings (
  _id uuid primary key default gen_random_uuid(),
  userId uuid not null references users(_id),
  cabinId uuid not null references cabins(_id),
  startDate date not null,
  endDate date not null,
  numNights integer not null,
  numGuests integer not null,
  cabinPrice numeric not null,
  extrasPrice numeric default 0,
  totalPrice numeric not null,
  status varchar not null default 'pending',
  hasBreakfast boolean default false,
  isPaid boolean default false,
  observations text,
  couponId uuid references coupons(_id),
  discountAmount numeric default 0,
  createdAt timestamp without time zone default now(),
  updatedAt timestamp without time zone default now(),
  constraint bookings_valid_dates check (endDate > startDate),
  constraint bookings_valid_guests check (numGuests > 0),
  constraint bookings_status_check check (status in ('pending', 'confirmed', 'cancelled', 'checked-in', 'checked-out'))
);

create table if not exists blocked_dates (
  _id uuid primary key default gen_random_uuid(),
  cabinId uuid not null references cabins(_id) on delete cascade,
  hostId uuid references users(_id),
  startDate date not null,
  endDate date not null,
  reason text,
  createdAt timestamp without time zone default now(),
  updatedAt timestamp without time zone default now(),
  constraint blocked_dates_valid_dates check (endDate > startDate)
);

create table if not exists payments (
  _id uuid primary key default gen_random_uuid(),
  bookingId uuid not null references bookings(_id) on delete cascade,
  userId uuid not null references users(_id),
  amount numeric not null,
  method varchar,
  provider varchar,
  transactionId text,
  status varchar not null default 'pending',
  paidAt timestamp without time zone,
  createdAt timestamp without time zone default now(),
  updatedAt timestamp without time zone default now(),
  constraint payments_status_check check (status in ('pending', 'paid', 'failed', 'refunded'))
);

create table if not exists rates (
  _id uuid primary key default gen_random_uuid(),
  userId uuid not null references users(_id),
  cabinId uuid not null references cabins(_id) on delete cascade,
  bookingId uuid references bookings(_id) on delete set null,
  rating integer not null check (rating between 1 and 5),
  comment text,
  createdAt timestamp without time zone default now(),
  updatedAt timestamp without time zone default now(),
  unique (bookingId)
);

create table if not exists wishlists (
  _id uuid primary key default gen_random_uuid(),
  userId uuid not null references users(_id) on delete cascade,
  cabinId uuid not null references cabins(_id) on delete cascade,
  createdAt timestamp without time zone default now(),
  unique (userId, cabinId)
);

create table if not exists conversations (
  _id uuid primary key default gen_random_uuid(),
  guestId uuid not null references users(_id),
  hostId uuid not null references users(_id),
  cabinId uuid references cabins(_id) on delete set null,
  bookingId uuid references bookings(_id) on delete set null,
  createdAt timestamp without time zone default now(),
  updatedAt timestamp without time zone default now()
);

create table if not exists messages (
  _id uuid primary key default gen_random_uuid(),
  conversationId uuid not null references conversations(_id) on delete cascade,
  senderId uuid not null references users(_id),
  message text not null,
  isRead boolean default false,
  createdAt timestamp without time zone default now()
);

create table if not exists notifications (
  _id uuid primary key default gen_random_uuid(),
  userId uuid not null references users(_id) on delete cascade,
  title varchar not null,
  message text not null,
  type varchar,
  isRead boolean default false,
  data jsonb,
  createdAt timestamp without time zone default now()
);

create table if not exists otps (
  _id uuid primary key default gen_random_uuid(),
  email varchar not null,
  otp varchar not null,
  expiresAt timestamp without time zone not null,
  userId uuid references users(_id) on delete cascade,
  createdAt timestamp without time zone default now()
);

create table if not exists promotions (
  _id uuid primary key default gen_random_uuid(),
  cabinId uuid not null references cabins(_id) on delete cascade,
  discountPercent numeric not null,
  startDate date not null,
  endDate date not null,
  isActive boolean default true,
  createdAt timestamp without time zone default now(),
  updatedAt timestamp without time zone default now(),
  constraint promotions_valid_dates check (endDate >= startDate)
);

create index if not exists idx_cabins_location on cabins using gin (to_tsvector('simple', coalesce(location, '') || ' ' || coalesce(name, '') || ' ' || coalesce(amenities, '')));
create index if not exists idx_cabins_host on cabins(hostId);
create index if not exists idx_bookings_user_created on bookings(userId, createdAt desc);
create index if not exists idx_bookings_cabin_dates on bookings(cabinId, startDate, endDate);
create index if not exists idx_blocked_dates_cabin_dates on blocked_dates(cabinId, startDate, endDate);
create index if not exists idx_payments_booking on payments(bookingId);
create index if not exists idx_notifications_user_read on notifications(userId, isRead, createdAt desc);
create index if not exists idx_messages_conversation_created on messages(conversationId, createdAt asc);

alter table cabins add column if not exists latitude numeric;
alter table cabins add column if not exists longitude numeric;
alter table cabins add column if not exists mapPlaceId text;
alter table amenities add column if not exists category varchar default 'General';

insert into settings (miniBookingLength, maxBookingLength, maxNumberOfGuests, breakfastPrice)
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
