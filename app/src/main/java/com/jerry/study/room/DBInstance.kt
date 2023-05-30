package com.jerry.study.room

import android.util.Log
import androidx.room.Room
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.jerry.study.L
import com.jerry.study.MyApp

object DBInstance {
    private val db = Room.databaseBuilder(
        MyApp.context!!,
        AppDatabase::class.java, "db-note"
    ).build()

    suspend fun insert(jNote: JSONObject) {
        val noteDao = db.noteDao()
        val bean = jNote.to(Note::class.java)
        L.i(bean)
        noteDao.insert(bean)
    }

    suspend fun delete(jNote: JSONObject) {
        val noteDao = db.noteDao()
        val bean = jNote.toJavaObject(Note::class.java)
        noteDao.delete(bean)
    }


    suspend fun getAll(): JSONArray{
        val noteDao = db.noteDao()
        val notes = noteDao.getAll()
        return JSONArray(notes)
    }
}