-- Supabase seed for Hotel Booking App
-- Safe to rerun: this script upgrades the demo schema columns, clears sample tables,
-- and repopulates them. For a brand-new database, run database/database.sql once first.

begin;

-- create new / schema upgrade for the Hotel Booking App search and map UI
create extension if not exists "pgcrypto";

alter table public.amenities add column if not exists category varchar default 'General';
alter table public.users drop constraint if exists users_role_check;
alter table public.users add constraint users_role_check check (role in ('customer', 'cabinOwner', 'admin'));
alter table public.cabins add column if not exists latitude numeric;
alter table public.cabins add column if not exists longitude numeric;
alter table public.cabins add column if not exists "mapPlaceId" text;
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

truncate table
  public.messages,
  public.conversations,
  public.notifications,
  public.rates,
  public.payments,
  public.bookings,
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
  public.cabins,
  public.amenities,
  public.users
restart identity cascade;

-- users
insert into public.users (
  "_id",
  "fullName",
  email,
  password,
  phone,
  "nationalId",
  "dateOfBirth",
  gender,
  address,
  nationality,
  role,
  "createdAt",
  "updatedAt"
) values
  ('10000000-0000-4000-8000-000000000001', 'Serein Support', 'support@sereinstay.test', 'Password123!', '+84900000001', 'SUP000001', date '1990-02-12', 'female', 'Ho Chi Minh City, Vietnam', 'Vietnamese', 'admin', timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('10000000-0000-4000-8000-000000000002', 'Olivia Tran', 'olivia.owner@sdp1.test', 'Password123!', '+84900000002', 'OWN000002', date '1987-04-12', 'female', 'Da Lat, Lam Dong', 'Vietnamese', 'cabinOwner', timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('10000000-0000-4000-8000-000000000003', 'Marcus Nguyen', 'marcus.owner@sdp1.test', 'Password123!', '+84900000003', 'OWN000003', date '1985-09-30', 'male', 'Sa Pa, Lao Cai', 'Vietnamese', 'cabinOwner', timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('10000000-0000-4000-8000-000000000004', 'Linh Pham', 'linh.owner@sdp1.test', 'Password123!', '+84900000004', 'OWN000004', date '1991-02-18', 'female', 'Hoi An, Quang Nam', 'Vietnamese', 'cabinOwner', timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('10000000-0000-4000-8000-000000000101', 'Alice Nguyen', 'alice.nguyen@sdp1.test', 'Password123!', '+84900000005', 'CUS000101', date '1995-03-20', 'female', 'Thu Duc, Ho Chi Minh City', 'Vietnamese', 'customer', timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('10000000-0000-4000-8000-000000000102', 'Bao Tran', 'bao.tran@sdp1.test', 'Password123!', '+84900000006', 'CUS000102', date '1992-08-12', 'male', 'Hai Chau, Da Nang', 'Vietnamese', 'customer', timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('10000000-0000-4000-8000-000000000103', 'Chi Pham', 'chi.pham@sdp1.test', 'Password123!', '+84900000007', 'CUS000103', date '1988-11-05', 'female', 'Hoan Kiem, Ha Noi', 'Vietnamese', 'customer', timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('10000000-0000-4000-8000-000000000104', 'David Le', 'david.le@sdp1.test', 'Password123!', '+84900000008', 'CUS000104', date '1998-06-22', 'male', 'Nha Trang, Khanh Hoa', 'Vietnamese', 'customer', timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('10000000-0000-4000-8000-000000000105', 'Eve Hoang', 'eve.hoang@sdp1.test', 'Password123!', '+84900000009', 'CUS000105', date '1996-12-09', 'female', 'Hue City, Thua Thien Hue', 'Vietnamese', 'customer', timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('10000000-0000-4000-8000-000000000106', 'Finn Vo', 'finn.vo@sdp1.test', 'Password123!', '+84900000010', 'CUS000106', date '1993-07-17', 'male', 'Can Tho City, Vietnam', 'Vietnamese', 'customer', timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00');

-- amenities
insert into public.amenities (
  "_id",
  name,
  icon,
  category,
  "createdAt",
  "updatedAt"
) values
  ('40000000-0000-4000-8000-000000000001', 'WiFi', 'wifi', 'Comfort', timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('40000000-0000-4000-8000-000000000002', 'Breakfast', 'coffee', 'Food', timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('40000000-0000-4000-8000-000000000003', 'Parking', 'parking', 'Convenience', timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('40000000-0000-4000-8000-000000000004', 'Kitchen', 'kitchen', 'Comfort', timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('40000000-0000-4000-8000-000000000005', 'Air conditioning', 'air-conditioner', 'Comfort', timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('40000000-0000-4000-8000-000000000006', 'Pool', 'pool', 'Relax', timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('40000000-0000-4000-8000-000000000007', 'BBQ', 'bbq', 'Outdoor', timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('40000000-0000-4000-8000-000000000008', 'Bathtub', 'bath', 'Relax', timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('40000000-0000-4000-8000-000000000009', 'Mountain view', 'mountain', 'View', timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('40000000-0000-4000-8000-000000000010', 'Balcony', 'balcony', 'View', timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00');

-- settings
insert into public.settings (
  "_id",
  "miniBookingLength",
  "maxBookingLength",
  "maxNumberOfGuests",
  "breakfastPrice",
  "createdAt",
  "updatedAt"
) values
  ('90000000-0000-4000-8000-000000000001', 1, 14, 8, 12, timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00');

-- destinations for the home Explore Vietnam carousel
insert into public.destinations (
  "_id",
  name,
  city,
  country,
  "imageUrl",
  "stayCount",
  latitude,
  longitude,
  "createdAt",
  "updatedAt"
) values
  ('91000000-0000-4000-8000-000000000001', 'TP. Ho Chi Minh', 'Ho Chi Minh City', 'Vietnam', 'https://images.unsplash.com/photo-1583417319070-4a69db38a482?auto=format&fit=crop&w=1200&q=80', 100, 10.7769, 106.7009, timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('91000000-0000-4000-8000-000000000002', 'Vung Tau', 'Vung Tau', 'Vietnam', 'https://images.unsplash.com/photo-1500375592092-40eb2168fd21?auto=format&fit=crop&w=1200&q=80', 50, 10.4114, 107.1362, timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('91000000-0000-4000-8000-000000000003', 'Ha Noi', 'Ha Noi', 'Vietnam', 'https://images.unsplash.com/photo-1528127269322-539801943592?auto=format&fit=crop&w=1200&q=80', 55, 21.0278, 105.8342, timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00');

-- cabins
insert into public.cabins (
  "_id",
  name,
  "maxCapacity",
  "regularPrice",
  discount,
  image,
  description,
  location,
  latitude,
  longitude,
  "mapPlaceId",
  amenities,
  "hostId",
  "createdAt",
  "updatedAt"
) values
  ('20000000-0000-4000-8000-000000000001', 'Ben Thanh Market Stay', 2, 120, 10, 'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1200&q=80', 'A central District 1 stay near Ben Thanh Market, street food, and the metro area.', 'Ben Thanh Market, District 1, Ho Chi Minh City', 10.7721, 106.6983, null, 'WiFi, Parking, Breakfast, Balcony', '10000000-0000-4000-8000-000000000002', timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('20000000-0000-4000-8000-000000000002', 'Nguyen Hue City Loft', 3, 150, 15, 'https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?auto=format&fit=crop&w=1200&q=80', 'A bright city loft near Nguyen Hue Walking Street and the Saigon River.', 'Nguyen Hue Walking Street, District 1, Ho Chi Minh City', 10.7756, 106.7039, null, 'WiFi, Breakfast, Parking, Bathtub', '10000000-0000-4000-8000-000000000002', timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('20000000-0000-4000-8000-000000000003', 'Notre-Dame Heritage Room', 2, 95, 0, 'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=1200&q=80', 'A calm room close to Notre-Dame Cathedral, Central Post Office, and book street.', 'Notre-Dame Cathedral Basilica of Saigon, District 1, Ho Chi Minh City', 10.7798, 106.6990, null, 'WiFi, Kitchen, Balcony, Air conditioning', '10000000-0000-4000-8000-000000000004', timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('20000000-0000-4000-8000-000000000004', 'Landmark 81 Skyline Suite', 4, 180, 20, 'https://images.unsplash.com/photo-1564013799919-ab600027ffc6?auto=format&fit=crop&w=1200&q=80', 'A bigger suite around Vinhomes Central Park with skyline views toward Landmark 81.', 'Landmark 81, Binh Thanh District, Ho Chi Minh City', 10.7940, 106.7218, null, 'WiFi, Breakfast, Parking, Balcony', '10000000-0000-4000-8000-000000000003', timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('20000000-0000-4000-8000-000000000005', 'Bui Vien Night Stay', 4, 160, 12, 'https://images.unsplash.com/photo-1494526585095-c41746248156?auto=format&fit=crop&w=1200&q=80', 'A lively stay near Bui Vien Walking Street, local restaurants, and backpacker nightlife.', 'Bui Vien Walking Street, District 1, Ho Chi Minh City', 10.7677, 106.6932, null, 'WiFi, Parking, Pool, Balcony', '10000000-0000-4000-8000-000000000004', timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('20000000-0000-4000-8000-000000000006', 'Thao Dien Garden Apartment', 3, 130, 8, 'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=1200&q=80', 'A softer apartment stay in Thao Dien with cafes, river views, and quiet streets nearby.', 'Thao Dien, Thu Duc City, Ho Chi Minh City', 10.8022, 106.7334, null, 'WiFi, Breakfast, Air conditioning, Kitchen', '10000000-0000-4000-8000-000000000002', timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('20000000-0000-4000-8000-000000000007', 'Tan Dinh Pink House', 2, 110, 5, 'https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=1200&q=80', 'A compact stay near Tan Dinh Church, local markets, and cafe streets.', 'Tan Dinh Church, District 3, Ho Chi Minh City', 10.7880, 106.6907, null, 'WiFi, Parking, Balcony, Bathtub', '10000000-0000-4000-8000-000000000003', timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('20000000-0000-4000-8000-000000000008', 'Saigon Zoo Family Stay', 5, 140, 0, 'https://images.unsplash.com/photo-1570129477492-45c003edd2be?auto=format&fit=crop&w=1200&q=80', 'A larger family stay near Saigon Zoo, the botanical gardens, and riverside routes.', 'Saigon Zoo and Botanical Gardens, District 1, Ho Chi Minh City', 10.7875, 106.7053, null, 'WiFi, Breakfast, Pool, Balcony', '10000000-0000-4000-8000-000000000004', timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('20000000-0000-4000-8000-000000000009', 'Front Beach Vung Tau Studio', 3, 115, 7, 'https://images.unsplash.com/photo-1512918728675-ed5a9ecdebfd?auto=format&fit=crop&w=1200&q=80', 'A compact coastal studio near Front Beach, seafood streets, and the ferry pier.', 'Front Beach, Vung Tau, Ba Ria - Vung Tau, Vietnam', 10.3459, 107.0764, null, 'WiFi, Parking, Air conditioning, Balcony', '10000000-0000-4000-8000-000000000002', timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('20000000-0000-4000-8000-000000000010', 'Back Beach Family Suite', 5, 150, 12, 'https://images.unsplash.com/photo-1560185127-6ed189bf02f4?auto=format&fit=crop&w=1200&q=80', 'A family-friendly Vung Tau stay near Back Beach with easy access to cafes and night markets.', 'Back Beach, Vung Tau, Ba Ria - Vung Tau, Vietnam', 10.3353, 107.0931, null, 'WiFi, Breakfast, Pool, Kitchen', '10000000-0000-4000-8000-000000000003', timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('20000000-0000-4000-8000-000000000011', 'Hoan Kiem Old Quarter Room', 2, 105, 5, 'https://images.unsplash.com/photo-1598928506311-c55ded91a20c?auto=format&fit=crop&w=1200&q=80', 'A walkable Ha Noi room near Hoan Kiem Lake, the Old Quarter, and weekend night market.', 'Hoan Kiem Lake, Ha Noi, Vietnam', 21.0287, 105.8523, null, 'WiFi, Breakfast, Air conditioning, Bathtub', '10000000-0000-4000-8000-000000000004', timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('20000000-0000-4000-8000-000000000012', 'West Lake Hanoi Apartment', 4, 135, 10, 'https://images.unsplash.com/photo-1600566753190-17f0baa2a6c3?auto=format&fit=crop&w=1200&q=80', 'A relaxed apartment near West Lake with balcony space, cafes, and lakeside walking routes.', 'West Lake, Tay Ho, Ha Noi, Vietnam', 21.0580, 105.8188, null, 'WiFi, Parking, Kitchen, Balcony', '10000000-0000-4000-8000-000000000002', timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00');


-- generated destination places and stays: 100 HCMC, 50 Vung Tau, 55 Ha Noi total including the hand-curated rows above
create table if not exists public.seed_extra_places (
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
truncate table public.seed_extra_places;

insert into public.seed_extra_places (city, destination_id, name, address, image, latitude, longitude, regular_price, max_capacity, discount) values
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Ben Thanh Market Stay 01', 'Ben Thanh Market, District 1, Ho Chi Minh City', 'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1200&q=80', 10.768700, 106.694900, 75, 2, 0),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Nguyen Hue Walking Street Stay 01', 'Nguyen Hue Walking Street, District 1, Ho Chi Minh City', 'https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?auto=format&fit=crop&w=1200&q=80', 10.773900, 106.700500, 80, 3, 2),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Notre-Dame Cathedral Stay 01', 'Notre-Dame Cathedral Basilica of Saigon, District 1, Ho Chi Minh City', 'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=1200&q=80', 10.779800, 106.695600, 85, 4, 4),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Central Post Office Stay 01', 'Saigon Central Post Office, District 1, Ho Chi Minh City', 'https://images.unsplash.com/photo-1564013799919-ab600027ffc6?auto=format&fit=crop&w=1200&q=80', 10.781600, 106.696400, 90, 5, 6),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Independence Palace Stay 01', 'Independence Palace, District 1, Ho Chi Minh City', 'https://images.unsplash.com/photo-1494526585095-c41746248156?auto=format&fit=crop&w=1200&q=80', 10.780500, 106.691900, 95, 2, 8),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'War Remnants Museum Stay 01', 'War Remnants Museum, District 3, Ho Chi Minh City', 'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=1200&q=80', 10.776100, 106.690300, 100, 3, 0),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Bitexco Financial Tower Stay 01', 'Bitexco Financial Tower, District 1, Ho Chi Minh City', 'https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=1200&q=80', 10.770000, 106.702700, 105, 4, 2),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Landmark 81 Stay 01', 'Landmark 81, Binh Thanh District, Ho Chi Minh City', 'https://images.unsplash.com/photo-1570129477492-45c003edd2be?auto=format&fit=crop&w=1200&q=80', 10.794000, 106.720100, 110, 5, 4),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Bui Vien Walking Street Stay 01', 'Bui Vien Walking Street, District 1, Ho Chi Minh City', 'https://images.unsplash.com/photo-1512918728675-ed5a9ecdebfd?auto=format&fit=crop&w=1200&q=80', 10.769400, 106.691500, 115, 2, 6),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Tan Dinh Church Stay 01', 'Tan Dinh Church, District 3, Ho Chi Minh City', 'https://images.unsplash.com/photo-1560185127-6ed189bf02f4?auto=format&fit=crop&w=1200&q=80', 10.791400, 106.689000, 75, 3, 8),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Saigon Zoo Stay 01', 'Saigon Zoo and Botanical Gardens, District 1, Ho Chi Minh City', 'https://images.unsplash.com/photo-1598928506311-c55ded91a20c?auto=format&fit=crop&w=1200&q=80', 10.784100, 106.705300, 80, 4, 0),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Thao Dien Stay 01', 'Thao Dien, Thu Duc City, Ho Chi Minh City', 'https://images.unsplash.com/photo-1600566753190-17f0baa2a6c3?auto=format&fit=crop&w=1200&q=80', 10.800500, 106.733400, 85, 5, 2),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Turtle Lake Stay 01', 'Turtle Lake, District 3, Ho Chi Minh City', 'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1200&q=80', 10.783000, 106.695700, 90, 2, 4),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Pham Ngu Lao Stay 01', 'Pham Ngu Lao, District 1, Ho Chi Minh City', 'https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?auto=format&fit=crop&w=1200&q=80', 10.768700, 106.693000, 95, 3, 6),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Cholon Chinatown Stay 01', 'Cholon, District 5, Ho Chi Minh City', 'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=1200&q=80', 10.757100, 106.664700, 100, 4, 8),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Binh Tay Market Stay 01', 'Binh Tay Market, District 6, Ho Chi Minh City', 'https://images.unsplash.com/photo-1564013799919-ab600027ffc6?auto=format&fit=crop&w=1200&q=80', 10.746500, 106.653500, 105, 5, 0),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Ho Thi Ky Flower Market Stay 01', 'Ho Thi Ky Flower Market, District 10, Ho Chi Minh City', 'https://images.unsplash.com/photo-1494526585095-c41746248156?auto=format&fit=crop&w=1200&q=80', 10.762400, 106.676400, 110, 2, 2),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Van Hanh Mall Stay 01', 'Van Hanh Mall, District 10, Ho Chi Minh City', 'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=1200&q=80', 10.770700, 106.671200, 115, 3, 4),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Saigon Exhibition Center Stay 01', 'SECC, District 7, Ho Chi Minh City', 'https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=1200&q=80', 10.731900, 106.723400, 75, 4, 6),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Phu My Hung Stay 01', 'Phu My Hung, District 7, Ho Chi Minh City', 'https://images.unsplash.com/photo-1570129477492-45c003edd2be?auto=format&fit=crop&w=1200&q=80', 10.732900, 106.705300, 80, 5, 8),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Crescent Mall Stay 01', 'Crescent Mall, District 7, Ho Chi Minh City', 'https://images.unsplash.com/photo-1512918728675-ed5a9ecdebfd?auto=format&fit=crop&w=1200&q=80', 10.725600, 106.721400, 85, 2, 0),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Dam Sen Park Stay 01', 'Dam Sen Cultural Park, District 11, Ho Chi Minh City', 'https://images.unsplash.com/photo-1560185127-6ed189bf02f4?auto=format&fit=crop&w=1200&q=80', 10.761800, 106.644400, 90, 3, 2),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Giac Lam Pagoda Stay 01', 'Giac Lam Pagoda, Tan Binh District, Ho Chi Minh City', 'https://images.unsplash.com/photo-1598928506311-c55ded91a20c?auto=format&fit=crop&w=1200&q=80', 10.783800, 106.655300, 95, 4, 4),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Tan Son Nhat Airport Stay 01', 'Tan Son Nhat Airport, Tan Binh District, Ho Chi Minh City', 'https://images.unsplash.com/photo-1600566753190-17f0baa2a6c3?auto=format&fit=crop&w=1200&q=80', 10.820500, 106.655300, 100, 5, 6),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Gia Dinh Park Stay 01', 'Gia Dinh Park, Go Vap District, Ho Chi Minh City', 'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1200&q=80', 10.816200, 106.681100, 105, 2, 8),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Vinhomes Central Park Stay 01', 'Vinhomes Central Park, Binh Thanh District, Ho Chi Minh City', 'https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?auto=format&fit=crop&w=1200&q=80', 10.791200, 106.717000, 110, 3, 0),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Bach Dang Wharf Stay 01', 'Bach Dang Wharf, District 1, Ho Chi Minh City', 'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=1200&q=80', 10.771600, 106.703000, 115, 4, 2),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Saigon Opera House Stay 01', 'Saigon Opera House, District 1, Ho Chi Minh City', 'https://images.unsplash.com/photo-1564013799919-ab600027ffc6?auto=format&fit=crop&w=1200&q=80', 10.776600, 106.699600, 75, 5, 4),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Japanese Town Le Thanh Ton Stay 01', 'Le Thanh Ton Japanese Town, District 1, Ho Chi Minh City', 'https://images.unsplash.com/photo-1494526585095-c41746248156?auto=format&fit=crop&w=1200&q=80', 10.783100, 106.701000, 80, 2, 6),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Book Street Stay 01', 'Nguyen Van Binh Book Street, District 1, Ho Chi Minh City', 'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=1200&q=80', 10.783700, 106.695800, 85, 3, 8),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Ben Thanh Market Stay 02', 'Ben Thanh Market, District 1, Ho Chi Minh City', 'https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=1200&q=80', 10.768700, 106.696600, 90, 4, 0),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Nguyen Hue Walking Street Stay 02', 'Nguyen Hue Walking Street, District 1, Ho Chi Minh City', 'https://images.unsplash.com/photo-1570129477492-45c003edd2be?auto=format&fit=crop&w=1200&q=80', 10.773900, 106.702200, 95, 5, 2),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Notre-Dame Cathedral Stay 02', 'Notre-Dame Cathedral Basilica of Saigon, District 1, Ho Chi Minh City', 'https://images.unsplash.com/photo-1512918728675-ed5a9ecdebfd?auto=format&fit=crop&w=1200&q=80', 10.779800, 106.697300, 100, 2, 4),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Central Post Office Stay 02', 'Saigon Central Post Office, District 1, Ho Chi Minh City', 'https://images.unsplash.com/photo-1560185127-6ed189bf02f4?auto=format&fit=crop&w=1200&q=80', 10.781600, 106.698100, 105, 3, 6),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Independence Palace Stay 02', 'Independence Palace, District 1, Ho Chi Minh City', 'https://images.unsplash.com/photo-1598928506311-c55ded91a20c?auto=format&fit=crop&w=1200&q=80', 10.780500, 106.693600, 110, 4, 8),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'War Remnants Museum Stay 02', 'War Remnants Museum, District 3, Ho Chi Minh City', 'https://images.unsplash.com/photo-1600566753190-17f0baa2a6c3?auto=format&fit=crop&w=1200&q=80', 10.776100, 106.692000, 115, 5, 0),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Bitexco Financial Tower Stay 02', 'Bitexco Financial Tower, District 1, Ho Chi Minh City', 'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1200&q=80', 10.770000, 106.704400, 75, 2, 2),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Landmark 81 Stay 02', 'Landmark 81, Binh Thanh District, Ho Chi Minh City', 'https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?auto=format&fit=crop&w=1200&q=80', 10.794000, 106.721800, 80, 3, 4),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Bui Vien Walking Street Stay 02', 'Bui Vien Walking Street, District 1, Ho Chi Minh City', 'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=1200&q=80', 10.769400, 106.693200, 85, 4, 6),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Tan Dinh Church Stay 02', 'Tan Dinh Church, District 3, Ho Chi Minh City', 'https://images.unsplash.com/photo-1564013799919-ab600027ffc6?auto=format&fit=crop&w=1200&q=80', 10.791400, 106.690700, 90, 5, 8),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Saigon Zoo Stay 02', 'Saigon Zoo and Botanical Gardens, District 1, Ho Chi Minh City', 'https://images.unsplash.com/photo-1494526585095-c41746248156?auto=format&fit=crop&w=1200&q=80', 10.784100, 106.707000, 95, 2, 0),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Thao Dien Stay 02', 'Thao Dien, Thu Duc City, Ho Chi Minh City', 'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=1200&q=80', 10.800500, 106.735100, 100, 3, 2),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Turtle Lake Stay 02', 'Turtle Lake, District 3, Ho Chi Minh City', 'https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=1200&q=80', 10.783000, 106.697400, 105, 4, 4),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Pham Ngu Lao Stay 02', 'Pham Ngu Lao, District 1, Ho Chi Minh City', 'https://images.unsplash.com/photo-1570129477492-45c003edd2be?auto=format&fit=crop&w=1200&q=80', 10.768700, 106.694700, 110, 5, 6),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Cholon Chinatown Stay 02', 'Cholon, District 5, Ho Chi Minh City', 'https://images.unsplash.com/photo-1512918728675-ed5a9ecdebfd?auto=format&fit=crop&w=1200&q=80', 10.757100, 106.666400, 115, 2, 8),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Binh Tay Market Stay 02', 'Binh Tay Market, District 6, Ho Chi Minh City', 'https://images.unsplash.com/photo-1560185127-6ed189bf02f4?auto=format&fit=crop&w=1200&q=80', 10.746500, 106.655200, 75, 3, 0),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Ho Thi Ky Flower Market Stay 02', 'Ho Thi Ky Flower Market, District 10, Ho Chi Minh City', 'https://images.unsplash.com/photo-1598928506311-c55ded91a20c?auto=format&fit=crop&w=1200&q=80', 10.762400, 106.678100, 80, 4, 2),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Van Hanh Mall Stay 02', 'Van Hanh Mall, District 10, Ho Chi Minh City', 'https://images.unsplash.com/photo-1600566753190-17f0baa2a6c3?auto=format&fit=crop&w=1200&q=80', 10.770700, 106.672900, 85, 5, 4),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Saigon Exhibition Center Stay 02', 'SECC, District 7, Ho Chi Minh City', 'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1200&q=80', 10.731900, 106.725100, 90, 2, 6),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Phu My Hung Stay 02', 'Phu My Hung, District 7, Ho Chi Minh City', 'https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?auto=format&fit=crop&w=1200&q=80', 10.732900, 106.707000, 95, 3, 8),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Crescent Mall Stay 02', 'Crescent Mall, District 7, Ho Chi Minh City', 'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=1200&q=80', 10.725600, 106.714600, 100, 4, 0),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Dam Sen Park Stay 02', 'Dam Sen Cultural Park, District 11, Ho Chi Minh City', 'https://images.unsplash.com/photo-1564013799919-ab600027ffc6?auto=format&fit=crop&w=1200&q=80', 10.761800, 106.637600, 105, 5, 2),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Giac Lam Pagoda Stay 02', 'Giac Lam Pagoda, Tan Binh District, Ho Chi Minh City', 'https://images.unsplash.com/photo-1494526585095-c41746248156?auto=format&fit=crop&w=1200&q=80', 10.783800, 106.648500, 110, 2, 4),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Tan Son Nhat Airport Stay 02', 'Tan Son Nhat Airport, Tan Binh District, Ho Chi Minh City', 'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=1200&q=80', 10.820500, 106.648500, 115, 3, 6),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Gia Dinh Park Stay 02', 'Gia Dinh Park, Go Vap District, Ho Chi Minh City', 'https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=1200&q=80', 10.816200, 106.674300, 75, 4, 8),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Vinhomes Central Park Stay 02', 'Vinhomes Central Park, Binh Thanh District, Ho Chi Minh City', 'https://images.unsplash.com/photo-1570129477492-45c003edd2be?auto=format&fit=crop&w=1200&q=80', 10.791200, 106.718700, 80, 5, 0),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Bach Dang Wharf Stay 02', 'Bach Dang Wharf, District 1, Ho Chi Minh City', 'https://images.unsplash.com/photo-1512918728675-ed5a9ecdebfd?auto=format&fit=crop&w=1200&q=80', 10.771600, 106.704700, 85, 2, 2),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Saigon Opera House Stay 02', 'Saigon Opera House, District 1, Ho Chi Minh City', 'https://images.unsplash.com/photo-1560185127-6ed189bf02f4?auto=format&fit=crop&w=1200&q=80', 10.776600, 106.701300, 90, 3, 4),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Japanese Town Le Thanh Ton Stay 02', 'Le Thanh Ton Japanese Town, District 1, Ho Chi Minh City', 'https://images.unsplash.com/photo-1598928506311-c55ded91a20c?auto=format&fit=crop&w=1200&q=80', 10.783100, 106.702700, 95, 4, 6),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Book Street Stay 02', 'Nguyen Van Binh Book Street, District 1, Ho Chi Minh City', 'https://images.unsplash.com/photo-1600566753190-17f0baa2a6c3?auto=format&fit=crop&w=1200&q=80', 10.783700, 106.697500, 100, 5, 8),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Ben Thanh Market Stay 03', 'Ben Thanh Market, District 1, Ho Chi Minh City', 'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1200&q=80', 10.768700, 106.698300, 105, 2, 0),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Nguyen Hue Walking Street Stay 03', 'Nguyen Hue Walking Street, District 1, Ho Chi Minh City', 'https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?auto=format&fit=crop&w=1200&q=80', 10.773900, 106.703900, 110, 3, 2),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Notre-Dame Cathedral Stay 03', 'Notre-Dame Cathedral Basilica of Saigon, District 1, Ho Chi Minh City', 'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=1200&q=80', 10.779800, 106.699000, 115, 4, 4),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Central Post Office Stay 03', 'Saigon Central Post Office, District 1, Ho Chi Minh City', 'https://images.unsplash.com/photo-1564013799919-ab600027ffc6?auto=format&fit=crop&w=1200&q=80', 10.781600, 106.699800, 75, 5, 6),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Independence Palace Stay 03', 'Independence Palace, District 1, Ho Chi Minh City', 'https://images.unsplash.com/photo-1494526585095-c41746248156?auto=format&fit=crop&w=1200&q=80', 10.780500, 106.695300, 80, 2, 8),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'War Remnants Museum Stay 03', 'War Remnants Museum, District 3, Ho Chi Minh City', 'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=1200&q=80', 10.776100, 106.693700, 85, 3, 0),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Bitexco Financial Tower Stay 03', 'Bitexco Financial Tower, District 1, Ho Chi Minh City', 'https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=1200&q=80', 10.770000, 106.706100, 90, 4, 2),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Landmark 81 Stay 03', 'Landmark 81, Binh Thanh District, Ho Chi Minh City', 'https://images.unsplash.com/photo-1570129477492-45c003edd2be?auto=format&fit=crop&w=1200&q=80', 10.794000, 106.723500, 95, 5, 4),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Bui Vien Walking Street Stay 03', 'Bui Vien Walking Street, District 1, Ho Chi Minh City', 'https://images.unsplash.com/photo-1512918728675-ed5a9ecdebfd?auto=format&fit=crop&w=1200&q=80', 10.769400, 106.694900, 100, 2, 6),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Tan Dinh Church Stay 03', 'Tan Dinh Church, District 3, Ho Chi Minh City', 'https://images.unsplash.com/photo-1560185127-6ed189bf02f4?auto=format&fit=crop&w=1200&q=80', 10.791400, 106.692400, 105, 3, 8),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Saigon Zoo Stay 03', 'Saigon Zoo and Botanical Gardens, District 1, Ho Chi Minh City', 'https://images.unsplash.com/photo-1598928506311-c55ded91a20c?auto=format&fit=crop&w=1200&q=80', 10.784100, 106.708700, 110, 4, 0),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Thao Dien Stay 03', 'Thao Dien, Thu Duc City, Ho Chi Minh City', 'https://images.unsplash.com/photo-1600566753190-17f0baa2a6c3?auto=format&fit=crop&w=1200&q=80', 10.800500, 106.736800, 115, 5, 2),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Turtle Lake Stay 03', 'Turtle Lake, District 3, Ho Chi Minh City', 'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1200&q=80', 10.783000, 106.699100, 75, 2, 4),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Pham Ngu Lao Stay 03', 'Pham Ngu Lao, District 1, Ho Chi Minh City', 'https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?auto=format&fit=crop&w=1200&q=80', 10.768700, 106.696400, 80, 3, 6),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Cholon Chinatown Stay 03', 'Cholon, District 5, Ho Chi Minh City', 'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=1200&q=80', 10.757100, 106.668100, 85, 4, 8),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Binh Tay Market Stay 03', 'Binh Tay Market, District 6, Ho Chi Minh City', 'https://images.unsplash.com/photo-1564013799919-ab600027ffc6?auto=format&fit=crop&w=1200&q=80', 10.746500, 106.648400, 90, 5, 0),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Ho Thi Ky Flower Market Stay 03', 'Ho Thi Ky Flower Market, District 10, Ho Chi Minh City', 'https://images.unsplash.com/photo-1494526585095-c41746248156?auto=format&fit=crop&w=1200&q=80', 10.762400, 106.671300, 95, 2, 2),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Van Hanh Mall Stay 03', 'Van Hanh Mall, District 10, Ho Chi Minh City', 'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=1200&q=80', 10.770700, 106.666100, 100, 3, 4),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Saigon Exhibition Center Stay 03', 'SECC, District 7, Ho Chi Minh City', 'https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=1200&q=80', 10.731900, 106.718300, 105, 4, 6),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Phu My Hung Stay 03', 'Phu My Hung, District 7, Ho Chi Minh City', 'https://images.unsplash.com/photo-1570129477492-45c003edd2be?auto=format&fit=crop&w=1200&q=80', 10.732900, 106.700200, 110, 5, 8),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Crescent Mall Stay 03', 'Crescent Mall, District 7, Ho Chi Minh City', 'https://images.unsplash.com/photo-1512918728675-ed5a9ecdebfd?auto=format&fit=crop&w=1200&q=80', 10.725600, 106.716300, 115, 2, 0),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Dam Sen Park Stay 03', 'Dam Sen Cultural Park, District 11, Ho Chi Minh City', 'https://images.unsplash.com/photo-1560185127-6ed189bf02f4?auto=format&fit=crop&w=1200&q=80', 10.761800, 106.639300, 75, 3, 2),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Giac Lam Pagoda Stay 03', 'Giac Lam Pagoda, Tan Binh District, Ho Chi Minh City', 'https://images.unsplash.com/photo-1598928506311-c55ded91a20c?auto=format&fit=crop&w=1200&q=80', 10.783800, 106.650200, 80, 4, 4),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Tan Son Nhat Airport Stay 03', 'Tan Son Nhat Airport, Tan Binh District, Ho Chi Minh City', 'https://images.unsplash.com/photo-1600566753190-17f0baa2a6c3?auto=format&fit=crop&w=1200&q=80', 10.820500, 106.650200, 85, 5, 6),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Gia Dinh Park Stay 03', 'Gia Dinh Park, Go Vap District, Ho Chi Minh City', 'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1200&q=80', 10.816200, 106.676000, 90, 2, 8),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Vinhomes Central Park Stay 03', 'Vinhomes Central Park, Binh Thanh District, Ho Chi Minh City', 'https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?auto=format&fit=crop&w=1200&q=80', 10.791200, 106.720400, 95, 3, 0),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Bach Dang Wharf Stay 03', 'Bach Dang Wharf, District 1, Ho Chi Minh City', 'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=1200&q=80', 10.771600, 106.706400, 100, 4, 2),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Saigon Opera House Stay 03', 'Saigon Opera House, District 1, Ho Chi Minh City', 'https://images.unsplash.com/photo-1564013799919-ab600027ffc6?auto=format&fit=crop&w=1200&q=80', 10.776600, 106.703000, 105, 5, 4),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Japanese Town Le Thanh Ton Stay 03', 'Le Thanh Ton Japanese Town, District 1, Ho Chi Minh City', 'https://images.unsplash.com/photo-1494526585095-c41746248156?auto=format&fit=crop&w=1200&q=80', 10.783100, 106.704400, 110, 2, 6),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Book Street Stay 03', 'Nguyen Van Binh Book Street, District 1, Ho Chi Minh City', 'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=1200&q=80', 10.783700, 106.699200, 115, 3, 8),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Ben Thanh Market Stay 04', 'Ben Thanh Market, District 1, Ho Chi Minh City', 'https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=1200&q=80', 10.768700, 106.700000, 75, 4, 0),
  ('Ho Chi Minh City', '91000000-0000-4000-8000-000000000001', 'Nguyen Hue Walking Street Stay 04', 'Nguyen Hue Walking Street, District 1, Ho Chi Minh City', 'https://images.unsplash.com/photo-1570129477492-45c003edd2be?auto=format&fit=crop&w=1200&q=80', 10.773900, 106.705600, 80, 5, 2),
  ('Vung Tau', '91000000-0000-4000-8000-000000000002', 'Front Beach Stay 01', 'Front Beach, Vung Tau, Ba Ria - Vung Tau, Vietnam', 'https://images.unsplash.com/photo-1512918728675-ed5a9ecdebfd?auto=format&fit=crop&w=1200&q=80', 10.342500, 107.073000, 65, 2, 0),
  ('Vung Tau', '91000000-0000-4000-8000-000000000002', 'Back Beach Stay 01', 'Back Beach, Vung Tau, Ba Ria - Vung Tau, Vietnam', 'https://images.unsplash.com/photo-1560185127-6ed189bf02f4?auto=format&fit=crop&w=1200&q=80', 10.333600, 107.089700, 70, 3, 2),
  ('Vung Tau', '91000000-0000-4000-8000-000000000002', 'Christ the King Statue Stay 01', 'Christ the King Statue, Vung Tau, Vietnam', 'https://images.unsplash.com/photo-1598928506311-c55ded91a20c?auto=format&fit=crop&w=1200&q=80', 10.331500, 107.081200, 75, 4, 4),
  ('Vung Tau', '91000000-0000-4000-8000-000000000002', 'Vung Tau Lighthouse Stay 01', 'Vung Tau Lighthouse, Vung Tau, Vietnam', 'https://images.unsplash.com/photo-1600566753190-17f0baa2a6c3?auto=format&fit=crop&w=1200&q=80', 10.338900, 107.075300, 80, 5, 6),
  ('Vung Tau', '91000000-0000-4000-8000-000000000002', 'Nghinh Phong Cape Stay 01', 'Nghinh Phong Cape, Vung Tau, Vietnam', 'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1200&q=80', 10.327900, 107.080600, 85, 2, 8),
  ('Vung Tau', '91000000-0000-4000-8000-000000000002', 'Hon Ba Island Stay 01', 'Hon Ba Island, Vung Tau, Vietnam', 'https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?auto=format&fit=crop&w=1200&q=80', 10.325800, 107.085700, 90, 3, 0),
  ('Vung Tau', '91000000-0000-4000-8000-000000000002', 'Bach Dinh White Palace Stay 01', 'Bach Dinh, Vung Tau, Vietnam', 'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=1200&q=80', 10.349700, 107.071100, 95, 4, 2),
  ('Vung Tau', '91000000-0000-4000-8000-000000000002', 'Ho May Park Stay 01', 'Ho May Park, Vung Tau, Vietnam', 'https://images.unsplash.com/photo-1564013799919-ab600027ffc6?auto=format&fit=crop&w=1200&q=80', 10.365000, 107.074500, 100, 5, 4),
  ('Vung Tau', '91000000-0000-4000-8000-000000000002', 'Marina Vung Tau Stay 01', 'Vung Tau Marina, Vung Tau, Vietnam', 'https://images.unsplash.com/photo-1494526585095-c41746248156?auto=format&fit=crop&w=1200&q=80', 10.410900, 107.112600, 105, 2, 6),
  ('Vung Tau', '91000000-0000-4000-8000-000000000002', 'Long Hai Beach Stay 01', 'Long Hai Beach, Ba Ria - Vung Tau, Vietnam', 'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=1200&q=80', 10.389600, 107.238900, 65, 3, 8),
  ('Vung Tau', '91000000-0000-4000-8000-000000000002', 'Doi Con Heo Stay 01', 'Doi Con Heo, Vung Tau, Vietnam', 'https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=1200&q=80', 10.340600, 107.091000, 70, 4, 0),
  ('Vung Tau', '91000000-0000-4000-8000-000000000002', 'Bai Dau Beach Stay 01', 'Bai Dau Beach, Vung Tau, Vietnam', 'https://images.unsplash.com/photo-1570129477492-45c003edd2be?auto=format&fit=crop&w=1200&q=80', 10.371900, 107.065300, 75, 5, 2),
  ('Vung Tau', '91000000-0000-4000-8000-000000000002', 'Niet Ban Tinh Xa Stay 01', 'Niet Ban Tinh Xa, Vung Tau, Vietnam', 'https://images.unsplash.com/photo-1512918728675-ed5a9ecdebfd?auto=format&fit=crop&w=1200&q=80', 10.331200, 107.082500, 80, 2, 4),
  ('Vung Tau', '91000000-0000-4000-8000-000000000002', 'Thich Ca Phat Dai Stay 01', 'Thich Ca Phat Dai, Vung Tau, Vietnam', 'https://images.unsplash.com/photo-1560185127-6ed189bf02f4?auto=format&fit=crop&w=1200&q=80', 10.373200, 107.070600, 85, 3, 6),
  ('Vung Tau', '91000000-0000-4000-8000-000000000002', 'Lam Son Stadium Stay 01', 'Lam Son Stadium, Vung Tau, Vietnam', 'https://images.unsplash.com/photo-1598928506311-c55ded91a20c?auto=format&fit=crop&w=1200&q=80', 10.354600, 107.080100, 90, 4, 8),
  ('Vung Tau', '91000000-0000-4000-8000-000000000002', 'Front Beach Stay 02', 'Front Beach, Vung Tau, Ba Ria - Vung Tau, Vietnam', 'https://images.unsplash.com/photo-1600566753190-17f0baa2a6c3?auto=format&fit=crop&w=1200&q=80', 10.342500, 107.078100, 95, 5, 0),
  ('Vung Tau', '91000000-0000-4000-8000-000000000002', 'Back Beach Stay 02', 'Back Beach, Vung Tau, Ba Ria - Vung Tau, Vietnam', 'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1200&q=80', 10.333600, 107.094800, 100, 2, 2),
  ('Vung Tau', '91000000-0000-4000-8000-000000000002', 'Christ the King Statue Stay 02', 'Christ the King Statue, Vung Tau, Vietnam', 'https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?auto=format&fit=crop&w=1200&q=80', 10.331500, 107.086300, 105, 3, 4),
  ('Vung Tau', '91000000-0000-4000-8000-000000000002', 'Vung Tau Lighthouse Stay 02', 'Vung Tau Lighthouse, Vung Tau, Vietnam', 'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=1200&q=80', 10.338900, 107.080400, 65, 4, 6),
  ('Vung Tau', '91000000-0000-4000-8000-000000000002', 'Nghinh Phong Cape Stay 02', 'Nghinh Phong Cape, Vung Tau, Vietnam', 'https://images.unsplash.com/photo-1564013799919-ab600027ffc6?auto=format&fit=crop&w=1200&q=80', 10.327900, 107.085700, 70, 5, 8),
  ('Vung Tau', '91000000-0000-4000-8000-000000000002', 'Hon Ba Island Stay 02', 'Hon Ba Island, Vung Tau, Vietnam', 'https://images.unsplash.com/photo-1494526585095-c41746248156?auto=format&fit=crop&w=1200&q=80', 10.325800, 107.090800, 75, 2, 0),
  ('Vung Tau', '91000000-0000-4000-8000-000000000002', 'Bach Dinh White Palace Stay 02', 'Bach Dinh, Vung Tau, Vietnam', 'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=1200&q=80', 10.349700, 107.076200, 80, 3, 2),
  ('Vung Tau', '91000000-0000-4000-8000-000000000002', 'Ho May Park Stay 02', 'Ho May Park, Vung Tau, Vietnam', 'https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=1200&q=80', 10.365000, 107.079600, 85, 4, 4),
  ('Vung Tau', '91000000-0000-4000-8000-000000000002', 'Marina Vung Tau Stay 02', 'Vung Tau Marina, Vung Tau, Vietnam', 'https://images.unsplash.com/photo-1570129477492-45c003edd2be?auto=format&fit=crop&w=1200&q=80', 10.410900, 107.117700, 90, 5, 6),
  ('Vung Tau', '91000000-0000-4000-8000-000000000002', 'Long Hai Beach Stay 02', 'Long Hai Beach, Ba Ria - Vung Tau, Vietnam', 'https://images.unsplash.com/photo-1512918728675-ed5a9ecdebfd?auto=format&fit=crop&w=1200&q=80', 10.389600, 107.244000, 95, 2, 8),
  ('Vung Tau', '91000000-0000-4000-8000-000000000002', 'Doi Con Heo Stay 02', 'Doi Con Heo, Vung Tau, Vietnam', 'https://images.unsplash.com/photo-1560185127-6ed189bf02f4?auto=format&fit=crop&w=1200&q=80', 10.340600, 107.087600, 100, 3, 0),
  ('Vung Tau', '91000000-0000-4000-8000-000000000002', 'Bai Dau Beach Stay 02', 'Bai Dau Beach, Vung Tau, Vietnam', 'https://images.unsplash.com/photo-1598928506311-c55ded91a20c?auto=format&fit=crop&w=1200&q=80', 10.371900, 107.061900, 105, 4, 2),
  ('Vung Tau', '91000000-0000-4000-8000-000000000002', 'Niet Ban Tinh Xa Stay 02', 'Niet Ban Tinh Xa, Vung Tau, Vietnam', 'https://images.unsplash.com/photo-1600566753190-17f0baa2a6c3?auto=format&fit=crop&w=1200&q=80', 10.331200, 107.079100, 65, 5, 4),
  ('Vung Tau', '91000000-0000-4000-8000-000000000002', 'Thich Ca Phat Dai Stay 02', 'Thich Ca Phat Dai, Vung Tau, Vietnam', 'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1200&q=80', 10.373200, 107.067200, 70, 2, 6),
  ('Vung Tau', '91000000-0000-4000-8000-000000000002', 'Lam Son Stadium Stay 02', 'Lam Son Stadium, Vung Tau, Vietnam', 'https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?auto=format&fit=crop&w=1200&q=80', 10.354600, 107.076700, 75, 3, 8),
  ('Vung Tau', '91000000-0000-4000-8000-000000000002', 'Front Beach Stay 03', 'Front Beach, Vung Tau, Ba Ria - Vung Tau, Vietnam', 'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=1200&q=80', 10.342500, 107.074700, 80, 4, 0),
  ('Vung Tau', '91000000-0000-4000-8000-000000000002', 'Back Beach Stay 03', 'Back Beach, Vung Tau, Ba Ria - Vung Tau, Vietnam', 'https://images.unsplash.com/photo-1564013799919-ab600027ffc6?auto=format&fit=crop&w=1200&q=80', 10.333600, 107.091400, 85, 5, 2),
  ('Vung Tau', '91000000-0000-4000-8000-000000000002', 'Christ the King Statue Stay 03', 'Christ the King Statue, Vung Tau, Vietnam', 'https://images.unsplash.com/photo-1494526585095-c41746248156?auto=format&fit=crop&w=1200&q=80', 10.331500, 107.082900, 90, 2, 4),
  ('Vung Tau', '91000000-0000-4000-8000-000000000002', 'Vung Tau Lighthouse Stay 03', 'Vung Tau Lighthouse, Vung Tau, Vietnam', 'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=1200&q=80', 10.338900, 107.077000, 95, 3, 6),
  ('Vung Tau', '91000000-0000-4000-8000-000000000002', 'Nghinh Phong Cape Stay 03', 'Nghinh Phong Cape, Vung Tau, Vietnam', 'https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=1200&q=80', 10.327900, 107.082300, 100, 4, 8),
  ('Vung Tau', '91000000-0000-4000-8000-000000000002', 'Hon Ba Island Stay 03', 'Hon Ba Island, Vung Tau, Vietnam', 'https://images.unsplash.com/photo-1570129477492-45c003edd2be?auto=format&fit=crop&w=1200&q=80', 10.325800, 107.087400, 105, 5, 0),
  ('Vung Tau', '91000000-0000-4000-8000-000000000002', 'Bach Dinh White Palace Stay 03', 'Bach Dinh, Vung Tau, Vietnam', 'https://images.unsplash.com/photo-1512918728675-ed5a9ecdebfd?auto=format&fit=crop&w=1200&q=80', 10.349700, 107.072800, 65, 2, 2),
  ('Vung Tau', '91000000-0000-4000-8000-000000000002', 'Ho May Park Stay 03', 'Ho May Park, Vung Tau, Vietnam', 'https://images.unsplash.com/photo-1560185127-6ed189bf02f4?auto=format&fit=crop&w=1200&q=80', 10.365000, 107.076200, 70, 3, 4),
  ('Vung Tau', '91000000-0000-4000-8000-000000000002', 'Marina Vung Tau Stay 03', 'Vung Tau Marina, Vung Tau, Vietnam', 'https://images.unsplash.com/photo-1598928506311-c55ded91a20c?auto=format&fit=crop&w=1200&q=80', 10.410900, 107.114300, 75, 4, 6),
  ('Vung Tau', '91000000-0000-4000-8000-000000000002', 'Long Hai Beach Stay 03', 'Long Hai Beach, Ba Ria - Vung Tau, Vietnam', 'https://images.unsplash.com/photo-1600566753190-17f0baa2a6c3?auto=format&fit=crop&w=1200&q=80', 10.389600, 107.240600, 80, 5, 8),
  ('Vung Tau', '91000000-0000-4000-8000-000000000002', 'Doi Con Heo Stay 03', 'Doi Con Heo, Vung Tau, Vietnam', 'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1200&q=80', 10.340600, 107.092700, 85, 2, 0),
  ('Vung Tau', '91000000-0000-4000-8000-000000000002', 'Bai Dau Beach Stay 03', 'Bai Dau Beach, Vung Tau, Vietnam', 'https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?auto=format&fit=crop&w=1200&q=80', 10.371900, 107.067000, 90, 3, 2),
  ('Vung Tau', '91000000-0000-4000-8000-000000000002', 'Niet Ban Tinh Xa Stay 03', 'Niet Ban Tinh Xa, Vung Tau, Vietnam', 'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=1200&q=80', 10.331200, 107.084200, 95, 4, 4),
  ('Vung Tau', '91000000-0000-4000-8000-000000000002', 'Thich Ca Phat Dai Stay 03', 'Thich Ca Phat Dai, Vung Tau, Vietnam', 'https://images.unsplash.com/photo-1564013799919-ab600027ffc6?auto=format&fit=crop&w=1200&q=80', 10.373200, 107.072300, 100, 5, 6),
  ('Vung Tau', '91000000-0000-4000-8000-000000000002', 'Lam Son Stadium Stay 03', 'Lam Son Stadium, Vung Tau, Vietnam', 'https://images.unsplash.com/photo-1494526585095-c41746248156?auto=format&fit=crop&w=1200&q=80', 10.354600, 107.081800, 105, 2, 8),
  ('Vung Tau', '91000000-0000-4000-8000-000000000002', 'Front Beach Stay 04', 'Front Beach, Vung Tau, Ba Ria - Vung Tau, Vietnam', 'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=1200&q=80', 10.342500, 107.079800, 65, 3, 0),
  ('Vung Tau', '91000000-0000-4000-8000-000000000002', 'Back Beach Stay 04', 'Back Beach, Vung Tau, Ba Ria - Vung Tau, Vietnam', 'https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=1200&q=80', 10.333600, 107.096500, 70, 4, 2),
  ('Vung Tau', '91000000-0000-4000-8000-000000000002', 'Christ the King Statue Stay 04', 'Christ the King Statue, Vung Tau, Vietnam', 'https://images.unsplash.com/photo-1570129477492-45c003edd2be?auto=format&fit=crop&w=1200&q=80', 10.331500, 107.088000, 75, 5, 4),
  ('Ha Noi', '91000000-0000-4000-8000-000000000003', 'Hoan Kiem Lake Stay 01', 'Hoan Kiem Lake, Ha Noi, Vietnam', 'https://images.unsplash.com/photo-1512918728675-ed5a9ecdebfd?auto=format&fit=crop&w=1200&q=80', 21.025300, 105.848900, 70, 2, 0),
  ('Ha Noi', '91000000-0000-4000-8000-000000000003', 'Old Quarter Stay 01', 'Old Quarter, Hoan Kiem, Ha Noi, Vietnam', 'https://images.unsplash.com/photo-1560185127-6ed189bf02f4?auto=format&fit=crop&w=1200&q=80', 21.032100, 105.846600, 75, 3, 2),
  ('Ha Noi', '91000000-0000-4000-8000-000000000003', 'West Lake Stay 01', 'West Lake, Tay Ho, Ha Noi, Vietnam', 'https://images.unsplash.com/photo-1598928506311-c55ded91a20c?auto=format&fit=crop&w=1200&q=80', 21.058000, 105.815400, 80, 4, 4),
  ('Ha Noi', '91000000-0000-4000-8000-000000000003', 'Temple of Literature Stay 01', 'Temple of Literature, Dong Da, Ha Noi, Vietnam', 'https://images.unsplash.com/photo-1600566753190-17f0baa2a6c3?auto=format&fit=crop&w=1200&q=80', 21.029700, 105.832200, 85, 5, 6),
  ('Ha Noi', '91000000-0000-4000-8000-000000000003', 'Ho Chi Minh Mausoleum Stay 01', 'Ho Chi Minh Mausoleum, Ba Dinh, Ha Noi, Vietnam', 'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1200&q=80', 21.040100, 105.831200, 90, 2, 8),
  ('Ha Noi', '91000000-0000-4000-8000-000000000003', 'One Pillar Pagoda Stay 01', 'One Pillar Pagoda, Ba Dinh, Ha Noi, Vietnam', 'https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?auto=format&fit=crop&w=1200&q=80', 21.032500, 105.831900, 95, 3, 0),
  ('Ha Noi', '91000000-0000-4000-8000-000000000003', 'Hanoi Opera House Stay 01', 'Hanoi Opera House, Hoan Kiem, Ha Noi, Vietnam', 'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=1200&q=80', 21.022600, 105.855300, 100, 4, 2),
  ('Ha Noi', '91000000-0000-4000-8000-000000000003', 'Long Bien Bridge Stay 01', 'Long Bien Bridge, Ha Noi, Vietnam', 'https://images.unsplash.com/photo-1564013799919-ab600027ffc6?auto=format&fit=crop&w=1200&q=80', 21.042200, 105.856500, 105, 5, 4),
  ('Ha Noi', '91000000-0000-4000-8000-000000000003', 'St Joseph Cathedral Stay 01', 'St Joseph Cathedral, Hoan Kiem, Ha Noi, Vietnam', 'https://images.unsplash.com/photo-1494526585095-c41746248156?auto=format&fit=crop&w=1200&q=80', 21.030400, 105.847700, 110, 2, 6),
  ('Ha Noi', '91000000-0000-4000-8000-000000000003', 'Tran Quoc Pagoda Stay 01', 'Tran Quoc Pagoda, Tay Ho, Ha Noi, Vietnam', 'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=1200&q=80', 21.051300, 105.834300, 70, 3, 8),
  ('Ha Noi', '91000000-0000-4000-8000-000000000003', 'Vietnam Museum of Ethnology Stay 01', 'Vietnam Museum of Ethnology, Cau Giay, Ha Noi, Vietnam', 'https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=1200&q=80', 21.037100, 105.798000, 75, 4, 0),
  ('Ha Noi', '91000000-0000-4000-8000-000000000003', 'Lotte Center Hanoi Stay 01', 'Lotte Center Hanoi, Ba Dinh, Ha Noi, Vietnam', 'https://images.unsplash.com/photo-1570129477492-45c003edd2be?auto=format&fit=crop&w=1200&q=80', 21.030500, 105.812800, 80, 5, 2),
  ('Ha Noi', '91000000-0000-4000-8000-000000000003', 'Keangnam Landmark 72 Stay 01', 'Keangnam Landmark 72, Nam Tu Liem, Ha Noi, Vietnam', 'https://images.unsplash.com/photo-1512918728675-ed5a9ecdebfd?auto=format&fit=crop&w=1200&q=80', 21.016900, 105.783300, 85, 2, 4),
  ('Ha Noi', '91000000-0000-4000-8000-000000000003', 'Dong Xuan Market Stay 01', 'Dong Xuan Market, Hoan Kiem, Ha Noi, Vietnam', 'https://images.unsplash.com/photo-1560185127-6ed189bf02f4?auto=format&fit=crop&w=1200&q=80', 21.039800, 105.850200, 90, 3, 6),
  ('Ha Noi', '91000000-0000-4000-8000-000000000003', 'Hanoi Train Street Stay 01', 'Hanoi Train Street, Hoan Kiem, Ha Noi, Vietnam', 'https://images.unsplash.com/photo-1598928506311-c55ded91a20c?auto=format&fit=crop&w=1200&q=80', 21.034200, 105.843100, 95, 4, 8),
  ('Ha Noi', '91000000-0000-4000-8000-000000000003', 'Imperial Citadel Stay 01', 'Imperial Citadel of Thang Long, Ba Dinh, Ha Noi, Vietnam', 'https://images.unsplash.com/photo-1600566753190-17f0baa2a6c3?auto=format&fit=crop&w=1200&q=80', 21.031800, 105.842000, 100, 5, 0),
  ('Ha Noi', '91000000-0000-4000-8000-000000000003', 'Truc Bach Lake Stay 01', 'Truc Bach Lake, Ba Dinh, Ha Noi, Vietnam', 'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1200&q=80', 21.043300, 105.840900, 105, 2, 2),
  ('Ha Noi', '91000000-0000-4000-8000-000000000003', 'Quang Ba Flower Market Stay 01', 'Quang Ba Flower Market, Tay Ho, Ha Noi, Vietnam', 'https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?auto=format&fit=crop&w=1200&q=80', 21.068900, 105.828600, 110, 3, 4),
  ('Ha Noi', '91000000-0000-4000-8000-000000000003', 'Aeon Mall Long Bien Stay 01', 'Aeon Mall Long Bien, Long Bien, Ha Noi, Vietnam', 'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=1200&q=80', 21.029200, 105.901200, 70, 4, 6),
  ('Ha Noi', '91000000-0000-4000-8000-000000000003', 'Vincom Ba Trieu Stay 01', 'Vincom Center Ba Trieu, Hai Ba Trung, Ha Noi, Vietnam', 'https://images.unsplash.com/photo-1564013799919-ab600027ffc6?auto=format&fit=crop&w=1200&q=80', 21.015100, 105.851500, 75, 5, 8),
  ('Ha Noi', '91000000-0000-4000-8000-000000000003', 'Hoan Kiem Lake Stay 02', 'Hoan Kiem Lake, Ha Noi, Vietnam', 'https://images.unsplash.com/photo-1494526585095-c41746248156?auto=format&fit=crop&w=1200&q=80', 21.025300, 105.855700, 80, 2, 0),
  ('Ha Noi', '91000000-0000-4000-8000-000000000003', 'Old Quarter Stay 02', 'Old Quarter, Hoan Kiem, Ha Noi, Vietnam', 'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=1200&q=80', 21.032100, 105.853400, 85, 3, 2),
  ('Ha Noi', '91000000-0000-4000-8000-000000000003', 'West Lake Stay 02', 'West Lake, Tay Ho, Ha Noi, Vietnam', 'https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=1200&q=80', 21.058000, 105.822200, 90, 4, 4),
  ('Ha Noi', '91000000-0000-4000-8000-000000000003', 'Temple of Literature Stay 02', 'Temple of Literature, Dong Da, Ha Noi, Vietnam', 'https://images.unsplash.com/photo-1570129477492-45c003edd2be?auto=format&fit=crop&w=1200&q=80', 21.029700, 105.839000, 95, 5, 6),
  ('Ha Noi', '91000000-0000-4000-8000-000000000003', 'Ho Chi Minh Mausoleum Stay 02', 'Ho Chi Minh Mausoleum, Ba Dinh, Ha Noi, Vietnam', 'https://images.unsplash.com/photo-1512918728675-ed5a9ecdebfd?auto=format&fit=crop&w=1200&q=80', 21.040100, 105.838000, 100, 2, 8),
  ('Ha Noi', '91000000-0000-4000-8000-000000000003', 'One Pillar Pagoda Stay 02', 'One Pillar Pagoda, Ba Dinh, Ha Noi, Vietnam', 'https://images.unsplash.com/photo-1560185127-6ed189bf02f4?auto=format&fit=crop&w=1200&q=80', 21.032500, 105.830200, 105, 3, 0),
  ('Ha Noi', '91000000-0000-4000-8000-000000000003', 'Hanoi Opera House Stay 02', 'Hanoi Opera House, Hoan Kiem, Ha Noi, Vietnam', 'https://images.unsplash.com/photo-1598928506311-c55ded91a20c?auto=format&fit=crop&w=1200&q=80', 21.022600, 105.853600, 110, 4, 2),
  ('Ha Noi', '91000000-0000-4000-8000-000000000003', 'Long Bien Bridge Stay 02', 'Long Bien Bridge, Ha Noi, Vietnam', 'https://images.unsplash.com/photo-1600566753190-17f0baa2a6c3?auto=format&fit=crop&w=1200&q=80', 21.042200, 105.854800, 70, 5, 4),
  ('Ha Noi', '91000000-0000-4000-8000-000000000003', 'St Joseph Cathedral Stay 02', 'St Joseph Cathedral, Hoan Kiem, Ha Noi, Vietnam', 'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1200&q=80', 21.030400, 105.846000, 75, 2, 6),
  ('Ha Noi', '91000000-0000-4000-8000-000000000003', 'Tran Quoc Pagoda Stay 02', 'Tran Quoc Pagoda, Tay Ho, Ha Noi, Vietnam', 'https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?auto=format&fit=crop&w=1200&q=80', 21.051300, 105.832600, 80, 3, 8),
  ('Ha Noi', '91000000-0000-4000-8000-000000000003', 'Vietnam Museum of Ethnology Stay 02', 'Vietnam Museum of Ethnology, Cau Giay, Ha Noi, Vietnam', 'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=1200&q=80', 21.037100, 105.796300, 85, 4, 0),
  ('Ha Noi', '91000000-0000-4000-8000-000000000003', 'Lotte Center Hanoi Stay 02', 'Lotte Center Hanoi, Ba Dinh, Ha Noi, Vietnam', 'https://images.unsplash.com/photo-1564013799919-ab600027ffc6?auto=format&fit=crop&w=1200&q=80', 21.030500, 105.811100, 90, 5, 2),
  ('Ha Noi', '91000000-0000-4000-8000-000000000003', 'Keangnam Landmark 72 Stay 02', 'Keangnam Landmark 72, Nam Tu Liem, Ha Noi, Vietnam', 'https://images.unsplash.com/photo-1494526585095-c41746248156?auto=format&fit=crop&w=1200&q=80', 21.016900, 105.781600, 95, 2, 4),
  ('Ha Noi', '91000000-0000-4000-8000-000000000003', 'Dong Xuan Market Stay 02', 'Dong Xuan Market, Hoan Kiem, Ha Noi, Vietnam', 'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=1200&q=80', 21.039800, 105.848500, 100, 3, 6),
  ('Ha Noi', '91000000-0000-4000-8000-000000000003', 'Hanoi Train Street Stay 02', 'Hanoi Train Street, Hoan Kiem, Ha Noi, Vietnam', 'https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=1200&q=80', 21.034200, 105.841400, 105, 4, 8),
  ('Ha Noi', '91000000-0000-4000-8000-000000000003', 'Imperial Citadel Stay 02', 'Imperial Citadel of Thang Long, Ba Dinh, Ha Noi, Vietnam', 'https://images.unsplash.com/photo-1570129477492-45c003edd2be?auto=format&fit=crop&w=1200&q=80', 21.031800, 105.840300, 110, 5, 0),
  ('Ha Noi', '91000000-0000-4000-8000-000000000003', 'Truc Bach Lake Stay 02', 'Truc Bach Lake, Ba Dinh, Ha Noi, Vietnam', 'https://images.unsplash.com/photo-1512918728675-ed5a9ecdebfd?auto=format&fit=crop&w=1200&q=80', 21.043300, 105.839200, 70, 2, 2),
  ('Ha Noi', '91000000-0000-4000-8000-000000000003', 'Quang Ba Flower Market Stay 02', 'Quang Ba Flower Market, Tay Ho, Ha Noi, Vietnam', 'https://images.unsplash.com/photo-1560185127-6ed189bf02f4?auto=format&fit=crop&w=1200&q=80', 21.068900, 105.826900, 75, 3, 4),
  ('Ha Noi', '91000000-0000-4000-8000-000000000003', 'Aeon Mall Long Bien Stay 02', 'Aeon Mall Long Bien, Long Bien, Ha Noi, Vietnam', 'https://images.unsplash.com/photo-1598928506311-c55ded91a20c?auto=format&fit=crop&w=1200&q=80', 21.029200, 105.899500, 80, 4, 6),
  ('Ha Noi', '91000000-0000-4000-8000-000000000003', 'Vincom Ba Trieu Stay 02', 'Vincom Center Ba Trieu, Hai Ba Trung, Ha Noi, Vietnam', 'https://images.unsplash.com/photo-1600566753190-17f0baa2a6c3?auto=format&fit=crop&w=1200&q=80', 21.015100, 105.849800, 85, 5, 8),
  ('Ha Noi', '91000000-0000-4000-8000-000000000003', 'Hoan Kiem Lake Stay 03', 'Hoan Kiem Lake, Ha Noi, Vietnam', 'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1200&q=80', 21.025300, 105.854000, 90, 2, 0),
  ('Ha Noi', '91000000-0000-4000-8000-000000000003', 'Old Quarter Stay 03', 'Old Quarter, Hoan Kiem, Ha Noi, Vietnam', 'https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?auto=format&fit=crop&w=1200&q=80', 21.032100, 105.851700, 95, 3, 2),
  ('Ha Noi', '91000000-0000-4000-8000-000000000003', 'West Lake Stay 03', 'West Lake, Tay Ho, Ha Noi, Vietnam', 'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=1200&q=80', 21.058000, 105.820500, 100, 4, 4),
  ('Ha Noi', '91000000-0000-4000-8000-000000000003', 'Temple of Literature Stay 03', 'Temple of Literature, Dong Da, Ha Noi, Vietnam', 'https://images.unsplash.com/photo-1564013799919-ab600027ffc6?auto=format&fit=crop&w=1200&q=80', 21.029700, 105.837300, 105, 5, 6),
  ('Ha Noi', '91000000-0000-4000-8000-000000000003', 'Ho Chi Minh Mausoleum Stay 03', 'Ho Chi Minh Mausoleum, Ba Dinh, Ha Noi, Vietnam', 'https://images.unsplash.com/photo-1494526585095-c41746248156?auto=format&fit=crop&w=1200&q=80', 21.040100, 105.836300, 110, 2, 8),
  ('Ha Noi', '91000000-0000-4000-8000-000000000003', 'One Pillar Pagoda Stay 03', 'One Pillar Pagoda, Ba Dinh, Ha Noi, Vietnam', 'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=1200&q=80', 21.032500, 105.837000, 70, 3, 0),
  ('Ha Noi', '91000000-0000-4000-8000-000000000003', 'Hanoi Opera House Stay 03', 'Hanoi Opera House, Hoan Kiem, Ha Noi, Vietnam', 'https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=1200&q=80', 21.022600, 105.860400, 75, 4, 2),
  ('Ha Noi', '91000000-0000-4000-8000-000000000003', 'Long Bien Bridge Stay 03', 'Long Bien Bridge, Ha Noi, Vietnam', 'https://images.unsplash.com/photo-1570129477492-45c003edd2be?auto=format&fit=crop&w=1200&q=80', 21.042200, 105.861600, 80, 5, 4),
  ('Ha Noi', '91000000-0000-4000-8000-000000000003', 'St Joseph Cathedral Stay 03', 'St Joseph Cathedral, Hoan Kiem, Ha Noi, Vietnam', 'https://images.unsplash.com/photo-1512918728675-ed5a9ecdebfd?auto=format&fit=crop&w=1200&q=80', 21.030400, 105.852800, 85, 2, 6),
  ('Ha Noi', '91000000-0000-4000-8000-000000000003', 'Tran Quoc Pagoda Stay 03', 'Tran Quoc Pagoda, Tay Ho, Ha Noi, Vietnam', 'https://images.unsplash.com/photo-1560185127-6ed189bf02f4?auto=format&fit=crop&w=1200&q=80', 21.051300, 105.839400, 90, 3, 8),
  ('Ha Noi', '91000000-0000-4000-8000-000000000003', 'Vietnam Museum of Ethnology Stay 03', 'Vietnam Museum of Ethnology, Cau Giay, Ha Noi, Vietnam', 'https://images.unsplash.com/photo-1598928506311-c55ded91a20c?auto=format&fit=crop&w=1200&q=80', 21.037100, 105.794600, 95, 4, 0),
  ('Ha Noi', '91000000-0000-4000-8000-000000000003', 'Lotte Center Hanoi Stay 03', 'Lotte Center Hanoi, Ba Dinh, Ha Noi, Vietnam', 'https://images.unsplash.com/photo-1600566753190-17f0baa2a6c3?auto=format&fit=crop&w=1200&q=80', 21.030500, 105.809400, 100, 5, 2),
  ('Ha Noi', '91000000-0000-4000-8000-000000000003', 'Keangnam Landmark 72 Stay 03', 'Keangnam Landmark 72, Nam Tu Liem, Ha Noi, Vietnam', 'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1200&q=80', 21.016900, 105.779900, 105, 2, 4);

insert into public.destination_places (
  "_id", "destinationId", city, name, address, image, latitude, longitude, "createdAt", "updatedAt"
)
select gen_random_uuid(), destination_id, city, name, address, image, latitude, longitude,
       timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'
from public.seed_extra_places;

insert into public.cabins (
  "_id", name, "maxCapacity", "regularPrice", discount, image, description, location,
  latitude, longitude, "mapPlaceId", amenities, "hostId", "createdAt", "updatedAt"
)
select gen_random_uuid(), name, max_capacity, regular_price, discount, image,
       'A verified demo stay near ' || address || ' with map-ready location data.', address,
       latitude, longitude, null,
       case when city = 'Vung Tau' then 'WiFi, Parking, Air conditioning, Balcony'
            when city = 'Ha Noi' then 'WiFi, Breakfast, Air conditioning, Bathtub'
            else 'WiFi, Breakfast, Parking, Air conditioning' end,
       case when city = 'Ho Chi Minh City' then '10000000-0000-4000-8000-000000000002'::uuid
            when city = 'Vung Tau' then '10000000-0000-4000-8000-000000000003'::uuid
            else '10000000-0000-4000-8000-000000000004'::uuid end,
       timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'
from public.seed_extra_places;

insert into public.destination_places (
  "_id", "destinationId", city, name, address, image, latitude, longitude, "createdAt", "updatedAt"
)
select gen_random_uuid(),
       case when location ilike '%Ho Chi Minh%' then '91000000-0000-4000-8000-000000000001'::uuid
            when location ilike '%Vung Tau%' then '91000000-0000-4000-8000-000000000002'::uuid
            when location ilike '%Ha Noi%' then '91000000-0000-4000-8000-000000000003'::uuid
            else null end,
       case when location ilike '%Ho Chi Minh%' then 'Ho Chi Minh City'
            when location ilike '%Vung Tau%' then 'Vung Tau'
            when location ilike '%Ha Noi%' then 'Ha Noi'
            else 'Vietnam' end,
       name, location, image, latitude, longitude,
       timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'
from public.cabins
where latitude is not null
  and not exists (
    select 1 from public.destination_places p
    where p.city = case when public.cabins.location ilike '%Ho Chi Minh%' then 'Ho Chi Minh City'
                        when public.cabins.location ilike '%Vung Tau%' then 'Vung Tau'
                        when public.cabins.location ilike '%Ha Noi%' then 'Ha Noi'
                        else 'Vietnam' end
      and p.name = public.cabins.name
  );

-- booking policies
insert into public.booking_policies (
  "_id",
  "cabinId",
  "breakfastPrice",
  "miniBookingLength",
  "maxBookingLength",
  "createdAt",
  "updatedAt"
) values
  ('50000000-0000-4000-8000-000000000001', '20000000-0000-4000-8000-000000000001', 12, 1, 10, timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('50000000-0000-4000-8000-000000000002', '20000000-0000-4000-8000-000000000002', 14, 1, 14, timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('50000000-0000-4000-8000-000000000003', '20000000-0000-4000-8000-000000000003', 10, 1, 14, timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('50000000-0000-4000-8000-000000000004', '20000000-0000-4000-8000-000000000004', 15, 1, 14, timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('50000000-0000-4000-8000-000000000005', '20000000-0000-4000-8000-000000000005', 13, 1, 12, timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('50000000-0000-4000-8000-000000000006', '20000000-0000-4000-8000-000000000006', 12, 1, 14, timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('50000000-0000-4000-8000-000000000007', '20000000-0000-4000-8000-000000000007', 11, 1, 10, timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('50000000-0000-4000-8000-000000000008', '20000000-0000-4000-8000-000000000008', 12, 1, 14, timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('50000000-0000-4000-8000-000000000009', '20000000-0000-4000-8000-000000000009', 10, 1, 10, timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('50000000-0000-4000-8000-000000000010', '20000000-0000-4000-8000-000000000010', 12, 1, 14, timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('50000000-0000-4000-8000-000000000011', '20000000-0000-4000-8000-000000000011', 11, 1, 12, timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('50000000-0000-4000-8000-000000000012', '20000000-0000-4000-8000-000000000012', 12, 1, 14, timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00');

insert into public.booking_policies (
  "_id", "cabinId", "breakfastPrice", "miniBookingLength", "maxBookingLength", "createdAt", "updatedAt"
)
select gen_random_uuid(), c."_id", 12, 1, 14,
       timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'
from public.cabins c
where not exists (
  select 1 from public.booking_policies p where p."cabinId" = c."_id"
);

-- cabin amenities
insert into public.cabin_amenities (
  "_id",
  "cabinId",
  "amenityId",
  "createdAt"
) values
  ('61000000-0000-4000-8000-000000000001', '20000000-0000-4000-8000-000000000001', '40000000-0000-4000-8000-000000000001', timestamp '2026-07-19 08:00:00'),
  ('61000000-0000-4000-8000-000000000002', '20000000-0000-4000-8000-000000000001', '40000000-0000-4000-8000-000000000003', timestamp '2026-07-19 08:00:00'),
  ('61000000-0000-4000-8000-000000000003', '20000000-0000-4000-8000-000000000001', '40000000-0000-4000-8000-000000000009', timestamp '2026-07-19 08:00:00'),
  ('61000000-0000-4000-8000-000000000004', '20000000-0000-4000-8000-000000000001', '40000000-0000-4000-8000-000000000010', timestamp '2026-07-19 08:00:00'),
  ('61000000-0000-4000-8000-000000000005', '20000000-0000-4000-8000-000000000002', '40000000-0000-4000-8000-000000000001', timestamp '2026-07-19 08:00:00'),
  ('61000000-0000-4000-8000-000000000006', '20000000-0000-4000-8000-000000000002', '40000000-0000-4000-8000-000000000002', timestamp '2026-07-19 08:00:00'),
  ('61000000-0000-4000-8000-000000000007', '20000000-0000-4000-8000-000000000002', '40000000-0000-4000-8000-000000000003', timestamp '2026-07-19 08:00:00'),
  ('61000000-0000-4000-8000-000000000008', '20000000-0000-4000-8000-000000000002', '40000000-0000-4000-8000-000000000008', timestamp '2026-07-19 08:00:00'),
  ('61000000-0000-4000-8000-000000000009', '20000000-0000-4000-8000-000000000003', '40000000-0000-4000-8000-000000000001', timestamp '2026-07-19 08:00:00'),
  ('61000000-0000-4000-8000-000000000010', '20000000-0000-4000-8000-000000000003', '40000000-0000-4000-8000-000000000004', timestamp '2026-07-19 08:00:00'),
  ('61000000-0000-4000-8000-000000000011', '20000000-0000-4000-8000-000000000003', '40000000-0000-4000-8000-000000000007', timestamp '2026-07-19 08:00:00'),
  ('61000000-0000-4000-8000-000000000012', '20000000-0000-4000-8000-000000000003', '40000000-0000-4000-8000-000000000010', timestamp '2026-07-19 08:00:00'),
  ('61000000-0000-4000-8000-000000000013', '20000000-0000-4000-8000-000000000004', '40000000-0000-4000-8000-000000000001', timestamp '2026-07-19 08:00:00'),
  ('61000000-0000-4000-8000-000000000014', '20000000-0000-4000-8000-000000000004', '40000000-0000-4000-8000-000000000002', timestamp '2026-07-19 08:00:00'),
  ('61000000-0000-4000-8000-000000000015', '20000000-0000-4000-8000-000000000004', '40000000-0000-4000-8000-000000000003', timestamp '2026-07-19 08:00:00'),
  ('61000000-0000-4000-8000-000000000016', '20000000-0000-4000-8000-000000000004', '40000000-0000-4000-8000-000000000009', timestamp '2026-07-19 08:00:00'),
  ('61000000-0000-4000-8000-000000000017', '20000000-0000-4000-8000-000000000005', '40000000-0000-4000-8000-000000000001', timestamp '2026-07-19 08:00:00'),
  ('61000000-0000-4000-8000-000000000018', '20000000-0000-4000-8000-000000000005', '40000000-0000-4000-8000-000000000003', timestamp '2026-07-19 08:00:00'),
  ('61000000-0000-4000-8000-000000000019', '20000000-0000-4000-8000-000000000005', '40000000-0000-4000-8000-000000000006', timestamp '2026-07-19 08:00:00'),
  ('61000000-0000-4000-8000-000000000020', '20000000-0000-4000-8000-000000000005', '40000000-0000-4000-8000-000000000010', timestamp '2026-07-19 08:00:00'),
  ('61000000-0000-4000-8000-000000000021', '20000000-0000-4000-8000-000000000006', '40000000-0000-4000-8000-000000000001', timestamp '2026-07-19 08:00:00'),
  ('61000000-0000-4000-8000-000000000022', '20000000-0000-4000-8000-000000000006', '40000000-0000-4000-8000-000000000002', timestamp '2026-07-19 08:00:00'),
  ('61000000-0000-4000-8000-000000000023', '20000000-0000-4000-8000-000000000006', '40000000-0000-4000-8000-000000000005', timestamp '2026-07-19 08:00:00'),
  ('61000000-0000-4000-8000-000000000024', '20000000-0000-4000-8000-000000000006', '40000000-0000-4000-8000-000000000009', timestamp '2026-07-19 08:00:00'),
  ('61000000-0000-4000-8000-000000000025', '20000000-0000-4000-8000-000000000007', '40000000-0000-4000-8000-000000000001', timestamp '2026-07-19 08:00:00'),
  ('61000000-0000-4000-8000-000000000026', '20000000-0000-4000-8000-000000000007', '40000000-0000-4000-8000-000000000003', timestamp '2026-07-19 08:00:00'),
  ('61000000-0000-4000-8000-000000000027', '20000000-0000-4000-8000-000000000007', '40000000-0000-4000-8000-000000000010', timestamp '2026-07-19 08:00:00'),
  ('61000000-0000-4000-8000-000000000028', '20000000-0000-4000-8000-000000000007', '40000000-0000-4000-8000-000000000008', timestamp '2026-07-19 08:00:00'),
  ('61000000-0000-4000-8000-000000000029', '20000000-0000-4000-8000-000000000008', '40000000-0000-4000-8000-000000000001', timestamp '2026-07-19 08:00:00'),
  ('61000000-0000-4000-8000-000000000030', '20000000-0000-4000-8000-000000000008', '40000000-0000-4000-8000-000000000002', timestamp '2026-07-19 08:00:00'),
  ('61000000-0000-4000-8000-000000000031', '20000000-0000-4000-8000-000000000008', '40000000-0000-4000-8000-000000000006', timestamp '2026-07-19 08:00:00'),
  ('61000000-0000-4000-8000-000000000032', '20000000-0000-4000-8000-000000000008', '40000000-0000-4000-8000-000000000010', timestamp '2026-07-19 08:00:00');

-- images
insert into public.images (
  "_id",
  "cabinId",
  "imageUrl",
  name,
  "isCover",
  "createdAt"
) values
  ('60000000-0000-4000-8000-000000000001', '20000000-0000-4000-8000-000000000001', 'https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?auto=format&fit=crop&w=1200&q=80', 'Cover', true, timestamp '2026-07-19 08:00:00'),
  ('60000000-0000-4000-8000-000000000002', '20000000-0000-4000-8000-000000000001', 'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=1200&q=80', 'Living room', false, timestamp '2026-07-19 08:00:00'),
  ('60000000-0000-4000-8000-000000000003', '20000000-0000-4000-8000-000000000002', 'https://images.unsplash.com/photo-1564013799919-ab600027ffc6?auto=format&fit=crop&w=1200&q=80', 'Cover', true, timestamp '2026-07-19 08:00:00'),
  ('60000000-0000-4000-8000-000000000004', '20000000-0000-4000-8000-000000000002', 'https://images.unsplash.com/photo-1494526585095-c41746248156?auto=format&fit=crop&w=1200&q=80', 'Bedroom', false, timestamp '2026-07-19 08:00:00'),
  ('60000000-0000-4000-8000-000000000005', '20000000-0000-4000-8000-000000000003', 'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=1200&q=80', 'Cover', true, timestamp '2026-07-19 08:00:00'),
  ('60000000-0000-4000-8000-000000000006', '20000000-0000-4000-8000-000000000003', 'https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=1200&q=80', 'Interior', false, timestamp '2026-07-19 08:00:00'),
  ('60000000-0000-4000-8000-000000000007', '20000000-0000-4000-8000-000000000004', 'https://images.unsplash.com/photo-1570129477492-45c003edd2be?auto=format&fit=crop&w=1200&q=80', 'Cover', true, timestamp '2026-07-19 08:00:00'),
  ('60000000-0000-4000-8000-000000000008', '20000000-0000-4000-8000-000000000004', 'https://images.unsplash.com/photo-1512918728675-ed5a9ecdebfd?auto=format&fit=crop&w=1200&q=80', 'Bedroom', false, timestamp '2026-07-19 08:00:00'),
  ('60000000-0000-4000-8000-000000000009', '20000000-0000-4000-8000-000000000005', 'https://images.unsplash.com/photo-1560185127-6ed189bf02f4?auto=format&fit=crop&w=1200&q=80', 'Cover', true, timestamp '2026-07-19 08:00:00'),
  ('60000000-0000-4000-8000-000000000010', '20000000-0000-4000-8000-000000000005', 'https://images.unsplash.com/photo-1598928506311-c55ded91a20c?auto=format&fit=crop&w=1200&q=80', 'Bedroom', false, timestamp '2026-07-19 08:00:00'),
  ('60000000-0000-4000-8000-000000000011', '20000000-0000-4000-8000-000000000006', 'https://images.unsplash.com/photo-1600566753190-17f0baa2a6c3?auto=format&fit=crop&w=1200&q=80', 'Cover', true, timestamp '2026-07-19 08:00:00'),
  ('60000000-0000-4000-8000-000000000012', '20000000-0000-4000-8000-000000000006', 'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1200&q=80', 'Lounge', false, timestamp '2026-07-19 08:00:00'),
  ('60000000-0000-4000-8000-000000000013', '20000000-0000-4000-8000-000000000007', 'https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?auto=format&fit=crop&w=1200&q=80', 'Cover', true, timestamp '2026-07-19 08:00:00'),
  ('60000000-0000-4000-8000-000000000014', '20000000-0000-4000-8000-000000000007', 'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=1200&q=80', 'Terrace', false, timestamp '2026-07-19 08:00:00'),
  ('60000000-0000-4000-8000-000000000015', '20000000-0000-4000-8000-000000000008', 'https://images.unsplash.com/photo-1564013799919-ab600027ffc6?auto=format&fit=crop&w=1200&q=80', 'Cover', true, timestamp '2026-07-19 08:00:00'),
  ('60000000-0000-4000-8000-000000000016', '20000000-0000-4000-8000-000000000008', 'https://images.unsplash.com/photo-1494526585095-c41746248156?auto=format&fit=crop&w=1200&q=80', 'Balcony', false, timestamp '2026-07-19 08:00:00'),
  ('60000000-0000-4000-8000-000000000017', '20000000-0000-4000-8000-000000000009', 'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=1200&q=80', 'Cover', true, timestamp '2026-07-19 08:00:00'),
  ('60000000-0000-4000-8000-000000000018', '20000000-0000-4000-8000-000000000010', 'https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=1200&q=80', 'Cover', true, timestamp '2026-07-19 08:00:00'),
  ('60000000-0000-4000-8000-000000000019', '20000000-0000-4000-8000-000000000011', 'https://images.unsplash.com/photo-1570129477492-45c003edd2be?auto=format&fit=crop&w=1200&q=80', 'Cover', true, timestamp '2026-07-19 08:00:00'),
  ('60000000-0000-4000-8000-000000000020', '20000000-0000-4000-8000-000000000012', 'https://images.unsplash.com/photo-1512918728675-ed5a9ecdebfd?auto=format&fit=crop&w=1200&q=80', 'Cover', true, timestamp '2026-07-19 08:00:00');

insert into public.images (
  "_id", "cabinId", "imageUrl", name, "isCover", "createdAt"
)
select gen_random_uuid(), c."_id", c.image, 'Cover', true, timestamp '2026-07-19 08:00:00'
from public.cabins c
where not exists (
  select 1 from public.images i where i."cabinId" = c."_id" and i."isCover" = true
);

-- coupons
insert into public.coupons (
  "_id",
  code,
  description,
  "discountType",
  "discountValue",
  "maxDiscountAmount",
  "minBookingAmount",
  "startDate",
  "endDate",
  "usageLimit",
  "usedCount",
  "isActive",
  "createdAt",
  "updatedAt"
) values
  ('70000000-0000-4000-8000-000000000001', 'WELCOME10', 'Ten percent off for first bookings', 'percent', 10, 50, 120, date '2026-07-01', date '2026-12-31', 100, 12, true, timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('70000000-0000-4000-8000-000000000002', 'FIXED25', 'Flat discount for bigger stays', 'fixed', 25, 25, 200, date '2026-07-01', date '2026-11-30', 60, 7, true, timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('70000000-0000-4000-8000-000000000003', 'EXPIRED15', 'Expired coupon used for testing', 'percent', 15, 100, 80, date '2026-05-01', date '2026-06-30', 25, 25, false, timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00');

-- promotions
insert into public.promotions (
  "_id",
  "cabinId",
  "discountPercent",
  "startDate",
  "endDate",
  "isActive",
  "createdAt",
  "updatedAt"
) values
  ('71000000-0000-4000-8000-000000000001', '20000000-0000-4000-8000-000000000001', 10, date '2026-07-01', date '2026-09-30', true, timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('71000000-0000-4000-8000-000000000002', '20000000-0000-4000-8000-000000000002', 15, date '2026-07-05', date '2026-08-31', true, timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('71000000-0000-4000-8000-000000000003', '20000000-0000-4000-8000-000000000005', 12, date '2026-07-10', date '2026-08-20', true, timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('71000000-0000-4000-8000-000000000004', '20000000-0000-4000-8000-000000000007', 8, date '2026-07-19', date '2026-10-01', true, timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00');

-- blocked dates
insert into public.blocked_dates (
  "_id",
  "cabinId",
  "hostId",
  "startDate",
  "endDate",
  reason,
  "createdAt",
  "updatedAt"
) values
  ('88000000-0000-4000-8000-000000000001', '20000000-0000-4000-8000-000000000004', '10000000-0000-4000-8000-000000000003', date '2026-08-12', date '2026-08-15', 'Host maintenance', timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('88000000-0000-4000-8000-000000000002', '20000000-0000-4000-8000-000000000005', '10000000-0000-4000-8000-000000000004', date '2026-07-25', date '2026-07-28', 'Private event', timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('88000000-0000-4000-8000-000000000003', '20000000-0000-4000-8000-000000000008', '10000000-0000-4000-8000-000000000004', date '2026-08-18', date '2026-08-19', 'Deep cleaning', timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00');

-- bookings
insert into public.bookings (
  "_id",
  "userId",
  "cabinId",
  "startDate",
  "endDate",
  "numNights",
  "numGuests",
  "cabinPrice",
  "extrasPrice",
  "totalPrice",
  status,
  "hasBreakfast",
  "isPaid",
  observations,
  "couponId",
  "discountAmount",
  "createdAt",
  "updatedAt"
) values
  ('80000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000101', '20000000-0000-4000-8000-000000000001', date '2026-08-04', date '2026-08-07', 3, 2, 324.00, 72.00, 396.00, 'pending', true, false, 'Anniversary trip', null, 0.00, timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('80000000-0000-4000-8000-000000000002', '10000000-0000-4000-8000-000000000102', '20000000-0000-4000-8000-000000000002', date '2026-08-10', date '2026-08-13', 3, 2, 405.00, 0.00, 405.00, 'confirmed', false, false, 'Late arrival', null, 0.00, timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('80000000-0000-4000-8000-000000000003', '10000000-0000-4000-8000-000000000103', '20000000-0000-4000-8000-000000000004', date '2026-07-18', date '2026-07-21', 3, 4, 480.00, 180.00, 660.00, 'checked-in', true, true, 'Family mountain trip', null, 0.00, timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('80000000-0000-4000-8000-000000000004', '10000000-0000-4000-8000-000000000104', '20000000-0000-4000-8000-000000000003', date '2026-07-01', date '2026-07-04', 3, 2, 285.00, 0.00, 285.00, 'checked-out', false, true, 'Quiet river stay', null, 0.00, timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('80000000-0000-4000-8000-000000000005', '10000000-0000-4000-8000-000000000105', '20000000-0000-4000-8000-000000000005', date '2026-06-15', date '2026-06-18', 3, 3, 444.00, 0.00, 444.00, 'cancelled', false, false, 'Cancelled due to weather', null, 0.00, timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('80000000-0000-4000-8000-000000000006', '10000000-0000-4000-8000-000000000106', '20000000-0000-4000-8000-000000000006', date '2026-07-08', date '2026-07-10', 2, 2, 244.00, 0.00, 219.60, 'checked-out', false, true, 'Need quiet room', '70000000-0000-4000-8000-000000000001', 24.40, timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('80000000-0000-4000-8000-000000000007', '10000000-0000-4000-8000-000000000101', '20000000-0000-4000-8000-000000000007', date '2026-08-20', date '2026-08-23', 3, 2, 315.00, 0.00, 315.00, 'pending', false, false, 'Work retreat', null, 0.00, timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('80000000-0000-4000-8000-000000000008', '10000000-0000-4000-8000-000000000102', '20000000-0000-4000-8000-000000000008', date '2026-08-02', date '2026-08-04', 2, 5, 280.00, 120.00, 400.00, 'confirmed', true, true, 'Family beach break', null, 0.00, timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00');

-- payments
insert into public.payments (
  "_id",
  "bookingId",
  "userId",
  amount,
  method,
  provider,
  "transactionId",
  status,
  "paidAt",
  "createdAt",
  "updatedAt"
) values
  ('81000000-0000-4000-8000-000000000001', '80000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000101', 396.00, 'app', 'mock', 'TXN-0001', 'pending', null, timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('81000000-0000-4000-8000-000000000002', '80000000-0000-4000-8000-000000000002', '10000000-0000-4000-8000-000000000102', 405.00, 'app', 'mock', 'TXN-0002', 'pending', null, timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('81000000-0000-4000-8000-000000000003', '80000000-0000-4000-8000-000000000003', '10000000-0000-4000-8000-000000000103', 660.00, 'card', 'stripe', 'TXN-0003', 'paid', timestamp '2026-07-21 09:00:00', timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('81000000-0000-4000-8000-000000000004', '80000000-0000-4000-8000-000000000004', '10000000-0000-4000-8000-000000000104', 285.00, 'card', 'stripe', 'TXN-0004', 'paid', timestamp '2026-07-04 09:00:00', timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('81000000-0000-4000-8000-000000000005', '80000000-0000-4000-8000-000000000006', '10000000-0000-4000-8000-000000000106', 219.60, 'card', 'stripe', 'TXN-0005', 'paid', timestamp '2026-07-10 09:00:00', timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('81000000-0000-4000-8000-000000000006', '80000000-0000-4000-8000-000000000007', '10000000-0000-4000-8000-000000000101', 315.00, 'app', 'mock', 'TXN-0006', 'pending', null, timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('81000000-0000-4000-8000-000000000007', '80000000-0000-4000-8000-000000000008', '10000000-0000-4000-8000-000000000102', 400.00, 'card', 'stripe', 'TXN-0007', 'paid', timestamp '2026-08-04 10:00:00', timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00');

-- rates
insert into public.rates (
  "_id",
  "userId",
  "cabinId",
  "bookingId",
  rating,
  comment,
  "createdAt",
  "updatedAt"
) values
  ('82000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000104', '20000000-0000-4000-8000-000000000003', '80000000-0000-4000-8000-000000000004', 5, 'Quiet river stay with kind service.', timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('82000000-0000-4000-8000-000000000002', '10000000-0000-4000-8000-000000000106', '20000000-0000-4000-8000-000000000006', '80000000-0000-4000-8000-000000000006', 4, 'Great balcony and easy check-in.', timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('82000000-0000-4000-8000-000000000003', '10000000-0000-4000-8000-000000000103', '20000000-0000-4000-8000-000000000004', '80000000-0000-4000-8000-000000000003', 4, 'Comfortable mountain cabin for a bigger group.', timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('82000000-0000-4000-8000-000000000004', '10000000-0000-4000-8000-000000000102', '20000000-0000-4000-8000-000000000008', '80000000-0000-4000-8000-000000000008', 5, 'Sunset view and pool area were excellent.', timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00');

-- wishlists
insert into public.wishlists (
  "_id",
  "userId",
  "cabinId",
  "createdAt"
) values
  ('83000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000101', '20000000-0000-4000-8000-000000000001', timestamp '2026-07-19 08:00:00'),
  ('83000000-0000-4000-8000-000000000002', '10000000-0000-4000-8000-000000000101', '20000000-0000-4000-8000-000000000005', timestamp '2026-07-19 08:00:00'),
  ('83000000-0000-4000-8000-000000000003', '10000000-0000-4000-8000-000000000102', '20000000-0000-4000-8000-000000000002', timestamp '2026-07-19 08:00:00'),
  ('83000000-0000-4000-8000-000000000004', '10000000-0000-4000-8000-000000000103', '20000000-0000-4000-8000-000000000004', timestamp '2026-07-19 08:00:00'),
  ('83000000-0000-4000-8000-000000000005', '10000000-0000-4000-8000-000000000104', '20000000-0000-4000-8000-000000000007', timestamp '2026-07-19 08:00:00');

-- conversations
insert into public.conversations (
  "_id",
  "guestId",
  "hostId",
  "cabinId",
  "bookingId",
  "createdAt",
  "updatedAt"
) values
  ('84000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000101', '10000000-0000-4000-8000-000000000002', '20000000-0000-4000-8000-000000000001', '80000000-0000-4000-8000-000000000001', timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00'),
  ('84000000-0000-4000-8000-000000000002', '10000000-0000-4000-8000-000000000102', '10000000-0000-4000-8000-000000000003', '20000000-0000-4000-8000-000000000002', '80000000-0000-4000-8000-000000000002', timestamp '2026-07-19 08:00:00', timestamp '2026-07-19 08:00:00');

-- messages
insert into public.messages (
  "_id",
  "conversationId",
  "senderId",
  message,
  "isRead",
  "createdAt"
) values
  ('85000000-0000-4000-8000-000000000001', '84000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000101', 'Hi, is the cabin still available for the first week of August?', true, timestamp '2026-07-19 08:00:00'),
  ('85000000-0000-4000-8000-000000000002', '84000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000002', 'Yes, the dates are still open and the cabin is ready.', true, timestamp '2026-07-19 08:00:00'),
  ('85000000-0000-4000-8000-000000000003', '84000000-0000-4000-8000-000000000002', '10000000-0000-4000-8000-000000000102', 'Does this loft have breakfast included?', false, timestamp '2026-07-19 08:00:00'),
  ('85000000-0000-4000-8000-000000000004', '84000000-0000-4000-8000-000000000002', '10000000-0000-4000-8000-000000000003', 'Breakfast can be added during checkout.', false, timestamp '2026-07-19 08:00:00');

-- notifications
insert into public.notifications (
  "_id",
  title,
  "userId",
  "isRead",
  data,
  "createdAt",
  type,
  message
) values
  ('86000000-0000-4000-8000-000000000001', 'Booking pending', '10000000-0000-4000-8000-000000000101', false, '{"bookingId":"80000000-0000-4000-8000-000000000001","status":"pending"}'::jsonb, timestamp '2026-07-19 08:00:00', 'booking', 'Your booking is waiting for host confirmation.'),
  ('86000000-0000-4000-8000-000000000002', 'Payment reminder', '10000000-0000-4000-8000-000000000102', false, '{"bookingId":"80000000-0000-4000-8000-000000000002","status":"confirmed"}'::jsonb, timestamp '2026-07-19 08:00:00', 'payment', 'Please finish the payment for your confirmed stay.'),
  ('86000000-0000-4000-8000-000000000003', 'New review', '10000000-0000-4000-8000-000000000003', true, '{"bookingId":"80000000-0000-4000-8000-000000000004"}'::jsonb, timestamp '2026-07-19 08:00:00', 'review', 'You received a new review on Landmark 81 Skyline Suite.'),
  ('86000000-0000-4000-8000-000000000004', 'Favorite deal', '10000000-0000-4000-8000-000000000104', false, '{"cabinId":"20000000-0000-4000-8000-000000000007"}'::jsonb, timestamp '2026-07-19 08:00:00', 'promotion', 'Tan Dinh Pink House now has an active discount.');

-- otps
insert into public.otps (
  "_id",
  email,
  otp,
  "expiresAt",
  "userId",
  "createdAt"
) values
  ('87000000-0000-4000-8000-000000000001', 'alice.nguyen@sdp1.test', '482159', timestamp '2026-07-19 08:20:00', '10000000-0000-4000-8000-000000000101', timestamp '2026-07-19 08:00:00'),
  ('87000000-0000-4000-8000-000000000002', 'support@sereinstay.test', '119844', timestamp '2026-07-19 07:30:00', '10000000-0000-4000-8000-000000000001', timestamp '2026-07-19 08:00:00');

commit;
