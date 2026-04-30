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
    private static boolean useLatinPrayers;

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
            useLatinPrayers = settings.getBoolean(BaseActivity.LATIN_PRAYERS_KEY, false);
            
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
        return getPrayerString(
                R.string.prayer_sign_of_cross,
                R.string.prayer_sign_of_cross_latin,
                "In the name of the Father...");
    }

    public static String getApostlesCreed() {
        return getPrayerString(
                R.string.prayer_apostles_creed,
                R.string.prayer_apostles_creed_latin,
                "I believe in God...");
    }

    public static String getOurFather() {
        return getPrayerString(
                R.string.prayer_our_father,
                R.string.prayer_our_father_latin,
                "Our Father...");
    }

    public static String getHailMary() {
        return getPrayerString(
                R.string.prayer_hail_mary,
                R.string.prayer_hail_mary_latin,
                "Hail Mary...");
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
                resId = useLatinPrayers ? R.string.prayer_hail_mary_faith_latin : R.string.prayer_hail_mary_faith;
                break;
            case 2:
                resId = useLatinPrayers ? R.string.prayer_hail_mary_hope_latin : R.string.prayer_hail_mary_hope;
                break;
            case 3:
                resId = useLatinPrayers ? R.string.prayer_hail_mary_charity_latin : R.string.prayer_hail_mary_charity;
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
        return getPrayerString(
                R.string.prayer_glory_be,
                R.string.prayer_glory_be_latin,
                "Glory be...");
    }

    public static String getFatimaPrayer() {
        return getPrayerString(
                R.string.prayer_fatima,
                R.string.prayer_fatima_latin,
                "O my Jesus...");
    }

    public static String getHailHolyQueen() {
        return getPrayerString(
                R.string.prayer_hail_holy_queen,
                R.string.prayer_hail_holy_queen_latin,
                "Hail, Holy Queen...");
    }

    public static String getRosaryPrayer() {
        return getPrayerString(
                R.string.prayer_rosary,
                R.string.prayer_rosary_latin,
                "Let us pray...");
    }

    private static String getPrayerString(int defaultResId, int latinResId, String fallback) {
        Resources res = getResourcesSafely();
        if (res == null) return fallback;

        try {
            return res.getString(useLatinPrayers ? latinResId : defaultResId);
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Prayer resource not found", e);
            return fallback;
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
