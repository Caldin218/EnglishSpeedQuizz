package com.example.englishspeedquiz;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class LessonListActivity extends AppCompatActivity {
    DbHelper db;
    ListView lvLessons;
    TextView txtCategoryTitle;
    ArrayList<DbHelper.Topic> lessonList;
    String categoryName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lesson_list);

        db = new DbHelper(this);
        lvLessons = findViewById(R.id.lvLessons);
        txtCategoryTitle = findViewById(R.id.txtCategoryTitle);

        // Nhận tên Category từ màn hình chính (VD: Animals)
        categoryName = getIntent().getStringExtra("CATEGORY_NAME");
        txtCategoryTitle.setText("Chủ đề: " + categoryName);

        // Bấm vào Lesson -> Vào xem từ vựng/chơi game
        lvLessons.setOnItemClickListener((parent, view, position, id) -> {
            DbHelper.Topic selectedTopic = lessonList.get(position);
            Intent intent = new Intent(LessonListActivity.this, WordManagerActivity.class);
            intent.putExtra("TOPIC_ID", selectedTopic.id);
            intent.putExtra("TOPIC_NAME", selectedTopic.name);
            intent.putExtra("TOPIC_TYPE", 1); // Lesson luôn là type 1
            startActivity(intent);
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> {
            finish(); // Đóng màn hình này -> Tự động quay về MainActivity
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        loadLessons();
    }

    void loadLessons() {
        lessonList = db.getLessonsByCategory(categoryName);
        ArrayAdapter<DbHelper.Topic> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, lessonList);
        lvLessons.setAdapter(adapter);
    }
}