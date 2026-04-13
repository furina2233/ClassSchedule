package com.lff.classschedule.config

import android.app.Application
import com.lff.classschedule.handler.ExceptionHandler

class MyApp: Application() {
    override fun onCreate() {
        super.onCreate()

        ExceptionHandler.instance.init(this)
    }
}