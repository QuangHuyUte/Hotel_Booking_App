# UI optimization notes

## Research references

- Booking.com GitHub organization: open-source overview and engineering projects, not a direct mobile UI template.
- `gaurav7ingh/Booking.com`: Booking.com-style clone with destination search, filters, reviews, and checkout pages.
- `rahulgrover99/booking.com-lld`: backend-oriented challenge with hotel listing/search ideas, including location-based search.

## Features added in this pass

- Reworked bottom navigation labels to `About`, `Search`, and `Profile`.
- Added `Book now` CTA on About/Home that opens the Search/Cabins flow.
- Renamed Cabins tab UI to Search.
- Added structured search fields:
  - destination/cabin name
  - check-in date
  - check-out date
  - guest count
  - free-text amenity/deal/max-price search
  - sort before or after date search
- Added DatePicker UI for check-in/check-out.
- Added availability filtering after selecting date range, using existing `BookingService.ensureRangeIsAvailable`.
- Search panel auto-hides when scrolling down the cabin list and appears again when scrolling up/top.
- Cabin cards now show image, location, amenities, guests, and price in a more booking-style layout.
- Cabin detail amenities render as separate chips/items instead of one long text row.
- Cabin detail includes `View on map`, using Android geo intent with the current location text.
- Profile screen redesigned with a booking-style header, avatar initials, and separated info rows.
- Back buttons now use a smoother chevron icon with a softer circular background.
- Booking history now prioritizes pending bookings, counts pending items in the status line, and shows contextual actions on unpaid/pending cards.

## Main functions/classes changed

- `HomeActivity.onCreate`: wires `Book now` to the Search flow.
- `CabinListActivity.onCreate`: binds structured search controls and RecyclerView scroll behavior.
- `CabinListActivity.setupDateInputs`, `showDatePicker`: date-picking UX.
- `CabinListActivity.renderCabins`: combines structured filters, text search, sorting, and date availability.
- `CabinListActivity.applyAvailabilityFilter`: checks selected date range against existing bookings/blocked dates.
- `CabinAdapter.onBindViewHolder`: renders richer cabin cards.
- `CabinDetailActivity.loadAmenities`, `renderAmenities`: renders amenities as individual items.
- `CabinDetailActivity.openMap`: opens Google Maps/installed map app via geo intent.
- `ProfileActivity.renderProfile`: fills the new structured profile layout.
- `MyBookingsActivity.renderBookings`: prioritizes and counts pending bookings.
- `BookingAdapter.contextualActionText`: adds action text for pending/unpaid bookings.
- `BookingDetailActivity.renderPaymentState`: makes pending state clearer.

## Google Maps definition

Current implementation is lightweight:

- Uses `geo:0,0?q=<location>` intent.
- Does not require a Google Maps SDK key.
- Works when the device/emulator has a maps app installed.

For production Google Maps inside the app:

- Add `latitude` and `longitude` to `cabins`.
- Add Google Maps SDK dependency.
- Store API key outside source control.
- Render a `MapView` or `SupportMapFragment` on Cabin Detail.

## Forgot password / email logic definition

Current app already calls Supabase Auth recovery through `SupabaseAuthClient.recoverPassword`.

Recommended production flow:

- Keep password reset in Supabase Auth, not in the custom `users.password` column.
- Configure Supabase Auth email templates and redirect URL.
- Add a `password_reset_requests` audit table only if the team needs admin visibility.
- Avoid storing raw OTP/password reset codes in app database unless absolutely required.

## Database recommendations

See `database/database.sql` for a full create-new script and incremental helper columns/indexes.
