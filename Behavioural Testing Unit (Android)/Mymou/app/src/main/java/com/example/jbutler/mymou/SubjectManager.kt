package com.example.jbutler.mymou

import android.util.Log
import com.google.gson.GsonBuilder

import jdk.nashorn.internal.runtime.ScriptingFunctions.readLine
import java.io.*


enum class Locations {
    local, database
}

class SubjectManager(var source: Locations = Locations.local, var path: String = "") {

    var subjects: MutableList<Subject> = mutableListOf()
    var subjectNames: MutableList<String> = arrayListOf()
    var nSubjects: Int = 0
    var subjectString: String = ""
    private val gson = GsonBuilder().setPrettyPrinting().create()

    init {
        loadSubjects(source)
    }

    fun loadSubjects(mysource: Locations) {
        Log.d("SubjectManager","Trying to load subject data from: $mysource")
        when (mysource) {
            Locations.local -> loadFromDisk()
            Locations.database -> loadFromServer()
        }
        update()
    }

    private fun update() {
        nSubjects = subjects.size
        if (subjects.size > 0) {
            subjectNames = arrayListOf()
            for (i in subjects)
                subjectNames.add(i.getFullName())
        }
    }

    private fun loadFromDisk() {
        val subject1 = Subject(name = "O",comment = "UCL")
        val subject2 = Subject(name = "V", comment = "UCL")
        subjects.add(subject1)
        subjects.add(subject2)
        update()
        subjectString = gson.toJson(subjects)
        saveToDisk()
        Log.d("SubjectManager","ADD: $subjectNames")
    }

    private fun loadFromServer() {
        //TODO need to build database service and API etc.
    }

    private fun readFromFile(): String {
        val appFolder = MainMenu.folderManager.getRootFolder()
        val fileName = "subjects.json"
        val loadFile = File(appFolder, fileName)
        return usingBufferedReader(loadFile)
    }

    private fun saveToDisk() {
        val appFolder = MainMenu.folderManager.getRootFolder()
        val fileName = "subjects.json"
        val saveFile = File(appFolder, fileName)
        try {
            FileWriter(saveFile, false).apply {
                write("$subjectString")
                close()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun usingBufferedReader(filePath: File): String {
        val contentBuilder = StringBuilder()
        try {
            BufferedReader(FileReader(filePath)).use { br ->

                while ((sCurrentLine = br.readLine()) != null) {
                    contentBuilder.append(sCurrentLine).append("\n")
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return contentBuilder.toString()
    }
}