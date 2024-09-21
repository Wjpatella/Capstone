
package com.example.capstone;

import static com.example.capstone.FS_DBHelper.Online_user_id;


import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import androidx.appcompat.app.AppCompatActivity;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DrawlingGameActivity extends AppCompatActivity {
    private static final String TAG = "DrawlingGameActivity";

    private DrawingView drawingView;
    private TextView vocabWordTextView, guessHistoryTextView, timerTextView, scoreTextView;
    private EditText guessEditText;
    private Button submitGuessButton;
    private FirebaseDatabase realtimeDb;

    private FirebaseFirestore firestore;
    private DatabaseReference gameRef, strokesRef;
    private int remainingTime = 60; // Initial time (in seconds)
    private Handler timerHandler;
    private Runnable timerRunnable;

    private String gameId;
    private String timer;
    private String mode;
    private String selectedClass;
    private String selectedTopic;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drawling_game);

        // Initialize UI components
        drawingView = findViewById(R.id.drawingView);
        vocabWordTextView = findViewById(R.id.vocabWordTextView);
        timerTextView = findViewById(R.id.timerTextView);
        scoreTextView = findViewById(R.id.scoreTextView);

        // Get the passed data from TeacherGameFragment
        Intent intent = getIntent();
        gameId = intent.getStringExtra("gameId");
        timer = intent.getStringExtra("timer");
        mode = intent.getStringExtra("mode");
        selectedClass = intent.getStringExtra("class");
        selectedTopic = intent.getStringExtra("topic");

        // Initialize Firebase database
        realtimeDb = FirebaseDatabase.getInstance();
        gameRef = realtimeDb.getReference("games").child(gameId);

        // Setup Realtime Database with game details
        setupRealtimeDatabase(gameId, timer, mode, selectedClass, selectedTopic);

        // Start the game timer
        startTimer(Integer.parseInt(timer));

        gameRef = realtimeDb.getReference("games/game_id_12345");
        strokesRef = gameRef.child("drawing/strokes");


        // Set up the drawing logic
        setupDrawing();

    }

    // Setup Realtime Database with game details
    private void setupRealtimeDatabase(String gameId, String timer, String mode, String selectedClass, String selectedTopic) {
        DatabaseReference gameRef = realtimeDb.getReference("games").child(gameId);

        Map<String, Object> gameData = new HashMap<>();
        gameData.put("timer", timer);
        gameData.put("mode", mode);
        gameData.put("class", selectedClass);
        gameData.put("status", "active");

        gameRef.setValue(gameData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Realtime Database updated with game data: " + gameId);
                    // Get random vocabulary word based on the selected topic
                    String vocabWord = getRandomVocabWord(selectedTopic);
                    vocabWordTextView.setText(vocabWord);

                    Toast.makeText(this, "Game started successfully!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error updating Realtime Database", e);
                    Toast.makeText(this, "Failed to set up game", Toast.LENGTH_SHORT).show();
                });
    }

    private String getCurrentUserId() {//Check if stundent or teacher then off the name of the selected person for the round
        // Method to retrieve the current user ID
        return Online_user_id;
    }


    private void ceckifDrawerorGuesser() {//Check if the current student or teacher is the a drawer or a guesser
        gameRef.child("drawer").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String drawerId = dataSnapshot.getValue(String.class);
                String currentUserId = getCurrentUserId(); // Get the logged-in user ID

                if (drawerId.equals(currentUserId)) {
                    // If the current user is the drawer, show the vocabulary word
                    gameRef.child("vocabulary_word").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            String vocabWord = snapshot.getValue(String.class);
                            vocabWordTextView.setText(vocabWord);
                            vocabWordTextView.setVisibility(View.VISIBLE); // Make the word visible to the drawer
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                        }
                    });
                } else {
                    // The user is a guesser
                    setupGuessing();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }





    /*GAME FEATURES*/

    private void setupDrawing() {//Drawling logic
        drawingView.setOnDrawListener(stroke -> {
            strokesRef.push().setValue(stroke);
        });

        // Add the ChildEventListener to listen for incoming strokes
        strokesRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String prevChildKey) {
                // Retrieve stroke data from the snapshot
                Map<String, Object> strokeData = dataSnapshot.getValue(new GenericTypeIndicator<Map<String, Object>>() {});
                List<Point> stroke = (List<Point>) strokeData.get("points");
                int color = ((Long) strokeData.get("color")).intValue();
                float strokeWidth = ((Double) strokeData.get("strokeWidth")).floatValue();

                // Add the received stroke to the DrawingView
                drawingView.addStroke(stroke, color, strokeWidth);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String prevChildKey) {}

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {}

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String prevChildKey) {}

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }


    private void setupGuessing() {//method that sets up the guessing logic so students can guess the word
        submitGuessButton.setOnClickListener(view -> {
            String guess = guessEditText.getText().toString().trim();
            if (!guess.isEmpty()) {
                //Submit guess to Firebase
                String currentUserId = getCurrentUserId();
                gameRef.child("guesses").child(currentUserId).setValue(guess);
                appendGuessToHistory(guess);
                checkGuess(guess, currentUserId);
            }
        });
    }


    // Start the game timer
    private void startTimer(int initialTime) {
        remainingTime = initialTime;
        timerHandler = new Handler();
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (remainingTime > 0) {
                    remainingTime--;
                    timerTextView.setText("Time Left: " + remainingTime + " seconds");
                    timerHandler.postDelayed(this, 1000);
                } else {
                    // Timer finished, handle game over
                    gameOver();
                }
            }
        };
        timerHandler.postDelayed(timerRunnable, 1000);
    }

    private void gameOver() {
        Toast.makeText(this, "Time's up! Game over.", Toast.LENGTH_SHORT).show();
        // Logic to handle the end of the game (e.g., show results, save scores)
    }

    // Get a random vocabulary word based on the selected topic
    private String getRandomVocabWord(String selectedTopic) {//move to
        DatabaseReference vocabRef = realtimeDb.getReference("vocabulary").child(selectedTopic).child(selectedTopic);/*vocabulary->Animals->Animals->Ant->example_sentence:"Ants live in very large groups.", meaning:"アリ", parts_of_speech_en:"Noun", parts_of_speech_jp:"名", word:"ant"
                                                                                                                            vocabulary->Verbs->Verbs->accept->example_sentence:"We should accept other cultures", meaning:"受け入れる", parts_of_speech_jp:"動", word:"accept"*/
        // This method should return a randomly selected vocabulary word from Firebase
        // Assume you have a Firebase node for storing vocabulary words.
        // Implement logic to retrieve a random word based on the selected topic
        // Example: Fetch words from Firebase based on topic (e.g., "Verbs", "Animals")
        if ("Verbs".equals(selectedTopic)) {
            // Fetch a random verb
            return "run"; // Placeholder
        } else if ("Animals".equals(selectedTopic)) {
            // Fetch a random animal
            return "cat"; // Placeholder
        }

        return "apple";  // For now, use a placeholder word
    }

    private void appendGuessToHistory(String guess) {
        String currentHistory = guessHistoryTextView.getText().toString();
        guessHistoryTextView.setText(currentHistory + "\n" + guess);
    }



    private void checkGuess(String guess, String currentUserId) {//checks the users guess against the correct word
        gameRef.child("vocabulary_word").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String correctWord = dataSnapshot.getValue(String.class);
                if (guess.equalsIgnoreCase(correctWord)) {
                    int score = calculateScore();
                    gameRef.child("scores").child(currentUserId).setValue(score);
                    scoreTextView.setText("Score: " + score);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }



    private int calculateScore() {//A users score is based on how long it took them to guess the word
        // Logic to calculate score based on time taken
        long currentTime = System.currentTimeMillis();
        long guessTime = Integer.parseInt(timer);
        long timeTaken = currentTime - guessTime;

        if (timeTaken < 10000) return 100; // Score 100 for guesses within 10 seconds
        else if (timeTaken < 20000) return 75;
        else return 50;
    }

    private long getGameStartTime() {
        // Retrieve start time of the game from Firebase or locally stored variable
        return System.currentTimeMillis() - 10000; // Example start time
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timerHandler != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }
}


        /*

        // Set up the drawing logic
        setupDrawing();




        // Check if the current student is the drawer
        gameRef.child("drawer").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String drawerId = dataSnapshot.getValue(String.class);
                String currentUserId = getCurrentUserId(); // Get the logged-in user ID

                if (drawerId.equals(currentUserId)) {
                    // If the current user is the drawer, show the vocabulary word
                    gameRef.child("vocabulary_word").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            String vocabWord = snapshot.getValue(String.class);
                            vocabWordTextView.setText(vocabWord);
                            vocabWordTextView.setVisibility(View.VISIBLE); // Make the word visible to the drawer
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {}
                    });
                } else {
                    // The user is a guesser
                    setupGuessing();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });

        return view;
    }






     private String getRandomVocabWord(String selectedTopic) {//move to
        // This method should return a randomly selected vocabulary word from Firebase
        // Assume you have a Firebase node for storing vocabulary words.
        // Implement logic to retrieve a random word based on the selected topic
        // Example: Fetch words from Firebase based on topic (e.g., "Verbs", "Animals")
        if ("Verbs".equals(selectedTopic)) {
            // Fetch a random verb
            return "run"; // Placeholder
        } else if ("Animals".equals(selectedTopic)) {
            // Fetch a random animal
            return "cat"; // Placeholder
        }

        return "apple";  // For now, use a placeholder word
    }

    private void appendGuessToHistory(String guess) {
        String currentHistory = guessHistoryTextView.getText().toString();
        guessHistoryTextView.setText(currentHistory + "\n" + guess);
    }

    private void checkGuess(String guess, String currentUserId) {
        gameRef.child("vocabulary_word").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String correctWord = dataSnapshot.getValue(String.class);
                if (guess.equalsIgnoreCase(correctWord)) {
                    int score = calculateScore();
                    gameRef.child("scores").child(currentUserId).setValue(score);
                    scoreTextView.setText("Score: " + score);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }

    private void startTimer() {
        timerHandler = new Handler();
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (remainingTime > 0) {
                    remainingTime--;
                    timerTextView.setText("Time Left: " + remainingTime);
                    timerHandler.postDelayed(this, 1000);
                } else {
                    // Timer finished
                    gameOver();
                }
            }
        };
        timerHandler.postDelayed(timerRunnable, 1000);
    }

    private void gameOver() {
        // Logic to handle end of game (e.g., show results or end the game session)
    }



    private String getCurrentUserId() {
        // Method to retrieve the current user ID
        return Online_user_id;
    }




     */

