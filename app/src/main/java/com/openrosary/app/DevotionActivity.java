package com.openrosary.app;

import android.content.res.Resources;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.GestureDetectorCompat;

public class DevotionActivity extends BaseActivity implements GestureDetector.OnGestureListener {
    private static final int SWIPE_THRESHOLD = 70;
    private static final int SWIPE_VELOCITY_THRESHOLD = 80;

    private DevotionState state;
    private TextView section, label, text, progress, instructions;
    private ProgressBar bar;
    private ScrollView scroll;
    private GestureDetectorCompat gestures;

    private float touchDownX, touchDownY;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_main);
        String customMarkdown = getIntent().getStringExtra("markdownContent");
        String customFile = getIntent().getStringExtra("customMarkdownFile");
        if (customMarkdown != null && !customMarkdown.isEmpty()) {
            DevotionParser.ParsedDevotion parsed = DevotionParser.parse(this, customMarkdown);
            state = new DevotionState(this, parsed);
        } else if (customFile != null && !customFile.isEmpty()) {
            DevotionParser.ParsedDevotion parsed = DevotionParser.parseFile(this, new java.io.File(customFile));
            state = new DevotionState(this, parsed);
        } else {
            String type = getIntent().getStringExtra("devotionType");
            state = new DevotionState(this, type == null ? "divine-mercy" : type);
        }
        section = findViewById(R.id.mysteryTitleTextView);
        label = findViewById(R.id.prayerLabelTextView);
        text = findViewById(R.id.prayerTextView);
        progress = findViewById(R.id.progressTextView);
        bar = findViewById(R.id.rosaryProgressBar);
        scroll = findViewById(R.id.prayerTextScrollView);
        instructions = findViewById(R.id.instructionsTextView);

        gestures = new GestureDetectorCompat(this, this);

        if (instructions != null) {
            instructions.setOnClickListener(v -> {
                if (state.isComplete()) {
                    finish();
                }
            });
        }
        render();
    }

    private void next() {
        if (state.isComplete()) {
            showCompletionDialog();
            return;
        }
        state.next();
        render();
        HapticHelper.vibrateSymbol(this, state.vibration());
        if (state.isComplete()) {
            showCompletionDialog();
        }
    }

    private void previous() {
        state.previous();
        render();
        HapticHelper.vibrate(this, 2);
    }

    private void showCompletionDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.completion_title)
                .setMessage(R.string.continue_iterate)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    private void render() {
        section.setText(state.section());
        label.setText(state.label());
        text.setText(state.text());
        bar.setMax(state.max());
        bar.setProgress(state.progress());
        progress.setText(state.progress() + "/" + state.max());
        progress.setVisibility(View.GONE);
        if (scroll != null) {
            scroll.post(() -> scroll.scrollTo(0, 0));
        }
        if (instructions != null) {
            if (state.isComplete()) {
                instructions.setText(R.string.prayer_complete);
            } else {
                instructions.setText(R.string.rosary_instructions);
            }
        }
    }

    private boolean gestureHandled = false;

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (gestures != null) {
            gestures.onTouchEvent(ev);
        }

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                touchDownX = ev.getX();
                touchDownY = ev.getY();
                gestureHandled = false;
                break;

            case MotionEvent.ACTION_MOVE:
                float diffX = Math.abs(ev.getX() - touchDownX);
                float diffY = Math.abs(ev.getY() - touchDownY);
                if (diffX > diffY && diffX > dp(8)) {
                    if (scroll != null) {
                        scroll.requestDisallowInterceptTouchEvent(true);
                    }
                } else if (diffY > diffX && diffY > dp(8)) {
                    if (scroll != null) {
                        scroll.requestDisallowInterceptTouchEvent(false);
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
                if (!gestureHandled) {
                    float totalDx = ev.getX() - touchDownX;
                    float totalDy = ev.getY() - touchDownY;
                    float totalAbsDx = Math.abs(totalDx);
                    float totalAbsDy = Math.abs(totalDy);
                    if (totalAbsDx >= dp(40) && totalAbsDx > totalAbsDy * 1.1f) {
                        gestureHandled = true;
                        if (scroll != null) {
                            scroll.requestDisallowInterceptTouchEvent(false);
                        }
                        if (totalDx < 0) {
                            next();
                        } else {
                            previous();
                        }
                        return true;
                    }
                }
                if (scroll != null) {
                    scroll.requestDisallowInterceptTouchEvent(false);
                }
                break;

            case MotionEvent.ACTION_CANCEL:
                if (scroll != null) {
                    scroll.requestDisallowInterceptTouchEvent(false);
                }
                break;
        }

        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (e1 == null || e2 == null || gestureHandled) return false;
        float diffX = e2.getX() - e1.getX();
        float diffY = e2.getY() - e1.getY();
        if (Math.abs(diffX) > Math.abs(diffY) && Math.abs(diffX) > dp(35)) {
            gestureHandled = true;
            if (diffX < 0) {
                next();
            } else {
                previous();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean onKeyDown(int code, KeyEvent event) {
        if (code == KeyEvent.KEYCODE_VOLUME_DOWN) { next(); return true; }
        if (code == KeyEvent.KEYCODE_VOLUME_UP) { previous(); return true; }
        return super.onKeyDown(code, event);
    }

    @Override public boolean onDown(MotionEvent e) { return true; }
    @Override public void onShowPress(MotionEvent e) {}
    @Override public boolean onSingleTapUp(MotionEvent e) { return false; }
    @Override public boolean onScroll(MotionEvent a, MotionEvent b, float x, float y) { return false; }
    @Override public void onLongPress(MotionEvent e) {}

    private int dp(float value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                Resources.getSystem().getDisplayMetrics()));
    }
}
