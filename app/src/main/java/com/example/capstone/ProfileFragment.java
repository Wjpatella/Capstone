package com.example.capstone;
import android.media.Image;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
public class ProfileFragment extends Fragment {

    private TextView teacherName;
    private TextView studentName;
    private ImageView teacher_profileImage;

    private ImageView student_profileImage;


    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_profile, container, false); //inflate the layout

        //Initialize the TextViews
        teacherName = view.findViewById(R.id.teacher_viewtext);
        studentName = view.findViewById(R.id.student_viewtext);
        teacher_profileImage = view.findViewById(R.id.teacher_profile_image);
        student_profileImage = view.findViewById(R.id.student_profile_image);


        if (FS_DBHelper.Teacher_online==true) {
            student_profileImage.setVisibility(View.GONE);
            studentName.setVisibility(View.GONE);
            teacherName.setVisibility(View.VISIBLE);
            teacher_profileImage.setVisibility(View.VISIBLE);


            //Fetch the teacher's data and update the TextViews when data is available
            FS_DBHelper.fetchTeacherData((teacher_name) -> {//add more data in () later
                if (teacher_name != null) {
                    teacherName.setText(teacher_name);
                }
            /*if (teacher_email != null) {
                teacherEmail.setText(teacher_email);
            }
             */
            });
        } else if (FS_DBHelper.Student_online==true) {
            teacherName.setVisibility(View.GONE);
            teacher_profileImage.setVisibility(View.GONE);
            studentName.setVisibility(View.VISIBLE);
            student_profileImage.setVisibility(View.VISIBLE);

            // Fetch the student's data and update the TextView when data is available
            FS_DBHelper.fetchStudentData((student_name) -> {//add more data in () later
                if (student_name != null) {
                    studentName.setText(student_name);
                }
            /*if (teacher_email != null) {
                teacherEmail.setText(teacher_email);
            }
             */
            });

        }
        else {
            Toast.makeText(ProfileFragment.this.getContext(), "Online status not found", Toast.LENGTH_SHORT).show();
        }
        return view;

    }
}