package com.zelyder.audionotes.data

import android.content.Context
import android.media.MediaMetadataRetriever
import androidx.annotation.WorkerThread
import androidx.core.net.toUri
import com.zelyder.audionotes.data.model.Audio
import com.zelyder.audionotes.media.constants.AppConst
import com.zelyder.audionotes.media.record.AndroidAudioRecorder
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class ContentResolverHelper @Inject
constructor(@ApplicationContext val context: Context) {
    private val recorder by lazy {
        AndroidAudioRecorder(context)
    }

    private var lastAudioFile: File? = null
    private var index = 0L
    @WorkerThread
    fun getAudioData(): List<Audio> {
        return loadAudioFromInternalStorage()
    }

    fun getLastAudio(): Audio? {
        return fileToAudio(lastAudioFile)
    }

    private fun fileToAudio(file: File?): Audio? {
        return file?.let {
            index++
            val mediaMediaRecorder = MediaMetadataRetriever()
            mediaMediaRecorder.setDataSource(it.path)
            val displayName = it.name
            val date = mediaMediaRecorder.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)
            val duration =
                mediaMediaRecorder.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val title =
                mediaMediaRecorder.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            val uri = it.toUri()

            var timestamp = 0L
            date?.let {
                val sdf = SimpleDateFormat(AppConst.DATE_MASK_FOR_METADATA, Locale.getDefault())
                timestamp = sdf.parse(date)?.time ?: 0L
            }
            if (timestamp > 0) {
                timestamp += 10800000       // + 3 часа
            }
            Audio(
                uri = uri,
                displayName = displayName,
                id = index,
                date = timestamp,
                duration = duration?.toLong() ?: 0,
                title = title ?: displayName
            )
        }
    }

    private fun loadAudioFromInternalStorage(): List<Audio> {
        val files = context.filesDir.listFiles()

        return files?.filter { it.canRead() && it.isFile && it.name.endsWith(".3gp") }?.map {
            fileToAudio(it)!!
        } ?: emptyList()
    }

    fun saveAudioToInternalStorage(fileName: String): Boolean {
        return try {
            File(context.filesDir, "${fileName}.3gp").also { file ->
                recorder.start(file)
                lastAudioFile = file
            }
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    fun deleteAudioFromInternalStorage(fileName: String): Boolean {
        return try {
            context.deleteFile(fileName)
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    fun stopRecordingAudio(fileName: String? = null) {
        fileName?.let {
            lastAudioFile?.renameTo(File(context.filesDir, "${fileName}.3gp"))
        }
        recorder.stop()
    }
}