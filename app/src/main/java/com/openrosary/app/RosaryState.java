package com.openrosary.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.Log; // Added for logging
import java.util.Locale;

/**
 * Manages the state of a rosary praying session
 */
public class RosaryState {
    // Rosary stages
    public static final int STAGE_INTRO = 0;          // Initial prayers (Sign of Cross, Apostles' Creed)
    public static final int STAGE_FIRST_PRAYERS = 1;  // Our Father, 3 Hail Marys, Glory Be
    public static final int STAGE_DECADE = 2;         // Decades (Mystery, Our Father, 10 Hail Marys, Glory Be, Fatima Prayer)
    public static final int STAGE_CONCLUSION = 3;     // Concluding prayers (Hail Holy Queen, Rosary Prayer)
    public static final int STAGE_COMPLETE = 4;       // Rosary is complete

    private static final String TAG = "RosaryState"; // Added for logging

    // Current state
    private int stage;              // Current stage
    private int mysteryIndex;       // Current mystery (0-4)
    private int prayerCount;        // Counter for prayers within a stage
    private String mysteryType;     // Type of mysteries being prayed (joyful, sorrowful, etc)
    private int decadeCount;        // Which decade we're on (1-5)
    private boolean isComplete;     // Whether the rosary is complete
    private boolean isGoingBack = false; // Track whether we're going forward or backward (for UI sync)
    
    // Context reference for accessing resources - MUST be set
    private static Context context;

    // Constructor
    public RosaryState(String mysteryType) {
        this.mysteryType = mysteryType;
        this.stage = STAGE_INTRO;
        this.mysteryIndex = 0;
        this.prayerCount = 0;
        this.decadeCount = 1;
        this.isComplete = false;
    }
    
    /**
     * Set the context to be used for accessing resources
     * @param appContext Application context
     */
    public static void setContext(Context appContext) {
        if (appContext == null) {
            Log.e(TAG, "Cannot set context: context is null");
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
            
            Log.d(TAG, "Context set successfully. Current locale: " + 
                  localeContext.getResources().getConfiguration().locale.getDisplayName() +
                  " (Language code: " + localeContext.getResources().getConfiguration().locale.getLanguage() + ")");
            
        } catch (Exception e) {
            Log.e(TAG, "Error configuring locale-aware context: " + e.getMessage());
            // Fallback to using the provided context
            context = appContext.getApplicationContext();
        }
    }

    /**
     * Helper to get resources safely, checking context.
     */
    private static Resources getResourcesSafely() {
        if (context == null) {
            Log.e(TAG, "Context is null. Cannot get resources.");
            // This indicates a setup problem. Maybe throw IllegalStateException?
            return null; // Or return Resources.getSystem() as a last resort?
        }
        return context.getResources();
    }

    /**
     * Advance the rosary to the next prayer based on volume or swipe actions
     * @return String with the current prayer to display
     */
    public String advance() {
        isGoingBack = false; // Reset direction flag
        
        if (isComplete) {
            return "God bless you.";
        }
        
        switch (stage) {
            case STAGE_INTRO:
                return handleIntroStage();
            case STAGE_FIRST_PRAYERS:
                return handleFirstPrayersStage();
            case STAGE_DECADE:
                return handleDecadeStage();
            case STAGE_CONCLUSION:
                return handleConclusionStage();
            case STAGE_COMPLETE:
                return "You have completed the " + mysteryType + " mysteries of the Holy Rosary.";
            default:
                return "God bless you.";
        }
    }

    /**
     * Go back one prayer step
     * @return String with the prayer text for the CURRENT state after going back
     */
    public String goBack() {
        isGoingBack = true; // Set flag for UI update
        Log.d(TAG, "Going back. Current stage: " + stage + ", prayerCount: " + prayerCount + ", decadeCount: " + decadeCount);
        
        // First, update the state (counter, stage, etc.) to go back one step
        switch (stage) {
            case STAGE_INTRO:
                // Can't go back from the very beginning (Sign of Cross)
                if (prayerCount > 0) {
                    prayerCount--;
                }
                break;
            case STAGE_FIRST_PRAYERS:
                if (prayerCount > 0) {
                    prayerCount--;
                } else {
                    // Go back to the end of STAGE_INTRO (Apostles' Creed)
                    stage = STAGE_INTRO;
                    prayerCount = 1; // Apostles' Creed is the last (index 1) in INTRO
                }
                break;
            case STAGE_DECADE:
                if (prayerCount > 0) {
                    // Fix for Glory Be to Hail Mary 10 issue
                    if (prayerCount == 12) {
                        // If currently at Glory Be (12), go back to last Hail Mary (11)
                        prayerCount = 11;
                        Log.d(TAG, "Special case: Going back from Glory Be to Hail Mary 10/10");
                    } else {
                        prayerCount--;
                    }
                } else {
                    // Going back from the beginning of a decade
                    if (decadeCount > 1) {
                        // Go back to the end of the previous decade (Fatima Prayer)
                        decadeCount--;
                        mysteryIndex--; // Decrement mystery index as well
                        prayerCount = 13; // Fatima Prayer
                    } else {
                        // Go back to the end of STAGE_FIRST_PRAYERS (Glory Be)
                        stage = STAGE_FIRST_PRAYERS;
                        prayerCount = 4; // Glory Be
                    }
                }
                break;
            case STAGE_CONCLUSION:
                if (prayerCount > 0) {
                    prayerCount--;
                } else {
                    // Go back to the end of the last decade (Fatima Prayer)
                    stage = STAGE_DECADE;
                    decadeCount = 5; // Should be the last decade
                    mysteryIndex = 4; // Should be the last mystery
                    // Set prayerCount to the last prayer of a decade (Fatima Prayer)
                    prayerCount = 13;
                }
                break;
            case STAGE_COMPLETE:
                // Go back to the last prayer of STAGE_CONCLUSION (Rosary Prayer)
                stage = STAGE_CONCLUSION;
                // Assuming: Hail Holy Queen (0), Rosary Prayer (1)
                prayerCount = 1;
                isComplete = false; // No longer complete
                break;
        }
        
        Log.d(TAG, "After going back. New state: stage=" + stage + ", prayerCount=" + prayerCount + ", decadeCount=" + decadeCount);
        
        // Now that the state has been updated, fetch the prayer text for the CURRENT state
        String prayerText = "";
        
        // Get the prayer text based on current state
        switch (stage) {
            case STAGE_INTRO:
                if (prayerCount == 0) prayerText = RosaryPrayers.getSignOfCross();
                else if (prayerCount == 1) prayerText = RosaryPrayers.getApostlesCreed();
                break;
                
            case STAGE_FIRST_PRAYERS:
                if (prayerCount == 0) prayerText = RosaryPrayers.getOurFather();
                else if (prayerCount >= 1 && prayerCount <= 3) prayerText = RosaryPrayers.getHailMaryForIntro(prayerCount);
                else if (prayerCount == 4) prayerText = RosaryPrayers.getGloryBe();
                break;
                
            case STAGE_DECADE:
                if (prayerCount == 0) {
                    // Mystery announcement
                    Resources res = getResourcesSafely();
                    String[] titles = RosaryPrayers.getMysteryTitles(mysteryType);
                    String localizedMysteryType = getLocalizedMysteryType(mysteryType);
                    
                    if (res != null && titles != null && mysteryIndex < titles.length) {
                        try {
                            prayerText = res.getString(R.string.mystery_announcement_format, 
                                                     decadeCount, 
                                                     localizedMysteryType, 
                                                     titles[mysteryIndex]);
                        } catch (Exception e) {
                            // Simple fallback format
                            prayerText = localizedMysteryType + " Mystery #" + decadeCount + ": " + 
                                        (mysteryIndex < titles.length ? titles[mysteryIndex] : "");
                        }
                    } else {
                        prayerText = "Mystery " + decadeCount;
                    }
                }
                else if (prayerCount == 1) prayerText = RosaryPrayers.getOurFather();
                else if (prayerCount >= 2 && prayerCount <= 11) prayerText = RosaryPrayers.getHailMary();
                else if (prayerCount == 12) prayerText = RosaryPrayers.getGloryBe();
                else if (prayerCount == 13) prayerText = RosaryPrayers.getFatimaPrayer();
                break;
                
            case STAGE_CONCLUSION:
                if (prayerCount == 0) prayerText = RosaryPrayers.getHailHolyQueen();
                else if (prayerCount == 1) prayerText = RosaryPrayers.getRosaryPrayer();
                else if (prayerCount == 2) prayerText = RosaryPrayers.getSignOfCross();
                break;
                
            case STAGE_COMPLETE:
                Resources res = getResourcesSafely();
                try {
                    prayerText = res != null ? res.getString(R.string.completion_title) : "Rosary Complete. God bless you.";
                } catch (Exception e) {
                    prayerText = "Rosary Complete. God bless you.";
                }
                break;
        }
        
        // Reset the flag after we're done
        isGoingBack = false;
        
        return prayerText;
    }
    
