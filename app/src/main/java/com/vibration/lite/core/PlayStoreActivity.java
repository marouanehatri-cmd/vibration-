package com.vibration.lite.core;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.webkit.DownloadListener;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.vibration.lite.R;

public class PlayStoreActivity extends AppCompatActivity {

    public static final String TARGET_PACKAGE = "com.konami.efootballpes";
    public static final String PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=" + TARGET_PACKAGE;

    private WebView mWebView;
    private ProgressBar mProgressBar;
    private TextView mTvStoreStatus;
    private MaterialButton mBtnDirectPlayStore;
    private MaterialButton mBtnRefresh;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PerformanceOptimizer.applyHardwareAcceleration(this);
        setContentView(R.layout.activity_play_store);

        initViews();
        setupWebView();
        setupListeners();
        checkCurrentGameVersion();
    }

    private void initViews() {
        mWebView = findViewById(R.id.webViewPlayStore);
        mProgressBar = findViewById(R.id.progressBarLoading);
        mTvStoreStatus = findViewById(R.id.tvStoreStatus);
        mBtnDirectPlayStore = findViewById(R.id.btnDirectPlayStore);
        mBtnRefresh = findViewById(R.id.btnRefresh);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setUserAgentString("Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36");

        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
                    mProgressBar.setVisibility(View.VISIBLE);
                    mProgressBar.setProgress(newProgress);
                } else {
                    mProgressBar.setVisibility(View.GONE);
                }
            }
        });

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("market://")) {
                    GameUpdateManager.openGameInPlayStore(PlayStoreActivity.this, TARGET_PACKAGE);
                    return true;
                }
                view.loadUrl(url);
                return true;
            }
        });

        mWebView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {
                try {
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                    request.setMimeType(mimeType);
                    request.setDescription("Downloading game update for Vibration...");
                    request.setTitle("eFootball Game Update");
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "efootball_update.apk");

                    DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                    if (dm != null) {
                        dm.enqueue(request);
                        Toast.makeText(PlayStoreActivity.this, "Update downloading to Downloads folder...", Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(PlayStoreActivity.this, "Download error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        mWebView.loadUrl(PLAY_STORE_URL);
    }

    private void setupListeners() {
        mBtnDirectPlayStore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GameUpdateManager.openGameInPlayStore(PlayStoreActivity.this, TARGET_PACKAGE);
            }
        });

        mBtnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWebView.reload();
                checkCurrentGameVersion();
            }
        });
    }

    private void checkCurrentGameVersion() {
        try {
            PackageManager pm = getPackageManager();
            PackageInfo pInfo = pm.getPackageInfo(TARGET_PACKAGE, 0);
            mTvStoreStatus.setText("Installed on Device: v" + pInfo.versionName + " (Ready to clone)");
        } catch (PackageManager.NameNotFoundException e) {
            mTvStoreStatus.setText("eFootball is Not Installed. Tap 'Open in Native Play Store' to install.");
        }
    }

    @Override
    public void onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
