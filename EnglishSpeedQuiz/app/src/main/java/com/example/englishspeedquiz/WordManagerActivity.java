package com.example.englishspeedquiz;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class WordManagerActivity extends AppCompatActivity {

    // --- KHAI BÁO BIẾN ---
    DbHelper db;
    int topicId;
    int topicType; // 0: User, 1: Lesson

    // Giao diện
    EditText edtEn, edtVn;
    TextView txtCount, txtTitle, txtReviewCount;
    Button btnAddWord, btnPlay, btnCancelDelete, btnConfirmDelete;
    ImageButton btnBack, btnTrash;

    LinearLayout layoutInput, layoutDeleteTools;
    ListView lvWords;
    Switch swReviewMode;
    CheckBox chkSelectAll;

    // Giao diện Dropdown Header
    LinearLayout layoutListHeader;
    ImageView imgArrow;
    boolean isListExpanded = true;

    // Dữ liệu
    ArrayList<DbHelper.Word> fullList;
    ArrayList<DbHelper.Word> reviewList;
    ArrayList<DbHelper.Word> currentList;
    WordAdapter adapter;

    int selectedWordId = -1; // -1: Thêm mới

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_word_manager);

        db = new DbHelper(this);

        // Lấy dữ liệu từ Intent
        topicId = getIntent().getIntExtra("TOPIC_ID", -1);
        String topicName = getIntent().getStringExtra("TOPIC_NAME");
        topicType = getIntent().getIntExtra("TOPIC_TYPE", 0);

        // --- 1. ÁNH XẠ VIEW ---
        txtTitle = findViewById(R.id.txtTitle);
        edtEn = findViewById(R.id.edtEn);
        edtVn = findViewById(R.id.edtVn);
        txtCount = findViewById(R.id.txtCount);

        btnAddWord = findViewById(R.id.btnAddWord);
        btnPlay = findViewById(R.id.btnPlay);
        btnBack = findViewById(R.id.btnBack);
        btnTrash = findViewById(R.id.btnTrash);

        layoutInput = findViewById(R.id.layoutInput);
        lvWords = findViewById(R.id.lvWords);

        swReviewMode = findViewById(R.id.swReviewMode);
        txtReviewCount = findViewById(R.id.txtReviewCount);

        layoutDeleteTools = findViewById(R.id.layoutDeleteTools);
        chkSelectAll = findViewById(R.id.chkSelectAll);
        btnCancelDelete = findViewById(R.id.btnCancelDelete);
        btnConfirmDelete = findViewById(R.id.btnConfirmDelete);

        layoutListHeader = findViewById(R.id.layoutListHeader);
        imgArrow = findViewById(R.id.imgArrow);

        // Thiết lập tiêu đề
        txtTitle.setText("Chủ đề: " + topicName);

        // --- 2. XỬ LÝ GIAO DIỆN BAN ĐẦU ---
        if (topicType == 1) {
            layoutInput.setVisibility(View.GONE);
            btnTrash.setVisibility(View.GONE);
        } else {
            layoutInput.setVisibility(View.VISIBLE);
            btnTrash.setVisibility(View.VISIBLE);
        }

        loadDataFromDB();

        // --- 3. CÁC SỰ KIỆN CLICK ---

        btnBack.setOnClickListener(v -> finish());

        // Dropdown Header
        layoutListHeader.setOnClickListener(v -> toggleListVisibility());

        // Nút Thùng rác
        btnTrash.setOnClickListener(v -> toggleDeleteMode(true));

        // Nút Hủy xóa
        btnCancelDelete.setOnClickListener(v -> toggleDeleteMode(false));

        // Checkbox Chọn tất cả
        chkSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (currentList != null) {
                for (DbHelper.Word w : currentList) {
                    w.isSelected = isChecked;
                }
                adapter.notifyDataSetChanged();
            }
        });

        // Nút Xác nhận Xóa
        btnConfirmDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Xác nhận xóa")
                    .setMessage("Bạn có chắc muốn xóa các từ đã chọn?")
                    .setPositiveButton("Xóa luôn", (dialog, which) -> {
                        int count = 0;
                        for (DbHelper.Word w : currentList) {
                            if (w.isSelected) {
                                db.deleteWord(w.id);
                                count++;
                            }
                        }
                        Toast.makeText(this, "Đã xóa " + count + " từ", Toast.LENGTH_SHORT).show();
                        toggleDeleteMode(false);
                        loadDataFromDB();
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });

        // Switch Chế độ Ôn tập
        swReviewMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentList = reviewList;
                txtCount.setText("Đang lọc: Các từ sai nhiều (" + reviewList.size() + ")");
                if (reviewList.isEmpty()) {
                    Toast.makeText(this, "Không có từ nào sai trên 10 lần!", Toast.LENGTH_SHORT).show();
                }
            } else {
                currentList = fullList;
                updateCountText();
            }
            adapter = new WordAdapter(this, currentList);
            lvWords.setAdapter(adapter);
        });

        // Nút Lưu / Cập nhật
        btnAddWord.setOnClickListener(v -> {
            String en = edtEn.getText().toString().trim();
            String vn = edtVn.getText().toString().trim();

            if (en.isEmpty() || vn.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedWordId == -1) {
                db.addWord(en, vn, topicId);
                Toast.makeText(this, "Đã thêm!", Toast.LENGTH_SHORT).show();
            } else {
                db.updateWord(selectedWordId, en, vn);
                Toast.makeText(this, "Đã cập nhật!", Toast.LENGTH_SHORT).show();
                selectedWordId = -1;
                btnAddWord.setText("Lưu từ này");
            }
            edtEn.setText(""); edtVn.setText("");
            loadDataFromDB();
        });

        // Sự kiện Click dòng (Sửa hoặc Chọn xóa)
        lvWords.setOnItemClickListener((parent, view, position, id) -> {
            DbHelper.Word word = currentList.get(position);

            if (adapter.isDeleteMode) {
                word.isSelected = !word.isSelected;
                adapter.notifyDataSetChanged();
                return;
            }

            if (topicType == 1) {
                Toast.makeText(this, word.en + ": " + word.vn, Toast.LENGTH_SHORT).show();
            } else {
                edtEn.setText(word.en);
                edtVn.setText(word.vn);
                selectedWordId = word.id;
                btnAddWord.setText("Cập nhật từ này");
            }
        });

        // Sự kiện Giữ lì (Xóa nhanh)
        lvWords.setOnItemLongClickListener((parent, view, position, id) -> {
            if (topicType == 0 && !adapter.isDeleteMode) {
                DbHelper.Word word = currentList.get(position);
                new AlertDialog.Builder(this)
                        .setTitle("Xóa từ này?")
                        .setMessage(word.en)
                        .setPositiveButton("Xóa", (dialog, which) -> {
                            db.deleteWord(word.id);
                            loadDataFromDB();
                            Toast.makeText(this, "Đã xóa", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
                return true;
            }
            return false;
        });

        // Nút Chơi Game
        btnPlay.setOnClickListener(v -> {
            boolean isReview = swReviewMode.isChecked();
            int listSize = isReview ? reviewList.size() : fullList.size();

            if (listSize >= 2) {
                Intent intent = new Intent(this, GameActivity.class);
                intent.putExtra("TOPIC_ID", topicId);
                intent.putExtra("IS_REVIEW", isReview);
                startActivity(intent);
            } else {
                if (isReview) Toast.makeText(this, "Cần ít nhất 2 từ sai >10 lần để ôn tập!", Toast.LENGTH_LONG).show();
                else Toast.makeText(this, "Cần ít nhất 2 từ để chơi!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- LOGIC ---

    void toggleListVisibility() {
        if (isListExpanded) {
            lvWords.setVisibility(View.GONE);
            imgArrow.setRotation(-90);
        } else {
            lvWords.setVisibility(View.VISIBLE);
            imgArrow.setRotation(0);
        }
        isListExpanded = !isListExpanded;
    }

    void toggleDeleteMode(boolean enable) {
        if (enable) {
            layoutDeleteTools.setVisibility(View.VISIBLE);
            layoutInput.setVisibility(View.GONE);
            btnPlay.setVisibility(View.GONE);
            btnTrash.setVisibility(View.GONE);
            adapter.isDeleteMode = true;
            adapter.notifyDataSetChanged();
        } else {
            layoutDeleteTools.setVisibility(View.GONE);
            if(topicType == 0) layoutInput.setVisibility(View.VISIBLE);
            btnPlay.setVisibility(View.VISIBLE);
            btnTrash.setVisibility(View.VISIBLE);

            adapter.isDeleteMode = false;
            if(currentList != null) {
                for(DbHelper.Word w : currentList) w.isSelected = false;
            }
            chkSelectAll.setChecked(false);
            adapter.notifyDataSetChanged();
        }
    }

    void loadDataFromDB() {
        fullList = db.getWordsByTopic(topicId);
        reviewList = db.getReviewWords(topicId);

        txtReviewCount.setText("(" + reviewList.size() + " từ cần ôn)");

        // [SỬA LOGIC HIỂN THỊ] Kiểm tra Switch để hiển thị đúng list và text
        if (swReviewMode.isChecked()) {
            currentList = reviewList;
            txtCount.setText("Đang lọc: Các từ sai nhiều (" + reviewList.size() + ")");
        } else {
            currentList = fullList;
            updateCountText();
        }

        adapter = new WordAdapter(this, currentList);
        lvWords.setAdapter(adapter);

        // Tự động thu gọn nếu quá dài
        int itemCount = adapter.getCount();
        if (itemCount > 5) {
            isListExpanded = true;
            toggleListVisibility();
        } else {
            lvWords.setVisibility(View.VISIBLE);
            imgArrow.setRotation(0);
            isListExpanded = true;
        }
    }

    void updateCountText() {
        if (fullList == null) return;
        if (topicType == 1)
            txtCount.setText("Danh sách từ vựng (" + fullList.size() + "):");
        else
            txtCount.setText("Danh sách từ vựng hiện có (" + fullList.size() + "):");
    }
}