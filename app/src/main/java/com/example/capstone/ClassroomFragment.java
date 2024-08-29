package com.example.capstone;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View;
import androidx.fragment.app.Fragment;
public class ClassroomFragment extends Fragment {

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_classroom, container, false); //inflate the layout

        return view;
    }


}
