package com.sshvan.tunnelmanager

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

@HiltAndroidApp
class TunnelManagerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            try {
                val sw = StringWriter()
                exception.printStackTrace(PrintWriter(sw))
                val crashFile = File(filesDir, "crash.txt")
                crashFile.writeText(sw.toString())
            } catch (e: Exception) {
                // Ignore
            }
            defaultHandler?.uncaughtException(thread, exception)
            exitProcess(1)
        }
    }
}
