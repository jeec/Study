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
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.jerry.study.room.DBInstance
import com.jerry.study.room.Note
import com.microsoft.cognitiveservices.speech.SpeechSynthesisResult
import com.microsoft.cognitiveservices.speech.SpeechSynthesizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    companion object {
        private const val PERIOD = 1  //MINUTES
    }
    private var levelFileName: String = ""
    private val levelFileNamePrefix: String
        get() = levelFileName.split(".")[0]
    private val chineseLevelName: String
        get() {
            return when(levelFileName){
                "oral_level_1.json" -> {
                    "一级"
                }
                "oral_level_2.json" -> {
                    "二级"
                }
                else -> ""
            }
        }
    private val vm: MainViewModel by viewModels()
    private var adapter: MyAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        lifecycleScope.launch(Dispatchers.Main) {
            val bGoOn = vm.spGetString("within", "")
            if(bGoOn.isNullOrEmpty()) {
                if(getNetTime() - BuildConfig.BUILD_TIME < PERIOD * 60 * 1000) {
                    //在指定的时间内安装并打开了应用
                    vm.spSaveString("within", "cong, you can use it!")
                } else {
                    Toast.makeText(this@MainActivity, "未授权设备，3秒后将关闭", Toast.LENGTH_LONG).show()
                    lifecycleScope.launch {
                        delay(3000)
                        finish()
                    }
                }
            }
        }


        levelFileName = vm.spGetString("level", "oral_level_1.json")?:"oral_level_1.json"

        supportActionBar?.title = "开口说英语($chineseLevelName)"

        val rv = findViewById<RecyclerView>(R.id.rv)
        rv.layoutManager = LinearLayoutManager(this)

        adapter = MyAdapter()
        rv.adapter = adapter
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED){
                vm.getTempData().clear()
                vm.loadNotes(assets, levelFileName)
            }
        }

        rv.addOnScrollListener(object : OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if(recyclerView.layoutManager != null) {
                    getPositionAndOffset(rv)
                }
            }
        })
        observeEvent(rv)
    }

    //获取网络时间
    private suspend fun getNetTime(): Long {
        return withContext(Dispatchers.IO) {
            val url = URL("https://www.baidu.com")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5 * 1000
            conn.connect()
            val date = conn.date
            date
        }
    }


    private fun getPositionAndOffset(rv: RecyclerView) {
        L.i(levelFileNamePrefix)
        val layoutManager = rv.layoutManager
        //获取可视的第一个view
        val topView: View? = layoutManager?.getChildAt(0)
        if (topView != null) {
            //获取与该view的顶部的偏移量
            val lastOffset = topView.top
            //得到该View的数组位置
            val lastPosition = layoutManager.getPosition(topView)
            vm.spSaveString("lastOffset$levelFileNamePrefix", lastOffset.toString())
            vm.spSaveString("lastPosition$levelFileNamePrefix", lastPosition.toString())
        }
    }

    private fun scrollToPosition(rv: RecyclerView) {
        val lastPosition = vm.spGetString("lastPosition$levelFileNamePrefix", "0")?.toInt()?:0
        val lastOffset = vm.spGetString("lastOffset$levelFileNamePrefix", "0")?.toInt()?:0
        if (rv.layoutManager != null && lastPosition >= 0) {
            (rv.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                lastPosition,
                lastOffset
            )
        }
    }

    private fun observeEvent(rv: RecyclerView) {
        vm.getNotes().observe(this) {
            adapter?.setData(it)
//            if (it.third) {
//                rv.scrollToPosition(0)
//            }
            scrollToPosition(rv)
        }
    }

    inner class MyAdapter(): RecyclerView.Adapter<MyViewHolder>() {
        private val mediaPlayer = MediaPlayer()
        private var pairData: Triple<JSONArray, JSONArray, Boolean>? = null
        public fun setData(pairParam: Triple<JSONArray, JSONArray, Boolean>?) {
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
                    val audio = it.getString("level") + ".mp3"
                    mediaPlayer.reset()
                    val afd = assets.openFd("I_am_ok.mp3")
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
            R.id.level_1 ->{
                loadJsonByDifferentLevel("oral_level_1.json", item.title)
            }
            R.id.level_2 ->{
                loadJsonByDifferentLevel("oral_level_2.json", item.title)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun loadJsonByDifferentLevel(fileName: String, title: CharSequence?): Unit {
        levelFileName = fileName
        vm.spSaveString("level", fileName)
        vm.loadNotes(assets, fileName,true)
        vm.getTempData().clear()
        supportActionBar?.title = "开口说英语($title)"
    }

}