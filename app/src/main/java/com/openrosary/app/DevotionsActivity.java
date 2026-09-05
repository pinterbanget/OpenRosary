package com.openrosary.app;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

public class DevotionsActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_devotions);
        findViewById(R.id.devotionsBack).setOnClickListener(v -> finish());
        bind(R.id.devotion77, "our-father-77");
        bind(R.id.devotionMercy, "divine-mercy");
        bind(R.id.devotionSorrows, "seven-sorrows");
        bind(R.id.devotionFranciscan, "franciscan-crown");

        equalizeCardHeights();
        AnimationHelper.attachPressScale(findViewById(R.id.devotion77));
        AnimationHelper.attachPressScale(findViewById(R.id.devotionMercy));
        AnimationHelper.attachPressScale(findViewById(R.id.devotionSorrows));
        AnimationHelper.attachPressScale(findViewById(R.id.devotionFranciscan));
    }

    private void bind(int id, String devotion) {
        View view = findViewById(id);
        if (view != null) {
            view.setOnClickListener(v -> {
                Intent intent = new Intent(this, DevotionActivity.class);
                intent.putExtra("devotionType", devotion);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }
    }

    private void equalizeCardHeights() {
        View[] cards = new View[] {
                findViewById(R.id.devotion77),
                findViewById(R.id.devotionMercy),
                findViewById(R.id.devotionSorrows),
                findViewById(R.id.devotionFranciscan)
        };

        Configuration configuration = getResources().getConfiguration();
        int screenHeightDp = configuration.screenHeightDp;
        int screenWidthDp = configuration.screenWidthDp;
        float scale = 1.0f;
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
        }

        final int baseHeight = dp(90 * scale);
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

    private int dp(float value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                Resources.getSystem().getDisplayMetrics()));
    }
}
