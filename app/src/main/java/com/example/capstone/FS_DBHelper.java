package com.example.capstone;

import static androidx.core.content.ContentProviderCompat.requireContext;
import static java.security.AccessController.getContext;

import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.Map;

public class FS_DBHelper {

    public static String Online_user_id=null;

    public static Boolean Teacher_online=false;

    public static Boolean Student_online=false;

    private  FirebaseFirestore cloud_fs_db;//declare firebase firestore

    public FS_DBHelper(FirebaseFirestore cloud_fs_db) {
        this.cloud_fs_db = cloud_fs_db;
    }

    public FirebaseFirestore getDb() {
        return cloud_fs_db;
    }

    //For help Adding teachers to database
    public static void addTeacher(String name, String teacher_password, Map<String, ArrayList<String>> classes, String activeGame, Map<String, ArrayList<String>> class_teams) {
        Map<String, Object> teacher = new HashMap<>();
        //Columns
        teacher.put("name", name);
        teacher.put("teacher_password", teacher_password);
        teacher.put("classes", classes);
        teacher.put("activeGame", activeGame);
        teacher.put("class_teams", class_teams);

        FirebaseFirestore cloud_fs_db = FirebaseFirestore.getInstance();
        cloud_fs_db.collection("teachers")
                .document(name)
                .set(teacher)
                .addOnSuccessListener(aVoid -> {
                    //Successfully added teacher
                })
                .addOnFailureListener(e -> {
                    //Failed to add teacher
                });
    }

    //For help Adding students to database
    public static void addStudent(String name, String student_password, String teacher, int score, String activeGame) {
        Map<String, Object> student = new HashMap<>();
        //Columns
        student.put("name", name);
        student.put("student_password", student_password);
        student.put("teacher", teacher);
        student.put("score", score);
        student.put("activeGame", activeGame);


        FirebaseFirestore cloud_fs_db = FirebaseFirestore.getInstance();
        cloud_fs_db.collection("students")
                .document(name)//students id's are their names
                .set(student)
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
        void onDataFetched(String studentName, String teacherName); // Add teacherName to the callback
    }

    public static void fetchStudentData(FetchStudentDataCallback fetch_student_data_callback) {
        FirebaseFirestore cloud_fs_db = FirebaseFirestore.getInstance();

        // Fetch student document using Online_user_id
        cloud_fs_db.collection("students").document(Online_user_id).get().addOnCompleteListener(task_fetch_student_data -> {
            if (task_fetch_student_data.isSuccessful()) {
                DocumentSnapshot student_id = task_fetch_student_data.getResult();
                if (student_id != null) {
                    String student_name = student_id.getString("name");
                    String teacher_name = student_id.getString("teacher"); // Assuming teacher_id directly stores the teacher's name

                    // Trigger the callback with both student name and teacher name
                    if (fetch_student_data_callback != null) {
                        fetch_student_data_callback.onDataFetched(student_name, teacher_name);
                    }
                }
            } else {
                // Handle error or return default value in case of failure
                if (fetch_student_data_callback != null) {
                    fetch_student_data_callback.onDataFetched(null, null);
                }
            }
        });
    }
/*
    public static void fetchStudentData(){
        FirebaseFirestore cloud_fs_db = FirebaseFirestore.getInstance();
    }

    // Fetch all classes
    public interface FetchStudentsCallback {
        void onStudentsFetched(List<Student> students);
    }


    public static void updateTeacherClasses(String teacherId, Map<String, ArrayList<String>> updatedClasses) {
        FirebaseFirestore cloud_fs_db = FirebaseFirestore.getInstance();
        cloud_fs_db.collection("teachers").document(teacherId)
                .update("classes", updatedClasses)
                .addOnSuccessListener(aVoid -> {
                    // Successfully updated classes
                })
                .addOnFailureListener(e -> {
                    // Failed to update classes
                });
    }

     */

