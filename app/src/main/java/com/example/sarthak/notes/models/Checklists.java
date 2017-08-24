package com.example.sarthak.notes.models;

import java.io.Serializable;
import java.util.HashMap;

public class Checklists implements Serializable {

    private String notesType;
    private String notesTitle;
    private HashMap<String, HashMap<String, String>> content = new HashMap<>();

    public Checklists() {

    }

    public Checklists(String notesTitle, HashMap<String, HashMap<String, String>> dataMap) {

        this.notesType = "Checklists";
        this.notesTitle = notesTitle;
        this.content = dataMap;
    }

    public String getNotesType() {
        return "Checklists";
    }

    public String getNotesTitle() {
        return notesTitle;
    }

    public HashMap<String, HashMap<String, String>> getContent() {
        return content;
    }
}