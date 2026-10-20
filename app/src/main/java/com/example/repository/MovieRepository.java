package com.example.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import com.example.model.MovieItem;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * MovieRepository manages live data fetching from Epic Frame Blogger JSON feed,
 * local offline caching via SharedPreferences, and user's bookmark list ("My List").
 */
public class MovieRepository {
    public static final String BLOGGER_FEED_URL = "https://epicframe20.blogspot.com/feeds/posts/default?alt=json&max-results=50";
    private static final String PREFS_NAME = "epic_frame_cache";
    private static final String KEY_CACHED_JSON = "cached_feed_json";
    private static final String KEY_BOOKMARKS = "user_bookmarks";

    private final Context context;
    private final SharedPreferences prefs;
    private final OkHttpClient httpClient;
    private final Handler mainHandler;

    private List<MovieItem> allMovies = new ArrayList<>();
    private Set<String> bookmarkedIds = new HashSet<>();

    public interface FeedCallback {
        void onSuccess(List<MovieItem> movies);
        void onError(String errorMessage);
    }

    public MovieRepository(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.httpClient = new OkHttpClient();
        this.mainHandler = new Handler(Looper.getMainLooper());

        loadBookmarks();
        loadCachedData();
    }

    private void loadBookmarks() {
        Set<String> saved = prefs.getStringSet(KEY_BOOKMARKS, null);
        if (saved != null) {
            bookmarkedIds = new HashSet<>(saved);
        }
    }

    public boolean isBookmarked(String id) {
        return bookmarkedIds.contains(id);
    }

    public boolean toggleBookmark(MovieItem item) {
        if (item == null || item.getId() == null) return false;
        boolean newState;
        if (bookmarkedIds.contains(item.getId())) {
            bookmarkedIds.remove(item.getId());
            item.setBookmarked(false);
            newState = false;
        } else {
            bookmarkedIds.add(item.getId());
            item.setBookmarked(true);
            newState = true;
        }
        prefs.edit().putStringSet(KEY_BOOKMARKS, new HashSet<>(bookmarkedIds)).apply();
        return newState;
    }

    public List<MovieItem> getMyList() {
        List<MovieItem> myList = new ArrayList<>();
        for (MovieItem item : allMovies) {
            if (bookmarkedIds.contains(item.getId())) {
                item.setBookmarked(true);
                myList.add(item);
            }
        }
        return myList;
    }

    public List<MovieItem> getAllMovies() {
        return new ArrayList<>(allMovies);
    }

    public MovieItem getHeroMovie() {
        if (!allMovies.isEmpty()) {
            return allMovies.get(0);
        }
        return null;
    }

    public List<MovieItem> getTrendingMovies() {
        List<MovieItem> list = new ArrayList<>();
        int count = Math.min(10, allMovies.size());
        for (int i = 0; i < count; i++) {
            list.add(allMovies.get(i));
        }
        return list;
    }

