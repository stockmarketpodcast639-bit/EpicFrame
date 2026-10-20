package com.example.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a Movie or Series post fetched from the Epic Frame Blogger website,
 * including direct streaming/download links, duration, starcast, and media metadata.
 */
public class MovieItem implements Serializable {

    public static class StreamOption implements Serializable {
        private String label;
        private String url;
        private String quality;

        public StreamOption(String label, String url, String quality) {
            this.label = label;
            this.url = url;
            this.quality = quality;
        }

        public String getLabel() { return label; }
        public String getUrl() { return url; }
        public String getQuality() { return quality; }
    }

    private String id;
    private String title;
    private String cleanTitle;
    private String year;
    private String quality;
    private String language;
    private String duration = "2h 15min";
    private String starcast = "";
    private String size = "";
    private String posterUrl;
    private String backdropUrl;
    private List<String> screenshots = new ArrayList<>();
    private List<String> genres = new ArrayList<>();
    private List<StreamOption> streamOptions = new ArrayList<>();
    private String synopsis;
    private String publishedDate;
    private String postUrl;
    private boolean isBookmarked;

    public MovieItem() {
    }

    public static MovieItem fromBloggerJson(String id, String rawTitle, String contentHtml,
                                           List<String> categories, String postUrl, String publishedDate) {
        MovieItem item = new MovieItem();
        item.id = id != null ? id : String.valueOf(System.currentTimeMillis());
        item.title = rawTitle != null ? rawTitle.trim() : "Untitled";
        item.postUrl = postUrl != null ? postUrl : "https://epicframe20.blogspot.com/";
        item.publishedDate = publishedDate != null ? publishedDate : "";
        item.genres = categories != null ? new ArrayList<>(categories) : new ArrayList<>();

        item.extractMetadata(rawTitle);
        item.parseContentHtml(contentHtml);
        return item;
    }

    private void extractMetadata(String rawTitle) {
        if (rawTitle == null || rawTitle.isEmpty()) {
            this.cleanTitle = "Epic Frame Movie";
            this.year = "2026";
            this.quality = "HD";
            this.language = "Hindi Dubbed";
            return;
        }

        // Year extraction
        Pattern yearPattern = Pattern.compile("\\b(202[0-9]|201[0-9]|199[0-9])\\b");
        Matcher yearMatcher = yearPattern.matcher(rawTitle);
        if (yearMatcher.find()) {
            this.year = yearMatcher.group(1);
        } else {
            this.year = "2026";
        }

        // Quality extraction
        if (rawTitle.toUpperCase().contains("HQCAM") || rawTitle.toUpperCase().contains("HQ CAM")) {
            this.quality = "HQCam";
        } else if (rawTitle.toUpperCase().contains("1080P") || rawTitle.toUpperCase().contains("FHD")) {
            this.quality = "1080p FHD";
        } else if (rawTitle.toUpperCase().contains("4K") || rawTitle.toUpperCase().contains("UHD")) {
            this.quality = "4K UHD";
        } else if (rawTitle.toUpperCase().contains("720P")) {
            this.quality = "720p HD";
        } else if (rawTitle.toUpperCase().contains("ESUB") || rawTitle.toUpperCase().contains("HD")) {
            this.quality = "HD";
        } else {
            this.quality = "HD";
        }

        // Language extraction
        if (rawTitle.toUpperCase().contains("DUAL AUDIO")) {
            this.language = "Dual Audio";
        } else if (rawTitle.toUpperCase().contains("HINDI DUBBED") || rawTitle.toUpperCase().contains("HINDI")) {
            this.language = "Hindi Dubbed";
        } else if (rawTitle.toUpperCase().contains("ENGLISH")) {
            this.language = "English";
        } else {
            this.language = "Multi-Audio";
        }

        // Clean title
        String clean = rawTitle;
        clean = clean.replaceAll("\\((.*?)\\)", "");
        clean = clean.replaceAll("\\[(.*?)\\]", "");
        clean = clean.replaceAll("(?i)\\b(South|Hindi|Dubbed|Movie|HQCam|HD|ESub|Dual Audio|Hollywood|Bollywood|UnCut|MCU)\\b", "");
        clean = clean.replaceAll("[- _:]+", " ").trim();
        clean = clean.replaceAll("\\s+", " ").trim();
        if (clean.length() < 2) {
            clean = rawTitle.replaceAll("\\((.*?)\\)", "").trim();
        }
        this.cleanTitle = clean;
    }

