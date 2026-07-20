package com.example.hotel_booking_app.ui.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hotel_booking_app.R;

public class OAuthBrowserActivity extends AppCompatActivity {
    public static final String EXTRA_AUTH_URL = "extra_auth_url";
    public static final String EXTRA_CALLBACK_URL = "extra_callback_url";

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_oauth_browser);

        webView = findViewById(R.id.web_view);
        TextView closeButton = findViewById(R.id.button_close);
        closeButton.setOnClickListener(view -> finishWithCancel());

        String authUrl = getIntent().getStringExtra(EXTRA_AUTH_URL);
        if (authUrl == null || authUrl.trim().isEmpty()) {
            finishWithCancel();
            return;
        }

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleUrl(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUrl(Uri.parse(url));
            }

            private boolean handleUrl(Uri uri) {
                if (uri == null) {
                    return false;
                }
                if ("hotelbookingapp".equals(uri.getScheme()) && "auth".equals(uri.getHost())) {
                    Intent result = new Intent();
                    result.putExtra(EXTRA_CALLBACK_URL, uri.toString());
                    setResult(RESULT_OK, result);
                    finish();
                    return true;
                }
                return false;
            }
        });

        if (savedInstanceState == null) {
            webView.loadUrl(authUrl);
        } else {
            webView.restoreState(savedInstanceState);
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
            return;
        }
        finishWithCancel();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (webView != null) {
            webView.saveState(outState);
        }
    }

    private void finishWithCancel() {
        setResult(RESULT_CANCELED);
        finish();
    }
}
