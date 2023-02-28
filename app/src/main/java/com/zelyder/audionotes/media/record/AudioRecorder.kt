package com.zelyder.audionotes.media.record

import java.io.File

interface AudioRecorder {
    fun start(outputFile: File)
    fun stop()
}