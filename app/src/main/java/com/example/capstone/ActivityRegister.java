package com.example.capstone;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import android.widget.EditText;

import com.google.firebase.firestore.FirebaseFirestore;

public class ActivityRegister extends MainActivity {

    private FirebaseFirestore cloud_fs_db; //Firestore firebase database

    //Teacher Data
    private EditText Teacher_name;
    private EditText Teacher_password;
    private EditText Teacher_email;
    //Teacher Data

    //Student Data
    private EditText Student_info_layout;
    private EditText Student_name;
    private EditText Student_password;
    private EditText Student_email;
    //Student Data

    private LinearLayout teacherInfoLayout;
    private LinearLayout studentInfoLayout;
    private CheckBox teacherCheckBox;
    private CheckBox studentCheckBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        teacherInfoLayout = findViewById(R.id.Teacher_info_layout);
        studentInfoLayout = findViewById(R.id.Student_info_layout);
        teacherCheckBox = findViewById(R.id.Teacher_checkBox);
        studentCheckBox = findViewById(R.id.Student_checkBox3);

        teacherCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {// The Text boxes for Teachers will appear when check box is checked
            if (isChecked) {
                teacherInfoLayout.setVisibility(View.VISIBLE);
                studentInfoLayout.setVisibility(View.GONE); //Hide student fields
                studentCheckBox.setChecked(false);
            } else {
                teacherInfoLayout.setVisibility(View.GONE);
            }
        });

        studentCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {// The Text boxes for Students will appear when check box is checked
            if (isChecked) {
                studentInfoLayout.setVisibility(View.VISIBLE);
                teacherInfoLayout.setVisibility(View.GONE); //Hide teacher fields
                teacherCheckBox.setChecked(false);
            } else {
                studentInfoLayout.setVisibility(View.GONE);
            }
        });
    }
}
