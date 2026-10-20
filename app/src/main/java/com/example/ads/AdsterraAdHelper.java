package com.example.ads;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * AdsterraAdHelper loads and renders Adsterra ad units seamlessly:
 * 1. Banner 468x60 - Video Player Pause Ad
 * 2. Banner 320x50 - Mobile Bottom Banner
 * 3. Native Banner - Movie Details Dialog & Feed
 *
 * Configured with:
 * - Proper iframe navigation allowance (so subframe ads render inside WebView)
 * - Dynamic height auto-resize via AdResizeBridge so native ads are never cut off
 * - Third-party cookies enabled (required for Cloudflare / Adsterra verification)
 * - Mobile Chrome User-Agent (bypasses in-app bot filters)
 * - WebChromeClient for javascript execution
 */
public class AdsterraAdHelper {

    public static final String BASE_URL = "https://epicframe20.blogspot.com/";

    public static final String BANNER_468_60_HTML =
            "<!DOCTYPE html><html><head>" +
            "<meta name='viewport' content='width=device-width, initial-scale=1.0, user-scalable=no'>" +
            "<style>" +
            "  * { margin:0; padding:0; box-sizing:border-box; }" +
            "  html, body { background-color: transparent; width: 100%; height: 100%; display: flex; justify-content: center; align-items: center; overflow: hidden; }" +
            "  iframe { max-width: 100% !important; border: none !important; margin: 0 auto !important; }" +
            "</style></head><body>" +
            "<script>" +
            "  atOptions = {" +
            "    'key' : '520d4300dff0ccf5ea53a37ba1a02008'," +
            "    'format' : 'iframe'," +
            "    'height' : 60," +
            "    'width' : 468," +
            "    'params' : {}" +
            "  };" +
            "</script>" +
            "<script src='https://bellnewyork.org/22/520d4300dff0ccf5ea53a37ba1a02008'></script>" +
            "</body></html>";

    public static final String BANNER_320_50_HTML =
            "<!DOCTYPE html><html><head>" +
            "<meta name='viewport' content='width=device-width, initial-scale=1.0, user-scalable=no'>" +
            "<style>" +
            "  * { margin:0; padding:0; box-sizing:border-box; }" +
            "  html, body { background-color: transparent; width: 100%; height: 100%; display: flex; justify-content: center; align-items: center; overflow: hidden; }" +
            "  iframe { max-width: 100% !important; border: none !important; margin: 0 auto !important; }" +
            "</style></head><body>" +
            "<script>" +
            "  atOptions = {" +
            "    'key' : '39243c9ee0e04939e05ce7d9b88c241a'," +
            "    'format' : 'iframe'," +
            "    'height' : 50," +
            "    'width' : 320," +
            "    'params' : {}" +
            "  };" +
            "</script>" +
            "<script src='https://bellnewyork.org/22/39243c9ee0e04939e05ce7d9b88c241a'></script>" +
            "</body></html>";

    public static final String NATIVE_BANNER_HTML =
            "<!DOCTYPE html><html><head>" +
            "<meta name='viewport' content='width=device-width, initial-scale=1.0, user-scalable=no'>" +
            "<style>" +
            "  * { margin:0; padding:0; box-sizing:border-box; }" +
            "  html, body { background-color: transparent; color: #FFFFFF; font-family: -apple-system, Roboto, sans-serif; padding: 2px; width: 100%; overflow: visible; }" +
            "  #container-244d7a5892f2e8e01e686f30f4591a2a { width: 100%; overflow: visible; display: block; }" +
            "  img { max-width: 100% !important; height: auto !important; border-radius: 8px !important; display: block !important; margin: 6px auto !important; }" +
            "  a { text-decoration: none !important; color: inherit !important; display: block !important; margin-bottom: 12px !important; }" +
            "</style></head><body>" +
            "<script async='async' data-cfasync='false' src='https://bellnewyork.org/21/244d7a5892f2e8e01e686f30f4591a2a'></script>" +
            "<div id='container-244d7a5892f2e8e01e686f30f4591a2a'></div>" +
            "<script>" +
            "  function reportAdHeight() {" +
            "    try {" +
            "      var c = document.getElementById('container-244d7a5892f2e8e01e686f30f4591a2a');" +
            "      var h = Math.max(" +
            "        document.body ? document.body.scrollHeight : 0," +
            "        document.documentElement ? document.documentElement.scrollHeight : 0," +
            "        c ? c.scrollHeight : 0" +
            "      );" +
            "      if (h > 60 && window.AdResizeBridge) {" +
            "        window.AdResizeBridge.onContentHeight(h);" +
            "      }" +
            "    } catch(e) {}" +
            "  }" +
            "  window.addEventListener('load', reportAdHeight);" +
            "  var count = 0;" +
            "  var timer = setInterval(function() {" +
            "    reportAdHeight();" +
            "    count++;" +
            "    if (count > 25) clearInterval(timer);" +
            "  }, 400);" +
            "</script>" +
            "</body></html>";

    public static void loadAd(final Context context, final WebView webView, final String htmlContent) {
        if (webView == null || context == null) return;

        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        // Enable third-party cookies (critical for Cloudflare & Adsterra verification cookies)
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(webView, true);
        }

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        // Set genuine Mobile Chrome User-Agent without the "; wv" (WebView) token
        // to prevent Cloudflare & ad servers from rejecting the request
        String defaultUa = settings.getUserAgentString();
        if (defaultUa != null && defaultUa.contains("; wv")) {
            settings.setUserAgentString(defaultUa.replace("; wv", ""));
        } else if (defaultUa == null || !defaultUa.contains("Chrome")) {
            settings.setUserAgentString("Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36");
        }

        webView.setWebChromeClient(new WebChromeClient());

        // JavaScript interface to automatically expand WebView height as Adsterra cards render
        webView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void onContentHeight(final int heightCss) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            float density = context.getResources().getDisplayMetrics().density;
                            int targetPx = (int) (heightCss * density) + (int) (14 * density);
                            android.view.ViewGroup.LayoutParams lp = webView.getLayoutParams();
                            if (lp != null && Math.abs(lp.height - targetPx) > 8) {
                                lp.height = targetPx;
                                webView.setLayoutParams(lp);
                                webView.requestLayout();
                            }
                        } catch (Exception ignored) {
                        }
                    }
                });
            }
        }, "AdResizeBridge");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (request == null || request.getUrl() == null) return false;

                // CRITICAL: Subframe / iframe requests MUST NOT be intercepted!
                // Intercepting them cancels the Adsterra ad iframe and causes blank ad slots.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    if (!request.isForMainFrame()) {
                        return false;
                    }
                }

                String url = request.getUrl().toString();
                // Allow base page and internal ad network loading
                if (url.equalsIgnoreCase(BASE_URL) || url.startsWith("about:") || url.contains("bellnewyork.org")) {
                    return false;
                }

                // If user tapped on an external sponsor / ad landing page, open in real browser
                openExternalBrowser(context, request.getUrl());
                return true;
            }

            @Override
            @SuppressWarnings("deprecation")
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url == null) return false;
                if (url.equalsIgnoreCase(BASE_URL) || url.startsWith("about:") || url.contains("bellnewyork.org")) {
                    return false;
                }
                openExternalBrowser(context, Uri.parse(url));
                return true;
            }

            private void openExternalBrowser(Context ctx, Uri uri) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ctx.startActivity(intent);
                } catch (Exception ignored) {
                }
            }
        });

        webView.loadDataWithBaseURL(BASE_URL, htmlContent, "text/html", "UTF-8", null);
    }
}
