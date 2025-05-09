package com.simplerosary.app;

import android.content.Context; // Added
import android.content.ContextWrapper; // Added
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build; // Added
import android.os.Bundle;
import android.os.LocaleList; // Added
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import java.util.Locale;

/**
 * BaseActivity that handles common functionality for all activities,
 * particularly language and theme management.
 */
public abstract class BaseActivity extends AppCompatActivity {

    private static final String TAG = "BaseActivity";
    protected static final String PREFS_NAME = "SimpleRosaryPrefs";
    protected static final String THEME_KEY = "theme";
    protected static final String LANGUAGE_KEY = "language";
    // Removed initialSetupComplete as it's less relevant with attachBaseContext approach

    // Helper method to update context configuration based on saved language
    private static ContextWrapper updateBaseContextLocale(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        // Read preference ("en" or "in"), default to "en"
        String languageCode = settings.getString(LANGUAGE_KEY, "en"); 
        Log.d(TAG, "updateBaseContextLocale: Reading language preference - Found: " + languageCode);
        // Create Locale using the code ("en" or "in")
        Locale locale = new Locale(languageCode); 
        Locale.setDefault(locale);

        Configuration config = new Configuration(context.getResources().getConfiguration());
        
        // Apply locale based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            LocaleList localeList = new LocaleList(locale);
            LocaleList.setDefault(localeList);
            config.setLocales(localeList);
        } else {
            config.setLocale(locale);
        }

        context = context.createConfigurationContext(config);
        Log.d(TAG, "updateBaseContextLocale: Context updated with locale: " + languageCode);
        return new ContextWrapper(context);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        // Update the context wrapper with the saved language before attaching it
        super.attachBaseContext(updateBaseContextLocale(newBase));
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme settings *before* super.onCreate()
        applyTheme(); 

        super.onCreate(savedInstanceState);
        // Language is handled by attachBaseContext now
    }

    @Override
    protected void onResume() {
        super.onResume();
        // No need for checkLanguageUpdate anymore, attachBaseContext handles it on recreate
    }

    /**
     * Apply user preferences for theme ONLY. Language is handled by attachBaseContext.
     */
    protected void applyTheme() {
        try {
            SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
            if (settings != null) {
                boolean isDarkMode = settings.getBoolean(THEME_KEY, false);

                // Apply theme BEFORE super.onCreate()
                if (isDarkMode) {
                    setTheme(R.style.AppTheme_Amoled); 
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                } else {
                    setTheme(R.style.AppTheme); 
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                }
            } else {
                 // Fallback if settings are null
                 setTheme(R.style.AppTheme); 
                 AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error applying theme: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
            // Default to standard theme if there's an error
            setTheme(R.style.AppTheme); 
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    // Removed loadPreferences() as applyTheme() covers the necessary part

    // Removed checkLanguageUpdate()

    /**
     * Saves the selected language preference ("en" or "in").
     */
    protected void setAppLocale(String languageCode) { // Expecting "en" or "in"
        try {
            SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
            String currentLang = settings.getString(LANGUAGE_KEY, "en");

            if (!currentLang.equals(languageCode)) {
                SharedPreferences.Editor editor = settings.edit();
                editor.putString(LANGUAGE_KEY, languageCode); // Save "en" or "in"
                // Use commit() for synchronous saving during this critical operation
                boolean saved = editor.commit(); 
                if (saved) {
                    Log.d(TAG, "Language preference saved successfully: " + languageCode);
                } else {
                    Log.e(TAG, "Failed to save language preference: " + languageCode);
                }
            } else {
                 Log.d(TAG, "Language preference (" + languageCode + ") already saved.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving language preference: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
        }
    }
}