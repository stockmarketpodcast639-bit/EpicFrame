package com.example.cache;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.model.MovieItem;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * OfflinePostCacheManager manages persistent disk-based caching of visited blog posts
 * so users can view and read them seamlessly even when completely disconnected from the internet.
 */
public class OfflinePostCacheManager {
    private static final String PREF_CACHE_INDEX = "epic_frame_offline_posts";

    public static void savePostHtml(Context context, String url, String htmlContent) {
        if (context == null || url == null || htmlContent == null || htmlContent.isEmpty()) return;
        try {
            String safeFileName = generateCacheKey(url);
            File cacheDir = new File(context.getCacheDir(), "offline_posts");
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
            File file = new File(cacheDir, safeFileName);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(htmlContent.getBytes(StandardCharsets.UTF_8));
            fos.flush();
            fos.close();
            SharedPreferences prefs = context.getSharedPreferences(PREF_CACHE_INDEX, Context.MODE_PRIVATE);
            prefs.edit().putLong(safeFileName, System.currentTimeMillis()).apply();
        } catch (Exception ignored) {
        }
    }

    public static String getCachedPostHtml(Context context, String url) {
        if (context == null || url == null) return null;
        try {
            String safeFileName = generateCacheKey(url);
            File file = new File(new File(context.getCacheDir(), "offline_posts"), safeFileName);
            if (file.exists() && file.length() > 0) {
                FileInputStream fis = new FileInputStream(file);
                byte[] data = new byte[(int) file.length()];
                fis.read(data);
                fis.close();
                return new String(data, StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public static boolean hasCachedPost(Context context, String url) {
        if (context == null || url == null) return false;
        String safeFileName = generateCacheKey(url);
        File file = new File(new File(context.getCacheDir(), "offline_posts"), safeFileName);
        return file.exists() && file.length() > 0;
    }

    public static String buildOfflineArticleTemplate(MovieItem movie) {
        if (movie == null) return "<html><body style='background:#121212;color:#fff;'><h3>Offline Mode</h3></body></html>";
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
        sb.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        sb.append("<style>");
        sb.append("body { background-color: #0F0F0F; color: #E5E5E5; font-family: -apple-system, Roboto, sans-serif; margin: 0; padding: 16px; line-height: 1.6; }");
        sb.append(".offline-banner { background: #E50914; color: #fff; padding: 8px 12px; border-radius: 6px; font-weight: bold; font-size: 12px; margin-bottom: 16px; display: inline-block; }");
        sb.append(".title { color: #FFFFFF; font-size: 22px; font-weight: 800; margin: 0 0 12px 0; }");
        sb.append(".poster-wrap { text-align: center; margin-bottom: 16px; }");
        sb.append(".poster { max-width: 100%; border-radius: 8px; box-shadow: 0 4px 16px rgba(0,0,0,0.8); }");
        sb.append(".meta-card { background: #1A1A1A; border: 1px solid #2A2A2A; border-radius: 8px; padding: 16px; margin-bottom: 16px; }");
        sb.append(".meta-row { margin-bottom: 8px; font-size: 14px; }");
        sb.append(".meta-label { color: #F59E0B; font-weight: bold; }");
        sb.append(".overview-title { font-size: 16px; font-weight: 700; color: #FFF; margin: 16px 0 8px 0; border-bottom: 2px solid #E50914; padding-bottom: 4px; display: inline-block; }");
        sb.append(".overview-text { color: #CCCCCC; font-size: 14px; }");
        sb.append(".screenshot-wrap { margin-top: 16px; text-align: center; }");
        sb.append(".screenshot { max-width: 100%; border-radius: 8px; margin-top: 8px; border: 1px solid #333; }");
        sb.append(".btn { background: #E50914; color: #fff; padding: 10px 16px; border-radius: 6px; text-decoration: none; display: inline-block; margin: 6px 4px 0 0; font-weight: bold; font-size: 13px; }");
        sb.append("</style></head><body>");
        sb.append("<div class='offline-banner'>⚡ OFFLINE MODE: CACHED ARTICLE</div>");
        sb.append("<h1 class='title'>").append(movie.getTitle()).append("</h1>");
        if (movie.getPosterUrl() != null && !movie.getPosterUrl().isEmpty()) {
            sb.append("<div class='poster-wrap'><img src='").append(movie.getPosterUrl()).append("' class='poster' alt='Poster' /></div>");
        }
        sb.append("<div class='meta-card'>");
        sb.append("<div class='meta-row'><span class='meta-label'>Year:</span> ").append(movie.getYear()).append("</div>");
        sb.append("<div class='meta-row'><span class='meta-label'>Duration:</span> ").append(movie.getDuration()).append("</div>");
        sb.append("<div class='meta-row'><span class='meta-label'>Language:</span> ").append(movie.getLanguage()).append("</div>");
        sb.append("<div class='meta-row'><span class='meta-label'>Quality:</span> ").append(movie.getQuality()).append("</div>");
        if (!movie.getSize().isEmpty()) {
            sb.append("<div class='meta-row'><span class='meta-label'>File Size:</span> ").append(movie.getSize()).append("</div>");
        }
        if (!movie.getStarcast().isEmpty()) {
            sb.append("<div class='meta-row'><span class='meta-label'>Starcast:</span> ").append(movie.getStarcast()).append("</div>");
        }
        sb.append("</div>");
        sb.append("<div class='overview-title'>Storyline & Overview</div>");
        sb.append("<div class='overview-text'>").append(movie.getSynopsis()).append("</div>");
        if (movie.getStreamOptions() != null && !movie.getStreamOptions().isEmpty()) {
            sb.append("<div class='overview-title' style='margin-top:20px;'>Available Downloads / Streams</div><div>");
            for (MovieItem.StreamOption opt : movie.getStreamOptions()) {
                sb.append("<span class='btn'>").append(opt.getQuality()).append(" (").append(opt.getLabel().replace(" ", "").trim()).append(")</span> ");
            }
            sb.append("</div>");
        }
        if (movie.getScreenshots() != null && !movie.getScreenshots().isEmpty()) {
            sb.append("<div class='overview-title' style='margin-top:24px;'>Movie Screenshots</div>");
            sb.append("<div class='screenshot-wrap'>");
            for (String ss : movie.getScreenshots()) {
                sb.append("<img src='").append(ss).append("' class='screenshot' alt='Screenshot' /><br>");
            }
            sb.append("</div>");
        }
        sb.append("<br><br><div style='text-align:center;color:#666;font-size:12px;'>Epic Frame 2.0 • Cached for Offline Reading</div>");
        sb.append("</body></html>");
        return sb.toString();
    }

    private static String generateCacheKey(String url) {
        return "post_" + Math.abs(url.hashCode()) + ".html";
    }
}
