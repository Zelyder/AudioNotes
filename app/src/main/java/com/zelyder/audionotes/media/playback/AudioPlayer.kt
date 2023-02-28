package com.zelyder.audionotes.media.playback

import java.io.File

interface AudioPlayer {
    fun playFile(file: File)
    fun stop()
}