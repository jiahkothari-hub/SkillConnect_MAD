package com.skillconnect.app.activities;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.skillconnect.app.R;
import com.skillconnect.app.adapters.OnboardingAdapter;
import com.skillconnect.app.utils.NavigationUtils;
import com.skillconnect.app.utils.PrefsManager;
import com.skillconnect.app.utils.UiUtils;

/** Three intro pages. Completion is stored in SharedPreferences so it only shows once. */
public class OnboardingActivity extends AppCompatActivity {

    /** When opened from Settings, just close at the end instead of going to login. */
    public static final String EXTRA_REPLAY = "replay";

    private ViewPager2 viewPager;
    private LinearLayout dotsContainer;
    private MaterialButton btnNext;
    private OnboardingAdapter.Page[] pages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        pages = new OnboardingAdapter.Page[]{
                new OnboardingAdapter.Page(R.drawable.ic_search, R.drawable.ic_location, R.string.onb_title_1, R.string.onb_desc_1),
                new OnboardingAdapter.Page(R.drawable.ic_people, R.drawable.ic_star, R.string.onb_title_2, R.string.onb_desc_2),
                new OnboardingAdapter.Page(R.drawable.ic_calendar, R.drawable.ic_check_circle, R.string.onb_title_3, R.string.onb_desc_3),
        };

        viewPager = findViewById(R.id.viewPager);
        dotsContainer = findViewById(R.id.dotsContainer);
        btnNext = findViewById(R.id.btnNext);
        viewPager.setAdapter(new OnboardingAdapter(pages));

        buildDots();
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateDots(position);
                btnNext.setText(position == pages.length - 1 ? R.string.get_started : R.string.next);
            }
        });

        btnNext.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            if (current < pages.length - 1) viewPager.setCurrentItem(current + 1);
            else finishOnboarding();
        });
        findViewById(R.id.btnSkip).setOnClickListener(v -> finishOnboarding());
    }

    private void buildDots() {
        dotsContainer.removeAllViews();
        for (int i = 0; i < pages.length; i++) {
            ImageView dot = new ImageView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            int margin = UiUtils.dp(this, 4);
            lp.setMargins(margin, 0, margin, 0);
            dot.setLayoutParams(lp);
            dotsContainer.addView(dot);
        }
        updateDots(0);
    }

    private void updateDots(int selected) {
        for (int i = 0; i < dotsContainer.getChildCount(); i++) {
            ((ImageView) dotsContainer.getChildAt(i)).setImageResource(
                    i == selected ? R.drawable.bg_indicator_active : R.drawable.bg_indicator_inactive);
        }
    }

    private void finishOnboarding() {
        PrefsManager.get(this).setOnboardingDone(true);
        if (getIntent().getBooleanExtra(EXTRA_REPLAY, false)) {
            finish();
            return;
        }
        NavigationUtils.goToLogin(this);
    }
}
