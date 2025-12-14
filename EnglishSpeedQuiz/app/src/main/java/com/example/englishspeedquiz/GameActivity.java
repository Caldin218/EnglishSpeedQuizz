package com.example.englishspeedquiz;

import android.content.Intent;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.speech.tts.TextToSpeech; // Import TTS
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

public class GameActivity extends AppCompatActivity {

    DbHelper db;
    int topicId;
    ArrayList<DbHelper.Word> wordList;

    DbHelper.Word currentCorrectWord;
    Random random = new Random();

    // View
    TextView txtScore, txtLives, txtQuestion;
    Button btnAns1, btnAns2;
    ProgressBar prgTimer, prgCombo;

    LinearLayout layoutCountdown;
    TextView txtCountdown;

    CountDownTimer timer;

    int score = 0;
    int lives = 3;
    final int MAX_LIVES = 10;

    int currentStreak = 0;
    long initialTimeLimit = 5000;
    long currentTimeLimit = initialTimeLimit;
    final long MIN_TIME_LIMIT = 3000;

    int correctAnswerLocation;
    boolean isReviewMode = false;

    // --- [MỚI] KHAI BÁO ÂM THANH & GIỌNG NÓI ---
    TextToSpeech tts;
    SoundPool soundPool;
    int soundCorrect, soundWrong, soundGameOver;
    boolean isSoundLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // --- 1. KHỞI TẠO TTS (GIỌNG NÓI) ---
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.US); // Chọn giọng Mỹ
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Ngôn ngữ không được hỗ trợ");
                }
            }
        });

        // --- 2. KHỞI TẠO SOUNDPOOL (ÂM THANH HIỆU ỨNG) ---
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(3) // Cho phép phát 3 âm thanh cùng lúc
                .setAudioAttributes(audioAttributes)
                .build();

        // Tải file âm thanh từ thư mục res/raw
        // LƯU Ý: Bạn phải có file mp3 trong res/raw thì mới hết lỗi đỏ ở đây
        try {
            soundCorrect = soundPool.load(this, R.raw.correct, 1);
            soundWrong = soundPool.load(this, R.raw.wrong, 1);
            soundGameOver = soundPool.load(this, R.raw.gameover, 1);
        } catch (Exception e) {
            e.printStackTrace(); // Nếu chưa có file thì bỏ qua, không crash app
        }

        soundPool.setOnLoadCompleteListener((soundPool, sampleId, status) -> isSoundLoaded = true);


        // --- LOGIC CŨ ---
        db = new DbHelper(this);
        topicId = getIntent().getIntExtra("TOPIC_ID", -1);
        isReviewMode = getIntent().getBooleanExtra("IS_REVIEW", false);

        if (isReviewMode) wordList = db.getReviewWords(topicId);
        else wordList = db.getWordsByTopic(topicId);

        if (wordList.size() < 2) {
            Toast.makeText(this, "Lỗi: Cần ít nhất 2 từ để chơi!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        txtScore = findViewById(R.id.txtScore);
        txtLives = findViewById(R.id.txtLives);
        txtQuestion = findViewById(R.id.txtQuestion);
        btnAns1 = findViewById(R.id.btnAns1);
        btnAns2 = findViewById(R.id.btnAns2);
        prgTimer = findViewById(R.id.prgTimer);
        prgCombo = findViewById(R.id.prgCombo);
        layoutCountdown = findViewById(R.id.layoutCountdown);
        txtCountdown = findViewById(R.id.txtCountdown);

        prgCombo.setMax(10);
        prgCombo.setProgress(0);

        updateUI();

        startIntroCountdown();

        btnAns1.setOnClickListener(v -> checkAnswer(correctAnswerLocation == 1));
        btnAns2.setOnClickListener(v -> checkAnswer(correctAnswerLocation == 2));
    }

    // Hàm phát âm từ vựng
    void speakWord(String word) {
        if (tts != null) {
            // QUEUE_FLUSH: Ngắt lời câu cũ để đọc câu mới ngay
            tts.speak(word, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    // Hàm phát âm thanh hiệu ứng
    void playSound(int soundID) {
        if (isSoundLoaded) {
            // volume trái/phải (1.0 là max), priority, loop (0=ko lặp), rate (1.0=bình thường)
            soundPool.play(soundID, 1.0f, 1.0f, 1, 0, 1.0f);
        }
    }

    void startIntroCountdown() {
        new CountDownTimer(4100, 1000) {
            public void onTick(long millisUntilFinished) {
                int count = (int) (millisUntilFinished / 1000);
                if (count > 0) {
                    txtCountdown.setText(String.valueOf(count));
                } else {
                    txtCountdown.setText("GO!");
                    txtCountdown.setTextColor(android.graphics.Color.parseColor("#4CAF50"));
                }
                ScaleAnimation fade_in =  new ScaleAnimation(0f, 1f, 0f, 1f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                fade_in.setDuration(500);
                fade_in.setFillAfter(true);
                txtCountdown.startAnimation(fade_in);
            }

            public void onFinish() {
                layoutCountdown.setVisibility(View.GONE);
                nextQuestion();
            }
        }.start();
    }

    void nextQuestion() {
        if (lives <= 0) {
            endGame("Game Over! Bạn đã hết mạng.");
            return;
        }

        if (wordList.isEmpty()) {
            endGame("Bạn đã hoàn thành danh sách!");
            return;
        }

        currentCorrectWord = wordList.get(random.nextInt(wordList.size()));
        DbHelper.Word wrongWord;
        do {
            wrongWord = wordList.get(random.nextInt(wordList.size()));
        } while (wrongWord.id == currentCorrectWord.id);

        txtQuestion.setText("Nghĩa của '" + currentCorrectWord.en + "' là gì?");

        // [MỚI] Đọc từ tiếng Anh lên ngay khi hiện câu hỏi
        speakWord(currentCorrectWord.en);

        correctAnswerLocation = random.nextInt(2) + 1;
        if (correctAnswerLocation == 1) {
            btnAns1.setText(currentCorrectWord.vn);
            btnAns2.setText(wrongWord.vn);
        } else {
            btnAns1.setText(wrongWord.vn);
            btnAns2.setText(currentCorrectWord.vn);
        }

        startTimer(currentTimeLimit);
    }

    void checkAnswer(boolean isCorrect) {
        if (timer != null) timer.cancel();

        if (isCorrect) {
            // [MỚI] Phát tiếng TING
            playSound(soundCorrect);

            currentStreak++;
            if (currentStreak >= 10) score += 2;
            else score += 1;

            prgCombo.setProgress(Math.min(currentStreak, 10));

            if (currentStreak > 0 && currentStreak % 10 == 0) {
                if (lives < MAX_LIVES) {
                    lives++;
                    Toast.makeText(this, "Chuỗi 10! +1 Mạng", Toast.LENGTH_SHORT).show();
                }
            }

            if (currentStreak == 5) currentTimeLimit = 4000;
            else if (currentStreak == 10) currentTimeLimit = MIN_TIME_LIMIT;

            if (isReviewMode && currentCorrectWord.mistakeCount > 0) {
                db.updateMistakeCount(currentCorrectWord.id, currentCorrectWord.mistakeCount - 1);
            }

            updateUI();
            nextQuestion();

        } else {
            // [MỚI] Phát tiếng ÈÈ
            playSound(soundWrong);

            if (!isReviewMode) {
                db.increaseMistakeCount(currentCorrectWord.id);
            }

            lives--;
            updateUI();

            currentStreak = 0;
            currentTimeLimit = initialTimeLimit;
            prgCombo.setProgress(0);

            showWrongAnswerDialog();
        }
    }


    void showWrongAnswerDialog() {
        // [SỬA LỖI] Không đọc ngay lập tức, mà đợi 0.5 giây để tiếng "È" kêu xong đã
        new android.os.Handler().postDelayed(() -> {
            speakWord(currentCorrectWord.en);
        }, 500); // Delay 500ms (0.5 giây)

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Sai rồi!");

        // Hiển thị rõ cả tiếng Anh và tiếng Việt
        builder.setMessage("Đáp án đúng là:\n" + currentCorrectWord.en + "\n(" + currentCorrectWord.vn + ")");

        builder.setCancelable(false);

        builder.setPositiveButton("Tiếp tục", (dialog, which) -> {
            dialog.dismiss();
            // Nếu đang đọc dở thì tắt tiếng đi để sang câu mới
            if (tts != null) tts.stop();

            if (lives > 0) {
                nextQuestion();
            } else {
                endGame("Game Over! Bạn đã hết mạng.");
            }
        });

        builder.show();
    }


    void startTimer(long millis) {
        if (timer != null) timer.cancel();

        prgTimer.setMax((int) millis);
        prgTimer.setProgress((int) millis);

        timer = new CountDownTimer(millis, 50) {
            @Override
            public void onTick(long millisUntilFinished) {
                prgTimer.setProgress((int) millisUntilFinished);
            }

            @Override
            public void onFinish() {
                prgTimer.setProgress(0);
                checkAnswer(false);
            }
        }.start();
    }

    void updateUI() {
        txtScore.setText("Score: " + score);
        txtLives.setText(String.valueOf(lives));
    }

    void endGame(String message) {
        if (timer != null) timer.cancel();

        // [MỚI] Phát tiếng Game Over
        playSound(soundGameOver);

        if (!isReviewMode) {
            db.updateHighScore(topicId, score);
        }

        new AlertDialog.Builder(this)
                .setTitle("Kết thúc")
                .setMessage(message + "\nĐiểm số của bạn: " + score)
                .setPositiveButton("Chơi lại", (dialog, which) -> {
                    recreate();
                })
                .setNegativeButton("Thoát", (dialog, which) -> {
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    @Override
    protected void onDestroy() {
        // Giải phóng tài nguyên âm thanh khi thoát game để tránh lag máy
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (soundPool != null) {
            soundPool.release();
        }
        super.onDestroy();
        if (timer != null) timer.cancel();
    }
}