//Needs a delete game button that also resets the values in teacher collection field back to null
package com.example.capstone;

import static com.example.capstone.FS_DBHelper.Online_user_id;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import com.google.firebase.database.FirebaseDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class TeacherGameFragment extends Fragment {
    public static boolean GameActive;

    private Spinner timerSpinner;
    private Spinner modeSpinner;
    private Spinner classSpinner;

    private Spinner topicSpinner;

    private Spinner roundSpinner;
    private Button startGameButton;
    private Button deleteGameDataButton;

    private RecyclerView teamsListGrid;

    private FirebaseFirestore firestore;
    private FirebaseDatabase realtimeDb;
    private FS_DBHelper dbHelper;

    private List<Team> teamsList;
    private TeamAdapter teamsAdapter;


    private static final String TAG = "TeacherGameFragment";
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

        deleteGameDataButton = view.findViewById(R.id.delete_game_data_button);
        startGameButton = view.findViewById(R.id.start_game_button);

        teamsListGrid = view.findViewById(R.id.teamsList);
        roundSpinner = view.findViewById(R.id.round_spinner);
        //include round

        // Set up spinners
        setUpSpinners();


        //Set up RecyclerView (GridLayoutManager for teams)
        setUpTeamsListGrid();

        // Load classes for the teacher and set up the class spinner
        loadClasses();

        //On Start Game Button Click
        startGameButton.setOnClickListener(v -> startGame());

        deleteGameDataButton.setOnClickListener(v -> getActiveGameIdForDeletion());



        return view;
    }

    private void setUpSpinners() {
        // Timer Spinner
        ArrayAdapter<CharSequence> timerAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.timer_options, android.R.layout.simple_spinner_item);
        timerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        timerSpinner.setAdapter(timerAdapter);

        // Mode Spinner
        ArrayAdapter<CharSequence> modeAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.mode_options, android.R.layout.simple_spinner_item);
        modeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modeSpinner.setAdapter(modeAdapter);

        // Topic Spinner
        ArrayAdapter<CharSequence> topicAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.topic_options, android.R.layout.simple_spinner_item);
        topicAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        topicSpinner.setAdapter(topicAdapter);

        //Round Spinner
        ArrayAdapter<CharSequence> roundAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.round_options, android.R.layout.simple_spinner_item);
        roundAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roundSpinner.setAdapter(roundAdapter);
    }

    private void setUpTeamsListGrid() {
        teamsList = new ArrayList<>();
        teamsAdapter = new TeamAdapter(teamsList);
        teamsListGrid.setAdapter(teamsAdapter);
        teamsListGrid.setLayoutManager(new GridLayoutManager(getContext(), 1)); // Grid layout with 1 column
    }



    /*Start Game Button stuff*/
    // Method to start the game
    private void startGame() {
            if (classSpinner.getSelectedItem() != null) {

                checkIfGameCanBeStarted();
            } else {
                showNoClassesDialog();
            }

    }

    // Method to check if a game can be started
    private void checkIfGameCanBeStarted() {
        firestore.collection("teachers")
                .document(teacherName)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.contains("activeGame")) {
                        String activeGameId = documentSnapshot.getString("activeGame");

                        // If active game exists, show prompt to delete it
                        if (activeGameId != null) {
                            Toast.makeText(getContext(), "An active game exists. Delete the current game before starting a new one.", Toast.LENGTH_LONG).show();
                        } else {
                            // No active game, proceed with starting a new one
                            createNewGameFlow();
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error checking active game status: ", e));
    }

    // Create a new game with timer, mode, and class
    private void createNewGame( String timer, String mode, String selectedClass, String selectedTopic, String selectedRounds) {//move to DrawingGameActivity?

        String gameId = "game_" + System.currentTimeMillis(); // Example game ID
        DocumentReference teacherDocRef = firestore.collection("teachers").document(teacherName);

        //prepare data for game
        Map<String, Object> gameData = new HashMap<>();
        gameData.put("activeGame", gameId); //Update the active game for this teacher
        //gameData.put("mode", mode);
        gameData.put("last_class_played_with", selectedClass); //Add selected class
        //gameData.put("topic", selectedTopic);
        //gameData.put("timer", timer);
        //gameData.put("rounds", selectedRounds);

        // Select the first drawer for each team
        Map<String, String> teamDrawers = selectFirstDrawersForTeams();
        if (teamDrawers.isEmpty()) {
            // If the team validation failed, stop the game creation process
            GameActive = false;

            return;
        }


        //Update teachers data in database with active_game and with what class
        teacherDocRef.update(gameData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Game started: " + gameId);


                    // After the data is successfully set, start the drawing game activity
                    uploadGameDataToFirestore(gameId, timer, mode, selectedClass, selectedTopic, Online_user_id, selectedRounds, teamDrawers);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error starting game", e);
                    Toast.makeText(getContext(), "Failed to start game", Toast.LENGTH_SHORT).show();
                });


    }

    // Method to randomly select a student from each team, ensuring each team has at least two students
    private Map<String, String> selectFirstDrawersForTeams() {
        Map<String, String> teamDrawers = new HashMap<>();

        for (Team team : teamsList) {
            List<String> studentNames = team.getStudentNames();
            Log.d("GameFragment", "Number of students in team " + team.getTeamName() + ": " + studentNames.size());
            // Ensure the team has at least two students
            if (studentNames.size() < 2) {
                // If any team has fewer than two members, show a message and return an empty map to prevent the game from starting
                Toast.makeText(getContext(), "Team " + team.getTeamName() + " must have at least two members!", Toast.LENGTH_LONG).show();
                GameActive = false;
                return new HashMap<>(); // Return an empty map to signal an issue
            }

            // Randomly select a student as the drawer
            int randomIndex = new Random().nextInt(studentNames.size());
            String selectedDrawer = studentNames.get(randomIndex);
            teamDrawers.put(team.getTeamName(), selectedDrawer);
        }

        return teamDrawers;
    }

    // Method to upload game data to Firestore
    private void uploadGameDataToFirestore(String gameId, String timer, String mode, String selectedClass, String selectedTopic, String teacherName, String selectedRounds, Map<String, String> teamDrawers) {
        // Create a map to store game data
        Map<String, Object> gameData = new HashMap<>();
        gameData.put("timer", timer);
        gameData.put("mode", mode);
        gameData.put("class", selectedClass);
        gameData.put("topic", selectedTopic);
        gameData.put("teacherName", teacherName);
        gameData.put("rounds", selectedRounds);
        gameData.put("teamDrawers", teamDrawers); // Add the selected drawers to the game data

        // Initialize GusseButtonQueue and other data structures
        Map<String, List<String>> GuessButtonQueue = new HashMap<>();
        Map<String, List<Integer>> timeLeftInTeamGame = new HashMap<>();
        Map<String, List<Integer>> roundInEachTeam = new HashMap<>();
        Map<String, List<String>> lastRoundInEachTeam = new HashMap<>();
        Map<String, List<String>> currentWordInEachTeam = new HashMap<>();

        int timerInt = Integer.parseInt(timer); // Convert timer to int

        for (Team team : teamsList) {
            GuessButtonQueue.put(team.getTeamName(), new ArrayList<>()); // Initialize with empty list for each team

            List<Integer> timerList = new ArrayList<>(); // Array for the times for each team game
            timerList.add(timerInt); // Add the timer value to the list

            List<Integer> teamsRound = new ArrayList<>(); // Array for the round that each team game is on
            teamsRound.add(1); // Start with round 1

            List<String> lastRound = new ArrayList<>();
            lastRound.add("Round: 1");

            List<String> currentWord = new ArrayList<>();
            currentWord.add("");

            Log.d("GameData", "Uploading timer value: " + timerInt);
            timeLeftInTeamGame.put(team.getTeamName(), timerList);
            roundInEachTeam.put(team.getTeamName(), teamsRound);
            lastRoundInEachTeam.put(team.getTeamName(), lastRound);
            currentWordInEachTeam.put(team.getTeamName(), currentWord);
        }
        gameData.put("GuessButtonQueue", GuessButtonQueue); // Add GusseButtonQueue to the game data
        gameData.put("timeLeftInTeamGame", timeLeftInTeamGame); // Add timeLeftInTeamGame to the game data
        gameData.put("roundInEachTeam", roundInEachTeam); // Add roundInEachTeam to the game data
        gameData.put("currentWordInEachTeam", currentWordInEachTeam); // Add currentWordInEachTeam to the game data
        gameData.put("lastRoundInEachTeam", lastRoundInEachTeam); // Add lastRoundInEachTeam to the game data

        // Upload to Firestore
        firestore.collection("games").document(gameId).set(gameData)
                .addOnSuccessListener(aVoid -> {
                    // Data uploaded, redirect players to the game
                    // Update activeGame field for all students in the teams
                    updateStudentsActiveGameAndScore(gameId); // Pass gameId to players
                    Toast.makeText(getContext(), "Game started with class " + selectedClass + "!", Toast.LENGTH_SHORT).show();
                    Log.d("Firestore", "Game data uploaded/created successfully " + gameId);

                    // Increment the round for each team
                    for (Team team : teamsList) {
                        updateRound(team.getTeamName(), gameId);
                    }

                })
                .addOnFailureListener(e -> {
                    Log.w("Firestore", "Error uploading game data", e);
                });
    }



    //Increment each roundInEachTeam due to firestore storing numbers as long and not int
    private void updateRound(String teamName, String gameId) {
        firestore.collection("games").document(gameId)
                .update("roundInEachTeam." + teamName, FieldValue.increment(1))
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Round for team " + teamName + " updated successfully."))
                .addOnFailureListener(e -> Log.e("FirestoreError", "Error updating round for team " + teamName, e));
    }






    // Method to delete the current game and reset the teacher's activeGame field
    private void getActiveGameIdForDeletion() {
        firestore.collection("teachers")
                .document(teacherName)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.contains("activeGame")) {
                        String activeGameId = documentSnapshot.getString("activeGame");

                        if (activeGameId != null) {
                            // Fetch the class name associated with the active game
                            getClasNameOfLastGame(activeGameId); // Show the delete confirmation dialog

                           // deleteGameData(activeGameId); // Call method to delete game data
                           // clearTeachersActiveGame(); // Clear the teacher's activeGame field
                        } else {
                            Toast.makeText(getContext(), "No active game found to delete.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "No active game found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching active game for deletion", e));
    }

    private void getClasNameOfLastGame(String activeGameId) {
        firestore.collection("games")
                .document(activeGameId)
                .get()
                .addOnCompleteListener(task_getLastClassName -> {
                    if (task_getLastClassName.isSuccessful() && task_getLastClassName.getResult() != null) {
                        DocumentSnapshot document = task_getLastClassName.getResult();
                        if (document.exists() && document.get("class") != null) {
                            String lastClassPlayedWith = document.getString("class");
                            showDeleteConfirmationDialog(activeGameId, lastClassPlayedWith); // Pass class name to dialog
                        } else {
                            showDeleteConfirmationDialog(activeGameId, "Unknown Class"); // Default if class name is not found
                        }
                    } else {
                        showDeleteConfirmationDialog(activeGameId, "Unknown Class"); // Handle failure case
                    }
                });
    }

    //clear teachers active game field
    private void clearTeachersActiveGame() {
        firestore.collection("teachers")
                .document(teacherName)
                .update("activeGame", null)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Active game field cleared successfully for teacher: " + teacherName))
                .addOnFailureListener(e -> Log.e("FirestoreError", "Error clearing active game field for teacher: " + teacherName, e));
    }



    private void createNewGameFlow() {
        String selectedTimer = timerSpinner.getSelectedItem().toString();
        String selectedMode = modeSpinner.getSelectedItem().toString();
        String selectedTopic = topicSpinner.getSelectedItem().toString();
        String selectedClass = classSpinner.getSelectedItem().toString();
        String selectedRounds = roundSpinner.getSelectedItem().toString();

        // Check for teams in "Team Mode"
        if ("Team Mode".equals(selectedMode) && (teamsList == null || teamsList.isEmpty())) {
            showNoTeamsDialog();
            return;
        }

        // Verify if the class has students
        checkIfClassHasStudents(selectedClass, hasStudents -> {
            if (!hasStudents) {
                showNoStudentsDialog();
            } else {
                GameActive = true; // Prevent starting multiple games
                createNewGame(selectedTimer, selectedMode, selectedClass, selectedTopic, selectedRounds);
            }
        });
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
                                List<Map<String, Object>> studentsList = (List<Map<String, Object>>) classData.get(selectedClass);
                                listener.onCheckComplete(studentsList != null && !studentsList.isEmpty());
                                return;
                            }
                        }
                    }
                    listener.onCheckComplete(false);
                });
    }

    private interface OnStudentsCheckCompleteListener {
        void onCheckComplete(boolean hasStudents);
    }


    // Load classes for the teacher into the class spinner
    private void loadClasses() {
        DocumentReference teacherDocRef = firestore.collection("teachers").document(teacherName);
        teacherDocRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> teacherData = documentSnapshot.getData();

                        if (teacherData != null) {
                            // class names are stored in a "classes" field in the teacher document
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

    private void updateStudentsActiveGameAndScore(String gameId) {
        // Loop through each team in the teamsList
        for (Team team : teamsList) {
            List<String> studentNames = team.getStudentNames(); // Get the list of students in the team

            // For each student in the team, update their activeGame field
            for (String studentName : studentNames) {
                DocumentReference studentDocRef = firestore.collection("students").document(studentName);
                studentDocRef.update("activeGame", gameId, "score", 0)//update score to 0 when starting a new game and update activeGame
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Updated activeGame for student: " + studentName);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to update activeGame for student: " + studentName, e);
                        });
            }
        }
    }


    private void deleteGameData(String gameId) {
        // Initialize Firestore and Realtime Database instances
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DatabaseReference realtimeDb = FirebaseDatabase.getInstance().getReference();

        // Delete game data from Firestore
        db.collection("games").document(gameId).delete()
                .addOnSuccessListener(aVoid -> {
                    // Now delete the game data from Realtime Database
                    realtimeDb.child("games").child(gameId).removeValue()
                            .addOnSuccessListener(aVoid2 -> {
                                Log.d("RealtimeDatabase", "Game data deleted from Realtime Database!");
                                GameActive = false;
                            })
                            .addOnFailureListener(e -> {
                                Log.w("RealtimeDatabase", "Error deleting game data from Realtime Database", e);
                            });

                    Log.d("Firestore", "Game data deleted from Firestore!");
                })
                .addOnFailureListener(e -> {
                    Log.w("Firestore", "Error deleting game data from Firestore", e);
                });
    }



    private void showDeleteConfirmationDialog(String activeGameId, String lastClassPlayedWith) {
        if (!lastClassPlayedWith.equals("Unknown Class")) {
            new AlertDialog.Builder(getActivity())

                    .setTitle("Delete Previous Game?")
                    .setMessage("An active game exists for the class \"" + lastClassPlayedWith + "\". Starting a new game will delete the previous game data. Do you want to continue?")
                    .setPositiveButton("Yes", (dialog, which) -> {

                        //Delete the old game data
                        deleteGameData(activeGameId);

                        //Clear the teacher's activeGame field
                        clearTeachersActiveGame();


                    })
                    .setNegativeButton("No", null) // Do nothing if the user chooses No
                    .show();
        } else {
            Log.d("Firestore", "Unknown Class found.");
        }
    }



    // Dialog to show if no students are found in the class
    private void showNoStudentsDialog() {
        new AlertDialog.Builder(getActivity())
                .setTitle("No Students")
                .setMessage("The selected class has no students. Please add students to the class before starting the game.")
                .setPositiveButton(android.R.string.ok, null)
                .show();
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
