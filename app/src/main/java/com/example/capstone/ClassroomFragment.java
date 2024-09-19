package com.example.capstone;

import static com.example.capstone.FS_DBHelper.Online_user_id;

import android.app.AlertDialog;
import android.opengl.Visibility;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClassroomFragment extends Fragment {

    private Spinner classSpinner;
    private RecyclerView studentGrid;

    private RecyclerView teamsListGird;
    private Button createClassroomButton;
    private Button createTeamButton;
    private ImageButton addStudentButton;

    private Button disbandTeamButton;

    private ImageButton removeStudentButton;

    private ImageButton editClassNameButton;

    private ImageButton deleteClassButton;

    private FirebaseFirestore db;
    private List<String> classList;
    private List<String> studentList;
    private List<Team> teamsList;

    private TeamAdapter teamsAdapter;
    private StudentAdapter studentAdapter; // Use ArrayAdapter for names
    private FS_DBHelper dbHelper; // Add FS_DBHelper instance

    private Map<String, String> studentTeamMap = new HashMap<>(); //To help track student already in team

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_classroom, container, false);
        db = FirebaseFirestore.getInstance();// Initialize Firestore

        // Initialize FS_DBHelper
        dbHelper = new FS_DBHelper(FirebaseFirestore.getInstance()); // Pass the Firestore instance

        // Bind UI components
        classSpinner = view.findViewById(R.id.class_spinner);
        studentGrid = view.findViewById(R.id.student_grid);
        teamsListGird = view.findViewById(R.id.teamsList);
        createClassroomButton = view.findViewById(R.id.create_classroom_button);
        createTeamButton = view.findViewById(R.id.create_team_button);
        disbandTeamButton= view.findViewById(R.id.disband_team_button);
        addStudentButton = view.findViewById(R.id.student_add_button);
        removeStudentButton = view.findViewById(R.id.student_remove_button);
        editClassNameButton = view.findViewById(R.id.edit_class_name_button);
        deleteClassButton = view.findViewById(R.id.delete_class_button);


        //Initialize class and student lists
        classList = new ArrayList<>();
        studentList = new ArrayList<>();


        //Set up RecyclerView (GridLayoutManager for student grid)
        studentGrid.setLayoutManager(new GridLayoutManager(getContext(), 3)); // 3 columns
        studentAdapter = new StudentAdapter(studentList);
        studentGrid.setAdapter(studentAdapter);


        //Set up RecyclerView (GridLayoutManager for teams)

        teamsList = new ArrayList<>();
        teamsAdapter = new TeamAdapter(teamsList);
        teamsListGird.setAdapter(teamsAdapter);
        teamsListGird.setLayoutManager(new GridLayoutManager(getContext(), 1)); // Grid layout with 1 column

        //Load classes and populate spinner
        loadClasses(null);


        //Handle class selection from the spinner
        classSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                String selectedClass = classList.get(position);
                loadStudentsForClass(selectedClass);//loads students for the selected class in the spinner
                loadTeamsForClass(selectedClass);//loads teams for the selected class

            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                //No action needed here for now
            }
        });

        //initialize dialog for creating a new classroom on button click
        createClassroomButton.setOnClickListener(v -> showCreateClassroomDialog());

        //initialize dialog for adding students on add student button click
        addStudentButton.setOnClickListener(v -> showAddStudentDialog());

        //initialize dialog for removing students on remove student button click
        removeStudentButton.setOnClickListener(v -> { showRemoveStudentDialog(); });

        //initialize dialog for creating a new team on create team button click
        createTeamButton.setOnClickListener(v -> showCreateTeamDialog());

        //initialize dialog for disbanding teams on disband team button click
        disbandTeamButton.setOnClickListener(v -> showDisbandTeamsDialog());

        //initialize dialog for editing class name on edit class name button click
        editClassNameButton.setOnClickListener(v -> showEditClassNameDialog());

        //initialize dialog for deleting class on delete class button click
        deleteClassButton.setOnClickListener(v -> showDeleteClassDialog());


        return view;
    }

    private void loadClasses(String selectedClass) {//method for loading classes and populating the spinner

        /*Once a classroom is created, show the buttons
        createTeamButton.setVisibility(View.VISIBLE);
        disbandTeamButton.setVisibility(View.VISIBLE);
        removeStudentButton.setVisibility(View.VISIBLE);
        addStudentButton.setVisibility(View.VISIBLE);

         */

        if (Online_user_id != null) {
            dbHelper.getTeacherClasses(Online_user_id, classes -> {
                if (classes == null) {
                    //Show dialog for no classes or error
                    showNoClassesDialog();
                }


                else {
                    classList.clear();//Clear the list
                    classList.addAll(classes); //Populates class list with data from FS_DBHelper

                    //Check if classList contains only non-null items
                    for (String className : classList) {
                        if (className == null) {
                            Log.e("ClassroomFragment", "Null item found in classList");
                        }
                    }
                    //Populate the spinner with the class names
                    ArrayAdapter<String> classAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, classList);
                    classAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    classSpinner.setAdapter(classAdapter);

                    //Sets the spinner selection to the newly created classroom
                    int position = classList.indexOf(selectedClass);
                    if (position != -1) {
                        classSpinner.setSelection(position);
                    }
                    loadTeamsForClass(selectedClass);//loads teams for the selected class
                }
            });
        } else {
            Log.e("ClassroomFragment", "No online user ID found.");
        }
    }

    private void loadStudentsForClass(String className) {//method for loading students for a selected class in the spinner
        //looks for students in the selected class
        db.collection("teachers")
                .document(Online_user_id)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> teacherData = documentSnapshot.getData();
                        if (teacherData != null && teacherData.containsKey("classes")) {
                            Map<String, ArrayList<String>> currentClasses = (Map<String, ArrayList<String>>) teacherData.get("classes");
                            ArrayList<String> studentNames = currentClasses.get(className);//Get students for the selected class

                            if (studentNames != null) {
                                studentList.clear();
                                studentList.addAll(studentNames);//Add students to the list
                                studentAdapter.notifyDataSetChanged();//Refresh the student grid
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("ClassroomFragment", "Error loading students for class.", e));
    }

    private void showCreateClassroomDialog() {//for the create classroom button
        //Create a dialog to input the new classroom name
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_create_classroom, null);
        builder.setView(dialogView);

        EditText classNameInput = dialogView.findViewById(R.id.class_name_input);

        builder.setPositiveButton("Create", (dialog, id) -> {
            String newClassName = classNameInput.getText().toString();

            createNewClassroom(newClassName);//calls FS_DBHelper method for creation of new classroom
        });

        builder.setNegativeButton("Cancel", (dialog, id) -> dialog.dismiss());

        builder.create().show();
    }

    private void showCreateTeamDialog() {//for the create team button

        if(Online_user_id != null){//check if user is logged in
            dbHelper.getTeacherClasses(Online_user_id, classes -> {
                if (classes == null) {
                    //Show dialog for no classes or error. Handles error of no classes on new new teacher logging in
                    showNoClassesDialog();
                }
                else {//Create a dialog to input the new team name
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    LayoutInflater inflater = getLayoutInflater();
                    View dialogView = inflater.inflate(R.layout.dialog_create_team, null);
                    builder.setView(dialogView);

                    //layout for dialog
                    EditText teamNameInput = dialogView.findViewById(R.id.team_name_input);
                    RecyclerView teamStudentSelectionList = dialogView.findViewById(R.id.team_student_selection_list);
                    teamStudentSelectionList.setLayoutManager(new GridLayoutManager(getContext(), 3)); // Adjust column count

                    List<Student> studentList_Teams = new ArrayList<>();
                    StudentSelectionAdapter teamSelectionAdapter = new StudentSelectionAdapter(studentList_Teams);
                    teamStudentSelectionList.setAdapter(teamSelectionAdapter);

                    //Fetch students for the selected class and populate the RecyclerView
                    fetchStudentsForSelectedClass(studentList_Teams, teamSelectionAdapter);

                    builder.setPositiveButton("Create", (dialog, id) -> {
                        String newTeamName = teamNameInput.getText().toString();//Get the team name from the input
                        if (newTeamName.isEmpty()) {//ensure team name is not empty
                            Toast.makeText(getContext(), "Please enter a team name.", Toast.LENGTH_SHORT).show();
                        }
                        else {
                            List<Student> selectedStudentsForTeam = teamSelectionAdapter.getSelectedStudents(); //Get the List<Student> directly

                            //Check if any selected students are already in another team
                            for (Student student : selectedStudentsForTeam) {
                                if (studentTeamMap.containsKey(student.getName())) {
                                    String currentTeam = studentTeamMap.get(student.getName());
                                    Toast.makeText(getContext(), student.getName() + " is already in team " + currentTeam, Toast.LENGTH_LONG).show();
                                    return; //Prevent team creation
                                }
                            }

                            createNewTeam(Online_user_id.toString(),newTeamName, selectedStudentsForTeam);//calls FS_DBHelper method for creation of new team
                        }
                    });

                    builder.setNegativeButton("Cancel", (dialog, id) -> dialog.dismiss());
                    builder.create().show();

                }

            });
        }
        else {
           Toast.makeText(getContext(), "No online user ID found.", Toast.LENGTH_SHORT).show();
        }

    }

    //method for creating a new team. Calls FS_DBHelper method since it is delegated for team creation
    private void createNewTeam(String className, String teamName, List<Student> selectedStudents) {
        dbHelper.createTeam(classSpinner.getSelectedItem().toString(), teamName, selectedStudents, isSuccess -> {
            if (isSuccess) {//On success load teams for the selected class and load the spinner with the new team
                loadTeamsForClass(classSpinner.getSelectedItem().toString());
            }
        });
    }

    private void showEditClassNameDialog() {//dialog for editing a the curently selected class
        if(Online_user_id != null){
            dbHelper.getTeacherClasses(Online_user_id, classes -> {
                if (classes == null) {
                    //Show dialog for no classes or error. Handles error of no classes on new new teacher logging in
                    showNoClassesDialog();
                }
                else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle("Edit Class Name");

                    View viewInflated = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_class, (ViewGroup) getView(), false);
                    final EditText input = viewInflated.findViewById(R.id.input_new_class_name);
                    builder.setView(viewInflated);

                    builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        dialog.dismiss();
                        String newClassName = input.getText().toString().trim();
                        if (!newClassName.isEmpty() ) {

                            //Firebase dose not allow map names to have any of these characters
                            Set<Character> invalidChars = new HashSet<>(Arrays.asList('.', '/', '*', '[', ']', '~', '#', '\0'));
                            for (char c : newClassName.toCharArray()) {//Check for invalid characters
                                if (invalidChars.contains(c)) {
                                    Log.d("ClassroomFragment", "Invalid characters in new class name.");
                                    Toast.makeText(getContext(), "Class name cannot contain: . / * [ ] ~ # \\0", Toast.LENGTH_LONG).show();
                                    return;
                                }
                            }

                            String oldClassName = classSpinner.getSelectedItem().toString();
                            renameClass(oldClassName, newClassName);
                        }
                    });
                    builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());

                    builder.show();
                }
            });
        }
    }

    private void renameClass(String oldClassName, String newClassName) {
        if (Online_user_id == null) {
            Log.e("ClassroomFragment", "No online user ID found.");
            return;
        }

        if (oldClassName.equals(newClassName)) {
            Log.d("ClassroomFragment", "Class name is the same, no need to rename.");
            return; //No need to rename if the names are the same
        }

        DocumentReference teacherRef = db.collection("teachers").document(Online_user_id);//get teacher in system

        teacherRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Map<String, Object> teacherData = documentSnapshot.getData();//snapShot of the teachers data
                if (teacherData != null) {

                    if (teacherData.containsKey("class_teams")) {//Check if class_teams exists
                        Map<String, Object> classTeams = (Map<String, Object>) teacherData.get("class_teams");
                        if (classTeams != null && classTeams.containsKey(newClassName)) {//Check if the new class name already exists in class_teams
                            Log.d("ClassroomFragment", "A class_team with the new name already exists.");
                            Toast.makeText(getContext(), "A Classroom with name '" + newClassName + "' already exists.", Toast.LENGTH_LONG).show();
                            return; //A class_team with the new name already exists
                        }
                    }

                    Map<String, Object> updates = new HashMap<>();//Map for updates

                    //Rename class in "classes" field
                    if (teacherData.containsKey("classes")) {
                        Map<String, Object> classes = (Map<String, Object>) teacherData.get("classes");
                        if (classes != null && classes.containsKey(oldClassName)) {
                            Object classData = classes.get(oldClassName);
                            classes.remove(oldClassName);
                            classes.put(newClassName, classData);
                            updates.put("classes", classes);
                        }
                    }

                    //Rename teams in "class_teams" field
                    if (teacherData.containsKey("class_teams")) {
                        Map<String, Object> classTeams = (Map<String, Object>) teacherData.get("class_teams");
                        if (classTeams != null && classTeams.containsKey(oldClassName)) {
                            updates.put("class_teams." + newClassName, classTeams.get(oldClassName));
                            updates.put("class_teams." + oldClassName, FieldValue.delete());
                        }
                    }

                    //Update Firestore Database
                    if (!updates.isEmpty()) {
                        teacherRef.update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("ClassroomFragment", "Class and teams updated successfully.");
                                    loadClasses(newClassName);
                                })
                                .addOnFailureListener(e -> Log.e("ClassroomFragment", "Error updating class and teams.", e));
                    }
                }
            }
        }).addOnFailureListener(e -> Log.e("ClassroomFragment", "Error getting teacher document", e));
    }

    private void showDeleteClassDialog(){//dialog for deleting a the curently selected class
        if(Online_user_id != null){
            dbHelper.getTeacherClasses(Online_user_id, classes -> {
                if (classes == null) {
                    //Show dialog for no classes or error. Handles error of no classes on new new teacher logging in
                    showNoClassesDialog();
                    }
                    else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle("You are about to Delete this class!");
                    //you are about to delete this class view next next
                    builder.setMessage("Are you sure you want to delete this class? This action cannot be undone.");
                    builder.setPositiveButton("Delete", (dialog, which) -> {
                        String selectedClass = classSpinner.getSelectedItem().toString();
                        deleteClass(selectedClass);
                    });
                    builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
                    builder.show();
                    }

                });
            }
    }

    private void deleteClass(String className) {//method for deleting a the curently selected class. deletes any teams in the class as well
        if (Online_user_id == null) {
            Log.e("ClassroomFragment", "No online user ID found.");
            return;
        }

        DocumentReference teacherRef = db.collection("teachers").document(Online_user_id);

        teacherRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Map<String, Object> teacherData = documentSnapshot.getData();
                if (teacherData != null && teacherData.containsKey("classes")) {
                    Map<String, Object> classes = (Map<String, Object>) teacherData.get("classes");
                    if (classes != null && classes.containsKey(className)) {
                        classes.remove(className);

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("classes", classes);
                        //Delete class from class_teams
                        updates.put("class_teams." + className, FieldValue.delete());

                        teacherRef.update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("ClassroomFragment", "Class deleted successfully.");
                                    classSpinner.setAdapter(null);//Clears spinner
                                    studentList.clear();//clears student list
                                    studentAdapter.notifyDataSetChanged();//updates student grid
                                    loadClasses(null); //Reload class to update the spinner
                                    loadTeamsForClass(null); //Reload teams for the deleted class as null

                                })
                                .addOnFailureListener(e -> Log.e("ClassroomFragment", "Error deleting class.", e));
                    }
                }
            }
        }).addOnFailureListener(e -> Log.e("ClassroomFragment", "Error getting teacher document", e));
    }



    private void showDisbandTeamsDialog() {//for the disband team button
        if(Online_user_id != null){
            dbHelper.getTeacherClasses(Online_user_id, classes -> {
                if (classes == null) {
                    //Show dialog for no classes or error. Handles error of no classes on new new teacher logging in
                    showNoClassesDialog();
                }
                else {

                    //layout for dialog
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    LayoutInflater inflater = getLayoutInflater();
                    View dialogView = inflater.inflate(R.layout.dialog_disband_team, null);
                    builder.setView(dialogView);
                    RecyclerView teamDisbandSelectionList = dialogView.findViewById(R.id.team_selection_disband_list);
                    teamDisbandSelectionList.setLayoutManager(new GridLayoutManager(getContext(), 1));

                    List<Team> teamList = new ArrayList<>();//This is the list of teams for the selected class
                    TeamDisbandAdapter teamDisbandAdapter = new TeamDisbandAdapter(teamList);//Adapter for the RecyclerView
                    teamDisbandSelectionList.setAdapter(teamDisbandAdapter);

                    String selectedClass = classSpinner.getSelectedItem().toString(); //Get the selected class from the spinner

                    //Searches teacher document -> class_teams -> class -> teams
                    db.collection("teachers")
                            .document(Online_user_id.toString())
                            .get()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    DocumentSnapshot document = task.getResult();
                                    if (document.exists()) {
                                        Map<String, Object> classTeams = (Map<String, Object>) document.get("class_teams");
                                        if (classTeams != null && classTeams.containsKey(selectedClass)) {

                                            Log.d("ClassroomFragment", "Class Teams found for class: " + selectedClass);

                                            //Extract teams for the given class
                                            Map<String, Object> classData = (Map<String, Object>) classTeams.get(selectedClass);
                                            if (classData != null && classData.containsKey("teams")) {

                                                Log.d("ClassroomFragment", "Teams data found: " + classData.toString());

                                                Map<String, List<Map<String, String>>> teamNameMap = (Map<String, List<Map<String, String>>>) classData.get("teams");
                                                if (teamNameMap != null) {//Iterate through teams in the class
                                                    for (Map.Entry<String, List<Map<String, String>>> entry : teamNameMap.entrySet()) {
                                                        String teamName = entry.getKey();
                                                        List<Map<String, String>> studentMaps = entry.getValue();

                                                        List<String> studentNames = new ArrayList<>();
                                                        for (Map<String, String> studentMap : studentMaps) {//Iterate through students in the team
                                                            String studentName = studentMap.get("name");
                                                            if (studentName != null) {
                                                                studentNames.add(studentName);
                                                            }
                                                        }
                                                        Team team = new Team(teamName, studentNames);//Creates a new team object with the team name and students
                                                        teamList.add(team);//adds the team to the list
                                                    }
                                                    teamDisbandAdapter.notifyDataSetChanged();//notifies the adapter of the changes
                                                }
                                            } else {
                                                Log.e("ClassroomFragment", "No 'teams' field found for class: " + selectedClass);
                                            }
                                        } else {
                                            Log.e("ClassroomFragment", "No teams data found for class: " + selectedClass);
                                        }
                                    } else {
                                        Log.d("ClassroomFragment", "No such document for teacher: " + Online_user_id);
                                    }
                                } else {
                                    Log.d("ClassroomFragment", "Error fetching teams for disbandment: ", task.getException());
                                }
                            });

                    builder.setPositiveButton("Disband", (dialog, which) -> {
                        List<Team> selectedTeams = teamDisbandAdapter.getSelectedTeams();//selected teams for disbanding
                        disbandTeams(selectedTeams);//calls disbandTeams method for disbanding teams
                    });

                    builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
                    builder.show();
                }
            });
        }
        else {
        Toast.makeText(getContext(), "No online user ID found.", Toast.LENGTH_SHORT).show();
        }
    }

    private void disbandTeams(List<Team> teams) {//method for disbanding teams. List of teams that were selected from the RecyclerView is passed in
        if (Online_user_id == null) {
            Log.e("ClassroomFragment", "No online user ID found.");
            return;
        }

        String selectedClass = classSpinner.getSelectedItem().toString();//gets the selected class from the spinner

        //Create a map to hold the updates for the teams
        Map<String, Object> updates = new HashMap<>();

        for (Team team : teams) {//for each team in the list of teams selected for disbanding the team is added to the updates map
            //Add the team to be deleted to the updates map
            updates.put("class_teams." + selectedClass + ".teams." + team.getTeamName(), FieldValue.delete());
        }
        //Update the teacher document with the updates. Updating the class_teams field
        db.collection("teachers")
                .document(Online_user_id.toString())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d("ClassroomFragment", "Teams successfully disbanded.");
                    loadTeamsForClass(selectedClass); //Reload teams for the current class
                })
                .addOnFailureListener(e -> Log.e("ClassroomFragment", "Error disbanding teams", e));
    }

    private void onremoveStudentFromClassDisbandTeamsbyName(String studentName){ //method for removing the student from the team they are in if they are removed from the classroom
        String selectedClass = classSpinner.getSelectedItem().toString();

        //Searches teacher document -> class_teams -> class -> teams -> teamName -> studentName
        db.collection("teachers")
                .document(Online_user_id.toString())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> classTeams = (Map<String, Object>) documentSnapshot.get("class_teams");
                        if (classTeams != null && classTeams.containsKey(selectedClass)) {
                            Map<String, Object> classData = (Map<String, Object>) classTeams.get(selectedClass);
                            if (classData != null && classData.containsKey("teams")) {
                                Map<String, List<Map<String, String>>> teamNameMap = (Map<String, List<Map<String, String>>>) classData.get("teams");
                                if (teamNameMap != null) {
                                    for (Map.Entry<String, List<Map<String, String>>> entry : teamNameMap.entrySet()) {
                                        String teamName = entry.getKey();
                                        List<Map<String, String>> studentMaps = entry.getValue();

                                        //Find the student in the team and remove them
                                        List<Map<String, String>> updatedStudentMaps = new ArrayList<>();
                                        for (Map<String, String> studentMap : studentMaps) {
                                            String name = studentMap.get("name");
                                            if (name != null && !name.equals(studentName)) {
                                                updatedStudentMaps.add(studentMap);//Add the student to the updated list to be removed from the team
                                            }
                                        }

                                        // Update the team with the removed student
                                        Map<String, Object> updates = new HashMap<>();
                                        updates.put("class_teams." + selectedClass + ".teams." + teamName, updatedStudentMaps);
                                        //Update the teacher document with the updates for removing the student from the team
                                        db.collection("teachers")
                                                .document(Online_user_id.toString())
                                                .update(updates)
                                                .addOnSuccessListener(aVoid -> {
                                                    Log.d("ClassroomFragment", "Student removed from team: " + teamName);
                                                    loadTeamsForClass(selectedClass); //Reload teams for the current class
                                                })
                                                .addOnFailureListener(e -> Log.e("ClassroomFragment", "Error updating team", e));
                                    }
                                }
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("ClassroomFragment", "Error getting teacher document", e));
    }

    //method for fetching students for the selected class
    private void fetchStudentsForSelectedClass(List<Student> studentList_Teams, StudentSelectionAdapter teamSelectionAdapter) {
        String selectedClass = classSpinner.getSelectedItem().toString(); // Get the selected class

        //Searches teacher document -> classes -> class -> students
        db.collection("teachers")
                .document(Online_user_id.toString()) // Fetch the teacher document using their ID
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Map<String, ArrayList<String>> classes = (Map<String, ArrayList<String>>) document.get("classes");
                            if (classes != null && classes.containsKey(selectedClass)) {
                                List<String> studentNames = classes.get(selectedClass);
                                fetchStudentsByNames(studentNames, studentList_Teams, teamSelectionAdapter);//calls method for fetching students by names
                            } else {
                                Log.d("ClassroomFragment", "Selected class not found in teacher's classes.");
                            }
                        } else {
                            Log.d("ClassroomFragment", "Teacher document does not exist.");
                        }
                    } else {
                        Log.d("ClassroomFragment", "Error fetching teacher document: ", task.getException());
                    }
                });
    }

    //method for fetching students by names
    private void fetchStudentsByNames(List<String> studentNames, List<Student> studentList_Teams, StudentSelectionAdapter teamSelectionAdapter) {
        if (studentNames == null || studentNames.isEmpty()) {
            Log.d("ClassroomFragment", "No students to fetch.");
            return;
        }
        //Searches students document -> name
        db.collection("students")
                .whereIn("name", studentNames) //students have a 'name' field that matches
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        studentList_Teams.clear(); //Clear the existing list
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Student student = document.toObject(Student.class);
                            studentList_Teams.add(student);
                        }
                        teamSelectionAdapter.notifyDataSetChanged(); //Refresh RecyclerView
                    } else {
                        Log.d("ClassroomFragment", "Error getting students: ", task.getException());
                    }
                });
    }

    private void loadTeamsForClass(String className) {//method for loading teams for the selected class
        if (Online_user_id == null) {
            Log.e("ClassroomFragment", "No online user ID found.");
            return;
        }
        //Searches teacher document -> class_teams -> class -> teams
        db.collection("teachers")
                .document(Online_user_id.toString())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Map<String, Object> classTeams = (Map<String, Object>) document.get("class_teams");
                            if (classTeams != null && classTeams.containsKey(className) && classSpinner.getSelectedItem().toString().equals(className)) {

                                Log.d("ClassroomFragment", "Class Teams found for class: " + className);

                                // Extract teams for the given class
                                Map<String, Object> classData = (Map<String, Object>) classTeams.get(className);
                                if (classData != null && classData.containsKey("teams")) {

                                    Log.d("ClassroomFragment", "Teams data found: " + classData.toString());

                                    //Teams map: teamName -> List of student maps
                                    Map<String, List<Map<String, String>>> teamNameMap = (Map<String, List<Map<String, String>>>) classData.get("teams");

                                    if (teamNameMap == null || teamNameMap.isEmpty()) {
                                        Log.d("ClassroomFragment!!!!!!!", "No teams found for class: " + className);

                                        //Update UI to indicate no teams found
                                        teamsList.clear();
                                        studentTeamMap.clear();
                                        if (teamsAdapter != null) {
                                            teamsAdapter.notifyDataSetChanged();
                                        }
                                    }
                                    else {

                                        teamsList.clear();
                                        studentTeamMap.clear();

                                        //Iterate through teams
                                        for (Map.Entry<String, List<Map<String, String>>> entry : teamNameMap.entrySet()) {
                                            String teamName = entry.getKey();
                                            List<Map<String, String>> studentMaps = entry.getValue();

                                            List<String> studentNames = new ArrayList<>();
                                            for (Map<String, String> studentMap : studentMaps) {//Iterate through students in the team
                                                String studentName = studentMap.get("name");
                                                if (studentName != null) {
                                                    studentNames.add(studentName);//Add the student name to the list
                                                    studentTeamMap.put(studentName, teamName); //Map student to their team
                                                }
                                            }

                                            //Create a Team object and add it to the teamsList
                                            Team team = new Team(teamName, studentNames);
                                            teamsList.add(team);

                                            //Log the team and its members
                                            Log.d("ClassroomFragment", "Team: " + teamName);
                                            for (String studentName : studentNames) {
                                                Log.d("ClassroomFragment", "Member: " + studentName);
                                            }
                                        }

                                        //Update adapter if it's not null
                                        if (teamsAdapter != null) {
                                            teamsAdapter.notifyDataSetChanged();
                                        } else {
                                            Log.e("ClassroomFragment", "teamsAdapter is null");
                                        }
                                    }

                                } else {
                                    Log.e("ClassroomFragment", "No 'teams' field found for class: " + className);
                                }
                            } else {
                                Log.e("ClassroomFragment", "No teams data found for class: " + className);
                                teamsList.clear();
                                studentTeamMap.clear();
                                if (teamsAdapter != null) {
                                    teamsAdapter.notifyDataSetChanged();
                                }
                            }
                        } else {
                            Log.d("ClassroomFragment", "No such document for teacher: " + Online_user_id);
                        }
                    } else {
                        Log.e("ClassroomFragment", "Error getting document: ", task.getException());
                    }
                });
    }


    private void createNewClassroom(String className) {//method for creating a new classroom
        if (Online_user_id == null) {
            Log.e("ClassroomFragment", "No online user ID found.");
            return;
        }

        //Firebase dose not allow map names to have any of these characters
        Set<Character> invalidChars = new HashSet<>(Arrays.asList('.', '/', '*', '[', ']', '~', '#', '\0'));

        for (char c : className.toCharArray()) {//check for invalid characters
            if (invalidChars.contains(c)) {
                Log.d("ClassroomFragment", "Invalid characters in class name.");
                Toast.makeText(getContext(), "Class name cannot contain: . / * [ ] ~ # \\0", Toast.LENGTH_LONG).show();
                return;
            }
        }

        //Get the existing classes for the teacher
        //Searches teacher document -> classes
        db.collection("teachers")
                .document(Online_user_id)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> teacherData = documentSnapshot.getData();
                        if (teacherData != null && teacherData.containsKey("classes")) {
                            //Fetch the current class list
                            Map<String, ArrayList<String>> currentClasses = (Map<String, ArrayList<String>>) teacherData.get("classes");

                            if (currentClasses != null) {
                                //Check if the class already exists for this teacher
                                if (currentClasses.containsKey(className)) {
                                    Log.d("ClassroomFragment", "Class already exists.");
                                    Toast.makeText(getContext(), "Class '" + className + "' already exists.", Toast.LENGTH_LONG).show();
                                    return; //Class already exists so dose not create a new one
                                }
                            } else {
                                //If there are no classes yet initialize the class map
                                currentClasses = new HashMap<>();
                            }

                            // Add the new class with no students
                            currentClasses.put(className, new ArrayList<>());

                            // Update Firestore with the new class list
                            db.collection("teachers")
                                    .document(Online_user_id)
                                    .update("classes", currentClasses)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("ClassroomFragment", "Classroom '" + className + "' successfully created.");
                                        Toast.makeText(getContext(), "Classroom '" + className + "' created.", Toast.LENGTH_SHORT).show();
                                        loadClasses(className);  //Refresh the spinner or UI element


                                    })
                                    .addOnFailureListener(e -> Log.e("ClassroomFragment", "Error updating classroom.", e));

                        } else {
                            //If no classes exist for this teacher create a new map for classes
                            Map<String, ArrayList<String>> newClassMap = new HashMap<>();
                            newClassMap.put(className, new ArrayList<>()); // Empty student list


                            //Save the new class map in Firestore
                            db.collection("teachers")
                                    .document(Online_user_id)
                                    .update("classes", newClassMap)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("ClassroomFragment", "New classroom '" + className + "' created successfully.");
                                        createNewTeam(className, null, null); //creates a empty team map for class
                                        loadClasses(className); //Refresh the spinner or UI element

                                    })
                                    .addOnFailureListener(e -> Log.e("ClassroomFragment", "Error creating classroom.", e));
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("ClassroomFragment", "Error fetching teacher's classes.", e));
    }


    private void showAddStudentDialog() {//for the add student button

        if(Online_user_id != null){
            dbHelper.getTeacherClasses(Online_user_id, classes -> {
                if (classes == null) {
                    //Show dialog for no classes or error
                    showNoClassesDialog();
                }
                else {
                    //Create a dialog to display all students associated with the teacher
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    LayoutInflater inflater = getLayoutInflater();
                    View dialogView = inflater.inflate(R.layout.dialog_add_students, null);
                    builder.setView(dialogView);

                    RecyclerView studentSelectionList = dialogView.findViewById(R.id.student_selection_list);
                    studentSelectionList.setLayoutManager(new GridLayoutManager(getContext(), 3)); // 3 columns

                    List<Student> studentList = new ArrayList<>();
                    StudentSelectionAdapter selectionAdapter = new StudentSelectionAdapter(studentList); // Assuming you have this adapter with checkboxes
                    studentSelectionList.setAdapter(selectionAdapter);

                    //Fetch students from Firestore whose 'teachers' array contains the current teacher's name
                    db.collection("students")
                            .whereEqualTo("teacher",Online_user_id.toString()) //Fetch only students with this teacher
                            .get()
                            .addOnCompleteListener(task_check_students_teach -> {

                                //teachers -> classes -> class(array) -> students
                                //check if students are already in a class


                                if (task_check_students_teach.isSuccessful()) {
                                    studentList.clear();
                                    for (QueryDocumentSnapshot document : task_check_students_teach.getResult()) {
                                        Student student = document.toObject(Student.class);
                                        studentList.add(student); //Add to the list
                                    }
                                    selectionAdapter.notifyDataSetChanged(); //Refresh RecyclerView
                                }
                                else{
                                    Toast.makeText(getContext(), "This student is already in a class you have.", Toast.LENGTH_SHORT).show();
                                }
                            });

                    builder.setPositiveButton("Add", (dialog, id) -> {
                        List<Student> selectedStudents = selectionAdapter.getSelectedStudents(); //Get selected students
                        if (!selectedStudents.isEmpty()) {
                            String selectedClass = classSpinner.getSelectedItem().toString();

                            //Get teacher's classes
                            db.collection("teachers")
                                    .document(Online_user_id)
                                    .get()
                                    .addOnSuccessListener(teacherDoc -> {
                                        if (teacherDoc.exists()) {
                                            Map<String, Object> teacherData = teacherDoc.getData();
                                            if (teacherData != null && teacherData.containsKey("classes")) {
                                                Map<String, ArrayList<String>> teacherClasses = (Map<String, ArrayList<String>>) teacherData.get("classes"); //Renamed to teacherClasses

                                                boolean studentExists = false;
                                                for (Student student : selectedStudents) {//for each selected student check if they are already in a class
                                                    if (isStudentInClasses(student.getName(), teacherClasses)) {
                                                        studentExists = true;
                                                        break;
                                                    }
                                                }

                                                if (studentExists) {//student is already in a class
                                                    Toast.makeText(getContext(), "One or more students are already in a class.", Toast.LENGTH_SHORT).show();
                                                } else {
                                                    addStudentsToClass(selectedClass, selectedStudents);
                                                }
                                            }
                                        }
                                    });
                        }
                    });

                    builder.setNegativeButton("Cancel", (dialog, id) -> dialog.dismiss());
                    builder.create().show();

                }
            });
        }
        else {
            Toast.makeText(getContext(), "No online user ID found.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isStudentInClasses(String studentName, Map<String, ArrayList<String>> classes) {//checks for if a student is in a array of classes
        for (ArrayList<String> classStudents : classes.values()) {//Iterate through each class's students
            if (classStudents.contains(studentName)) {//Check if the student is in the class
                return true;
            }
        }
        return false;
    }


    private void showRemoveStudentDialog() {//dialog for removing a student from a class

        if (Online_user_id != null) {
            dbHelper.getTeacherClasses(Online_user_id, classes -> {
                if (classes == null) {
                    //Show dialog for no classes or error
                    showNoClassesDialog();
                } else {
                    //Create a dialog to display the students in the current class
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    LayoutInflater inflater = getLayoutInflater();
                    View dialogView = inflater.inflate(R.layout.dialog_remove_students, null);
                    builder.setView(dialogView);

                    Spinner student_remove_Spinner = dialogView.findViewById(R.id.student_spinner_remove);
                    //RecyclerView studentSelectionList = dialogView.findViewById(R.id.student_selection_list_remove);
                    List<Student> studentList = new ArrayList<>();
                    List<String> studentNamesList = new ArrayList<>(); //To hold student names for the Spinner

                    String teacherClass = classSpinner.getSelectedItem().toString(); //Get selected class

                    //Fetch students for the selected class
                    db.collection("teachers")
                            .document(Online_user_id)
                            .get()
                            .addOnSuccessListener(documentSnapshot -> {
                                if (documentSnapshot.exists()) {
                                    Map<String, List<String>> mapclasses = (Map<String, List<String>>) documentSnapshot.get("classes");
                                    if (mapclasses != null && mapclasses.containsKey(teacherClass)) {
                                        List<String> studentNames = mapclasses.get(teacherClass);
                                        //Fetch Student objects from Firestore based on the student names
                                        db.collection("students")
                                                .whereIn("name", studentNames) //Query for students by names
                                                .get()
                                                .addOnCompleteListener(task -> {
                                                    if (task.isSuccessful()) {
                                                        studentList.clear();
                                                        studentNamesList.clear();
                                                        for (QueryDocumentSnapshot document : task.getResult()) {
                                                            Student student = document.toObject(Student.class);
                                                            studentList.add(student);
                                                            studentNamesList.add(student.getName());
                                                        }
                                                        //Set up the Spinner with student names
                                                        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, studentNamesList);
                                                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                                        student_remove_Spinner.setAdapter(adapter);
                                                    }
                                                });
                                    }
                                }
                            })
                            .addOnFailureListener(e -> {
                                // Handle failure
                            });

                    builder.setPositiveButton("Remove", (dialog, id) -> {
                        //Get the selected student name
                        String selectedStudentName = (String) student_remove_Spinner.getSelectedItem();
                        if (selectedStudentName != null && !selectedStudentName.isEmpty()) {
                            Log.d("ClassroomFragment", "Removing student: " + selectedStudentName);
                            removeStudentFromClass(teacherClass, selectedStudentName); // Remove the selected student from the class
                        }
                    });

                    builder.setNegativeButton("Cancel", (dialog, id) -> dialog.dismiss());
                    builder.create().show();
                }
            });
        } else {
            Toast.makeText(getContext(), "No online user ID found.", Toast.LENGTH_SHORT).show();
        }




    }

    private void removeStudentFromClass(String className, String studentName) {
        if (Online_user_id == null) {
            Log.e("ClassroomFragment", "No online user ID found.");
            return;
        }

        // Get the teacher's class data from Firestore
        //Searches teacher document -> classes -> class -> students
        db.collection("teachers")
                .document(Online_user_id)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> teacherData = documentSnapshot.getData();
                        if (teacherData != null && teacherData.containsKey("classes")) {
                            //Fetch the current class list
                            Map<String, ArrayList<String>> currentClasses = (Map<String, ArrayList<String>>) teacherData.get("classes");
                            ArrayList<String> classStudents = currentClasses.get(className);

                            if (classStudents != null) {
                                //Remove the selected student from the class
                                if (classStudents.remove(studentName)) {
                                    Log.d("ClassroomFragment", "Removed student: " + studentName);
                                    onremoveStudentFromClassDisbandTeamsbyName(studentName); //Custom logic for disbanding teams

                                    //Update Firestore with the new class list
                                    db.collection("teachers")
                                            .document(Online_user_id)
                                            .update("classes", currentClasses)
                                            .addOnSuccessListener(aVoid -> {
                                                Log.d("ClassroomFragment", "Student successfully removed from class.");
                                                loadStudentsForClass(className); //Refresh the student list
                                            })
                                            .addOnFailureListener(e -> Log.e("ClassroomFragment", "Error removing student from class.", e));
                                } else {
                                    Log.d("ClassroomFragment", "Student not found in class.");
                                }
                            } else {
                                Log.e("ClassroomFragment", "No students found in the class.");
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("ClassroomFragment", "Error fetching teacher's classes.", e));
    }


    private void addStudentsToClass(String className, List<Student> selectedStudents) {//method for adding students to a class
        if (Online_user_id == null) {
            Log.e("ClassroomFragment", "No online user ID found.");
            return;
        }

        //teachers -> class_teams -> class -> students
        db.collection("teachers")
                .document(Online_user_id)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> teacherData = documentSnapshot.getData();
                        if (teacherData != null && teacherData.containsKey("classes")) {
                            Map<String, ArrayList<String>> currentClasses = (Map<String, ArrayList<String>>) teacherData.get("classes");

                            //Update the selected class with the new students
                            ArrayList<String> studentNames = currentClasses.getOrDefault(className, new ArrayList<>());
                            for (Student student : selectedStudents) {//Iterate through the selected students for adding them to the class
                                if (!studentNames.contains(student.getName())) {
                                    studentNames.add(student.getName());
                                }
                            }
                            currentClasses.put(className, studentNames);//calls method for updating the class with the new students

                            //Update the teacher document in Firestore
                            db.collection("teachers")
                                    .document(Online_user_id)
                                    .update("classes", currentClasses)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("ClassroomFragment", "Students added to the class successfully.");
                                        loadStudentsForClass(className); //Refresh the student grid
                                    })
                                    .addOnFailureListener(e -> Log.e("ClassroomFragment", "Error adding students to class.", e));
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("ClassroomFragment", "Error fetching teacher's data.", e));
    }

    private void showNoClassesDialog() {//prevents error if no classes exist for teacher
        new AlertDialog.Builder(getActivity()) //Use 'this' if in Activity; replace with 'getActivity()' in Fragment
                .setTitle("No Classes")
                .setMessage("You don't have any classes yet. Please create one.")
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }


}
