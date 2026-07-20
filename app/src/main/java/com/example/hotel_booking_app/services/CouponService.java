package com.example.hotel_booking_app.services;

import com.example.hotel_booking_app.data.models.Coupon;
import com.example.hotel_booking_app.data.remote.SupabaseCallback;
import com.example.hotel_booking_app.data.remote.SupabaseClient;
import com.example.hotel_booking_app.utils.AppConstants;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CouponService {
    private final SupabaseClient supabaseClient;

    public CouponService() {
        supabaseClient = SupabaseClient.getInstance();
    }

    public void getCouponByCode(String code, SupabaseCallback<Coupon> callback) {
        String normalizedCode = code.trim().toUpperCase(Locale.US);
        getCoupons(new SupabaseCallback<List<Coupon>>() {
            @Override
            public void onSuccess(List<Coupon> coupons) {
                for (Coupon coupon : coupons) {
                    if (coupon.getCode() != null && coupon.getCode().trim().equalsIgnoreCase(normalizedCode)) {
                        callback.onSuccess(coupon);
                        return;
                    }
                }
                callback.onError("Không tìm thấy mã: " + normalizedCode + ".");
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    public void getCoupons(SupabaseCallback<List<Coupon>> callback) {
        supabaseClient.getList(AppConstants.TABLE_COUPONS, "*", null, "createdAt.desc", null, Coupon[].class, callback);
    }

    public double calculateDiscount(Coupon coupon, double bookingAmount) {
        if (!isUsable(coupon, bookingAmount)) {
            return 0;
        }

        double discount;
        if (AppConstants.COUPON_FIXED.equals(coupon.getDiscountType())) {
            discount = coupon.getDiscountValue();
        } else {
            discount = bookingAmount * coupon.getDiscountValue() / 100.0;
        }

        if (coupon.getMaxDiscountAmount() != null) {
            discount = Math.min(discount, coupon.getMaxDiscountAmount());
        }
        return Math.min(discount, bookingAmount);
    }

    public void validateCoupon(Coupon coupon, double bookingAmount, SupabaseCallback<Double> callback) {
        String validationError = getValidationError(coupon, bookingAmount);
        if (validationError != null) {
            callback.onError(validationError);
            return;
        }

        double discount = calculateDiscount(coupon, bookingAmount);
        if (discount <= 0) {
            callback.onError("Mã giảm giá không tạo ra giá trị giảm.");
            return;
        }
        callback.onSuccess(discount);
    }

    public void incrementUsedCount(Coupon coupon, SupabaseCallback<Coupon> callback) {
        if (coupon == null) {
            callback.onError("Không có mã giảm giá để cập nhật lượt dùng.");
            return;
        }

        Map<String, String> filters = new HashMap<>();
        filters.put("_id", coupon.getId());

        Map<String, Object> payload = new HashMap<>();
        payload.put("usedCount", coupon.getUsedCount() + 1);
        supabaseClient.update(AppConstants.TABLE_COUPONS, filters, payload, Coupon[].class, callback);
    }

    public boolean isUsable(Coupon coupon, double bookingAmount) {
        return getValidationError(coupon, bookingAmount) == null;
    }

    private String getValidationError(Coupon coupon, double bookingAmount) {
        if (coupon == null) {
            return "Mã giảm giá không tồn tại.";
        }
        if (!coupon.isActive()) {
            return "Mã giảm giá chưa kích hoạt hoặc đã bị tắt.";
        }
        if (bookingAmount < coupon.getMinBookingAmount()) {
            return "Đặt phòng chưa đạt giá trị tối thiểu " + coupon.getMinBookingAmount() + ".";
        }
        if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            return "Mã giảm giá đã hết lượt sử dụng.";
        }

        LocalDate today = LocalDate.now();
        if (coupon.getStartDate() != null && today.isBefore(LocalDate.parse(coupon.getStartDate()))) {
            return "Mã giảm giá chưa bắt đầu.";
        }
        if (coupon.getEndDate() != null && today.isAfter(LocalDate.parse(coupon.getEndDate()))) {
            return "Mã giảm giá đã hết hạn.";
        }
        return null;
    }
}
