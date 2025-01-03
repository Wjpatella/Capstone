
package com.example.capstone;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {


    private EditText Username_input;

    private EditText Password_input;

    private Button  Press_login_button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);


        Username_input = findViewById(R.id.username_input);
        Password_input = findViewById(R.id.password_input);

        Press_login_button = findViewById(R.id.login_button);




    Press_login_button.setOnClickListener(new View.OnClickListener(){
        @Override
        public void onClick(View v){
            String username = Username_input.getText().toString().trim();//convert to string
            String password = Password_input.getText().toString().trim();//convert to string

        if (username.isEmpty() || password.isEmpty()) {//checks to make sure all fields are filled
            Toast.makeText(LoginActivity.this, "Please enter all the fields.", Toast.LENGTH_SHORT).show();
            Toast.makeText(LoginActivity.this, "すべての項目を入力してください。", Toast.LENGTH_SHORT).show();
        } else {

            FS_DBHelper.Student_online=false;//reset the online status
            FS_DBHelper.Teacher_online=false;

            //calls the checkUsers method from the FS_DBHelper class to check if the user exists
            FS_DBHelper.checkUsers(username, password, new FS_DBHelper.OnUserCheckCompleteListener() {
                @Override
                public void onSuccess() {
                    Toast.makeText(LoginActivity.this, "Login successful.", Toast.LENGTH_SHORT).show();
                    Toast.makeText(LoginActivity.this, "ログインに成功しました。", Toast.LENGTH_SHORT).show();
                    //go to directory   go_to_directory(v);
                    Intent intent = new Intent(LoginActivity.this, Directory.class);
                    startActivity(intent);
                }

                @Override
                public void onFailure() {
                    // Handle failed login
                    Toast.makeText(LoginActivity.this, "Login failed.", Toast.LENGTH_SHORT).show();
                    Toast.makeText(LoginActivity.this, "ログインに失敗しました。", Toast.LENGTH_SHORT).show();
                    }

                    });

                }

            }
        });

    }


    public void goto_main_activity(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }


}