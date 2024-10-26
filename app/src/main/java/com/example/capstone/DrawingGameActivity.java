
package com.example.capstone;

import static com.example.capstone.FS_DBHelper.Online_user_id;

import androidx.appcompat.app.AlertDialog;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class DrawingGameActivity extends AppCompatActivity {
    private static final String TAG = "DrawingGameActivity";
    public static String getGameId;

    private DrawingView drawingView;
    private TextView vocabWordTextView, timerTextView, scoreTextView, roundTextView, roundView, drawerTextView, drawerNameTextView, topicTextView, topicView;
    private Button guessButton;
    private ProgressBar progressBar;

    private String roundText;
    private FirebaseFirestore firestore;

    private boolean CheckGuessQueue = true;


    // Keep track of processed guesses
    private Set<String> processedGuessers = new HashSet<>();

    private FirebaseDatabase database;
    private DatabaseReference vocabularyWordRef;

    private DatabaseReference japaneseMeaningRef;

    private DatabaseReference strokesRef;
    private String gameId;
    private String mode;
    private String selectedClass;

    private List<String> userTeammates;
    private String selectedTopic;
    private String teacherName;
    private String currentDrawerId;

    private String currentDrawer;
    private String correctWord;
    //private int currentTeamScore = 0;

    private Map<String, List<String>> currentGuessersMap = new HashMap<>();

    private Map<String, Integer> teamScores = new HashMap<>(); // Map to track scores for each team
    private int selectedRound;

    private boolean userIsDrawer;

    private int currentRound = 0;

    //private int currentTeam = 0; // 0 for Team A, 1 for Team B

    private int remainingTime;
    private int currentTeamScore = 0;

    private int currentTeamIndex = 0; // Index of the current team

    private int roundtime; //Class variable for round time
    private Handler timerHandler;
    private Runnable timerRunnable;

    //private List<String> studentNames = new ArrayList<>();

    private List<String> userTeammatesDrawerRemoved = new ArrayList<>();

    private List<VocabularyItem> randomizedWords = new ArrayList<>(); // To store randomized words
    private List<VocabularyItem> vocabWordsList;
    private VocabularyItem correctWordItem;

    //private boolean wordGuessed = false;

    private Map<String, String> currentDrawerMap = new HashMap<>();

    private Map<String, String> studentTeamMap = new HashMap<>();

    private List<String> teamNames; // Holds just the team names

    private String teamName;
    private List<Team> teamList = new ArrayList<>(); // Holds Team objects with members
    //store the guess listener registration
    private ListenerRegistration guessListener;

    //store the guess queue listener
    private ListenerRegistration guessQueueListener;

    private ListenerRegistration wordListener;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drawling_game);

        // Initialize UI components
        drawingView = findViewById(R.id.drawingView);
        vocabWordTextView = findViewById(R.id.vocabWordTextView);
        timerTextView = findViewById(R.id.timerTextView);
        scoreTextView = findViewById(R.id.scoreTextView);
        guessButton = findViewById(R.id.guessButton);
        roundTextView = findViewById(R.id.roundTextView);
        //roundView = findViewById(R.id.roundView);
        drawerTextView = findViewById(R.id.drawerTextView);
        drawerNameTextView = findViewById(R.id.drawerNameTextView);
        progressBar = findViewById(R.id.progressBar);
        topicTextView = findViewById(R.id.topicTextView);
        topicView = findViewById(R.id.topicView);


        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        database = FirebaseDatabase.getInstance();
        // Get the passed data from TeacherGameFragment




        String gameId = getIntent().getStringExtra("studentGameStatus");
        Log.d("DrawingGameActivity", "Received through intent gameId: " + gameId);



        if (gameId != null) {
            //strokesRef = database.getReference("games").child(gameId).child("strokes");//get strokes reference from realtime database
            setGameId(gameId);
        } else {
            Toast.makeText(this, "Student Game Status not found", Toast.LENGTH_SHORT).show();
            goto_Student_Game_Fragment();
        }

        // Initialize Firebase Database reference
        //databaseReference = FirebaseDatabase.getInstance().getReference("vocabulary");


        // Retrieve game data from Firestore
        if (gameId != null) {
            firestore.collection("games").document(gameId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            // Extract data
                            roundtime = Integer.parseInt(documentSnapshot.getString("timer"));
                            mode = documentSnapshot.getString("mode");
                            selectedClass = documentSnapshot.getString("class");
                            selectedTopic = documentSnapshot.getString("topic");
                            teacherName = documentSnapshot.getString("teacherName");

                            Log.d("Firestore", "teachers Name: " + teacherName);

                            selectedRound = Integer.parseInt(documentSnapshot.getString("rounds"));

                            // Set up game details
                            setupGame(selectedTopic, teacherName, selectedClass);

                            // Initialize the game with the retrieved data
                            // Use timer, mode, class, topic, and teacherName as needed
                        } else {
                            Log.d("Firestore", "No such document!");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.w("Firestore", "Error retrieving game data", e);
                    });
        }
        else {
            //Handle the case where studentGameStatus is not passed
            Toast.makeText(this, "Student Game Status not found", Toast.LENGTH_SHORT).show();
            goto_Student_Game_Fragment();
        }



    }

    // Override onBackPressed to disable the back button
    @Override
    @SuppressLint("MissingSuperCall")
    //prevent student from exiting game
    public void onBackPressed() {
        // Prevent the back button from exiting the activity
        Toast.makeText(this, "You cannot exit the game at this stage.", Toast.LENGTH_SHORT).show();
    }

    //For loading animation
    private void startLoadingAnimation() {
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setIndeterminate(true);  // Start animation
    }

    //Stop  loading animation
    private void stopLoadingAnimation() {
        progressBar.setVisibility(View.GONE);
        progressBar.setIndeterminate(false);  // Stop animation
    }


    public void setGameId(String gameId) {
        this.gameId = gameId;
    }


    private void goto_Student_Game_Fragment() {//return to student game fragment

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.directory_container, new StudentGameFragment());
        fragmentTransaction.commit();
    }

    private void setupGame(String selectedTopic, String teacherName, String selectedClass) {
        topicView.setText(selectedTopic);// Show selected topic in the UI
        // Fetch teams from Firestore
        getScore(Online_user_id);
        fetchTeams(selectedTopic ,teacherName, selectedClass, gameId);

        //getDrawer(); //THIS IS CALLED IN fetchTEAMS. Select a drawer after fetching teams
        // Proceed with game logic after assigning drawers and guessers
        //getRandomizedWords(selectedTopic); moved to getDrawer()
        //initializeDrawing();//moved to getDrawer()

    }

    private void fetchTeams(String selectedTopic, String teacherName, String selectedClass, String gameId) {//Gets students team name and team members
        if (teacherName == null || selectedClass == null) {
            Log.e(TAG, "teacherName or selectedClass is null.");
            return; // Prevent further execution if the values are null
        }

        firestore.collection("teachers")
                .document(teacherName)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot teacherDoc = task.getResult();
                        if (teacherDoc.exists()) {
                            // Access the class_teams map
                            Map<String, Object> classTeams = (Map<String, Object>) teacherDoc.get("class_teams");
                            if (classTeams != null && classTeams.containsKey(selectedClass)) {
                                // Get the specified classroom's data
                                Map<String, Object> classroomMap = (Map<String, Object>) classTeams.get(selectedClass);

                                if (classroomMap != null && classroomMap.containsKey("teams")) {
                                    // Get the teams map
                                    Map<String, List<Map<String, String>>> teamsMap = (Map<String, List<Map<String, String>>>) classroomMap.get("teams");

                                    if (teamsMap != null) {
                                        teamNames = new ArrayList<>();
                                        studentTeamMap.clear(); // Clear previous mappings

                                        // Iterate over the teams and students
                                        for (Map.Entry<String, List<Map<String, String>>> entry : teamsMap.entrySet()) {
                                            teamName = entry.getKey();
                                            List<Map<String, String>> studentMaps = entry.getValue();
                                            List<String> studentNames = new ArrayList<>();

                                            // Populate student names for this team
                                            for (Map<String, String> studentMap : studentMaps) {
                                                String studentName = studentMap.get("name");
                                                if (studentName != null) {
                                                    studentNames.add(studentName);
                                                    studentTeamMap.put(studentName, teamName); // Map student to their team
                                                }
                                            }

                                            // Check if the online_user_id is part of this team
                                            if (studentNames.contains(Online_user_id)) {
                                                // Log the user's team and teammates for debugging
                                                Log.d(TAG, "User " + Online_user_id + " is in team: " + teamName);

                                                // Assign the full team (including the user) to userTeammates
                                                userTeammates = new ArrayList<>(studentNames);//TEAMMATES
                                                Log.d(TAG, "Teammates: " + userTeammates);


                                                //Now that you have the teamName, set strokesRef
                                                strokesRef = database.getReference("games")
                                                        .child(gameId)
                                                        .child(teamName)
                                                        .child("strokes");

                                                Log.d(TAG, "strokesRef initialized to: " + strokesRef.toString());

                                                // Pass the teamName to DrawingView
                                                drawingView.setTeamName(teamName);
                                                drawingView.setGameId(gameId);  // Pass gameId to DrawingView

                                                // Call getDrawer here after fetching teams and pass the user teammates and team name
                                                getLastRoundForTeam(teamName, gameId);//Is student accidentally backs out of the game the round corrected
                                                getDrawer(selectedTopic, userTeammates, teamName, gameId); //Pass teammates and team name

                                                //No need to continue searching other teams after the user is found
                                                break;
                                            }

                                            // Add the team and its students to the list
                                            teamList.add(new Team(teamName, userTeammates)); // Store team and its students
                                            teamNames.add(teamName); // Store team name

                                        }

                                        if (userTeammates == null || userTeammates.isEmpty()) {
                                            Log.e(TAG, "Online user " + Online_user_id + " not found in any team.");
                                        }



                                    } else {
                                        Log.e(TAG, "No teams found for classroom: " + selectedClass);
                                    }
                                } else {
                                    Log.e(TAG, "Classroom data is missing teams: " + selectedClass);
                                }
                            } else {
                                Log.e(TAG, "Teacher document does not exist.");
                            }
                        } else {
                            Log.e(TAG, "Error fetching teacher data: ", task.getException());
                        }
                    }
                });
    }
    private void handleGuessButtonClick() {
        // Use the team name from userTeammates or the specific team you need
        if (userTeammates != null && !userTeammates.isEmpty()) {

            String teamName = studentTeamMap.get(Online_user_id); // Get the team name for the online user

            // Show a dialog or navigate to the guessing activity
            showGuessDialog(teamName);
        } else {
            Toast.makeText(this, "error", Toast.LENGTH_SHORT).show();
        }
    }
    //guessers guess dialog
    private void showGuessDialog(String teamName) {

        //Check if the activity is finishing or destroyed to prevent crash
        if (isFinishing() || isDestroyed()) {
            return; //Don't show the dialog if the activity is not valid
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Update the database as soon as the dialog is shown
        if (teamName != null) {
            firestore.collection("games").document(gameId)
                    .update("GuessButtonQueue." + teamName, FieldValue.arrayUnion(Online_user_id))
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "User added to the queue successfully.");
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error adding user to queue", e);
                    });
        }

        builder.setTitle("Tell the Drawer Your Guess!")
                .setMessage("I think the word is...")
                .setPositiveButton("Done", (dialog, which) -> {
                    // Disable the guess button when done
                    guessButton.setEnabled(false);
                    dialog.dismiss(); // Dismiss the dialog after pressing Done
                })
                .setCancelable(false); // Allow the dialog to not be cancelable by back button

        // Create the dialog instance
        AlertDialog dialog = builder.create();

        // Allow the dialog to not be canceled when the user taps outside
        dialog.setCanceledOnTouchOutside(false);

        // Show the dialog
        dialog.show();

        pauseGame();

        // Start checking the guess queue for the user
        checkGuessQueueForUser(teamName);
        // check current round to check if roles need to be swapped
        // Gets called to soon need to be called while dialog is open
    }

    private void checkGuessQueueForUser(String teamName) {
        CheckGuessQueue = true;

        // Remove any previous listener before adding a new one
        if (guessQueueListener != null) {
            guessQueueListener.remove();  // Detach the old listener
        }

        // Start listening for changes in the GuessButtonQueue for the specific team
        guessQueueListener = firestore.collection("games")
                .document(gameId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        Log.e("FirestoreError", "checkGuessQueueForUser listener failed.", e);
                        return;
                    }
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        List<String> guessQueue = (List<String>) documentSnapshot.get("GuessButtonQueue." + teamName);
                        Log.d("Firestore checkGuessQueueForUser", "GuessQueue: " + guessQueue);

                        if (guessQueue != null) {
                            if (!guessQueue.contains(Online_user_id)) {
                                // Once the user is no longer in the queue, stop checking and allow the round to update
                                Log.d("checkGuessQueueForUser", "User " + Online_user_id + " is no longer in guess queue. Proceeding with getCurrentRound.");
                                CheckGuessQueue = false;

                                // Clean up the listener once no longer needed
                                if (guessQueueListener != null) {
                                    guessQueueListener.remove();
                                    guessQueueListener = null;
                                }

                                // Now call getCurrentRound to proceed with the game
                                Log.d("checkGuessQueueForUser", "Calling getCurrentRound.");
                                getCurrentRound();

                            } else if (CheckGuessQueue) {
                                // If the user is still in the queue, retry after a short delay
                                Log.d("checkGuessQueueForUser", "User " + Online_user_id + " is still in the queue. Retrying in 3 seconds.");
                                retryCheckGuessQueueForUser(teamName);  // Retry after a delay
                            }
                        }
                    }
                });
    }


    private void retryCheckGuessQueueForUser(String teamName) {
        if  (CheckGuessQueue) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                checkGuessQueueForUser(teamName); // Recheck the queue after 3 seconds
            }, 3000); // 3-second delay before rechecking
        }
        else {
            Log.d("retryCheckGuessQueueForUser", "Retry stopped as CheckGuessQueue is false.");
        }
    }



    private void getDrawer(String selectedTopic, List<String> userTeammates, String teamName, String gameId) {


        // Check if Online_user_id is a drawer based on teamName
        checkIfUserIsDrawler(teamName, isDrawler -> {
            if (isDrawler) {
                Log.d(TAG, "User " + Online_user_id + " is the drawer for team: " + teamName);
                guessButton.setVisibility(View.GONE);
                currentDrawer = Online_user_id;
                userIsDrawer = true;
                drawerNameTextView.setText(currentDrawer);//updates UI with name of drawer
                //Randomly select a drawer from each team
                getRandomizedWords(selectedTopic);
                userTeammatesDrawerRemoved = userTeammates;
                userTeammatesDrawerRemoved.remove(Online_user_id);//remove drawer from list of students so he doesn't get picked again consecutively for selectNewDrawer()
                //Start listening for guesses only now that the user is the drawer
                listenForGuesses(gameId, teamName);

            } else {
                Log.d(TAG, "User " + Online_user_id + " is NOT the drawer for team: " + teamName);
                getDrawler(drawer -> {
                    Log.d(TAG, "Drawer: " + drawer);
                    if (drawer != null) {
                        currentDrawer = drawer; // Update currentDrawer if a value is returned
                    }
                    //Initalize guess button
                    guessButton.setOnClickListener(v -> handleGuessButtonClick());
                    drawerNameTextView.setText(drawer); // Update UI on the guesser side
                    guessButton.setVisibility(View.VISIBLE);// Means user is a guesser anc can guess
                    guessButton.setEnabled(true); // Enable guessing for guessers
                    userIsDrawer = false;
                    vocabWordTextView.setVisibility(View.GONE);//guesser can't see word
                    //Start game for guessers
                    startGame();
                });
            }

            // Optionally remove Online_user_id from userTeammates if they are the drawer
            List<String> gussers = null;
            if (isDrawler) {
                gussers = userTeammates;
                gussers.remove(Online_user_id); // Exclude the drawer from being a guesser
            }

            Log.d(TAG, "Team: " + teamName + ", Guessers: " + gussers);
        });

    }

    private void listenForGuesses(String gameId, String teamName) {
        Log.e("Firestore", "listenForGuesses called");

        // Remove the old listener if it exists to avoid duplication
        if (guessListener != null) {
            guessListener.remove(); // Detach the old listener
        }

        // Set up a new listener on the GuessButtonQueue for the specific team
        guessListener = firestore.collection("games")
                .document(gameId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        Log.e("FirestoreError", "Listen failed.", e);
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        // Retrieve the GuessButtonQueue array for the current team
                        List<String> guessQueue = (List<String>) documentSnapshot.get("GuessButtonQueue." + teamName);
                        Log.d("Firestore listenForGuesses", "GuessQueue: " + guessQueue);

                        if (guessQueue != null && !guessQueue.isEmpty()) {
                            String firstGuesserId = guessQueue.get(0);  // The first person to guess
                            Log.d("GuessDetected", "User " + firstGuesserId + " wants to make a guess.");

                            // Only process the guess if it has not been processed before
                            if (!processedGuessers.contains(firstGuesserId)) {
                                // Pause the game when a guess is detected
                                pauseGame();

                                // Show a dialog to the drawer with the guess
                                showGuessReceivedDialog(firstGuesserId);

                                // Mark the guess as processed to avoid reprocessing
                                processedGuessers.add(firstGuesserId);

                                // Remove the guesser from the queue immediately to prevent reading twice
                                //handleGuessResult(firstGuesserId);
                            }
                        }
                    }
                });
    }

    // Ensure proper cleanup when the activity or fragment is paused or destroyed
    @Override
    protected void onPause() {
        super.onPause();
        if (guessListener != null) {
            guessListener.remove(); // Remove listener to prevent memory leaks
        }
    }



    private void pauseGame() {
        // Logic to pause the timer and drawing
        Log.d("GameStatus", "Game is paused for a guess.");
        // Pause the timer (You may need to implement this)
        timerTextView.setText("Paused");  // Update the UI to reflect that the game is paused
        // Disable drawing while the game is paused
        drawingView.setDrawingEnabled(false);
    }

    private void showGuessReceivedDialog(String guesserId) {
        // Check if the activity is valid (not destroyed or finishing) to prevent a crash
        if (isFinishing() || isDestroyed()) {
            Log.e("DialogError", "Activity is not in a valid state to show dialog.");
            return;  // Do not proceed with showing the dialog
        }

        // Show a dialog to the drawer with the guess from the guesser
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("New Guess Received!")
                .setMessage("Player " + guesserId + " wants to guess the word.")
                .setPositiveButton("Correct", (dialog, which) -> {
                    // Update the score for the guesser
                    saveScore(guesserId);  // User is rewarded points for the correct guess
                    //Log.d("Time", String.valueOf(remainingTime));
                   // showWordToGuessers();

                    // Proceed to the next round on correct guess
                    updateRound();

                    // Handle guess result after the round update
                    handleGuessResult(guesserId);  // Ensure guess is handled only once

                    //Reset round timer and get the current round
                    //remainingTime = 0;
                    getCurrentRound();
                })
                .setNegativeButton("Wrong", (dialog, which) -> {
                    dialog.dismiss();
                    // Even if the guess was wrong, handle the guesser removal
                    handleGuessResult(guesserId);  // Ensure guess is handled only once

                    // Resume the game if the guess is wrong
                    resumeGame();
                })
                .setCancelable(false);  // Prevent dialog from being canceled by the back button

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);  // Can't dismiss the dialog by tapping outside

        // Show the dialog only if the activity is in a valid state to prevent a crash
        if (!isFinishing() && !isDestroyed()) {
            dialog.show();
        } else {
            Log.e("DialogError", "Failed to show dialog: Activity is in an invalid state.");
        }
    }

    private void listenForWordInFirestore() {
        // Remove any existing listener before adding a new one
        if (wordListener != null) {
            wordListener.remove();
            wordListener = null;
        }

        // Start listening for changes in the currentWordInEachTeam path
        wordListener = firestore.collection("games")
                .document(gameId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        Log.e("FirestoreError", "listenForWordInFirestore listener failed.", e);
                        return;
                    }
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        String currentWordPlusMeaning = documentSnapshot.getString("currentWordInEachTeam." + teamName);
                        if (currentWordPlusMeaning != null && !currentWordPlusMeaning.equals("")) {
                            Log.d("Firestore", "Retrieved word for team " + teamName + ": " + currentWordPlusMeaning);

                            //show word to guessers
                            vocabWordTextView.setVisibility(View.VISIBLE);
                            vocabWordTextView.setText(currentWordPlusMeaning);

                            // Once word processed remove listener
                            if (wordListener != null) {
                                wordListener.remove();
                                wordListener = null;
                            }
                        } else {
                            Log.d("Firestore", "No word found for team " + teamName);
                        }
                    }
                });
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
                            scoreTextView.setText("Score: " + score);

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


    private void handleGuessResult(String firstGuesserId) {
        // Remove the guesser from the queue (only called once no matter the guess outcome)
        firestore.collection("games").document(gameId)
                .update("GuessButtonQueue." + teamName, FieldValue.arrayRemove(firstGuesserId))
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "Successfully removed " + firstGuesserId + " from GuessButtonQueue.");

                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreError", "Failed to remove guesser from queue", e);
                });
    }

    private void nextRound(){

        // Inflate the layout for 3 second loading screen
        //View loadingView = getLayoutInflater().inflate(R.layout.loading_screen, null);
        //Add the loading view to the current layout
        //((ViewGroup) findViewById(android.R.id.content)).addView(loadingView);

        //show dialog for what the word was

        if (!Online_user_id.equals(currentDrawer)) {
            guessButton.setEnabled(false);
            listenForWordInFirestore();
            startLoadingAnimation();
            //Use a Handler to start delay
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                //Remove the loading view after the delay
                //((ViewGroup) findViewById(android.R.id.content)).removeView(loadingView);
                stopLoadingAnimation();
                restartGameState();
                //restartGameState();
            }, 8000);//delay to make sure guesser is behind in terms of time and lest guesser have time to comprehend the vocabulary word

            //restartGameState();
        }
        // getScore(studentTeamMap.get(Online_user_id)); //Get the score for student and redisplay it
        if (Online_user_id.equals(currentDrawer)) {//if drawer is online user they make new instance/round of game
            drawingView.setDrawingEnabled(false);//fixes bug where drawer can draw on while on loading page
            startLoadingAnimation();
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
            eraseDrawView(); //Erase the draw view ERROR
            //String noWord = "";
            //updateWordInFireStore(noWord);//clear word in firestore

            // Select a new drawer asynchronously
            selectNewDrawer();
            resetTimerInDB();
            remainingTime = roundtime;//reassures time is reset
            stopLoadingAnimation();
            restartGameState(); // Restart the whole activity

            }, 3000);
        }

    }

    private void resetTimerInDB() {
        firestore.collection("games").document(gameId)
                .update("timeLeftInTeamGame." + teamName, roundtime)
                .addOnSuccessListener(aVoid -> Log.d("resetTimerInDB", "Timer reset successfully."))
                .addOnFailureListener(e -> Log.e("resetTimerInDB", "Error resetting timer", e));
        Log.d("resetTimerInDB", "Game Id" + gameId);
        Log.d("resetTimerInDB", "Team Name" + teamName);
        Log.d("resetTimerInDB", "Round Time" + roundtime);
    }

    private void restartGameState() {
        stopListening();//stop listening for guesses
        //setupGame(selectedTopic, teacherName, selectedClass);

        if (processedGuessers != null) {
            processedGuessers.clear();  //Clear to avoid carrying over old processed guesses
        }
        Intent intent = getIntent(); // Get the current intent
        finish(); // Finish the current activity
        startActivity(intent); // Start the activity again with the same intent
    }
    private void stopListening() {
        if (guessListener != null) {
            guessListener.remove();
            guessListener = null; // Clear the listener reference
        }
    }
    //games(collection)->gameId(document)->teamDrawers(map)->teamName(array)->name of drawer as sting in index 0
    //The user was a draler they where removed from userTeammatesDrawerRemoved so they shouldn't be picked back to back
    private void selectNewDrawer() {
        if (userTeammatesDrawerRemoved == null || userTeammatesDrawerRemoved.isEmpty()) {
            Log.e(TAG, "No students available to select a drawer.");
            return; // Prevent further execution if the list is empty or null
        }
        Log.d(TAG, "Should not have Online user in it if they were drawer once: " + userTeammates);
        // Select a random student from the list (since the Online_user_id is no longer in the list)
        Random random = new Random();
        int randomIndex = random.nextInt(userTeammatesDrawerRemoved.size()); // Get random index
        String newDrawer = userTeammatesDrawerRemoved.get(randomIndex); // Get new drawers name

        // Update the Firestore database with the new drawer
        firestore.collection("games").document(gameId)
                .update("teamDrawers." + teamName, newDrawer)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "New drawer " + newDrawer + " successfully updated in Firestore.");

                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating new drawer in Firestore", e);
                });

        // Optionally update the UI immediately after selecting the new drawer
        //drawerNameTextView.setText(newDrawer);
    }

    private void eraseDrawView() {
        // Initialize Firestore and Realtime Database instances

        // Now delete the game data from Realtime Database
        database.getReference("games").child(gameId).child(teamName).child("strokes").removeValue()
                .addOnSuccessListener(aVoid2 -> {
                    Log.d("RealtimeDatabase", "stokes erased from draw view!");

                })
                .addOnFailureListener(e -> {
                    Log.w("RealtimeDatabase", "Error trying to erase strokes from Realtime Database", e);
                });

    }

    private void resumeGame() {


        // Resume the timer and drawing after the guess has been processed
        Log.d("GameStatus", "Resuming the game.");
        timerTextView.setText("Time Left: ");//Set the timer to back to something that is not "Paused"
        // Logic to resume the timer (re-implement this as per your timer logic)
        //startGame();  // Call startGame or your resume logic
        drawingView.setDrawingEnabled(userIsDrawer);  // Enable drawing only if the user is the drawer
    }

    public interface DrawerCallback {
        void onDrawerRetrieved(String drawer);
    }

    private void getDrawler(DrawerCallback callback) {
        firestore.collection("games")
                .document(gameId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot gameDoc = task.getResult();
                        if (gameDoc.exists()) {
                            Map<String, String> teamDrawers = (Map<String, String>) gameDoc.get("teamDrawers");
                            if (teamDrawers != null && teamDrawers.containsKey(teamName)) {
                                String drawer = teamDrawers.get(teamName);
                                Log.d("Firestore", "Drawer for team: " + teamName + " is " + drawer);
                                callback.onDrawerRetrieved(drawer); // Pass the value to the callback
                            } else {
                                Log.e(TAG, "No drawer found for the team: " + teamName);
                                callback.onDrawerRetrieved(null); // Pass null if no drawer found
                            }
                        } else {
                            Log.e(TAG, "Game document does not exist.");
                            callback.onDrawerRetrieved(null); // Pass null if document doesn't exist
                        }
                    } else {
                        Log.e(TAG, "Error fetching game data: ", task.getException());
                        callback.onDrawerRetrieved(null); // Pass null on error
                    }
                });
    }

    private void checkIfUserIsDrawler(String teamName, OnCheckDrawerListener listener) {
        // Fetch the game data from Firestore to check for the drawer
        firestore.collection("games")
                .document(gameId) // Replace with the actual game ID
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot gameDoc = task.getResult();
                        if (gameDoc.exists()) {
                            // Get the teamDrawers map from the game document
                            Map<String, String> teamDrawers = (Map<String, String>) gameDoc.get("teamDrawers");
                            String selectedDrawer = teamDrawers.get(teamName); // Get the drawer for the specific team

                            // Check if the Online_user_id matches the selected drawer
                            boolean isDrawler = Online_user_id.equals(selectedDrawer);
                            listener.onCheck(isDrawler); // Notify the result using the listener
                        } else {
                            Log.e(TAG, "Game document does not exist.");
                            listener.onCheck(false); // If no document exists, the user is not a drawer
                        }
                    } else {
                        Log.e(TAG, "Error fetching game data: ", task.getException());
                        listener.onCheck(false); // On failure, the user is not a drawer
                    }
                });
    }

    // Listener interface to handle the async result
    interface OnCheckDrawerListener {
        void onCheck(boolean isDrawler);
    }


    private void getRandomizedWords(String topic) {
        vocabularyWordRef = FirebaseDatabase.getInstance().getReference("vocabulary").child(topic).child(topic);

        vocabularyWordRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Clear the list before adding new words
                randomizedWords.clear();
                Log.d("FirebaseData", "DataSnapshot: " + dataSnapshot.toString()); // Log the data snapshot

                // Check if there are any children in the snapshot
                if (!dataSnapshot.hasChildren()) {
                    Log.d("FirebaseData", "No children found for topic: " + topic);
                    return; // Exit early if no children
                }

                List<VocabularyItem> allVocabularyItems = new ArrayList<>(); // List to store all vocabulary items

                // Add all vocabulary items (words and meanings) from the snapshot to the list
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    VocabularyItem item = snapshot.getValue(VocabularyItem.class);
                    if (item != null && item.getWord() != null && item.getMeaning() != null) { // Ensure item, word, and meaning are not null
                        allVocabularyItems.add(item); // Add the word and meaning to the list
                        Log.d("FirebaseData", "Added word: " + item.getWord() + ", meaning: " + item.getMeaning());
                    } else {
                        Log.w("FirebaseData", "Item, word, or meaning was null for snapshot: " + snapshot.toString());
                    }
                }

                // Shuffle the list of all vocabulary items
                Collections.shuffle(allVocabularyItems);

                // Add the first 15 vocabulary items to the randomized list (or less if fewer than 15 are available)
                randomizedWords.addAll(allVocabularyItems.subList(0, Math.min(15, allVocabularyItems.size())));

                Log.d("FirebaseData", "Randomized words (limited to 15): " + randomizedWords); // Log the randomized vocabulary items

                // Start the game with the randomized words and meanings
                startGameWithRandomWords(randomizedWords);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle possible errors
                Log.e("FirebaseError", databaseError.getMessage());
            }
        });
    }

    private void startGameWithRandomWords(List<VocabularyItem> words) {
        // Logic to start the game with the randomized words
        showWordSelectionDialog(words); // Show dialog for word selection
    }

    private void showWordSelectionDialog(List<VocabularyItem> words) {
        Log.d("WordSelection", "Available words: " + words);

        // Check if the activity is finishing or destroyed before proceeding
        if (isFinishing() || isDestroyed()) {
            Log.w("WordSelection", "Activity is finishing or destroyed, cannot show dialog.");
            return;
        }

        // Check if the words list is null or empty
        if (words == null || words.isEmpty()) {
            // You can show a Toast message or handle this scenario as needed
            Toast.makeText(this, "No words available for selection.", Toast.LENGTH_SHORT).show();
            return; // Exit the method early if no words are available
        }


        //Create the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_word_selection, null);
        builder.setView(dialogView);

        //Set the dialog title (optional since it's in the layout)
        //TextView wordSelectionTitle = dialogView.findViewById(R.id.wordSelectionTitle);

        //Create the dialog instance
        AlertDialog dialog = builder.create();

        dialog.setCanceledOnTouchOutside(false); // Prevent dismissal on touch outside
        dialog.setCancelable(false); // Prevent dismissal by back button

        ListView wordListView = dialogView.findViewById(R.id.wordListView);

        //Create a custom adapter to show only the word and meaning
        ArrayAdapter<VocabularyItem> adapter = new ArrayAdapter<VocabularyItem>(this,
                android.R.layout.simple_list_item_2, android.R.id.text1, words) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView wordTextView = view.findViewById(android.R.id.text1);
                TextView meaningTextView = view.findViewById(android.R.id.text2);

                // Get the vocabulary item for this position
                VocabularyItem item = getItem(position);

                // Set the word and meaning text
                wordTextView.setText(item.getWord()); // Display the English word
                meaningTextView.setText(item.getMeaning()); // Display the Japanese meaning

                return view;
            }
        };

        wordListView.setAdapter(adapter);

        wordListView.setOnItemClickListener((parent, view, position, id) -> {
            VocabularyItem selectedWord = words.get(position); // Get the selected word and meaning
            correctWord = selectedWord.getWord(); // Store the selected word
            vocabWordTextView.setText(correctWord + " (" + selectedWord.getMeaning() + ")"); // Display the word and meaning
            dialog.dismiss(); // Close the dialog

            updateWordInFireStore(correctWord + " (" + selectedWord.getMeaning() + ")"); // Update Firestore with the selected word

            startGame(); // Start the game after word selection
        });

        dialog.show();
    }

    //update currentWordInEachTeam(map) in firestore for team
    private void updateWordInFireStore(String correctWordandMeaning){
        firestore.collection("games").document(gameId)
                .update("currentWordInEachTeam." + teamName, correctWordandMeaning)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Word updated successfully."))
                .addOnFailureListener(e -> Log.e("FirestoreError", "Error updating word", e));

    }

    private void startGame() {
        setupDrawing(); // Call to set up drawing only after word selection
        initializeDrawing();
    }



    private void setupDrawing() {


        // Enable drawing based on whether the user is the drawer
        if (userIsDrawer==true) {
            drawingView.setDrawingEnabled(true); // Allow the drawer to draw
            Log.d("DrawingGame", "Drawer can draw.");
        } else {
            drawingView.setDrawingEnabled(false); // Disable drawing for the guesser
            Log.d("DrawingGame", "Guesser cannot draw.");
        }

        drawingView.setOnDrawListener(stroke -> {
            // Logic to save strokes can go here if needed
        });


        //roundTextView.setText("Round:" + selectedRound);
        //getCurrentRound();//Sets round number on UI and gets current round from firestore

        //need a database boolean for word picked


        getTimeLeft();

    }

    private void updateRound() {
        Log.d("Firestore", "Updating round in Firestore updateRound called.");
        firestore.collection("games").document(gameId)
                .update("roundInEachTeam." + studentTeamMap.get(Online_user_id), FieldValue.increment(1))
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Round updated successfully."))
                .addOnFailureListener(e -> Log.e("FirestoreError", "Error updating round", e));



    }
    private void getCurrentRound() {
        // Gets current round from Firestore for the team
        firestore.collection("games").document(gameId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Check if roundInEachTeam exists in the document
                        if (documentSnapshot.contains("roundInEachTeam")) {
                            // Retrieve the roundInEachTeam map
                            Map<String, Object> roundInEachTeam = (Map<String, Object>) documentSnapshot.get("roundInEachTeam");

                            // Get the current user's team name
                            String userTeamName = studentTeamMap.get(Online_user_id);

                            // Check if the team name exists in roundInEachTeam
                            if (roundInEachTeam.containsKey(userTeamName)) {
                                // Get the team data associated with the userTeamName
                                Object teamData = roundInEachTeam.get(userTeamName);  // This can be either Long or List<Integer>

                                // Log the actual type of the object and its contents
                                Log.d("CurrentRound", "Type of teamData: " + teamData.getClass().getSimpleName());
                                Log.d("CurrentRound", "Contents of teamData: " + teamData.toString());

                                // Check if teamData is a List
                                if (teamData instanceof List<?>) {
                                    List<Integer> teamDetails = (List<Integer>) teamData;

                                    // Check if the list is not empty
                                    if (!teamDetails.isEmpty()) {

                                        int teamRoundNumber = teamDetails.get(0);
                                        Log.d("CurrentRound", "Inside if (!teamDetails.isEmpty()): " + teamRoundNumber);
                                        handleRoundUpdateForTeam(teamRoundNumber);



                                    } else {
                                        Log.d("CurrentRound", "The list is empty for team: " + userTeamName);
                                    }
                                }
                                // Check if teamData is a Long (or Integer)
                                else if (teamData instanceof Long) {
                                    int teamRoundNumber = ((Long) teamData).intValue();  // Convert Long to int
                                    handleRoundUpdateForTeam(teamRoundNumber);
                                    Log.d("CurrentRound", "else if (teamData instanceof Long): " + teamRoundNumber);
                                } else {
                                    Log.d("CurrentRound", "Team data is not a List<Integer> or Long for team: " + userTeamName);
                                }
                            } else {
                                Log.d("CurrentRound", "Team name not found in roundInEachTeam for user: " + userTeamName);
                            }
                        } else {
                            Log.d("CurrentRound", "roundInEachTeam does not exist.");
                        }
                    } else {
                        Log.d("CurrentRound", "Game document does not exist.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreError", "Error getting document: ", e);
                });
    }

    private void handleRoundUpdateForTeam(int teamRoundNumber) {
        //Extract the current round number from roundTextView
        roundText = roundTextView.getText().toString(); // e.g., "Round: 1"

        //Split and extract the number part from "Round: X"
        int currentRoundNumberFromText = Integer.parseInt(roundText.split(":")[1].trim());

        Log.d("handleRoundUpdateForTeam", "Current round for team: " + teamRoundNumber);
        Log.d("handleRoundUpdateForTeam", "round text: " + roundText);
        Log.d("handleRoundUpdateForTeam", "Current round from text: " + currentRoundNumberFromText);
        Log.d("handleRoundUpdateForTeam", "User is drawer: " + userIsDrawer);
        Log.d("handleRoundUpdateForTeam", "Selected round: " + selectedRound);

        if (teamRoundNumber > selectedRound) {
            endGame();
            return;
        }
        //Since teamRoundNumber is only greater than currentRoundNumberFromText when a round is incremented on a correct guess we know that the word was solved if this is true
        if(teamRoundNumber> currentRoundNumberFromText) {
            roundTextView.setText("Round: " + teamRoundNumber);

            Log.d("CurrentRound", "Current round for team: " + teamRoundNumber);
            Log.d("CurrentRound", "User is drawer: " + userIsDrawer);
            Log.d("CurrentRound", "Selected round: " + selectedRound);

            if (userIsDrawer) {

                Log.d("CurrentRound", "Next round about to be called");
                Log.d("handleRoundUpdateForTeam", "about to call setLastRoundForTeam" + roundText);
                setLastRoundForTeam((String) roundTextView.getText());
                nextRound();
            }
            getDrawler(drawer -> {
                Log.d("CurrentRound", "Current drawer: " + drawer);

                Log.d("CurrentRound", "currentDrawer: " + currentDrawer);
                // If the user is a guesser and the drawer has changed, it's time for the next round
                if (!userIsDrawer) {
                    Log.d("CurrentRound", "Drawer has changed, moving to next round");
                    nextRound();
                }
            });

        }

    }

    private void setLastRoundForTeam(String roundText) {
        firestore.collection("games").document(gameId)
                .update("lastRoundInEachTeam." + studentTeamMap.get(Online_user_id), roundText)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Last Round updated successfully."))
                .addOnFailureListener(e -> Log.e("FirestoreError", "Error updating last round", e));
    }

    private void getLastRoundForTeam(String teamName, String gameId) {
        // Fetch the last round from Firestore
        firestore.collection("games").document(gameId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Access the "lastRoundInEachTeam" map
                        Map<String, Object> lastRoundMap = (Map<String, Object>) documentSnapshot.get("lastRoundInEachTeam");

                        // Check if the team exists in the map
                        if (lastRoundMap != null && lastRoundMap.containsKey(teamName)) {
                            Object lastRoundObj = lastRoundMap.get(teamName);

                            // Check if the retrieved value is a String or a List
                            if (lastRoundObj instanceof String) {
                                // It's a String, set it directly
                                String lastRound = (String) lastRoundObj;
                                Log.d("Firestore", "Last Round for team getLastRoundForTeam1" + teamName + ": " + lastRound);
                                roundTextView.setText(lastRound);
                            } else if (lastRoundObj instanceof List<?>) {
                                // If it's a List, you can decide how to handle it
                                List<?> lastRoundList = (List<?>) lastRoundObj;

                                // For example, set the TextView to the first element in the list
                                if (!lastRoundList.isEmpty() && lastRoundList.get(0) instanceof String) {
                                    String lastRound = (String) lastRoundList.get(0);
                                    Log.d("Firestore", "Last Round for team getLastRoundForTeam2" + teamName + ": " + lastRound);
                                    roundTextView.setText(lastRound);
                                } else {
                                    Log.e("FirestoreError", "The list is empty or the first element is not a String.");
                                }
                            } else {
                                Log.e("FirestoreError", "Unexpected data type for last round: " + lastRoundObj.getClass().getSimpleName());
                            }
                        } else {
                            Log.e("FirestoreError", "No Last Round found for team " + studentTeamMap.get(Online_user_id));
                        }
                    } else {
                        Log.e("FirestoreError", "Game document does not exist.");
                    }
                })
                .addOnFailureListener(e -> Log.e("FirestoreError", "Error fetching last round", e));
    }

    private void getTimeLeft() {
        // Fetch the remaining time from the exact location in Firestore
        firestore.collection("games").document(gameId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Check if the "timeLeftInTeamGame" field exists
                        if (documentSnapshot.contains("timeLeftInTeamGame")) {
                            // Navigate to the map
                            Map<String, Object> timeLeftMap = (Map<String, Object>) documentSnapshot.get("timeLeftInTeamGame");

                            // Check if the specified team exists in the map
                            if (timeLeftMap.containsKey(studentTeamMap.get(Online_user_id))) {
                                // Retrieve the time object for the current team
                                Object timeLeftObj = timeLeftMap.get(studentTeamMap.get(Online_user_id));

                                // Check if the retrieved object is an instance of List (array)
                                if (timeLeftObj instanceof List<?>) {
                                    List<?> timeLeftList = (List<?>) timeLeftObj;

                                    // Ensure the list is not empty and contains a number at index 0
                                    if (!timeLeftList.isEmpty() && timeLeftList.get(0) instanceof Number) {
                                        remainingTime = ((Number) timeLeftList.get(0)).intValue(); // Get the time from index 0
                                        Log.d("DrawingGame", "Remaining time fetched from list: " + remainingTime);


                                    } else {
                                        Log.e("DrawingGame", "CS array is empty or the first element is not a number.");
                                        remainingTime = 0; // Default to 0 if no valid time is found
                                    }
                                } else if (timeLeftObj instanceof Number) {
                                    // Handle the case where the value is a single number (Long or Integer)
                                    remainingTime = ((Number) timeLeftObj).intValue();
                                    Log.d("DrawingGame", "Remaining time fetched as a single value: " + remainingTime);
                                } else {
                                    Log.e("DrawingGame", "Expected an array or number for team " + studentTeamMap.get(Online_user_id) + ", but got: " + timeLeftObj.getClass().getSimpleName());
                                    remainingTime = 0; // Default to 0 if the type is incorrect
                                }
                            } else {
                                Log.e("DrawingGame", "Field for team " + studentTeamMap.get(Online_user_id) + " does not exist.");
                                remainingTime = 0; // Default to 0 if no time is found
                            }
                        } else {
                            Log.e("DrawingGame", "Field 'timeLeftInTeamGame' does not exist.");
                            remainingTime = 0; // Default to 0 if no time is found
                        }

                        // Start the timer based on whether the user is the drawer or the guesser
                        if (userIsDrawer) {
                            startTimerForDrawer();
                            Log.d("DrawingGame", "Drawer time started.");
                        } else {
                            startTimerForGuesser();
                            Log.d("DrawingGame", "Guess time started.");
                        }
                    } else {
                        Log.e("DrawingGame", "Game document not found.");
                    }
                })
                .addOnFailureListener(e -> Log.e("DrawingGame", "Error fetching remaining time: " + e.getMessage()));
    }


    private void startTimerForDrawer() {
        timerHandler = new Handler();
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (!"Paused".equals(timerTextView.getText().toString())) {
                    // Continue the countdown when not paused
                    if (remainingTime > 0) {
                        remainingTime--;

                        // Update the UI with the new time
                        timerTextView.setText("Time Left: " + remainingTime);
                    } else if (remainingTime == 0) {
                        Log.d("DrawingGame", String.valueOf(remainingTime));
                        Toast.makeText(DrawingGameActivity.this, "Time's up!", Toast.LENGTH_SHORT).show();

                        if (userIsDrawer) {
                            // Call these methods once and then continue with Firestore updates
                            updateRound();
                            clearGuessButtonQueue();
                            getCurrentRound();
                            return;
                        }
                    }
                }

                // Firestore update logic
                firestore.collection("games").document(gameId)
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                Map<String, Object> timeLeftMap = (Map<String, Object>) documentSnapshot.get("timeLeftInTeamGame");

                                if (timeLeftMap != null && timeLeftMap.containsKey(studentTeamMap.get(Online_user_id))) {
                                    Object timeLeftObj = timeLeftMap.get(studentTeamMap.get(Online_user_id));

                                    if (timeLeftObj instanceof Long) {
                                        // Handle Long case (Due to firestore's limitation)
                                        if(userIsDrawer){
                                            firestore.collection("games").document(gameId)
                                                    .update("timeLeftInTeamGame." + studentTeamMap.get(Online_user_id), remainingTime)
                                                    .addOnSuccessListener(aVoid -> Log.d("DrawingGame", "Time updated in Firestore (Long): " + remainingTime))
                                                    .addOnFailureListener(e -> Log.e("DrawingGame", "Error updating time in Firestore: " + e.getMessage()));
                                        }
                                    } else if (timeLeftObj instanceof List<?>) {
                                        // Handle List case (array of times)
                                        List<Object> timeLeftList = (List<Object>) timeLeftObj;

                                        if (!timeLeftList.isEmpty()) {
                                            timeLeftList.set(0, remainingTime);
                                            if(userIsDrawer) {
                                                firestore.collection("games").document(gameId)
                                                        .update("timeLeftInTeamGame." + studentTeamMap.get(Online_user_id), timeLeftList)
                                                        .addOnSuccessListener(aVoid -> Log.d("DrawingGame", "Time updated in Firestore (List): " + remainingTime))
                                                        .addOnFailureListener(e -> Log.e("DrawingGame", "Error updating time in Firestore: " + e.getMessage()));
                                            }
                                        }
                                    } else {
                                        Log.e("DrawingGame", "Unexpected type: " + timeLeftObj.getClass().getSimpleName());
                                    }
                                } else {
                                    Log.e("DrawingGame", "Field for team " + studentTeamMap.get(Online_user_id) + " does not exist.");
                                }
                            } else {
                                Log.e("DrawingGame", "Game document not found.");
                            }
                        })
                        .addOnFailureListener(e -> Log.e("DrawingGame", "Error fetching time for update: " + e.getMessage()));

                // Schedule the next timer tick in 1 second
                timerHandler.postDelayed(this, 1000);
            }
        };

        timerHandler.postDelayed(timerRunnable, 1000);
    }


    private void startTimerForGuesser() {
        // Initialize the handler and runnable for the guesser's timer
        timerHandler = new Handler();
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                // Fetch the updated remaining time from Firestore
                firestore.collection("games").document(gameId)
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                // Navigate to the timeLeftInTeamGame map
                                Map<String, Object> timeLeftMap = (Map<String, Object>) documentSnapshot.get("timeLeftInTeamGame");

                                // Check if the team exists in the map
                                if (timeLeftMap != null && timeLeftMap.containsKey(studentTeamMap.get(Online_user_id))) {
                                    Object timeLeftObj = timeLeftMap.get(studentTeamMap.get(Online_user_id));

                                    // Handle if timeLeftObj is a Long (single value) or a List (array)
                                    if (timeLeftObj instanceof Long) {
                                        // If it's a Long, simply cast and use it
                                        remainingTime = ((Long) timeLeftObj).intValue();
                                        Log.d("DrawingGame", "Got Long value for remaining time: " + remainingTime);
                                    } else if (timeLeftObj instanceof List<?>) {
                                        // If it's a List, proceed with the previous logic
                                        List<?> timeLeftList = (List<?>) timeLeftObj;

                                        // Ensure the array is not empty and index 0 contains a number
                                        if (!timeLeftList.isEmpty() && timeLeftList.get(0) instanceof Number) {
                                            remainingTime = ((Number) timeLeftList.get(0)).intValue(); // Get the time from index 0
                                            Log.d("DrawingGame", "Got List value for remaining time: " + remainingTime);
                                        } else {
                                            Log.e("DrawingGame", "CS array is empty or first element is not a number.");
                                            remainingTime = 0; // Default to 0 if no valid time is found
                                        }
                                    } else {
                                        Log.e("DrawingGame", "Expected a Long or array for team " + studentTeamMap.get(Online_user_id) + ", but got: " + timeLeftObj.getClass().getSimpleName());
                                        remainingTime = 0; // Default to 0 if the type is incorrect
                                    }

                                    // Update the UI and handle the countdown
                                    if (remainingTime > 0) {
                                        // Display the remaining time (subtract 1 to sync with drawer)
                                        timerTextView.setText("Time Left: " + (remainingTime - 1));
                                        // Schedule the next fetch immediately to sync with drawer
                                        timerHandler.postDelayed(this, 1000);
                                    }
                                    else if (!userIsDrawer && remainingTime == 0) {
                                        Toast.makeText(DrawingGameActivity.this, "Time's up!", Toast.LENGTH_SHORT).show();
                                        Log.d("DrawingGame", "3-second delay before switching rounds...");

                                        listenForWordInFirestore();//show word and meaning to guesser
                                        startLoadingAnimation();
                                        //3-second delay to make sure Drawer updates round
                                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                            stopLoadingAnimation();
                                            getCurrentRound(); // Now call getCurrentRound after 2 seconds
                                        }, 2000);
                                    }
                                    Log.d("DrawingGame", "Remaining time: " + remainingTime);
                                    Log.d("DrawingGame", "User is drawer: " + userIsDrawer);
                                } else {
                                    Log.e("DrawingGame", "Field for team " + studentTeamMap.get(Online_user_id) + " does not exist.");
                                    remainingTime = 0; // Default to 0 if no time is found
                                }
                            } else {
                                Log.e("DrawingGame", "Game document not found.");
                            }
                        })
                        .addOnFailureListener(e -> Log.e("DrawingGame", "Error fetching remaining time: " + e.getMessage()));
            }
        };
        // Start the guesser's timer
        timerHandler.postDelayed(timerRunnable, 0);
    }

    private void clearGuessButtonQueue() {
        firestore.collection("games").document(gameId)
                .update("GuessButtonQueue." + teamName, FieldValue.delete())
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "GuessButtonQueue cleared successfully."))
                .addOnFailureListener(e -> Log.e("FirestoreError", "Error clearing GuessButtonQueue", e));

    }


    private void initializeDrawing() {
        if (strokesRef == null) {
            Log.e(TAG, "strokesRef is null. Make sure to initialize it properly.");
            return; // Early return to prevent further actions with a null reference.
        }
        loadExistingStrokes();
        listenforStrokes(); // Ensure real-time updates

    }
    private void loadExistingStrokes() {
        Log.d("DrawingGame", "Game ID: " + gameId); // Log the Game ID

        if (strokesRef == null) {
            Log.e("DrawingGame", "strokesRef is null. Cannot load strokes.");
            return;
        }


        if (gameId == null) {
            Log.e("DrawingGame", "Game ID is null!");
            return;
        }

        Log.d("DrawingGame", "Loading existing strokes...");

        strokesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d("DrawingGame", "Number of strokes: " + dataSnapshot.getChildrenCount());
                if (dataSnapshot.exists()) { // Check if the snapshot exists
                    for (DataSnapshot strokeSnapshot : dataSnapshot.getChildren()) {
                        Map<String, Object> strokeData = (Map<String, Object>) strokeSnapshot.getValue();
                        // Additional logging for strokeData
                        Log.d("DrawingGame", "Stroke data: " + strokeData);
                        // Process strokes as before...
                    }
                } else {
                    Log.d("DrawingGame", "No strokes found under this game ID.");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("DrawingGame", "Error loading existing strokes: " + databaseError.getMessage());
            }
        });
    }

    private void listenforStrokes(){
        strokesRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                // A new stroke is added to Firebase, retrieve it
                Map<String, Object> strokeData = (Map<String, Object>) dataSnapshot.getValue();

                //Extract stroke data
                List<Object> serializedPoints = (List<Object>) strokeData.get("points");

                // Cast color to Long and then convert to int
                Long colorLong = (Long) strokeData.get("color");
                int color = colorLong.intValue(); // Convert Long to int

                // Cast strokeWidth to Float, checking if it's stored as Long
                Object strokeWidthObj = strokeData.get("strokeWidth");
                float strokeWidth;
                if (strokeWidthObj instanceof Long) {
                    strokeWidth = ((Long) strokeWidthObj).floatValue(); // Convert Long to float
                } else if (strokeWidthObj instanceof Double) {
                    strokeWidth = ((Double) strokeWidthObj).floatValue(); // Convert Double to float
                } else {
                    strokeWidth = 0; // Default value if strokeWidth is not found or not a recognized type
                    Log.e("DrawingGame", "Invalid strokeWidth type: " + strokeWidthObj.getClass().getSimpleName());
                }
                // Deserialize points
                List<Point> stroke = drawingView.deserializePoints(serializedPoints);

                // Add the stroke to the DrawingView
                drawingView.addStroke(stroke, color, strokeWidth);

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                // Handle stroke updates (if necessary)
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                // Handle stroke removal (if necessary)
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
                // Handle stroke moved (if necessary)
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle errors
            }
        });
    }






    //End game when all rounds complete
    private void endGame() {
        // Check if the activity is finishing or destroyed before showing the dialog to prevent memory leakage
        if (!isFinishing() && !isDestroyed()) {
            getScore(Online_user_id);//call one more time to get most up to date score
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Game Over");
            builder.setMessage("Final Scores:\n" + scoreTextView.getText());
            builder.setPositiveButton("OK", (dialog, which) -> {
                dialog.dismiss();

                // Clear the active game field for student so there light indicator dose not show an active game
                clearActiveGameField();

                // Attempt to navigate to the ProfileFragment
                navigateToLoginActivity();


            });

            builder.setCancelable(false); // Prevent dialog from being canceled by the back button
            // Create the dialog instance
            AlertDialog dialog = builder.create();
            dialog.setCanceledOnTouchOutside(false); // Allow the dialog to not be cancelable when the user taps outside

            // Show the dialog if the activity is in a valid state
            if (!isFinishing() && !isDestroyed()) {
                dialog.show();
            }
        } else {
            Log.d("DrawingGame", "Activity is finishing or destroyed, cannot show dialog.");
        }
    }


    private void clearActiveGameField(){
        firestore.collection("students")
                .document(Online_user_id)
                .update("activeGame", null)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Active game field cleared successfully for student: " + Online_user_id))
                .addOnFailureListener(e -> Log.e("FirestoreError", "Error clearing active game field for student: " + Online_user_id, e));
    }

    private void navigateToLoginActivity() {
        try {
            // Create an intent to navigate to LoginActivity
            Intent intent = new Intent(DrawingGameActivity.this, LoginActivity.class);

            // Start the LoginActivity
            startActivity(intent);

            //finish the current activity if you don't want to return to it
            finish();

        } catch (Exception errors) {
            Log.e("DrawingGame", "Error navigating to LoginActivity: " + errors.getMessage());
            Toast.makeText(this, "An error occurred while navigating to the login page.", Toast.LENGTH_SHORT).show();
        }
    }


    //Calculates and updates score for the guesser who guessed correctly
    private void saveScore(String guesserId) {
        int minPoints = 10;
        int maxPoints  = 100;

        //Calculate percentage of remaining time
        double timePercentage = (double) remainingTime / (double) roundtime;

        //Score given is scaled off how much remaining time is left. Less time lower score, more time higher score
        int rewardedPoints = (int) (timePercentage * (maxPoints  - minPoints) + minPoints);

        //Update score in Firestore
        firestore.collection("students").document(guesserId)
                .update("score", FieldValue.increment(rewardedPoints)) // add the rewarded points to the users score
                .addOnSuccessListener(aVoid -> {
                    //Log success
                    Log.d(TAG, "Score updated successfully for student: " + Online_user_id);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error updating score for student: " + Online_user_id, e));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timerHandler != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
        if (guessListener != null) {
            guessListener.remove();  // Cleanup listener on destroy
        }
        if (guessQueueListener != null) {
            guessQueueListener.remove();  // Clean up the listener on destroy
            guessQueueListener = null;
        }

    }

}



