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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zelyder.audionotes.media.service.HomeScreen
import com.zelyder.audionotes.ui.audio.AudioViewModel
import com.zelyder.audionotes.ui.components.InputDialog
import com.zelyder.audionotes.ui.theme.AudioNotesTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var readPermissionGranted = false
    private var writePermissionGranted = false
    private var recordAudioPermissionGranted = false
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
        }
        updateOrRequestPermission()
        setContent {
            AudioNotesTheme {


                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    if (readPermissionGranted && writePermissionGranted && recordAudioPermissionGranted) {
                        val audioViewModel = viewModel(
                            modelClass = AudioViewModel::class.java
                        )


                        InputDialog(
                            title = "Название файла ",
                            state = audioViewModel.showDialog,
                            value = audioViewModel.currentAudioName,
                            onDismiss = audioViewModel::onDialogDismiss,
                            onConfirm = audioViewModel::onDialogConfirm,
                        )

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
                            currentPlayingAudio = audioViewModel
                                .currentPlayingAudio.value,
                            onStart = {
                                audioViewModel.playAudio(it)
                            },
                            onStartRecorder = {
                                audioViewModel.onOpenDialogClicked()
                            },
                            onStopRecorder = {
                                audioViewModel.stopRecordingAudio()
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
    }
}

@Composable
fun SimpleAlertDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (show) {
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = onConfirm)
                { Text(text = "OK") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss)
                { Text(text = "Cancel") }
            },
            title = { Text(text = "Saving audio") },
            text = { Text(text = "Should I continue with the requested action?") }
        )
    }
}
