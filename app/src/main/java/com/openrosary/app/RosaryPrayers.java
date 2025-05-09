package com.openrosary.app;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import java.util.Locale;

/**
 * Class to retrieve rosary prayers and mysteries directly from resources
 * to support multiple languages dynamically.
 */
public class RosaryPrayers {
    private static final String TAG = "RosaryPrayers";

    // Context for accessing resources
    private static Context context;

    /**
     * Initialize the RosaryPrayers class with context.
     * This must be called before accessing any prayers.
     * @param appContext The application context
     */
    public static void initialize(Context appContext) {
        if (appContext == null) {
            Log.e(TAG, "Cannot initialize prayers: context is null");
            // Set a flag or throw an exception? For now, log and proceed cautiously.
            context = null;
            return;
        }
        
        try {
            // Get the saved language preference to ensure we use the correct locale
            SharedPreferences settings = appContext.getSharedPreferences("SimpleRosaryPrefs", 0);
            String languageCode = settings.getString("language", "en");
            
            // Create Locale using the code ("en" or "in")
            Locale locale = new Locale(languageCode);
            Locale.setDefault(locale);

            // Create a configuration with the correct locale
            Configuration config = new Configuration(appContext.getResources().getConfiguration());
            
            // Apply locale based on Android version
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                android.os.LocaleList localeList = new android.os.LocaleList(locale);
                android.os.LocaleList.setDefault(localeList);
                config.setLocales(localeList);
            } else {
                config.setLocale(locale);
            }
            
            // Create a locale-aware context
            Context localeContext = appContext.createConfigurationContext(config);
            
            // Store this locale-aware context
            context = localeContext;
            
            // Log current configuration details for debugging
            java.util.Locale currentLocale = context.getResources().getConfiguration().locale;
            Log.d(TAG, "RosaryPrayers initialized with context. Current locale: " + currentLocale.getDisplayName() + 
                  " (Language code: " + currentLocale.getLanguage() + ")");
            
            // Try to load a prayer to see if it works
            try {
                String testPrayer = context.getResources().getString(R.string.prayer_our_father);
                Log.d(TAG, "Test prayer loaded successfully (first 20 chars): " + 
                      (testPrayer.length() > 20 ? testPrayer.substring(0, 20) + "..." : testPrayer));
            } catch (Exception e) {
                Log.e(TAG, "Error loading test prayer: " + e.getMessage());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error configuring locale-aware context: " + e.getMessage());
            // Fallback to using the provided context
            context = appContext.getApplicationContext();
        }
    }

    /**
     * Helper to check if context is available and get Resources.
     * @return Resources object or null if context is missing.
     */
    private static Resources getResourcesSafely() {
        if (context == null) {
            Log.e(TAG, "Context is null in RosaryPrayers. Cannot get resources.");
            // Attempt to recover context? Or just return null?
            // For now, returning null, getters will handle it.
            return null;
        }
        try {
            // Get the resources with the current configuration
            Resources res = context.getResources();
            
            // Log the current locale for debugging
            java.util.Locale currentLocale = res.getConfiguration().locale;
            Log.d(TAG, "Current locale in getResourcesSafely: " + currentLocale.getDisplayName() + 
                  " (Language code: " + currentLocale.getLanguage() + ")");
            
            return res;
        } catch (Exception e) {
            Log.e(TAG, "Error getting resources: " + e.getMessage());
            return null;
        }
    }

    // Getters for prayers - Modified to fetch directly from resources
    public static String getSignOfCross() {
        Resources res = getResourcesSafely();
        if (res == null) return "In the name of the Father..."; // Basic fallback
        try {
            return res.getString(R.string.prayer_sign_of_cross);
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Resource not found: prayer_sign_of_cross");
            return "Error: Sign of Cross missing.";
        }
    }

    public static String getApostlesCreed() {
        Resources res = getResourcesSafely();
        if (res == null) return "I believe in God..."; // Basic fallback
        try {
            return res.getString(R.string.prayer_apostles_creed);
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Resource not found: prayer_apostles_creed");
            return "Error: Creed missing.";
        }
    }

    public static String getOurFather() {
        Resources res = getResourcesSafely();
        if (res == null) return "Our Father..."; // Basic fallback
        try {
            return res.getString(R.string.prayer_our_father);
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Resource not found: prayer_our_father");
            return "Error: Our Father missing.";
        }
    }

    public static String getHailMary() {
        Resources res = getResourcesSafely();
        if (res == null) return "Hail Mary..."; // Basic fallback
        try {
            return res.getString(R.string.prayer_hail_mary);
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Resource not found: prayer_hail_mary");
            return "Error: Hail Mary missing.";
        }
    }

    /**
     * Get the appropriate Hail Mary prayer based on its position in the intro sequence
     * @param position 1 for Faith, 2 for Hope, 3 for Charity
     * @return The appropriate Hail Mary prayer text
     */
    public static String getHailMaryForIntro(int position) {
        Resources res = getResourcesSafely();
        if (res == null) return getHailMary(); // Fallback to standard Hail Mary

        int resId;
        switch (position) {
            case 1:
                resId = R.string.prayer_hail_mary_faith;
                break;
            case 2:
                resId = R.string.prayer_hail_mary_hope;
                break;
            case 3:
                resId = R.string.prayer_hail_mary_charity;
                break;
            default:
                return getHailMary(); // Fallback for invalid position
        }

        try {
            return res.getString(resId);
        } catch (Resources.NotFoundException e) {
            Log.w(TAG, "Specialized Hail Mary resource not found (pos " + position + "), using standard.");
            return getHailMary(); // Fallback if specific resource is missing
        }
    }

