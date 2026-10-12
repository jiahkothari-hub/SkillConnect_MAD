package com.skillconnect.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.skillconnect.app.R;
import com.skillconnect.app.data.Callback;
import com.skillconnect.app.data.DemoData;
import com.skillconnect.app.data.RepositoryProvider;
import com.skillconnect.app.models.User;
import com.skillconnect.app.utils.NavigationUtils;
import com.skillconnect.app.utils.UiUtils;
import com.skillconnect.app.utils.ValidationUtils;

/** Email/password login, forgot password and quick demo accounts. */
public class LoginActivity extends AppCompatActivity {

    private TextInputLayout tilEmail;
    private TextInputLayout tilPassword;
    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private MaterialButton btnLogin;
    private View progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        progress = findViewById(R.id.progress);

        // Demo banner is shown only when Firebase is not configured.
        findViewById(R.id.cardDemoBanner).setVisibility(
                RepositoryProvider.isFirebaseAvailable() ? View.GONE : View.VISIBLE);

        btnLogin.setOnClickListener(v -> attemptLogin());
        etPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptLogin();
                return true;
            }
            return false;
        });
        findViewById(R.id.btnForgot).setOnClickListener(v -> showForgotPasswordDialog());
        findViewById(R.id.btnRegister).setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
        findViewById(R.id.btnDemo).setOnClickListener(v -> showDemoAccounts());
    }

    private void attemptLogin() {
        tilEmail.setError(null);
        tilPassword.setError(null);
        String email = text(etEmail);
        String password = text(etPassword);

        boolean valid = true;
        if (!ValidationUtils.isValidEmail(email)) {
            tilEmail.setError("Enter a valid email address");
            valid = false;
        }
        if (ValidationUtils.isEmpty(password)) {
            tilPassword.setError("Enter your password");
            valid = false;
        }
        if (!valid) return;
        if (!RepositoryProvider.isDemoMode() && !UiUtils.isOnline(this)) {
            UiUtils.toast(this, getString(R.string.error_offline));
            return;
        }
        UiUtils.hideKeyboard(this);
        login(email, password);
    }

    private void login(String email, String password) {
        setLoading(true);
        RepositoryProvider.get().login(email, password, new Callback<User>() {
            @Override
            public void onSuccess(User user) {
                setLoading(false);
                UiUtils.toast(LoginActivity.this, "Welcome back, " + user.getFirstName() + "!");
                NavigationUtils.goToHome(LoginActivity.this, user);
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                tilPassword.setError(message);
            }
        });
    }

    /** Lets anyone try the full app instantly with fictional sample accounts. */
    private void showDemoAccounts() {
        String[] labels = {
                "Customer — Priya Singh",
                "Provider — Rahul Sharma (Electrician)",
                "Admin — SkillConnect Admin"
        };
        String[] emails = {DemoData.CUSTOMER_EMAIL, DemoData.PROVIDER_EMAIL, DemoData.ADMIN_EMAIL};
        new MaterialAlertDialogBuilder(this)
                .setTitle("Choose a demo account")
                .setItems(labels, (d, which) -> {
                    // Demo accounts always use local sample data, even when Firebase is configured.
                    RepositoryProvider.setDemoSession(true);
                    login(emails[which], DemoData.DEMO_PASSWORD);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showForgotPasswordDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_forgot_password, null);
        TextInputLayout til = view.findViewById(R.id.tilResetEmail);
        TextInputEditText et = view.findViewById(R.id.etResetEmail);
        et.setText(text(etEmail));

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.reset_password)
                .setMessage(R.string.reset_password_msg)
                .setView(view)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.send_link, null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String email = text(et);
                    if (!ValidationUtils.isValidEmail(email)) {
                        til.setError("Enter a valid email address");
                        return;
                    }
                    RepositoryProvider.get().sendPasswordReset(email, new Callback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            UiUtils.toast(LoginActivity.this, "Password reset link sent to " + email);
                            dialog.dismiss();
                        }

                        @Override
                        public void onError(String message) {
                            til.setError(message);
                        }
                    });
                }));
        dialog.show();
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
    }

    private static String text(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }
}
