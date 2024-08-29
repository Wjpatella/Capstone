package com.example.capstone;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import com.example.capstone.databinding.DirectoryPageBinding;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class Directory extends AppCompatActivity {

    DirectoryPageBinding binding; //binds the xml page to the Directory

    private ProfileFragment profileFragment;

    private GamesFragment gamesFragment;

    private LeaderBoardFragment leaderBoardFragment;
    private ClassroomFragment classroomFragment;



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
        classroomFragment = new ClassroomFragment();
        leaderBoardFragment = new LeaderBoardFragment();

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
                loadFragment(classroomFragment);
                return true;
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
