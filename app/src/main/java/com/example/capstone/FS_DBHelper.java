package com.example.capstone;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FS_DBHelper {

    private FirebaseFirestore cloud_fs_db;//declare firebase firestore

    public FS_DBHelper() {
        cloud_fs_db = FirebaseFirestore.getInstance();
    }

    public FirebaseFirestore getDb() {
        return cloud_fs_db;
    }

    //For help Adding teachers to database
    public void addTeacher(String name, String teacher_password, String user_email, Map<String, ArrayList<String>> classes, String activeGame) {
        Map<String, Object> teacher = new HashMap<>();
        //Columns
        teacher.put("name", name);
        teacher.put("teacher_password", teacher_password);
        teacher.put("user_email", user_email);
        teacher.put("classes", classes);
        teacher.put("activeGame", activeGame);

        FirebaseFirestore cloud_fs_db = FirebaseFirestore.getInstance();
        cloud_fs_db.collection("teachers")
                .add(teacher)
                .addOnSuccessListener(documentReference -> {
                    //Successfully added teacher
                })
                .addOnFailureListener(e -> {
                    //Failed to add teacher
                });
    }

    public void addStudent(String name, String student_password, String phone_num, String grade, String teachers, String class_id, String activeGame) {
        Map<String, Object> student = new HashMap<>();
        //Columns
        student.put("name", name);
        student.put("student_password", student_password);
        student.put("phone_num", phone_num);
        student.put("grade", grade);
        student.put("teachers", teachers);
        student.put("class_id", class_id);
        student.put("activeGame", activeGame);


        FirebaseFirestore cloud_fs_db = FirebaseFirestore.getInstance();
        cloud_fs_db.collection("students")
                .add(student)
                .addOnSuccessListener(documentReference -> {
                    //Successfully added student
                })
                .addOnFailureListener(e -> {
                    //Failed to add student
                });
    }


}


