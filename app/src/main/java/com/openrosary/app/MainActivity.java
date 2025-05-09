package com.openrosary.app;

// import android.app.AlertDialog; // Removing this import as we're using the androidx version
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Vibrator;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.GestureDetectorCompat;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.lang.ref.WeakReference;
import java.util.Locale;

public class MainActivity extends BaseActivity implements GestureDetector.OnGestureListener {

    private static final String TAG = "MainActivity";
    // These are now defined in BaseActivity
    // private static final String PREFS_NAME = "SimpleRosaryPrefs";
    // private static final String THEME_KEY = "theme";
    // private static final String LANGUAGE_KEY = "language";
    
    // Constants for swipe detection
    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;

    // UI Elements
    private TextView prayerLabelTextView;
    private TextView prayerTextView;
    private TextView mysteryTitleTextView;
    private TextView instructionsTextView;
    private ScrollView prayerTextScrollView;
    private ProgressBar rosaryProgressBar;
    private TextView progressTextView;
    
    // System services
    private Vibrator vibrator;
    // Make the gesture detector public so it can be accessed without synthetic accessors
    public GestureDetectorCompat gestureDetector;
    
    // Variables to track volume button states
    private boolean volumeUpPressed = false;
    private boolean volumeDownPressed = false;
    
    // Rosary state variables
    private RosaryState rosaryState;
    private String mysteryType = "joyful"; // Default mystery
    private static final String KEY_MYSTERY_TYPE = "mysteryType";
    private static final String KEY_TOTAL_COUNT = "totalCount";

    // Use static inner classes to avoid retain references to the activity
    private static class CompletionTask implements Runnable {
        private final WeakReference<MainActivity> activityRef;
        
        public CompletionTask(MainActivity activity) {
            this.activityRef = new WeakReference<>(activity);
        }
        
        @Override
        public void run() {
            MainActivity activity = activityRef.get();
            if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
                return;
            }
            
            try {
                Intent intent = new Intent(activity, ChoicesActivity.class);
                activity.startActivity(intent);
                activity.finish();
            } catch (Exception e) {
                Log.e("MainActivity", "Error returning to choices: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
            }
        }
    }
    
    // Custom touch listeners using WeakReferences to avoid memory leaks
    private static class MainLayoutTouchListener implements View.OnTouchListener {
        private final WeakReference<MainActivity> activityRef;
        
        public MainLayoutTouchListener(MainActivity activity) {
            this.activityRef = new WeakReference<>(activity);
        }
        
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            MainActivity activity = activityRef.get();
            if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
                return false;
            }
            
