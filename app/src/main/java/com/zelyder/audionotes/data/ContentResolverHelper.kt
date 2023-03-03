package com.zelyder.audionotes.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
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

    private var mCursor: Cursor? = null

    private val projection: Array<String> = arrayOf(
        MediaStore.Audio.AudioColumns.DISPLAY_NAME,
        MediaStore.Audio.AudioColumns._ID,
        MediaStore.Audio.AudioColumns.DATE_ADDED,
        MediaStore.Audio.AudioColumns.DURATION,
        MediaStore.Audio.AudioColumns.TITLE,
    )

    private var selectionClause: String? =
        "${
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                MediaStore.Audio.AudioColumns.IS_RECORDING else
                MediaStore.Audio.AudioColumns.IS_MUSIC
        } = ? "
    private var selectionArg = arrayOf("1")
    private val sortOrder = "${MediaStore.Audio.AudioColumns.DISPLAY_NAME} ASC"

    @WorkerThread
    fun getAudioData(): List<Audio> {
        return loadAudioFromInternalStorage()
    }

    private fun loadAudioFromInternalStorage(): List<Audio> {
        val files = context.filesDir.listFiles()
        var i = 0L
        return files?.filter { it.canRead() && it.isFile && it.name.endsWith(".3gp") }?.map {
            i++
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
                id = i,
                date = timestamp,
                duration = duration?.toLong() ?: 0,
                title = title ?: displayName
            )
        } ?: emptyList()
    }

    fun saveAudioToInternalStorage(fileName: String): Boolean {
        return try {
            recorder.start(
                File(context.filesDir, "${fileName}.3gp")
            )
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    fun startRecordingAudio(fileName: String): Boolean {
        val audioCollection = sdk29AndUp {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } ?: MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val contentValues = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Audio.Media.TITLE, fileName)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/3gpp")
            put(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    MediaStore.Audio.AudioColumns.IS_RECORDING else
                    MediaStore.Audio.AudioColumns.IS_MUSIC, true
            )
            sdk29AndUp {
                put(
                    MediaStore.Audio.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_MUSIC}/AudioRecorder"
                )
            } ?: put(
                MediaStore.Audio.Media.DATA, "${
                    Environment.getExternalStorageDirectory()
                        .absolutePath
                }/Music/AudioRecorder/$fileName"
            )
        }

        return try {
            context.contentResolver.insert(audioCollection, contentValues)?.also { uri ->
                val recordingFilePath = getFilePathFromUri(uri)
                recorder.start(File(recordingFilePath))
            } ?: throw IOException("Couldn't create MediaStore entry")
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    private fun getFilePathFromUri(uri: Uri): String {
        val projection = arrayOf(MediaStore.Audio.Media.DATA)
        val cursor = context.contentResolver.query(uri, projection, null, null, null)
        val filePath = if (cursor != null && cursor.moveToFirst()) {
            cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
        } else {
            ""
        }
        cursor?.close()
        return filePath
    }

    fun stopRecordingAudio() {
        recorder.stop()
    }


    private fun getCursorData(): MutableList<Audio> {
        val audioList = mutableListOf<Audio>()

        mCursor = context.contentResolver.query(
            context.filesDir.toUri(),
            projection,
            selectionClause,
            selectionArg,
            sortOrder
        )


        mCursor?.use { cursor ->
            val idColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns._ID)
            val displayNameColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DISPLAY_NAME)
            val dateColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DATE_ADDED)
            val durationColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DURATION)
            val titleColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.TITLE)

            cursor.apply {
                if (count == 0) {
                    Log.e("Cursor", "getCursorData: Cursor is Empty")
                } else {
                    while (cursor.moveToNext()) {
                        val displayName = getString(displayNameColumn)
                        val id = getLong(idColumn)
                        val date = getLong(dateColumn)
                        val duration = getLong(durationColumn)
                        val title = getString(titleColumn)
                        val uri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            id
                        )

                        audioList += Audio(
                            uri = uri,
                            displayName = displayName,
                            id = id,
                            date = date,
                            duration = duration,
                            title = title
                        )
                    }
                }

            }
        }
        return audioList
    }
}