package com.example.englishspeedquiz;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;

public class DbHelper extends SQLiteOpenHelper {

    // --- CẤU HÌNH DATABASE ---
    private static final String DB_NAME = "QuizGame.db";
    private static final int DB_VERSION = 7; // [QUAN TRỌNG] Giữ nguyên số 5

    // Tên bảng
    public static final String TABLE_TOPIC = "Topic";
    public static final String TABLE_WORD = "Word";

    // Cột của bảng Topic
    public static final String COL_ID = "id";
    public static final String COL_TOPIC_NAME = "name";
    public static final String COL_HIGH_SCORE = "highScore";
    public static final String COL_TYPE = "type";       // 0: User, 1: Lesson
    public static final String COL_CATEGORY = "category"; // Tên chủ đề lớn (Animals, Food...)

    // Cột của bảng Word
    public static final String COL_EN = "enWord";
    public static final String COL_VN = "vnMeaning";
    public static final String COL_TOPIC_ID = "topicId";
    public static final String COL_MISTAKE_COUNT = "mistakeCount"; // Đếm số lần sai

    public DbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    // --- KHỞI TẠO BẢNG ---
    @Override
    public void onCreate(SQLiteDatabase db) {
        // 1. Tạo bảng Topic
        String createTopic = "CREATE TABLE " + TABLE_TOPIC + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_TOPIC_NAME + " TEXT, " +
                COL_HIGH_SCORE + " INTEGER DEFAULT 0, " +
                COL_TYPE + " INTEGER DEFAULT 0, " +
                COL_CATEGORY + " TEXT)";
        db.execSQL(createTopic);

        // 2. Tạo bảng Word
        String createWord = "CREATE TABLE " + TABLE_WORD + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_EN + " TEXT, " +
                COL_VN + " TEXT, " +
                COL_TOPIC_ID + " INTEGER, " +
                COL_MISTAKE_COUNT + " INTEGER DEFAULT 0)";
        db.execSQL(createWord);

        // 3. Nạp dữ liệu mẫu (Lessons)
        insertDefaultData(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_WORD);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TOPIC);
        onCreate(db);
    }

    // --- DỮ LIỆU MẪU (LESSONS) ---
    private void insertDefaultData(SQLiteDatabase db) {
        // --- CHỦ ĐỀ LỚN: ANIMALS ---
        long id1 = addDefaultTopic(db, "Lesson 1: Thú cưng", "Animals", 1);
        addDefaultWord(db, id1, "Cat", "Con mèo");
        addDefaultWord(db, id1, "Dog", "Con chó");

        long id2 = addDefaultTopic(db, "Lesson 2: Động vật hoang dã", "Animals", 1);
        addDefaultWord(db, id2, "Lion", "Sư tử");
        addDefaultWord(db, id2, "Tiger", "Con hổ");
        addDefaultWord(db, id2, "Elephant", "Con voi");

        // --- CHỦ ĐỀ LỚN: FOOD ---
        long id3 = addDefaultTopic(db, "Lesson 1: Trái cây", "Food", 1);
        addDefaultWord(db, id3, "Apple", "Quả táo");
        addDefaultWord(db, id3, "Banana", "Quả chuối");

        long id4 = addDefaultTopic(db, "Lesson 2: Món chính", "Food", 1);
        addDefaultWord(db, id4, "Rice", "Cơm");
        addDefaultWord(db, id4, "Bread", "Bánh mì");
    }

    // Helper để thêm Topic mẫu
    private long addDefaultTopic(SQLiteDatabase db, String name, String category, int type) {
        ContentValues values = new ContentValues();
        values.put(COL_TOPIC_NAME, name);
        values.put(COL_CATEGORY, category);
        values.put(COL_TYPE, type);
        return db.insert(TABLE_TOPIC, null, values);
    }

    // Helper để thêm Word mẫu
    private void addDefaultWord(SQLiteDatabase db, long topicId, String en, String vn) {
        ContentValues values = new ContentValues();
        values.put(COL_EN, en);
        values.put(COL_VN, vn);
        values.put(COL_TOPIC_ID, topicId);
        values.put(COL_MISTAKE_COUNT, 0);
        db.insert(TABLE_WORD, null, values);
    }

    // ==========================================
    //          CÁC HÀM XỬ LÝ TOPIC
    // ==========================================

    // 1. Thêm chủ đề mới (Do User tạo -> Type = 0)
    public long addTopic(String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TOPIC_NAME, name);
        values.put(COL_TYPE, 0);
        values.put(COL_CATEGORY, "My Words");
        return db.insert(TABLE_TOPIC, null, values);
    }

    // 2. Lấy danh sách Category duy nhất (Animals, Food...) cho tab Lessons
    public ArrayList<String> getUniqueCategories() {
        ArrayList<String> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT DISTINCT " + COL_CATEGORY + " FROM " + TABLE_TOPIC + " WHERE " + COL_TYPE + " = 1", null);
        if (cursor.moveToFirst()) {
            do {
                list.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    // 3. Lấy các Lesson con thuộc 1 Category (VD: Bấm Animals ra Lesson 1, Lesson 2)
    public ArrayList<Topic> getLessonsByCategory(String category) {
        ArrayList<Topic> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_TOPIC + " WHERE " + COL_CATEGORY + " = ?", new String[]{category});
        if (cursor.moveToFirst()) {
            do {
                list.add(new Topic(cursor.getInt(0), cursor.getString(1), cursor.getInt(2), 1));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    // 4. Lấy danh sách của tôi (My Words - Type 0)
    public ArrayList<Topic> getMyTopics() {
        ArrayList<Topic> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_TOPIC + " WHERE " + COL_TYPE + " = 0", null);
        if (cursor.moveToFirst()) {
            do {
                list.add(new Topic(cursor.getInt(0), cursor.getString(1), cursor.getInt(2), 0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    // 5. Cập nhật điểm cao (High Score)
    public void updateHighScore(int topicId, int newScore) {
        SQLiteDatabase db = this.getWritableDatabase();
        int currentScore = 0;

        // Lấy điểm cũ ra xem
        Cursor cursor = db.rawQuery("SELECT " + COL_HIGH_SCORE + " FROM " + TABLE_TOPIC + " WHERE " + COL_ID + "=?", new String[]{String.valueOf(topicId)});
        if (cursor.moveToFirst()) {
            currentScore = cursor.getInt(0);
        }
        cursor.close();

        // Chỉ cập nhật nếu điểm MỚI > điểm CŨ
        if (newScore > currentScore) {
            ContentValues values = new ContentValues();
            values.put(COL_HIGH_SCORE, newScore);
            db.update(TABLE_TOPIC, values, COL_ID + "=?", new String[]{String.valueOf(topicId)});
        }
    }

    // ==========================================
    //          CÁC HÀM XỬ LÝ WORD
    // ==========================================

    // 1. Thêm từ mới
    public void addWord(String en, String vn, int topicId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_EN, en);
        values.put(COL_VN, vn);
        values.put(COL_TOPIC_ID, topicId);
        values.put(COL_MISTAKE_COUNT, 0);
        db.insert(TABLE_WORD, null, values);
    }

    // 2. Sửa từ
    public void updateWord(int id, String en, String vn) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_EN, en);
        values.put(COL_VN, vn);
        db.update(TABLE_WORD, values, COL_ID + "=?", new String[]{String.valueOf(id)});
    }

    // 3. Xóa từ
    public void deleteWord(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_WORD, COL_ID + "=?", new String[]{String.valueOf(id)});
    }

    // 4. Lấy danh sách từ theo Topic (Bao gồm cả số lỗi sai)
    public ArrayList<Word> getWordsByTopic(int topicId) {
        ArrayList<Word> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_WORD + " WHERE " + COL_TOPIC_ID + " = ?", new String[]{String.valueOf(topicId)});
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0);
                String en = cursor.getString(1);
                String vn = cursor.getString(2);
                int tId = cursor.getInt(3);
                int mistakes = cursor.getInt(4); // Lấy số lỗi sai
                list.add(new Word(id, en, vn, tId, mistakes));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    // 5. Tăng số lần sai (Dùng khi chơi game trả lời sai)
    public void increaseMistakeCount(int wordId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("UPDATE " + TABLE_WORD +
                " SET " + COL_MISTAKE_COUNT + " = " + COL_MISTAKE_COUNT + " + 1 " +
                " WHERE " + COL_ID + " = " + wordId);
    }

    // 6. Lấy danh sách từ Ôn tập (Sai > 10 lần)
    public ArrayList<Word> getReviewWords(int topicId) {
        ArrayList<Word> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // LƯU Ý: Bạn có thể sửa số 10 thành số 0 để test cho dễ
        String query = "SELECT * FROM " + TABLE_WORD +
                " WHERE " + COL_TOPIC_ID + " = ? AND " + COL_MISTAKE_COUNT + " > 2";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(topicId)});
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0);
                String en = cursor.getString(1);
                String vn = cursor.getString(2);
                int tId = cursor.getInt(3);
                int mistakes = cursor.getInt(4);
                list.add(new Word(id, en, vn, tId, mistakes));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    // ==========================================
    //          MODELS (CLASS CON)
    // ==========================================


    // [MỚI] Cập nhật số lỗi sai thành một giá trị cụ thể (Dùng cho chế độ Ôn tập)
    public void updateMistakeCount(int wordId, int newCount) {
        SQLiteDatabase db = this.getWritableDatabase();
        // Đảm bảo số lỗi không bao giờ âm
        if (newCount < 0) newCount = 0;

        ContentValues values = new ContentValues();
        values.put(COL_MISTAKE_COUNT, newCount);

        db.update(TABLE_WORD, values, COL_ID + "=?", new String[]{String.valueOf(wordId)});
    }


    public static class Topic {
        public int id; public String name; public int highScore; public int type;
        public Topic(int id, String name, int highScore, int type) {
            this.id = id; this.name = name; this.highScore = highScore; this.type = type;
        }
        @Override public String toString() {
            return name + "   (🏆 " + highScore + ")";
        }
    }

    public static class Word {
        public int id; public String en; public String vn; int topicId;
        public int mistakeCount;
        public boolean isSelected = false;


        public Word(int id, String en, String vn, int topicId, int mistakeCount) {
            this.id = id; this.en = en; this.vn = vn; this.topicId = topicId;
            this.mistakeCount = mistakeCount;
        }

        // Constructor cũ để tương thích
        public Word(int id, String en, String vn, int topicId) {
            this(id, en, vn, topicId, 0);
        }

        @Override public String toString() {
            return en + " - " + vn;
        }
    }

    // [MỚI] Kiểm tra xem một Topic đã tồn tại trong SQLite chưa (Dựa theo tên)
    public boolean isTopicExists(String topicName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COL_ID + " FROM " + TABLE_TOPIC +
                " WHERE " + COL_TOPIC_NAME + " = ?", new String[]{topicName});
        boolean exists = (cursor.getCount() > 0);
        cursor.close();
        return exists;
    }
}