
//May need a page refresh button *************************
package com.example.capstone;

import static com.example.capstone.FS_DBHelper.Online_user_id;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StudentClassroomFragment extends Fragment {

    private TextView teacherTextView;
    private TextView classNameTextView;
    private RecyclerView studentGrid;
    private RecyclerView teamsGrid;
    private List<String> studentList;
    private List<Team> teamsList;

    private FirebaseFirestore firestore;
    private StudentAdapter studentAdapter;
    private TeamAdapter teamAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student_classroom, container, false);

        firestore = FirebaseFirestore.getInstance();

        // Initialize views
        teacherTextView = view.findViewById(R.id.teacher_text);
        classNameTextView = view.findViewById(R.id.class_name_text);
        studentGrid = view.findViewById(R.id.student_grid);
        teamsGrid = view.findViewById(R.id.teams_grid);

        // Initialize studentList and teamsList
        studentList = new ArrayList<>();
        teamsList = new ArrayList<>();

        // Set up RecyclerViews
        studentGrid.setLayoutManager(new GridLayoutManager(getContext(), 3));
        studentAdapter = new StudentAdapter(studentList);
        studentGrid.setAdapter(studentAdapter);

        teamsGrid.setLayoutManager(new GridLayoutManager(getContext(), 1));
        teamAdapter = new TeamAdapter(teamsList);
        teamsGrid.setAdapter(teamAdapter);

        // Load classroom data
        loadClassroomData();

        return view;
    }

    private void loadClassroomData() {
        if (Online_user_id != null) {
            firestore.collection("students").document(Online_user_id)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Student student = documentSnapshot.toObject(Student.class);
                            if (student != null) {
                                // Update teacher and class name
                                String studentsTeacher = student.getTeacher();
                                teacherTextView.setText(studentsTeacher);

                                getStudentsClassName(studentsTeacher, className -> {
                                    if (className != null) {
                                        Log.d("$$$$$$$$$$$$StudentClassroomFragment", "Class Name: " + className);
                                        classNameTextView.setText(className);
                                        loadStudentsForClass(studentsTeacher, className);
                                        getTeamsClass(studentsTeacher, className);
                                    } else {
                                        Toast.makeText(getContext(), "Class data not found", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        } else {
                            Log.e("StudentClassroomFragment", "Student document not found");
                        }
                    })
                    .addOnFailureListener(e -> Log.e("StudentClassroomFragment", "Error loading student data", e));
        }
    }

    interface OnClassNameRetrievedListener {
        void onClassNameRetrieved(String className);
    }

    private void getStudentsClassName(String studentsTeacher, OnClassNameRetrievedListener listener) {//Gets the class name for the student
        firestore.collection("teachers").document(studentsTeacher)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, ArrayList<String>> classes = (Map<String, ArrayList<String>>) documentSnapshot.get("classes");//Get the classes data
                        if (classes != null) {
                            for (Map.Entry<String, ArrayList<String>> entry : classes.entrySet()) {
                                if (entry.getValue().contains(Online_user_id)) {
                                    listener.onClassNameRetrieved(entry.getKey());

                                    return;
                                }
                            }
                        }
                    }
                    listener.onClassNameRetrieved(null);
                })
                .addOnFailureListener(e -> Log.e("StudentClassroomFragment", "Error getting teacher data in method getsStudentsClassName", e));
    }

    private void getTeamsClass(String studentsTeacher, String className) {//Gets the teams in there class for the student
        firestore.collection("teachers")
                .document(studentsTeacher)
                .get()
                .addOnCompleteListener(task_get_teams -> {
                    if (task_get_teams.isSuccessful()) {
                        DocumentSnapshot document = task_get_teams.getResult();
                        if (document.exists()) {
                        Map<String, Object> classTeams = (Map<String, Object>) document.get("class_teams");
                        if (classTeams != null && classTeams.containsKey(className)) {
                            Log.d("StudentClassroomFragment", "Class Teams found for class: " + className);
                            Map<String, Object> teamsData = (Map<String, Object>) classTeams.get(className);
                            if (teamsData != null && teamsData.containsKey("teams")) {

                                Log.d("StudentClassroomFragment", "Teams data found: " + teamsData);

                                //Extract teams for the given class
                                Map<String, List<Map<String, String>>> teamNameMap = (Map<String, List<Map<String, String>>>) teamsData.get("teams");

                                Log.d("StudentClassroomFragment", "Teams Map: " + teamNameMap);

                                if (teamNameMap == null || teamNameMap.isEmpty()) {
                                    Log.d("ClassroomFragment!!!!!!!", "No teams found for class: " + className);

                                    //Update UI to indicate no teams found
                                    teamsList.clear();
                                    if (teamAdapter != null) {
                                        teamAdapter.notifyDataSetChanged();
                                    }
                                }
                                else {
                                    teamsList.clear();

                                    //Iterate through teams
                                    for (Map.Entry<String, List<Map<String, String>>> entry : teamNameMap.entrySet()) {
                                        String teamName = entry.getKey();
                                        List<Map<String, String>> studentMaps = entry.getValue();

                                        List<String> studentNames = new ArrayList<>();
                                        for (Map<String, String> studentMap : studentMaps) {//Iterate through students in the team
                                            String studentName = studentMap.get("name");
                                            Log.d("StudentClassroomFragment", "teams in class: " + studentName);
                                            if (studentName != null) {
                                                studentNames.add(studentName);//Add the student name to the list

                                            }
                                        }

                                        //Create a Team object and add it to the teamsList
                                        Team team = new Team(teamName, studentNames);
                                        teamsList.add(team);

                                    }
                                    //Update adapter if it's not null
                                    if (teamAdapter != null) {
                                        teamAdapter.notifyDataSetChanged();
                                    } else {
                                        Log.e("StudentClassroomFragment", "teamsAdapter is null");
                                    }
                                }

                            } else {
                                Log.e("StudentClassroomFragment", "No 'teams' field found for class: " + className);
                            }
                        } else {
                            Log.e("StudentClassroomFragment", "No teams data found for class: " + className);
                            teamsList.clear();
                            if (teamAdapter != null) {
                                teamAdapter.notifyDataSetChanged();
                            }
                        }
                        } else {
                        Log.d("StudentClassroomFragment", "No such document exist for Teacher: " + Online_user_id);
                    }
                } else {
            Log.e("StudentClassroomFragment", "Error getting document: ", task_get_teams.getException());
            }
        });
    }

    private void loadStudentsForClass(String studentsTeacher, String className) {//Gets the students in there class for the student
        Log.d("$$StudentClassroomFragment", "teacher: " + studentsTeacher);
        Log.d("$$StudentClassroomFragment", "Class Name: " + className);
        firestore.collection("teachers")
                .document(studentsTeacher)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> classesData = documentSnapshot.getData();
                        if (classesData != null && classesData.containsKey("classes")) {
                            Map<String, ArrayList<String>> currentClasses = (Map<String, ArrayList<String>>) classesData.get("classes");
                            ArrayList<String> studentNames = currentClasses.get(className);
                            Log.d("TTTTTStudentClassroomFragment", "Studnets in class: " + studentNames );
                            if (studentNames != null) {
                                studentList.clear();
                                studentList.addAll(studentNames);
                                studentAdapter.notifyDataSetChanged();
                            }
                        } else {
                            Log.e("StudentClassroomFragment", "No students found for class: " + className);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("StudentClassroomFragment", "Error fetching student data", e));
    }
}