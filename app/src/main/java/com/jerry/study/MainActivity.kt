package com.jerry.study

import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.jerry.study.room.DBInstance
import com.jerry.study.room.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private val vm: MainViewModel by viewModels()
    private var adapter: MyAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.title = "开口说英语"

        observeEvent()

        val rv = findViewById<RecyclerView>(R.id.rv)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = MyAdapter()
        rv.adapter = adapter
        vm.loadNotes(assets,"oral.json")
    }

    private fun observeEvent() {
        vm.getNotes().observe(this) {
            adapter?.setData(it)
        }
    }

    inner class MyAdapter(): RecyclerView.Adapter<MyViewHolder>() {
        private val mediaPlayer = MediaPlayer()
        private var pairData: Pair<JSONArray, JSONArray>? = null
        public fun setData(pairParam: Pair<JSONArray, JSONArray>?) {
            pairData = pairParam
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            return MyViewHolder(layoutInflater.inflate(R.layout.item_sentence, parent, false))
        }

        override fun getItemCount(): Int {
            return pairData?.first?.size?:0
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val itemData = pairData?.first?.getJSONObject(position)
            val itemEnglish = itemData?.getString("english")
            holder.tvEnglish?.text = itemEnglish
            holder.tvChinese.text = itemData?.getString("chinese")

            setBtnStyle(false, holder)
            pairData?.second?.forEach {
                val noteEnglish = (it as Note).english
                if (noteEnglish == itemEnglish) {
                    setBtnStyle(true, holder)
                    return@forEach
                }
            }

            vm.getTempData().forEach {
                val tempEnglish = (it as JSONObject).getString("english")
                if (tempEnglish == itemEnglish) {
                    setBtnStyle(true, holder)
                    return@forEach
                }
            }

            holder.itemView.setOnClickListener {
                itemData?.let {
                    val audio = it.getString("audio")
                    mediaPlayer.reset()
                    val afd = assets.openFd(audio)
                    mediaPlayer.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    mediaPlayer.prepare()
                    mediaPlayer.start()
                }
            }

            holder.icon.setOnClickListener {
                val bStatus = holder.icon.text.toString() == "-"
                if (!bStatus) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        DBInstance.insert(itemData)
                    }
                } else {
                    lifecycleScope.launch(Dispatchers.IO) {
                        DBInstance.delete(itemData)
                    }
                }
                val textToast = if (!bStatus) "已添加到复习本" else "已从复习本删除"
                Toast.makeText(this@MainActivity, textToast, Toast.LENGTH_SHORT).show()
                setBtnStyle(!bStatus, holder)
                vm.updateTempNote(!bStatus, itemData)
            }
        }
    }

    private fun setBtnStyle(bAdded: Boolean, holder: MyViewHolder): Unit {
        holder.icon.text = if (bAdded) {
            holder.icon.setTextColor(Color.LTGRAY)
            "-"
        } else {
            holder.icon.setTextColor(getColor(R.color.teal_200))
            "+"
        }
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvEnglish = itemView.findViewById<TextView?>(R.id.tvEnglish)
        val tvChinese = itemView.findViewById<TextView>(R.id.tvChinese)
        val icon = itemView.findViewById<Button>(R.id.imBtnSound)
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
}