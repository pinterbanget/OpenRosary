package com.openrosary.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.Log;
import java.util.Locale;

/**
 * Manages the state of a rosary praying session.
 * Clean state machine with Indonesian Kemuliaan custom support (81 steps).
 */
public class RosaryState {
    public static final int STAGE_INTRO = 0;          // Initial prayers (Sign of Cross, Apostles' Creed)
    public static final int STAGE_FIRST_PRAYERS = 1;  // First prayers (Our Father, 3 Hail Marys, Glory Be, + Kemuliaan in ID)
    public static final int STAGE_DECADE = 2;         // Decades (Mystery, Our Father, 10 Hail Marys, Glory Be, Fatima)
    public static final int STAGE_CONCLUSION = 3;     // Concluding prayers (Hail Holy Queen, Rosary Prayer, Sign of Cross)
    public static final int STAGE_COMPLETE = 4;       // Rosary is complete

    private static final String TAG = "RosaryState";

    private int stage;
    private int mysteryIndex;
    private int prayerCount;
    private String mysteryType;
    private int decadeCount;
    private boolean isComplete;
    private boolean isGoingBack = false;
    
    private static Context context;

    public RosaryState(String mysteryType) {
        this.mysteryType = (mysteryType != null && !mysteryType.isEmpty()) ? mysteryType : "joyful";
        this.stage = STAGE_INTRO;
        this.mysteryIndex = 0;
        this.prayerCount = 0;
        this.decadeCount = 1;
        this.isComplete = false;
    }
    
