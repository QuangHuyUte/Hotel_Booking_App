package com.example.hotel_booking_app.ui.helpers;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.hotel_booking_app.R;
import com.example.hotel_booking_app.ui.activities.AccountHubActivity;
import com.example.hotel_booking_app.ui.activities.ConversationListActivity;
import com.example.hotel_booking_app.ui.activities.GuestBookingsActivity;
import com.example.hotel_booking_app.ui.activities.HotelSearchActivity;
import com.example.hotel_booking_app.ui.activities.SavedHotelsActivity;

public final class CustomerNavigationHelper {
    public static final int TAB_SEARCH = 0;
    public static final int TAB_SAVED = 1;
    public static final int TAB_BOOKINGS = 2;
    public static final int TAB_MESSAGES = 3;
    public static final int TAB_PROFILE = 4;

    private CustomerNavigationHelper() {
    }

    public static void bind(Activity activity, int activeTab) {
        View root = activity.findViewById(R.id.nav_customer_root);
        if (root != null) {
            root.setVisibility(View.VISIBLE);
        }

        bindItem(activity, activeTab, TAB_SEARCH, R.id.nav_cabins, HotelSearchActivity.class);
        bindItem(activity, activeTab, TAB_SAVED, R.id.nav_wishlist, SavedHotelsActivity.class);
        bindItem(activity, activeTab, TAB_BOOKINGS, R.id.nav_bookings, GuestBookingsActivity.class);
        bindItem(activity, activeTab, TAB_MESSAGES, R.id.nav_messages, ConversationListActivity.class);
        bindItem(activity, activeTab, TAB_PROFILE, R.id.nav_personal, AccountHubActivity.class);
    }

    private static void bindItem(
            Activity activity,
            int activeTab,
            int tab,
            int containerId,
            Class<?> target
    ) {
        LinearLayout container = activity.findViewById(containerId);
        if (container == null) {
            return;
        }

        ImageView icon = null;
        TextView label = null;
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (icon == null && child instanceof ImageView) {
                icon = (ImageView) child;
            } else if (label == null && child instanceof TextView) {
                label = (TextView) child;
            }
        }
        if (icon == null || label == null) {
            return;
        }

        boolean active = activeTab == tab;
        int color = activity.getColor(active ? R.color.booking_blue : R.color.booking_muted);
        icon.setColorFilter(color);
        label.setTextColor(color);
        label.setTypeface(null, active ? Typeface.BOLD : Typeface.NORMAL);
        container.setAlpha(active ? 1f : 0.82f);
        container.setScaleX(active ? 1.03f : 1f);
        container.setScaleY(active ? 1.03f : 1f);
        updateIndicator(activity, container, active);
        container.setOnClickListener(view -> {
            if (active) {
                return;
            }
            view.animate()
                    .scaleX(0.96f)
                    .scaleY(0.96f)
                    .setDuration(70)
                    .withEndAction(() -> {
                        view.animate().scaleX(1f).scaleY(1f).setDuration(110).start();
                        Intent intent = new Intent(activity, target);
                        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        activity.startActivity(intent);
                        activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    })
                    .start();
        });
    }

    private static void updateIndicator(Activity activity, LinearLayout container, boolean active) {
        for (int i = container.getChildCount() - 1; i >= 0; i--) {
            View child = container.getChildAt(i);
            if (!(child instanceof ImageView) && !(child instanceof TextView)) {
                container.removeViewAt(i);
            }
        }
        if (!active) {
            return;
        }

        View indicator = new View(activity);
        int width = dp(activity, 26);
        int height = dp(activity, 3);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
        params.topMargin = dp(activity, 5);
        indicator.setLayoutParams(params);
        indicator.setBackgroundResource(R.drawable.bg_bottom_indicator);
        container.addView(indicator);
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
