package com.example.englishspeedquiz;
import com.google.firebase.firestore.SetOptions;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageButton; // Import ImageButton
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    // Khai báo View
    ImageView imgAvatar;
    TextView txtEmail;
    EditText edtName;
    RadioGroup radioGroupGender;
    RadioButton radioMale, radioFemale;
    Button btnSaveProfile, btnLogout;
    ImageButton btnBack; // [MỚI] Nút Back

    // Firebase
    FirebaseAuth mAuth;
    FirebaseFirestore db;
    String uid;

    // Mảng ảnh Avatar (Đảm bảo bạn đã tạo các file này trong drawable)
    int[] avatarResources = {
            R.drawable.ic_launcher_foreground,
            R.drawable.ic_heart_red,
            R.drawable.ic_delete_trash
    };

    int currentAvatarIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() == null) {
            finish(); return;
        }
        uid = mAuth.getCurrentUser().getUid();

        // Ánh xạ
        btnBack = findViewById(R.id.btnBack); // Ánh xạ nút Back
        imgAvatar = findViewById(R.id.imgAvatar);
        txtEmail = findViewById(R.id.txtEmail);
        edtName = findViewById(R.id.edtName);
        radioGroupGender = findViewById(R.id.radioGroupGender);
        radioMale = findViewById(R.id.radioMale);
        radioFemale = findViewById(R.id.radioFemale);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnLogout = findViewById(R.id.btnLogout);

        txtEmail.setText(mAuth.getCurrentUser().getEmail());

        // Tải dữ liệu cũ
        loadProfile();

        // --- SỰ KIỆN NÚT BACK ---
        btnBack.setOnClickListener(v -> {
            finish(); // Đóng màn hình này để quay lại Main
        });

        // Sự kiện bấm vào Avatar
        imgAvatar.setOnClickListener(v -> showAvatarSelectionDialog());

        // Sự kiện Lưu
        btnSaveProfile.setOnClickListener(v -> saveProfile());

        // Sự kiện Đăng xuất
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    void showAvatarSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chọn Ảnh Đại Diện");

        GridLayout gridLayout = new GridLayout(this);
        gridLayout.setColumnCount(3);
        gridLayout.setPadding(20, 20, 20, 20);

        final AlertDialog dialog = builder.create();

        for (int i = 0; i < avatarResources.length; i++) {
            final int index = i;
            ImageView imageView = new ImageView(this);
            imageView.setImageResource(avatarResources[i]);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(150, 150);
            params.setMargins(10, 10, 10, 10);
            imageView.setLayoutParams(params);
            imageView.setBackgroundResource(R.drawable.bg_circle_error);
            imageView.setPadding(15,15,15,15);

            imageView.setOnClickListener(v -> {
                currentAvatarIndex = index;
                imgAvatar.setImageResource(avatarResources[index]);
                dialog.dismiss();
            });

            gridLayout.addView(imageView);
        }

        dialog.setView(gridLayout);
        dialog.show();
    }

    void loadProfile() {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        if (document.contains("name")) edtName.setText(document.getString("name"));

                        if (document.contains("gender")) {
                            String gender = document.getString("gender");
                            if ("Nam".equals(gender)) radioMale.setChecked(true);
                            else if ("Nữ".equals(gender)) radioFemale.setChecked(true);
                        }

                        if (document.contains("avatarIndex")) {
                            Long idx = document.getLong("avatarIndex");
                            if (idx != null) {
                                currentAvatarIndex = idx.intValue();
                                if (currentAvatarIndex >= 0 && currentAvatarIndex < avatarResources.length) {
                                    imgAvatar.setImageResource(avatarResources[currentAvatarIndex]);
                                }
                            }
                        }
                    }
                });
    }

    void saveProfile() {
        String name = edtName.getText().toString().trim();
        String gender = "Khác";
        if (radioMale.isChecked()) gender = "Nam";
        else if (radioFemale.isChecked()) gender = "Nữ";

        if (name.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên!", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("gender", gender);
        updates.put("avatarIndex", currentAvatarIndex);

        // [SỬA ĐOẠN NÀY]
        // Thay vì dùng .update(), ta dùng .set() kết hợp SetOptions.merge()
        // Ý nghĩa: "Nếu chưa có hồ sơ thì tạo mới, nếu có rồi thì chỉ cập nhật các trường này thôi"
        db.collection("users").document(uid)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "✅ Cập nhật hồ sơ thành công!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "❌ Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}