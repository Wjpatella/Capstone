package com.example.capstone;

import static com.example.capstone.FS_DBHelper.Online_user_id;
import static com.example.capstone.FS_DBHelper.Student_online;

import com.google.firebase.database.FirebaseDatabase;
import androidx.fragment.app.Fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;


public class StudentGameFragment extends Fragment {

    private Button JoinGameButton;
    private View gameStatusIndicator;
    private String studentsTeacher;
    private String studentGameStatus;
    private FirebaseDatabase realtimeDb;


    private boolean gameInFireStore = false;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.student_game_fragment, container, false);
        Student_online=true;
        JoinGameButton = view.findViewById(R.id.join_game_button);
        gameStatusIndicator = view.findViewById(R.id.game_status_indicator);
        updateIndicatorColor(studentGameStatus);// Initialize gameStatusIndicator
        getStudentsTeacher();

        JoinGameButton.setOnClickListener(v -> goto_DrawlingGameActivity());

        return view;
    }

    private void getStudentsTeacher() {//Check if this student has a teacher
        if (Online_user_id != null) {
            FirebaseFirestore.getInstance().collection("students").document(Online_user_id)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Student student = documentSnapshot.toObject(Student.class);
                            if (student != null) {
                                studentsTeacher = student.getTeacher();
                                Log.e("StudentClassroomFragment", "Teacher "+ studentsTeacher +" found for "+ Online_user_id);
                                checkGameStatus(studentsTeacher);
                            } else {
                                Log.e("StudentClassroomFragment", "Student document not found");
                            }
                        }
                    })
                    .addOnFailureListener(e -> Log.e("StudentClassroomFragment", "Error loading student data", e));
        }
    }

    private void checkGameStatus(String studentsTeacher) {//Check if the student has an active game (in student collection and then checks games collection)
        if (studentsTeacher != null) {
            FirebaseFirestore.getInstance().collection("students").document(Online_user_id)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Student student = documentSnapshot.toObject(Student.class);
                            if (student != null) {
                                this.studentGameStatus = student.getActiveGame(); // Get student's active game ID
                                if (studentGameStatus != null) {
                                    // Check if the game with studentGameStatus exists in the "games" collection in Firestore
                                    FirebaseFirestore.getInstance().collection("games").document(studentGameStatus)
                                            .get()
                                            .addOnSuccessListener(gameSnapshot -> {
                                                if (gameSnapshot.exists()) {
                                                    // Game ID exists in the "games" collection
                                                    gameInFireStore = true;
                                                    updateIndicatorColor(studentGameStatus); // Update indicator based on game status
                                                    Log.e("StudentClassroomFragment", "Active game found for student: " + Online_user_id);
                                                } else {
                                                    // No game with this ID found in the "games" collection
                                                    Log.e("StudentClassroomFragment", "No active game found in Firestore for game ID: " + studentGameStatus);
                                                }
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e("StudentClassroomFragment", "Error checking Firestore: " + e.getMessage());
                                            });
                                } else {
                                    Log.e("StudentClassroomFragment", "No active game found for student: " + Online_user_id);
                                }
                            } else {
                                Log.e("StudentClassroomFragment", "Student document not found");
                            }
                        } else {
                            Log.e("StudentClassroomFragment", "Snapshot failed");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("StudentClassroomFragment", "Error fetching student: " + e.getMessage());
                    });
        } else {
            Toast.makeText(getActivity(), "You do not have a Teacher", Toast.LENGTH_SHORT).show();
        }
    }



    private void updateIndicatorColor(String studentGameStatus) {
        if (studentGameStatus != null && gameInFireStore==true) {
            gameStatusIndicator.setBackgroundColor(getResources().getColor(R.color.green));
        } else {
            gameStatusIndicator.setBackgroundColor(getResources().getColor(R.color.red));
        }
    }

    public void goto_DrawlingGameActivity() {
        if (studentGameStatus != null && gameInFireStore==true) {
            Log.d("StudentGameFragment", "studentGameStatus: " + studentGameStatus); // Debugging
            Intent intent = new Intent(getActivity(), DrawingGameActivity.class);
            intent.putExtra("studentGameStatus", studentGameStatus);
            startActivity(intent);
        }
        else {
            Toast.makeText(getActivity(), "No active game found", Toast.LENGTH_SHORT).show();
        }
    }


}
