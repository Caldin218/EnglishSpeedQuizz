package com.example.englishspeedquiz;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView; // [QUAN TRỌNG] Thêm thư viện này

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

// Import thư viện cho Báo thức và Quyền thông báo
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.Calendar;

public class MenuActivity extends AppCompatActivity {

    TextView txtPlayerName;

    // [ĐÃ SỬA]: Đổi từ ImageButton thành CardView để khớp với giao diện mới
    CardView btnPlayGame;

    Button btnProfile, btnAbout, btnSettings;

    FirebaseAuth mAuth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // --- ÁNH XẠ VIEW ---
        txtPlayerName = findViewById(R.id.txtPlayerName);

        // [ĐÃ SỬA]: findViewById sẽ tìm CardView thay vì ImageButton
        btnPlayGame = findViewById(R.id.btnPlayGame);

        btnProfile = findViewById(R.id.btnProfile);
        btnAbout = findViewById(R.id.btnAbout);
        btnSettings = findViewById(R.id.btnSettings);

        // --- CÁC SỰ KIỆN CLICK ---

        // 1. Nút Play (CardView vẫn nhận sự kiện click như bình thường)
        btnPlayGame.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class)); // Chuyển sang màn hình chơi game
        });

        // 2. Nút Profile
        btnProfile.setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
        });

        // 3. Nút About
        btnAbout.setOnClickListener(v -> {
            startActivity(new Intent(this, AboutActivity.class));
        });

        // 4. Nút Settings
        btnSettings.setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
        });

        // --- KÍCH HOẠT NHẮC NHỞ HỌC TẬP ---
        askNotificationPermission();
        scheduleDailyReminder();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserName();
    }

    void loadUserName() {
        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();
            db.collection("users").document(uid).get()
                    .addOnSuccessListener(document -> {
                        if (document.exists() && document.contains("name")) {
                            String name = document.getString("name");
                            // Hiển thị tên
                            txtPlayerName.setText("Hi, " + name);
                        } else {
                            txtPlayerName.setText("Hi, Player");
                        }
                    })
                    .addOnFailureListener(e -> {
                        txtPlayerName.setText("Hi, Player");
                    });
        }
    }

    // --- LOGIC THÔNG BÁO (NOTIFICATION) ---

    // 1. Xin quyền gửi thông báo (Android 13+)
    void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    // 2. Đặt lịch báo thức (8h tối mỗi ngày)
    void scheduleDailyReminder() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        // Lưu ý: Phải chắc chắn bạn đã tạo file ReminderReceiver.java
        Intent intent = new Intent(this, ReminderReceiver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 20); // 20 giờ
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        // Nếu 8h tối nay đã qua, đặt lịch cho 8h tối mai
        if (Calendar.getInstance().after(calendar)) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        try {
            if (alarmManager != null) {
                alarmManager.setRepeating(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        AlarmManager.INTERVAL_DAY,
                        pendingIntent
                );
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }
}