            try {
                if (activity.gestureDetector != null && event != null) {
                    return activity.gestureDetector.onTouchEvent(event);
                }
            } catch (Exception e) {
                Log.e("MainActivity", "Error in mainLayout touch event: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
            }
            return false;
        }
    }
    
    private static class ScrollViewTouchListener implements View.OnTouchListener {
        private final WeakReference<MainActivity> activityRef;

        public ScrollViewTouchListener(MainActivity activity) {
            this.activityRef = new WeakReference<>(activity);
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            MainActivity activity = activityRef.get();
            if (activity == null || activity.isFinishing() || activity.isDestroyed() || activity.gestureDetector == null) {
                return false;
            }

            // Pass the event to the GestureDetector.
            // This will trigger the appropriate callbacks in MainActivity (onDown, onScroll, onFling, etc.).
            boolean gestureConsumedByDetector = activity.gestureDetector.onTouchEvent(event);

            // If the event is ACTION_UP and the GestureDetector consumed it (i.e., onFling returned true),
            // then we should return true to indicate the event was handled as a swipe.
            if (event.getAction() == MotionEvent.ACTION_UP && gestureConsumedByDetector) {
                return true;
            }

            // For all other cases (ACTION_DOWN, ACTION_MOVE, or ACTION_UP not consumed by onFling),
            // return false. This allows the ScrollView to handle its native scrolling behavior.
            // The GestureDetector has already seen the event.
            return false;
        }
    }
    
    /**
     * Runnable for scrolling a ScrollView to the top safely
     */
    private static class ScrollToTopRunnable implements Runnable {
        private final WeakReference<ScrollView> scrollViewReference;

        ScrollToTopRunnable(ScrollView scrollView) {
            this.scrollViewReference = new WeakReference<>(scrollView);
        }

        @Override
        public void run() {
            try {
                ScrollView scrollView = scrollViewReference.get();
                if (scrollView != null) {
                    scrollView.smoothScrollTo(0, 0);
                }
            } catch (Exception e) {
                Log.e("ScrollToTopRunnable", "Error scrolling to top", e);
            }
        }
    }
    
    private static class DialogClickListener implements DialogInterface.OnClickListener {
        private final WeakReference<MainActivity> activityRef;
        
        public DialogClickListener(MainActivity activity) {
            this.activityRef = new WeakReference<>(activity);
        }
        
        @Override
        public void onClick(DialogInterface dialog, int which) {
            try {
                MainActivity activity = activityRef.get();
                if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
                    return;
                }
                
                Intent intent = new Intent(activity, ChoicesActivity.class);
                activity.startActivity(intent);
                activity.finish();
            } catch (Exception e) {
                Log.e("MainActivity", "Error in dialog onClick: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
            }
        }
    }
    
    /**
     * A safe Handler implementation that prevents memory leaks and handles failures gracefully
     */
    private static class SafeHandler {
        private final WeakReference<MainActivity> activityReference;
        private final Handler handler;

        SafeHandler(MainActivity activity) {
            this.activityReference = new WeakReference<>(activity);
            this.handler = new Handler(Looper.getMainLooper());
        }

        void postSafeAction(Runnable action) {
            handler.post(() -> {
                MainActivity activity = activityReference.get();
                if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                    try {
                        action.run();
                    } catch (Exception e) {
                        Log.e("SafeHandler", "Error executing posted action", e);
                    }
                }
            });
        }

        void postSafeActionDelayed(Runnable action, long delayMillis) {
            handler.postDelayed(() -> {
                MainActivity activity = activityReference.get();
                if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                    try {
                        action.run();
                    } catch (Exception e) {
                        Log.e("SafeHandler", "Error executing delayed action", e);
                    }
                }
            }, delayMillis);
        }

        void removeCallbacksAndMessages() {
            handler.removeCallbacksAndMessages(null);
        }
    }
    
    private SafeHandler safeHandler;
    private MainLayoutTouchListener mainLayoutTouchListener;
    private ScrollViewTouchListener scrollViewTouchListener;
    private DialogClickListener dialogClickListener;

    /**
     * A custom wrapper class for prayer information to ensure synchronization
     */
    private static class PrayerInfo {
        final String prayerText;
        final String prayerTitle;

        PrayerInfo(String text, String title) {
            this.prayerText = text != null ? text : "";
            this.prayerTitle = title != null ? title : "";
        }
    }
    
    /**
     * Get both the prayer text and title together to ensure they're synchronized
     * This version includes additional null safety
     */
    private PrayerInfo getPrayerInfo(boolean isAdvancing) {
        if (rosaryState == null) {
            return new PrayerInfo("", "");
        }
        
        String text;
        String title;
        
        try {
            if (isAdvancing) {
                // For advancing forward - get new text and matching title
                text = rosaryState.advance();
                // Additional null check
                if (text == null) {
                    Log.e(TAG, "Received null prayer text from rosaryState.advance()");
                    text = ""; // Use empty string instead of null
                }
                title = rosaryState.getCurrentPrayerLabel();
                if (title == null) {
                    title = "";
                }
            } else {
                // For backward navigation
                // Store the old state to know where we're coming from
                int oldStage = rosaryState.getStage();
                int oldPrayerCount = rosaryState.getPrayerCount();
                
                // Get the new prayer text by going back
                text = rosaryState.goBack();
                // Additional null check
                if (text == null) {
                    Log.e(TAG, "Received null prayer text from rosaryState.goBack()");
                    text = ""; // Use empty string instead of null
                }
                
                // Get the title after the state has changed
                title = rosaryState.getCurrentPrayerLabel();
                if (title == null) {
                    title = "";
                }
                
                // Extra sanity check to ensure UI is fully synced
                if (oldStage != rosaryState.getStage() || oldPrayerCount != rosaryState.getPrayerCount()) {
                    // State changed, make sure the UI refreshes completely
                    title = rosaryState.getCurrentPrayerLabel();
                    if (title == null) {
                        title = "";
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in getPrayerInfo: " + (e.getMessage() != null ? e.getMessage() : "unknown"), e);
            return new PrayerInfo("", "");
        }
        
        return new PrayerInfo(text, title);
    }
    
    /**
     * Update the UI with synchronized prayer information
     */
    private void updatePrayerUI(PrayerInfo prayerInfo) {
        try {
            // Check if activity is still valid
            if (isFinishing() || isDestroyed()) {
                return;
            }
            
            // Additional null check for prayerInfo
            if (prayerInfo == null) {
                Log.e(TAG, "Attempted to update UI with null prayerInfo");
                prayerInfo = new PrayerInfo("", "");
            }
            
            // Update prayer text
            if (prayerTextView != null) {
                String text = prayerInfo.prayerText != null ? prayerInfo.prayerText : "";
                prayerTextView.setText(text);
                
                // Scroll to the top
                if (prayerTextScrollView != null && safeHandler != null) {
                    safeHandler.postSafeAction(new ScrollToTopRunnable(prayerTextScrollView));
                }
            }
            
            // Update prayer title/label
            if (prayerLabelTextView != null) {
                String title = prayerInfo.prayerTitle != null ? prayerInfo.prayerTitle : "";
                prayerLabelTextView.setText(title);
            }
            
            // Update mystery title
            updateMysteryTitle();
            
            // Update the progress bar
            updateProgressBar();
            
            // DO NOT call checkCompletion() here. It will be called in advanceRosary() after advancing.
            
        } catch (Exception e) {
            Log.e(TAG, "Error updating prayer UI: " + (e.getMessage() != null ? e.getMessage() : "unknown"), e);
        }
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            // Load preferences and apply theme and language
            // loadPreferences(); // REMOVED - BaseActivity handles this
            
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);
            
            // Initialize RosaryPrayers with application context to load prayers from resources
            RosaryPrayers.initialize(getApplicationContext());
            
            // Set context for RosaryState to access string resources
            RosaryState.setContext(getApplicationContext());
            
            // Initialize handler
            safeHandler = new SafeHandler(this);
            
            // Initialize views
            initializeViews();

            // Initialize DialogClickListener here as well to be safe
            dialogClickListener = new DialogClickListener(this);
            
            // Initialize vibrator for haptic feedback
            vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            
            // Initialize gesture detector for swipe support
            gestureDetector = new GestureDetectorCompat(this, this);
            
            // Initialize touch listeners
            mainLayoutTouchListener = new MainLayoutTouchListener(this);
            scrollViewTouchListener = new ScrollViewTouchListener(this); // Ensure scrollViewTouchListener is instantiated
            
            // Get mystery type from intent or saved instance state
            if (savedInstanceState != null) {
                mysteryType = savedInstanceState.getString(KEY_MYSTERY_TYPE, "joyful");
            } else if (getIntent() != null && getIntent().getExtras() != null) {
                String intentMysteryType = getIntent().getStringExtra("mysteryType");
                if (intentMysteryType != null && !intentMysteryType.isEmpty()) {
                    mysteryType = intentMysteryType;
                }
            }
            
            // Initialize Rosary state
            rosaryState = new RosaryState(mysteryType);

            if (savedInstanceState != null) {
                int savedTotalCount = savedInstanceState.getInt(KEY_TOTAL_COUNT, 0);
                int targetCountForDisplay = savedTotalCount;

                if (savedTotalCount == 0) {
                    // If savedTotalCount is 0, it means state was before the first prayer.
                    // Advance once to get to the first prayer (totalCount = 1).
                    rosaryState.advance(); 
                    targetCountForDisplay = 1; // We will display the prayer for totalCount = 1
                } else { // savedTotalCount > 0
                    // Advance rosaryState's internal counters to match savedTotalCount.
                    for (int i = 0; i < savedTotalCount; i++) {
                        rosaryState.advance(); 
                    }
                    // After this loop, rosaryState.getTotalCount() should be equal to savedTotalCount.
                    // targetCountForDisplay is already savedTotalCount.
                }

                // Fetch the prayer text and label corresponding to targetCountForDisplay.
                String textForUI = rosaryState.getPrayerTextByTotalCount(targetCountForDisplay);
                String labelForUI = rosaryState.getPrayerLabelByTotalCount(targetCountForDisplay);

                if (prayerTextView != null) {
                    prayerTextView.setText(textForUI != null ? textForUI : "");
                }
                if (prayerLabelTextView != null) {
                    prayerLabelTextView.setText(labelForUI != null ? labelForUI : "");
                }
            } else {
                // No savedInstanceState bundle at all - completely fresh start
                String initialPrayerText = rosaryState.advance(); // Advances to totalCount = 1
                String initialPrayerLabel = rosaryState.getPrayerLabelByTotalCount(1); // Get label for totalCount = 1

                if (prayerTextView != null) {
                    prayerTextView.setText(initialPrayerText != null ? initialPrayerText : "");
                }
                if (prayerLabelTextView != null) {
                    prayerLabelTextView.setText(initialPrayerLabel != null ? initialPrayerLabel : "");
                }
            }
            
            // Update other UI elements
            updateMysteryTitle();
            updateProgressBar();
            
            // Set instructions text
            updateInstructions();
            
            // Set up touch listener for the main layout to enable swipe gestures
            final ConstraintLayout mainLayout = findViewById(R.id.mainLayout);
            if (mainLayout != null) {
                mainLayout.setOnTouchListener(mainLayoutTouchListener);
            }
            
            // Set up touch listener for the ScrollView to handle scrolling and swipe arbitration
            if (prayerTextScrollView != null) {
                prayerTextScrollView.setOnTouchListener(scrollViewTouchListener);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (rosaryState != null) {
            outState.putString(KEY_MYSTERY_TYPE, mysteryType);
            outState.putInt(KEY_TOTAL_COUNT, rosaryState.getTotalCount());
        }
    }
    
    @Override
    protected void onDestroy() {
        // Remove any pending callbacks to prevent memory leaks and crashes
        if (safeHandler != null) {
            safeHandler.removeCallbacksAndMessages();
        }
        super.onDestroy();
    }
    
    private void initializeViews() {
        prayerLabelTextView = findViewById(R.id.prayerLabelTextView);
        prayerTextView = findViewById(R.id.prayerTextView);
        mysteryTitleTextView = findViewById(R.id.mysteryTitleTextView);
        instructionsTextView = findViewById(R.id.instructionsTextView);
        prayerTextScrollView = findViewById(R.id.prayerTextScrollView);
        rosaryProgressBar = findViewById(R.id.rosaryProgressBar);
        progressTextView = findViewById(R.id.progressTextView);
        
        // Configure ScrollView to only show scrollbar when scrolling
        if (prayerTextScrollView != null) {
            prayerTextScrollView.setScrollbarFadingEnabled(true);
            prayerTextScrollView.setVerticalScrollBarEnabled(true);
            prayerTextScrollView.setScrollBarFadeDuration(1500); // 1.5 seconds fade delay
        }
        
        // Set up progress bar max value
        if (rosaryProgressBar != null) {
            rosaryProgressBar.setMax(80); // Total count for a complete rosary
        }
    }
    
    private void updateMysteryTitle() {
        try {
            // Check if activity is still valid
            if (isFinishing() || isDestroyed()) {
                return;
            }
            
            if (mysteryTitleTextView != null && rosaryState != null) {
                // Mystery title should always show the current mystery type
                // (never show prayer names like "Fatima Prayer" here)
                switch (mysteryType) {
                    case "joyful":
                        mysteryTitleTextView.setText(getString(R.string.joyful_mysteries));
                        break;
                    case "sorrowful":
                        mysteryTitleTextView.setText(getString(R.string.sorrowful_mysteries));
                        break;
                    case "glorious":
                        mysteryTitleTextView.setText(getString(R.string.glorious_mysteries));
                        break;
                    case "luminous":
                        mysteryTitleTextView.setText(getString(R.string.luminous_mysteries));
                        break;
                }
                
                // If we're in a decade, add the specific mystery title
                // IMPORTANT: Don't change mystery title during Fatima Prayer (count 13)
                if (rosaryState.getStage() == RosaryState.STAGE_DECADE && 
                    rosaryState.getPrayerCount() > 0 && rosaryState.getPrayerCount() != 13) {
                    String currentMysteryTitle = rosaryState.getCurrentMysteryTitle();
                    if (currentMysteryTitle != null && !currentMysteryTitle.isEmpty()) {
                        mysteryTitleTextView.setText(currentMysteryTitle);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating mystery title: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
        }
    }
    
    private void updateInstructions() {
        try {
            // Check if activity is still valid
            if (isFinishing() || isDestroyed()) {
                return;
            }
            
            if (instructionsTextView != null) {
                instructionsTextView.setText(R.string.rosary_instructions);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating instructions: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
        }
    }
    
    /**
     * Check if the rosary is complete and show a confirmation dialog
     */
    private void checkCompletion() {
        try {
            // Check if activity is still valid
            if (isFinishing() || isDestroyed() || rosaryState == null) {
                return;
            }

            // Show completion dialog if the rosary is complete
            if (rosaryState.isComplete()) {
                Log.i(TAG, "Rosary complete! Showing completion dialog: " + getString(R.string.continue_iterate));

                // Ensure dialogClickListener is initialized
                if (dialogClickListener == null) {
                    dialogClickListener = new DialogClickListener(this);
                }
                
                // Show dialog immediately (no delay)
                try {
                    // Check again if activity is still valid before showing dialog
                    if (!isFinishing() && !isDestroyed()) {
                        // Show completion dialog with a single "OK" button
                        new AlertDialog.Builder(this)
                            .setTitle(R.string.completion_title)
                            .setMessage(R.string.continue_iterate)
                            .setPositiveButton(android.R.string.ok, dialogClickListener)
                            .setCancelable(false) // Prevent dismissing by tapping outside or back button
                            .show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error showing completion dialog: " + (e.getMessage() != null ? e.getMessage() : "unknown"), e);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking completion: " + (e.getMessage() != null ? e.getMessage() : "unknown"), e);
        }
    }

    /**
     * Provide haptic feedback based on the type of transition
     * @param isBeadTransition Whether this is a transition between bead types
     */
    private void provideHapticFeedback(boolean isBeadTransition) {
        try {
            if (vibrator != null && vibrator.hasVibrator()) {
                if (isBeadTransition) {
                    // Triple vibration for transitions between small/large beads
                    long[] pattern = {0, 50, 100, 50, 100, 50};  // Wait 0ms, vibrate 50ms, wait 100ms, vibrate 50ms, wait 100ms, vibrate 50ms
                    vibrator.vibrate(pattern, -1);      // -1 means don't repeat the pattern
                } else {
                    // Single vibration for regular transitions
                    vibrator.vibrate(50);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error providing haptic feedback: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
        }
    }
    
    /**
     * Navigate backward in the rosary using the totalCount approach
     * This completely bypasses state tracking issues by using direct mapping
     */
    private void previousRosary() {
        try {
            // Basic validity checks
            if (isFinishing() || isDestroyed() || rosaryState == null) {
                return;
            }
            
            // Get the current total count
            int currentCount = rosaryState.getTotalCount();
            
            // Log the current state before going back
            Log.d(TAG, "BEFORE going back - total count: " + currentCount);
            
            // Don't go back from the beginning
            if (currentCount <= 1) {
                Log.d(TAG, "Already at the beginning, not going back.");
                return;
            }
            
            // Calculate the previous count
            int previousCount = currentCount - 1;
            Log.d(TAG, "Going back to total count: " + previousCount);
            
            // Get the prayer text directly from the total count
            String previousPrayerText = rosaryState.getPrayerTextByTotalCount(previousCount);
            String previousPrayerLabel = rosaryState.getPrayerLabelByTotalCount(previousCount);
            String mysteryTitle = rosaryState.getMysteryTitleByTotalCount(previousCount);
            
            Log.d(TAG, "Previous prayer text (first 20 chars): " + 
                  (previousPrayerText.length() > 20 ? previousPrayerText.substring(0, 20) + "..." : previousPrayerText));
            Log.d(TAG, "Previous prayer label: " + previousPrayerLabel);
            
            // Update the state tracking in rosaryState to match our new position
            // This call updates internal state counters
            rosaryState.goBack();
            
            // Update UI with our carefully retrieved values
            if (prayerTextView != null) {
                prayerTextView.setText(previousPrayerText);
            }
            
            if (prayerLabelTextView != null) {
                prayerLabelTextView.setText(previousPrayerLabel);
            }
            
            // Update mystery title if we have one
            if (mysteryTitleTextView != null && !mysteryTitle.isEmpty()) {
                mysteryTitleTextView.setText(mysteryTitle);
            } else {
                // Use default mystery type title when not in a specific mystery
                updateMysteryTitle();
            }
            
            // Update progress bar (this will reflect the new count after goBack())
            updateProgressBar();
            
            // Scroll to top
            if (prayerTextScrollView != null && safeHandler != null) {
                safeHandler.postSafeAction(new ScrollToTopRunnable(prayerTextScrollView));
            }
            
            // Two vibrations for backward navigation instead of one
            if (vibrator != null && vibrator.hasVibrator()) {
                long[] pattern = {0, 50, 100, 50};  // Wait 0ms, vibrate 50ms, wait 100ms, vibrate 50ms
                vibrator.vibrate(pattern, -1);      // -1 means don't repeat the pattern
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error navigating backward: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
        }
    }
    
    /**
     * Helper method to get the general mystery title based on mystery type
     */
    private String getGeneralMysteryTitle() {
        switch (mysteryType) {
            case "joyful":
                return getString(R.string.joyful_mysteries);
            case "sorrowful":
                return getString(R.string.sorrowful_mysteries);
            case "glorious":
                return getString(R.string.glorious_mysteries);
            case "luminous":
                return getString(R.string.luminous_mysteries);
            default:
                return "";
        }
    }
    
    private void advanceRosary() {
        try {
            // Check if activity is still valid
            if (isFinishing() || isDestroyed()) {
                return;
            }
            
            if (rosaryState != null) {
                // Get the current total count before advancing
                int currentCount = rosaryState.getTotalCount();
                
                // Check if we are currently at the last prayer of the Rosary sequence
                boolean wasAtMaxCount = (currentCount == rosaryState.getMaxCount());

                // If we are already past the max count (e.g., completion dialog shown, user tries to advance again)
                // OR if the rosary is already complete (but dialog hasn't been shown yet)
                if (currentCount > rosaryState.getMaxCount() || rosaryState.isComplete()) {
                    // Show completion dialog if not already shown and trying to navigate forward
                    checkCompletion();
                    Log.d(TAG, "Already past max count or rosary complete, showing completion dialog.");
                    return;
                }
                
                // Check if this is a transition between bead types before advancing
                boolean isBeadTransition = rosaryState.isBeadTypeTransition(true);
                
                // Advance the internal state first. This will update totalCount and isComplete.
                rosaryState.advance();

                // Now, check if we *just* completed the Rosary by advancing from the last prayer
                if (wasAtMaxCount && rosaryState.isComplete() && rosaryState.getTotalCount() > rosaryState.getMaxCount()) {
                    checkCompletion(); // Call checkCompletion to show the dialog
                    // Clear the prayer text and label as there's no prayer after completion dialog
                    if (prayerTextView != null) prayerTextView.setText("");
                    if (prayerLabelTextView != null) prayerLabelTextView.setText(getString(R.string.complete)); // Or a specific "Completed" label
                    updateProgressBar(); // Update progress to show it's past the max
                    return; // Stop further UI updates for prayer text for this advance
                }
                
                // If not completed, or completed but not yet past max (i.e., displaying the last prayer), get the prayer content
                String nextPrayerText = rosaryState.getPrayerTextByTotalCount(rosaryState.getTotalCount()); 
                String nextPrayerLabel = rosaryState.getPrayerLabelByTotalCount(rosaryState.getTotalCount());
                String mysteryTitle = rosaryState.getMysteryTitleByTotalCount(rosaryState.getTotalCount());
                
                // Update prayer text
                if (prayerTextView != null) {
                    prayerTextView.setText(nextPrayerText);
                }
                
                // Update prayer label
                if (prayerLabelTextView != null) {
                    prayerLabelTextView.setText(nextPrayerLabel);
                }
                
                // Update mystery title if we have one
                if (mysteryTitleTextView != null && !mysteryTitle.isEmpty()) {
                    mysteryTitleTextView.setText(mysteryTitle);
                } else {
                    // Use default mystery type title when not in a specific mystery
                    updateMysteryTitle();
                }
                
                // Update progress bar
                updateProgressBar();
                
                // Scroll to the top
                if (prayerTextScrollView != null && safeHandler != null) {
                    safeHandler.postSafeAction(new ScrollToTopRunnable(prayerTextScrollView));
                }
                
                // DO NOT call checkCompletion() here anymore, it's called earlier if needed.
                
                // Provide appropriate haptic feedback
                provideHapticFeedback(isBeadTransition);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error advancing rosary: " + (e.getMessage() != null ? e.getMessage() : "unknown"), e);
        }
    }
    
    // Handle volume buttons to advance/go back in the rosary
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        try {
            // Check if activity is still valid
            if (isFinishing() || isDestroyed()) {
                return super.onKeyDown(keyCode, event);
            }
            
            // Check if event is valid
            if (event == null) {
                return super.onKeyDown(keyCode, event);
            }
            
            // Check if this is a new press (not a repeated press from holding)
            if (event.getRepeatCount() == 0) {
                if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && !volumeDownPressed) {
                    volumeDownPressed = true;
                    advanceRosary();
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP && !volumeUpPressed) {
                    volumeUpPressed = true;
                    previousRosary();
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_BACK) {
                    // Handle back button press
                    onBackPressed();
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling key press: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
        }
        return super.onKeyDown(keyCode, event);
    }
    
    // Reset button state flags when buttons are released
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        try {
            // Check if activity is still valid
            if (isFinishing() || isDestroyed()) {
                return super.onKeyUp(keyCode, event);
            }
            
            // Check if event is valid
            if (event == null) {
                return super.onKeyUp(keyCode, event);
            }
            
            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                volumeDownPressed = false;
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                volumeUpPressed = false;
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling key release: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
        }
        return super.onKeyUp(keyCode, event);
    }
    
    @Override
    public void onBackPressed() {
        try {
            // Check if activity is still valid
            if (isFinishing() || isDestroyed()) {
                super.onBackPressed();
                return;
            }
            
            // Create the dialog click listener
            if (dialogClickListener == null) {
                dialogClickListener = new DialogClickListener(this);
            }
            
            // Exit confirmation dialog
            new AlertDialog.Builder(this)
                    .setTitle(R.string.exit_confirmation_title)
                    .setMessage(R.string.exit_confirmation_message)
                    .setPositiveButton(R.string.yes, dialogClickListener)
                    .setNegativeButton(R.string.no, null)
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Error in onBackPressed: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
            super.onBackPressed(); // Fall back to default behavior if our custom handling fails
        }
    }
    
    // Implementation of GestureDetector.OnGestureListener for swipe support
    
    @Override
    public boolean onDown(MotionEvent e) {
        return true; // Required for gestures to be detected
    }

    @Override
    public void onShowPress(MotionEvent e) {
        // Not needed
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        // Disable advancing the rosary on tap - only allow swipes and volume buttons
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        // Allow normal scrolling in ScrollView
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        // Not needed
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        try {
            // Check if activity is finishing or destroyed
            if (isFinishing() || isDestroyed()) {
                return false;
            }
            
            // Check if motion events are null
            if (e1 == null || e2 == null) {
                Log.e(TAG, "onFling received null MotionEvent");
                return false;
            }
            
            float diffX = e2.getX() - e1.getX();
            float diffY = e2.getY() - e1.getY();
            
            // Only respond to horizontal swipes for navigation
            if (Math.abs(diffX) > Math.abs(diffY)) {
                if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        // Right swipe - go back to previous prayer
                        previousRosary();
                        return true;
                    } else {
                        // Left swipe - advance to next prayer
                        advanceRosary();
                        return true;
                    }
                }
            }
            // Ignore vertical swipes for navigation - they're just for scrolling
        } catch (Exception e) {
            Log.e(TAG, "Error in onFling: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
        }
        return false;
    }

    /**
     * Update the progress bar to show current position in the rosary
     */
    private void updateProgressBar() {
        try {
            // Check if activity is still valid
            if (isFinishing() || isDestroyed()) {
                return;
            }
            
            if (rosaryState != null && rosaryProgressBar != null && progressTextView != null) {
                int currentProgress = rosaryState.getTotalCount();
                int maxProgress = rosaryState.getMaxCount();
                
                // Update progress bar
                rosaryProgressBar.setProgress(currentProgress);
                
                // Hide the text display as requested
                progressTextView.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating progress bar: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
        }
    }

    /**
     * Initialize or re-initialize the rosary state with the given mystery type
     * @param mysteryType The type of mystery to initialize (joyful, sorrowful, glorious, luminous)
     */
    private void initializeRosaryState(String mysteryType) {
        try {
            this.mysteryType = mysteryType;
            rosaryState = new RosaryState(mysteryType);
            
            // Update the UI with the first prayer
            PrayerInfo initialPrayerInfo = getPrayerInfo(true);
            updatePrayerUI(initialPrayerInfo);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing rosary state: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
        }
    }

    /**
     * Update all UI elements with the current rosary state
     */
    private void updateAllUI() {
        try {
            // Check if activity is still valid
            if (isFinishing() || isDestroyed()) {
                return;
            }
            
            if (prayerTextView != null && prayerLabelTextView != null && rosaryState != null) {
                // Update prayer title/label
                prayerLabelTextView.setText(rosaryState.getCurrentPrayerLabel());

                // Update mystery title  
                updateMysteryTitle();
                
                // Update the progress bar
                updateProgressBar();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating UI: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
        }
    }
    
    /**
     * Update the prayer text display with additional null safety
     */
    private void updatePrayerText(String prayerText) {
        try {
            // Check if activity is still valid
            if (isFinishing() || isDestroyed()) {
                return;
            }

            // Add null check for prayerText
            if (prayerText == null) {
                Log.e(TAG, "Attempted to update prayer text with null value");
                prayerText = ""; // Use empty string instead of null
            }

            if (prayerTextView != null) {
                prayerTextView.setText(prayerText);

                // Scroll to the top
                if (prayerTextScrollView != null && safeHandler != null) {
                    safeHandler.postSafeAction(new ScrollToTopRunnable(prayerTextScrollView));
                }
                
                // Update all UI elements to match the new state
                updateAllUI();

                // DO NOT call checkCompletion() here. It is handled by advanceRosary().
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating prayer text: " + (e.getMessage() != null ? e.getMessage() : "unknown"), e);
        }
    }
}