package com.example.hotel_booking_app.ui.helpers;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.hotel_booking_app.R;
import com.example.hotel_booking_app.ui.activities.ConversationListActivity;
import com.example.hotel_booking_app.ui.activities.HostHotelDashboardActivity;
import com.example.hotel_booking_app.ui.activities.PaymentHistoryActivity;
import com.example.hotel_booking_app.ui.activities.ProfileDetailsActivity;

public final class ManagerNavigationHelper {
    public static final int TAB_DASHBOARD = 0;
    public static final int TAB_MESSAGES = 1;
    public static final int TAB_TRANSACTIONS = 2;
    public static final int TAB_PROFILE = 3;

    private ManagerNavigationHelper() {
    }

    public static void bind(Activity activity, int activeTab) {
        View root = activity.findViewById(R.id.nav_manager_root);
        if (root == null) {
            return;
        }
        root.setVisibility(View.VISIBLE);

        bindItem(activity, activeTab, TAB_DASHBOARD,
                R.id.nav_manager_dashboard,
                R.id.icon_manager_dashboard,
                R.id.text_manager_dashboard,
                HostHotelDashboardActivity.class);
        bindItem(activity, activeTab, TAB_MESSAGES,
                R.id.nav_manager_messages,
                R.id.icon_manager_messages,
                R.id.text_manager_messages,
                ConversationListActivity.class);
        bindItem(activity, activeTab, TAB_TRANSACTIONS,
                R.id.nav_manager_transactions,
                R.id.icon_manager_transactions,
                R.id.text_manager_transactions,
                PaymentHistoryActivity.class);
        bindItem(activity, activeTab, TAB_PROFILE,
                R.id.nav_manager_profile,
                R.id.icon_manager_profile,
                R.id.text_manager_profile,
                ProfileDetailsActivity.class);
    }

    private static void bindItem(
            Activity activity,
            int activeTab,
            int tab,
            int containerId,
            int iconId,
            int labelId,
            Class<?> target
    ) {
        LinearLayout container = activity.findViewById(containerId);
        ImageView icon = activity.findViewById(iconId);
        TextView label = activity.findViewById(labelId);
        if (container == null || icon == null || label == null) {
            return;
        }

        boolean active = activeTab == tab;
        int textColor = activity.getColor(active ? R.color.booking_blue : R.color.booking_muted);
        icon.setBackgroundResource(active ? R.drawable.bg_manager_icon_active : R.drawable.bg_manager_icon_idle);
        icon.setColorFilter(textColor);
        label.setTextColor(textColor);
        label.setTypeface(null, active ? Typeface.BOLD : Typeface.NORMAL);
        container.setAlpha(active ? 1f : 0.82f);
        container.setScaleX(active ? 1.03f : 1f);
        container.setScaleY(active ? 1.03f : 1f);
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
}
