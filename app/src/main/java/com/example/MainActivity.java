package com.example;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.adapter.MovieCardAdapter;
import com.example.adapter.MovieGridAdapter;
import com.example.adapter.ScreenshotAdapter;
import com.example.adapter.TrendingAdapter;
import com.example.model.MovieItem;
import com.example.repository.MovieRepository;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * MainActivity delivers a Netflix-style native experience for Epic Frame:
 * featuring a cinematic Billboard hero, ranked Top 10 carousel, categorized horizontal rows,
 * live instant search, persistent bookmarks ("My List"), and bottom-sheet details.
 */
public class MainActivity extends AppCompatActivity implements MovieCardAdapter.OnMovieClickListener {
    private MovieRepository repository;

    private enum TabState { HOME, SEARCH, MY_LIST }
    private TabState currentTab = TabState.HOME;

    private SwipeRefreshLayout swipeRefreshLayout;
    private View layoutSearchView;
    private View layoutMyListView;
    private View layoutErrorOverlay;
    private ProgressBar topProgressBar;
    private android.webkit.WebView webMainBottomBanner;
    private android.webkit.WebView webHomeNativeAd;

    private LinearLayout navHome, navSearch, navMyList;
    private ImageView imgNavHome, imgNavSearch, imgNavMyList;
    private TextView tvNavHome, tvNavSearch, tvNavMyList;

    private ImageView imgHeroBackdrop;
    private TextView tvHeroTitle, tvHeroTags, tvHeroMyListLabel;
    private ImageView imgHeroMyListIcon;
    private LinearLayout btnHeroMyList, btnHeroInfo;
    private Button btnHeroPlay;
    private MovieItem currentHeroMovie;

    private LinearLayout layoutFilterChips;
    private String selectedFilter = "All";

    private TrendingAdapter trendingAdapter;
    private MovieCardAdapter actionAdapter;
    private MovieCardAdapter hollywoodAdapter;
    private MovieCardAdapter southAdapter;
    private MovieCardAdapter seriesAdapter;
    private MovieCardAdapter allAdapter;

    private EditText etSearchQuery;
    private ImageView btnClearSearch;
    private TextView tvSearchCount;
    private MovieGridAdapter searchAdapter;

    private View layoutEmptyMyList;
    private MovieGridAdapter myListAdapter;
    private RecyclerView rvSearchGrid;
    private RecyclerView rvMyListGrid;

    // Desktop Widescreen Navigation Elements
    private View layoutDesktopNav;
    private TextView tvDesktopModeBadge;
    private TextView navDesktopHome, navDesktopMovies, navDesktopSeries, navDesktopSearch, navDesktopMyList;

    private long backPressedTime = 0;
    private Toast exitToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        repository = new MovieRepository(this);

        initViews();
        setupBottomNavigation();
        setupDesktopNavigation();
        setupHeroActions();
        setupFilterChips();
        setupRecyclerViews();
        applyAdaptiveDesktopLayout();
        setupSearch();
        setupBackHandler();

