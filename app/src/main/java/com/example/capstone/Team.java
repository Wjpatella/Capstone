package com.example.capstone;

import java.util.List;

//Team module class
public class Team {
    private String teamName;
    private List<String> studentNames;

    // Constructor
    public Team() {
        // Default constructor
    }

    public Team(String teamName, List<String> studentNames) {
        this.teamName = teamName;
        this.studentNames = studentNames;
    }

    //Getter for student names
    public List<String> getStudentNames() {
        return studentNames;
    }

    //Setter for student names
    public void setStudentNames(List<String> studentNames) {
        this.studentNames = studentNames;
    }

    //Getter and Setter for teamName
    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }
}