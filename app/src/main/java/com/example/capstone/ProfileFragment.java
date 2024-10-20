package com.example.capstone;

import static com.example.capstone.FS_DBHelper.Online_user_id;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProfileFragment extends Fragment {

        private TextView teacherName, lastscoreTextView, studentName, leaderboardTitle, class_title, teacherTextView;
        private ImageView teacher_profileImage;

        private ImageView student_profileImage;

        private FirebaseFirestore firestore;

        private Spinner teacherLeaderBoardClassSpinner;
        private TableLayout leaderboardTable;


        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
            View view = inflater.inflate(R.layout.fragment_profile, container, false); //inflate the layout

            //Initialize Firestore
            firestore = FirebaseFirestore.getInstance();

            //Initialize the TextViews
            teacherName = view.findViewById(R.id.teacher_viewtext);
            studentName = view.findViewById(R.id.student_viewtext);
            teacher_profileImage = view.findViewById(R.id.teacher_profile_image);
            student_profileImage = view.findViewById(R.id.student_profile_image);
            lastscoreTextView = view.findViewById(R.id.textViewLastScore);
            teacherLeaderBoardClassSpinner = view.findViewById(R.id.teacher_class_spinner);
            leaderboardTitle = view.findViewById(R.id.leaderboard_name_title);
            class_title = view.findViewById(R.id.select_class_title);
            teacherTextView = view.findViewById(R.id.teacherTextView);
            leaderboardTable = view.findViewById(R.id.leaderboard_table);

            if (FS_DBHelper.Teacher_online==true) {
                teacherTextView.setVisibility(View.GONE);
                teacherLeaderBoardClassSpinner.setVisibility(View.VISIBLE);
                class_title.setVisibility(View.VISIBLE);
                student_profileImage.setVisibility(View.GONE);
                studentName.setVisibility(View.GONE);
                lastscoreTextView.setVisibility(View.GONE);
                teacherName.setVisibility(View.VISIBLE);
                teacher_profileImage.setVisibility(View.VISIBLE);

                setupLeaderboardClassSpinner(Online_user_id);




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
                teacherLeaderBoardClassSpinner.setVisibility(View.GONE);
                class_title.setVisibility(View.GONE);
                teacherName.setVisibility(View.GONE);
                teacher_profileImage.setVisibility(View.GONE);
                teacherTextView.setVisibility(View.VISIBLE);
                studentName.setVisibility(View.VISIBLE);
                lastscoreTextView.setVisibility(View.VISIBLE);
                student_profileImage.setVisibility(View.VISIBLE);
                setupLeaderboardTable();

                // Fetch the student's data and update the TextView when data is available
                FS_DBHelper.fetchStudentData((student_name, teacherName) -> {
                    if (student_name != null) {
                        studentName.setText(student_name);
                        teacherTextView.setText("Teacher: " + teacherName);
                        getScore(student_name);

                        //Get the students class name and load the leaderboard for that class
                        getStudentsClassName(teacherName, className -> {
                            if (className != null) {
                                leaderboardTitle.setText("Leaderboard - " + className);
                                getStudentsForClass(teacherName, className); // Load the students and leaderboard for this class
                            }
                        });
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

    private void setupLeaderboardClassSpinner(String teacher_name) {
        // Fetch the list of classes for the teacher
        firestore.collection("teachers").document(teacher_name)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> teacherData = documentSnapshot.getData();
                        if (teacherData != null) {
                            Map<String, Object> classes = (Map<String, Object>) teacherData.get("classes");
                            if (classes != null) {
                                // Populate class spinner with class names
                                String[] classArray = classes.keySet().toArray(new String[0]);
                                ArrayAdapter<String> classAdapter = new ArrayAdapter<>(getContext(),
                                        android.R.layout.simple_spinner_item, classArray);
                                classAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                teacherLeaderBoardClassSpinner.setAdapter(classAdapter);


                                // Update leaderboard when a class is selected
                                teacherLeaderBoardClassSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                    @Override
                                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                        String selectedClass = classArray[position];
                                        leaderboardTitle.setText("Leaderboard - " + selectedClass);
                                        //loadLeaderboard(selectedClass);
                                        //setupLeaderboardTable();
                                        getStudentsForClass(teacher_name, selectedClass); // Fetch and display the leaderboard for the selected class
                                    }

                                    @Override
                                    public void onNothingSelected(AdapterView<?> parent) {
                                        // Handle case where no class is selected
                                    }
                                });
                            }
                        }
                    }
                });
    }



        private void setupLeaderboardTable() {
            Log.d("Leaderboard", "Setting up leaderboard table");

            TableRow headerRow = new TableRow(getContext());

            TextView positionHeader = new TextView(getContext());
            positionHeader.setText("Position");
            positionHeader.setPadding(16, 16, 16, 16);
            positionHeader.setTypeface(null, Typeface.BOLD); // Set Bold
            positionHeader.setPaintFlags(positionHeader.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG); // Set Underline

            TextView nameHeader = new TextView(getContext());
            nameHeader.setText("Name");
            nameHeader.setPadding(16, 16, 16, 16);
            nameHeader.setTypeface(null, Typeface.BOLD); //Set Bold
            nameHeader.setPaintFlags(nameHeader.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG); // Set Underline

            TextView scoreHeader = new TextView(getContext());
            scoreHeader.setText("Last Score");
            scoreHeader.setPadding(16, 16, 16, 16);
            scoreHeader.setTypeface(null, Typeface.BOLD); // Set Bold
            scoreHeader.setPaintFlags(scoreHeader.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG); // Set Underline

            // Add headers to the row
            headerRow.addView(positionHeader);
            headerRow.addView(nameHeader);
            headerRow.addView(scoreHeader);

            // Add the row to the table
            leaderboardTable.addView(headerRow);


        }

    private void getStudentsForClass(String teacher, String className) {//Gets the students in there class for the student
        Log.d("ProfileFragment", "teacher: " + teacher);
        Log.d("ProfileFragment", "Class Name: " + className);
        firestore.collection("teachers")
                .document(teacher)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> classesData = documentSnapshot.getData();
                        if (classesData != null && classesData.containsKey("classes")) {
                            Map<String, ArrayList<String>> currentClasses = (Map<String, ArrayList<String>>) classesData.get("classes");
                            ArrayList<String> studentNames = currentClasses.get(className);
                            Log.d("ProfileFragment", "Studnets in class: " + studentNames );
                            if (studentNames != null) {

                                getStudentsScores(studentNames);

                            }
                        } else {
                            Log.e("ProfileFragment", "No students found for class: " + className);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("ProfileFragment", "Error fetching student data", e));
    }




    interface OnClassNameRetrievedListener {
        void onClassNameRetrieved(String className);
    }

    private void getStudentsClassName(String teacherName, StudentClassroomFragment.OnClassNameRetrievedListener listener) {//Gets the class name for the student
        firestore.collection("teachers").document(teacherName)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, ArrayList<String>> classes = (Map<String, ArrayList<String>>) documentSnapshot.get("classes");//Get the classes data
                        if (classes != null) {
                            for (Map.Entry<String, ArrayList<String>> entry : classes.entrySet()) {
                                if (entry.getValue().contains(Online_user_id)) {
                                    listener.onClassNameRetrieved(entry.getKey());

                                    //getStudentsForClass(teacherName, entry.getKey());
                                    return;
                                }
                            }
                        }
                    }
                    listener.onClassNameRetrieved(null);
                })
                .addOnFailureListener(e -> Log.e("StudentClassroomFragment", "Error getting teacher data in method getsStudentsClassName", e));
    }

    //Get the scores of all the studnets in the class
    private void getStudentsScores(ArrayList<String> studentNames) {
        List<Integer> scores = new ArrayList<>();
        List<String> students = new ArrayList<>(studentNames); //avoid modifying the original list

        for (String studentId : studentNames) {
            firestore.collection("students")
                    .document(studentId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Long scoreValue = documentSnapshot.getLong("score");
                            Integer score = (scoreValue != null) ? scoreValue.intValue() : 0;
                            scores.add(score);
                        } else {
                            Log.e("getStudentsScores", "Document for student " + studentId + " does not exist.");
                            scores.add(0); // Set score to 0 if the document does not exist
                        }

                        // After fetching all the scores, update the leaderboard
                        if (scores.size() == studentNames.size()) {
                            setPositionsForStudents(students, scores); // Pass students and their scores to set positions
                        }
                    })
                    .addOnFailureListener(e -> Log.e("FirestoreError", "Error getting score for student " + studentId, e));
        }
    }

        private void setPositionsForStudents(List<String> students, List<Integer> scores) {
            // Create a list of student-score pairs
            List<Pair<String, Integer>> studentScores = new ArrayList<>();// Pair is a class that holds two values
            for (int i = 0; i < students.size(); i++) {
                studentScores.add(new Pair<>(students.get(i), scores.get(i)));
            }

            // Sort the list based on scores in descending order. Compare scores to find positions.
            studentScores.sort((position1, position2) -> position2.second.compareTo(position1.second));

            // Clear the leaderboard table before adding new rows
            leaderboardTable.removeAllViews();


            setupLeaderboardTable();

            // Populate the leaderboard table with ranked students. Highest to lowest scores
            for (int i = 0; i < studentScores.size(); i++) {
                Pair<String, Integer> studentScore = studentScores.get(i);

                TableRow row = new TableRow(getContext());

                TextView position = new TextView(getContext());
                position.setText(String.valueOf(i + 1)); // Set position
                position.setPadding(16, 16, 16, 16);

                TextView name = new TextView(getContext());
                name.setText(studentScore.first); // Student name
                name.setPadding(16, 16, 16, 16);

                TextView score = new TextView(getContext());
                score.setText(String.valueOf(studentScore.second)); // Student score
                score.setPadding(16, 16, 16, 16);

                row.addView(position);
                row.addView(name);
                row.addView(score);

                leaderboardTable.addView(row);
            }
        }






        private void getScore(String studentId) {//students(collection)->gusserId(document)->score(int)
            firestore.collection("students").document(studentId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            //Check if the score field exists
                            if (documentSnapshot.contains("score")) {
                                //Retrieve the score value
                                Integer score = documentSnapshot.getLong("score").intValue(); // Get as long then convert to int
                                lastscoreTextView.setText("Previous Score: " + score);

                                Log.d("Score", "Score for guesser " + studentId + ": " + score);


                            } else {
                                Log.d("Score", "Score field does not exist for guesser: " + studentId);
                            }
                        } else {
                            Log.d("Score", "Document does not exist for guesser: " + studentId);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("FirestoreError", "Error getting score for guesser: ", e);
                    });
        }

    }