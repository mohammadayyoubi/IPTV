package com.example.iptv.activities.admin;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.example.iptv.R;
import com.example.iptv.ui.BaseActivity;
import com.example.iptv.ui.UiUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

public class activity_forgot_password extends BaseActivity {

    private TextInputEditText emailEditText;
    private TextInputLayout emailField;
    private MaterialButton resetPasswordButton;
    private View resetProgress;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        UiUtils.applyInsets(findViewById(R.id.root), true, true, true);

        emailEditText = findViewById(R.id.emailEditText);
        emailField = findViewById(R.id.emailTextField);
        resetPasswordButton = findViewById(R.id.resetPasswordButton);
        resetProgress = findViewById(R.id.resetProgress);
        firebaseAuth = FirebaseAuth.getInstance();

        findViewById(R.id.backButton).setOnClickListener(v -> finish());

        resetPasswordButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            emailField.setError(null);

            if (TextUtils.isEmpty(email)) {
                emailField.setError(getString(R.string.adm_enter_email));
                emailEditText.requestFocus();
                return;
            }

            setLoading(true);
            firebaseAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        setLoading(false);
                        if (task.isSuccessful()) {
                            Toast.makeText(this, R.string.adm_reset_sent, Toast.LENGTH_LONG).show();
                            finish();
                        } else {
                            emailField.setError(task.getException().getMessage());
                        }
                    });
        });
    }

    private void setLoading(boolean loading) {
        resetPasswordButton.setEnabled(!loading);
        resetPasswordButton.setText(loading ? "" : getString(R.string.send_reset_link));
        resetProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}
