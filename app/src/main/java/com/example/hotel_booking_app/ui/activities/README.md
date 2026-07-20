# Activity Screen Flow

This folder uses screen-purpose names so the navigation flow is easier to read.

## Entry and Auth

- `SplashActivity` starts the app and routes users by role.
- `SignInActivity`, `SignUpActivity`, and `PasswordResetActivity` handle account access.
- `OAuthBrowserActivity` handles web-based auth redirects.

## Guest Booking Flow

- `HotelSearchActivity` is the main hotel browsing and filtering screen.
- `HotelMapActivity` shows hotel price markers on the map and keeps the selected hotel card in sync.
- `HotelDetailActivity` shows one hotel, favorite state, map preview, and booking actions.
- `BookingCreateActivity` creates a booking request for a selected hotel.
- `BookingPaymentActivity` handles checkout and payment.
- `BookingDetailsActivity`, `BookingEditActivity`, and `BookingInvoiceActivity` show booking follow-up screens.

## Guest Account Flow

- `AccountHubActivity` is the personal/account menu.
- `GuestBookingsActivity` shows guest bookings.
- `SavedHotelsActivity` shows favorite hotels.
- `PaymentHistoryActivity` shows previous payments.
- `ProfileDetailsActivity` and `EditProfileActivity` handle profile viewing and editing.
- `NotificationCenterActivity` shows notifications.

## Chat Flow

- `ConversationListActivity` shows all conversations.
- `ChatThreadActivity` opens one conversation thread.

## Host and Admin Flow

- `HostHotelDashboardActivity` shows host-owned hotels and host actions.
- `AdminHotelFormActivity` creates or edits a hotel.
- `AdminBookingManagementActivity` manages bookings as host/admin.
- `AdminAppSettingsActivity` manages app settings.

## Compatibility Redirect

- `HomeRedirectActivity` is a lightweight redirect kept for older navigation paths.
