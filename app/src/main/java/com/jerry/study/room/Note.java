package com.jerry.study.room;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "note")
public class Note {
    @PrimaryKey
    @NonNull
    public String english;
    public String chinese;
    public String level;

}
