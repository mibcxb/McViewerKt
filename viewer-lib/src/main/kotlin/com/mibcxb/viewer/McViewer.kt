package com.mibcxb.viewer

import java.io.File

object McViewer {
    val dataRoot: File by lazy {
        val appData = File("data")
        val homeDir = System.getProperty("user.home")
        val usrData = File(homeDir, ".mcviewer")
        if (appData.exists() && appData.isDirectory) {
            appData
        } else {
            usrData.apply { if (!exists()) mkdirs() }
        }
    }
}