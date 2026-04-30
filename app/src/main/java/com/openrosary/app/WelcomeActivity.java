package com.openrosary.app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.view.GestureDetectorCompat;

import java.util.Locale;

public class WelcomeActivity extends BaseActivity implements GestureDetector.OnGestureListener, AdapterView.OnItemSelectedListener {

    private static final String TAG = "WelcomeActivity";
    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;

    private Spinner themeSpinner;
    private Spinner languageSpinner;
    private Button startButton;
    private SwitchCompat latinPrayersToggle;
    private GestureDetectorCompat gestureDetector;
    private boolean suppressThemeSelection = false;
    private boolean suppressLanguageSelection = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        try {
            initializeViews();
            setupGestureDetector();
            setupThemeSpinner();
            setupLanguageSpinner();
            setupLatinPrayersToggle();
            setupStartButton();
            checkForUpdates();
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
        }
    }

    private void initializeViews() {
        try {
            themeSpinner = findViewById(R.id.themeSpinner);
            languageSpinner = findViewById(R.id.languageSpinner);
            latinPrayersToggle = findViewById(R.id.latinPrayersToggle);
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

    private void setupThemeSpinner() {
        try {
            if (themeSpinner == null) {
                return;
            }

            ArrayAdapter<CharSequence> themeAdapter = ArrayAdapter.createFromResource(
                    this,
                    R.array.theme_options,
                    android.R.layout.simple_spinner_item
            );
            themeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            themeSpinner.setAdapter(themeAdapter);

            suppressThemeSelection = true;
            themeSpinner.setSelection(getThemeSelectionIndex(getThemeMode()), false);
            themeSpinner.post(() -> {
                suppressThemeSelection = false;
                themeSpinner.setOnItemSelectedListener(WelcomeActivity.this);
            });
        } catch (Exception e) {
            Log.e(TAG, "Error setting up theme spinner: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
        }
    }

    private void setupLanguageSpinner() {
        try {
            if (languageSpinner == null) {
                Log.e(TAG, "Language spinner is null");
                return;
            }

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
        } catch (Exception e) {
            Log.e(TAG, "Error setting up language spinner: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
        }
    }

    private void setupLatinPrayersToggle() {
        try {
            if (latinPrayersToggle == null) {
                Log.e(TAG, "Latin prayers toggle is null");
                return;
            }

            latinPrayersToggle.setOnCheckedChangeListener(null);
            latinPrayersToggle.setChecked(areLatinPrayersEnabled());
            latinPrayersToggle.setOnCheckedChangeListener((buttonView, isChecked) ->
                    setLatinPrayersEnabled(isChecked));
        } catch (Exception e) {
            Log.e(TAG, "Error setting up Latin prayers toggle: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
        }
    }

    private void setupStartButton() {
        try {
            if (startButton == null) {
                return;
            }

            startButton.setOnClickListener(v -> {
                try {
                    startButton.setEnabled(false);

                    Intent intent = new Intent(WelcomeActivity.this, ChoicesActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

                    v.postDelayed(() -> {
                        if (startButton != null) {
                            startButton.setEnabled(true);
                        }
                    }, 300);
                } catch (Exception e) {
                    Log.e(TAG, "Error navigating to ChoicesActivity: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
                    startActivity(new Intent(WelcomeActivity.this, ChoicesActivity.class));
                    if (startButton != null) {
                        startButton.setEnabled(true);
                    }
                }
            });
            startButton.setText(R.string.start_button);
        } catch (Exception e) {
            Log.e(TAG, "Error setting up start button: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
        }
    }

    private void applySelectedTheme(String themeMode) {
        try {
            String normalizedThemeMode = normalizeThemeMode(themeMode);
            setThemeMode(normalizedThemeMode);

            if (THEME_MODE_LIGHT.equals(normalizedThemeMode)) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            }

            recreate();
        } catch (Exception e) {
            Log.e(TAG, "Error applying selected theme: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
        }
    }

    private String normalizeThemeMode(String themeMode) {
        if (THEME_MODE_AMOLED.equals(themeMode)) {
            return THEME_MODE_AMOLED;
        }
        if (THEME_MODE_DARK.equals(themeMode)) {
            return THEME_MODE_AMOLED;
        }
        return THEME_MODE_LIGHT;
    }

    private int getThemeSelectionIndex(String themeMode) {
        if (THEME_MODE_AMOLED.equals(themeMode)) {
            return 1;
        }
        return 0;
    }

    private String getThemeModeForSelection(int position) {
        if (position == 1) {
            return THEME_MODE_AMOLED;
        }
        return THEME_MODE_LIGHT;
    }

    private void cycleThemeSelection(boolean forward) {
        if (themeSpinner == null) {
            return;
        }

        int currentPosition = themeSpinner.getSelectedItemPosition();
        int itemCount = themeSpinner.getCount();
        int nextPosition = forward
                ? (currentPosition + 1) % itemCount
                : (currentPosition - 1 + itemCount) % itemCount;
        themeSpinner.setSelection(nextPosition);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
        try {
            if (parent.getId() == R.id.themeSpinner) {
                if (suppressThemeSelection) {
                    return;
                }

                String selectedThemeMode = getThemeModeForSelection(position);
                if (!selectedThemeMode.equals(getThemeMode())) {
                    applySelectedTheme(selectedThemeMode);
                }
                return;
            }

            if (parent.getId() == R.id.languageSpinner) {
                if (suppressLanguageSelection) {
                    return;
                }

                String selectedLangCode = position == 0 ? "en" : "in";
                Locale currentLocale = getResources().getConfiguration().getLocales().get(0);
                String currentLangCode = currentLocale.getLanguage();

                if (!selectedLangCode.equals(currentLangCode)) {
                    setAppLocale(selectedLangCode);
                    recreate();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling selection: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
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
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        try {
            if (e1 == null || e2 == null) {
                return false;
            }

            float diffX = e2.getX() - e1.getX();
            float diffY = e2.getY() - e1.getY();

            if (Math.abs(diffX) > Math.abs(diffY)
                    && Math.abs(diffX) > SWIPE_THRESHOLD
                    && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                cycleThemeSelection(diffX < 0);
                return true;
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

    private void checkForUpdates() {
        try {
            UpdateChecker updateChecker = new UpdateChecker(this);
            updateChecker.checkForUpdates();
        } catch (Exception e) {
            Log.e(TAG, "Error checking for updates: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
        }
    }
}
