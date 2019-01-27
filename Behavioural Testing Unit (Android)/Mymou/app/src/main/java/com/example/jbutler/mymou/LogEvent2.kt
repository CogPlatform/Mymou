package com.example.jbutler.mymou

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.io.IOException

object LogEvent2 {

    private val TAG: String = "LogEvent2"
    private val ioJob = Job()
    private val ioScope = CoroutineScope(Dispatchers.IO + ioJob)

    fun writeMessage(message: String) {
        ioScope.launch {
            val thisFolder = FolderManager.getFolder()
            val fileName = "${FolderManager.getBaseDate()}.txt"
            val saveFile = File(thisFolder, fileName)
            try {
                FileWriter(saveFile, true).apply {
                    write("$message\n")
                    close()
                }
                Log.d(TAG, "writeMessage():$message in ${Thread.currentThread().name} @ ${System.currentTimeMillis()} > $saveFile ")
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun cancel() {
        ioJob.cancel()
    }

}