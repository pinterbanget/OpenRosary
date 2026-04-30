package com.openrosary.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class ChoicesActivity extends BaseActivity {

    private static final String TAG = "ChoicesActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_choices);

            applyResponsiveLayout();
            
            // Set up mystery cards
            setupMysteryCards();
            
            // Set up bottom buttons
            setupBottomButtons();
            
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
        }
    }

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
        }
    }

    private void setupMysteryCard(LinearLayout card, String mysteryType, String suggestedMystery,
                                 TextView recommendationPill, String recommendationText) {
        if (card != null) {
            // Set click listener for the card
            card.setOnClickListener(v -> launchMainActivity(mysteryType));
            
            // Store current padding to re-apply it after background change
            int paddingLeft = card.getPaddingLeft();
            int paddingTop = card.getPaddingTop();
            int paddingRight = card.getPaddingRight();
            int paddingBottom = card.getPaddingBottom();
            
            TypedValue typedValue = new TypedValue();
            if (recommendationPill != null && mysteryType.equals(suggestedMystery)) {
                // Use theme attribute for highlighted background
                getTheme().resolveAttribute(R.attr.mysteryCardHighlightStyle, typedValue, true);
                card.setBackgroundResource(typedValue.resourceId);
                recommendationPill.setText(recommendationText);
                recommendationPill.setVisibility(View.VISIBLE);
            } else {
                // Use theme attribute for normal background
                getTheme().resolveAttribute(R.attr.mysteryCardBorderStyle, typedValue, true);
                card.setBackgroundResource(typedValue.resourceId);
                if (recommendationPill != null) recommendationPill.setVisibility(View.GONE);
            }
            
            // Re-apply the padding
            card.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
        }
    }

    private void applyResponsiveLayout() {
        try {
            Configuration configuration = getResources().getConfiguration();
            int screenHeightDp = configuration.screenHeightDp;
            int screenWidthDp = configuration.screenWidthDp;
            float scale;

            if (screenHeightDp <= 740) {
                scale = 0.82f;
            } else if (screenHeightDp <= 820) {
                scale = 0.88f;
            } else if (screenHeightDp <= 900) {
                scale = 0.94f;
            } else if (screenWidthDp <= 360) {
                scale = 0.90f;
            } else if (screenWidthDp <= 380) {
                scale = 0.94f;
            } else {
                scale = 1.0f;
            }

            if (scale >= 0.999f) {
                return;
            }

            LinearLayout headerContainer = findViewById(R.id.headerContainer);
            ScrollView mainScrollView = findViewById(R.id.mainScrollView);
            LinearLayout fixedBottomButtonsLayout = findViewById(R.id.fixedBottomButtonsLayout);
            TextView titleTextView = findViewById(R.id.titleTextView);
            TextView subtitleTextView = findViewById(R.id.choicesSubtitleTextView);

            scaleTopLevelSpacing(headerContainer, mainScrollView, fixedBottomButtonsLayout, scale);

            setScaledTextSize(titleTextView, 30f * scale);
            setScaledTextSize(subtitleTextView, 15f * scale);

            int[] cardIds = {
                    R.id.joyfulMysteriesCard,
                    R.id.sorrowfulMysteriesCard,
                    R.id.gloriousMysteriesCard,
                    R.id.luminousMysteriesCard
            };
            int[] imageIds = {
                    R.id.joyfulImagePlaceholder,
                    R.id.sorrowfulImagePlaceholder,
                    R.id.gloriousImagePlaceholder,
                    R.id.luminousImagePlaceholder
            };
            int[] titleIds = {
                    R.id.joyfulTitleText,
                    R.id.sorrowfulTitleText,
                    R.id.gloriousTitleText,
                    R.id.luminousTitleText
            };
            int[] descriptionIds = {
                    R.id.joyfulDescriptionText,
                    R.id.sorrowfulDescriptionText,
                    R.id.gloriousDescriptionText,
                    R.id.luminousDescriptionText
            };
            int[] recommendationIds = {
                    R.id.joyfulRecommendationPill,
                    R.id.sorrowfulRecommendationPill,
                    R.id.gloriousRecommendationPill,
                    R.id.luminousRecommendationPill
            };
            int[] chevronIds = {
                    R.id.joyfulChevronText,
                    R.id.sorrowfulChevronText,
                    R.id.gloriousChevronText,
                    R.id.luminousChevronText
            };

            for (int i = 0; i < cardIds.length; i++) {
                LinearLayout card = findViewById(cardIds[i]);
                ImageView image = findViewById(imageIds[i]);
                TextView title = findViewById(titleIds[i]);
                TextView description = findViewById(descriptionIds[i]);
                TextView recommendation = findViewById(recommendationIds[i]);
                TextView chevron = findViewById(chevronIds[i]);
                scaleMysteryCard(card, image, title, description, recommendation, chevron, scale);
            }

            Button fixedOptionsButton = findViewById(R.id.fixedOptionsButton);
            Button fixedInfoButton = findViewById(R.id.fixedInfoButton);
            scaleBottomButton(fixedOptionsButton, scale);
            scaleBottomButton(fixedInfoButton, scale);
        } catch (Exception e) {
            Log.e(TAG, "Error applying responsive layout: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
        }
    }

    private void scaleTopLevelSpacing(View headerContainer, View mainScrollView, View fixedBottomButtonsLayout, float scale) {
        updateMargins(headerContainer, dp(24), dp(28 * scale), dp(24), 0);
        updateMargins(mainScrollView, 0, dp(20 * scale), 0, 0);
        setHorizontalPadding(mainScrollView, dp(20 * scale), dp(20 * scale));
        updateMargins(fixedBottomButtonsLayout, dp(20 * scale), 0, dp(20 * scale), dp(24 * scale));
    }

    private void scaleMysteryCard(LinearLayout card, ImageView image, TextView title,
                                  TextView description, TextView recommendation, TextView chevron, float scale) {
        if (card == null) return;

        View parent = (View) card.getParent();
        if (parent instanceof FrameLayout) {
            updateMargins(parent, 0, 0, 0, dp(14 * scale));
        }

        updateMargins(card, 0, dp(12 * scale), 0, 0);
        int cardPadding = dp(18 * scale);
        card.setPadding(cardPadding, cardPadding, cardPadding, cardPadding);
        card.setMinimumHeight(dp(118 * scale));

        if (image != null) {
            ViewGroup.MarginLayoutParams imageParams = (ViewGroup.MarginLayoutParams) image.getLayoutParams();
            imageParams.width = dp(64 * scale);
            imageParams.height = dp(64 * scale);
            imageParams.setMarginEnd(dp(16 * scale));
            image.setLayoutParams(imageParams);
        }

        setScaledTextSize(title, 19f * scale);
        setScaledTextSize(description, 13f * scale);
        if (description != null) {
            description.setMaxLines(2);
        }
        updateTopMargin(description, dp(6 * scale));

        if (recommendation != null) {
            setScaledTextSize(recommendation, 11f * scale);
            recommendation.setPadding(dp(12 * scale), 0, dp(12 * scale), 0);
            ViewGroup.LayoutParams params = recommendation.getLayoutParams();
            params.height = dp(24 * scale);
            recommendation.setLayoutParams(params);
        }

        if (chevron != null) {
            setScaledTextSize(chevron, 26f * scale);
            updateMargins(chevron, dp(12 * scale), 0, 0, 0);
        }
    }

    private void scaleBottomButton(Button button, float scale) {
        if (button == null) return;
        button.setMinimumHeight(dp(52 * scale));
        int padding = dp(16 * scale);
        button.setPadding(button.getPaddingLeft(), padding, button.getPaddingRight(), padding);
        setScaledTextSize(button, 16f * scale);
    }

    private void setScaledTextSize(TextView textView, float sizeSp) {
        if (textView != null) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        }
    }

    private void updateTopMargin(View view, int topMarginPx) {
        if (view == null) return;
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) layoutParams;
            marginParams.topMargin = topMarginPx;
            view.setLayoutParams(marginParams);
        }
    }

    private void updateMargins(View view, int start, int top, int end, int bottom) {
        if (view == null) return;
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) layoutParams;
            marginParams.setMargins(start, top, end, bottom);
            view.setLayoutParams(marginParams);
        }
    }

    private void setHorizontalPadding(View view, int start, int end) {
        if (view != null) {
            view.setPadding(start, view.getPaddingTop(), end, view.getPaddingBottom());
        }
    }

    private int dp(float value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                Resources.getSystem().getDisplayMetrics()));
    }

    private void setupBottomButtons() {
        try {
            LinearLayout fixedBottomLayout = findViewById(R.id.fixedBottomButtonsLayout);
            LinearLayout scrollableBottomLayout = findViewById(R.id.scrollableBottomButtonsLayout);
            
            if (fixedBottomLayout != null) {
                fixedBottomLayout.setVisibility(View.VISIBLE);
            }
            if (scrollableBottomLayout != null) {
                scrollableBottomLayout.setVisibility(View.GONE);
            }
            
            setupButtonClickListeners();
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting up bottom buttons: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
        }
    }    
    
    private void setupButtonClickListeners() {
        try {
            Button fixedOptionsButton = findViewById(R.id.fixedOptionsButton);
            Button fixedInfoButton = findViewById(R.id.fixedInfoButton);
            Button scrollableOptionsButton = findViewById(R.id.scrollableOptionsButton);
            Button scrollableInfoButton = findViewById(R.id.scrollableInfoButton);
            
            View.OnClickListener optionsClickListener = v -> {
                Intent intent = new Intent(ChoicesActivity.this, WelcomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
            };
            
            View.OnClickListener infoClickListener = v -> {
                Intent intent = new Intent(ChoicesActivity.this, InfoActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            };
            
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
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        } catch (Exception e) {
            Log.e(TAG, "Error launching MainActivity: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
        }
    }
    
    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }
}
