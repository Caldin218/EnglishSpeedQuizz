package com.example.englishspeedquiz;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton; // Import thêm ImageButton cho nút Back
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // 1. Ánh xạ nút Đăng xuất
        Button btnLogout = findViewById(R.id.btnLogout);

        // 2. Ánh xạ nút Back (Đây là phần bạn đang thiếu)
        ImageButton btnBack = findViewById(R.id.btnBack);

        // --- XỬ LÝ NÚT BACK ---
        btnBack.setOnClickListener(v -> {
            finish(); // Đóng màn hình này -> Quay lại Menu
        });

        // --- XỬ LÝ NÚT ĐĂNG XUẤT ---
        btnLogout.setOnClickListener(v -> {
            // Đăng xuất Firebase
            FirebaseAuth.getInstance().signOut();

            // Quay về màn hình Đăng nhập và xóa hết lịch sử cũ
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }
}