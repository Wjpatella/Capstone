package com.example.capstone;

import static com.example.capstone.TeacherGameFragment.GameActive;


import androidx.fragment.app.Fragment;

import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;


public class StudentGameFragment extends Fragment {

    private Button JoinGameButton;
    private View gameStatusIndicator;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.student_game_fragment, container, false);

        JoinGameButton = view.findViewById(R.id.join_game_button);
        gameStatusIndicator = view.findViewById(R.id.game_status_indicator); // Initialize gameStatusIndicator

        updateIndicatorColor(); // Call after initializing gameStatusIndicator

        JoinGameButton.setOnClickListener(v -> goto_DrawlingGameActivity());

        return view;
    }

    private void updateIndicatorColor() {
        if (GameActive) {
            gameStatusIndicator.setBackgroundColor(getResources().getColor(R.color.green));
        } else {
            gameStatusIndicator.setBackgroundColor(getResources().getColor(R.color.red));
        }
    }

    public void goto_DrawlingGameActivity() {
        Intent intent = new Intent(getActivity(), DrawlingGameActivity.class);
        startActivity(intent);
    }

    //need method to check if game is active or not so if student try's to join a game that is not active
}
