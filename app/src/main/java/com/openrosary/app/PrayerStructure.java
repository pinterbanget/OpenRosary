package com.openrosary.app;

/**
 * Defines the structure of prayers in the rosary using indices.
 * In Indonesian, opening Kemuliaan / Terpujilah is added between Creed and first Our Father (81 total).
 * In all other languages, total is 80.
 */
public class PrayerStructure {
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
    
    private static final int[] PRAYER_TITLE_RESOURCE_IDS = {
        R.string.title_sign_of_cross,
        R.string.title_apostles_creed,
        R.string.title_our_father,
        R.string.title_hail_mary_decade,
        R.string.title_hail_mary_faith,
        R.string.title_hail_mary_hope,
        R.string.title_hail_mary_charity,
        R.string.title_glory_be,
        R.string.title_fatima_prayer,
        R.string.mystery_label,
        R.string.title_hail_holy_queen,
        R.string.title_rosary_prayer
    };

    public static int getMaxCount(boolean isIndonesian) {
        return isIndonesian ? 81 : 80;
    }

    public static int getPrayerTypeForCount(int totalCount) {
        return getPrayerTypeForCount(totalCount, false);
    }
    
    public static int getPrayerTypeForCount(int totalCount, boolean isIndonesian) {
        int max = getMaxCount(isIndonesian);
        if (totalCount < 1) totalCount = 1;
        if (totalCount > max) totalCount = max;

        // Intro
        if (totalCount == 1) return PRAYER_SIGN_OF_CROSS;
        if (totalCount == 2) return PRAYER_APOSTLES_CREED;

        int firstPrayersLen = isIndonesian ? 6 : 5;
        if (totalCount <= 2 + firstPrayersLen) {
            int idx = totalCount - 3;
            if (isIndonesian) {
                // 0: Kemuliaan, 1: Our Father, 2: HM Faith, 3: HM Hope, 4: HM Charity, 5: Kemuliaan
                if (idx == 0 || idx == 5) return PRAYER_GLORY_BE;
                if (idx == 1) return PRAYER_OUR_FATHER;
                if (idx == 2) return PRAYER_HAIL_MARY_FAITH;
                if (idx == 3) return PRAYER_HAIL_MARY_HOPE;
                return PRAYER_HAIL_MARY_CHARITY;
            } else {
                // 0: Our Father, 1: HM Faith, 2: HM Hope, 3: HM Charity, 4: Glory Be
                if (idx == 0) return PRAYER_OUR_FATHER;
                if (idx == 1) return PRAYER_HAIL_MARY_FAITH;
                if (idx == 2) return PRAYER_HAIL_MARY_HOPE;
                if (idx == 3) return PRAYER_HAIL_MARY_CHARITY;
                return PRAYER_GLORY_BE;
            }
        }

        int decadeStart = 2 + firstPrayersLen + 1; // 9 in ID, 8 in EN
        int conclusionStart = decadeStart + 70;    // 79 in ID, 78 in EN

        if (totalCount >= conclusionStart) {
            int cIdx = totalCount - conclusionStart;
            if (cIdx == 0) return PRAYER_HAIL_HOLY_QUEEN;
            if (cIdx == 1) return PRAYER_ROSARY_PRAYER;
            return PRAYER_SIGN_OF_CROSS;
        }

        int posInDecade = (totalCount - decadeStart) % 14;
        if (posInDecade == 0) return PRAYER_MYSTERY_ANNOUNCEMENT;
        if (posInDecade == 1) return PRAYER_OUR_FATHER;
        if (posInDecade >= 2 && posInDecade <= 11) return PRAYER_HAIL_MARY;
        if (posInDecade == 12) return PRAYER_GLORY_BE;
        return PRAYER_FATIMA;
    }
    
    public static int getTitleResourceId(int prayerType) {
        if (prayerType >= 0 && prayerType < PRAYER_TITLE_RESOURCE_IDS.length) {
            return PRAYER_TITLE_RESOURCE_IDS[prayerType];
        }
        return R.string.prayer;
    }

    public static int getDecadeForCount(int totalCount) {
        return getDecadeForCount(totalCount, false);
    }
    
    public static int getDecadeForCount(int totalCount, boolean isIndonesian) {
        int decadeStart = 2 + (isIndonesian ? 6 : 5) + 1;
        if (totalCount < decadeStart || totalCount >= decadeStart + 70) return 0;
        return ((totalCount - decadeStart) / 14) + 1;
    }

    public static int getHailMaryNumber(int totalCount) {
        return getHailMaryNumber(totalCount, false);
    }
    
    public static int getHailMaryNumber(int totalCount, boolean isIndonesian) {
        int decadeStart = 2 + (isIndonesian ? 6 : 5) + 1;
        if (totalCount < decadeStart || totalCount >= decadeStart + 70) return 0;
        int posInDecade = (totalCount - decadeStart) % 14;
        if (posInDecade >= 2 && posInDecade <= 11) {
            return posInDecade - 1;
        }
        return 0;
    }

    public static int getMysteryIndex(int totalCount) {
        return getMysteryIndex(totalCount, false);
    }
    
    public static int getMysteryIndex(int totalCount, boolean isIndonesian) {
        int decadeStart = 2 + (isIndonesian ? 6 : 5) + 1;
        if (totalCount < decadeStart || totalCount >= decadeStart + 70) return -1;
        return (totalCount - decadeStart) / 14;
    }
}
