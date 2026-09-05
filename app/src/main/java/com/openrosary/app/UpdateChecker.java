package com.openrosary.app;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateChecker {
    private static final String TAG = "UpdateChecker";
    private static final String GITHUB_API_URL = "https://api.github.com/repos/pinterbanget/openrosary/releases/latest";
    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)(?:\\.(\\d+))?");

    private Context context;
    private Handler mainHandler;
    private ExecutorService executorService;
    private String currentVersion;

    public UpdateChecker(Context context) {
        this.context = context;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.executorService = Executors.newSingleThreadExecutor();
        this.currentVersion = getCurrentAppVersion();
        Log.d(TAG, "Current app version: " + currentVersion);
    }

    public void checkForUpdates() {
        Log.d(TAG, "Starting update check...");
        executorService.execute(this::performUpdateCheck);
    }

    public void testUpdateChecker(String testVersion) {
        this.currentVersion = testVersion;
        executorService.execute(this::performUpdateCheck);
    }

    private String getCurrentAppVersion() {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Error getting app version: " + e.getMessage());
            return "0.4";
        }
    }

    private void performUpdateCheck() {
        try {
            URI uri = new URI(GITHUB_API_URL);
            HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
            connection.setRequestProperty("User-Agent", "OpenRosary-App");

            int responseCode = connection.getResponseCode();
            Log.d(TAG, "GitHub API response code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder responseBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    responseBuilder.append(line);
                }
                reader.close();
                connection.disconnect();

                processUpdateResponse(responseBuilder.toString());
            } else {
                Log.e(TAG, "GitHub API returned status: " + responseCode);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking for updates: " + e.getMessage());
        }
    }

    private void processUpdateResponse(String response) {
        try {
            JSONObject json = new JSONObject(response);
            String tagName = json.optString("tag_name", "");
            String releaseName = json.optString("name", "");
            String htmlUrl = json.optString("html_url", "https://github.com/pinterbanget/openrosary/releases/latest");
            String downloadUrl = htmlUrl;

            if (json.has("assets")) {
                JSONArray assets = json.getJSONArray("assets");
                for (int i = 0; i < assets.length(); i++) {
                    JSONObject asset = assets.getJSONObject(i);
                    String assetName = asset.optString("name", "");
                    if (assetName.endsWith(".apk")) {
                        downloadUrl = asset.optString("browser_download_url", htmlUrl);
                        break;
                    }
                }
            }

            String latestVersion = extractVersion(tagName);
            if (latestVersion == null) {
                latestVersion = extractVersion(releaseName);
            }
            if (latestVersion == null && downloadUrl != null) {
                latestVersion = extractVersion(downloadUrl);
            }

            Log.d(TAG, "Comparing versions - Current: " + currentVersion + ", Latest found: " + latestVersion);
            if (latestVersion != null && isNewVersionAvailable(currentVersion, latestVersion)) {
                final String finalVersion = latestVersion;
                final String finalUrl = downloadUrl;
                mainHandler.post(() -> showUpdateDialog(finalVersion, finalUrl));
            } else {
                Log.d(TAG, "OpenRosary is up to date.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing update response: " + e.getMessage());
        }
    }

    private String extractVersion(String text) {
        if (text == null || text.isEmpty()) return null;
        Matcher matcher = VERSION_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(0);
        }
        return null;
    }

    private boolean isNewVersionAvailable(String current, String latest) {
        try {
            int[] cParts = parseVersionParts(current);
            int[] lParts = parseVersionParts(latest);

            for (int i = 0; i < 3; i++) {
                if (lParts[i] > cParts[i]) return true;
                if (lParts[i] < cParts[i]) return false;
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error comparing versions: " + e.getMessage());
            return false;
        }
    }

    private int[] parseVersionParts(String version) {
        int[] parts = new int[]{0, 0, 0};
        if (version == null) return parts;
        Matcher matcher = VERSION_PATTERN.matcher(version);
        if (matcher.find()) {
            if (matcher.group(1) != null) parts[0] = Integer.parseInt(matcher.group(1));
            if (matcher.group(2) != null) parts[1] = Integer.parseInt(matcher.group(2));
            if (matcher.group(3) != null) parts[2] = Integer.parseInt(matcher.group(3));
        }
        return parts;
    }

    private void showUpdateDialog(String latestVersion, String downloadUrl) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(context.getString(R.string.update_available_title));
            builder.setMessage(context.getString(R.string.update_available_message, latestVersion));
            builder.setCancelable(true);

            builder.setPositiveButton(context.getString(R.string.update_download),
                    (dialog, which) -> {
                        openDownloadUrl(downloadUrl);
                        dialog.dismiss();
                    });

            builder.setNegativeButton(context.getString(R.string.update_later),
                    (dialog, which) -> dialog.dismiss());

            builder.create().show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing update dialog: " + e.getMessage());
        }
    }

    private void openDownloadUrl(String downloadUrl) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl));
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening download URL: " + e.getMessage());
        }
    }

    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
