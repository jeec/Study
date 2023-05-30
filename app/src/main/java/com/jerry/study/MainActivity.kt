package com.jerry.study

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.jerry.study.room.DBInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.title = "开口说英语"

        val inputStream = assets.open("oral.json")
        val data = JSON.parseArray(inputStream)
        Log.i(">>>", data.size.toString())
        val rv = findViewById<RecyclerView>(R.id.rv)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = MyAdapter(data)
    }

    inner class MyAdapter(private val data: JSONArray): RecyclerView.Adapter<MyViewHolder>() {
        private val mediaPlayer = MediaPlayer()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            return MyViewHolder(layoutInflater.inflate(R.layout.item_sentence, parent, false))
        }

        override fun getItemCount(): Int {
            return data.size
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val itemData = data.getJSONObject(position)
            holder.tvEnglish?.text = itemData.getString("english")
            holder.tvChinese.text = itemData.getString("chinese")
            holder.itemView.setOnClickListener {
                val audio = itemData.getString("audio")
                mediaPlayer.reset()
                val afd = assets.openFd(audio)
                mediaPlayer.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                mediaPlayer.prepare()
                mediaPlayer.start()
            }

            holder.icon.setOnClickListener {
                Toast.makeText(this@MainActivity, "已添加到复习本", Toast.LENGTH_LONG).show()
                lifecycleScope.launch(Dispatchers.IO) {
                    DBInstance.insert(itemData)
                }
            }
        }
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvEnglish = itemView.findViewById<TextView?>(R.id.tvEnglish)
        val tvChinese = itemView.findViewById<TextView>(R.id.tvChinese)
        val icon = itemView.findViewById<ImageButton>(R.id.imBtnSound)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.note ->
                startActivity(Intent(this@MainActivity, NoteActivity::class.java))
            else ->
                supportActionBar?.title = "开口说英语" + "("+ item.title +")"

        }
        return super.onOptionsItemSelected(item)
    }

    fun addItemToNote(item: JSONObject){
        val outputStream = openFileOutput("note.json", MODE_APPEND)
        JSON.writeTo(outputStream, item)


    }
}