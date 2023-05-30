package com.jerry.study

import android.app.Application
import android.content.Context

class MyApp: Application() {
        companion object{
            var context: Context? = null
        }

        override fun onCreate() {
        super.onCreate()
        context = this@MyApp
    }
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
    }
}