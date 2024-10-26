package com.example.capstone;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import java.util.ArrayList;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;



import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;


public class ActivityRegister extends MainActivity {

    private FirebaseFirestore cloud_fs_db; //Firestore firebase database

    private Spinner teachersSpinner;
    private ArrayList<String> teacherList; //Replace with actual teacher data
    //Teacher Data
    private EditText teacher_Name;
    private EditText teacher_Password;

    //Teacher Data

    //Student Data
    private EditText student_info_layout;
    private EditText student_Name;
    private EditText student_Password;

    private TextView select_teacher_title;
    //Student Data

    private LinearLayout teacherInfoLayout;
    private LinearLayout studentInfoLayout;
    private CheckBox teacherCheckBox;
    private CheckBox studentCheckBox;

    private Button CreatAccountButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        cloud_fs_db = FirebaseFirestore.getInstance();//Initialize firebase

        /*Checkboxes for teachers and students*/
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
        /*Checkboxes for teachers and students*/

        CreatAccountButton = findViewById(R.id.Create_account_button);

        teacherInfoLayout = findViewById(R.id.Teacher_info_layout);
        studentInfoLayout = findViewById(R.id.Student_info_layout);

        teacher_Name = findViewById(R.id.Teacher_name);
        teacher_Password = findViewById(R.id.Teacher_password);


        student_Name = findViewById(R.id.Student_name);
        student_Password = findViewById(R.id.Student_password);
        select_teacher_title = findViewById(R.id.select_teacher_textView2);

        teachersSpinner = findViewById(R.id.teachersSpinner);

        teacherList = new ArrayList<>();

        //Fetch teacher names from Firestore and update teacherList
        FS_DBHelper.fetchTeachers(task_teachers_names -> {
            if (task_teachers_names.isSuccessful()) {
                for (DocumentSnapshot document : task_teachers_names.getResult()) {
                    String t_name = document.getString("name");
                    teacherList.add(t_name);
                }
                //Update array adapter with the the data
                ArrayAdapter<String> adapter = new ArrayAdapter<>(ActivityRegister.this, android.R.layout.simple_dropdown_item_1line, teacherList);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                teachersSpinner.setAdapter(adapter);
            } else {
                Toast.makeText(ActivityRegister.this, "Failed to fetch teachers", Toast.LENGTH_SHORT).show();
            }
        });

        CreatAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (teacherCheckBox.isChecked() && !teacher_Name.getText().toString().isEmpty() && !teacher_Password.getText().toString().isEmpty() ) {
                    String teacherName = teacher_Name.getText().toString();

                    cloud_fs_db.collection("teachers").document(teacherName).get().addOnCompleteListener(task_check_if_Tname_taken ->{//check if the teacher name is already taken
                        DocumentSnapshot document = task_check_if_Tname_taken.getResult();
                        if (task_check_if_Tname_taken.isSuccessful() && document.exists()){
                                Toast.makeText(ActivityRegister.this, "Teacher name already taken", Toast.LENGTH_SHORT).show();
                        }
                        else {
                            
                            String teacherPassword = teacher_Password.getText().toString();


                            FS_DBHelper.addTeacher(teacherName, teacherPassword, null, null,null); //Calls the FS_DBHelper to add the teacher to the Cloudstore firebase
                            goto_login_activity();
                        }
                    });

                }

                else if (studentCheckBox.isChecked() && !student_Name.getText().toString().isEmpty() && !student_Password.getText().toString().isEmpty() && teachersSpinner.getSelectedItem() != null) {
                    String studentName = student_Name.getText().toString();

                    cloud_fs_db.collection("students").whereEqualTo("name", studentName).get().addOnCompleteListener(task_check_if_Sname_taken -> {//check if the student name is already taken
                        QuerySnapshot querySnapshot= task_check_if_Sname_taken.getResult();
                        if (task_check_if_Sname_taken.isSuccessful() && !querySnapshot.isEmpty()) {
                            Toast.makeText(ActivityRegister.this, "Student name already taken", Toast.LENGTH_SHORT).show();
                        } else {

                            String studentPassword = student_Password.getText().toString();
                            //Extract selected teachers from MultiAutoCompleteTextView
                            String selectedTeacher = teachersSpinner.getSelectedItem().toString();

                            FS_DBHelper.addStudent(studentName, studentPassword, selectedTeacher, 0, null); //Calls the FS_DBHelper to add the student to the Cloudstore firebase
                            goto_login_activity();
                        }
                    });
                }
                else {
                    Toast.makeText(ActivityRegister.this, "You must select a checkbox and fill in all the required information.", Toast.LENGTH_SHORT).show();
                }
            }
         });

        }

        public void goto_main_activity(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        }
        public void goto_login_activity() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

}

