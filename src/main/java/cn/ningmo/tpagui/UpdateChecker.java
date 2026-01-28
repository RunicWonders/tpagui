package cn.ningmo.tpagui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 更新检查器
 * 检查GitHub Releases获取最新版本
 */
public class UpdateChecker {
    private static final String GITHUB_API_URL = "https://api.github.com/repos/RunicWonders/tpagui/releases";
    private static final String GITHUB_RELEASES_URL = "https://github.com/RunicWonders/tpagui/releases";
    
    private final String currentVersion;
    private final java.util.function.Consumer<String> logger;
    private final java.util.function.Function<String, String> i18n;
    private final java.util.function.Supplier<Boolean> isEnabled;
    private String latestVersion;
    private String downloadUrl;
    
    public UpdateChecker(String currentVersion, java.util.function.Consumer<String> logger, 
                        java.util.function.Function<String, String> i18n, 
                        java.util.function.Supplier<Boolean> isEnabled) {
        this.currentVersion = currentVersion;
        this.logger = logger;
        this.i18n = i18n;
        this.isEnabled = isEnabled;
    }
    
    /**
     * 兼容 Bukkit 的构造函数
     */
    public UpdateChecker(TpaGui plugin) {
        this(
            plugin.getDescription().getVersion(),
            msg -> plugin.getLogger().warning(msg),
            key -> plugin.getLogMessage(key),
            () -> plugin.getConfig().getBoolean("update-check.enabled", true)
        );
    }
    
    /**
     * 异步检查更新
     * @return CompletableFuture<Boolean> true表示有新版本
     */
    public CompletableFuture<Boolean> checkForUpdates() {
        return CompletableFuture.supplyAsync(() -> {
            if (!isEnabled.get()) {
                return false;
            }
            
            try {
                URL url = new URL(GITHUB_API_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 TpaGui-UpdateChecker");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                
                int responseCode = connection.getResponseCode();
                if (responseCode == 403) {
                    // 处理频率限制
                    String rateLimitLimit = connection.getHeaderField("X-RateLimit-Limit");
                    String rateLimitRemaining = connection.getHeaderField("X-RateLimit-Remaining");
                    logger.accept(
                        i18n.apply("update-check-failed").replace("{error}", 
                            "HTTP 403 (Rate Limited? Limit: " + rateLimitLimit + ", Remaining: " + rateLimitRemaining + ")")
                    );
                    return false;
                }
                
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    logger.accept(
                        i18n.apply("update-check-failed").replace("{error}", "HTTP " + responseCode)
                    );
                    return false;
                }
                
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    
                    JsonParser parser = new JsonParser();
                    com.google.gson.JsonArray releases = parser.parse(response.toString()).getAsJsonArray();
                    
                    if (releases.size() == 0) return false;

                    boolean includePrerelease = isPrerelease(currentVersion);
                    JsonObject bestRelease = null;
                    String bestVersion = null;

                    for (int i = 0; i < releases.size(); i++) {
                        JsonObject release = releases.get(i).getAsJsonObject();
                        String tagName = release.get("tag_name").getAsString();
                        String version = tagName.startsWith("v") ? tagName.substring(1) : tagName;
                        boolean isPrerelease = release.get("prerelease").getAsBoolean() || isPrerelease(version);

                        // 如果当前是稳定版，只看新的稳定版
                        // 如果当前是预发布版，可以看新的预发布版或稳定版
                        if (!includePrerelease && isPrerelease) {
                            continue;
                        }

                        if (bestVersion == null || compareVersions(version, bestVersion) > 0) {
                            bestVersion = version;
                            bestRelease = release;
                        }
                    }

                    if (bestVersion != null && compareVersions(bestVersion, currentVersion) > 0) {
                        latestVersion = bestVersion;
                        // 获取下载URL
                        if (bestRelease.has("assets") && bestRelease.get("assets").getAsJsonArray().size() > 0) {
                            JsonObject asset = bestRelease.get("assets").getAsJsonArray().get(0).getAsJsonObject();
                            downloadUrl = asset.get("browser_download_url").getAsString();
                        } else {
                            downloadUrl = bestRelease.get("html_url").getAsString();
                        }
                        return true;
                    }
                    
                    return false;
                }
            } catch (Exception e) {
                logger.accept(
                    i18n.apply("update-check-error")
                        .replace("{error}", e.getMessage())
                );
                return false;
            }
        });
    }

    private boolean isPrerelease(String version) {
        return version.contains("-") || version.toLowerCase().contains("alpha") || 
               version.toLowerCase().contains("beta") || version.toLowerCase().contains("rc");
    }

    /**
     * 比较两个版本号
     * @return 1 if v1 > v2, -1 if v1 < v2, 0 if v1 == v2
     */
    public int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("[-.]");
        String[] parts2 = v2.split("[-.]");
        
        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            // 稳定版部分 (major.minor.patch)
            if (i < 3) {
                int p1 = getSafeInt(parts1, i);
                int p2 = getSafeInt(parts2, i);
                if (p1 > p2) return 1;
                if (p1 < p2) return -1;
            } else {
                // 预发布版部分
                if (i >= parts1.length) return 1; // v2 有后缀，v1 没有，v1 更接近稳定版或就是稳定版
                if (i >= parts2.length) return -1; // v1 有后缀，v2 没有
                
                String s1 = parts1[i].toLowerCase();
                String s2 = parts2[i].toLowerCase();
                
                // 尝试作为数字比较
                try {
                    int n1 = Integer.parseInt(s1.replaceAll("\\D", ""));
                    int n2 = Integer.parseInt(s2.replaceAll("\\D", ""));
                    if (n1 > n2) return 1;
                    if (n1 < n2) return -1;
                } catch (NumberFormatException ignored) {
                    // 否则作为字符串比较
                    int cmp = s1.compareTo(s2);
                    if (cmp != 0) return cmp;
                }
            }
        }
        return 0;
    }

    private int getSafeInt(String[] parts, int index) {
        if (index >= parts.length) return 0;
        try {
            return Integer.parseInt(parts[index].replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    /**
     * 获取最新版本号
     * @return 最新版本号
     */
    public String getLatestVersion() {
        return latestVersion;
    }
    
    /**
     * 获取下载URL
     * @return 下载URL
     */
    public String getDownloadUrl() {
        return downloadUrl != null ? downloadUrl : GITHUB_RELEASES_URL;
    }
    
    /**
     * 获取当前版本号
     * @return 当前版本号
     */
    public String getCurrentVersion() {
        return currentVersion;
    }
    
    /**
     * 显示更新通知
     */
    public void notifyUpdate() {
        if (latestVersion == null) {
            return;
        }
        
        String message = i18n.apply("update-available")
            .replace("{current}", currentVersion)
            .replace("{latest}", latestVersion)
            .replace("{url}", getDownloadUrl());
        
        logger.accept("==========================================");
        logger.accept(message);
        logger.accept("==========================================");
    }
}

