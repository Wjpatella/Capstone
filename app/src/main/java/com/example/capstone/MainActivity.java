package com.example.capstone;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);//enables edge to edge display
        setContentView(R.layout.login_page); //UI xml file

        FirebaseApp.initializeApp(this);//Initializes Firebase

        /*ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {//Sets listener for the main view and react when area covered by system bar changes
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);//Sets padding to the view and system bars
            return insets;
        });
         */
    }
        public void goto_LoginActivity (View view){//Method to navigate to LoginActivity
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        }
        public void goto_ActivityRegister (View view){//Method to navigate to ActivityRegister
            Intent intent = new Intent(this, ActivityRegister.class);
            startActivity(intent);

    }
}
