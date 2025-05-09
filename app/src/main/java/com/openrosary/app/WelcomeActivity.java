package com.openrosary.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.GestureDetectorCompat;

import java.util.Locale;

public class WelcomeActivity extends BaseActivity implements GestureDetector.OnGestureListener, AdapterView.OnItemSelectedListener {

    private static final String TAG = "WelcomeActivity";
    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;
    
    private Spinner themeSpinner;
    private Spinner languageSpinner;
    private Button startButton;
    private SwitchCompat themeToggle;
    private TextView themeTextView;
    private TextView lightThemeText;
    private TextView darkThemeText;
    private LinearLayout themeToggleContainer;
    private GestureDetectorCompat gestureDetector;
    private boolean isDarkMode = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // BaseActivity handles theme application before super.onCreate()
        super.onCreate(savedInstanceState); 
        
        // Set the content view 
        setContentView(R.layout.activity_welcome);
        
        try {
            // Determine initial dark mode state based on the applied theme/config
            // Note: BaseActivity already applied the theme based on prefs
            isDarkMode = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
            
            initializeViews();
            setupGestureDetector();
            setupThemeControls(); // Uses isDarkMode
            setupLanguageSpinner(); // Listener is NOT set here anymore
            setupStartButton();
            
            // ADDED: Delay setting the listener until after initial layout
            if (languageSpinner != null) {
                languageSpinner.post(() -> languageSpinner.setOnItemSelectedListener(WelcomeActivity.this));
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
        }
    }
    
    private void initializeViews() {
        try {
            themeTextView = findViewById(R.id.themeTextView);
            themeSpinner = findViewById(R.id.themeSpinner);
            themeToggle = findViewById(R.id.themeToggle);
            lightThemeText = findViewById(R.id.lightThemeText);
            darkThemeText = findViewById(R.id.darkThemeText);
            themeToggleContainer = findViewById(R.id.themeToggleContainer);
            languageSpinner = findViewById(R.id.languageSpinner);
            startButton = findViewById(R.id.startButton);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
        }
    }
    
