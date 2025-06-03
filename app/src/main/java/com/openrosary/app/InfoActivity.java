package com.openrosary.app;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

public class InfoActivity extends BaseActivity {

    private static final String TAG = "InfoActivity";
    private boolean isDarkMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_info);
            
            // Get the isDarkMode value from preferences to use for UI elements
            SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
            if (settings != null) {
                isDarkMode = settings.getBoolean(THEME_KEY, false);
            }
            
            // Update text colors based on theme
            updateTextColors();
            
            // Set up back button
            setupBackButton();
            
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
        }
    }

    private void updateTextColors() {
        try {
            int primaryTextColor = isDarkMode ? 
                getResources().getColor(R.color.colorAccent) : 
                getResources().getColor(R.color.colorPrimary);
            
            int secondaryTextColor = isDarkMode ? 
                getResources().getColor(R.color.colorAccent) : 
                getResources().getColor(R.color.colorPrimary);
            
            // Update primary text elements
            TextView infoTitleTextView = findViewById(R.id.infoTitleTextView);
            TextView appNameTextView = findViewById(R.id.appNameTextView);
            TextView featuresHeaderTextView = findViewById(R.id.featuresHeaderTextView);
            TextView attributionHeaderTextView = findViewById(R.id.attributionHeaderTextView);
            
            if (infoTitleTextView != null) infoTitleTextView.setTextColor(primaryTextColor);
            if (appNameTextView != null) appNameTextView.setTextColor(primaryTextColor);
            if (featuresHeaderTextView != null) featuresHeaderTextView.setTextColor(primaryTextColor);
            if (attributionHeaderTextView != null) attributionHeaderTextView.setTextColor(primaryTextColor);
            
            // Update secondary text elements
            TextView appDescriptionTextView = findViewById(R.id.appDescriptionTextView);
            TextView featuresTextView = findViewById(R.id.featuresTextView);
            TextView attributionTextView = findViewById(R.id.attributionTextView);
            
            if (appDescriptionTextView != null) appDescriptionTextView.setTextColor(secondaryTextColor);
            if (featuresTextView != null) featuresTextView.setTextColor(secondaryTextColor);
            if (attributionTextView != null) attributionTextView.setTextColor(secondaryTextColor);
            
        } catch (Exception e) {
            Log.e(TAG, "Error updating text colors: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
        }
    }

    private void setupBackButton() {
        try {
            Button backButton = findViewById(R.id.backButton);
            if (backButton != null) {
                backButton.setOnClickListener(v -> {
                    // Simply finish this activity to return to the previous screen
                    finish();
                });
            } else {
                Log.e(TAG, "Back button not found!");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up back button: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
        }
    }

    @Override
    public void onBackPressed() {
        // Call the parent implementation which will finish the activity
        super.onBackPressed();
    }
}
