package com.jerry.study

import android.content.res.AssetManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.jerry.study.room.DBInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class MainViewModel: ViewModel() {
    private val data = MutableLiveData<Pair<JSONArray, JSONArray>>()
    private val tempArrayData = JSONArray()

    fun getNotes(): LiveData<Pair<JSONArray, JSONArray>> {
        return data
    }

    fun loadNotes(assets: AssetManager, jsonLevelFileName: String){
        viewModelScope.launch(Dispatchers.IO) {
            val asset = async {
                val inputStream = assets.open(jsonLevelFileName)
                JSON.parseArray(inputStream)
            }
            val db = async {
                DBInstance.getAll()
            }
            val fileJson = asset.await()
            val noteJson = db.await()
            data.postValue(Pair(fileJson, noteJson))
        }
    }

    fun updateTempNote(bAdd: Boolean, tempData: JSONObject?): Unit {
        if (bAdd) {
            tempArrayData.add(tempData)
        } else {
            tempArrayData.remove(tempData)
        }
    }

    fun getTempData(): JSONArray{
        return tempArrayData
    }
}