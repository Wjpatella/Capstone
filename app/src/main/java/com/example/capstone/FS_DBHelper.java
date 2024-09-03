package com.example.capstone;

import static androidx.core.content.ContentProviderCompat.requireContext;
import static java.security.AccessController.getContext;

import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FS_DBHelper {

    public static String Online_user_id=null;

    public static Boolean Teacher_online=false;

    public static Boolean Student_online=false;

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

    public static void checkUsers(String username, String password, OnUserCheckCompleteListener listener){//checks for users in the database
        FirebaseFirestore cloud_fs_db = FirebaseFirestore.getInstance();
        if (!username.isEmpty() && !password.isEmpty()) {//makes sure both are filled with a string
            cloud_fs_db.collection("teachers")
                    .whereEqualTo("name", username)
                    .whereEqualTo("teacher_password", password)
                    .get()
                    .addOnCompleteListener(task_find_teacher -> {
                if (task_find_teacher.isSuccessful() && !task_find_teacher.getResult().isEmpty()) {

                    Online_user_id = task_find_teacher.getResult().getDocuments().get(0).getId();//fethces the id of the teacher
                    Teacher_online=true;
                    listener.onSuccess();
                    //teacher found

                }
                else {
                    //check students collection if the teacher is not found
                    cloud_fs_db.collection("students")
                            .whereEqualTo("name", username)
                            .whereEqualTo("student_password", password)
                            .get()
                            .addOnCompleteListener(task_find_students -> {
                                if (task_find_students.isSuccessful() && !task_find_students.getResult().isEmpty()) {

                                    Online_user_id = task_find_students.getResult().getDocuments().get(0).getId();//fethces the id of the student
                                    Student_online=true;

                                    listener.onSuccess();
                                    //student found
                                } else {
                                    listener.onFailure();
                                    //Neither teacher or student found
                                }
                            });

                }

                });

        }
        else {
            //username or password was empty
            listener.onFailure();
        }
    }

    public interface OnUserCheckCompleteListener {//informs caller of success in task or not. Works with checkUser
        void onSuccess();
        void onFailure();
    }


    public interface FetchTeacherDataCallback {
        void onDataFetched(String teacherName);//add more data here later
    }

    public static void fetchTeacherData(FetchTeacherDataCallback fetch_teacher_data_callback){
        FirebaseFirestore cloud_fs_db = FirebaseFirestore.getInstance();
        cloud_fs_db.collection("teachers").document(Online_user_id).get().addOnCompleteListener(task_fetch_teacher_data ->{//fetch teacher based off id
            if(task_fetch_teacher_data.isSuccessful()){
                DocumentSnapshot teacher_id = task_fetch_teacher_data.getResult();
                if (teacher_id != null) {
                    String teacher_name = teacher_id.getString("name");
                    //String teacher_email = teacher_id.getString("email");

                    // Trigger the callback with values
                    if (fetch_teacher_data_callback != null) {
                        fetch_teacher_data_callback.onDataFetched(teacher_name);//add more here later;
                    }
                }
            } else {
                // Handle error or return some default value
                if (fetch_teacher_data_callback != null) {
                    fetch_teacher_data_callback.onDataFetched(null);//add more here later;
                }
            }
        });
    }

    public interface FetchStudentDataCallback {
        void onDataFetched(String studentName);//add more data here later
    }

    public static void fetchStudentData(FetchStudentDataCallback fetch_student_data_callback){
        FirebaseFirestore cloud_fs_db = FirebaseFirestore.getInstance();
        cloud_fs_db.collection("students").document(Online_user_id).get().addOnCompleteListener(task_fetch_student_data ->{//fetch student based off id
            if(task_fetch_student_data.isSuccessful()){
                DocumentSnapshot student_id = task_fetch_student_data.getResult();
                if (student_id != null) {
                    String student_name = student_id.getString("name");
                    //String

                    //Trigger the callback with values
                    if (fetch_student_data_callback != null) {
                        fetch_student_data_callback.onDataFetched(student_name);//add more here later;//sends back student name
                    }
                }
            } else {
                //Handle error or return some default value
                if (fetch_student_data_callback != null) {
                    fetch_student_data_callback.onDataFetched(null);//add more here later;
                }
            }
        });
    }

    public static void fetchStudentData(){
        FirebaseFirestore cloud_fs_db = FirebaseFirestore.getInstance();
    }

    /*
    public static void updateTeacherList(){
        private ArrayList<String> teacherList;
    }
    *
     */


}


