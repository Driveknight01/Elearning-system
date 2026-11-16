package com.elearning.smartflash.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.elearning.smartflash.R;
import com.elearning.smartflash.data.local.prefs.TokenManager;
import com.elearning.smartflash.data.remote.dto.UserSummaryDto;
import com.elearning.smartflash.data.repository.AuthRepository;
import com.elearning.smartflash.databinding.ActivityMainBinding;
import com.elearning.smartflash.ui.auth.GoogleSignInHelper;
import com.elearning.smartflash.ui.auth.LoginActivity;

/**
 * Main entry activity (Empty Views Activity with ViewBinding).
 * Will host BottomNavigationView and ProfileFragment in T2.9.
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private TokenManager tokenManager;
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        tokenManager = TokenManager.getInstance(this);
        authRepository = new AuthRepository(this);

        renderSessionSummary();

        binding.btnGetStarted.setOnClickListener(v ->
                Toast.makeText(this, "SmartFlash Android sẵn sàng (BottomNavigationView sẽ mở ở T2.9)!", Toast.LENGTH_SHORT).show()
        );
    }

    private void renderSessionSummary() {
        UserSummaryDto user = tokenManager.getUser();
        if (tokenManager.isLoggedIn() && user != null) {
            String displayName = user.getFullName() != null ? user.getFullName() : user.getUsername();
            binding.tvSubtitle.setText(
                    "Xin chào " + displayName + " (@" + user.getUsername() + " • " + user.getRole() + ")"
            );
            binding.btnAuthAction.setText(R.string.action_logout);
            binding.btnAuthAction.setOnClickListener(v -> performLogout());
        } else {
            binding.tvSubtitle.setText("Đang trải nghiệm với tư cách Khách (Guest)");
            binding.btnAuthAction.setText(R.string.action_login);
            binding.btnAuthAction.setOnClickListener(v -> {
                tokenManager.setGuestMode(false);
                navigateToLogin();
            });
        }
    }

    private void performLogout() {
        binding.btnAuthAction.setEnabled(false);
        GoogleSignInHelper.signOutSilent(this);
        authRepository.logout(() -> runOnUiThread(() -> {
            Toast.makeText(this, "Đã đăng xuất.", Toast.LENGTH_SHORT).show();
            navigateToLogin();
        }));
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
