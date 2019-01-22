package com.example.jbutler.mymou;

import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileWriter;

/**
 * Writes linked String into  CURRENT_DATE.txt
 */
class LogEvent implements Runnable {

    private final String message;

    public LogEvent(String msg) {
        message = msg;
    }

    @Override
    public void run() {
        //Log.d("LogEvent","Start logging @ " + Thread.currentThread().getName());
        File appFolder = MainMenu.folderManager.getFolder();
        String fileName = MainMenu.folderManager.getBaseDate() + ".txt";
        File saveFile = new File(appFolder, fileName);
        try {
            FileWriter thisWriter = new FileWriter(saveFile, true);
            thisWriter.write("$message\n");
            thisWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //Log.d("LogEvent",message + " logged...");
    }

}