    /**
     * Helper method to get prayer text for a specific state
     * Used by goBack() to retrieve the correct prayer text
     */
    private String getPrayerTextForState(int targetStage, int targetPrayerCount, int targetMysteryIndex, int targetDecadeCount) {
        switch (targetStage) {
            case STAGE_INTRO:
                if (targetPrayerCount == 0) return RosaryPrayers.getSignOfCross();
                else if (targetPrayerCount == 1) return RosaryPrayers.getApostlesCreed();
                break;

            case STAGE_FIRST_PRAYERS:
                if (targetPrayerCount == 0) return RosaryPrayers.getOurFather();
                else if (targetPrayerCount >= 1 && targetPrayerCount <= 3) return RosaryPrayers.getHailMaryForIntro(targetPrayerCount);
                else if (targetPrayerCount == 4) return RosaryPrayers.getGloryBe();
                break;

            case STAGE_DECADE:
                if (targetPrayerCount == 0) { 
                    // Mystery Announcement
                    Resources res = getResourcesSafely();
                    String[] mysteryTitles = RosaryPrayers.getMysteryTitles(mysteryType);
                    String localizedMysteryType = getLocalizedMysteryType(mysteryType);
                    
                    if (res != null && mysteryTitles != null && targetMysteryIndex < mysteryTitles.length) {
                        try {
                            return res.getString(R.string.mystery_announcement_format,
                                targetDecadeCount,
                                localizedMysteryType,
                                mysteryTitles[targetMysteryIndex]);
                        } catch (Exception e) {
                            return localizedMysteryType + " Mystery #" + targetDecadeCount + ": " + 
                                   (targetMysteryIndex < mysteryTitles.length ? mysteryTitles[targetMysteryIndex] : "");
                        }
                    }
                    return "Mystery " + targetDecadeCount; // Fallback
                }
                else if (targetPrayerCount == 1) return RosaryPrayers.getOurFather();
                else if (targetPrayerCount >= 2 && targetPrayerCount <= 11) return RosaryPrayers.getHailMary();
                else if (targetPrayerCount == 12) return RosaryPrayers.getGloryBe();
                else if (targetPrayerCount == 13) return RosaryPrayers.getFatimaPrayer();
                break;

            case STAGE_CONCLUSION:
                if (targetPrayerCount == 0) return RosaryPrayers.getHailHolyQueen();
                else if (targetPrayerCount == 1) return RosaryPrayers.getRosaryPrayer();
                else if (targetPrayerCount == 2) return RosaryPrayers.getSignOfCross();
                break;

            case STAGE_COMPLETE:
                Resources res = getResourcesSafely();
                try {
                    return res != null ? res.getString(R.string.completion_title) : "Rosary Complete. God bless you.";
                } catch (Exception e) {
                    return "Rosary Complete. God bless you.";
                }
        }
        
        // Fallback for unexpected state
        return "Prayer";
    }

    /**
     * Get the current mystery title being prayed
     * @return The title of the current mystery
     */
    public String getCurrentMysteryTitle() {
        if (stage == STAGE_DECADE && mysteryIndex < 5) {
            String[] titles = RosaryPrayers.getMysteryTitles(mysteryType);
            if (titles != null && mysteryIndex < titles.length) {
                return titles[mysteryIndex];
            }
        }
        return "";
    }

    /**
     * Get the current mystery description being prayed
     * @return The description of the current mystery
     */
    public String getCurrentMysteryDescription() {
        if (stage == STAGE_DECADE && mysteryIndex < 5) {
            String[] descriptions = RosaryPrayers.getMysteryDescriptions(mysteryType);
            if (descriptions != null && mysteryIndex < descriptions.length) {
                return descriptions[mysteryIndex];
            }
        }
        return "";
    }

