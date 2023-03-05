package com.zelyder.audionotes

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zelyder.audionotes.ui.audio.HomeScreen
import com.zelyder.audionotes.ui.audio.AudioViewModel
import com.zelyder.audionotes.ui.components.ConfirmDialog
import com.zelyder.audionotes.ui.components.InputDialog
import com.zelyder.audionotes.ui.theme.AudioNotesTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var readPermissionGranted = false
    private var writePermissionGranted = false
    private var recordAudioPermissionGranted = false
    private val allPermissionGranted
        get() = readPermissionGranted && writePermissionGranted && recordAudioPermissionGranted
    private val allPermissionState = mutableStateOf(allPermissionGranted)
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionLauncher = registerForActivityResult(
            ActivityResultContracts
                .RequestMultiplePermissions()
        ) { permissions ->
            readPermissionGranted =
                permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: readPermissionGranted
            writePermissionGranted =
                permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] ?: writePermissionGranted
            recordAudioPermissionGranted =
                permissions[Manifest.permission.RECORD_AUDIO] ?: recordAudioPermissionGranted
            allPermissionState.value = allPermissionGranted
        }
        updateOrRequestPermission()
        setContent {
            AudioNotesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    if (allPermissionState.value) {
                        val audioViewModel = viewModel(
                            modelClass = AudioViewModel::class.java
                        )

                        InputDialog(
                            title = "Название файла ",
                            state = audioViewModel.showFileNameDialog,
                            value = audioViewModel.currentAudioName,
                            onDismiss = audioViewModel::onDialogFileNameDismiss,
                            onConfirm = audioViewModel::onDialogFileNameConfirm,
                        )
                        ConfirmDialog(
                            title = stringResource(id = R.string.dialog_confirmation_title),
                            state = audioViewModel.showDeleteConfirmationDialog,
                            text = stringResource(
                                id = R.string.dialog_confirmation_delete_text,
                                audioViewModel.audioToDelete.value?.displayName ?: 
                                stringResource(id = R.string.unknown)
                            ),
                            onConfirm = audioViewModel::onDialogDeleteConfirmationConfirm)

                        val audioList = audioViewModel.audioList

                        HomeScreen(
                            audioList = audioList,
                            progress = audioViewModel.currentAudioProgress.value,
                            currentPoint = audioViewModel.currentPlaybackPosition.value,
                            onProgressChange = {
                                audioViewModel.seekTo(it)
                            },
                            isAudioPlaying = audioViewModel.isAudioPlaying,
                            isStartRecord = audioViewModel.isRecordingAudio.value,
                            seconds = audioViewModel.seconds.value,
                            minutes = audioViewModel.minutes.value,
                            hours = audioViewModel.hours.value,
                            currentPlayingAudio = audioViewModel
                                .currentPlayingAudio.value,
                            onStart = {
                                audioViewModel.playAudio(it)
                            },
                            onStartRecorder = {
                                audioViewModel.openDialogFileNameConfirm()
                            },
                            onStopRecorder = {
                                audioViewModel.stopRecordingAudio()
                            },
                            onDeleteAudio = {audio ->
                                audioViewModel.deleteAudio(audio)
                            }
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Grant permission first to use this app",
                                style = MaterialTheme.typography.h6
                            )
                        }
                    }

                }
            }
        }
    }


    private fun updateOrRequestPermission() {
        val hasReadPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        val hasWritePermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        val hasRecordAudioPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        val minSdk29 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

        readPermissionGranted = hasReadPermission
        writePermissionGranted = hasWritePermission || minSdk29
        recordAudioPermissionGranted = hasRecordAudioPermission

        val permissionsToRequest = mutableListOf<String>()
        if (!writePermissionGranted) {
            permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (!readPermissionGranted) {
            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (!recordAudioPermissionGranted) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
        allPermissionState.value = allPermissionGranted
    }
}
