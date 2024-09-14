package com.example.capstone;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

//Adapter for Disband Team
public class TeamDisbandAdapter extends RecyclerView.Adapter<TeamDisbandAdapter.TeamViewHolder> {
    private List<Team> teams;
    private List<Team> selectedTeams = new ArrayList<>();

    public TeamDisbandAdapter(List<Team> teamsList) {
        this.teams = teamsList;
    }

    public List<Team> getSelectedTeams() {
        return selectedTeams;
    }

    @NonNull
    @Override
    public TeamViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_team_for_disband, parent, false); // Inflate item_team_disband.xml
        return new TeamViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull TeamViewHolder holder, int position) {
        Team team = teams.get(position);
        holder.teamNameTextView.setText(team.getTeamName());

        // Display student names
        StringBuilder members = new StringBuilder();
        for (String studentName : team.getStudentNames()) {
            members.append(studentName).append(", ");
        }
        // Remove trailing comma and space
        if (members.length() > 0) {
            members.setLength(members.length() - 2);
        }
        holder.membersTextView.setText(members.toString());

        // Set checkbox state
        holder.checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedTeams.add(team);
            } else {
                selectedTeams.remove(team);
            }
        });

        //To handle the case where the state of the checkbox might be reset
        holder.checkbox.setChecked(selectedTeams.contains(team));
    }

    @Override
    public int getItemCount() {
        return teams.size();
    }

    static class TeamViewHolder extends RecyclerView.ViewHolder {
        TextView teamNameTextView;
        TextView membersTextView;
        CheckBox checkbox; //Add a checkbox

        public TeamViewHolder(@NonNull View itemView) {
            super(itemView);
            teamNameTextView = itemView.findViewById(R.id.team_name_disband);
            membersTextView = itemView.findViewById(R.id.names_in_team_disband);
            checkbox = itemView.findViewById(R.id.team_disband_checkbox);
        }
    }
}