    /**
     * Handle progression through the introduction stage
     * @return The prayer text to display
     */
    private String handleIntroStage() {
        String prayerText = "";
        
        switch (prayerCount) {
            case 0: // Sign of the Cross
                prayerText = RosaryPrayers.getSignOfCross();
                prayerCount++;
                break;
            case 1: // Apostles' Creed
                prayerText = RosaryPrayers.getApostlesCreed();
                prayerCount++;
                break;
            default:
                // Move to next stage
                stage = STAGE_FIRST_PRAYERS;
                prayerCount = 0;
                return advance();
        }
        
        return prayerText;
    }

    /**
     * Handle progression through the first prayers stage (Our Father, 3 Hail Marys, Glory Be)
     * @return The prayer text to display
     */
    private String handleFirstPrayersStage() {
        String prayerText = "";
        
        switch (prayerCount) {
            case 0: // Our Father
                prayerText = RosaryPrayers.getOurFather();
                prayerCount++;
                break;
            case 1: // First Hail Mary (for Faith)
                prayerText = RosaryPrayers.getHailMaryForIntro(1);
                prayerCount++;
                break;
            case 2: // Second Hail Mary (for Hope)
                prayerText = RosaryPrayers.getHailMaryForIntro(2);
                prayerCount++;
                break;
            case 3: // Third Hail Mary (for Charity)
                prayerText = RosaryPrayers.getHailMaryForIntro(3);
                prayerCount++; // Added missing increment
                break; // Added missing break
            case 4: // Glory Be
                prayerText = RosaryPrayers.getGloryBe();
                prayerCount++;
                break;
            default:
                // Move to next stage (Decades)
                stage = STAGE_DECADE;
                prayerCount = 0; // Reset prayer count for the new stage
                mysteryIndex = 0; // Start with the first mystery
                decadeCount = 1; // Start with the first decade
                return advance(); // Call advance to get the first prayer of the decade stage
        }
        
        return prayerText;
    } // Added missing closing brace for the method

    /**
     * Handle progression through the decades
     * @return The prayer text to display
     */
    private String handleDecadeStage() {
        Resources res = getResourcesSafely();
        if (res == null) {
            Log.e(TAG, "Resources object is null in handleDecadeStage. Cannot proceed.");
            return "Error: Resources not available."; // Return an error message or handle appropriately
        }
        String prayerText = "";

        switch (prayerCount) {
            case 0: // Announce Mystery
                String[] titles = RosaryPrayers.getMysteryTitles(mysteryType);
                String[] descriptions = RosaryPrayers.getMysteryDescriptions(mysteryType);
                String localizedMysteryType = getLocalizedMysteryType(mysteryType); // Use our own method instead of RosaryPrayers
                
                if (titles != null && descriptions != null && mysteryIndex < titles.length && mysteryIndex < descriptions.length) {
                    // Use the format string resource for the announcement
                    String mysteryAnnouncement = res.getString(R.string.mystery_announcement_format, 
                                                decadeCount, 
                                                localizedMysteryType, 
                                                titles[mysteryIndex]);
                    
                    // Add the mystery description after the title
                    prayerText = mysteryAnnouncement + "\n\n" + descriptions[mysteryIndex];
                } else {
                    Log.e(TAG, "Error retrieving mystery title or description for index: " + mysteryIndex);
                    prayerText = "Error: Mystery information not found."; // Handle error
                }
                prayerCount++;
                break;
            case 1: // Our Father
                prayerText = RosaryPrayers.getOurFather();
                prayerCount++;
                break;
            // Fix: Changed case range to properly handle exactly 10 Hail Marys (2-11)
            case 2: case 3: case 4: case 5: case 6: case 7: case 8: case 9: case 10: case 11: // 10 Hail Marys
                prayerText = RosaryPrayers.getHailMary();
                Log.d(TAG, "Hail Mary #" + (prayerCount - 1) + " of decade " + decadeCount);
                prayerCount++;
                break;
            case 12: // Glory Be
                prayerText = RosaryPrayers.getGloryBe();
                prayerCount++;
                break;
            case 13: // Fatima Prayer
                prayerText = RosaryPrayers.getFatimaPrayer();
                prayerCount++;
                break;
            default:
                // Move to next mystery or conclusion
                mysteryIndex++;
                decadeCount++;
                if (mysteryIndex < 5) {
                    prayerCount = 0; // Start next decade
                    return advance(); // Recursive call to handle the announcement
                } else {
                    // Fix for last decade: properly move to conclusion stage
                    stage = STAGE_CONCLUSION;
                    prayerCount = 0;
                    mysteryIndex = 5; // Ensure mysteryIndex is exactly 5 to avoid any issues
                    return advance(); // Recursive call to handle conclusion start
                }
        }

        return prayerText;
    }

    /**
     * Handle progression through the concluding prayers
     * @return The prayer text to display
     */
    private String handleConclusionStage() {
        String prayerText = "";
        
        switch (prayerCount) {
            case 0: // Hail Holy Queen
                prayerText = RosaryPrayers.getHailHolyQueen();
                prayerCount++;
                break;
            case 1: // Rosary Prayer
                prayerText = RosaryPrayers.getRosaryPrayer();
                prayerCount++;
                break;
            case 2: // Sign of the Cross
                prayerText = RosaryPrayers.getSignOfCross();
                prayerCount++;
                
                // Set the rosary as complete when we finish the final Sign of the Cross
                isComplete = true;
                stage = STAGE_COMPLETE;
                break;
            default:
                // Rosary is complete
                stage = STAGE_COMPLETE;
                isComplete = true;
                Resources res = getResourcesSafely();
                try {
                    return res != null ? res.getString(R.string.continue_iterate) : "You have completed the " + mysteryType + " mysteries of the Holy Rosary. God bless you.";
                } catch (Resources.NotFoundException e) {
                    return "You have completed the " + mysteryType + " mysteries of the Holy Rosary. God bless you.";
                }
        }
        
        return prayerText;
    }

