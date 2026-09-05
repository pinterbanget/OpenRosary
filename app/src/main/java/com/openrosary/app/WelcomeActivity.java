package com.openrosary.app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import java.util.Calendar;
import java.util.Locale;

/**
 * Main 3-page swipeable hub of OpenRosary:
 * Page 0: Options (Theme, Language, Latin Prayers, 52dp Start Button)
 * Page 1: Prayer / Mystery Selection (Default)
 * Page 2: About / Information
 *
 * The top header (brand title + description) is FIXED and only the description text
 * updates in place as pages change ("options" -> "a simple native rosary tool." -> "about").
 */
public class WelcomeActivity extends BaseActivity implements AdapterView.OnItemSelectedListener {

    private static final String TAG = "WelcomeActivity";
    public static final String EXTRA_INITIAL_PAGE = "extra_initial_page";

    private static int sPendingPage = 1;

    private TextView brandTitleTextView;
    private TextView brandSubtitleTextView;
    private ViewPager viewPager;
    private TextView pillThemeLight, pillThemeDark, pillThemeOled;
    private Spinner languageSpinner;
    private SwitchCompat latinPrayersToggle;
    private boolean suppressLanguageSelection = false;
    private boolean isDevotionsExpanded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        try {
            int targetPage = getIntent().getIntExtra(EXTRA_INITIAL_PAGE, sPendingPage);
            sPendingPage = 1; // Reset to default

            brandTitleTextView = findViewById(R.id.brandTitleTextView);
            brandSubtitleTextView = findViewById(R.id.brandSubtitleTextView);

            setupViewPager(targetPage);
            checkForUpdates();
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + (e.getMessage() != null ? e.getMessage() : "unknown"), e);
        }
    }

    private void setupViewPager(int initialPage) {
        viewPager = findViewById(R.id.mainViewPager);
        if (viewPager == null) return;

        viewPager.setAdapter(new HubPagerAdapter());
        viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                updateSubtitleForPage(position);
            }
        });

        viewPager.setCurrentItem(initialPage, false);
        updateSubtitleForPage(initialPage);
    }

    private void updateSubtitleForPage(int position) {
        if (brandSubtitleTextView == null) return;
        int resId;
        if (position == 0) {
            resId = R.string.options;
        } else if (position == 2) {
            resId = R.string.about;
        } else {
            resId = R.string.brand_tagline;
        }
        brandSubtitleTextView.animate().alpha(0f).setDuration(80).withEndAction(() -> {
            brandSubtitleTextView.setText(resId);
            brandSubtitleTextView.animate().alpha(1f).setDuration(110).start();
        }).start();
    }

    private class HubPagerAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            LayoutInflater inflater = LayoutInflater.from(WelcomeActivity.this);
            View pageView;

            if (position == 0) {
                // Page 0: Options
                pageView = inflater.inflate(R.layout.page_options, container, false);
                setupOptionsPage(pageView);
            } else if (position == 1) {
                // Page 1: Mystery Selection (Center / Default)
                pageView = inflater.inflate(R.layout.page_mysteries, container, false);
                setupMysteriesPage(pageView);
            } else {
                // Page 2: About
                pageView = inflater.inflate(R.layout.page_about, container, false);
                setupAboutPage(pageView);
            }

            container.addView(pageView);
            return pageView;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView((View) object);
        }
    }

    // =========================================================================
    // Page 0: Options Setup
    // =========================================================================
    private void setupOptionsPage(View root) {
        pillThemeLight = root.findViewById(R.id.pillThemeLight);
        pillThemeDark = root.findViewById(R.id.pillThemeDark);
        pillThemeOled = root.findViewById(R.id.pillThemeOled);
        languageSpinner = root.findViewById(R.id.languageSpinner);
        latinPrayersToggle = root.findViewById(R.id.latinPrayersToggle);

        Button startButton = root.findViewById(R.id.startButton);
        if (startButton != null) {
            AnimationHelper.attachPressScale(startButton);
            startButton.setOnClickListener(v -> viewPager.setCurrentItem(1, true));
        }

        setupThemePills();
        setupLanguageSpinner();
        setupLatinPrayersToggle();
    }

    private void setupThemePills() {
        if (pillThemeLight == null || pillThemeDark == null || pillThemeOled == null) return;

        updateThemePillsHighlight(getThemeMode());

        pillThemeLight.setOnClickListener(v -> applySelectedTheme(THEME_MODE_LIGHT));
        pillThemeDark.setOnClickListener(v -> applySelectedTheme(THEME_MODE_DARK));
        pillThemeOled.setOnClickListener(v -> applySelectedTheme(THEME_MODE_AMOLED));
    }

    private void updateThemePillsHighlight(String currentTheme) {
        setPillState(pillThemeLight, THEME_MODE_LIGHT.equals(currentTheme));
        setPillState(pillThemeDark, THEME_MODE_DARK.equals(currentTheme));
        setPillState(pillThemeOled, THEME_MODE_AMOLED.equals(currentTheme));
    }

    private void setPillState(TextView pill, boolean isSelected) {
        if (pill == null) return;
        if (isSelected) {
            TypedValue typedValue = new TypedValue();
            getTheme().resolveAttribute(R.attr.segmentedPillActiveStyle, typedValue, true);
            pill.setBackgroundResource(typedValue.resourceId);

            TypedValue colorVal = new TypedValue();
            getTheme().resolveAttribute(R.attr.segmentedPillActiveTextColor, colorVal, true);
            pill.setTextColor(colorVal.data);
            pill.setElevation(dp(1.5f));
            AnimationHelper.animatePillSelection(pill);
        } else {
            pill.setBackgroundResource(R.drawable.segmented_pill_inactive);

            TypedValue colorVal = new TypedValue();
            getTheme().resolveAttribute(R.attr.segmentedPillInactiveTextColor, colorVal, true);
            pill.setTextColor(colorVal.data);
            pill.setElevation(0);
        }
    }

    private void applySelectedTheme(String themeMode) {
        try {
            if (themeMode.equals(getThemeMode())) return;

            sPendingPage = 0; // Stay on Options page
            setThemeMode(themeMode);
            if (THEME_MODE_LIGHT.equals(themeMode)) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            }

            recreate();
            overridePendingTransition(0, 0);
        } catch (Exception e) {
            Log.e(TAG, "Error applying theme: " + e.getMessage());
        }
    }

    private void setupLanguageSpinner() {
        if (languageSpinner == null) return;

        String[] languages = new String[] {
                getString(R.string.language_english),
                getString(R.string.language_indonesian)
        };

        ArrayAdapter<String> languageAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                languages
        );
        languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageSpinner.setAdapter(languageAdapter);

        Locale currentLocale = getResources().getConfiguration().getLocales().get(0);
        String currentLangCode = currentLocale.getLanguage();

        suppressLanguageSelection = true;
        languageSpinner.setSelection(currentLangCode.equals("in") ? 1 : 0, false);
        languageSpinner.post(() -> {
            suppressLanguageSelection = false;
            languageSpinner.setOnItemSelectedListener(WelcomeActivity.this);
        });
    }

    private void setupLatinPrayersToggle() {
        if (latinPrayersToggle == null) return;
        latinPrayersToggle.setOnCheckedChangeListener(null);
        latinPrayersToggle.setChecked(areLatinPrayersEnabled());
        latinPrayersToggle.setOnCheckedChangeListener((bv, isChecked) -> setLatinPrayersEnabled(isChecked));
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (suppressLanguageSelection) return;
        String selectedLangCode = position == 0 ? "en" : "in";
        Locale currentLocale = getResources().getConfiguration().getLocales().get(0);
        String currentLangCode = currentLocale.getLanguage();
        if (!selectedLangCode.equals(currentLangCode)) {
            sPendingPage = 0; // Stay on options page after language change
            setAppLocale(selectedLangCode);
            recreate();
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}

    // =========================================================================
    // Page 1: Mystery Selection Setup
    // =========================================================================
    private void setupMysteriesPage(View root) {
        View joyfulCard = root.findViewById(R.id.joyfulMysteriesCard);
        View sorrowfulCard = root.findViewById(R.id.sorrowfulMysteriesCard);
        View gloriousCard = root.findViewById(R.id.gloriousMysteriesCard);
        View luminousCard = root.findViewById(R.id.luminousMysteriesCard);

        AnimationHelper.attachPressScale(joyfulCard);
        AnimationHelper.attachPressScale(sorrowfulCard);
        AnimationHelper.attachPressScale(gloriousCard);
        AnimationHelper.attachPressScale(luminousCard);

        if (joyfulCard != null) joyfulCard.setOnClickListener(v -> startRosary("joyful"));
        if (sorrowfulCard != null) sorrowfulCard.setOnClickListener(v -> startRosary("sorrowful"));
        if (gloriousCard != null) gloriousCard.setOnClickListener(v -> startRosary("glorious"));
        if (luminousCard != null) luminousCard.setOnClickListener(v -> startRosary("luminous"));

        setupRecommendationPill(root);
        setupOtherDevotionsDropdown(root);

        // Bottom buttons: Options and Info (52dp)
        Button fixedOptionsButton = root.findViewById(R.id.fixedOptionsButton);
        Button fixedInfoButton = root.findViewById(R.id.fixedInfoButton);

        if (fixedOptionsButton != null) {
            AnimationHelper.attachPressScale(fixedOptionsButton);
            fixedOptionsButton.setOnClickListener(v -> viewPager.setCurrentItem(0, true));
        }

        if (fixedInfoButton != null) {
            AnimationHelper.attachPressScale(fixedInfoButton);
            fixedInfoButton.setOnClickListener(v -> viewPager.setCurrentItem(2, true));
        }

        equalizeCardHeights(root);
    }

    private void setupRecommendationPill(View root) {
        int dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        int targetPillId = -1;

        switch (dayOfWeek) {
            case Calendar.MONDAY:
            case Calendar.SATURDAY:
                targetPillId = R.id.joyfulRecommendationPill;
                break;
            case Calendar.TUESDAY:
            case Calendar.FRIDAY:
                targetPillId = R.id.sorrowfulRecommendationPill;
                break;
            case Calendar.WEDNESDAY:
            case Calendar.SUNDAY:
                targetPillId = R.id.gloriousRecommendationPill;
                break;
            case Calendar.THURSDAY:
                targetPillId = R.id.luminousRecommendationPill;
                break;
        }

        if (targetPillId != -1) {
            View pill = root.findViewById(targetPillId);
            if (pill != null) {
                pill.setVisibility(View.VISIBLE);
            }
        }
    }

    private void setupOtherDevotionsDropdown(View root) {
        View otherDevotionsButton = root.findViewById(R.id.otherDevotionsButton);
        View otherDevotionsContainer = root.findViewById(R.id.otherDevotionsExpandableContainer);
        TextView otherDevotionsChevron = root.findViewById(R.id.otherDevotionsChevronText);
        ScrollView mainScrollView = root.findViewById(R.id.mainScrollView);
        View otherDevotionsCard = root.findViewById(R.id.otherDevotionsCard);

        if (otherDevotionsButton != null && otherDevotionsContainer != null) {
            AnimationHelper.attachPressScale(otherDevotionsButton);
            otherDevotionsButton.setOnClickListener(v -> {
                isDevotionsExpanded = !isDevotionsExpanded;
                if (isDevotionsExpanded) {
                    otherDevotionsContainer.setVisibility(View.VISIBLE);
                    otherDevotionsContainer.setAlpha(0f);
                    otherDevotionsContainer.animate().alpha(1f).setDuration(220).start();
                    if (otherDevotionsChevron != null) {
                        otherDevotionsChevron.animate().rotation(90f).setDuration(200).start();
                    }
                    if (mainScrollView != null && otherDevotionsCard != null) {
                        mainScrollView.postDelayed(() -> {
                            View parentFrame = (View) otherDevotionsCard.getParent();
                            int targetY = parentFrame != null ? parentFrame.getTop() : otherDevotionsCard.getTop();
                            mainScrollView.smoothScrollTo(0, targetY);
                        }, 120);
                    }
                } else {
                    if (otherDevotionsChevron != null) {
                        otherDevotionsChevron.animate().rotation(0f).setDuration(200).start();
                    }
                    otherDevotionsContainer.animate().alpha(0f).setDuration(160).withEndAction(() -> {
                        otherDevotionsContainer.setVisibility(View.GONE);
                    }).start();
                }
            });
        }

        bindDevotionItem(root, R.id.devotion77Item, "our-father-77");
        bindDevotionItem(root, R.id.devotionMercyItem, "divine-mercy");
        bindDevotionItem(root, R.id.devotionSorrowsItem, "seven-sorrows");
        bindDevotionItem(root, R.id.devotionFranciscanItem, "franciscan-crown");
    }

    private void bindDevotionItem(View root, int viewId, String devotionId) {
        View item = root.findViewById(viewId);
        if (item != null) {
            AnimationHelper.attachPressScale(item);
            item.setOnClickListener(v -> {
                Intent intent = new Intent(WelcomeActivity.this, DevotionActivity.class);
                intent.putExtra("devotionType", devotionId);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }
    }

    private void equalizeCardHeights(View root) {
        View[] cards = new View[] {
                root.findViewById(R.id.joyfulMysteriesCard),
                root.findViewById(R.id.sorrowfulMysteriesCard),
                root.findViewById(R.id.gloriousMysteriesCard),
                root.findViewById(R.id.luminousMysteriesCard),
                root.findViewById(R.id.otherDevotionsButton)
        };

        final int baseHeight = dp(90);
        for (View card : cards) {
            if (card == null) continue;
            ViewGroup.LayoutParams lp = card.getLayoutParams();
            if (lp != null) {
                lp.height = baseHeight;
                card.setLayoutParams(lp);
            }
            card.setMinimumHeight(baseHeight);
        }

        View first = cards[0];
        if (first != null) {
            first.post(() -> {
                int maxHeight = baseHeight;
                for (View card : cards) {
                    if (card == null) continue;
                    int widthSpec = View.MeasureSpec.makeMeasureSpec(
                            card.getWidth() > 0 ? card.getWidth() : getResources().getDisplayMetrics().widthPixels,
                            View.MeasureSpec.AT_MOST);
                    int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
                    card.measure(widthSpec, heightSpec);
                    int measured = card.getMeasuredHeight();
                    if (measured > maxHeight) {
                        maxHeight = measured;
                    }
                }
                for (View card : cards) {
                    if (card == null) continue;
                    ViewGroup.LayoutParams lp = card.getLayoutParams();
                    if (lp != null && lp.height != maxHeight) {
                        lp.height = maxHeight;
                        card.setLayoutParams(lp);
                    }
                }
            });
        }
    }

    private void startRosary(String mysteryType) {
        Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
        intent.putExtra("mysteryType", mysteryType);
        intent.putExtra("mystery_type", mysteryType);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    // =========================================================================
    // Page 2: About Setup
    // =========================================================================
    private void setupAboutPage(View root) {
        Button aboutBackButton = root.findViewById(R.id.aboutBackButton);
        if (aboutBackButton != null) {
            AnimationHelper.attachPressScale(aboutBackButton);
            aboutBackButton.setOnClickListener(v -> viewPager.setCurrentItem(1, true));
        }
    }

    private int dp(float val) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, val, getResources().getDisplayMetrics()));
    }

    private void checkForUpdates() {
        try {
            UpdateChecker updateChecker = new UpdateChecker(this);
            updateChecker.checkForUpdates();
        } catch (Exception e) {
            Log.e(TAG, "Error checking updates: " + e.getMessage());
        }
    }

    @Override
    public void onBackPressed() {
        if (viewPager != null && viewPager.getCurrentItem() != 1) {
            // Return to central mysteries page
            viewPager.setCurrentItem(1, true);
        } else {
            super.onBackPressed();
        }
    }
}
