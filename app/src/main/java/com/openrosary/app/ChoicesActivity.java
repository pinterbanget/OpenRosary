package com.openrosary.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class ChoicesActivity extends BaseActivity {

    private static final String TAG = "ChoicesActivity";
    private boolean isDarkMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            // Load preferences and apply theme and language
            // loadPreferences(); // REMOVED - BaseActivity handles this now
            
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_choices);
            
            // Get the isDarkMode value from preferences to use for UI elements
            SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
            if (settings != null) {
                isDarkMode = settings.getBoolean(THEME_KEY, false);
            }
              // Update title text color based on theme
            TextView titleTextView = findViewById(R.id.titleTextView);
            if (titleTextView != null) {
                if (isDarkMode) {
                    titleTextView.setTextColor(getResources().getColor(R.color.colorAccent));
                } else {
                    titleTextView.setTextColor(getResources().getColor(R.color.colorPrimary));
                }
            }            // Set up mystery cards
            setupMysteryCards();
            
            // Update mystery card text colors based on theme
            updateMysteryCardTextColors();
            
            // Set up bottom buttons
            setupBottomButtons();
            
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + (e.getMessage() != null ? e.getMessage() : "unknown"));        }
    }

    // loadPreferences() and setAppLocale() methods removed - using BaseActivity's implementation instead
      private void setupMysteryCards() {
        try {
            // Get the suggested mystery for today
            String suggestedMystery = RosaryPrayers.getSuggestedMysteryForToday();
            String recommendationText = getString(R.string.recommended_for_today);
              // Setup Joyful Mysteries card
            LinearLayout joyfulCard = findViewById(R.id.joyfulMysteriesCard);
            TextView joyfulRecommendationPill = findViewById(R.id.joyfulRecommendationPill);
            setupMysteryCard(joyfulCard, "joyful", suggestedMystery, joyfulRecommendationPill, recommendationText);
            
            // Setup Sorrowful Mysteries card
            LinearLayout sorrowfulCard = findViewById(R.id.sorrowfulMysteriesCard);
            TextView sorrowfulRecommendationPill = findViewById(R.id.sorrowfulRecommendationPill);
            setupMysteryCard(sorrowfulCard, "sorrowful", suggestedMystery, sorrowfulRecommendationPill, recommendationText);
            
            // Setup Glorious Mysteries card
            LinearLayout gloriousCard = findViewById(R.id.gloriousMysteriesCard);
            TextView gloriousRecommendationPill = findViewById(R.id.gloriousRecommendationPill);
            setupMysteryCard(gloriousCard, "glorious", suggestedMystery, gloriousRecommendationPill, recommendationText);
              // Setup Luminous Mysteries card
            LinearLayout luminousCard = findViewById(R.id.luminousMysteriesCard);
            TextView luminousRecommendationPill = findViewById(R.id.luminousRecommendationPill);
            setupMysteryCard(luminousCard, "luminous", suggestedMystery, luminousRecommendationPill, recommendationText);
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting up mystery cards: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
        }}
      private void setupMysteryCard(LinearLayout card, String mysteryType, String suggestedMystery,
                                 TextView recommendationPill, String recommendationText) {
        if (card != null) {
            // Set click listener for the card
            card.setOnClickListener(v -> launchMainActivity(mysteryType));
            
            // Show recommendation pill if this mystery is suggested for today
            if (recommendationPill != null && mysteryType.equals(suggestedMystery)) {
                recommendationPill.setText(recommendationText);
                recommendationPill.setVisibility(View.VISIBLE);
            } else if (recommendationPill != null) {
                recommendationPill.setVisibility(View.GONE);
            }
        }
    }    private void setupBottomButtons() {
        try {
            // Just use fixed bottom buttons for now to avoid the positioning complexity
            LinearLayout fixedBottomLayout = findViewById(R.id.fixedBottomButtonsLayout);
            LinearLayout scrollableBottomLayout = findViewById(R.id.scrollableBottomButtonsLayout);
            
            if (fixedBottomLayout != null) {
                fixedBottomLayout.setVisibility(View.VISIBLE);
            }
            if (scrollableBottomLayout != null) {
                scrollableBottomLayout.setVisibility(View.GONE);
            }
            
            // Set up click listeners for both sets of buttons
            setupButtonClickListeners();
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting up bottom buttons: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
        }
    }    
    private void setupButtonClickListeners() {
        try {
            // Fixed bottom buttons
            Button fixedOptionsButton = findViewById(R.id.fixedOptionsButton);
            Button fixedInfoButton = findViewById(R.id.fixedInfoButton);
            
            // Scrollable bottom buttons
            Button scrollableOptionsButton = findViewById(R.id.scrollableOptionsButton);
            Button scrollableInfoButton = findViewById(R.id.scrollableInfoButton);
            
            // Options button click listener
            View.OnClickListener optionsClickListener = v -> {
                Intent intent = new Intent(ChoicesActivity.this, WelcomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            };
            
            // Info button click listener
            View.OnClickListener infoClickListener = v -> {
                Intent intent = new Intent(ChoicesActivity.this, InfoActivity.class);
                startActivity(intent);
            };
            
            // Apply listeners to both sets of buttons
            if (fixedOptionsButton != null) fixedOptionsButton.setOnClickListener(optionsClickListener);
            if (fixedInfoButton != null) fixedInfoButton.setOnClickListener(infoClickListener);
            if (scrollableOptionsButton != null) scrollableOptionsButton.setOnClickListener(optionsClickListener);
            if (scrollableInfoButton != null) scrollableInfoButton.setOnClickListener(infoClickListener);
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting up button click listeners: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
        }
    }
    
    private void launchMainActivity(String mysteryType) {
        try {
            Intent intent = new Intent(ChoicesActivity.this, MainActivity.class);
            intent.putExtra("mysteryType", mysteryType);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error launching MainActivity: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
        }
    }
    
    private void updateMysteryCardTextColors() {
        try {
            int textColor = isDarkMode ? 
                getResources().getColor(R.color.colorAccent) : 
                getResources().getColor(R.color.colorPrimary);
            
            // Update Joyful Mysteries text colors
            TextView joyfulTitleText = findViewById(R.id.joyfulTitleText);
            TextView joyfulDescriptionText = findViewById(R.id.joyfulDescriptionText);
            if (joyfulTitleText != null) joyfulTitleText.setTextColor(textColor);
            if (joyfulDescriptionText != null) joyfulDescriptionText.setTextColor(textColor);
            
            // Update Sorrowful Mysteries text colors
            TextView sorrowfulTitleText = findViewById(R.id.sorrowfulTitleText);
            TextView sorrowfulDescriptionText = findViewById(R.id.sorrowfulDescriptionText);
            if (sorrowfulTitleText != null) sorrowfulTitleText.setTextColor(textColor);
            if (sorrowfulDescriptionText != null) sorrowfulDescriptionText.setTextColor(textColor);
            
            // Update Glorious Mysteries text colors
            TextView gloriousTitleText = findViewById(R.id.gloriousTitleText);
            TextView gloriousDescriptionText = findViewById(R.id.gloriousDescriptionText);
            if (gloriousTitleText != null) gloriousTitleText.setTextColor(textColor);
            if (gloriousDescriptionText != null) gloriousDescriptionText.setTextColor(textColor);
            
            // Update Luminous Mysteries text colors
            TextView luminousTitleText = findViewById(R.id.luminousTitleText);
            TextView luminousDescriptionText = findViewById(R.id.luminousDescriptionText);
            if (luminousTitleText != null) luminousTitleText.setTextColor(textColor);
            if (luminousDescriptionText != null) luminousDescriptionText.setTextColor(textColor);
            
        } catch (Exception e) {
            Log.e(TAG, "Error updating mystery card text colors: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
        }
    }
    
    @Override
    public void onBackPressed() {
        // Just minimize the app when back is pressed from choices screen
        // This prevents going back to the welcome screen accidentally
        moveTaskToBack(true);
    }
}