    public static void setContext(Context appContext) {
        if (appContext == null) {
            context = null;
            return;
        }
        try {
            SharedPreferences settings = appContext.getSharedPreferences("SimpleRosaryPrefs", 0);
            String languageCode = settings.getString("language", "en");
            Locale locale = new Locale(languageCode);
            Locale.setDefault(locale);

            Configuration config = new Configuration(appContext.getResources().getConfiguration());
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                android.os.LocaleList localeList = new android.os.LocaleList(locale);
                android.os.LocaleList.setDefault(localeList);
                config.setLocales(localeList);
            } else {
                config.setLocale(locale);
            }
            context = appContext.createConfigurationContext(config);
        } catch (Exception e) {
            context = appContext.getApplicationContext();
        }
    }

    public boolean isIndonesian() {
        if (context != null) {
            try {
                SharedPreferences settings = context.getSharedPreferences("SimpleRosaryPrefs", 0);
                String languageCode = settings.getString("language", "");
                if ("in".equalsIgnoreCase(languageCode) || "id".equalsIgnoreCase(languageCode)) {
                    return true;
                }
                String lang = context.getResources().getConfiguration().locale.getLanguage();
                return "in".equalsIgnoreCase(lang) || "id".equalsIgnoreCase(lang);
            } catch (Exception ignored) {}
        }
        return false;
    }

    private static Resources getResourcesSafely() {
        if (context == null) return null;
        return context.getResources();
    }

    public int getStage() { return stage; }
    public int getMysteryIndex() { return mysteryIndex; }
    public int getPrayerCount() { return prayerCount; }
    public String getMysteryType() { return mysteryType; }
    public int getDecadeCount() { return decadeCount; }
    public boolean isComplete() { return isComplete; }

    public int getMaxCount() {
        return PrayerStructure.getMaxCount(isIndonesian());
    }

    public int getTotalCount() {
        int totalCount = 0;
        if (stage == STAGE_INTRO) {
            totalCount = prayerCount;
        } else if (stage > STAGE_INTRO) {
            totalCount = 2; // Sign of Cross + Apostles' Creed
        }
        
        int firstPrayersLen = isIndonesian() ? 6 : 5;
        if (stage == STAGE_FIRST_PRAYERS) {
            totalCount += prayerCount;
        } else if (stage > STAGE_FIRST_PRAYERS) {
            totalCount += firstPrayersLen;
        }
        
        if (stage == STAGE_DECADE) {
            totalCount += (decadeCount - 1) * 14;
            totalCount += prayerCount;
        } else if (stage > STAGE_DECADE) {
            totalCount += 5 * 14;
        }
        
        if (stage == STAGE_CONCLUSION) {
            totalCount += prayerCount;
        } else if (stage == STAGE_COMPLETE) {
            totalCount = getMaxCount();
        }
        
        return Math.min(totalCount, getMaxCount());
    }

    /**
     * Advance the rosary to the next prayer
     * Returns the text of the prayer just advanced TO.
     */
    public String advance() {
        isGoingBack = false;
        
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
                return "Rosary Complete. God bless you.";
            default:
                return "God bless you.";
        }
    }

    private String handleIntroStage() {
        String prayerText;
        switch (prayerCount) {
            case 0:
                prayerText = RosaryPrayers.getSignOfCross();
                prayerCount++;
                break;
            case 1:
                prayerText = RosaryPrayers.getApostlesCreed();
                prayerCount++;
                break;
            default:
                stage = STAGE_FIRST_PRAYERS;
                prayerCount = 0;
                return advance();
        }
        return prayerText;
    }

    private String handleFirstPrayersStage() {
        String prayerText;
        boolean id = isIndonesian();
        
        if (id) {
            // Indonesian: 0 = Kemuliaan, 1 = Our Father, 2-4 = Hail Marys, 5 = Kemuliaan
            switch (prayerCount) {
                case 0:
                    prayerText = RosaryPrayers.getGloryBe();
                    prayerCount++;
                    break;
                case 1:
                    prayerText = RosaryPrayers.getOurFather();
                    prayerCount++;
                    break;
                case 2:
                    prayerText = RosaryPrayers.getHailMaryForIntro(1);
                    prayerCount++;
                    break;
                case 3:
                    prayerText = RosaryPrayers.getHailMaryForIntro(2);
                    prayerCount++;
                    break;
                case 4:
                    prayerText = RosaryPrayers.getHailMaryForIntro(3);
                    prayerCount++;
                    break;
                case 5:
                    prayerText = RosaryPrayers.getGloryBe();
                    prayerCount++;
                    break;
                default:
                    stage = STAGE_DECADE;
                    prayerCount = 0;
                    mysteryIndex = 0;
                    decadeCount = 1;
                    return advance();
            }
        } else {
            // Standard: 0 = Our Father, 1-3 = Hail Marys, 4 = Glory Be
            switch (prayerCount) {
                case 0:
                    prayerText = RosaryPrayers.getOurFather();
                    prayerCount++;
                    break;
                case 1:
                    prayerText = RosaryPrayers.getHailMaryForIntro(1);
                    prayerCount++;
                    break;
                case 2:
                    prayerText = RosaryPrayers.getHailMaryForIntro(2);
                    prayerCount++;
                    break;
                case 3:
                    prayerText = RosaryPrayers.getHailMaryForIntro(3);
                    prayerCount++;
                    break;
                case 4:
                    prayerText = RosaryPrayers.getGloryBe();
                    prayerCount++;
                    break;
                default:
                    stage = STAGE_DECADE;
                    prayerCount = 0;
                    mysteryIndex = 0;
                    decadeCount = 1;
                    return advance();
            }
        }
        return prayerText;
    }

    private String handleDecadeStage() {
        String prayerText;
        switch (prayerCount) {
            case 0:
                String[] descriptions = RosaryPrayers.getMysteryDescriptions(mysteryType);
                if (descriptions != null && mysteryIndex < descriptions.length) {
                    prayerText = descriptions[mysteryIndex];
                } else {
                    prayerText = "";
                }
                prayerCount++;
                break;
            case 1:
                prayerText = RosaryPrayers.getOurFather();
                prayerCount++;
                break;
            case 2: case 3: case 4: case 5: case 6:
            case 7: case 8: case 9: case 10: case 11:
                prayerText = RosaryPrayers.getHailMary();
                prayerCount++;
                break;
            case 12:
                prayerText = RosaryPrayers.getGloryBe();
                prayerCount++;
                break;
            case 13:
                prayerText = RosaryPrayers.getFatimaPrayer();
                prayerCount++;
                break;
            default:
                if (decadeCount < 5) {
                    decadeCount++;
                    mysteryIndex++;
                    prayerCount = 0;
                    return advance();
                } else {
                    stage = STAGE_CONCLUSION;
                    prayerCount = 0;
                    return advance();
                }
        }
        return prayerText;
    }

    private String handleConclusionStage() {
        String prayerText;
        switch (prayerCount) {
            case 0:
                prayerText = RosaryPrayers.getHailHolyQueen();
                prayerCount++;
                break;
            case 1:
                prayerText = RosaryPrayers.getRosaryPrayer();
                prayerCount++;
                break;
            case 2:
                prayerText = RosaryPrayers.getSignOfCross();
                prayerCount++;
                break;
            default:
                stage = STAGE_COMPLETE;
                isComplete = true;
                return "Rosary Complete. God bless you.";
        }
        return prayerText;
    }

    /**
     * Go back one prayer step
     */
    public String goBack() {
        isGoingBack = true;
        
        switch (stage) {
            case STAGE_INTRO:
                if (prayerCount > 0) {
                    prayerCount--;
                }
                break;
            case STAGE_FIRST_PRAYERS:
                if (prayerCount > 0) {
                    prayerCount--;
                } else {
                    stage = STAGE_INTRO;
                    prayerCount = 1; // Apostles' Creed
                }
                break;
            case STAGE_DECADE:
                if (prayerCount > 0) {
                    prayerCount--;
                } else {
                    if (decadeCount > 1) {
                        decadeCount--;
                        mysteryIndex--;
                        prayerCount = 13; // Fatima Prayer
                    } else {
                        stage = STAGE_FIRST_PRAYERS;
                        prayerCount = isIndonesian() ? 5 : 4; // Glory Be
                    }
                }
                break;
            case STAGE_CONCLUSION:
                if (prayerCount > 0) {
                    prayerCount--;
                } else {
                    stage = STAGE_DECADE;
                    decadeCount = 5;
                    mysteryIndex = 4;
                    prayerCount = 13; // Fatima Prayer
                }
                break;
            case STAGE_COMPLETE:
                stage = STAGE_CONCLUSION;
                prayerCount = 2; // Sign of the Cross
                isComplete = false;
                break;
        }
        
        return getPrayerTextByTotalCount(getTotalCount());
    }

    public boolean isBeadTypeTransition(boolean isAdvancing) {
        if (stage == STAGE_DECADE) {
            if (prayerCount == 0 || prayerCount == 1) return false;
        }

        if (isAdvancing) {
            if (stage == STAGE_DECADE) {
                if (prayerCount == 12) return true; // Hail Mary to Glory Be
                if (prayerCount == 1) return true;  // Our Father to first Hail Mary
            } else if (stage == STAGE_FIRST_PRAYERS && prayerCount == (isIndonesian() ? 5 : 4)) {
                return true;
            } else if (stage == STAGE_INTRO && prayerCount == 1) {
                return true;
            }
        } else {
            if (stage == STAGE_DECADE) {
                if (prayerCount == 2) return true;
                if (prayerCount == 12) return true;
            } else if (stage == STAGE_DECADE && prayerCount == 0 && mysteryIndex == 0) {
                return true;
            } else if (stage == STAGE_FIRST_PRAYERS && prayerCount == 0) {
                return true;
            } else if (stage == STAGE_CONCLUSION && prayerCount == 0) {
                return true;
            }
        }
        return false;
    }

    public String getPrayerTextByTotalCount(int totalCount) {
        boolean indonesian = isIndonesian();
        int max = PrayerStructure.getMaxCount(indonesian);
        if (totalCount < 1) totalCount = 1;
        if (totalCount > max) totalCount = max;

        int prayerType = PrayerStructure.getPrayerTypeForCount(totalCount, indonesian);
        int mysteryIdx = PrayerStructure.getMysteryIndex(totalCount, indonesian);

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
                String[] descriptions = RosaryPrayers.getMysteryDescriptions(mysteryType);
                if (descriptions != null && mysteryIdx >= 0 && mysteryIdx < descriptions.length) {
                    return descriptions[mysteryIdx];
                }
                return "";
            case PrayerStructure.PRAYER_HAIL_HOLY_QUEEN:
                return RosaryPrayers.getHailHolyQueen();
            case PrayerStructure.PRAYER_ROSARY_PRAYER:
                return RosaryPrayers.getRosaryPrayer();
            default:
                return "";
        }
    }

    public String getPrayerLabelByTotalCount(int totalCount) {
        Resources res = getResourcesSafely();
        if (res == null) return "Prayer";

        boolean indonesian = isIndonesian();
        int prayerType = PrayerStructure.getPrayerTypeForCount(totalCount, indonesian);

        if (prayerType == PrayerStructure.PRAYER_MYSTERY_ANNOUNCEMENT) {
            int decade = PrayerStructure.getDecadeForCount(totalCount, indonesian);
            return String.format(res.getString(R.string.mystery_label), decade);
        } else if (prayerType == PrayerStructure.PRAYER_HAIL_MARY) {
            int hmNum = PrayerStructure.getHailMaryNumber(totalCount, indonesian);
            return res.getString(R.string.title_hail_mary_decade, hmNum);
        } else {
            int resId = PrayerStructure.getTitleResourceId(prayerType);
            return res.getString(resId);
        }
    }

    public String getCurrentPrayerLabel() {
        return getPrayerLabelByTotalCount(getTotalCount());
    }

    public String getCurrentMysteryTitle() {
        return getMysteryTitleByTotalCount(getTotalCount());
    }

    public String getCurrentPrayerText() {
        return getPrayerTextByTotalCount(getTotalCount());
    }

    public String getMysteryTitleByTotalCount(int totalCount) {
        boolean indonesian = isIndonesian();
        int mysteryIdx = PrayerStructure.getMysteryIndex(totalCount, indonesian);
        if (mysteryIdx >= 0 && mysteryIdx < 5) {
            String[] titles = RosaryPrayers.getMysteryTitles(mysteryType);
            if (titles != null && mysteryIdx < titles.length) {
                return titles[mysteryIdx];
            }
        }
        return "";
    }
}
