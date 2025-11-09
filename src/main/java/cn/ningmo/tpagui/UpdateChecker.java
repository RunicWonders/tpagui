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
    private static final String GITHUB_API_URL = "https://api.github.com/repos/RunicWonders/tpagui/releases/latest";
    private static final String GITHUB_RELEASES_URL = "https://github.com/RunicWonders/tpagui/releases";
    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)");
    
    private final TpaGui plugin;
    private final String currentVersion;
    private String latestVersion;
    private String downloadUrl;
    
    public UpdateChecker(TpaGui plugin) {
        this.plugin = plugin;
        this.currentVersion = plugin.getDescription().getVersion();
    }
    
    /**
     * 异步检查更新
     * @return CompletableFuture<Boolean> true表示有新版本
     */
    public CompletableFuture<Boolean> checkForUpdates() {
        return CompletableFuture.supplyAsync(() -> {
            if (!plugin.getConfig().getBoolean("update-check.enabled", true)) {
                return false;
            }
            
            try {
                URL url = new URL(GITHUB_API_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setInstanceFollowRedirects(true);
                
                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    plugin.getLogger().fine(
                        plugin.getLogMessage("update-check-failed",
                            "{error}", "HTTP " + responseCode)
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
                    JsonObject json = parser.parse(response.toString()).getAsJsonObject();
                    
                    String tagName = json.get("tag_name").getAsString();
                    // 移除 "v" 前缀（如果存在）
                    latestVersion = tagName.startsWith("v") ? tagName.substring(1) : tagName;
                    
                    // 获取下载URL（从assets中获取，如果没有则使用zipball_url）
                    if (json.has("assets") && json.get("assets").getAsJsonArray().size() > 0) {
                        JsonObject asset = json.get("assets").getAsJsonArray().get(0).getAsJsonObject();
                        downloadUrl = asset.get("browser_download_url").getAsString();
                    } else {
                        downloadUrl = GITHUB_RELEASES_URL;
                    }
                    
                    // 比较版本
                    return isNewerVersion(latestVersion, currentVersion);
                }
            } catch (Exception e) {
                plugin.getLogger().fine(
                    plugin.getLogMessage("update-check-error",
                        "{error}", e.getMessage())
                );
                return false;
            }
        });
    }
    
    /**
     * 比较版本号
     * @param latest 最新版本
     * @param current 当前版本
     * @return true如果最新版本更新
     */
    private boolean isNewerVersion(String latest, String current) {
        try {
            Matcher latestMatcher = VERSION_PATTERN.matcher(latest);
            Matcher currentMatcher = VERSION_PATTERN.matcher(current);
            
            if (!latestMatcher.find() || !currentMatcher.find()) {
                return false;
            }
            
            int latestMajor = Integer.parseInt(latestMatcher.group(1));
            int latestMinor = Integer.parseInt(latestMatcher.group(2));
            int latestPatch = Integer.parseInt(latestMatcher.group(3));
            
            int currentMajor = Integer.parseInt(currentMatcher.group(1));
            int currentMinor = Integer.parseInt(currentMatcher.group(2));
            int currentPatch = Integer.parseInt(currentMatcher.group(3));
            
            if (latestMajor > currentMajor) {
                return true;
            } else if (latestMajor == currentMajor) {
                if (latestMinor > currentMinor) {
                    return true;
                } else if (latestMinor == currentMinor) {
                    return latestPatch > currentPatch;
                }
            }
            return false;
        } catch (Exception e) {
            plugin.getLogger().warning(
                plugin.getLogMessage("update-version-compare-error",
                    "{error}", e.getMessage())
            );
            return false;
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
        
        String message = plugin.getLogMessage("update-available",
            "{current}", currentVersion,
            "{latest}", latestVersion,
            "{url}", getDownloadUrl());
        
        plugin.getLogger().info("==========================================");
        plugin.getLogger().info(message);
        plugin.getLogger().info("==========================================");
        
        // 发送给在线管理员（需要在主线程执行）
        if (Bukkit.isPrimaryThread()) {
            sendUpdateMessageToAdmins();
        } else {
            Bukkit.getScheduler().runTask(plugin, this::sendUpdateMessageToAdmins);
        }
    }
    
    /**
     * 发送更新消息给管理员
     */
    private void sendUpdateMessageToAdmins() {
        if (latestVersion == null) {
            return;
        }
        
        String playerMessage = plugin.getMessage("update-available",
            "{current}", currentVersion,
            "{latest}", latestVersion,
            "{url}", getDownloadUrl());
        
        Bukkit.getOnlinePlayers().stream()
            .filter(player -> player.hasPermission("tpagui.admin"))
            .forEach(player -> player.sendMessage(playerMessage));
    }
}

