package com.example.englishspeedquiz;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;
import java.util.ArrayList;

public class WordAdapter extends ArrayAdapter<DbHelper.Word> {

    // Biến kiểm soát trạng thái xóa
    public boolean isDeleteMode = false;

    public WordAdapter(Context context, ArrayList<DbHelper.Word> words) {
        super(context, 0, words);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        DbHelper.Word word = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_word, parent, false);
        }

        TextView txtContent = convertView.findViewById(R.id.txtWordContent);
        TextView txtMistake = convertView.findViewById(R.id.txtMistakeCount);
        CheckBox chkSelect = convertView.findViewById(R.id.chkSelect);

        txtContent.setText(word.en + " - " + word.vn);

        // Xử lý hiện vòng tròn lỗi
        if (word.mistakeCount > 0) {
            txtMistake.setVisibility(View.VISIBLE);
            txtMistake.setText(String.valueOf(word.mistakeCount));
        } else {
            txtMistake.setVisibility(View.GONE);
        }

        // [MỚI] Xử lý chế độ xóa
        if (isDeleteMode) {
            chkSelect.setVisibility(View.VISIBLE); // Hiện checkbox
            chkSelect.setChecked(word.isSelected); // Set trạng thái tích hay chưa

            // Sự kiện khi bấm vào checkbox
            chkSelect.setOnClickListener(v -> {
                word.isSelected = chkSelect.isChecked();
            });
        } else {
            chkSelect.setVisibility(View.GONE); // Ẩn checkbox
        }

        return convertView;
    }
}