package com.example.capstone;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import android.view.View;
import java.util.ArrayList;
import java.util.List;

//This is the adapter for the student selection recycler view
public class StudentSelectionAdapter extends RecyclerView.Adapter<StudentSelectionAdapter.ViewHolder> {
    private List<Student> students;
    private List<Student> selectedStudents = new ArrayList<>();

    public StudentSelectionAdapter(List<Student> students) {
        this.students = students;
    }

    public List<Student> getSelectedStudents() {
        return selectedStudents;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student_selection, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Student student = students.get(position);
        holder.nameTextView.setText(student.getName());
        holder.checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedStudents.add(student);
            } else {
                selectedStudents.remove(student);
            }
        });
    }

    @Override
    public int getItemCount() {
        return students.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        CheckBox checkbox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.student_name);
            checkbox = itemView.findViewById(R.id.student_checkbox);
        }
    }
}
