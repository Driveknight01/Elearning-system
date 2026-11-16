package com.elearning.smartflash.ui.main;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.elearning.smartflash.databinding.ActivityMainBinding;

/**
 * Main entry activity (Empty Views Activity with ViewBinding).
 * Will host BottomNavigationView in Phase 2 (T2.9).
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnGetStarted.setOnClickListener(v ->
                Toast.makeText(this, "SmartFlash Android sẵn sàng!", Toast.LENGTH_SHORT).show()
        );
    }
}
