package com.example.capstone;

import static com.example.capstone.FS_DBHelper.Online_user_id;
import static com.example.capstone.FS_DBHelper.Teacher_online;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.example.capstone.databinding.DirectoryPageBinding;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class Directory extends AppCompatActivity {

    DirectoryPageBinding binding; //binds the xml page to the Directory

    private ProfileFragment profileFragment;

    private GamesFragment gamesFragment;

    private LeaderBoardFragment leaderBoardFragment;
    private ClassroomFragment teacher_classroomFragment;
    private StudentClassroomFragment student_classroomFragment;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DirectoryPageBinding.inflate(getLayoutInflater()); //inflates layout of page
        setContentView(binding.getRoot());//sets the content of the page

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Intent intent = getIntent();//receives intent from previous activity

        //Initializes fragments
        profileFragment = new ProfileFragment();
        gamesFragment = new GamesFragment();
        leaderBoardFragment = new LeaderBoardFragment();

            if (Teacher_online==true) {

                teacher_classroomFragment = new ClassroomFragment();
            }
            else{
                student_classroomFragment = new StudentClassroomFragment();
            }

        //loads the default fragment profileFragment
        loadFragment(profileFragment);


        BottomNavigationView bottomNavigationView = findViewById(R.id.directory_nav);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            //checks which item is selected
            int itemId = item.getItemId();

            if (itemId == R.id.nav_profile) {
                loadFragment(profileFragment);
                return true;
            } else if (itemId == R.id.nav_games) {
                loadFragment(gamesFragment);
                return true;
            } else if (itemId == R.id.nav_leaderboard) {
                loadFragment(leaderBoardFragment);
                return true;
            } else if (itemId == R.id.nav_classroom) {
                if (Teacher_online==true) {
                    loadFragment(teacher_classroomFragment);
                    return true;
                }
                else{
                    loadFragment(student_classroomFragment);
                    return true;
                }

            }

            return false;
        });
    }

    private void loadFragment(Fragment fragment) {//loads the fragments in the container in the xml page
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.directory_container, fragment)
                .commit();
    }
}
