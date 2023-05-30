package com.jerry.study.room

import androidx.room.*

@Dao
interface NoteDao {
    @Query("SELECT * FROM note")
    fun getAll(): List<Note>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(note: Note?)

    @Delete(entity = Note::class)
    fun delete(user: Note?)
}