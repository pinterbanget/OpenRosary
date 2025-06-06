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

public class UpdateChecker {
      private static final String TAG = "UpdateChecker";
    private static final String GITHUB_API_URL = "https://api.github.com/repos/pinterbanget/openrosary/releases/latest";
    
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
    }    public void checkForUpdates() {
        Log.d(TAG, "Starting update check...");
        executorService.execute(() -> performUpdateCheck());
    }
    
    // Test method - force a specific version for testing
    public void testUpdateChecker(String testVersion) {
        Log.d(TAG, "Testing update checker with version: " + testVersion);
        this.currentVersion = testVersion;
        executorService.execute(() -> performUpdateCheck());
    }
    
    private String getCurrentAppVersion() {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Error getting app version: " + e.getMessage());
            return "0.0.0"; // fallback version
        }
    }
      private void performUpdateCheck() {
        Log.d(TAG, "Performing update check against: " + GITHUB_API_URL);
        try {
            URI uri = new URI(GITHUB_API_URL);
            HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000); // Increased to 10 seconds
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
                
                Log.d(TAG, "Received response, processing...");
                processUpdateResponse(responseBuilder.toString());
            } else {
                Log.e(TAG, "HTTP error code: " + responseCode);
                if (responseCode == 404) {
                    Log.e(TAG, "Repository or releases not found. Check if the repo exists and has releases.");
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking for updates: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void processUpdateResponse(String response) {
        try {
            JSONObject jsonResponse = new JSONObject(response);
            String latestVersion = jsonResponse.getString("tag_name");
            String downloadUrl = null;
            
            // Get the download URL for the APK
            if (jsonResponse.has("assets")) {
                JSONArray assets = jsonResponse.getJSONArray("assets");
                for (int i = 0; i < assets.length(); i++) {
                    JSONObject asset = assets.getJSONObject(i);
                    String assetName = asset.getString("name");
                    if (assetName.endsWith(".apk")) {
                        downloadUrl = asset.getString("browser_download_url");
                        break;
                    }
                }
            }
              // Compare versions
            Log.d(TAG, "Comparing versions - Current: " + currentVersion + ", Latest: " + latestVersion);
            if (isNewVersionAvailable(currentVersion, latestVersion)) {
                Log.d(TAG, "New version available, showing dialog");
                final String finalDownloadUrl = downloadUrl;
                mainHandler.post(() -> showUpdateDialog(latestVersion, finalDownloadUrl));
            } else {
                Log.d(TAG, "App is up to date");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error parsing update response: " + e.getMessage());
        }
    }    private boolean isNewVersionAvailable(String currentVersion, String latestVersion) {
        try {
            Log.d(TAG, "Version comparison - Current: '" + currentVersion + "', Latest: '" + latestVersion + "'");
            
            // Remove 'v' prefix if present
            String current = currentVersion.startsWith("v") ? currentVersion.substring(1) : currentVersion;
            String latest = latestVersion.startsWith("v") ? latestVersion.substring(1) : latestVersion;
            
            Log.d(TAG, "After prefix removal - Current: '" + current + "', Latest: '" + latest + "'");
            
            // Skip pre-release versions like "beta", "alpha", etc. - don't treat as updates
            if (latest.equalsIgnoreCase("beta") || latest.equalsIgnoreCase("alpha") || latest.contains("-rc") || latest.contains("-alpha") || latest.contains("-beta")) {
                Log.d(TAG, "Latest version is pre-release, skipping update check");
                return false; // Don't show updates for pre-release versions
            }
            
            String[] currentParts = current.split("\\.");
            String[] latestParts = latest.split("\\.");
            
            Log.d(TAG, "Current parts: " + java.util.Arrays.toString(currentParts));
            Log.d(TAG, "Latest parts: " + java.util.Arrays.toString(latestParts));
            
            int maxLength = Math.max(currentParts.length, latestParts.length);
            
            for (int i = 0; i < maxLength; i++) {
                int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
                int latestPart = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;
                
                Log.d(TAG, "Comparing part " + i + ": current=" + currentPart + ", latest=" + latestPart);
                
                if (latestPart > currentPart) {
                    Log.d(TAG, "New version available! (latest > current)");
                    return true;
                } else if (latestPart < currentPart) {
                    Log.d(TAG, "Current version is newer (current > latest)");
                    return false;
                }
                // If equal, continue to next part
            }
            
            Log.d(TAG, "Versions are equal");
            return false; // Versions are equal
            
        } catch (Exception e) {
            Log.e(TAG, "Error comparing versions: " + e.getMessage());
            e.printStackTrace();
            // If we can't parse versions, don't show update dialog
            return false;
        }
    }
      private void showUpdateDialog(String latestVersion, String downloadUrl) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(context.getString(R.string.update_available_title));
            builder.setMessage(context.getString(R.string.update_available_message, latestVersion));
            builder.setCancelable(true);
            
            builder.setPositiveButton(context.getString(R.string.update_download), 
                (DialogInterface dialog, int which) -> {
                    openDownloadUrl(downloadUrl);
                    dialog.dismiss();
                });
                
            builder.setNegativeButton(context.getString(R.string.update_later), 
                (DialogInterface dialog, int which) -> dialog.dismiss());
            
            builder.create().show();
            
        } catch (Exception e) {
            Log.e(TAG, "Error showing update dialog, failing silently: " + e.getMessage());
            // Fail silently - no user notification on error
        }
    }
    
    private void openDownloadUrl(String downloadUrl) {
        try {
            String urlToOpen;
            if (downloadUrl != null && !downloadUrl.isEmpty()) {
                urlToOpen = downloadUrl;
            } else {
                // Fallback to releases page
                urlToOpen = "https://github.com/pinterbanget/openrosary/releases/latest";
            }
            
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlToOpen));
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
