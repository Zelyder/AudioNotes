package com.zelyder.audionotes.ui.audio

import android.support.v4.media.MediaBrowserCompat
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zelyder.audionotes.data.model.Audio
import com.zelyder.audionotes.media.constants.AppConst
import com.zelyder.audionotes.media.exoplayer.MediaPlayerServiceConnection
import com.zelyder.audionotes.media.exoplayer.currentPosition
import com.zelyder.audionotes.media.exoplayer.isPlaying
import com.zelyder.audionotes.media.service.MediaPlayerService
import com.zelyder.audionotes.repository.AudioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AudioViewModel @Inject constructor(
    private val repository: AudioRepository,
    serviceConnection: MediaPlayerServiceConnection
) : ViewModel() {
    var audioList = mutableStateListOf<Audio>()
    var currentPlaybackPosition = mutableStateOf(0L)
    var currentAudioProgress = mutableStateOf(0f)

    lateinit var rootMediaId: String

    val currentPlayingAudio = serviceConnection.currentPlayingAudio
    private var updatePosition = true
    private val isConnected = serviceConnection.isConnected
    private val playbackState = serviceConnection.playbackState
    val isAudioPlaying: Boolean
        get() = playbackState.value?.isPlaying == true

    private val subscriptionCallback = object : MediaBrowserCompat.SubscriptionCallback() {
        override fun onChildrenLoaded(
            parentId: String,
            children: MutableList<MediaBrowserCompat.MediaItem>
        ) {
            super.onChildrenLoaded(parentId, children)
        }
    }

    private val serviceConnection = serviceConnection.also {
        updatePlayback()
    }

    val currentDuration: Long
        get() = MediaPlayerService.currentDuration

    init {
        viewModelScope.launch {
            audioList += getAndFormatAudioData()
            isConnected.collect {
                if (it) {
                    rootMediaId = serviceConnection.rootMediaId
                    serviceConnection.playbackState.value?.apply {
                        currentPlaybackPosition.value = position
                    }
                    serviceConnection.subscribe(rootMediaId, subscriptionCallback)
                }
            }
        }
    }

    private suspend fun getAndFormatAudioData(): List<Audio> {
        return repository.getAudioData().map {
            val displayName = it.displayName.substringBefore(".")
            it.copy(
                displayName = displayName
            )
        }
    }

    fun startRecordingAudio(fileName: String) {
        viewModelScope.launch {
            repository.startRecordingAudio(fileName)
        }
    }

    fun stopRecordingAudio() {
        viewModelScope.launch {
            repository.stopRecordingAudio()
        }
    }

    fun playAudio(currentAudio: Audio) {
        serviceConnection.playAudio(audioList)
        if (currentAudio.id == currentPlayingAudio.value?.id) {
            if (isAudioPlaying) {
                serviceConnection.transportControl.pause()
            } else {
                serviceConnection.transportControl.play()
            }
        } else {
            serviceConnection.transportControl.playFromMediaId(
                currentAudio.id.toString(),
                null
            )
        }
    }

    fun stopPlayback() {
        serviceConnection.transportControl.stop()
    }

    fun fastForward() {
        serviceConnection.fastForward()
    }

    fun rewind() {
        serviceConnection.rewind()
    }

    fun skipToNext() {
        serviceConnection.skipToNext()
    }

    fun seekTo(value: Float) {
        serviceConnection.transportControl.seekTo(
            (currentDuration * value / 100f).toLong() // In percentages
        )
    }

    private fun updatePlayback() {
        viewModelScope.launch {
            val position = playbackState.value?.currentPosition ?: 0

            if (currentPlaybackPosition.value != position) {
                currentPlaybackPosition.value = position
            }

            if (currentDuration > 0) {
                currentAudioProgress.value = (
                        currentPlaybackPosition.value.toFloat() / currentDuration.toFloat() * 100f
                        )
            }

            delay(AppConst.PLAYBACK_UPDATE_INTERVAL)
            if (updatePosition) {
                updatePlayback()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        serviceConnection.unsubscribe(
            AppConst.MEDIA_ROOT_ID,
            object : MediaBrowserCompat.SubscriptionCallback() {}
        )
        updatePosition = false
    }

}