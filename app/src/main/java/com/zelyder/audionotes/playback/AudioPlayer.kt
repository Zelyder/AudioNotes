package com.zelyder.audionotes.playback

import java.io.File

interface AudioPlayer {
    fun playFile(file: File)
    fun stop()
}