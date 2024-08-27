package com.example.capstone;

import static androidx.core.content.ContentProviderCompat.requireContext;
import static java.security.AccessController.getContext;

import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FS_DBHelper {

    private  FirebaseFirestore cloud_fs_db;//declare firebase firestore

    public FS_DBHelper() {
        cloud_fs_db = FirebaseFirestore.getInstance();
    }

    public FirebaseFirestore getDb() {
        return cloud_fs_db;
    }

    //For help Adding teachers to database
    public static void addTeacher(String name, String teacher_password, String user_email, Map<String, ArrayList<String>> classes, String activeGame) {
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

    //For help Adding students to database
    public static void addStudent(String name, String student_password, String teacher, String class_id, String activeGame) {
        Map<String, Object> student = new HashMap<>();
        //Columns
        student.put("name", name);
        student.put("student_password", student_password);
        student.put("teacher", teacher);
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

    public static void fetchTeachers(OnCompleteListener<QuerySnapshot> onCompleteListener){ //when complete the Teachers from the querysnapshot can be acquired
        FirebaseFirestore cloud_fs_db = FirebaseFirestore.getInstance();
        cloud_fs_db.collection("teachers").get().addOnCompleteListener(onCompleteListener);
    }

    /*
    public static void updateTeacherList(){
        private ArrayList<String> teacherList;
    }
    *
     */


}


