package com.example.player;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.OptIn;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.SeekParameters;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;
import com.example.R;
import java.util.Locale;

/**
 * Reusable CinemaPlayer component using AndroidX Media3 and ExoPlayer:
 * - Ultra-fast start with 500ms buffering
 * - Full-screen video view
 * - Custom play/pause icons with instant toggle
 * - 10-second seek forward & backward buttons (+10s / -10s)
 * - Persistent touch handling: tapping screen ALWAYS works even when controls are hidden
 * - Double-tap gestures for quick 10s skip on left/right screen halves
 */
@OptIn(markerClass = UnstableApi.class)
public class CinemaPlayer {

    public interface PlayerListener {
        void onPlaybackReady(long durationMs);
        void onPlaybackBuffering();
        void onPlaybackEnded();
        void onIsPlayingChanged(boolean isPlaying);
        void onPlayerError(PlaybackException error);
        void onControlsVisibilityChanged(boolean isVisible);
    }

    private final Context context;
    private final PlayerView playerView;
    private ExoPlayer exoPlayer;
    private PlayerListener listener;

    // Custom Controls UI Elements
    private View overlayRoot;
    private View topBar;
    private View centerControls;
    private View bottomControls;
    private View slideButtonsBar;

    private ImageView btnMainPlayPause;
    private ImageView btnMiniPlayPause;
    private TextView tvMiniPlayPause;
    private View btnRewind10;
    private View btnForward10;
    private View btnSlideRewind10;
    private View btnSlideForward10;

    private SeekBar seekBar;
    private TextView tvCurrentTime;
    private TextView tvTotalTime;

    private View indicatorRewind10;
    private View indicatorForward10;

    // State & Handlers
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isControlsVisible = true;
    private boolean isUserSeeking = false;
    private GestureDetector gestureDetector;

    private final Runnable hideControlsRunnable = new Runnable() {
        @Override
        public void run() {
            if (exoPlayer != null && exoPlayer.isPlaying()) {
                hideControls();
            }
        }
    };

    private final Runnable updateProgressRunnable = new Runnable() {
        @Override
        public void run() {
            if (exoPlayer != null && !isUserSeeking) {
                long current = exoPlayer.getCurrentPosition();
                long total = exoPlayer.getDuration();
                if (total > 0) {
                    if (seekBar != null) {
                        seekBar.setProgress((int) ((current * 1000) / total));
                    }
                    if (tvCurrentTime != null) {
                        tvCurrentTime.setText(formatTime(current));
                    }
                    if (tvTotalTime != null) {
                        tvTotalTime.setText(formatTime(total));
                    }
                }
            }
            mainHandler.postDelayed(this, 500);
        }
    };

    public CinemaPlayer(Context context, PlayerView playerView) {
        this.context = context;
        this.playerView = playerView;
        initExoPlayer();
    }