        displayMoviesData(repository.getAllMovies());
        fetchFeedData();
    }

    private void initViews() {
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        layoutSearchView = findViewById(R.id.layoutSearchView);
        layoutMyListView = findViewById(R.id.layoutMyListView);
        layoutErrorOverlay = findViewById(R.id.layoutErrorOverlay);
        topProgressBar = findViewById(R.id.topProgressBar);

        // Desktop Widescreen Bar
        layoutDesktopNav = findViewById(R.id.layoutDesktopNav);
        tvDesktopModeBadge = findViewById(R.id.tvDesktopModeBadge);
        navDesktopHome = findViewById(R.id.navDesktopHome);
        navDesktopMovies = findViewById(R.id.navDesktopMovies);
        navDesktopSeries = findViewById(R.id.navDesktopSeries);
        navDesktopSearch = findViewById(R.id.navDesktopSearch);
        navDesktopMyList = findViewById(R.id.navDesktopMyList);

        ImageView btnTopRefresh = findViewById(R.id.btnTopRefresh);
        ImageView btnTopSearch = findViewById(R.id.btnTopSearch);
        btnTopRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchFeedData();
            }
        });
        btnTopSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(TabState.SEARCH);
            }
        });

        navHome = findViewById(R.id.navHome);
        navSearch = findViewById(R.id.navSearch);
        navMyList = findViewById(R.id.navMyList);
        imgNavHome = findViewById(R.id.imgNavHome);
        imgNavSearch = findViewById(R.id.imgNavSearch);
        imgNavMyList = findViewById(R.id.imgNavMyList);
        tvNavHome = findViewById(R.id.tvNavHome);
        tvNavSearch = findViewById(R.id.tvNavSearch);
        tvNavMyList = findViewById(R.id.tvNavMyList);

        imgHeroBackdrop = findViewById(R.id.imgHeroBackdrop);
        tvHeroTitle = findViewById(R.id.tvHeroTitle);
        tvHeroTags = findViewById(R.id.tvHeroTags);
        btnHeroPlay = findViewById(R.id.btnHeroPlay);
        btnHeroMyList = findViewById(R.id.btnHeroMyList);
        btnHeroInfo = findViewById(R.id.btnHeroInfo);
        imgHeroMyListIcon = findViewById(R.id.imgHeroMyListIcon);
        tvHeroMyListLabel = findViewById(R.id.tvHeroMyListLabel);

        layoutFilterChips = findViewById(R.id.layoutFilterChips);

        etSearchQuery = findViewById(R.id.etSearchQuery);
        btnClearSearch = findViewById(R.id.btnClearSearch);
        tvSearchCount = findViewById(R.id.tvSearchCount);

        layoutEmptyMyList = findViewById(R.id.layoutEmptyMyList);

        swipeRefreshLayout.setColorSchemeResources(R.color.netflix_red, R.color.netflix_gold);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                fetchFeedData();
            }
        });

        Button btnRetry = findViewById(R.id.btnRetry);
        btnRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchFeedData();
            }
        });

        // Initialize Adsterra Monetization Ads
        webMainBottomBanner = findViewById(R.id.webMainBottomBanner);
        if (webMainBottomBanner != null) {
            com.example.ads.AdsterraAdHelper.loadAd(this, webMainBottomBanner, com.example.ads.AdsterraAdHelper.BANNER_320_50_HTML);
        }

        webHomeNativeAd = findViewById(R.id.webHomeNativeAd);
        if (webHomeNativeAd != null) {
            com.example.ads.AdsterraAdHelper.loadAd(this, webHomeNativeAd, com.example.ads.AdsterraAdHelper.NATIVE_BANNER_HTML);
        }
    }

    private void setupBottomNavigation() {
        navHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(TabState.HOME);
            }
        });
        navSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(TabState.SEARCH);
            }
        });
        navMyList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchTab(TabState.MY_LIST);
            }
        });
    }

    private void setupDesktopNavigation() {
        if (navDesktopHome != null) {
            navDesktopHome.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switchTab(TabState.HOME);
                }
            });
        }
        if (navDesktopMovies != null) {
            navDesktopMovies.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switchTab(TabState.HOME);
                    selectedFilter = "All";
                    setupFilterChips();
                    filterHomeContent("All");
                }
            });
        }
        if (navDesktopSeries != null) {
            navDesktopSeries.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switchTab(TabState.HOME);
                    selectedFilter = "Webseries";
                    setupFilterChips();
                    filterHomeContent("Webseries");
                }
            });
        }
        if (navDesktopSearch != null) {
            navDesktopSearch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switchTab(TabState.SEARCH);
                }
            });
        }
        if (navDesktopMyList != null) {
            navDesktopMyList.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switchTab(TabState.MY_LIST);
                }
            });
        }
    }

    private void switchTab(TabState tab) {
        currentTab = tab;
        swipeRefreshLayout.setVisibility(tab == TabState.HOME ? View.VISIBLE : View.GONE);
        layoutSearchView.setVisibility(tab == TabState.SEARCH ? View.VISIBLE : View.GONE);
        layoutMyListView.setVisibility(tab == TabState.MY_LIST ? View.VISIBLE : View.GONE);
        layoutErrorOverlay.setVisibility(View.GONE);

        int red = ContextCompat.getColor(this, R.color.netflix_red);
        int white = ContextCompat.getColor(this, R.color.white);
        int secColor = ContextCompat.getColor(this, R.color.text_secondary);

        imgNavHome.setColorFilter(tab == TabState.HOME ? red : white);
        imgNavHome.setAlpha(tab == TabState.HOME ? 1.0f : 0.6f);
        tvNavHome.setTextColor(tab == TabState.HOME ? red : white);
        tvNavHome.setAlpha(tab == TabState.HOME ? 1.0f : 0.6f);

        imgNavSearch.setColorFilter(tab == TabState.SEARCH ? red : white);
        imgNavSearch.setAlpha(tab == TabState.SEARCH ? 1.0f : 0.6f);
        tvNavSearch.setTextColor(tab == TabState.SEARCH ? red : white);
        tvNavSearch.setAlpha(tab == TabState.SEARCH ? 1.0f : 0.6f);

        imgNavMyList.setColorFilter(tab == TabState.MY_LIST ? red : white);
        imgNavMyList.setAlpha(tab == TabState.MY_LIST ? 1.0f : 0.6f);
        tvNavMyList.setTextColor(tab == TabState.MY_LIST ? red : white);
        tvNavMyList.setAlpha(tab == TabState.MY_LIST ? 1.0f : 0.6f);

        // Update Desktop Navigation tabs styling
        if (navDesktopHome != null) {
            navDesktopHome.setTextColor(tab == TabState.HOME ? red : secColor);
        }
        if (navDesktopSearch != null) {
            navDesktopSearch.setTextColor(tab == TabState.SEARCH ? red : secColor);
        }
        if (navDesktopMyList != null) {
            navDesktopMyList.setTextColor(tab == TabState.MY_LIST ? red : secColor);
        }

        if (tab == TabState.SEARCH) {
            etSearchQuery.requestFocus();
            performSearch(etSearchQuery.getText().toString());
        } else if (tab == TabState.MY_LIST) {
            loadMyListScreen();
        }
    }

    private void setupHeroActions() {
        btnHeroPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentHeroMovie != null) {
                    openMovieDirectPlay(currentHeroMovie, null);
                }
            }
        });

        btnHeroInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentHeroMovie != null) {
                    onMovieClick(currentHeroMovie);
                }
            }
        });

        btnHeroMyList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentHeroMovie != null) {
                    boolean isAdded = repository.toggleBookmark(currentHeroMovie);
                    updateHeroMyListButton(isAdded);
                    Toast.makeText(MainActivity.this,
                            isAdded ? "Added to My List" : "Removed from My List",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateHeroMyListButton(boolean isBookmarked) {
        if (isBookmarked) {
            imgHeroMyListIcon.setImageResource(R.drawable.ic_check);
            tvHeroMyListLabel.setText("In List");
            tvHeroMyListLabel.setTextColor(ContextCompat.getColor(this, R.color.netflix_red));
        } else {
            imgHeroMyListIcon.setImageResource(R.drawable.ic_add);
            tvHeroMyListLabel.setText("My List");
            tvHeroMyListLabel.setTextColor(ContextCompat.getColor(this, R.color.white));
        }
    }

    private void setupFilterChips() {
        layoutFilterChips.removeAllViews();
        List<String> categories = Arrays.asList("All", "2026", "Action", "Hollywood", "South", "Webseries", "Thriller", "Comedy");
        for (final String cat : categories) {
            final TextView chip = new TextView(this);
            chip.setText(cat.equalsIgnoreCase("South") ? "South Indian" : cat);
            chip.setTextSize(13);
            chip.setTypeface(null, android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 16, 0);
            chip.setLayoutParams(lp);
            updateChipStyle(chip, cat.equals(selectedFilter));

            chip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedFilter = cat;
                    setupFilterChips();
                    filterHomeContent(selectedFilter);
                }
            });
            layoutFilterChips.addView(chip);
        }
    }

    private void updateChipStyle(TextView chip, boolean isSelected) {
        if (isSelected) {
            chip.setBackgroundResource(R.drawable.bg_pill_filter_selected);
            chip.setTextColor(ContextCompat.getColor(this, R.color.white));
        } else {
            chip.setBackgroundResource(R.drawable.bg_pill_filter_unselected);
            chip.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        }
    }

    private void filterHomeContent(String filter) {
        if (filter.equalsIgnoreCase("All")) {
            displayMoviesData(repository.getAllMovies());
        } else {
            List<MovieItem> filtered = repository.getMoviesByFilter(filter);
            displayMoviesData(filtered);
            Toast.makeText(this, "Showing " + filter + " (" + filtered.size() + ")", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupRecyclerViews() {
        RecyclerView rvTrending = findViewById(R.id.rvTrending);
        rvTrending.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        trendingAdapter = new TrendingAdapter(this, this);
        rvTrending.setAdapter(trendingAdapter);

        RecyclerView rvAction = findViewById(R.id.rvAction);
        rvAction.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        actionAdapter = new MovieCardAdapter(this, this);
        rvAction.setAdapter(actionAdapter);

        RecyclerView rvHollywood = findViewById(R.id.rvHollywood);
        rvHollywood.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        hollywoodAdapter = new MovieCardAdapter(this, this);
        rvHollywood.setAdapter(hollywoodAdapter);

        RecyclerView rvSouth = findViewById(R.id.rvSouth);
        rvSouth.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        southAdapter = new MovieCardAdapter(this, this);
        rvSouth.setAdapter(southAdapter);

        RecyclerView rvSeries = findViewById(R.id.rvSeries);
        rvSeries.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        seriesAdapter = new MovieCardAdapter(this, this);
        rvSeries.setAdapter(seriesAdapter);

        RecyclerView rvAll = findViewById(R.id.rvAll);
        rvAll.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        allAdapter = new MovieCardAdapter(this, this);
        rvAll.setAdapter(allAdapter);

        rvSearchGrid = findViewById(R.id.rvSearchGrid);
        rvSearchGrid.setLayoutManager(new GridLayoutManager(this, calculateSpanCount()));
        searchAdapter = new MovieGridAdapter(this, this);
        rvSearchGrid.setAdapter(searchAdapter);

        rvMyListGrid = findViewById(R.id.rvMyListGrid);
        rvMyListGrid.setLayoutManager(new GridLayoutManager(this, calculateSpanCount()));
        myListAdapter = new MovieGridAdapter(this, this);
        rvMyListGrid.setAdapter(myListAdapter);
    }

    private int calculateSpanCount() {
        int widthDp = getResources().getConfiguration().screenWidthDp;
        if (widthDp >= 1200) {
            return 6; // Desktop Ultra-Wide
        } else if (widthDp >= 840) {
            return 5; // Desktop / Large Tablet
        } else if (widthDp >= 600) {
            return 4; // Tablet / Foldable
        } else {
            return 3; // Mobile Portrait
        }
    }

    private void applyAdaptiveDesktopLayout() {
        int widthDp = getResources().getConfiguration().screenWidthDp;
        boolean isDesktopOrWide = widthDp >= 600;

        if (layoutDesktopNav != null) {
            layoutDesktopNav.setVisibility(isDesktopOrWide ? View.VISIBLE : View.GONE);
        }
        if (tvDesktopModeBadge != null) {
            tvDesktopModeBadge.setVisibility(isDesktopOrWide ? View.VISIBLE : View.GONE);
        }
        View bottomNav = findViewById(R.id.bottomNavBar);
        if (bottomNav != null) {
            bottomNav.setVisibility(isDesktopOrWide ? View.GONE : View.VISIBLE);
        }

        int span = calculateSpanCount();
        if (rvSearchGrid != null) {
            rvSearchGrid.setLayoutManager(new GridLayoutManager(this, span));
        }
        if (rvMyListGrid != null) {
            rvMyListGrid.setLayoutManager(new GridLayoutManager(this, span));
        }
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        applyAdaptiveDesktopLayout();
    }

    @Override
    public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
        if (event.isCtrlPressed() && keyCode == android.view.KeyEvent.KEYCODE_F) {
            switchTab(TabState.SEARCH);
            return true;
        }
        if (keyCode == android.view.KeyEvent.KEYCODE_ESCAPE) {
            if (currentTab != TabState.HOME) {
                switchTab(TabState.HOME);
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void setupSearch() {
        btnClearSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etSearchQuery.setText("");
            }
        });

        etSearchQuery.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String q = s.toString();
                btnClearSearch.setVisibility(q.isEmpty() ? View.GONE : View.VISIBLE);
                performSearch(q);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void performSearch(String query) {
        List<MovieItem> results = repository.searchMovies(query);
        searchAdapter.updateList(results);
        if (query.trim().isEmpty()) {
            tvSearchCount.setText("Explore All " + results.size() + " Releases on Epic Frame");
        } else {
            tvSearchCount.setText("Found " + results.size() + " result" + (results.size() == 1 ? "" : "s") + " for \"" + query + "\"");
        }
    }

    private void loadMyListScreen() {
        List<MovieItem> bookmarks = repository.getMyList();
        myListAdapter.updateList(bookmarks);
        layoutEmptyMyList.setVisibility(bookmarks.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void fetchFeedData() {
        topProgressBar.setVisibility(View.VISIBLE);
        repository.fetchFeed(new MovieRepository.FeedCallback() {
            @Override
            public void onSuccess(List<MovieItem> movies) {
                topProgressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
                layoutErrorOverlay.setVisibility(View.GONE);
                displayMoviesData(movies);
                if (currentTab == TabState.MY_LIST) {
                    loadMyListScreen();
                } else if (currentTab == TabState.SEARCH) {
                    performSearch(etSearchQuery.getText().toString());
                }
            }

            @Override
            public void onError(String errorMessage) {
                topProgressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
                if (repository.getAllMovies().isEmpty()) {
                    layoutErrorOverlay.setVisibility(View.VISIBLE);
                } else {
                    Toast.makeText(MainActivity.this, "Offline Mode: Showing cached posts", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void displayMoviesData(List<MovieItem> movies) {
        if (movies == null || movies.isEmpty()) return;

        currentHeroMovie = movies.get(0);
        tvHeroTitle.setText(currentHeroMovie.getCleanTitle());
        tvHeroTags.setText(currentHeroMovie.getYear() + " • " + currentHeroMovie.getLanguage() + " • " + currentHeroMovie.getQuality());

        if (currentHeroMovie.getBackdropUrl() != null && !currentHeroMovie.getBackdropUrl().isEmpty()) {
            Glide.with(this)
                    .load(currentHeroMovie.getBackdropUrl())
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(R.drawable.bg_card_movie)
                    .into(imgHeroBackdrop);
        }
        updateHeroMyListButton(currentHeroMovie.isBookmarked());

        trendingAdapter.updateList(repository.getTrendingMovies());
        actionAdapter.updateList(repository.getMoviesByFilter("Action"));
        hollywoodAdapter.updateList(repository.getMoviesByFilter("Hollywood"));
        southAdapter.updateList(repository.getMoviesByFilter("South"));
        seriesAdapter.updateList(repository.getMoviesByFilter("Webseries"));
        allAdapter.updateList(movies);
    }

    @Override
    public void onMovieClick(final MovieItem movie) {
        showMovieDetailSheet(movie);
    }

    private void showMovieDetailSheet(final MovieItem movie) {
        if (movie == null) return;
        final BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.Theme_EpicFrame);
        View sheetView = LayoutInflater.from(this).inflate(R.layout.dialog_movie_detail, null);
        dialog.setContentView(sheetView);

        ImageView imgBackdrop = sheetView.findViewById(R.id.imgDetailBackdrop);
        TextView tvQuality = sheetView.findViewById(R.id.tvDetailQualityBadge);
        TextView tvTitle = sheetView.findViewById(R.id.tvDetailTitle);
        TextView tvYear = sheetView.findViewById(R.id.tvDetailYear);
        TextView tvDuration = sheetView.findViewById(R.id.tvDetailDuration);
        TextView tvLanguage = sheetView.findViewById(R.id.tvDetailLanguage);
        TextView tvGenres = sheetView.findViewById(R.id.tvDetailGenres);
        TextView tvSynopsis = sheetView.findViewById(R.id.tvDetailSynopsis);
        TextView tvStarcast = sheetView.findViewById(R.id.tvDetailStarcast);
        LinearLayout layoutQualityPills = sheetView.findViewById(R.id.layoutDetailQualityPills);
        Button btnPlay = sheetView.findViewById(R.id.btnDetailPlay);
        FrameLayout btnOverlayPlay = sheetView.findViewById(R.id.btnOverlayPlay);
        LinearLayout btnMyList = sheetView.findViewById(R.id.btnToggleMyList);
        final ImageView imgMyListIcon = sheetView.findViewById(R.id.imgMyListIcon);
        final TextView tvMyListText = sheetView.findViewById(R.id.tvMyListText);
        LinearLayout btnShare = sheetView.findViewById(R.id.btnDetailShare);
        LinearLayout btnWebArticle = sheetView.findViewById(R.id.btnDetailWeb);
        ImageView btnClose = sheetView.findViewById(R.id.btnCloseDetail);
        TextView tvScreenshotsHeader = sheetView.findViewById(R.id.tvScreenshotsHeader);
        RecyclerView rvScreenshots = sheetView.findViewById(R.id.rvScreenshots);
        WebView webDetailNativeAd = sheetView.findViewById(R.id.webDetailNativeAd);
        if (webDetailNativeAd != null) {
            com.example.ads.AdsterraAdHelper.loadAd(this, webDetailNativeAd, com.example.ads.AdsterraAdHelper.NATIVE_BANNER_HTML);
        }

        tvTitle.setText(movie.getCleanTitle());
        tvYear.setText(movie.getYear());
        if (tvDuration != null) {
            tvDuration.setText(movie.getDuration());
        }
        tvLanguage.setText(movie.getLanguage());
        tvQuality.setText(movie.getQuality());
        if (tvStarcast != null) {
            tvStarcast.setText(movie.getStarcast().isEmpty() ? "Starring Popular Cast" : movie.getStarcast());
        }

        StringBuilder genresStr = new StringBuilder();
        for (int i = 0; i < movie.getGenres().size(); i++) {
            genresStr.append(movie.getGenres().get(i));
            if (i < movie.getGenres().size() - 1) genresStr.append(", ");
        }
        tvGenres.setText(genresStr.toString());
        tvSynopsis.setText(movie.getSynopsis());

        if (movie.getBackdropUrl() != null && !movie.getBackdropUrl().isEmpty()) {
            Glide.with(this)
                    .load(movie.getBackdropUrl())
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(R.drawable.bg_card_movie)
                    .into(imgBackdrop);
        }

        final boolean isBookmarked = repository.isBookmarked(movie.getId());
        updateSheetBookmarkState(imgMyListIcon, tvMyListText, isBookmarked);

        btnMyList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean newState = repository.toggleBookmark(movie);
                updateSheetBookmarkState(imgMyListIcon, tvMyListText, newState);
                if (currentHeroMovie != null && currentHeroMovie.getId().equals(movie.getId())) {
                    updateHeroMyListButton(newState);
                }
                if (currentTab == TabState.MY_LIST) {
                    loadMyListScreen();
                }
                Toast.makeText(MainActivity.this,
                        newState ? "Added to My List" : "Removed from My List",
                        Toast.LENGTH_SHORT).show();
            }
        });

        View.OnClickListener directPlayListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                openMovieDirectPlay(movie, null);
            }
        };
        btnPlay.setOnClickListener(directPlayListener);
        btnOverlayPlay.setOnClickListener(directPlayListener);

        if (layoutQualityPills != null) {
            layoutQualityPills.removeAllViews();
            if (movie.getStreamOptions() != null && !movie.getStreamOptions().isEmpty()) {
                for (final MovieItem.StreamOption opt : movie.getStreamOptions()) {
                    Button pillBtn = new Button(this);
                    pillBtn.setText("⚡ Direct Stream: " + opt.getQuality() + "  (" + opt.getLabel().replace(" ", "").trim() + ")");
                    pillBtn.setTextSize(13);
                    pillBtn.setTypeface(null, android.graphics.Typeface.BOLD);
                    pillBtn.setAllCaps(false);
                    pillBtn.setBackgroundResource(R.drawable.bg_pill_filter_unselected);
                    pillBtn.setTextColor(ContextCompat.getColor(this, R.color.white));
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            (int) (44 * getResources().getDisplayMetrics().density));
                    lp.setMargins(0, 0, 0, (int) (8 * getResources().getDisplayMetrics().density));
                    pillBtn.setLayoutParams(lp);
                    pillBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                            openMovieDirectPlay(movie, opt.getUrl());
                        }
                    });
                    layoutQualityPills.addView(pillBtn);
                }
            }
        }

        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareMovie(movie);
            }
        });

        if (btnWebArticle != null) {
            btnWebArticle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                    openMovieWeb(movie);
                }
            });
        }

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        if (movie.getScreenshots() != null && !movie.getScreenshots().isEmpty()) {
            tvScreenshotsHeader.setVisibility(View.VISIBLE);
            rvScreenshots.setVisibility(View.VISIBLE);
            rvScreenshots.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            ScreenshotAdapter ssAdapter = new ScreenshotAdapter(this);
            ssAdapter.updateList(movie.getScreenshots());
            rvScreenshots.setAdapter(ssAdapter);
        } else {
            tvScreenshotsHeader.setVisibility(View.GONE);
            rvScreenshots.setVisibility(View.GONE);
        }

        dialog.show();
    }

    private void updateSheetBookmarkState(ImageView icon, TextView label, boolean isBookmarked) {
        if (isBookmarked) {
            icon.setImageResource(R.drawable.ic_check);
            label.setText("In My List");
            label.setTextColor(ContextCompat.getColor(this, R.color.netflix_red));
        } else {
            icon.setImageResource(R.drawable.ic_add);
            label.setText("My List");
            label.setTextColor(ContextCompat.getColor(this, R.color.white));
        }
    }

    private void openMovieDirectPlay(MovieItem movie, String streamUrl) {
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra(PlayerActivity.EXTRA_MOVIE, movie);
        if (streamUrl != null) {
            intent.putExtra(PlayerActivity.EXTRA_STREAM_URL, streamUrl);
        }
        startActivity(intent);
    }

    private void openMovieWeb(MovieItem movie) {
        Intent intent = new Intent(this, WebActivity.class);
        intent.putExtra(WebActivity.EXTRA_URL, movie.getPostUrl());
        intent.putExtra(WebActivity.EXTRA_TITLE, movie.getCleanTitle());
        startActivity(intent);
    }

    private void shareMovie(MovieItem movie) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        String shareBody = "Check out \"" + movie.getCleanTitle() + "\" on Epic Frame:\n" + movie.getPostUrl();
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, movie.getCleanTitle());
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(shareIntent, "Share with"));
    }

    private void setupBackHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (currentTab != TabState.HOME) {
                    switchTab(TabState.HOME);
                } else {
                    if (backPressedTime + 2000 > System.currentTimeMillis()) {
                        if (exitToast != null) {
                            exitToast.cancel();
                        }
                        setEnabled(false);
                        getOnBackPressedDispatcher().onBackPressed();
                    } else {
                        exitToast = Toast.makeText(MainActivity.this, "Press back again to exit", Toast.LENGTH_SHORT);
                        exitToast.show();
                        backPressedTime = System.currentTimeMillis();
                    }
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (webMainBottomBanner != null) {
            webMainBottomBanner.stopLoading();
            webMainBottomBanner.destroy();
        }
        if (webHomeNativeAd != null) {
            webHomeNativeAd.stopLoading();
            webHomeNativeAd.destroy();
        }
        super.onDestroy();
    }
}
