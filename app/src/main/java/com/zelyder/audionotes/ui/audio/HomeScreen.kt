package com.zelyder.audionotes.media.service

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zelyder.audionotes.R
import com.zelyder.audionotes.data.model.Audio
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.floor

@Composable
fun HomeScreen(
    audioList: List<Audio>,
    progress: Float,
    currentPoint: Long,
    onProgressChange: (Float) -> Unit,
    isAudioPlaying: Boolean,
    isStartRecord: Boolean,
    currentPlayingAudio: Audio?,
    onStart: (Audio) -> Unit,
    onStartRecorder: () -> Unit,
    onStopRecorder: () -> Unit,
    onDeleteAudio: (Audio) -> Unit
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                content = {
                    if (isStartRecord)
                        Icon(
                            painterResource(id = R.drawable.ic_baseline_record_24),
                            contentDescription = "Начать запись"
                        )
                    else Icon(
                        painterResource(id = R.drawable.ic_baseline_mic_none_24),
                        contentDescription = "Остановить запись"
                    )
                },
                onClick = {
                    if (isStartRecord) {
                        onStopRecorder()
                    } else {
                        onStartRecorder()
                    }
                }
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
                    items(audioList) { audio ->
                        AudioItem(
                            audio = audio,
                            isPlaying = isAudioPlaying,
                            isCurrentAudio = audio == currentPlayingAudio,
                            onProgressChange = onProgressChange,
                            onItemClick = {
                                onStart.invoke(audio)
                            },
                            onLongItemClick = {
                                onDeleteAudio.invoke(audio)
                            },
                            progress = if (audio == currentPlayingAudio) progress else 0f,
                            currentPoint = if (audio == currentPlayingAudio) currentPoint else 0,
                        )
                    }
                }
            }

        }
    }

}


private fun formatDate(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("MM.dd.yyyy в HH:mm", Locale.getDefault())
    val todayFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    dateFormat.timeZone = TimeZone.getTimeZone("UTC")
    val date = Date(timestamp)
    val today = Calendar.getInstance().apply { time = Date() }
    val timestampDay = Calendar.getInstance().apply { time = date }
    return if (today.get(Calendar.DAY_OF_YEAR) == timestampDay.get(Calendar.DAY_OF_YEAR)) {
        "Сегодня в ${todayFormat.format(date)}"
    } else {
        dateFormat.format(date)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AudioItem(
    audio: Audio,
    isCurrentAudio: Boolean,
    isPlaying: Boolean,
    onProgressChange: (Float) -> Unit,
    onItemClick: (id: Long) -> Unit,
    onLongItemClick: (id: Long) -> Unit,
    progress: Float = 0.0f,
    currentPoint: Long = 0L
) {
    Card(elevation = 0.dp) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { onItemClick.invoke(audio.id) },
                    onLongClick = { onLongItemClick.invoke(audio.id) }
                )
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
                        text = audio.displayName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.body1,
                        overflow = TextOverflow.Clip,
                        maxLines = 1
                    )
                    Text(
                        text = formatDate(audio.date),
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
                    if (progress > 0) {
                        Text(
                            text = timeStampToDuration(currentPoint),
                            style = MaterialTheme.typography.subtitle1
                        )
                        Text(text = "/", color = Color.Gray)
                    }
                    Text(text = timeStampToDuration(audio.duration), color = Color.Gray)
                    Spacer(modifier = Modifier.width(8.dp))
                    Image(
                        modifier = Modifier
                            .size(32.dp)
                            .clickable { onItemClick.invoke(audio.id) },
                        painter = painterResource(
                            id = if (isCurrentAudio && isPlaying) R.drawable.ic_baseline_pause_circle_filled_24
                            else R.drawable.ic_baseline_play_circle_filled_24
                        ),
                        contentDescription = null
                    )
                }

            }
            if (isCurrentAudio) {
                Slider(
                    value = progress, //((audio.currentPoint * 100) / audio.duration)
                    onValueChange = { onProgressChange.invoke(it) },
                    valueRange = 0f..100f
                )
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

//val audioList = listOf(
//    Audio(
//        id = 1,
//        uri = Uri.EMPTY,
//        displayName = "Поход к адвокату",
//        title = "Поход к адвокату",
//        date = 1677563425,
//        duration = 332000,
//    ),
//    Audio(
//        id = 2,
//        uri = Uri.EMPTY,
//        displayName = "Разговор с Иваном",
//        title = "Разговор с Иваном",
//        date = 1646043085,
//        duration = 151000,
//    ),
//    Audio(
//        id = 3,
//        uri = Uri.EMPTY,
//        displayName = "Куртой трек - надо найти!",
//        title = "Куртой трек - надо найти!",
//        date = 1677563425,
//        duration = 232000,
//    )
//)
//
//@Preview(showBackground = true)
//@Composable
//fun DefaultPreview() {
//    AudioNotesTheme {
//        AudioItem(
//            audio = audioList.first(),
//            isPlaying = false,
//            isCurrentAudio = true,
//            onProgressChange = {},
//            onItemClick = {}
//        )
//    }
//}
//
//@Preview(showBackground = true)
//@Composable
//fun DefaultPreview2() {
//    AudioNotesTheme {
//        AudioItem(
//            audio = audioList.first(),
//            isPlaying = true,
//            isCurrentAudio = true,
//            onProgressChange = {},
//            onItemClick = {},
//            progress = 50f,
//            currentPoint = audioList.first().duration / 2
//        )
//    }
//}

//
//@Preview(showBackground = true, showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
//@Composable
//fun RecordingsScreenDarkPreview() {
//    AudioNotesTheme {
//        HomeScreen(
//            audioList = audioList,
//            progress = 50f,
//            currentPoint = audioList.first().duration / 2,
//            onProgressChange = {},
//            isAudioPlaying = true,
//            currentPlayingAudio = audioList.first(),
//            onStart = {},
//            onNext = {},
//            recorder = null
//        )
//    }
//}
//
//@Preview(showBackground = true, showSystemUi = true)
//@Composable
//fun RecordingsScreenPreview() {
//    AudioNotesTheme {
//        HomeScreen(
//            audioList = audioList,
//            progress = 50f,
//            currentPoint = audioList.first().duration / 2,
//            onProgressChange = {},
//            isAudioPlaying = true,
//            currentPlayingAudio = audioList.first(),
//            onStart = {},
//            onNext = {},
//        )
//    }
//}
