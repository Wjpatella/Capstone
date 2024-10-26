package com.example.capstone;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import android.content.Context;
import android.net.ConnectivityManager;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import android.net.NetworkInfo;

import com.google.firebase.FirebaseApp;


public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);//enables edge to edge display
        setContentView(R.layout.login_page); //UI xml file

        FirebaseApp.initializeApp(this);//Initializes Firebase

        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No internet connection. Please check your connection.", Toast.LENGTH_SHORT).show();
            Toast.makeText(this, "インターネットに接続できません。接続を確認してください。", Toast.LENGTH_SHORT).show();
        }
    }

    //Method to check if the device has an active network connection
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    //Method to navigate to LoginActivity
        public void goto_LoginActivity (View view){
        if (isNetworkAvailable()) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        }else{
            Toast.makeText(this, "No internet connection. Please check your connection.", Toast.LENGTH_SHORT).show();
            Toast.makeText(this, "インターネットに接続できません。接続を確認してください。", Toast.LENGTH_SHORT).show();
        }
        }
    //Method to navigate to ActivityRegister
        public void goto_ActivityRegister (View view){//Method to navigate to ActivityRegister
        if (isNetworkAvailable()) {
            Intent intent = new Intent(this, ActivityRegister.class);
            startActivity(intent);
        } else {
            Toast.makeText(this, "No internet connection. Please check your connection.", Toast.LENGTH_SHORT).show();
            Toast.makeText(this, "インターネットに接続できません。接続を確認してください。", Toast.LENGTH_SHORT).show();
        }

    }
}
