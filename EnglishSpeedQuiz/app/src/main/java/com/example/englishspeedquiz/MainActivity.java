package com.example.englishspeedquiz;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.HashMap; // Import HashMap
import java.util.Map;     // Import Map


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.Calendar;
public class MainActivity extends AppCompatActivity {

    DbHelper db;
    ListView lvTopics;
    Button btnTabLessons, btnTabMyWords, btnAddTopic;
    TextView txtLabel;
    ImageButton btnBack;

    int currentType = 1; // 1: Lessons, 0: My Words

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new DbHelper(this);

        // --- 1. ÁNH XẠ VIEW ---
        lvTopics = findViewById(R.id.lvTopics);
        btnTabLessons = findViewById(R.id.btnTabLessons);
        btnTabMyWords = findViewById(R.id.btnTabMyWords);
        btnAddTopic = findViewById(R.id.btnAddTopic);
        txtLabel = findViewById(R.id.txtLabel);
        btnBack = findViewById(R.id.btnBack);

        // Mặc định load Tab Lessons
        switchTab(1);

        // --- 2. CÁC SỰ KIỆN CLICK ---
        btnBack.setOnClickListener(v -> finish());
        btnTabLessons.setOnClickListener(v -> switchTab(1));
        btnTabMyWords.setOnClickListener(v -> switchTab(0));

        btnAddTopic.setOnClickListener(v -> {
            EditText input = new EditText(this);
            new AlertDialog.Builder(this)
                    .setTitle("Tên danh sách mới")
                    .setView(input)
                    .setPositiveButton("Lưu", (dialog, which) -> {
                        String name = input.getText().toString().trim();
                        if (!name.isEmpty()) {
                            db.addTopic(name);
                            loadData();
                        }
                    }).show();
        });

        lvTopics.setOnItemClickListener((parent, view, position, id) -> {
            if (currentType == 1) {
                // Tab Lessons: Mở danh sách bài học con
                String categoryName = (String) parent.getItemAtPosition(position);
                Intent intent = new Intent(MainActivity.this, LessonListActivity.class);
                intent.putExtra("CATEGORY_NAME", categoryName);
                startActivity(intent);
            } else {
                // Tab My Words: Mở trình quản lý từ
                DbHelper.Topic selectedTopic = (DbHelper.Topic) parent.getItemAtPosition(position);
                Intent intent = new Intent(MainActivity.this, WordManagerActivity.class);
                intent.putExtra("TOPIC_ID", selectedTopic.id);
                intent.putExtra("TOPIC_NAME", selectedTopic.name);
                intent.putExtra("TOPIC_TYPE", 0);
                startActivity(intent);
            }
        });

        // --- 3. XỬ LÝ DỮ LIỆU ---

        // [LƯU Ý] Mở comment dòng này 1 lần để nạp dữ liệu phân cấp, sau đó đóng lại
        //seedDataToFirestore();

