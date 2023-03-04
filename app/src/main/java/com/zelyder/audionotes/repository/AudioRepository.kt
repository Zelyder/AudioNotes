package com.zelyder.audionotes.repository

import com.zelyder.audionotes.data.ContentResolverHelper
import com.zelyder.audionotes.data.model.Audio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AudioRepository @Inject
constructor(private val contentResolverHelper: ContentResolverHelper) {
    suspend fun getAudioData(): List<Audio> = withContext(Dispatchers.IO) {
        contentResolverHelper.getAudioData()
    }

    suspend fun getLastAudio(): Audio? = withContext(Dispatchers.IO) {
        contentResolverHelper.getLastAudio()
    }

    suspend fun startRecordingAudio(fileName: String) =
        withContext(Dispatchers.IO) {
            contentResolverHelper.saveAudioToInternalStorage(fileName)
        }

    suspend fun stopRecordingAudio() = withContext(Dispatchers.IO) {
        contentResolverHelper.stopRecordingAudio()
    }
}