    /**
     * Get the prayer text for the current state
     * @return The prayer text to display
     */
    public String getCurrentPrayerText() { // Changed from private to public
        Resources res = getResourcesSafely();
        String mysteryTypeLocalized = getLocalizedMysteryType(mysteryType); // Get localized type

        switch (stage) {
            case STAGE_INTRO:
                if (prayerCount == 0) return RosaryPrayers.getSignOfCross();
                else if (prayerCount == 1) return RosaryPrayers.getApostlesCreed();
                break;

            case STAGE_FIRST_PRAYERS:
                if (prayerCount == 0) return RosaryPrayers.getOurFather();
                else if (prayerCount >= 1 && prayerCount <= 3) return RosaryPrayers.getHailMaryForIntro(prayerCount);
                else if (prayerCount == 4) return RosaryPrayers.getGloryBe();
                break;

            case STAGE_DECADE:
                if (prayerCount == 0) { // Mystery Announcement
                    String[] mysteryTitles = RosaryPrayers.getMysteryTitles(mysteryType);
                    if (res != null && mysteryTitles != null && mysteryIndex < mysteryTitles.length) {
                         try {
                             // Use the format string: "%2$s Mystery #%1$d: %3$s"
                             return res.getString(R.string.mystery_announcement_format,
                                                 this.decadeCount, // %1$d - Use the actual decadeCount field
                                                 mysteryTypeLocalized, // %2$s
                                                 mysteryTitles[mysteryIndex]); // %3$s
                         } catch (Resources.NotFoundException | java.util.IllegalFormatException e) {
                             Log.e(TAG, "Mystery announcement format string error: " + e.getMessage());
                             // Fallback using the new format structure
                             return mysteryTypeLocalized + " Mystery #" + this.decadeCount + ": " + mysteryTitles[mysteryIndex];
                         }
                    } else {
                         // Fallback if resources or titles are unavailable
                         return mysteryTypeLocalized + " Mystery #" + this.decadeCount;
                    }
                }
                else if (prayerCount == 1) return RosaryPrayers.getOurFather();
                else if (prayerCount >= 2 && prayerCount <= 11) return RosaryPrayers.getHailMary();
                else if (prayerCount == 12) return RosaryPrayers.getGloryBe();
                else if (prayerCount == 13) return RosaryPrayers.getFatimaPrayer();
                break;

            case STAGE_CONCLUSION:
                if (prayerCount == 0) return RosaryPrayers.getHailHolyQueen();
                else if (prayerCount == 1) return RosaryPrayers.getRosaryPrayer();
                else if (prayerCount == 2) return RosaryPrayers.getSignOfCross();
                break;

            case STAGE_COMPLETE:
                // Use a resource string for completion message if available
                try {
                    return res.getString(R.string.completion_title); // Assuming you add this string
                } catch (Resources.NotFoundException e) {
                    return "Rosary Complete. God bless you."; // Fallback
                }
        }

        // Fallback if state is unexpected
        Log.w(TAG, "Unexpected state in getCurrentPrayerText: stage=" + stage + ", prayerCount=" + prayerCount);
        try {
            return res.getString(R.string.prayer); // Use generic "Prayer" string
        } catch (Resources.NotFoundException e) {
            return "Prayer"; // Absolute fallback
        }
    }

    /**
     * Get the localized string for the mystery type (e.g., "Joyful", "Sorrowful")
     * @param mysteryType The internal mystery type string (e.g., "joyful")
     * @return The localized mystery type string
     */
    private String getLocalizedMysteryType(String mysteryType) {
        Resources res = getResourcesSafely();
        if (res == null) return mysteryType; // Fallback

        int resId = res.getIdentifier("mystery_type_" + mysteryType.toLowerCase(), "string", context.getPackageName());
        if (resId != 0) {
            try {
                return res.getString(resId);
            } catch (Resources.NotFoundException e) {
                Log.w(TAG, "Localized string not found for mystery type: " + mysteryType);
            }
        }
        // Fallback to capitalized version if resource not found
        return mysteryType.substring(0, 1).toUpperCase() + mysteryType.substring(1);
    }

    /**
     * Get a short description of the current prayer step using resources.
     * @return A label describing the current prayer
     */
    public String getCurrentPrayerLabel() {
        Resources res = getResourcesSafely();
        if (res == null) {
            // Fallback to simple descriptions if resources are not available
            if (stage == STAGE_INTRO) return "Introduction";
            else if (stage == STAGE_FIRST_PRAYERS) return "First Prayers";
            else if (stage == STAGE_DECADE) return "Decade";
            else if (stage == STAGE_CONCLUSION) return "Conclusion";
            else return "Prayer";
        }

        // Calculate temporary count to account for the decade structure
        // This allows us to distinguish between different prayers within a decade
        int tempCount = prayerCount;
        
        switch (stage) {
            case STAGE_INTRO:
                if (tempCount == 0) return res.getString(R.string.title_sign_of_cross);
                else if (tempCount == 1) return res.getString(R.string.title_apostles_creed);
                else return "Introduction Prayers"; // Default/Error case

            case STAGE_FIRST_PRAYERS:
                if (tempCount == 0) return res.getString(R.string.title_our_father);
                else if (tempCount == 1) return res.getString(R.string.title_hail_mary_faith);
                else if (tempCount == 2) return res.getString(R.string.title_hail_mary_hope);
                else if (tempCount == 3) return res.getString(R.string.title_hail_mary_charity);
                else if (tempCount == 4) return res.getString(R.string.title_glory_be);
                else return res.getString(R.string.title_our_father); // Default/Error case

            case STAGE_DECADE:
                if (tempCount == 0) {
                    // Use the string resource instead of hardcoded "Mystery X"
                    return String.format(res.getString(R.string.mystery_label), decadeCount);
                } else if (tempCount == 1) return res.getString(R.string.title_our_father);
                else if (tempCount >= 2 && tempCount <= 11) {
                    // Calculate which Hail Mary we're on (1-10)
                    int hailMaryNumber = tempCount - 1;
                    return res.getString(R.string.title_hail_mary_decade, hailMaryNumber);
                }
                else if (tempCount == 12) return res.getString(R.string.title_glory_be);
                else if (tempCount == 13) return res.getString(R.string.title_fatima_prayer);
                else {
                    // Use the string resource instead of hardcoded "Mystery"
                    return res.getString(R.string.mystery);
                }

            case STAGE_CONCLUSION:
                if (tempCount == 0) return res.getString(R.string.title_hail_holy_queen);
                else if (tempCount == 1) return res.getString(R.string.title_rosary_prayer);
                else if (tempCount == 2) return res.getString(R.string.title_sign_of_cross);
                else return "Conclusion Prayers";

            default:
                return res.getString(R.string.prayer);
        }
    }

