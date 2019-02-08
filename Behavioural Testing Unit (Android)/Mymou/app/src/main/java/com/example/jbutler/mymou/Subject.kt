package com.example.jbutler.mymou

import java.util.UUID

data class Subject(
    var name: String = "",
    val uuid: String = UUID.randomUUID().toString(),
    var comment: String = "",
    var localLog: MutableList<String> = mutableListOf()
) {
    fun getFullName(): String {
        return "$name[${uuid.slice(0..7)}]"
    }

    fun addToSubjectLog(message: String?) {
        if (message != null) {
            localLog.add(message)
        }
    }
}