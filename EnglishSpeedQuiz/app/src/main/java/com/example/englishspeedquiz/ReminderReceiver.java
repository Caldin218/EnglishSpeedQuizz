package com.example.englishspeedquiz;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class ReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // Khi báo thức reo -> Gọi hàm hiện thông báo
        createNotificationChannel(context);
        showNotification(context);
    }

    void createNotificationChannel(Context context) {
        // Bắt buộc cho Android 8.0 trở lên
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Nhắc nhở học tập";
            String description = "Thông báo nhắc bạn vào học mỗi ngày";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("study_reminder", name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    void showNotification(Context context) {
        // Khi bấm vào thông báo sẽ mở màn hình Login
        Intent i = new Intent(context, LoginActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, i, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "study_reminder")
                .setSmallIcon(R.drawable.ic_heart_red) // Dùng tạm icon trái tim
                .setContentTitle("⏰ Đã đến giờ học từ vựng!")
                .setContentText("Dành 5 phút ôn tập để không bị quên bài nhé!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // [ĐÃ SỬA LỖI CHÍNH TẢ Ở ĐÂY: android.Manifest]
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify(200, builder.build());
        }
    }
}