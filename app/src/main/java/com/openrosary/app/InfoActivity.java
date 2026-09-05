package com.openrosary.app;

import android.content.Intent;
import android.os.Bundle;

/**
 * Redirects to WelcomeActivity (3-page swipeable hub) at Page 2 (About).
 */
public class InfoActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent(this, WelcomeActivity.class);
        intent.putExtra(WelcomeActivity.EXTRA_INITIAL_PAGE, 2);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
