package com.jerry.study.room;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "note")
public class Note {
    @PrimaryKey(autoGenerate = true)
    public Integer uid;
    public String chinese;
    public String english;
    public String audio;

}