    private void initExoPlayer() {
        // Fast buffer load control: starts playback after only 500ms of buffered video!
        DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                        1500,   // Min buffer ms
                        30000,  // Max buffer ms
                        500,    // Buffer for playback ms (Instant start)
                        1000    // Buffer after rebuffer ms
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                .build();

        DefaultHttpDataSource.Factory httpDataSourceFactory = new DefaultHttpDataSource.Factory()
                .setConnectTimeoutMs(5000)
                .setReadTimeoutMs(8000)
                .setAllowCrossProtocolRedirects(true)
                .setUserAgent("Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36");

        DefaultMediaSourceFactory mediaSourceFactory = new DefaultMediaSourceFactory(context)
                .setDataSourceFactory(httpDataSourceFactory);

        exoPlayer = new ExoPlayer.Builder(context)
                .setLoadControl(loadControl)
                .setMediaSourceFactory(mediaSourceFactory)
                .setSeekParameters(SeekParameters.CLOSEST_SYNC)
                .setSeekBackIncrementMs(10000)
                .setSeekForwardIncrementMs(10000)
                .build();

        playerView.setPlayer(exoPlayer);
        playerView.setUseController(false);

        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_READY) {
                    updatePlayPauseIcons(exoPlayer.isPlaying());
                    long duration = exoPlayer.getDuration();
                    if (tvTotalTime != null && duration > 0) {
                        tvTotalTime.setText(formatTime(duration));
                    }
                    if (listener != null) {
                        listener.onPlaybackReady(duration);
                    }
                    scheduleHideControls();
                } else if (playbackState == Player.STATE_BUFFERING) {
                    if (listener != null) {
                        listener.onPlaybackBuffering();
                    }
                } else if (playbackState == Player.STATE_ENDED) {
                    updatePlayPauseIcons(false);
                    showControls();
                    if (listener != null) {
                        listener.onPlaybackEnded();
                    }
                }
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                updatePlayPauseIcons(isPlaying);
                if (listener != null) {
                    listener.onIsPlayingChanged(isPlaying);
                }
                if (isPlaying) {
                    scheduleHideControls();
                } else {
                    showControls();
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                if (listener != null) {
                    listener.onPlayerError(error);
                }
            }
        });

        mainHandler.post(updateProgressRunnable);
    }

    /**
     * Binds custom UI overlay views and wires up instant touch and button controls
     */
    public void attachControls(
            View overlayRoot,
            View topBar,
            View centerControls,
            View bottomControls,
            View slideButtonsBar,
            ImageView btnMainPlayPause,
            ImageView btnMiniPlayPause,
            TextView tvMiniPlayPause,
            View btnRewind10,
            View btnForward10,
            View btnSlideRewind10,
            View btnSlideForward10,
            SeekBar seekBar,
            TextView tvCurrentTime,
            TextView tvTotalTime,
            View indicatorRewind10,
            View indicatorForward10
    ) {
        this.overlayRoot = overlayRoot;
        this.topBar = topBar;
        this.centerControls = centerControls;
        this.bottomControls = bottomControls;
        this.slideButtonsBar = slideButtonsBar;

        this.btnMainPlayPause = btnMainPlayPause;
        this.btnMiniPlayPause = btnMiniPlayPause;
        this.tvMiniPlayPause = tvMiniPlayPause;
        this.btnRewind10 = btnRewind10;
        this.btnForward10 = btnForward10;
        this.btnSlideRewind10 = btnSlideRewind10;
        this.btnSlideForward10 = btnSlideForward10;

        this.seekBar = seekBar;
        this.tvCurrentTime = tvCurrentTime;
        this.tvTotalTime = tvTotalTime;

        this.indicatorRewind10 = indicatorRewind10;
        this.indicatorForward10 = indicatorForward10;

        setupButtonListeners();
        setupPersistentTouchGestures();
    }

    private void setupButtonListeners() {
        // Instant Play / Pause toggle
        View.OnClickListener playPauseClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePlayPause();
            }
        };

        if (btnMainPlayPause != null) {
            btnMainPlayPause.setOnClickListener(playPauseClick);
        }
        if (btnMiniPlayPause != null) {
            btnMiniPlayPause.setOnClickListener(playPauseClick);
        }

        // Center 10s Rewind button
        if (btnRewind10 != null) {
            btnRewind10.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    seekBackward10();
                }
            });
        }

        // Center 10s Forward button
        if (btnForward10 != null) {
            btnForward10.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    seekForward10();
                }
            });
        }

        // 10s Slide Rewind button
        if (btnSlideRewind10 != null) {
            btnSlideRewind10.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    seekBackward10();
                }
            });
        }

        // 10s Slide Forward button
        if (btnSlideForward10 != null) {
            btnSlideForward10.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    seekForward10();
                }
            });
        }

        // Responsive Timeline Scrubbing
        if (seekBar != null) {
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                    if (fromUser && exoPlayer != null && exoPlayer.getDuration() > 0) {
                        long newPos = (progress * exoPlayer.getDuration()) / 1000;
                        if (tvCurrentTime != null) {
                            tvCurrentTime.setText(formatTime(newPos));
                        }
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar sb) {
                    isUserSeeking = true;
                    mainHandler.removeCallbacks(hideControlsRunnable);
                }

                @Override
                public void onStopTrackingTouch(SeekBar sb) {
                    isUserSeeking = false;
                    if (exoPlayer != null && exoPlayer.getDuration() > 0) {
                        long newPos = (sb.getProgress() * exoPlayer.getDuration()) / 1000;
                        exoPlayer.seekTo(newPos);
                    }
                    scheduleHideControls();
                }
            });
        }
    }

    /**
     * Sets up a persistent gesture detector on the overlay root.
     * CRITICAL FIX: The overlayRoot is NEVER set to View.GONE.
     * When hidden, the overlayRoot remains View.VISIBLE with a transparent background,
     * ensuring taps ALWAYS work to bring back controls or double-tap 10s seek!
     */
    private void setupPersistentTouchGestures() {
        if (overlayRoot == null) return;

        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                // CRITICAL: Returning true ensures subsequent touch events (tap/double-tap) are not dropped!
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                int width = overlayRoot.getWidth();
                if (width <= 0) width = 1080;

                if (e.getX() < width / 2f) {
                    // Left half: Rewind 10 Seconds
                    seekBackward10();
                } else {
                    // Right half: Forward 10 Seconds
                    seekForward10();
                }
                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                toggleControls();
                return true;
            }
        });

        overlayRoot.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (gestureDetector != null) {
                    return gestureDetector.onTouchEvent(event);
                }
                return false;
            }
        });

        // Backup direct click listener on overlayRoot
        overlayRoot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleControls();
            }
        });

        if (playerView != null) {
            playerView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleControls();
                }
            });
        }
    }

    public void playUri(String streamUrl) {
        if (exoPlayer == null || streamUrl == null || streamUrl.isEmpty()) return;
        MediaItem mediaItem = MediaItem.fromUri(Uri.parse(streamUrl));
        exoPlayer.setMediaItem(mediaItem);
        exoPlayer.prepare();
        exoPlayer.play();
    }

    public void play() {
        if (exoPlayer != null) {
            updatePlayPauseIcons(true);
            exoPlayer.play();
            scheduleHideControls();
        }
    }

    public void pause() {
        if (exoPlayer != null) {
            updatePlayPauseIcons(false);
            exoPlayer.pause();
            showControls();
        }
    }

    public void togglePlayPause() {
        if (exoPlayer == null) return;
        boolean willPlay = !exoPlayer.isPlaying();
        updatePlayPauseIcons(willPlay);
        if (willPlay) {
            exoPlayer.play();
            scheduleHideControls();
        } else {
            exoPlayer.pause();
            showControls();
        }
    }

    public void seekBackward10() {
        seekBy(-10000);
        showSeekIndicator(indicatorRewind10);
        scheduleHideControls();
    }

    public void seekForward10() {
        seekBy(10000);
        showSeekIndicator(indicatorForward10);
        scheduleHideControls();
    }

    public void seekBy(long deltaMs) {
        if (exoPlayer == null) return;
        long current = exoPlayer.getCurrentPosition();
        long duration = exoPlayer.getDuration();
        long target = Math.max(0, current + deltaMs);
        if (duration > 0) {
            target = Math.min(target, duration);
            if (seekBar != null) {
                seekBar.setProgress((int) ((target * 1000) / duration));
            }
        }
        if (tvCurrentTime != null) {
            tvCurrentTime.setText(formatTime(target));
        }
        exoPlayer.seekTo(target);
    }

    public void seekTo(long positionMs) {
        if (exoPlayer != null) {
            exoPlayer.seekTo(positionMs);
        }
    }

    public void showControls() {
        isControlsVisible = true;
        if (overlayRoot != null) {
            overlayRoot.setBackgroundColor(0x66000000); // Semi-transparent dark scrim
        }
        setChildrenVisibility(View.VISIBLE);
        scheduleHideControls();
        if (listener != null) {
            listener.onControlsVisibilityChanged(true);
        }
    }

    public void hideControls() {
        isControlsVisible = false;
        if (overlayRoot != null) {
            // Keep overlayRoot View.VISIBLE so it CONTINUES receiving touches and taps!
            overlayRoot.setBackgroundColor(0x00000000); // Fully transparent
        }
        setChildrenVisibility(View.GONE);
        if (listener != null) {
            listener.onControlsVisibilityChanged(false);
        }
    }

    public void toggleControls() {
        if (isControlsVisible) {
            hideControls();
        } else {
            showControls();
        }
    }

    private void setChildrenVisibility(int visibility) {
        if (topBar != null) topBar.setVisibility(visibility);
        if (centerControls != null) centerControls.setVisibility(visibility);
        if (bottomControls != null) bottomControls.setVisibility(visibility);
        if (slideButtonsBar != null) slideButtonsBar.setVisibility(visibility);
    }

    public void scheduleHideControls() {
        mainHandler.removeCallbacks(hideControlsRunnable);
        mainHandler.postDelayed(hideControlsRunnable, 4000);
    }

    private void updatePlayPauseIcons(boolean isPlaying) {
        if (btnMainPlayPause != null) {
            btnMainPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play_arrow);
        }
        if (btnMiniPlayPause != null) {
            btnMiniPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play_arrow);
        }
        if (tvMiniPlayPause != null) {
            tvMiniPlayPause.setText(isPlaying ? "Pause" : "Play");
        }
    }

    private void showSeekIndicator(final View indicator) {
        if (indicator == null) return;
        indicator.setVisibility(View.VISIBLE);
        indicator.setAlpha(1.0f);
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                indicator.animate()
                        .alpha(0f)
                        .setDuration(350)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                indicator.setVisibility(View.GONE);
                            }
                        })
                        .start();
            }
        }, 500);
    }

    private String formatTime(long ms) {
        int seconds = (int) ((ms / 1000) % 60);
        int minutes = (int) ((ms / (1000 * 60)) % 60);
        int hours = (int) (ms / (1000 * 60 * 60));
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        }
    }

    public void setListener(PlayerListener listener) {
        this.listener = listener;
    }

    public boolean isPlaying() {
        return exoPlayer != null && exoPlayer.isPlaying();
    }

    public boolean isControlsVisible() {
        return isControlsVisible;
    }

    public long getCurrentPosition() {
        return exoPlayer != null ? exoPlayer.getCurrentPosition() : 0;
    }

    public long getDuration() {
        return exoPlayer != null ? exoPlayer.getDuration() : 0;
    }

    public void stop() {
        if (exoPlayer != null) {
            exoPlayer.stop();
        }
    }

    public void release() {
        mainHandler.removeCallbacks(hideControlsRunnable);
        mainHandler.removeCallbacks(updateProgressRunnable);
        if (exoPlayer != null) {
            exoPlayer.stop();
            exoPlayer.release();
            exoPlayer = null;
        }
    }
}