    private void setupGestureDetector() {
        try {
            gestureDetector = new GestureDetectorCompat(this, this);
        } catch (Exception e) {
            Log.e(TAG, "Error setting up gesture detector: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
        }
    }
    
    private void setupStartButton() {
        try {
            if (startButton != null) {
                // Apply correct background based on theme
                // if (isDarkMode) {
                //     startButton.setBackgroundResource(R.drawable.start_button_background_dark);
                // } else {
                //     startButton.setBackgroundResource(R.drawable.start_button_background);
                // }
                
                startButton.setOnClickListener(v -> {
                    try {
                        // Prevent multiple rapid clicks
                        startButton.setEnabled(false);
                        
                        Intent intent = new Intent(WelcomeActivity.this, ChoicesActivity.class);
                        
                        // Add flags to prevent flickering
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        
                        // Start activity with a fade transition
                        startActivity(intent);
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        
                        // Re-enable the button after a delay
                        v.postDelayed(() -> {
                            if (startButton != null) {
                                startButton.setEnabled(true);
                            }
                        }, 300);
                    } catch (Exception e) {
                        Log.e(TAG, "Error navigating to ChoicesActivity: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
                        // Fallback to simple start without animation if there's an error
                        Intent fallbackIntent = new Intent(WelcomeActivity.this, ChoicesActivity.class);
                        startActivity(fallbackIntent);
                        
                        // Re-enable the button
                        if (startButton != null) {
                            startButton.setEnabled(true);
                        }
                    }
                });
                startButton.setText(R.string.start_button);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up start button: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
        }
    }

    private void handleThemeToggleChange(boolean isChecked) {
        try {
            // Save the preference first
            saveThemePreference(isChecked); 
            
            // Set the default night mode for the app process
            // BaseActivity will apply the correct theme on recreate
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
            recreate(); // Recreate to apply theme change fully via BaseActivity
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling theme toggle: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
        }
    }

    private void setupThemeControls() {
        try {
            // Create array adapter for theme choices (for hidden spinner - compatibility)
            if (themeSpinner != null) {
                ArrayAdapter<CharSequence> themeAdapter = ArrayAdapter.createFromResource(
                        this,
                        R.array.theme_options,
                        android.R.layout.simple_spinner_item
                );
                themeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                themeSpinner.setAdapter(themeAdapter);
            }

            // Set initial state for theme toggle
            if (themeToggle != null) {
                // Remove existing listener to prevent unnecessary callbacks
                themeToggle.setOnCheckedChangeListener(null);
                
                // Set the initial state based on saved preference
                themeToggle.setChecked(isDarkMode);
                
                // Set the listener after initial state is set
                themeToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    handleThemeToggleChange(isChecked);
                });
                
                // Update theme text appearance based on current theme
                updateThemeTextAppearance();
            }
            
            // Set up theme text labels
            setupThemeTextLabels();
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting up theme controls: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
        }
    }
    
    private void setupThemeTextLabels() {
        try {
            // Make theme text labels clickable
            if (lightThemeText != null) {
                lightThemeText.setOnClickListener(v -> {
                    try {
                        if (isDarkMode) {
                            // Switch to light theme
                            isDarkMode = false;
                            if (themeToggle != null) {
                                themeToggle.setChecked(false);
                            } else {
                                handleThemeToggleChange(false);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error in light theme click: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
                    }
                });
            }
            
            if (darkThemeText != null) {
                darkThemeText.setOnClickListener(v -> {
                    try {
                        if (!isDarkMode) {
                            // Switch to dark theme
                            isDarkMode = true;
                            if (themeToggle != null) {
                                themeToggle.setChecked(true);
                            } else {
                                handleThemeToggleChange(true);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error in dark theme click: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up theme text labels: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
        }
    }
    
    private void updateThemeTextAppearance() {
        try {
            if (lightThemeText != null && darkThemeText != null) {
                // Update text appearance based on selected theme
                lightThemeText.setAlpha(isDarkMode ? 0.5f : 1.0f);
                darkThemeText.setAlpha(isDarkMode ? 1.0f : 0.5f);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating theme text appearance: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
        }
    }
    
    private void setupLanguageSpinner() {
        try {
            if (languageSpinner == null) {
                Log.e(TAG, "Language spinner is null");
                return;
            }
            
            // Use native language names that won't be translated
            // This ensures that each language appears in its native form regardless of app language
            String[] languages = new String[] {
                getString(R.string.language_english),  // Will always be "English"
                getString(R.string.language_indonesian) // Will always be "Bahasa Indonesia"
            };
            
            ArrayAdapter<String> languageAdapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_spinner_item,
                    languages
            );
            languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            languageSpinner.setAdapter(languageAdapter);
            
            // Set initial selection based on the *current* configuration locale
            // (which BaseActivity set via attachBaseContext)
            Locale currentLocale = getResources().getConfiguration().getLocales().get(0);
            String currentLangCode = currentLocale.getLanguage();
            
            languageSpinner.setSelection(currentLangCode.equals("in") ? 1 : 0); // 0 for en, 1 for in
            
            // Clear any existing listeners first
            languageSpinner.setOnItemSelectedListener(null);
            // languageSpinner.setOnItemSelectedListener(this); // REMOVED

        } catch (Exception e) {
            Log.e(TAG, "Error setting up language spinner: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
        }
    }
    
    // Implementation of OnItemSelectedListener methods
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        try {
            // Proceed only if it's the language spinner
            if (parent.getId() == R.id.languageSpinner) {
                // Determine selected language code from position
                String selectedLangCode = position == 0 ? "en" : "in";

                // Get the *actual* current language code from the configuration
                Locale currentLocale = getResources().getConfiguration().getLocales().get(0);
                String currentLangCode = currentLocale.getLanguage();

                Log.d(TAG, "Processing language selection. Selected: " + selectedLangCode + ", Current: " + currentLangCode);

                // Only proceed if the selected language is different from the current one
                if (!selectedLangCode.equals(currentLangCode)) {
                    Log.d(TAG, "Language change detected. Saving preference and recreating...");
                    setAppLocale(selectedLangCode);
                    recreate();
                } else {
                     Log.d(TAG, "Selected language (" + selectedLangCode + ") is the same as current (" + currentLangCode + "). No action needed.");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling language selection: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
        }
    }
    
    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Do nothing
    }
    
    private void saveThemePreference(boolean isDarkMode) {
        try {
            SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
            if (settings != null) {
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean(THEME_KEY, isDarkMode);
                editor.apply();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving theme preference: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
        }
    }
    
    private void toggleTheme() {
        try {
            isDarkMode = !isDarkMode;
            if (themeToggle != null) {
                themeToggle.setChecked(isDarkMode); // This will trigger onCheckedChanged
            } else {
                // If toggle is null, handle theme change manually
                handleThemeToggleChange(isDarkMode);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error toggling theme: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
        }
    }

    // Implement GestureDetector methods for swipe functionality
    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
        // Not needed
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        // Not needed
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        try {
            if (e1 == null || e2 == null) return false;
            
            float diffX = e2.getX() - e1.getX();
            float diffY = e2.getY() - e1.getY();
            
            // Check if horizontal swipe is more prominent than vertical swipe
            if (Math.abs(diffX) > Math.abs(diffY)) {
                if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        // Swipe right, toggle theme to light if currently dark
                        if (isDarkMode) toggleTheme();
                    } else {
                        // Swipe left, toggle theme to dark if currently light
                        if (!isDarkMode) toggleTheme();
                    }
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onFling: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
        }
        return false;
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        try {
            if (gestureDetector != null && gestureDetector.onTouchEvent(event)) {
                return true;
            }
            return super.onTouchEvent(event);
        } catch (Exception e) {
            Log.e(TAG, "Error in onTouchEvent: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
            return super.onTouchEvent(event);
        }
    }

    /**
     * Updates all UI elements based on current theme
     * NOTE: This might become less necessary if theme changes always recreate the activity.
     * Consider removing if recreate() handles all UI updates.
     */
    private void updateUIForTheme() {
        try {
            // Update theme text appearance
            updateThemeTextAppearance();
            
            // Update button backgrounds
            if (startButton != null) {
                startButton.setBackgroundResource(isDarkMode ? 
                    R.drawable.start_button_background_dark : 
                    R.drawable.start_button_background);
            }
            
            // Update background colors for relevant views
            View rootView = getWindow().getDecorView().getRootView();
            if (rootView != null) {
                // Use attribute for background color to respect theme
                // Instead of hardcoding colors
                // rootView.setBackgroundColor(getResources().getColor(isDarkMode ? 
                //    R.color.backgroundColorAmoled : 
                //    R.color.backgroundColorLight)); 
                // Better: Define android:colorBackground in themes and let system handle it
            }
            
            // Update text colors recursively
            // updateTextViewColors(rootView); // Also potentially redundant if recreating
        } catch (Exception e) {
            Log.e(TAG, "Error updating UI for theme: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
        }
    }
    
    /**
     * Helper method to update text colors recursively
     */
    private void updateTextViewColors(View view) {
        if (view == null) return;
        
        if (view instanceof TextView) {
            // Skip theme text views which have special handling
            if (view == lightThemeText || view == darkThemeText || view == themeTextView) {
                return;
            }
            
            // Update text color based on theme
            ((TextView) view).setTextColor(getResources().getColor(isDarkMode ? 
                R.color.colorAccent : 
                R.color.colorPrimary));
        } else if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                updateTextViewColors(viewGroup.getChildAt(i));
            }
        }
    }
}