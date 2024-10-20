package com.example.capstone;

public class Student {//Student model
    private String name;
    private String password;
    private String teacher;

    private int score;

    private String activeGame;

    public Student() {
        //Default constructor required for calls to DataSnapshot.getValue(Student.class)
    }

    //Default constructor is required for Firestore

    public Student(String name, String password, String teacher, int score, String activeGame) {
        this.name = name;
        this.password = password;
        this.teacher = teacher;
        this.score = score;
        this.activeGame = activeGame;
    }
//Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }




    public void setPassword(String password){
        this.password = password;
    }
    public String getPassword(String password){
        return password;
    }
    public void setTeacher(String teacher){
        this.teacher = teacher;
    }
    public String getTeacher() {
        return teacher;
    }
    public int getscore() {
        return score;

    }
    public String getActiveGame() {
        return activeGame;
    }

    public void setscore(int score) {
        this.score = score;
    }

    public void setActiveGame(String activeGame) {
         this.activeGame = activeGame;
    }







}
