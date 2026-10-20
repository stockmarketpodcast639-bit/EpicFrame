package com.example;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.media3.common.PlaybackException;
import androidx.media3.ui.PlayerView;
import com.bumptech.glide.Glide;
import com.example.model.MovieItem;
import com.example.player.CinemaPlayer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * PlayerActivity uses the reusable CinemaPlayer component to deliver:
 * - High-speed hardware-accelerated Media3 ExoPlayer playback
 * - Zero-lag play/pause response with custom icons
 * - 10-second seek forward/backward buttons and quick 10s slide chips
 * - Persistent screen tap handling (tapping screen ALWAYS toggles buttons back on)
 * - Double-tap screen gestures for -10s / +10s seeking
 * - Direct download and external player (VLC/MX Player) launching
 */
public class PlayerActivity extends AppCompatActivity {
    public static final String EXTRA_MOVIE = "extra_movie_item";
    public static final String EXTRA_STREAM_URL = "extra_stream_url";

    private MovieItem movie;
    private String currentStreamPageUrl;
    private String resolvedDirectVideoUrl = null;

    // CinemaPlayer Component & Views
    private CinemaPlayer cinemaPlayer;
    private PlayerView exoPlayerView;
    private WebView playerWebView;
    private ImageView imgPlayerPoster;
    private View playerLoadingView;
    private TextView tvPlayerLoadingText;
    private TextView tvPlayerLoadingSubtext;

    // Controls Overlay
    private View layoutControlsOverlay;
    private View topPlayerBar;
    private View centerControlsRow;
    private View bottomPlayerControls;
    private View layoutSlideButtons;
    private View layoutPlayerPauseAd;
    private WebView webPlayerPauseAd;
    private View layoutPlayerExtraRows;

