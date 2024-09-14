package com.example.capstone;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

//TeamAdapter for RecyclerView
public class TeamAdapter extends RecyclerView.Adapter<TeamAdapter.TeamViewHolder> {
    private List<Team> teams;

    public TeamAdapter(List<Team> teamsList) {
        this.teams = teamsList;
    }

    @NonNull
    @Override
    public TeamViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_team, parent, false);
        return new TeamViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TeamViewHolder holder, int position) {
        Team team = teams.get(position);
        holder.teamNameTextView.setText(team.getTeamName());

        //Display student names
        StringBuilder members = new StringBuilder();
        for (String studentName : team.getStudentNames()) { // Use getStudentNames()
            members.append(studentName).append(", ");
        }
        //Remove trailing comma and space
        if (members.length() > 0) {
            members.setLength(members.length() - 2);
        }
        holder.membersTextView.setText(members.toString());
    }

    @Override
    public int getItemCount() {
        return teams.size();
    }

    static class TeamViewHolder extends RecyclerView.ViewHolder {
        TextView teamNameTextView;
        TextView membersTextView;

        public TeamViewHolder(@NonNull View itemView) {
            super(itemView);
            teamNameTextView = itemView.findViewById(R.id.team_name);
            membersTextView = itemView.findViewById(R.id.members_text_view);
        }
    }
}