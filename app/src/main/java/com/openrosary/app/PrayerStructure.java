package com.openrosary.app;

import android.content.res.Resources;

/**
 * Defines the structure of prayers in the rosary using arrays and indices
 * for more efficient prayer management and fewer conditionals
 */
public class PrayerStructure {
    // Prayer types - using constants for clarity
    public static final int PRAYER_SIGN_OF_CROSS = 0;
    public static final int PRAYER_APOSTLES_CREED = 1;
    public static final int PRAYER_OUR_FATHER = 2;
    public static final int PRAYER_HAIL_MARY = 3;
    public static final int PRAYER_HAIL_MARY_FAITH = 4;
    public static final int PRAYER_HAIL_MARY_HOPE = 5;
    public static final int PRAYER_HAIL_MARY_CHARITY = 6;
    public static final int PRAYER_GLORY_BE = 7;
    public static final int PRAYER_FATIMA = 8;
    public static final int PRAYER_MYSTERY_ANNOUNCEMENT = 9;
    public static final int PRAYER_HAIL_HOLY_QUEEN = 10;
    public static final int PRAYER_ROSARY_PRAYER = 11;
    
    // Stage prayer sequences - each array defines the prayer types in a stage
    private static final int[] INTRO_PRAYERS = {
        PRAYER_SIGN_OF_CROSS,
        PRAYER_APOSTLES_CREED
    };
    
    private static final int[] FIRST_PRAYERS = {
        PRAYER_OUR_FATHER,
        PRAYER_HAIL_MARY_FAITH,
        PRAYER_HAIL_MARY_HOPE, 
        PRAYER_HAIL_MARY_CHARITY,
        PRAYER_GLORY_BE
    };
    
    private static final int[] DECADE_PRAYERS = {
        PRAYER_MYSTERY_ANNOUNCEMENT,
        PRAYER_OUR_FATHER,
        PRAYER_HAIL_MARY, // Will be repeated 10 times
        PRAYER_GLORY_BE,
        PRAYER_FATIMA
    };
    
    private static final int[] CONCLUSION_PRAYERS = {
        PRAYER_HAIL_HOLY_QUEEN,
        PRAYER_ROSARY_PRAYER,
        PRAYER_SIGN_OF_CROSS
    };
    
    // Resource IDs for prayer titles
    private static final int[] PRAYER_TITLE_RESOURCE_IDS = {
        R.string.title_sign_of_cross,
        R.string.title_apostles_creed,
        R.string.title_our_father,
        R.string.title_hail_mary_decade, // Requires special handling for count
        R.string.title_hail_mary_faith,
        R.string.title_hail_mary_hope,
        R.string.title_hail_mary_charity,
        R.string.title_glory_be,
        R.string.title_fatima_prayer,
        R.string.mystery_label, // Requires special handling for count
        R.string.title_hail_holy_queen,
        R.string.title_rosary_prayer
    };
    
    /**
     * Get the prayer type for a given total count position in the rosary
     * @param totalCount The position in the overall rosary (1-80)
     * @return The prayer type constant
     */
    public static int getPrayerTypeForCount(int totalCount) {
        // Validate count range
        if (totalCount < 1) totalCount = 1;
        if (totalCount > 80) totalCount = 80;
        
        // Intro prayers: 1-2
        if (totalCount <= 2) {
            return INTRO_PRAYERS[totalCount - 1];
        }
        
        // First prayers: 3-7
        if (totalCount <= 7) {
            return FIRST_PRAYERS[totalCount - 3];
        }
        
        // Conclusion prayers: 78-80
        if (totalCount >= 78) {
            return CONCLUSION_PRAYERS[totalCount - 78];
        }
        
        // Decades: 8-77
        // Determine which decade we're in (0-4)
        int decadeBase = 8; // Base count for first decade
        int decadeSize = 14; // Size of each decade
        int decadeIndex = (totalCount - decadeBase) / decadeSize;
        int positionInDecade = (totalCount - decadeBase) % decadeSize;
        
        if (positionInDecade == 0) {
            return PRAYER_MYSTERY_ANNOUNCEMENT;
        } else if (positionInDecade == 1) {
            return PRAYER_OUR_FATHER;
        } else if (positionInDecade >= 2 && positionInDecade <= 11) {
            return PRAYER_HAIL_MARY;
        } else if (positionInDecade == 12) {
            return PRAYER_GLORY_BE;
        } else { // 13
            return PRAYER_FATIMA;
        }
    }
    
    /**
     * Get the title resource ID for a prayer type
     * @param prayerType The prayer type constant
     * @return The resource ID for the prayer title
     */
    public static int getTitleResourceId(int prayerType) {
        if (prayerType >= 0 && prayerType < PRAYER_TITLE_RESOURCE_IDS.length) {
            return PRAYER_TITLE_RESOURCE_IDS[prayerType];
        }
        return R.string.prayer; // Generic fallback
    }
    
    /**
     * Calculate which decade (1-5) a total count falls in
     * @param totalCount The overall position in rosary (1-80)
     * @return Decade number (1-5) or 0 if not in a decade
     */
    public static int getDecadeForCount(int totalCount) {
        if (totalCount >= 8 && totalCount <= 21) return 1;
        if (totalCount >= 22 && totalCount <= 35) return 2;
        if (totalCount >= 36 && totalCount <= 49) return 3;
        if (totalCount >= 50 && totalCount <= 63) return 4;
        if (totalCount >= 64 && totalCount <= 77) return 5;
        return 0; // Not in a decade
    }
    
    /**
     * Calculate the Hail Mary number (1-10) within a decade
     * @param totalCount The overall position in rosary
     * @return Hail Mary number (1-10) or 0 if not a Hail Mary
     */
    public static int getHailMaryNumber(int totalCount) {
        if (totalCount < 8) return 0; // Before decades
        if (totalCount > 77) return 0; // After decades
        
        int relativePosition = (totalCount - 8) % 14;
        if (relativePosition >= 2 && relativePosition <= 11) {
            return relativePosition - 1; // Convert to 1-10 range
        }
        return 0; // Not a Hail Mary
    }
    
    /**
     * Calculate the mystery index (0-4) for a given total count
     * @param totalCount The overall position in rosary
     * @return Mystery index (0-4) or -1 if not in a decade
     */
    public static int getMysteryIndex(int totalCount) {
        if (totalCount < 8 || totalCount > 77) return -1;
        return (totalCount - 8) / 14;
    }
}