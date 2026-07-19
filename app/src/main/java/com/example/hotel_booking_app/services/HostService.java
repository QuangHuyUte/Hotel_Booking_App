package com.example.hotel_booking_app.services;

import com.example.hotel_booking_app.data.models.Booking;
import com.example.hotel_booking_app.data.models.Cabin;
import com.example.hotel_booking_app.data.remote.SupabaseCallback;

import java.util.List;

public class HostService {
    private final CabinService cabinService;
    private final BookingService bookingService;

    public HostService() {
        cabinService = new CabinService();
        bookingService = new BookingService();
    }

    public void getCabins(SupabaseCallback<List<Cabin>> callback) {
        cabinService.getCabins(callback);
    }

    public void getCabinsForHost(String hostId, SupabaseCallback<List<Cabin>> callback) {
        cabinService.getCabinsForHost(hostId, callback);
    }

    public void createCabin(Cabin cabin, SupabaseCallback<Cabin> callback) {
        cabinService.createCabin(cabin, callback);
    }

    public void updateCabin(Cabin cabin, SupabaseCallback<Cabin> callback) {
        cabinService.updateCabin(cabin, callback);
    }

    public void deleteCabin(String cabinId, SupabaseCallback<Boolean> callback) {
        cabinService.deleteCabin(cabinId, callback);
    }

    public void getBookings(SupabaseCallback<List<Booking>> callback) {
        bookingService.getAllBookings(callback);
    }

    public void getBookingsForCabin(String cabinId, SupabaseCallback<List<Booking>> callback) {
        bookingService.getBookingsForCabin(cabinId, callback);
    }

    public void updateBookingStatus(String bookingId, String status, boolean isPaid, SupabaseCallback<Booking> callback) {
        bookingService.updateStatus(bookingId, status, isPaid, callback);
    }
}
