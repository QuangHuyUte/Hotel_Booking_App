package com.example.hotel_booking_app.utils;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class DateUtils {
    private DateUtils() {
    }

    public static int nightsBetween(String startDate, String endDate) {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        return (int) ChronoUnit.DAYS.between(start, end);
    }

    public static boolean isDateRangeValid(String startDate, String endDate) {
        try {
            return nightsBetween(startDate, endDate) > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
