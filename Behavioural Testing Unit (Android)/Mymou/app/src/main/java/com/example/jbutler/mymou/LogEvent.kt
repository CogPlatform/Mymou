package com.example.jbutler.mymou

import java.io.File
import java.io.IOException
import java.io.FileWriter

/**
 * Writes linked String into CURRENT_DATE.txt
 */
internal class LogEvent(private val message: String) : Runnable {
    override fun run() {
        val appFolder = MainMenu.folderManager.getFolder()
        val fileName = MainMenu.folderManager.getBaseDate() + ".txt"
        val saveFile = File(appFolder, fileName)
        try {
            FileWriter(saveFile, true).apply {
                write("$message\n\n")
                close()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}