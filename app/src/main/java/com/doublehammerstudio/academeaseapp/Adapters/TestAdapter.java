package com.doublehammerstudio.academeaseapp.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.doublehammerstudio.academeaseapp.Models.TestItem;
import com.doublehammerstudio.academeaseapp.R;

import java.util.ArrayList;

public class TestAdapter extends RecyclerView.Adapter<TestAdapter.TestViewHolder> {

    private ArrayList<TestItem> testList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(TestItem testItem);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public TestAdapter(ArrayList<TestItem> testList) {
        this.testList = testList;
    }

    @NonNull
    @Override
    public TestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.test_item_layout, parent, false);
        return new TestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TestViewHolder holder, int position) {
        TestItem currentItem = testList.get(position);
        holder.testName.setText(currentItem.getTestName());
        holder.testDate.setText(currentItem.getTestDate());
        holder.teacherName.setText(currentItem.getTeacherName());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(currentItem);
            }
        });
    }

    @Override
    public int getItemCount() {
        return testList.size();
    }

    public static class TestViewHolder extends RecyclerView.ViewHolder {
        public TextView testName, testDate, teacherName;

        public TestViewHolder(@NonNull View itemView) {
            super(itemView);
            testName = itemView.findViewById(R.id.testName);
            testDate = itemView.findViewById(R.id.testDate);
            teacherName = itemView.findViewById(R.id.teacherName);
        }
    }
}
