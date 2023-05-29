package com.jerry.study

import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONArray

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val inputStream = assets.open("oral.json")
        val data = JSON.parseArray(inputStream)
        Log.i(">>>", data.size.toString())
        val rv = findViewById<RecyclerView>(R.id.rv)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = MyAdapter(data)
    }

    inner class MyAdapter(private val data: JSONArray): RecyclerView.Adapter<MyViewHolder>() {
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
            holder.icon.setOnClickListener {
                val audio = itemData.getString("audio")
                val mediaPlayer = MediaPlayer()
                val afd = assets.openFd(audio)
                mediaPlayer.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                mediaPlayer.prepare()
                mediaPlayer.start()
            }
        }
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvEnglish = itemView.findViewById<TextView?>(R.id.tvEnglish)
        val tvChinese = itemView.findViewById<TextView>(R.id.tvChinese)
        val icon = itemView.findViewById<ImageButton>(R.id.imBtnSound)
    }
}