    public static String getGloryBe() {
        Resources res = getResourcesSafely();
        if (res == null) return "Glory be..."; // Basic fallback
        try {
            return res.getString(R.string.prayer_glory_be);
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Resource not found: prayer_glory_be");
            return "Error: Glory Be missing.";
        }
    }

    public static String getFatimaPrayer() {
        Resources res = getResourcesSafely();
        if (res == null) return "O my Jesus..."; // Basic fallback
        try {
            return res.getString(R.string.prayer_fatima);
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Resource not found: prayer_fatima");
            return "Error: Fatima Prayer missing.";
        }
    }

    public static String getHailHolyQueen() {
        Resources res = getResourcesSafely();
        if (res == null) return "Hail, Holy Queen..."; // Basic fallback
        try {
            return res.getString(R.string.prayer_hail_holy_queen);
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Resource not found: prayer_hail_holy_queen");
            return "Error: Hail Holy Queen missing.";
        }
    }

    public static String getRosaryPrayer() {
        Resources res = getResourcesSafely();
        if (res == null) return "Let us pray..."; // Basic fallback
        try {
            return res.getString(R.string.prayer_rosary);
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Resource not found: prayer_rosary");
            return "Error: Rosary Prayer missing.";
        }
    }

    /**
     * Get the titles for a specific mystery type directly from resources
     * @param mysteryType The type of mystery (joyful, luminous, sorrowful, glorious)
     * @return Array of titles for each mystery in the set
     */
    public static String[] getMysteryTitles(String mysteryType) {
        Resources res = getResourcesSafely();
        if (res == null || mysteryType == null) {
            Log.w(TAG, "Cannot get mystery titles: resources or mysteryType is null.");
            return new String[0];
        }

        int arrayId = 0;
        switch (mysteryType.toLowerCase()) {
            case "joyful":
                arrayId = R.array.joyful_mysteries_titles;
                break;
            case "luminous":
                arrayId = R.array.luminous_mysteries_titles;
                break;
            case "sorrowful":
                arrayId = R.array.sorrowful_mysteries_titles;
                break;
            case "glorious":
                arrayId = R.array.glorious_mysteries_titles;
                break;
            default:
                Log.w(TAG, "Unknown mystery type for titles: " + mysteryType);
                return new String[0];
        }

        try {
            return res.getStringArray(arrayId);
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Resource array not found for mystery titles: " + mysteryType);
            return new String[0];
        }
    }

    /**
     * Get the descriptions for a specific mystery type directly from resources
     * @param mysteryType The type of mystery (joyful, luminous, sorrowful, glorious)
     * @return Array of descriptions for each mystery in the set
     */
    public static String[] getMysteryDescriptions(String mysteryType) {
        Resources res = getResourcesSafely();
         if (res == null || mysteryType == null) {
            Log.w(TAG, "Cannot get mystery descriptions: resources or mysteryType is null.");
            return new String[0];
        }

        int arrayId = 0;
        switch (mysteryType.toLowerCase()) {
            case "joyful":
                arrayId = R.array.joyful_mysteries_descriptions;
                break;
            case "luminous":
                arrayId = R.array.luminous_mysteries_descriptions;
                break;
            case "sorrowful":
                arrayId = R.array.sorrowful_mysteries_descriptions;
                break;
            case "glorious":
                arrayId = R.array.glorious_mysteries_descriptions;
                break;
             default:
                Log.w(TAG, "Unknown mystery type for descriptions: " + mysteryType);
                return new String[0];
        }

        try {
            // Handle potentially missing description arrays gracefully
            if (arrayId != 0) { // Ensure a valid ID was found
                 return res.getStringArray(arrayId);
            } else {
                return new String[0]; // Return empty if no valid ID
            }
        } catch (Resources.NotFoundException e) {
            Log.w(TAG, "Resource array not found for mystery descriptions: " + mysteryType);
            // Return empty array instead of crashing if descriptions are optional/missing
            return new String[0];
        }
    }

    public static String getSuggestedMysteryForToday() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK);

        // Traditional assignment of mysteries to days of the week
        switch(dayOfWeek) {
            case java.util.Calendar.MONDAY:
                return "joyful";
            case java.util.Calendar.TUESDAY:
                return "sorrowful";
            case java.util.Calendar.WEDNESDAY:
                return "glorious";
            case java.util.Calendar.THURSDAY:
                return "luminous";
            case java.util.Calendar.FRIDAY:
                return "sorrowful";
            case java.util.Calendar.SATURDAY:
                return "joyful";
            case java.util.Calendar.SUNDAY:
                // During Advent and Christmas: Joyful
                // During Lent: Sorrowful
                // During Easter and Ordinary Time: Glorious
                int month = calendar.get(java.util.Calendar.MONTH);
                // Simple approximation for liturgical seasons
                if (month == java.util.Calendar.DECEMBER || month == java.util.Calendar.JANUARY) {
                    return "joyful"; // Advent and Christmas season
                } else if (month == java.util.Calendar.FEBRUARY || month == java.util.Calendar.MARCH) {
                    return "sorrowful"; // Approximate for Lent
                } else {
                    return "glorious"; // Rest of the year
                }
            default:
                return "joyful";
        }
    }
}