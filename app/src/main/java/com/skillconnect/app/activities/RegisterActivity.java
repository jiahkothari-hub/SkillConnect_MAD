package com.skillconnect.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.skillconnect.app.R;
import com.skillconnect.app.data.Callback;
import com.skillconnect.app.data.RepositoryProvider;
import com.skillconnect.app.models.User;
import com.skillconnect.app.utils.UiUtils;
import com.skillconnect.app.utils.ValidationUtils;

/**
 * Registration with role selection (CUSTOMER / PROVIDER).
 * Creates the Firebase Authentication account and the users/{uid} Firestore document.
 */
public class RegisterActivity extends AppCompatActivity {

    private MaterialCardView cardCustomer;
    private MaterialCardView cardProvider;
    private RadioGroup rgRole;
    private TextInputLayout tilName, tilEmail, tilPhone, tilPassword, tilConfirm;
    private TextInputEditText etName, etEmail, etPhone, etPassword, etConfirm;
    private MaterialButton btnRegister;
    private View progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        cardCustomer = findViewById(R.id.cardCustomer);
        cardProvider = findViewById(R.id.cardProvider);
        rgRole = findViewById(R.id.rgRole);
        tilName = findViewById(R.id.tilName);
        tilEmail = findViewById(R.id.tilEmail);
        tilPhone = findViewById(R.id.tilPhone);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirm = findViewById(R.id.tilConfirmPassword);
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        etConfirm = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        progress = findViewById(R.id.progress);

        // Styled cards and RadioButtons stay in sync
        cardCustomer.setOnClickListener(v -> rgRole.check(R.id.rbCustomer));
        cardProvider.setOnClickListener(v -> rgRole.check(R.id.rbProvider));
        rgRole.setOnCheckedChangeListener((group, checkedId) -> updateRoleCards());
        updateRoleCards();

        btnRegister.setOnClickListener(v -> attemptRegister());
        findViewById(R.id.btnGoLogin).setOnClickListener(v -> finish());
    }

    private void updateRoleCards() {
        boolean provider = rgRole.getCheckedRadioButtonId() == R.id.rbProvider;
        cardCustomer.setChecked(!provider);
        cardProvider.setChecked(provider);
        cardCustomer.setStrokeColor(getColor(!provider ? R.color.primary : R.color.divider));
        cardProvider.setStrokeColor(getColor(provider ? R.color.primary : R.color.divider));
        cardCustomer.setStrokeWidth(UiUtils.dp(this, !provider ? 2 : 1));
        cardProvider.setStrokeWidth(UiUtils.dp(this, provider ? 2 : 1));
    }

    private void attemptRegister() {
        for (TextInputLayout t : new TextInputLayout[]{tilName, tilEmail, tilPhone, tilPassword, tilConfirm}) {
            t.setError(null);
        }
        String name = text(etName);
        String email = text(etEmail);
        String phone = text(etPhone);
        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";
        String confirm = etConfirm.getText() != null ? etConfirm.getText().toString() : "";

        boolean valid = true;
        if (!ValidationUtils.isValidName(name)) {
            tilName.setError("Enter your full name (letters only)");
            valid = false;
        }
        if (!ValidationUtils.isValidEmail(email)) {
            tilEmail.setError("Enter a valid email address");
            valid = false;
        }
        if (!ValidationUtils.isValidPhone(phone)) {
            tilPhone.setError("Enter a valid 10-digit mobile number");
            valid = false;
        }
        if (!ValidationUtils.isValidPassword(password)) {
            tilPassword.setError("Password must be at least " + ValidationUtils.MIN_PASSWORD_LENGTH + " characters");
            valid = false;
        }
        if (!password.equals(confirm)) {
            tilConfirm.setError("Passwords don't match");
            valid = false;
        }
        if (!valid) return;
        if (!RepositoryProvider.isDemoMode() && !UiUtils.isOnline(this)) {
            UiUtils.toast(this, getString(R.string.error_offline));
            return;
        }

        String role = rgRole.getCheckedRadioButtonId() == R.id.rbProvider ? User.ROLE_PROVIDER : User.ROLE_CUSTOMER;
        User user = new User(null, name, email, ValidationUtils.normalizePhone(phone), role);

        UiUtils.hideKeyboard(this);
        setLoading(true);
        RepositoryProvider.get().register(user, password, new Callback<User>() {
            @Override
            public void onSuccess(User created) {
                setLoading(false);
                UiUtils.toast(RegisterActivity.this, "Account created. Welcome to SkillConnect!");
                Intent intent;
                if (created.isProvider()) {
                    // Providers set up their professional profile first
                    intent = new Intent(RegisterActivity.this, EditProviderProfileActivity.class);
                    intent.putExtra(EditProviderProfileActivity.EXTRA_FIRST_SETUP, true);
                } else {
                    intent = new Intent(RegisterActivity.this, CustomerHomeActivity.class);
                }
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                if (message.toLowerCase().contains("email")) tilEmail.setError(message);
                else UiUtils.toast(RegisterActivity.this, message);
            }
        });
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!loading);
    }

    private static String text(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }
}
