package com.example.jbutler.mymou

import android.os.Environment
import android.util.Log

import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Created by jbutler on 18/09/2018.
 * Checks if session folder already exists, and creates it if not
 */

class FolderManager {

    private val TAG = "FolderManager"
    var suffixes = arrayListOf("i","f")
    var currentFolder: File? = null

    init {
        getFolder()
    }

    fun getFolder(): File {
        val appFolder = File(makeFullPathName())
        if (!appFolder.exists()) {
            Log.d(TAG, "$appFolder doesn't exist...")
            appFolder.mkdirs()
            when (appFolder.exists()) {
                true -> makeFoldersForSession(appFolder)
                false -> Log.e(TAG,"CANNOT MAKE BASE FOLDER!!!")
            }
        }
        currentFolder = appFolder
        return appFolder
    }

    fun getSubFolder(suffix: String = ""): File? {
        if (currentFolder == null) getFolder()
        Log.d(TAG, "Getting $currentFolder/$suffix")
        return when (suffix) {
            "" -> currentFolder
            else -> File(currentFolder,suffix)
        }
    }

    fun getRootFolder(): File? {
        if (currentFolder == null) getFolder()
        return File(currentFolder?.parent)
    }

    fun addFolders(newSuffixes: MutableList<String>) {
        suffixes.addAll(newSuffixes)
        if (currentFolder == null) {
            getFolder()
        }
        makeFoldersForSession(currentFolder)
    }

    fun getBaseDate(): String {
        return SimpleDateFormat("yyyyMMdd", Locale.ENGLISH).format(System.currentTimeMillis()) //API < 26
        //return DateTimeFormatter.BASIC_ISO_DATE.format(LocalDate.now()) //API > 25
    }

    fun getTimestamp(): String {
        return SimpleDateFormat("HHmmss_SSS", Locale.ENGLISH).format(System.currentTimeMillis()) //API < 26
    }

    private fun makeFoldersForSession(path: File?) {
        if (path == null) return
        for (suffix in suffixes) {
            Log.d(TAG, "... making sub-folder $suffix ...")
            File(path, suffix).mkdirs()
        }
    }

    private fun makeFullPathName(): String {
        return Environment.getExternalStorageDirectory().absolutePath + "/Mymou/" + getBaseDate()
    }
}