    private TextView tvPlayerTitle;
    private TextView tvPlayerSubtitle;
    private TextView playerQualityTag;
    private LinearLayout layoutQualityButtons;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_player);

        movie = (MovieItem) getIntent().getSerializableExtra(EXTRA_MOVIE);
        currentStreamPageUrl = getIntent().getStringExtra(EXTRA_STREAM_URL);
        if (movie != null && (currentStreamPageUrl == null || currentStreamPageUrl.isEmpty())) {
            currentStreamPageUrl = movie.getBestStreamUrl();
        }

        initViews();
        setupCinemaPlayer();
        setupQualityButtons();
        setupBackNavigation();

        // Start ultra-fast stream resolution & immediate playback
        resolveAndPlayStream(currentStreamPageUrl);
    }

    private void initViews() {
        exoPlayerView = findViewById(R.id.exoPlayerView);
        playerWebView = findViewById(R.id.playerWebView);
        imgPlayerPoster = findViewById(R.id.imgPlayerPoster);
        playerLoadingView = findViewById(R.id.playerLoadingView);
        tvPlayerLoadingText = findViewById(R.id.tvPlayerLoadingText);
        tvPlayerLoadingSubtext = findViewById(R.id.tvPlayerLoadingSubtext);

        layoutControlsOverlay = findViewById(R.id.layoutControlsOverlay);
        topPlayerBar = findViewById(R.id.topPlayerBar);
        centerControlsRow = findViewById(R.id.centerControlsRow);
        bottomPlayerControls = findViewById(R.id.bottomPlayerControls);
        layoutSlideButtons = findViewById(R.id.layoutSlideButtons);

        layoutPlayerPauseAd = findViewById(R.id.layoutPlayerPauseAd);
        webPlayerPauseAd = findViewById(R.id.webPlayerPauseAd);
        View btnClosePauseAd = findViewById(R.id.btnClosePauseAd);
        if (btnClosePauseAd != null) {
            btnClosePauseAd.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (layoutPlayerPauseAd != null) {
                        layoutPlayerPauseAd.setVisibility(View.GONE);
                    }
                }
            });
        }
        if (webPlayerPauseAd != null) {
            com.example.ads.AdsterraAdHelper.loadAd(this, webPlayerPauseAd, com.example.ads.AdsterraAdHelper.BANNER_468_60_HTML);
        }

        layoutPlayerExtraRows = findViewById(R.id.layoutPlayerExtraRows);
        updateLayoutForOrientation(getResources().getConfiguration().orientation);

        tvPlayerTitle = findViewById(R.id.tvPlayerTitle);
        tvPlayerSubtitle = findViewById(R.id.tvPlayerSubtitle);
        playerQualityTag = findViewById(R.id.playerQualityTag);
        layoutQualityButtons = findViewById(R.id.layoutQualityButtons);

        if (movie != null) {
            tvPlayerTitle.setText(movie.getCleanTitle());
            tvPlayerSubtitle.setText(movie.getYear() + " • " + movie.getLanguage() + " • " + movie.getDuration());
            if (movie.getBackdropUrl() != null && !movie.getBackdropUrl().isEmpty()) {
                Glide.with(this).load(movie.getBackdropUrl()).into(imgPlayerPoster);
            }
        }

        View.OnClickListener closeListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closePlayerAndExit();
            }
        };

        findViewById(R.id.btnPlayerBack).setOnClickListener(closeListener);
        View btnCloseTop = findViewById(R.id.btnClosePlayer);
        if (btnCloseTop != null) {
            btnCloseTop.setOnClickListener(closeListener);
        }

        findViewById(R.id.btnOpenExternalPlayer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openInExternalPlayer();
            }
        });

        findViewById(R.id.btnDownloadMovie).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startDirectDownload();
            }
        });

        findViewById(R.id.btnOpenWebArticle).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (movie != null) {
                    Intent intent = new Intent(PlayerActivity.this, WebActivity.class);
                    intent.putExtra(WebActivity.EXTRA_URL, movie.getPostUrl());
                    intent.putExtra(WebActivity.EXTRA_TITLE, movie.getCleanTitle());
                    startActivity(intent);
                }
            }
        });

        findViewById(R.id.btnFullscreenToggle).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleOrientation();
            }
        });
    }

    private void setupCinemaPlayer() {
        cinemaPlayer = new CinemaPlayer(this, exoPlayerView);

        // Attach custom overlay controls and persistent tap gestures
        cinemaPlayer.attachControls(
                layoutControlsOverlay,
                topPlayerBar,
                centerControlsRow,
                bottomPlayerControls,
                layoutSlideButtons,
                (ImageView) findViewById(R.id.imgMainPlayPause),
                (ImageView) findViewById(R.id.imgMiniPlayPause),
                (TextView) findViewById(R.id.tvMiniPlayPause),
                findViewById(R.id.btnRewind10),
                findViewById(R.id.btnForward10),
                findViewById(R.id.btnSlideRewind10),
                findViewById(R.id.btnSlideForward10),
                (SeekBar) findViewById(R.id.playerSeekBar),
                (TextView) findViewById(R.id.tvCurrentTime),
                (TextView) findViewById(R.id.tvTotalTime),
                findViewById(R.id.indicatorRewind10),
                findViewById(R.id.indicatorForward10)
        );

        // Ensure clicking the big red circle FrameLayout also toggles Play/Pause instantly
        View btnMainPlayPauseFrame = findViewById(R.id.btnMainPlayPause);
        if (btnMainPlayPauseFrame != null) {
            btnMainPlayPauseFrame.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    cinemaPlayer.togglePlayPause();
                }
            });
        }

        cinemaPlayer.setListener(new CinemaPlayer.PlayerListener() {
            @Override
            public void onPlaybackReady(long durationMs) {
                playerLoadingView.setVisibility(View.GONE);
                imgPlayerPoster.setVisibility(View.GONE);
                exoPlayerView.setVisibility(View.VISIBLE);
                playerWebView.setVisibility(View.GONE);
            }

            @Override
            public void onPlaybackBuffering() {
                playerLoadingView.setVisibility(View.VISIBLE);
                tvPlayerLoadingText.setText("Buffering Cinema Stream...");
                tvPlayerLoadingSubtext.setText("Ultra-fast network buffering");
            }

            @Override
            public void onPlaybackEnded() {
                if (layoutPlayerPauseAd != null) {
                    layoutPlayerPauseAd.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                if (layoutPlayerPauseAd != null) {
                    if (!isPlaying && cinemaPlayer != null && cinemaPlayer.getDuration() > 0) {
                        layoutPlayerPauseAd.setVisibility(View.VISIBLE);
                    } else {
                        layoutPlayerPauseAd.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                fallbackToWebStream(currentStreamPageUrl);
            }

            @Override
            public void onControlsVisibilityChanged(boolean isVisible) {
                if (!isVisible && layoutPlayerPauseAd != null) {
                    layoutPlayerPauseAd.setVisibility(View.GONE);
                }
            }
        });
    }

    private void resolveAndPlayStream(final String streamUrl) {
        if (streamUrl == null || streamUrl.isEmpty()) return;

        playerLoadingView.setVisibility(View.VISIBLE);
        tvPlayerLoadingText.setText("Connecting to Direct Stream...");
        tvPlayerLoadingSubtext.setText("Bypassing ads & resolving high-speed stream");

        // If it's already a direct video stream file (.mkv, .mp4, .webm)
        if (streamUrl.contains(".mkv") || streamUrl.contains(".mp4") || streamUrl.contains(".webm")) {
            startPlayback(streamUrl);
            return;
        }

        // Fetch the cloud page in background with fast timeout
        Request request = new Request.Builder()
                .url(streamUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        fallbackToWebStream(streamUrl);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            fallbackToWebStream(streamUrl);
                        }
                    });
                    return;
                }

                String html = response.body().string();
                final String directUrl = extractDirectStreamFromHtml(html);
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (directUrl != null && !directUrl.isEmpty()) {
                            resolvedDirectVideoUrl = directUrl;
                            startPlayback(directUrl);
                        } else {
                            fallbackToWebStream(streamUrl);
                        }
                    }
                });
            }
        });
    }

    private String extractDirectStreamFromHtml(String html) {
        if (html == null) return null;

        Pattern p1 = Pattern.compile("href=['\"]([^'\"]+\\.(?:mkv|mp4|webm)[^'\"]*)['\"]", Pattern.CASE_INSENSITIVE);
        Matcher m1 = p1.matcher(html);
        if (m1.find()) {
            return m1.group(1);
        }

        Pattern p2 = Pattern.compile("<a[^>]+href=['\"]([^'\"]+)['\"][^>]*>Cloud Direct", Pattern.CASE_INSENSITIVE);
        Matcher m2 = p2.matcher(html);
        if (m2.find()) {
            return m2.group(1);
        }

        Pattern p3 = Pattern.compile("<a[^>]+href=['\"]([^'\"]+)['\"][^>]*>Direct Download", Pattern.CASE_INSENSITIVE);
        Matcher m3 = p3.matcher(html);
        if (m3.find()) {
            return m3.group(1);
        }

        return null;
    }

    private void startPlayback(String videoUrl) {
        resolvedDirectVideoUrl = videoUrl;
        playerLoadingView.setVisibility(View.VISIBLE);
        tvPlayerLoadingText.setText("Buffering Cinema Video...");
        tvPlayerLoadingSubtext.setText("Ultra-fast Hardware Stream Initialized");

        exoPlayerView.setVisibility(View.VISIBLE);
        playerWebView.setVisibility(View.GONE);

        if (cinemaPlayer != null) {
            cinemaPlayer.playUri(videoUrl);
        }
    }

    private void fallbackToWebStream(String webUrl) {
        if (cinemaPlayer != null) {
            cinemaPlayer.stop();
        }
        exoPlayerView.setVisibility(View.GONE);
        playerWebView.setVisibility(View.VISIBLE);
        playerLoadingView.setVisibility(View.VISIBLE);

        WebSettings settings = playerWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        playerWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String u = request.getUrl().toString().toLowerCase();
                if (u.contains("effectivecpmnetwork") || u.contains("adsterra") || u.contains("syndication") || u.contains("popads")) {
                    return true;
                }
                if (u.contains(".mkv") || u.contains(".mp4")) {
                    startPlayback(request.getUrl().toString());
                    return true;
                }
                return false;
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String u = request.getUrl().toString().toLowerCase();
                if (u.contains("effectivecpmnetwork") || u.contains("adsterra") || u.contains("popads")) {
                    return new WebResourceResponse("text/javascript", "UTF-8", new ByteArrayInputStream("".getBytes()));
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                playerLoadingView.setVisibility(View.GONE);
                imgPlayerPoster.setVisibility(View.GONE);
            }
        });

        playerWebView.loadUrl(webUrl);
    }

    private void setupQualityButtons() {
        layoutQualityButtons.removeAllViews();
        if (movie != null && movie.getStreamOptions() != null && !movie.getStreamOptions().isEmpty()) {
            List<MovieItem.StreamOption> options = movie.getStreamOptions();
            for (int i = 0; i < options.size(); i++) {
                final MovieItem.StreamOption opt = options.get(i);
                Button qBtn = new Button(this);
                qBtn.setText("⚡ " + opt.getQuality());
                qBtn.setTextSize(12);
                qBtn.setTypeface(null, android.graphics.Typeface.BOLD);
                qBtn.setAllCaps(false);

                boolean isCurrent = (opt.getUrl().equals(currentStreamPageUrl));
                styleQualityButton(qBtn, isCurrent);

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        (int) (36 * getResources().getDisplayMetrics().density));
                lp.setMargins(0, 0, 10, 0);
                qBtn.setLayoutParams(lp);

                qBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        currentStreamPageUrl = opt.getUrl();
                        playerQualityTag.setText(opt.getQuality().toUpperCase());
                        setupQualityButtons();
                        resolveAndPlayStream(currentStreamPageUrl);
                        Toast.makeText(PlayerActivity.this, "Switched to " + opt.getQuality(), Toast.LENGTH_SHORT).show();
                    }
                });
                layoutQualityButtons.addView(qBtn);
            }
        } else {
            Button defaultBtn = new Button(this);
            defaultBtn.setText("⚡ Direct Play (HD)");
            defaultBtn.setTextSize(12);
            styleQualityButton(defaultBtn, true);
            layoutQualityButtons.addView(defaultBtn);
        }
    }

    private void styleQualityButton(Button btn, boolean isSelected) {
        if (isSelected) {
            btn.setBackgroundResource(R.drawable.bg_netflix_button_white);
            btn.setTextColor(ContextCompat.getColor(this, R.color.black));
        } else {
            btn.setBackgroundResource(R.drawable.bg_netflix_button_dark);
            btn.setTextColor(ContextCompat.getColor(this, R.color.white));
        }
    }

    private void openInExternalPlayer() {
        String videoUrl = resolvedDirectVideoUrl != null ? resolvedDirectVideoUrl : currentStreamPageUrl;
        if (videoUrl != null) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(videoUrl), "video/*");
                startActivity(Intent.createChooser(intent, "Play with External Video Player (VLC, MX Player)"));
            } catch (Exception e) {
                Toast.makeText(this, "No external video player found (VLC, MX Player)", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startDirectDownload() {
        String downloadUrl = resolvedDirectVideoUrl != null ? resolvedDirectVideoUrl : currentStreamPageUrl;
        if (downloadUrl != null) {
            if (downloadUrl.contains(".mkv") || downloadUrl.contains(".mp4")) {
                try {
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadUrl));
                    String title = movie != null ? movie.getCleanTitle() : "Movie";
                    request.setTitle(title);
                    request.setDescription("Downloading " + title + " from Epic Frame");
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, title + ".mkv");
                    DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                    if (dm != null) {
                        dm.enqueue(request);
                        Toast.makeText(this, "Download started for " + title, Toast.LENGTH_LONG).show();
                        return;
                    }
                } catch (Exception ignored) {
                }
            }
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl));
            startActivity(intent);
        }
    }

    private void toggleOrientation() {
        int orientation = getRequestedOrientation();
        if (orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            updateLayoutForOrientation(android.content.res.Configuration.ORIENTATION_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            updateLayoutForOrientation(android.content.res.Configuration.ORIENTATION_LANDSCAPE);
        }
    }

    @Override
    public void onConfigurationChanged(@androidx.annotation.NonNull android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateLayoutForOrientation(newConfig.orientation);
    }

    private void updateLayoutForOrientation(int orientation) {
        boolean isLandscape = (orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE);
        if (layoutPlayerExtraRows != null) {
            layoutPlayerExtraRows.setVisibility(isLandscape ? View.GONE : View.VISIBLE);
        }
        if (webPlayerPauseAd != null) {
            android.view.ViewGroup.LayoutParams lp = webPlayerPauseAd.getLayoutParams();
            if (lp != null) {
                int screenWidth = getResources().getDisplayMetrics().widthPixels;
                if (isLandscape) {
                    lp.width = (int) (468 * getResources().getDisplayMetrics().density);
                } else {
                    lp.width = Math.min((int) (468 * getResources().getDisplayMetrics().density), screenWidth - (int) (32 * getResources().getDisplayMetrics().density));
                }
                webPlayerPauseAd.setLayoutParams(lp);
            }
        }
    }

    private void closePlayerAndExit() {
        if (cinemaPlayer != null) {
            cinemaPlayer.release();
            cinemaPlayer = null;
        }
        if (playerWebView != null) {
            playerWebView.stopLoading();
        }
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void setupBackNavigation() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                } else {
                    closePlayerAndExit();
                }
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
        if (cinemaPlayer != null) {
            switch (keyCode) {
                case android.view.KeyEvent.KEYCODE_SPACE:
                case android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                case android.view.KeyEvent.KEYCODE_K:
                    cinemaPlayer.togglePlayPause();
                    return true;
                case android.view.KeyEvent.KEYCODE_DPAD_LEFT:
                case android.view.KeyEvent.KEYCODE_J:
                    cinemaPlayer.seekBackward10();
                    return true;
                case android.view.KeyEvent.KEYCODE_DPAD_RIGHT:
                case android.view.KeyEvent.KEYCODE_L:
                    cinemaPlayer.seekForward10();
                    return true;
                case android.view.KeyEvent.KEYCODE_F:
                    toggleOrientation();
                    return true;
                case android.view.KeyEvent.KEYCODE_ESCAPE:
                    closePlayerAndExit();
                    return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cinemaPlayer != null) {
            cinemaPlayer.pause();
        }
        if (playerWebView != null) {
            playerWebView.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (playerWebView != null) {
            playerWebView.onResume();
        }
    }

    @Override
    protected void onDestroy() {
        if (cinemaPlayer != null) {
            cinemaPlayer.release();
            cinemaPlayer = null;
        }
        if (playerWebView != null) {
            playerWebView.stopLoading();
            playerWebView.destroy();
        }
        if (webPlayerPauseAd != null) {
            webPlayerPauseAd.stopLoading();
            webPlayerPauseAd.destroy();
        }
        super.onDestroy();
    }
}