    public void getTeacherClasses(String teacherId, Consumer<List<String>> callback) {
        cloud_fs_db.collection("teachers").document(teacherId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null) {
                            // Get the classes map from the document
                            Map<String, ArrayList<String>> classesMap = (Map<String, ArrayList<String>>) document.get("classes");

                            // Check if classesMap is null before accessing keySet()
                            if (classesMap != null && !classesMap.isEmpty()) {
                                List<String> classes = new ArrayList<>(classesMap.keySet());
                                callback.accept(classes);
                            } else {
                                // Handle no classes scenario
                                callback.accept(null); // Notify that there are no classes
                            }
                        } else {
                            // Handle no document scenario
                            callback.accept(null); // Notify that there is no document
                        }
                    } else {
                        // Handle error
                        callback.accept(null); // Notify that there is an error
                    }
                });
    }

    /*
    public void getStudentsForClass(String className, FetchStudentsCallback callback) {
        cloud_fs_db.collection("students")
                .whereEqualTo("class_id", className)
                .get()
                .addOnCompleteListener(task -> {
                    List<Student> students = new ArrayList<>();
                    if (task.isSuccessful()) {
                        QuerySnapshot result = task.getResult();
                        if (result != null) {
                            for (QueryDocumentSnapshot document : result) {
                                String name = document.getString("name");
                                // Add more fields as necessary

                                students.add(new Student(name));
                            }
                        }
                    }
                    if (callback != null) {
                        callback.onStudentsFetched(students);
                    }
                });
    }



    public interface FetchClassesCallback {
        void onClassesFetched(List<String> classes);
    }


    public void getClasses(FetchClassesCallback callback) {
        cloud_fs_db.collection("classes")
                .get()
                .addOnCompleteListener(task -> {
                    List<String> classes = new ArrayList<>();
                    if (task.isSuccessful()) {
                        QuerySnapshot result = task.getResult();
                        if (result != null) {
                            for (QueryDocumentSnapshot document : result) {
                                String className = document.getString("class_id");
                                classes.add(className);
                            }
                        }
                    }
                    if (callback != null) {
                        callback.onClassesFetched(classes);
                    }
                });
    }
    *
     */


    /*
    public static void updateTeacherList(){
        private ArrayList<String> teacherList;
    }
    *
     */
// Fetch students for a particular class
    /*
    public void getStudentsForClass(String className, Consumer<List<Student>> callback) {
        cloud_fs_db.collection("classes").document(className).collection("students")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Student> studentList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String name = document.getString("name"); // Assuming student name field is "name"
                            studentList.add(new Student(name)); // Assuming you have a Student model
                        }
                        callback.accept(studentList);
                    }
                });
    }

     */



    // Create new classroom
    /*
    public void createClassroom(String className, List<Student> selectedStudents, Consumer<Boolean> callback) {
        cloud_fs_db.collection("classes").document(className).set(new Classroom(className, selectedStudents))
                .addOnSuccessListener(aVoid -> callback.accept(true))
                .addOnFailureListener(e -> callback.accept(false));
    }
     */

    // Create new team
    public void createTeam(String class_name, String teamName, List<Student> selectedStudents, Consumer<Boolean> callback) {
        DocumentReference teamRef = cloud_fs_db.collection("teachers").document(Online_user_id);

        //Extract student names and create maps for each student
        List<Map<String, String>> studentNameMaps = new ArrayList<>();
        for (Student student : selectedStudents) {
            Map<String, String> studentNameMap = new HashMap<>();
            studentNameMap.put("name", student.getName());
            studentNameMaps.add(studentNameMap);
        }

        //Create a nested map to update the specific team
        Map<String, Object> updates = new HashMap<>();
        updates.put("class_teams." + class_name + ".teams." + teamName, studentNameMaps);

        teamRef.update(updates)
                .addOnSuccessListener(aVoid -> callback.accept(true))
                .addOnFailureListener(e -> callback.accept(false));
    }

}


