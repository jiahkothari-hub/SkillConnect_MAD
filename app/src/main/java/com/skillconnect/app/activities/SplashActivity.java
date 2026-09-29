package com.skillconnect.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.appcompat.app.AppCompatActivity;

import com.skillconnect.app.R;
import com.skillconnect.app.data.AppRepository;
import com.skillconnect.app.data.Callback;
import com.skillconnect.app.data.RepositoryProvider;
import com.skillconnect.app.models.User;
import com.skillconnect.app.utils.NavigationUtils;
import com.skillconnect.app.utils.PrefsManager;

/** Brand splash screen. Decides where to go: onboarding, login or the role's home. */
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY_MS = 1400;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        animateIn();
        handler.postDelayed(this::route, SPLASH_DELAY_MS);
    }

    private void animateIn() {
        View logo = findViewById(R.id.ivLogo);
        View name = findViewById(R.id.tvAppName);
        View tagline = findViewById(R.id.tvTagline);
        logo.setAlpha(0f);
        logo.setScaleX(0.7f);
        logo.setScaleY(0.7f);
        logo.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(600)
                .setInterpolator(new DecelerateInterpolator()).start();
        for (View v : new View[]{name, tagline}) {
            v.setAlpha(0f);
            v.setTranslationY(24f);
        }
        name.animate().alpha(1f).translationY(0f).setStartDelay(250).setDuration(500).start();
        tagline.animate().alpha(1f).translationY(0f).setStartDelay(400).setDuration(500).start();
    }

    private void route() {
        if (isFinishing()) return;
        if (!PrefsManager.get(this).isOnboardingDone()) {
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
            return;
        }
        AppRepository repo = RepositoryProvider.get();
        if (!repo.isLoggedIn()) {
            NavigationUtils.goToLogin(this);
            return;
        }
        repo.loadCurrentUser(new Callback<User>() {
            @Override
            public void onSuccess(User user) {
                NavigationUtils.goToHome(SplashActivity.this, user);
            }

            @Override
            public void onError(String message) {
                NavigationUtils.goToLogin(SplashActivity.this);
            }
        });
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
