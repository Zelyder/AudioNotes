package com.zelyder.audionotes

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.vk.api.sdk.VK
import com.vk.api.sdk.auth.VKAuthenticationResult
import com.vk.api.sdk.auth.VKScope
import com.zelyder.audionotes.ui.audio.AudioViewModel
import com.zelyder.audionotes.ui.audio.HomeScreen
import com.zelyder.audionotes.ui.components.ConfirmDialog
import com.zelyder.audionotes.ui.components.InputDialog
import com.zelyder.audionotes.ui.components.PermissionDialog
import com.zelyder.audionotes.ui.components.RecordAudioPermissionTextProvider
import com.zelyder.audionotes.ui.theme.AudioNotesTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        val TAG: String = this::class.java.simpleName
    }

    private var recordAudioPermissionGranted = mutableStateOf(false)
    lateinit var authLauncher: ActivityResultLauncher<Collection<VKScope>>

    override fun onStart() {
        super.onStart()
        vkAuth()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updatePermission()
        setContent {
            AudioNotesTheme {
                MainContent()
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    val audioViewModel = viewModel(
                        modelClass = AudioViewModel::class.java
                    )
                    val dialogQueue = audioViewModel.visiblePermissionDialogQueue
                    val multiplePermissionResultLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestMultiplePermissions(),
                        onResult = { perms ->
                            perms.keys.forEach { permission ->
                                if (permission == Manifest.permission.RECORD_AUDIO && perms[permission] == true) {
                                    recordAudioPermissionGranted.value = true
                                }
                                audioViewModel.onPermissionResult(
                                    permission = permission,
                                    isGranted = perms[permission] == true
                                )
                            }
                        }
                    )

                    if (recordAudioPermissionGranted.value) {
                        InputDialog(
                            title = stringResource(id = R.string.dialog_input_filename_title),
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
                                audioViewModel.audioToDelete.value?.displayName
                                    ?: stringResource(id = R.string.unknown)
                            ),
                            onConfirm = audioViewModel::onDialogDeleteConfirmationConfirm
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
                            onDeleteAudio = { audio ->
                                audioViewModel.deleteAudio(audio)
                            }
                        )
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(id = R.string.no_permissions_text),
                                style = MaterialTheme.typography.h6,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = {
                                multiplePermissionResultLauncher.launch(
                                    arrayOf(Manifest.permission.RECORD_AUDIO)
                                )
                            }) {
                                Text(text = stringResource(id = R.string.grant_permission))
                            }
                            dialogQueue.reversed().forEach { permission ->
                                PermissionDialog(
                                    permission = when (permission) {
                                        Manifest.permission.RECORD_AUDIO -> {
                                            RecordAudioPermissionTextProvider()
                                        }
                                        else -> return@forEach
                                    },
                                    isPermanentlyDeclined = !shouldShowRequestPermissionRationale(
                                        permission
                                    ),
                                    onConfirm = {
                                        audioViewModel.dismissPermissionDialog()
                                        multiplePermissionResultLauncher.launch(
                                            arrayOf(permission)
                                        )
                                    },
                                    onDismiss = audioViewModel::dismissPermissionDialog,
                                    onGoToAppSettingsClick = ::openAppSettings
                                )
                            }

                        }
                    }
                }
            }
        }
    }

    fun vkAuth() {
        authLauncher = VK.login(this) { result : VKAuthenticationResult ->
            when (result) {
                is VKAuthenticationResult.Success -> {
                    Log.d(TAG, "VKAuthenticationResult.Success")
                }
                is VKAuthenticationResult.Failed -> {
                    // User didn't pass authorization
                    Log.d(TAG, "VKAuthenticationResult.Failed")
                }
            }
        }

    }

    @Composable
    fun MainContent() {
        val systemUiController = rememberSystemUiController()
        systemUiController.setStatusBarColor(MaterialTheme.colors.background)
    }

    private fun updatePermission() {
        val hasRecordAudioPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        recordAudioPermissionGranted.value = hasRecordAudioPermission
    }
}

fun Activity.openAppSettings() {
    Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null)
    ).also {
        startActivity(it)
    }
}
