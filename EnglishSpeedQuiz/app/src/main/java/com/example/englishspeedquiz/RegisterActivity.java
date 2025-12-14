package com.example.englishspeedquiz;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    EditText edtEmail, edtPassword, edtConfirmPassword;
    Button btnRegister;
    FirebaseAuth mAuth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register); // Đã sửa đúng tên layout

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Ánh xạ View
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);

        btnRegister.setOnClickListener(v -> registerUser());
    }

    void registerUser() {
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();
        String confirmPass = edtConfirmPassword.getText().toString().trim();

        // 1. Kiểm tra rỗng
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPass)) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Kiểm tra độ dài mật khẩu
        if (password.length() < 6) {
            Toast.makeText(this, "Mật khẩu phải từ 6 ký tự trở lên!", Toast.LENGTH_SHORT).show();
            return;
        }

        // 3. Kiểm tra khớp mật khẩu
        if (!password.equals(confirmPass)) {
            Toast.makeText(this, "Mật khẩu xác nhận không khớp!", Toast.LENGTH_SHORT).show();
            return;
        }

        // 4. Thực hiện đăng ký với Firebase
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Đăng ký Auth thành công -> Tạo Profile trên Firestore
                        String uid = mAuth.getCurrentUser().getUid();
                        createDefaultProfile(uid, email);
                    } else {
                        // Đăng ký thất bại (do trùng email, mất mạng...)
                        Toast.makeText(this, "Lỗi: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    void createDefaultProfile(String uid, String email) {
        Map<String, Object> user = new HashMap<>();
        user.put("email", email);
        user.put("name", "Người chơi mới"); // Tên mặc định
        user.put("level", "Cơ bản");
        user.put("goal", "Học 50 từ mỗi tuần");
        user.put("totalScore", 0);
        user.put("avatarIndex", 0); // Avatar mặc định số 0

        db.collection("users").document(uid).set(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();

                    // Chuyển sang màn hình Đăng nhập để người dùng đăng nhập lại
                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish(); // Đóng màn hình đăng ký
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tạo profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}