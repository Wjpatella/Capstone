package com.example.capstone;

import static com.example.capstone.FS_DBHelper.Online_user_id;
import static com.example.capstone.FS_DBHelper.Student_online;

import com.google.firebase.database.FirebaseDatabase;

import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;


public class StudentGameFragment extends Fragment {

    private Button JoinGameButton;
    private View gameStatusIndicator;
    private String studentsTeacher;
    private String studentGameStatus;
    private ListenerRegistration gameStatusListener;

    private ListenerRegistration gameExistenceListener;

    private boolean gameInFireStore = false;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.student_game_fragment, container, false);
        Student_online=true;
        JoinGameButton = view.findViewById(R.id.join_game_button);
        gameStatusIndicator = view.findViewById(R.id.game_status_indicator);
        //updateIndicatorColor(studentGameStatus);// Initialize gameStatusIndicator
        updateIndicatorColor(null);
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
                                Log.e("StudentGameFragment", "Teacher "+ studentsTeacher +" found for "+ Online_user_id);
                                checkGameStatus(studentsTeacher);
                            } else {
                                Log.e("StudentGameFragment", "Student document not found");
                            }
                        }
                    })
                    .addOnFailureListener(e -> Log.e("StudentGameFragment", "Error loading student data", e));
        }
    }

    private void checkGameStatus(String studentsTeacher) {
        if (studentsTeacher != null && Online_user_id != null) {
            //Stop any existing listener to prevent duplicate listeners
            if (gameStatusListener != null) {
                gameStatusListener.remove();
            }

            gameStatusListener = FirebaseFirestore.getInstance()
                    .collection("students")
                    .document(Online_user_id)
                    .addSnapshotListener((documentSnapshot, e) -> {
                        if (e != null) {
                            Log.e("StudentGameFragment", "Listen failed.", e);
                            return;
                        }

                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            Student student = documentSnapshot.toObject(Student.class);
                            if (student != null) {
                                studentGameStatus = student.getActiveGame();

                                if (studentGameStatus != null) {
                                    checkGameInFirestore();
                                } else {
                                    gameInFireStore = false;
                                    updateIndicatorColor(null); // Set to red if no game is active
                                }
                            }
                        }
                    });
        } else {
            Toast.makeText(getActivity(), "You do not have a Teacher", Toast.LENGTH_SHORT).show();
            Toast.makeText(getActivity(), "あなたには先生がいない。", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkGameInFirestore() {

        if (studentGameStatus == null) {
            Log.e("StudentGameFragment", "No game status found for student.");
            updateIndicatorColor(null); //Set indicator to red if no game status
            return;
        }

        //Remove any existing listener to prevent duplicate listeners
        if (gameExistenceListener != null) {
            gameExistenceListener.remove();
        }

        //Set up the real time listener for the game document
        DocumentReference gameRef = FirebaseFirestore.getInstance().collection("games").document(studentGameStatus);
        gameExistenceListener = gameRef.addSnapshotListener((gameSnapshot, e) -> {
            if (e != null) {
                Log.e("StudentGameFragment", "Error listening to game document", e);
                return;
            }

            //Check if the game document exists and update the color indicator
            gameInFireStore = gameSnapshot != null && gameSnapshot.exists();
            updateIndicatorColor(studentGameStatus);
        });
    }



    private void updateIndicatorColor(String studentGameStatus) {
        Drawable drawable = gameStatusIndicator.getBackground();
        drawable = DrawableCompat.wrap(drawable);

        if (studentGameStatus != null && gameInFireStore) {
            DrawableCompat.setTint(drawable, getResources().getColor(R.color.green));//Game active
        } else {
            DrawableCompat.setTint(drawable, getResources().getColor(R.color.red));//Game inactive
        }
    }

    public void goto_DrawlingGameActivity() {
        if (studentGameStatus != null && gameInFireStore==true) {
            Log.d("StudentGameFragment", "studentGameStatus: " + studentGameStatus);
            Intent intent = new Intent(getActivity(), DrawingGameActivity.class);
            intent.putExtra("studentGameStatus", studentGameStatus);
            startActivity(intent);
        }
        else {
            Toast.makeText(getActivity(), "No active game found.", Toast.LENGTH_SHORT).show();
            Toast.makeText(getActivity(), "アクティブな試合は見つかりませんでした。", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        //Start the listener to check game status when fragment becomes visible
        if (studentsTeacher != null) {
            checkGameStatus(studentsTeacher);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        //Remove the listener when fragment is no longer visible
        if (gameStatusListener != null) {
            gameStatusListener.remove();
            gameStatusListener = null;
        }
        if (gameExistenceListener != null) {
            gameExistenceListener.remove();
            gameExistenceListener = null;
        }
    }


}
