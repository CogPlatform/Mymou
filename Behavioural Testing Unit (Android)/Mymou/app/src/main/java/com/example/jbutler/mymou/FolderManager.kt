package com.example.jbutler.mymou

import android.os.Environment
import android.util.Log

import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by jbutler on 18/09/2018.
 * Checks if session folder already exists, and creates it if not
 */

internal object FolderManager {

    private val TAG = "FolderManager:"
    private val uuid = UUID.randomUUID().toString()
    var currentFolder = ""
    var folderExists = false


    fun getFolder(): File {
        Log.d(TAG+uuid, "getFolder()")
        val thisPath = makeFullPathName()
        val appFolder = File(thisPath)
        if (!appFolder.exists()) {
            Log.d(TAG+uuid, "$appFolder doesn't exist...")
            appFolder.mkdirs()
            if (appFolder.exists()) {
                makeFoldersForSession(thisPath)

            } else {
                Log.e(TAG,"CANNOT MAKE FOLDER!!!")
            }
        }
        currentFolder = thisPath
        folderExists = appFolder.exists()
        return appFolder
    }

    private fun makeFoldersForSession(path: String) {
        Log.d(TAG+uuid, "making sub-folders..")
        makeFolder(path, "i")
        makeFolder(path, "f")
        makeFolder(path, "O")
        makeFolder(path, "V")
    }

    private fun makeFullPathName(): String {
        return Environment.getExternalStorageDirectory().absolutePath +
                "/Mymou/" +
                SimpleDateFormat("YYYYMMDD", Locale.ENGLISH).format(System.currentTimeMillis())
    }

    private fun makeFolder(path: String, suffix: String) {
        File("$path/$suffix/").mkdirs()
    }
}
