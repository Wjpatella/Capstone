//Needs a delete game button that also resets the values in teacher collection field back to null
package com.example.capstone;

import static com.example.capstone.FS_DBHelper.Online_user_id;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeacherGameFragment extends Fragment {
    public static boolean GameActive = false;

    private Spinner timerSpinner;
    private Spinner modeSpinner;
    private Spinner classSpinner;

    private Spinner topicSpinner;
    private Button startGameButton;
    private RecyclerView teamsListGrid;

    private FirebaseFirestore firestore;
    private FirebaseDatabase realtimeDb;
    private FS_DBHelper dbHelper;

    private List<Team> teamsList;
    private TeamAdapter teamsAdapter;

    private static final String TAG = "GameFragment";
    private String teacherName = Online_user_id; // Replace with the logged-in teacher's name

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_game_setup, container, false);

        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance();
        realtimeDb = FirebaseDatabase.getInstance();
        dbHelper = new FS_DBHelper(FirebaseFirestore.getInstance());

        // Initialize UI components
        timerSpinner = view.findViewById(R.id.timer_spinner);
        modeSpinner = view.findViewById(R.id.mode_spinner);
        classSpinner = view.findViewById(R.id.class_spinner);
        topicSpinner = view.findViewById(R.id.topic_spinner);
        startGameButton = view.findViewById(R.id.start_game_button);
        teamsListGrid = view.findViewById(R.id.teamsList);

        // Set up timer spinner
        ArrayAdapter<CharSequence> timerAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.timer_options, android.R.layout.simple_spinner_item);
        timerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        timerSpinner.setAdapter(timerAdapter);

        // Set up mode spinner
        ArrayAdapter<CharSequence> modeAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.mode_options, android.R.layout.simple_spinner_item);  // Add mode options in your strings.xml
        modeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modeSpinner.setAdapter(modeAdapter);

        // Set up mode spinner
        ArrayAdapter<CharSequence> topicAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.topic_options, android.R.layout.simple_spinner_item);  // Add mode options in your strings.xml
        topicAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        topicSpinner.setAdapter(topicAdapter);


        //Set up RecyclerView (GridLayoutManager for teams)
        teamsList = new ArrayList<>();
        teamsAdapter = new TeamAdapter(teamsList);
        teamsListGrid.setAdapter(teamsAdapter);
        teamsListGrid.setLayoutManager(new GridLayoutManager(getContext(), 1)); // Grid layout with 1 column


        // Load classes for the teacher and set up the class spinner
        loadClasses();

        //On Start Game Button Click
        startGameButton.setOnClickListener(v -> startGame());

        return view;
    }

    // Method to start the game
    private void startGame() {
        if (GameActive != true) {
            if (classSpinner.getSelectedItem() != null){

                // Get the selected timer as a String and convert it to an int
                String selectedTimerString = timerSpinner.getSelectedItem().toString();
            String selectedMode = modeSpinner.getSelectedItem().toString();
            String selectedTopic = topicSpinner.getSelectedItem().toString();
            String selectedClass = classSpinner.getSelectedItem().toString(); // Get the selected class

            Log.d(TAG, "IS CLASS SPINNER NULL? " + classSpinner.getSelectedItem());


            // Check if in "Team Mode" and there are no teams
            if ("Team Mode".equals(selectedMode) && (teamsList == null || teamsList.isEmpty())) {
                showNoTeamsDialog();  // Show dialog if no teams exist in "Team Mode"
                return;
            }

            // Check if the selected class has any students
            checkIfClassHasStudents(selectedClass, hasStudents -> {
                if (!hasStudents) {
                    showNoStudentsDialog(); // Show dialog if no students are in the class
                } else {
                    // Proceed to create the game since there are students in the class
                    GameActive = true;//While this is true the teacher needs to not be able to do any functions in the classroom fragment
                    Log.d(TAG, "NEW GAME CREATED WITH: " + selectedClass);
                    createNewGame(selectedTimerString, selectedMode, selectedClass, selectedTopic);
                    startDrawlingGameActivity();
                }
            });
        }
            else{
                showNoClassesDialog();
            }
        }
        else{//temporary. Wil need to make it so they go back to active game
            Toast.makeText(getContext(), "Game is already active", Toast.LENGTH_SHORT).show();
        }
    }



    private void startDrawlingGameActivity() {
        Intent intent = new Intent(getActivity(), DrawlingGameActivity.class);
        startActivity(intent);
    }



    // Method to check if a class has students
    private void checkIfClassHasStudents(String selectedClass, final OnStudentsCheckCompleteListener listener) {
        firestore.collection("teachers").document(Online_user_id)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Map<String, Object> classData = (Map<String, Object>) document.get("classes");
                            if (classData != null && classData.containsKey(selectedClass)) {
                                // Retrieve the array of students in the selected class
                                List<Map<String, Object>> studentsList = (List<Map<String, Object>>) classData.get(selectedClass);

                                // Check if the students list is not empty
                                if (studentsList != null && !studentsList.isEmpty()) {
                                    listener.onCheckComplete(true); // Students exist in the class
                                    return;
                                }
                            }
                        }
                    }
                    listener.onCheckComplete(false);  // No students found or error
                });
    }

    // Callback interface to check if class has students
    private interface OnStudentsCheckCompleteListener {
        void onCheckComplete(boolean hasStudents);
    }

    // Dialog to show if no students are found in the class
    private void showNoStudentsDialog() {
        new AlertDialog.Builder(getActivity())
                .setTitle("No Students")
                .setMessage("The selected class has no students. Please add students to the class before starting the game.")
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    // Create a new game with timer, mode, and class
    private void createNewGame(String timer, String mode, String selectedClass, String selectedTopic) {//move to DrawlingGameActivity?

        String gameId = "game_" + System.currentTimeMillis(); // Example game ID
        DocumentReference teacherDocRef = firestore.collection("teachers").document(teacherName);

        //prepare data for game
        Map<String, Object> gameData = new HashMap<>();
        gameData.put("activeGame", gameId); //Update the active game for this teacher
        gameData.put("mode", mode);
        gameData.put("class", selectedClass); //Add selected class
        gameData.put("topic", selectedTopic);

        //Update Firestore with the new game details
        teacherDocRef.update(gameData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Firestore updated with active game: " + gameId);

                    // After the data is successfully set, start the drawing game activity
                    startDrawlingGameActivity(timer, mode, selectedClass, selectedTopic);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error updating Firestore", e);
                    Toast.makeText(getContext(), "Failed to start game", Toast.LENGTH_SHORT).show();
                });


    }

    // Method to start DrawlingGameActivity and pass the necessary data
    private void startDrawlingGameActivity(String timer, String mode, String selectedClass, String selectedTopic) {
        Intent intent = new Intent(getActivity(), DrawlingGameActivity.class);
        intent.putExtra("timer", timer);
        intent.putExtra("mode", mode);
        intent.putExtra("class", selectedClass);
        intent.putExtra("topic", selectedTopic);
        startActivity(intent);
    }


    // Load classes for the teacher into the class spinner
    private void loadClasses() {
        DocumentReference teacherDocRef = firestore.collection("teachers")
                .document(teacherName);

        teacherDocRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> teacherData = documentSnapshot.getData();

                        if (teacherData != null) {
                            // Assuming class names are stored in a "classes" field in the teacher document
                            Map<String, Object> classes = (Map<String, Object>) teacherData.get("classes");

                            if (classes != null) {
                                // Populate class spinner with class names
                                String[] classArray = classes.keySet().toArray(new String[0]);
                                ArrayAdapter<String> classAdapter = new ArrayAdapter<>(getContext(),
                                        android.R.layout.simple_spinner_item, classArray);
                                classAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                classSpinner.setAdapter(classAdapter);

                                // Update teams based on selected class
                                classSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                    @Override
                                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                        String selectedClass = classArray[position];
                                        getTeamsClass(selectedClass);  // Load teams for the selected class
                                    }

                                    @Override
                                    public void onNothingSelected(AdapterView<?> parent) {
                                        // Do nothing
                                    }
                                });
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Log.w(TAG, "Error getting document", e));
    }

    private void getTeamsClass(String className) {
        if (Online_user_id != null) {
            dbHelper.getTeacherClasses(Online_user_id, classes -> {
                if (classes == null) {
                    // Show dialog or handle the case where no classes are found
                    teamsList.clear(); // Clear teams list if no classes or error
                    teamsAdapter.notifyDataSetChanged(); // Notify adapter that data has changed
                    return; // Exit the method
                }
                firestore.collection("teachers")
                        .document(Online_user_id)
                        .get()
                        .addOnCompleteListener(task_get_teams -> {
                            if (task_get_teams.isSuccessful()) {
                                DocumentSnapshot document = task_get_teams.getResult();
                                if (document.exists()) {
                                    Map<String, Object> classTeams = (Map<String, Object>) document.get("class_teams");
                                    if (classTeams != null && classTeams.containsKey(className)) {
                                        Log.d(TAG, "Class Teams found for class: " + className);
                                        Map<String, Object> teamsData = (Map<String, Object>) classTeams.get(className);
                                        if (teamsData != null && teamsData.containsKey("teams")) {
                                            Log.d(TAG, "Teams data found: " + teamsData);

                                            // Extract teams for the given class
                                            Map<String, List<Map<String, String>>> teamNameMap = (Map<String, List<Map<String, String>>>) teamsData.get("teams");

                                            if (teamNameMap == null || teamNameMap.isEmpty()) {
                                                Log.d(TAG, "No teams found for class: " + className);
                                                // Clear the teams list and notify the adapter
                                                teamsList.clear();
                                                teamsAdapter.notifyDataSetChanged();
                                            } else {
                                                teamsList.clear();
                                                for (Map.Entry<String, List<Map<String, String>>> entry : teamNameMap.entrySet()) {
                                                    String teamName = entry.getKey();
                                                    List<Map<String, String>> studentMaps = entry.getValue();

                                                    List<String> studentNames = new ArrayList<>();
                                                    for (Map<String, String> studentMap : studentMaps) {
                                                        String studentName = studentMap.get("name");
                                                        if (studentName != null) {
                                                            studentNames.add(studentName);
                                                        }
                                                    }

                                                    // Create a Team object and add it to the teamsList
                                                    Team team = new Team(teamName, studentNames);
                                                    teamsList.add(team);
                                                }
                                                // Notify adapter of changes
                                                teamsAdapter.notifyDataSetChanged();
                                            }
                                        } else {
                                            Log.e(TAG, "No 'teams' field found for class: " + className);
                                            teamsList.clear();
                                            teamsAdapter.notifyDataSetChanged();
                                        }
                                    } else {
                                        Log.e(TAG, "No teams data found for class: " + className);
                                        teamsList.clear();
                                        teamsAdapter.notifyDataSetChanged();
                                    }
                                } else {
                                    Log.d(TAG, "No such document exists for Teacher: " + Online_user_id);
                                }
                            } else {
                                Log.e(TAG, "Error getting document: ", task_get_teams.getException());
                            }
                        });
            });
        }
    }

    private void showNoClassesDialog() {//prevents error if no classes exist for teacher
        new AlertDialog.Builder(getActivity()) //Use 'this' if in Activity; replace with 'getActivity()' in Fragment
                .setTitle("No Classes")
                .setMessage("You don't have any classes yet. Please create one to start a game.")
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    // Show a dialog when no teams exist in "Team Mode"
    private void showNoTeamsDialog() {
        new AlertDialog.Builder(getActivity())
                .setTitle("No Teams")
                .setMessage("You don't have any teams in the selected class. Please make teams in your class to start the game in Team Mode.")
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

}