        // Luôn chạy dòng này để đồng bộ dữ liệu mới về máy
        syncLessonsFromCloud();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    void switchTab(int type) {
        currentType = type;
        if (type == 1) {
            btnTabLessons.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_blue_dark));
            btnTabMyWords.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.darker_gray));
            btnAddTopic.setVisibility(View.GONE);
            txtLabel.setText("Chủ đề bài học:");
        } else {
            btnTabLessons.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.darker_gray));
            btnTabMyWords.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_blue_dark));
            btnAddTopic.setVisibility(View.VISIBLE);
            txtLabel.setText("Danh sách bạn đã tạo:");
        }
        loadData();
    }

    void loadData() {
        if (currentType == 1) {
            ArrayList<String> categories = db.getUniqueCategories();
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, categories);
            lvTopics.setAdapter(adapter);
        } else {
            ArrayList<DbHelper.Topic> myTopics = db.getMyTopics();
            ArrayAdapter<DbHelper.Topic> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, myTopics);
            lvTopics.setAdapter(adapter);
        }
    }

    // --- TOOL ADMIN: NẠP DỮ LIỆU PHÂN CẤP (HIERARCHY) ---
    void seedDataToFirestore() {
        com.google.firebase.firestore.FirebaseFirestore firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance();
        Toast.makeText(this, "Đang nạp dữ liệu phân cấp...", Toast.LENGTH_LONG).show();

        // 1. TRAVEL
        createCategoryWithLessons(firestore, "Travel (Du lịch)", new Object[][]{
                {"Lesson 1: Tại sân bay", new String[][]{
                        {"Passport", "Hộ chiếu"}, {"Ticket", "Vé máy bay"}, {"Luggage", "Hành lý"}, {"Departure", "Khởi hành"}
                }},
                {"Lesson 2: Khách sạn", new String[][]{
                        {"Hotel", "Khách sạn"}, {"Reception", "Lễ tân"}, {"Room key", "Chìa khóa phòng"}, {"Check-out", "Trả phòng"}
                }}
        });

        // 2. BUSINESS
        createCategoryWithLessons(firestore, "Business (Kinh doanh)", new Object[][]{
                {"Lesson 1: Văn phòng", new String[][]{
                        {"Meeting", "Cuộc họp"}, {"Boss", "Sếp"}, {"Deadline", "Hạn chót"}, {"Salary", "Lương"}
                }},
                {"Lesson 2: Hợp đồng", new String[][]{
                        {"Contract", "Hợp đồng"}, {"Signature", "Chữ ký"}, {"Partner", "Đối tác"}, {"Deal", "Thỏa thuận"}
                }}
        });
    }

    // [QUAN TRỌNG] Đây là hàm bạn bị thiếu lúc nãy
    void createCategoryWithLessons(com.google.firebase.firestore.FirebaseFirestore db, String categoryName, Object[][] lessons) {
        for (Object[] lesson : lessons) {
            String lessonName = (String) lesson[0];
            String[][] words = (String[][]) lesson[1];

            Map<String, Object> lessonData = new HashMap<>();
            lessonData.put("name", lessonName);
            lessonData.put("category", categoryName); // Gắn nhãn Category

            db.collection("lessons").add(lessonData).addOnSuccessListener(docRef -> {
                for (String[] pair : words) {
                    Map<String, Object> wordData = new HashMap<>();
                    wordData.put("en", pair[0]);
                    wordData.put("vn", pair[1]);
                    docRef.collection("words").add(wordData);
                }
            });
        }
    }

    // --- SYNC DATA ---
    void syncLessonsFromCloud() {
        com.google.firebase.firestore.FirebaseFirestore firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance();

        firestore.collection("lessons").get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (com.google.firebase.firestore.DocumentSnapshot document : queryDocumentSnapshots) {
                String lessonName = document.getString("name");
                String categoryName = document.getString("category");

                if (categoryName == null) categoryName = "Other";

                if (!db.isTopicExists(lessonName)) {
                    // 1. Tạo Topic
                    long topicId = db.addTopic(lessonName);

                    // 2. Cập nhật Category và Type
                    updateTopicInfo(lessonName, categoryName);

                    // 3. Tải từ vựng
                    document.getReference().collection("words").get().addOnSuccessListener(wordSnapshots -> {
                        for (com.google.firebase.firestore.DocumentSnapshot wordDoc : wordSnapshots) {
                            String en = wordDoc.getString("en");
                            String vn = wordDoc.getString("vn");
                            db.addWord(en, vn, (int) topicId);
                        }
                        if (currentType == 1) loadData();
                    });
                }
            }
        });
    }

    // Hàm cập nhật thông tin Topic
    void updateTopicInfo(String topicName, String categoryName) {
        db.getWritableDatabase().execSQL("UPDATE " + DbHelper.TABLE_TOPIC +
                        " SET " + DbHelper.COL_TYPE + " = 1, " +
                        DbHelper.COL_CATEGORY + " = ? " +
                        " WHERE " + DbHelper.COL_TOPIC_NAME + " = ?",
                new Object[]{categoryName, topicName});
    }
}