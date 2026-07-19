package com.example.hotel_booking_app.data.remote;

public interface SupabaseCallback<T> {
    void onSuccess(T data);

    void onError(String message);
}