    /**
     * Gets the total count for display
     * @return Count representing progress through the rosary
     */
    public int getTotalCount() {
        // Calculate the count based on current position in the rosary
        int totalCount = 0;
        
        // For intro stage (Sign of Cross + Apostles' Creed)
        if (stage == STAGE_INTRO) {
            totalCount = prayerCount;
        }
        // For completed intro stage
        else if (stage > STAGE_INTRO) {
            totalCount = 2; // Sign of Cross + Apostles' Creed
        }
        
        // For first prayers stage (Our Father + 3 Hail Marys + Glory Be)
        if (stage == STAGE_FIRST_PRAYERS) {
            totalCount += prayerCount;
        }
        // For completed first prayers stage
        else if (stage > STAGE_FIRST_PRAYERS) {
            totalCount += 5; // Our Father + 3 Hail Marys + Glory Be
        }
        
        // For decade stage
        if (stage == STAGE_DECADE) {
            // Add count for completed decades
            totalCount += mysteryIndex * 14; // 14 prayers per completed decade
            
            // Add progress in current decade
            totalCount += prayerCount;
        }
        // For completed decade stage
        else if (stage > STAGE_DECADE) {
            totalCount += 5 * 14; // All decades completed (5 decades × 14 prayers)
        }
        
        // For conclusion stage
        if (stage == STAGE_CONCLUSION) {
            totalCount += prayerCount;
        }
        // For completed conclusion stage
        else if (stage == STAGE_COMPLETE) {
            totalCount += 3; // All concluding prayers completed
        }
        
        return totalCount;
    }
    
    /**
     * Gets the maximum count for a complete rosary
     * @return Maximum count for the rosary
     */
    public int getMaxCount() {
        // Opening prayers (Sign of Cross + Apostles' Creed) = 2
        // First prayers (Our Father + 3 Hail Marys + Glory Be) = 5
        // 5 decades × 14 prayers each = 70
        // (Each decade: Mystery Announcement + Our Father + 10 Hail Marys + Glory Be + Fatima Prayer)
        // Conclusion (Hail Holy Queen + Rosary Prayer + Sign of Cross) = 3
        return 80; // Fixed total count
    }

    /**
     * Check if the prayer transition is between different bead types
     * Small beads = Hail Marys
     * Large beads = Our Father, Glory Be, etc.
     * 
     * @param isAdvancing Whether we're moving forward or backward
     * @return True if the transition is between different bead types
     */
    public boolean isBeadTypeTransition(boolean isAdvancing) {
        // Current prayer info
        int currentStage = this.stage;
        int currentPrayer = this.prayerCount;
        
        // Mystery transitions should not trigger special vibration
        if (currentStage == STAGE_DECADE) {
            // When at Mystery announcement or going from Mystery to Our Father
            if (currentPrayer == 0 || currentPrayer == 1) {
                return false;
            }
        }
        
        if (isAdvancing) {
            // Check transitions when moving forward
            if (currentStage == STAGE_DECADE) {
                // From Hail Mary to Glory Be
                if (currentPrayer == 12) return true;
                // From Our Father to first Hail Mary
                else if (currentPrayer == 2) return true;
                // From last Hail Mary of previous decade to Our Father
                // This is no longer true as we excluded prayer count 1 above
                // else if (currentPrayer == 1 && mysteryIndex > 0) return true;
            }
            // From Glory Be in first prayers to next stage
            else if (currentStage == STAGE_FIRST_PRAYERS && currentPrayer == 5) return true;
            // From Apostles' Creed to Our Father
            else if (currentStage == STAGE_INTRO && currentPrayer == 2) return true;
            // From last decade to conclusion prayers
            else if (currentStage == STAGE_DECADE && mysteryIndex == 5) return true;
        } else {
            // Check transitions when moving backward
            if (currentStage == STAGE_DECADE) {
                // From first Hail Mary back to Our Father
                if (currentPrayer == 2) return true;
                // From Glory Be back to last Hail Mary
                else if (currentPrayer == 12) return true;
            }
            // From first decade back to Glory Be of first prayers
            else if (currentStage == STAGE_DECADE && currentPrayer == 0 && mysteryIndex == 0) return true;
            // From Our Father in first prayers back to Apostles' Creed
            else if (currentStage == STAGE_FIRST_PRAYERS && currentPrayer == 0) return true;
            // From conclusion prayers back to last decade
            else if (currentStage == STAGE_CONCLUSION && currentPrayer == 0) return true;
        }
        
        return false;
    }

    /**
     * Gets the current stage of the rosary.
     * @return The current stage constant (e.g., STAGE_INTRO, STAGE_DECADE).
     */
    public int getStage() {
        return stage;
    }

    /**
     * Gets the current prayer count within the current stage.
     * @return The current prayer count.
     */
    public int getPrayerCount() {
        return prayerCount;
    }

    /**
     * Checks if the rosary is complete.
     * @return True if the rosary is complete, false otherwise.
     */
    public boolean isComplete() {
        return isComplete;
    }

    /**
     * Gets the type of mysteries being prayed (e.g., "joyful", "luminous").
     * @return The internal string identifier for the mystery type.
     */
    public String getMysteryType() {
        return mysteryType;
    }

    // Getters for UI updates
    public int getMysteryIndex() { return mysteryIndex; }
    public int getDecadeCount() { return decadeCount; }
    public boolean isGoingBack() { return isGoingBack; } // Getter for direction