    public List<MovieItem> getMoviesByFilter(String categoryFilter) {
        if (categoryFilter == null || categoryFilter.equalsIgnoreCase("All")) {
            return new ArrayList<>(allMovies);
        }
        List<MovieItem> filtered = new ArrayList<>();
        String query = categoryFilter.toLowerCase();
        for (MovieItem item : allMovies) {
            boolean matches = false;
            for (String g : item.getGenres()) {
                if (g.toLowerCase().contains(query)) {
                    matches = true;
                    break;
                }
            }
            if (!matches && item.getTitle().toLowerCase().contains(query)) {
                matches = true;
            }
            if (!matches && item.getYear().contains(query)) {
                matches = true;
            }
            if (matches) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    public List<MovieItem> searchMovies(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>(allMovies);
        }
        String q = query.trim().toLowerCase();
        List<MovieItem> results = new ArrayList<>();
        for (MovieItem item : allMovies) {
            if (item.getTitle().toLowerCase().contains(q) ||
                item.getCleanTitle().toLowerCase().contains(q) ||
                item.getYear().contains(q) ||
                item.getLanguage().toLowerCase().contains(q)) {
                results.add(item);
            } else {
                for (String g : item.getGenres()) {
                    if (g.toLowerCase().contains(q)) {
                        results.add(item);
                        break;
                    }
                }
            }
        }
        return results;
    }

    private void loadCachedData() {
        String cachedJson = prefs.getString(KEY_CACHED_JSON, null);
        if (cachedJson != null && !cachedJson.isEmpty()) {
            try {
                List<MovieItem> parsed = parseBloggerFeedJson(cachedJson);
                if (!parsed.isEmpty()) {
                    this.allMovies = parsed;
                }
            } catch (Exception ignored) {
            }
        }
    }

    public void fetchFeed(final FeedCallback callback) {
        Request request = new Request.Builder()
                .url(BLOGGER_FEED_URL)
                .header("User-Agent", "EpicFrameAndroid/2.0")
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, final IOException e) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (!allMovies.isEmpty()) {
                            callback.onSuccess(allMovies);
                        } else {
                            callback.onError("Network error: " + e.getMessage());
                        }
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (!allMovies.isEmpty()) {
                                callback.onSuccess(allMovies);
                            } else {
                                callback.onError("Failed to fetch feed from Epic Frame.");
                            }
                        }
                    });
                    return;
                }

                try {
                    String jsonString = response.body().string();
                    final List<MovieItem> parsed = parseBloggerFeedJson(jsonString);
                    prefs.edit().putString(KEY_CACHED_JSON, jsonString).apply();
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            allMovies = parsed;
                            callback.onSuccess(parsed);
                        }
                    });
                } catch (final Exception ex) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (!allMovies.isEmpty()) {
                                callback.onSuccess(allMovies);
                            } else {
                                callback.onError("Parsing error: " + ex.getMessage());
                            }
                        }
                    });
                }
            }
        });
    }

    private List<MovieItem> parseBloggerFeedJson(String jsonString) throws Exception {
        List<MovieItem> items = new ArrayList<>();
        JSONObject root = new JSONObject(jsonString);
        JSONObject feed = root.optJSONObject("feed");
        if (feed == null) return items;

        JSONArray entries = feed.optJSONArray("entry");
        if (entries == null) return items;

        for (int i = 0; i < entries.length(); i++) {
            JSONObject entry = entries.getJSONObject(i);
            String id = "";
            JSONObject idObj = entry.optJSONObject("id");
            if (idObj != null) id = idObj.optString("$t", "");

            String title = "";
            JSONObject titleObj = entry.optJSONObject("title");
            if (titleObj != null) title = titleObj.optString("$t", "");

            String published = "";
            JSONObject pubObj = entry.optJSONObject("published");
            if (pubObj != null) published = pubObj.optString("$t", "");

            String postUrl = "https://epicframe20.blogspot.com/";
            JSONArray links = entry.optJSONArray("link");
            if (links != null) {
                for (int l = 0; l < links.length(); l++) {
                    JSONObject link = links.getJSONObject(l);
                    if ("alternate".equals(link.optString("rel"))) {
                        postUrl = link.optString("href", postUrl);
                        break;
                    }
                }
            }

            List<String> categories = new ArrayList<>();
            JSONArray cats = entry.optJSONArray("category");
            if (cats != null) {
                for (int c = 0; c < cats.length(); c++) {
                    String term = cats.getJSONObject(c).optString("term");
                    if (term != null && !term.isEmpty()) {
                        categories.add(term);
                    }
                }
            }

            String contentHtml = "";
            JSONObject contentObj = entry.optJSONObject("content");
            if (contentObj != null) {
                contentHtml = contentObj.optString("$t", "");
            }

            MovieItem movie = MovieItem.fromBloggerJson(id, title, contentHtml, categories, postUrl, published);
            movie.setBookmarked(bookmarkedIds.contains(movie.getId()));
            items.add(movie);
        }
        return items;
    }
}
