package com.zelyder.audionotes

import android.Manifest
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.zelyder.audionotes.media.playback.AndroidAudioPlayer
import com.zelyder.audionotes.media.record.AndroidAudioRecorder
import com.zelyder.audionotes.ui.audio.AudioViewModel
import com.zelyder.audionotes.ui.theme.AudioNotesTheme
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import kotlin.math.floor

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val recorder by lazy {
        AndroidAudioRecorder(applicationContext)
    }

    private val player by lazy {
        AndroidAudioPlayer(applicationContext)
    }

    private var audioFile: File? = null

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AudioNotesTheme {
                val multiplePermissionsState = rememberMultiplePermissionsState(
                    permissions = listOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.RECORD_AUDIO
                    )
                )
                multiplePermissionsState.permissions.forEach { perm ->
                    when (perm.permission) {
                        Manifest.permission.WRITE_EXTERNAL_STORAGE -> {
                            GetPermission(perm, "Write Permission")
                        }
                        Manifest.permission.READ_EXTERNAL_STORAGE -> {
                            GetPermission(perm, "Read Permission")
                        }
                        Manifest.permission.RECORD_AUDIO -> {
                            GetPermission(perm, "recode audio Permission")
                        }
                    }
                }

                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    if (multiplePermissionsState.allPermissionsGranted) {
                        val audioViewModel = viewModel(
                            modelClass = AudioViewModel::class.java
                        )
                        val audioList = audioViewModel.audioList.map { it.asRecording() }
                        RecordingsScreen(audioList)
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Grant permission first to use this app",
                                style = MaterialTheme.typography.h3
                            )
                        }
                    }

                }
            }
        }
    }

    @ExperimentalPermissionsApi
    @Composable
    private fun GetPermission(
        perm: PermissionState,
        permissionText: String
    ) {
        when (perm.status) {
            is PermissionStatus.Granted -> {
                Text("$permissionText permission Granted")
            }
            is PermissionStatus.Denied -> {
                Column {
                    val textToShow =
                        if ((perm.status as PermissionStatus.Denied).shouldShowRationale) {
                            "The $permissionText is important for this app. Please grant the permission."
                        } else {
                            "The $permissionText required for this feature to be available. " +
                                    "Please grant the permission"
                        }
                    Text(textToShow)
                    Button(onClick = { perm.launchPermissionRequest() }) {
                        Text("Request $permissionText")
                    }
                }
            }
        }
    }

    @Composable
    fun SimpleRecording() {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = {
                File(cacheDir, "audio.mp3").also {
                    recorder.start(it)
                    audioFile = it
                }
            }) {
                Text(text = "Start recording")
            }
            Button(onClick = {
                recorder.stop()
            }) {
                Text(text = "Stop recording")
            }
            Button(onClick = {
                player.playFile(audioFile ?: return@Button)
            }) {
                Text(text = "Play")
            }
            Button(onClick = {
                player.stop()
            }) {
                Text(text = "Stop playing")
            }
        }
    }

    @Composable
    fun AudioItem(audio: Recording) {
        Card(elevation = 0.dp) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(2.0f),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = audio.title,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.body1,
                            overflow = TextOverflow.Clip,
                            maxLines = 1
                        )
                        Text(
                            text = audio.date,
                            color = Color.Gray,
                            overflow = TextOverflow.Clip,
                            maxLines = 1
                        )
                    }
                    Row(
                        modifier = Modifier.weight(1.0f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (audio.currentPoint > 0) {
                            Text(
                                text = timeStampToDuration(audio.currentPoint),
                                style = MaterialTheme.typography.subtitle1
                            )
                            Text(text = "/", color = Color.Gray)
                        }
                        Text(text = timeStampToDuration(audio.duration), color = Color.Gray)
                        Spacer(modifier = Modifier.width(8.dp))
                        Image(
                            modifier = Modifier.size(32.dp),
                            painter = painterResource(
                                id = if (audio.isPlaying) R.drawable.ic_baseline_pause_circle_filled_24
                                else R.drawable.ic_baseline_play_circle_filled_24
                            ),
                            contentDescription = null
                        )
                    }

                }
                if (audio.isPlaying) {
                    Slider(
                        value = ((audio.currentPoint * 100) / audio.duration) / 100.0f,
                        onValueChange = { }
                    )
                }
            }
        }

    }

    @Composable
    fun RecordingsScreen(recordingsList: List<Recording>) {
        val isStartRecord = remember { mutableStateOf(false) }
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    content = {
                        if (isStartRecord.value)
                            Icon(
                                painterResource(id = R.drawable.ic_baseline_record_24),
                                contentDescription = "Начать запись"
                            )
                        else Icon(
                            painterResource(id = R.drawable.ic_baseline_mic_none_24),
                            contentDescription = "Остановить запись"
                        )
                    },
                    onClick = { isStartRecord.value = !isStartRecord.value }
                )
            },
            floatingActionButtonPosition = FabPosition.Center
        ) { contentPadding ->
            Surface(
                modifier = Modifier
                    .padding(contentPadding)
                    .fillMaxSize(),
                color = MaterialTheme.colors.background
            ) {
                Column(
                    modifier = Modifier.padding(
                        top = 16.dp,
                        start = 16.dp,
                        end = 16.dp
                    )
                ) {
                    Text(
                        text = "Ваши записи",
                        style = MaterialTheme.typography.h4,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(top = 8.dp)
                    ) {
                        items(recordingsList) { audio ->
                            AudioItem(audio = audio)
                        }
                    }
                }

            }
        }

    }

    private fun timeStampToDuration(position: Long): String {
        val totalSeconds = floor(position / 1E3).toInt()
        val minutes = totalSeconds / 60
        val remainingSeconds = totalSeconds - (minutes * 60)

        return if (position < 0) "--:--"
        else "%d:%02d".format(minutes, remainingSeconds)


    }

    data class Recording(
        val title: String,
        val date: String,
        val duration: Long,
        var currentPoint: Long = 0,
        var isPlaying: Boolean = false
    )

    val recordingsList = listOf(
        Recording(
            title = "Поход к адвокату",
            date = "Сегодня в 12:51",
            duration = 332,
            currentPoint = 138,
            isPlaying = true
        ),
        Recording(
            title = "Разговор с Иваном",
            date = "14.02.2022 в 15:32",
            duration = 151,
        ),
        Recording(
            title = "Куртой трек - надо найти!",
            date = "12.02.2022 в 13:11",
            duration = 31,
        ),
        Recording(
            title = "Куртой трек - надо найти! Куртой ",
            date = "12.02.2022 в 13:11 12.",
            duration = 31,
        ),
    )

    @Preview(showBackground = true)
    @Composable
    fun DefaultPreview() {
        AudioNotesTheme {
            AudioItem(
                Recording(
                    title = "Поход к адвокату",
                    date = "Сегодня в 12:51",
                    duration = 332,
                    currentPoint = 138,
                    isPlaying = false
                )
            )
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun DefaultPreview2() {
        AudioNotesTheme {
            AudioItem(
                Recording(
                    title = "Поход к адвокату",
                    date = "Сегодня в 12:51",
                    duration = 332,
                    currentPoint = 138,
                    isPlaying = true
                )
            )
        }
    }


    @Preview(showBackground = true, showSystemUi = true, uiMode = UI_MODE_NIGHT_YES)
    @Composable
    fun RecordingsScreenDarkPreview() {
        AudioNotesTheme {
            RecordingsScreen(recordingsList)
        }
    }

    @Preview(showBackground = true, showSystemUi = true)
    @Composable
    fun RecordingsScreenPreview() {
        AudioNotesTheme {
            RecordingsScreen(recordingsList)
        }
    }
}