    /**
     * Calculate what the previous prayer text would be without actually changing state
     * This is used to directly get the previous prayer before changing state
     * @return The prayer text for the previous prayer
     */
    public String getPreviousPrayerText() {
        // Log current state before calculating previous prayer
        Log.d(TAG, "Getting previous prayer from stage: " + stage + ", prayerCount: " + prayerCount + ", decadeCount: " + decadeCount);
        
        // Figure out what the previous state would be
        int prevStage;
        int prevPrayerCount;
        int prevMysteryIndex = this.mysteryIndex;
        int prevDecadeCount = this.decadeCount;
        
        // Critical fix: Properly determine the previous prayer based on current position
        switch (stage) {
            case STAGE_INTRO:
                // From Intro stage prayers
                if (prayerCount > 0) {
                    // If we're at Apostles' Creed (count 1), previous is Sign of Cross (count 0)
                    // If we're at Sign of Cross (count 0), can't go back further
                    prevStage = STAGE_INTRO;
                    prevPrayerCount = prayerCount - 1;
                } else {
                    // Can't go back from the beginning
                    prevStage = STAGE_INTRO;
                    prevPrayerCount = 0;
                }
                break;
                
            case STAGE_FIRST_PRAYERS:
                // CRITICAL FIX: Special handling for Hail Mary sequence
                if (prayerCount == 2) {
                    // If at the first Hail Mary (prayerCount=2), previous should be Our Father
                    prevStage = STAGE_FIRST_PRAYERS;
                    prevPrayerCount = 0; // Our Father is at position 0
                    Log.d(TAG, "Fixing critical case: From first Hail Mary to Our Father");
                }
                else if (prayerCount > 0) {
                    // Going back within First Prayers for other prayers
                    prevStage = STAGE_FIRST_PRAYERS;
                    prevPrayerCount = prayerCount - 1;
                } else {
                    // If at Our Father (count 0), previous is Apostles' Creed (final prayer of INTRO)
                    prevStage = STAGE_INTRO;
                    prevPrayerCount = 1; // Apostles' Creed
                    Log.d(TAG, "Fixing critical case: From Our Father to Apostles' Creed");
                }
                break;
                
            case STAGE_DECADE:
                // From Decade stage
                if (prayerCount > 0) {
                    // Going back within current decade
                    prevStage = STAGE_DECADE;
                    prevPrayerCount = prayerCount - 1;
                } else {
                    // From Mystery announcement at start of decade
                    if (decadeCount > 1) {
                        // Go back to end of previous decade
                        prevStage = STAGE_DECADE;
                        prevDecadeCount = decadeCount - 1;
                        prevMysteryIndex = mysteryIndex - 1;
                        prevPrayerCount = 13; // Fatima Prayer
                    } else {
                        // Go back to end of First Prayers
                        prevStage = STAGE_FIRST_PRAYERS;
                        prevPrayerCount = 4; // Glory Be
                    }
                }
                break;
                
            case STAGE_CONCLUSION:
                // From Conclusion stage
                if (prayerCount > 0) {
                    // Going back within Conclusion
                    prevStage = STAGE_CONCLUSION;
                    prevPrayerCount = prayerCount - 1;
                } else {
                    // From Hail, Holy Queen (first prayer), go back to last decade's Fatima Prayer
                    prevStage = STAGE_DECADE;
                    prevDecadeCount = 5;
                    prevMysteryIndex = 4;
                    prevPrayerCount = 13; // Fatima Prayer
                }
                break;
                
            case STAGE_COMPLETE:
                // From Complete stage, go back to final Sign of the Cross
                prevStage = STAGE_CONCLUSION;
                prevPrayerCount = 2; // Sign of Cross
                break;
                
            default:
                // Fallback
                prevStage = stage;
                prevPrayerCount = prayerCount > 0 ? prayerCount - 1 : 0;
        }
        
        // Log the calculated previous state
        Log.d(TAG, "Previous state calculated: stage=" + prevStage + 
              ", prayerCount=" + prevPrayerCount + 
              ", mysteryIndex=" + prevMysteryIndex + 
              ", decadeCount=" + prevDecadeCount);
        
        // Get the prayer text for the calculated previous state
        String prevPrayerText;
        
        switch (prevStage) {
            case STAGE_INTRO:
                if (prevPrayerCount == 0) prevPrayerText = RosaryPrayers.getSignOfCross();
                else prevPrayerText = RosaryPrayers.getApostlesCreed();
                break;
                
            case STAGE_FIRST_PRAYERS:
                if (prevPrayerCount == 0) {
                    prevPrayerText = RosaryPrayers.getOurFather();
                } else if (prevPrayerCount >= 1 && prevPrayerCount <= 3) {
                    // Handle the special Hail Mary prayers in the intro sequence
                    Log.d(TAG, "Getting Hail Mary for position " + prevPrayerCount);
                    prevPrayerText = RosaryPrayers.getHailMaryForIntro(prevPrayerCount);
                }
                else prevPrayerText = RosaryPrayers.getGloryBe();
                break;
                
            case STAGE_DECADE:
                if (prevPrayerCount == 0) {
                    // Mystery announcement
                    Resources res = getResourcesSafely();
                    String[] titles = RosaryPrayers.getMysteryTitles(mysteryType);
                    String localizedMysteryType = getLocalizedMysteryType(mysteryType);
                    
                    if (res != null && titles != null && prevMysteryIndex < titles.length) {
                        try {
                            prevPrayerText = res.getString(R.string.mystery_announcement_format, 
                                                        prevDecadeCount, 
                                                        localizedMysteryType, 
                                                        titles[prevMysteryIndex]);
                        } catch (Exception e) {
                            prevPrayerText = localizedMysteryType + " Mystery #" + prevDecadeCount + ": " + 
                                            (prevMysteryIndex < titles.length ? titles[prevMysteryIndex] : "");
                        }
                    } else {
                        prevPrayerText = "Mystery " + prevDecadeCount;
                    }
                }
                else if (prevPrayerCount == 1) prevPrayerText = RosaryPrayers.getOurFather();
                else if (prevPrayerCount >= 2 && prevPrayerCount <= 11) prevPrayerText = RosaryPrayers.getHailMary();
                else if (prevPrayerCount == 12) prevPrayerText = RosaryPrayers.getGloryBe();
                else prevPrayerText = RosaryPrayers.getFatimaPrayer();
                break;
                
            case STAGE_CONCLUSION:
                if (prevPrayerCount == 0) prevPrayerText = RosaryPrayers.getHailHolyQueen();
                else if (prevPrayerCount == 1) prevPrayerText = RosaryPrayers.getRosaryPrayer();
                else prevPrayerText = RosaryPrayers.getSignOfCross();
                break;
                
            case STAGE_COMPLETE:
                Resources res = getResourcesSafely();
                try {
                    prevPrayerText = res != null ? res.getString(R.string.completion_title) : "Rosary Complete. God bless you.";
                } catch (Exception e) {
                    prevPrayerText = "Rosary Complete. God bless you.";
                }
                break;
                
            default:
                prevPrayerText = "Prayer";
        }
        
        // Special debug log for the critical first Hail Mary case
        if (stage == STAGE_FIRST_PRAYERS && prayerCount == 2) {
            Log.d(TAG, "Special case: Returning Our Father as previous prayer for first Hail Mary");
        }
        
        // Log the prayer text we're returning
        Log.d(TAG, "Previous prayer text (first 20 chars): " + 
              (prevPrayerText.length() > 20 ? prevPrayerText.substring(0, 20) + "..." : prevPrayerText));
        
        return prevPrayerText;
    }

