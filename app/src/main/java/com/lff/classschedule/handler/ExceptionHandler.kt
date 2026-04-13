package com.lff.classschedule.handler

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.lff.classschedule.HomeActivity
import com.lff.classschedule.MainActivity
import com.lff.classschedule.config.SharedPreferenceConfig
import kotlin.system.exitProcess

class ExceptionHandler: Thread.UncaughtExceptionHandler {

    companion object{
        private const val TAG = "ExceptionHandler"
        val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { ExceptionHandler() }
    }

    private lateinit var mContext: Context
    private var mDefaultHandler: Thread.UncaughtExceptionHandler? = null

    fun init(context: Context) {
        mContext = context.applicationContext
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }


    override fun uncaughtException(t: Thread, e: Throwable) {
        val realCause = getRealCause(e)

        if (realCause is NumberFormatException){
            Thread {
                Looper.prepare()
                Toast.makeText(mContext, "出现数据错误，已重置设置", Toast.LENGTH_SHORT).show()

                Handler(Looper.myLooper()!!).postDelayed({
                    Looper.myLooper()?.quit()
                }, 200)
                Looper.loop()

                val intent = Intent(mContext, HomeActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                mContext.startActivity(intent)

                android.os.Process.killProcess(android.os.Process.myPid())
                exitProcess(1)
            }.start()
        }else{
            mDefaultHandler?.uncaughtException(t,e)
        }
    }

    private fun getRealCause(throwable: Throwable): Throwable {
        val cause = throwable.cause
        return if (cause == null) throwable else getRealCause(cause)
    }
}