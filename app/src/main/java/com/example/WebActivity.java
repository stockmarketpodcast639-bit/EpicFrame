package com.example;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import java.net.URISyntaxException;

public class WebActivity extends AppCompatActivity {
    public static final String EXTRA_URL = "extra_target_url";
    public static final String EXTRA_TITLE = "extra_target_title";

    private WebView webView;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView tvTitle;
    private String currentUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_viewer);

        currentUrl = getIntent().getStringExtra(EXTRA_URL);
        if (currentUrl == null || currentUrl.isEmpty()) {
            currentUrl = "https://epicframe20.blogspot.com/";
        }

        String initialTitle = getIntent().getStringExtra(EXTRA_TITLE);
        initViews(initialTitle);
        setupWebView();
        setupActions();

        webView.loadUrl(currentUrl);
    }

    private void initViews(String initialTitle) {
        webView = findViewById(R.id.innerWebView);
        progressBar = findViewById(R.id.webProgressBar);
        swipeRefreshLayout = findViewById(R.id.webSwipeRefresh);
        tvTitle = findViewById(R.id.tvWebTitle);

        if (initialTitle != null && !initialTitle.isEmpty()) {
            tvTitle.setText(initialTitle);
        }

        ImageView btnBack = findViewById(R.id.btnWebBack);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    finish();
                }
            }
        });

        ImageView btnRefresh = findViewById(R.id.btnWebRefresh);
        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webView.reload();
            }
        });

        ImageView btnOpenExternal = findViewById(R.id.btnWebOpenExternal);
        btnOpenExternal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    String url = webView.getUrl() != null ? webView.getUrl() : currentUrl;
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(WebActivity.this, "Cannot open external browser", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleUri(request.getUrl());
            }

            @Override
            @SuppressWarnings("deprecation")
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUri(Uri.parse(url));
            }

            private boolean handleUri(Uri uri) {
                if (uri == null) return false;
                String scheme = uri.getScheme();
                if (scheme != null && !scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")) {
                    try {
                        Intent intent;
                        if (scheme.equalsIgnoreCase("intent")) {
                            intent = Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME);
                        } else {
                            intent = new Intent(Intent.ACTION_VIEW, uri);
                        }
                        startActivity(intent);
                        return true;
                    } catch (URISyntaxException | ActivityNotFoundException e) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
                if (view.getTitle() != null && !view.getTitle().isEmpty()) {
                    tvTitle.setText(view.getTitle());
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(newProgress);
                } else {
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                }
                super.onProgressChanged(view, newProgress);
            }
        });
    }

    private void setupActions() {
        swipeRefreshLayout.setColorSchemeResources(R.color.netflix_red, R.color.netflix_gold);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                webView.reload();
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
        }
        super.onDestroy();
    }
}