    /**
     * Get prayer text based on the total count using the optimized PrayerStructure
     * @param totalCount The total count of prayers in the rosary (1-80)
     * @return The prayer text for the specified count
     */
    public String getPrayerTextByTotalCount(int totalCount) {
        // Validate count range
        if (totalCount < 1) totalCount = 1;
        if (totalCount > 80) totalCount = 80;
        
        // Get the prayer type for this count
        int prayerType = PrayerStructure.getPrayerTypeForCount(totalCount);
        
        // Get the mystery index if this is a mystery announcement
        int mysteryIndex = PrayerStructure.getMysteryIndex(totalCount);
        
        // Get the decade number if applicable
        int decadeNumber = PrayerStructure.getDecadeForCount(totalCount);
        
        // Return the appropriate prayer text based on prayer type
        switch (prayerType) {
            case PrayerStructure.PRAYER_SIGN_OF_CROSS:
                return RosaryPrayers.getSignOfCross();
                
            case PrayerStructure.PRAYER_APOSTLES_CREED:
                return RosaryPrayers.getApostlesCreed();
                
            case PrayerStructure.PRAYER_OUR_FATHER:
                return RosaryPrayers.getOurFather();
                
            case PrayerStructure.PRAYER_HAIL_MARY:
                return RosaryPrayers.getHailMary();
                
            case PrayerStructure.PRAYER_HAIL_MARY_FAITH:
                return RosaryPrayers.getHailMaryForIntro(1);
                
            case PrayerStructure.PRAYER_HAIL_MARY_HOPE:
                return RosaryPrayers.getHailMaryForIntro(2);
                
            case PrayerStructure.PRAYER_HAIL_MARY_CHARITY:
                return RosaryPrayers.getHailMaryForIntro(3);
                
            case PrayerStructure.PRAYER_GLORY_BE:
                return RosaryPrayers.getGloryBe();
                
            case PrayerStructure.PRAYER_FATIMA:
                return RosaryPrayers.getFatimaPrayer();
                
            case PrayerStructure.PRAYER_MYSTERY_ANNOUNCEMENT:
                String[] titles = RosaryPrayers.getMysteryTitles(mysteryType);
                String[] descriptions = RosaryPrayers.getMysteryDescriptions(mysteryType);
                String localizedMysteryType = getLocalizedMysteryType(mysteryType);
                
                Resources res = getResourcesSafely();
                if (res != null && titles != null && mysteryIndex >= 0 && mysteryIndex < titles.length) {
                    // Use the format string resource for the announcement
                    String mysteryAnnouncement = res.getString(R.string.mystery_announcement_format, 
                                                decadeNumber, 
                                                localizedMysteryType, 
                                                titles[mysteryIndex]);
                    
                    // Add the mystery description if available
                    if (descriptions != null && mysteryIndex < descriptions.length) {
                        return descriptions[mysteryIndex];
                    }
                    return mysteryAnnouncement;
                }
                return "Mystery " + decadeNumber;
                
            case PrayerStructure.PRAYER_HAIL_HOLY_QUEEN:
                return RosaryPrayers.getHailHolyQueen();
                
            case PrayerStructure.PRAYER_ROSARY_PRAYER:
                return RosaryPrayers.getRosaryPrayer();
                
            default:
                return "Prayer";
        }
    }
    
    /**
     * Get prayer label based on the total count using explicit positions
     * @param totalCount The total count of prayers in the rosary (1-80)
     * @return The prayer label for the specified count
     */
    public String getPrayerLabelByTotalCount(int totalCount) {
        Resources res = getResourcesSafely();
        if (res == null) return "Prayer";
        
        try {
            // Validate count range
            if (totalCount < 1) totalCount = 1;
            if (totalCount > 80) totalCount = 80;
            
            switch (totalCount) {
                // Opening
                case 1: return res.getString(R.string.title_sign_of_cross);
                case 2: return res.getString(R.string.title_apostles_creed);
                case 3: return res.getString(R.string.title_our_father);
                case 4: return res.getString(R.string.title_hail_mary_faith);
                case 5: return res.getString(R.string.title_hail_mary_hope);
                case 6: return res.getString(R.string.title_hail_mary_charity);
                case 7: return res.getString(R.string.title_glory_be);
                
                // First Decade
                case 8: return String.format(res.getString(R.string.mystery_label), 1);
                case 9: return res.getString(R.string.title_our_father);
                case 10: return res.getString(R.string.title_hail_mary_decade, 1);
                case 11: return res.getString(R.string.title_hail_mary_decade, 2);
                case 12: return res.getString(R.string.title_hail_mary_decade, 3);
                case 13: return res.getString(R.string.title_hail_mary_decade, 4);
                case 14: return res.getString(R.string.title_hail_mary_decade, 5);
                case 15: return res.getString(R.string.title_hail_mary_decade, 6);
                case 16: return res.getString(R.string.title_hail_mary_decade, 7);
                case 17: return res.getString(R.string.title_hail_mary_decade, 8);
                case 18: return res.getString(R.string.title_hail_mary_decade, 9);
                case 19: return res.getString(R.string.title_hail_mary_decade, 10);
                case 20: return res.getString(R.string.title_glory_be);
                case 21: return res.getString(R.string.title_fatima_prayer);
                
                // Second Decade
                case 22: return String.format(res.getString(R.string.mystery_label), 2);
                case 23: return res.getString(R.string.title_our_father);
                case 24: return res.getString(R.string.title_hail_mary_decade, 1);
                case 25: return res.getString(R.string.title_hail_mary_decade, 2);
                case 26: return res.getString(R.string.title_hail_mary_decade, 3);
                case 27: return res.getString(R.string.title_hail_mary_decade, 4);
                case 28: return res.getString(R.string.title_hail_mary_decade, 5);
                case 29: return res.getString(R.string.title_hail_mary_decade, 6);
                case 30: return res.getString(R.string.title_hail_mary_decade, 7);
                case 31: return res.getString(R.string.title_hail_mary_decade, 8);
                case 32: return res.getString(R.string.title_hail_mary_decade, 9);
                case 33: return res.getString(R.string.title_hail_mary_decade, 10);
                case 34: return res.getString(R.string.title_glory_be);
                case 35: return res.getString(R.string.title_fatima_prayer);
                
                // Third Decade
                case 36: return String.format(res.getString(R.string.mystery_label), 3);
                case 37: return res.getString(R.string.title_our_father);
                case 38: return res.getString(R.string.title_hail_mary_decade, 1);
                case 39: return res.getString(R.string.title_hail_mary_decade, 2);
                case 40: return res.getString(R.string.title_hail_mary_decade, 3);
                case 41: return res.getString(R.string.title_hail_mary_decade, 4);
                case 42: return res.getString(R.string.title_hail_mary_decade, 5);
                case 43: return res.getString(R.string.title_hail_mary_decade, 6);
                case 44: return res.getString(R.string.title_hail_mary_decade, 7);
                case 45: return res.getString(R.string.title_hail_mary_decade, 8);
                case 46: return res.getString(R.string.title_hail_mary_decade, 9);
                case 47: return res.getString(R.string.title_hail_mary_decade, 10);
                case 48: return res.getString(R.string.title_glory_be);
                case 49: return res.getString(R.string.title_fatima_prayer);
                
                // Fourth Decade
                case 50: return String.format(res.getString(R.string.mystery_label), 4);
                case 51: return res.getString(R.string.title_our_father);
                case 52: return res.getString(R.string.title_hail_mary_decade, 1);
                case 53: return res.getString(R.string.title_hail_mary_decade, 2);
                case 54: return res.getString(R.string.title_hail_mary_decade, 3);
                case 55: return res.getString(R.string.title_hail_mary_decade, 4);
                case 56: return res.getString(R.string.title_hail_mary_decade, 5);
                case 57: return res.getString(R.string.title_hail_mary_decade, 6);
                case 58: return res.getString(R.string.title_hail_mary_decade, 7);
                case 59: return res.getString(R.string.title_hail_mary_decade, 8);
                case 60: return res.getString(R.string.title_hail_mary_decade, 9);
                case 61: return res.getString(R.string.title_hail_mary_decade, 10);
                case 62: return res.getString(R.string.title_glory_be);
                case 63: return res.getString(R.string.title_fatima_prayer);
                
                // Fifth Decade
                case 64: return String.format(res.getString(R.string.mystery_label), 5);
                case 65: return res.getString(R.string.title_our_father);
                case 66: return res.getString(R.string.title_hail_mary_decade, 1);
                case 67: return res.getString(R.string.title_hail_mary_decade, 2);
                case 68: return res.getString(R.string.title_hail_mary_decade, 3);
                case 69: return res.getString(R.string.title_hail_mary_decade, 4);
                case 70: return res.getString(R.string.title_hail_mary_decade, 5);
                case 71: return res.getString(R.string.title_hail_mary_decade, 6);
                case 72: return res.getString(R.string.title_hail_mary_decade, 7);
                case 73: return res.getString(R.string.title_hail_mary_decade, 8);
                case 74: return res.getString(R.string.title_hail_mary_decade, 9);
                case 75: return res.getString(R.string.title_hail_mary_decade, 10);
                case 76: return res.getString(R.string.title_glory_be);
                case 77: return res.getString(R.string.title_fatima_prayer);
                
                // Conclusion
                case 78: return res.getString(R.string.title_hail_holy_queen);
                case 79: return res.getString(R.string.title_rosary_prayer);
                case 80: return res.getString(R.string.title_sign_of_cross);
            }
            
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Resource not found in getPrayerLabelByTotalCount: " + e.getMessage());
        }
        
        // Fallback
        return "Prayer";
    }
    
