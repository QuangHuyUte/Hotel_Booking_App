package com.example.hotel_booking_app.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hotel_booking_app.R;
import com.example.hotel_booking_app.data.models.Cabin;
import com.example.hotel_booking_app.data.models.Conversation;
import com.example.hotel_booking_app.data.remote.SupabaseCallback;
import com.example.hotel_booking_app.services.CabinService;
import com.example.hotel_booking_app.services.ChatService;
import com.example.hotel_booking_app.utils.AppConstants;
import com.example.hotel_booking_app.utils.SessionManager;

import java.util.List;

public class MessagesActivity extends AppCompatActivity {
    private TextView statusTextView;
    private LinearLayout conversationsContainer;
    private ChatService chatService;
    private CabinService cabinService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);

        statusTextView = findViewById(R.id.text_status);
        conversationsContainer = findViewById(R.id.container_conversations);
        Button backButton = findViewById(R.id.button_back);
        Button backBottomButton = findViewById(R.id.button_back_bottom);
        LinearLayout searchTab = findViewById(R.id.nav_cabins);
        LinearLayout bookingsTab = findViewById(R.id.nav_bookings);
        LinearLayout wishlistTab = findViewById(R.id.nav_wishlist);
        LinearLayout profileTab = findViewById(R.id.nav_personal);
        chatService = new ChatService();
        cabinService = new CabinService();
        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        backButton.setOnClickListener(view -> finish());
        backBottomButton.setOnClickListener(view -> finish());
        searchTab.setOnClickListener(view -> startActivity(new Intent(this, CabinListActivity.class)));
        bookingsTab.setOnClickListener(view -> startActivity(new Intent(this, MyBookingsActivity.class)));
        wishlistTab.setOnClickListener(view -> startActivity(new Intent(this, MyWishlistActivity.class)));
        profileTab.setOnClickListener(view -> startActivity(new Intent(this, PersonalActivity.class)));
        loadConversations();
    }

    private void loadConversations() {
        statusTextView.setText("Loading conversations...");
        SupabaseCallback<List<Conversation>> callback = new SupabaseCallback<List<Conversation>>() {
            @Override
            public void onSuccess(List<Conversation> conversations) {
                conversationsContainer.removeAllViews();
                if (conversations.isEmpty()) {
                    statusTextView.setText("No messages yet.");
                    return;
                }
                statusTextView.setText(conversations.size() + " conversation" + (conversations.size() == 1 ? "" : "s") + ".");
                for (Conversation conversation : conversations) {
                    addConversationCard(conversation, "Cabin conversation");
                    loadCabinName(conversation);
                }
            }

            @Override
            public void onError(String message) {
                statusTextView.setText(message);
            }
        };

        if (sessionManager.isHostOrAdmin()) {
            chatService.getAllConversations(callback);
        } else {
            chatService.getGuestConversations(sessionManager.getUserId(), callback);
        }
    }

    private void addConversationCard(Conversation conversation, String title) {
        Button button = new Button(this);
        button.setText(title + "\nTap to open chat");
        button.setAllCaps(false);
        button.setTextColor(getColor(R.color.black));
        button.setBackgroundResource(R.drawable.bg_button_secondary);
        button.setPadding(18, 14, 18, 14);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 12);
        button.setLayoutParams(params);
        button.setTag(conversation.getId());
        button.setOnClickListener(view -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra(ChatActivity.EXTRA_CONVERSATION_ID, conversation.getId());
            startActivity(intent);
        });
        conversationsContainer.addView(button);
    }

    private void loadCabinName(Conversation conversation) {
        if (conversation.getCabinId() == null || conversation.getCabinId().trim().isEmpty()) {
            return;
        }
        cabinService.getCabinById(conversation.getCabinId(), new SupabaseCallback<Cabin>() {
            @Override
            public void onSuccess(Cabin cabin) {
                for (int i = 0; i < conversationsContainer.getChildCount(); i++) {
                    Button button = (Button) conversationsContainer.getChildAt(i);
                    if (conversation.getId().equals(button.getTag())) {
                        button.setText(cabin.getName() + "\nTap to open chat");
                        return;
                    }
                }
            }

            @Override
            public void onError(String message) {
            }
        });
    }
}
