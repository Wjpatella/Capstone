package com.example.capstone;

public class VocabularyItem {
    private String example_sentence;
    private String meaning;
    private String parts_of_speech_en;
    private String parts_of_speech_jp;
    private String word;

    // Default constructor required for calls to DataSnapshot.getValue(VocabularyItem.class)
    public VocabularyItem() {}

    public VocabularyItem(String example_sentence, String meaning, String parts_of_speech_en, String parts_of_speech_jp, String word) {
        this.example_sentence = example_sentence;
        this.meaning = meaning;
        this.parts_of_speech_en = parts_of_speech_en;
        this.parts_of_speech_jp = parts_of_speech_jp;
        this.word = word;
    }

    // Getters
    public String getExample_sentence() { return example_sentence; }
    public String getMeaning() { return meaning; }
    public String getParts_of_speech_en() { return parts_of_speech_en; }
    public String getParts_of_speech_jp() { return parts_of_speech_jp; }
    public String getWord() { return word; }
}