    /**
     * Calculate which mystery decade a total count belongs to (1-5)
     * @param totalCount The total count position
     * @return The decade number (1-5) or 0 if not in a decade
     */
    private int calculateMysteryDecade(int totalCount) {
        if (totalCount >= 8 && totalCount < 22) return 1;
        if (totalCount >= 22 && totalCount < 36) return 2;
        if (totalCount >= 36 && totalCount < 50) return 3;
        if (totalCount >= 50 && totalCount < 64) return 4;
        if (totalCount >= 64 && totalCount < 78) return 5;
        return 0; // Not in a decade
    }
    
    /**
     * Get mystery title by total count using the mathematical pattern
     * @param totalCount The total count
     * @return The mystery title appropriate for the count, or empty string if not at a mystery
     */
    public String getMysteryTitleByTotalCount(int totalCount) {
        // Mystery pattern:
        // - First mystery: 8-21
        // - Second mystery: 22-35
        // - Third mystery: 36-49
        // - Fourth mystery: 50-63
        // - Fifth mystery: 64+
        
        if (totalCount >= 8 && totalCount < 22) {
            return getMysteryTitle(0);
        } else if (totalCount >= 22 && totalCount < 36) {
            return getMysteryTitle(1);
        } else if (totalCount >= 36 && totalCount < 50) {
            return getMysteryTitle(2);
        } else if (totalCount >= 50 && totalCount < 64) {
            return getMysteryTitle(3);
        } else if (totalCount >= 64 && totalCount < 78) {
            return getMysteryTitle(4);
        }
        
        // Not in a mystery decade
        return ""; // Empty string
    }
    
    /**
     * Helper method to get a specific mystery title
     * @param index The index of the mystery (0-4)
     * @return The mystery title
     */
    private String getMysteryTitle(int index) {
        String[] titles = RosaryPrayers.getMysteryTitles(mysteryType);
        if (titles != null && index >= 0 && index < titles.length) {
            return titles[index];
        }
        return "Mystery " + (index + 1);
    }
    
    /**
     * Helper to construct the mystery announcement text
     * @param decadeNum The decade number (1-5)
     * @param mysteryIndex The mystery index (0-4)
     * @return Formatted mystery announcement text
     */
    private String getMysteryAnnouncement(int decadeNum, int mysteryIndex) {
        Resources res = getResourcesSafely();
        String[] titles = RosaryPrayers.getMysteryTitles(mysteryType);
        String localizedMysteryType = getLocalizedMysteryType(mysteryType);
        
        if (res != null && titles != null && mysteryIndex > 0 && mysteryIndex <= titles.length) {
            try {
                return res.getString(R.string.mystery_announcement_format, 
                    decadeNum, 
                    localizedMysteryType, 
                    titles[mysteryIndex - 1]); // Adjust index (1-5 to 0-4)
            } catch (Exception e) {
                return localizedMysteryType + " Mystery #" + decadeNum + ": " + 
                    titles[mysteryIndex - 1];
            }
        }
        return "Mystery " + decadeNum;
    }
    
    /**
     * Calculate the mystery announcement count from decade number
     * @param decadeNum The decade number (1-5)
     * @return The total count for this mystery announcement
     */
    public static int getMysteryAnnouncementCount(int decadeNum) {
        // The mathematical formula is: 8 + (decadeNum - 1) * 14
        return 8 + (decadeNum - 1) * 14;
    }
} // End of RosaryState class