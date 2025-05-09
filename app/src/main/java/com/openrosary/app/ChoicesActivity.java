package com.openrosary.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

public class ChoicesActivity extends BaseActivity {

    private static final String TAG = "ChoicesActivity";
    private boolean isDarkMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            // Load preferences and apply theme and language
            // loadPreferences(); // REMOVED - BaseActivity handles this now
            
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_choices);
            
            // Get the isDarkMode value from preferences to use for UI elements
            SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
            if (settings != null) {
                isDarkMode = settings.getBoolean(THEME_KEY, false);
            }
            
            // Update title text color based on theme
            TextView titleTextView = findViewById(R.id.titleTextView);
            if (titleTextView != null) {
                if (isDarkMode) {
                    titleTextView.setTextColor(getResources().getColor(R.color.colorAccent));
                } else {
                    titleTextView.setTextColor(getResources().getColor(R.color.colorPrimary));
                }
            }
            
            // Set up mystery buttons
            setupMysteryButtons();
            
            // Set up general options button
            setupGeneralOptionsButton();
            
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
        }
    }
    
    // loadPreferences() and setAppLocale() methods removed - using BaseActivity's implementation instead
    
    private void applyThemeToButton(Button button) {
        if (button != null) {
            if (isDarkMode) {
                button.setBackgroundResource(R.drawable.mystery_button_background_dark);
                button.setTextColor(getResources().getColor(R.color.colorAccent));
            } else {
                button.setBackgroundResource(R.drawable.mystery_button_background);
                button.setTextColor(getResources().getColor(R.color.colorPrimary)); // Changed to colorPrimary for light mode
            }
        }
    }
    
    private void setupMysteryButtons() {
        try {
            // Setup Joyful Mysteries button
            Button joyfulButton = findViewById(R.id.joyfulMysteriesButton);
            applyThemeToButton(joyfulButton);
            if (joyfulButton != null) {
                joyfulButton.setText(R.string.joyful_mysteries);
                joyfulButton.setOnClickListener(v -> launchMainActivity("joyful"));
            }
            
            // Setup Sorrowful Mysteries button
            Button sorrowfulButton = findViewById(R.id.sorrowfulMysteriesButton);
            applyThemeToButton(sorrowfulButton);
            if (sorrowfulButton != null) {
                sorrowfulButton.setText(R.string.sorrowful_mysteries);
                sorrowfulButton.setOnClickListener(v -> launchMainActivity("sorrowful"));
            }
            
            // Setup Glorious Mysteries button
            Button gloriousButton = findViewById(R.id.gloriousMysteriesButton);
            applyThemeToButton(gloriousButton);
            if (gloriousButton != null) {
                gloriousButton.setText(R.string.glorious_mysteries);
                gloriousButton.setOnClickListener(v -> launchMainActivity("glorious"));
            }
            
            // Setup Luminous Mysteries button
            Button luminousButton = findViewById(R.id.luminousMysteriesButton);
            applyThemeToButton(luminousButton);
            if (luminousButton != null) {
                luminousButton.setText(R.string.luminous_mysteries);
                luminousButton.setOnClickListener(v -> launchMainActivity("luminous"));
            }
            
            // Set general options button text
            Button generalOptionsButton = findViewById(R.id.generalOptionsButton);
            if (generalOptionsButton != null) {
                generalOptionsButton.setText(R.string.general_options);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting up mystery buttons: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
        }
    }
    
    private void setupGeneralOptionsButton() {
        Button generalOptionsButton = findViewById(R.id.generalOptionsButton);
        if (generalOptionsButton != null) {
            generalOptionsButton.setOnClickListener(v -> {
                Intent intent = new Intent(ChoicesActivity.this, WelcomeActivity.class);
                // Clear the task stack above WelcomeActivity and bring it to front,
                // or start it fresh if not running.
                // Added FLAG_ACTIVITY_NEW_TASK for potentially more robust clearing.
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish(); // Finish ChoicesActivity so it's removed from the back stack
            });
        } else {
             Log.e("ChoicesActivity", "General Options button not found!");
        }
    }
    
    private void launchMainActivity(String mysteryType) {
        try {
            Intent intent = new Intent(ChoicesActivity.this, MainActivity.class);
            intent.putExtra("mysteryType", mysteryType);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error launching MainActivity: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
        }
    }
    
    @Override
    public void onBackPressed() {
        // Just minimize the app when back is pressed from choices screen
        // This prevents going back to the welcome screen accidentally
        moveTaskToBack(true);
    }
}