    private void parseContentHtml(String html) {
        if (html == null || html.isEmpty()) return;

        // 1. Extract direct stream/download button links
        Pattern btnPattern = Pattern.compile("window\\.location\\.href=['\"]([^'\"]+)['\"];?[^>]*>([^<]+)</button>", Pattern.CASE_INSENSITIVE);
        Matcher btnMatcher = btnPattern.matcher(html);
        while (btnMatcher.find()) {
            String url = btnMatcher.group(1);
            String rawLabel = btnMatcher.group(2).trim();
            String qualityTag = "720p HD";
            if (rawLabel.contains("1080")) {
                qualityTag = "1080p FHD";
            } else if (rawLabel.contains("720")) {
                qualityTag = "720p HD";
            } else if (rawLabel.contains("480")) {
                qualityTag = "480p SD";
            }
            streamOptions.add(new StreamOption(rawLabel, url, qualityTag));
        }

        // 2. Extract Duration, Starcast, Size
        Pattern durPattern = Pattern.compile("Duration:</strong>\\s*([^<]+)", Pattern.CASE_INSENSITIVE);
        Matcher durMatcher = durPattern.matcher(html);
        if (durMatcher.find()) {
            this.duration = durMatcher.group(1).trim();
        }

        Pattern castPattern = Pattern.compile("Starcast:</strong>\\s*([^<]+)", Pattern.CASE_INSENSITIVE);
        Matcher castMatcher = castPattern.matcher(html);
        if (castMatcher.find()) {
            this.starcast = castMatcher.group(1).trim();
        }

        Pattern sizePattern = Pattern.compile("Size:</strong>\\s*([^<]+)", Pattern.CASE_INSENSITIVE);
        Matcher sizeMatcher = sizePattern.matcher(html);
        if (sizeMatcher.find()) {
            this.size = sizeMatcher.group(1).trim();
        }

        // 3. Extract all <img src="..."> URLs
        Pattern imgPattern = Pattern.compile("<img[^>]+src=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
        Matcher matcher = imgPattern.matcher(html);
        List<String> images = new ArrayList<>();
        while (matcher.find()) {
            String imgUrl = matcher.group(1);
            if (imgUrl != null && !imgUrl.isEmpty() && !imgUrl.contains("blank.gif")) {
                String highResUrl = imgUrl.replaceAll("/s[0-9]+(-[a-z])?/", "/s1600/");
                images.add(highResUrl);
            }
        }

        if (!images.isEmpty()) {
            this.posterUrl = images.get(0);
            this.backdropUrl = images.get(0);
            if (images.size() > 1) {
                for (int i = 1; i < images.size(); i++) {
                    this.screenshots.add(images.get(i));
                }
            }
        }

        // 4. Extract clean text synopsis
        String text = html.replaceAll("(?s)<style.*?>.*?</style>", "");
        text = text.replaceAll("(?s)<script.*?>.*?</script>", "");
        text = text.replaceAll("<[^>]+>", " ");
        text = text.replaceAll("&nbsp;", " ")
                .replaceAll("&amp;", "&")
                .replaceAll("&quot;", "\"")
                .replaceAll("&#39;", "'")
                .replaceAll("\\s+", " ")
                .trim();
        if (text.length() > 380) {
            this.synopsis = text.substring(0, 380) + "...";
        } else if (!text.isEmpty()) {
            this.synopsis = text;
        } else {
            this.synopsis = "Experience the thrilling cinematic journey of " + cleanTitle + ", available in " + language + " " + quality + ". Stream or download details directly on Epic Frame.";
        }
    }

    public String getBestStreamUrl() {
        if (streamOptions != null && !streamOptions.isEmpty()) {
            for (StreamOption opt : streamOptions) {
                if (opt.getQuality().contains("1080")) {
                    return opt.getUrl();
                }
            }
            for (StreamOption opt : streamOptions) {
                if (opt.getQuality().contains("720")) {
                    return opt.getUrl();
                }
            }
            return streamOptions.get(0).getUrl();
        }
        return postUrl;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getCleanTitle() { return cleanTitle != null ? cleanTitle : title; }
    public String getYear() { return year; }
    public String getQuality() { return quality; }
    public String getLanguage() { return language; }
    public String getDuration() { return duration; }
    public String getStarcast() { return starcast; }
    public String getSize() { return size; }
    public String getPosterUrl() { return posterUrl; }
    public String getBackdropUrl() { return backdropUrl != null ? backdropUrl : posterUrl; }
    public List<String> getScreenshots() { return screenshots; }
    public List<String> getGenres() { return genres; }
    public List<StreamOption> getStreamOptions() { return streamOptions; }
    public String getSynopsis() { return synopsis; }
    public String getPublishedDate() { return publishedDate; }
    public String getPostUrl() { return postUrl; }
    public boolean isBookmarked() { return isBookmarked; }
    public void setBookmarked(boolean bookmarked) { isBookmarked = bookmarked; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }
}
