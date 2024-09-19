package com.example.capstone;

public class Student {//Student model
    private String name;
    private String password;
    private String teacher;

    private String class_id;

    private String activeGame;

    public Student() {
        //Default constructor required for calls to DataSnapshot.getValue(Student.class)
    }

    //Default constructor is required for Firestore

    public Student(String name, String password, String teacher, String class_id, String activeGame) {
        this.name = name;
        this.password = password;
        this.teacher = teacher;
        this.class_id = class_id;
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
    public String getClass_id() {
        return class_id;

    }

    public void setClass_id(String class_id) {
        this.class_id = class_id;
    }

    public void setActiveGame(String activeGame) {
         this.activeGame = activeGame;
    }
    public String getActiveGame() {
        return activeGame;
    }






}
