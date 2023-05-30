package com.jerry.study

import android.util.Log

object L {
    fun i(msg: Any = "+++++++++++++++++++++++"): Unit {
        Log.i(">>>", msg.toString())
    }
}