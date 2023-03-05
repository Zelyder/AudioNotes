package com.zelyder.audionotes.ui.audio

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    seconds: String,
    minutes: String,
    hours: String,
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
                            contentDescription = stringResource(id = R.string.recording_start),
                            tint = Color.Red
                        )
                    else Icon(
                        painterResource(id = R.drawable.ic_baseline_mic_none_24),
                        contentDescription = stringResource(id = R.string.recording_stop)
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
                if (isStartRecord) {
                    Spacer(modifier = Modifier.height(34.dp))
                    RecordingTimer(
                        seconds = seconds,
                        minutes = minutes,
                        hours = hours
                    )
                    Spacer(modifier = Modifier.height(34.dp))
                } else {
                    Text(
                        text = stringResource(id = R.string.home_screen_title),
                        style = MaterialTheme.typography.h4,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                }


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

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun RecordingTimer(
    modifier: Modifier = Modifier,
    seconds: String,
    minutes: String,
    hours: String,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        val numberTransitionSpec: AnimatedContentScope<String>.() -> ContentTransform = {
            slideInVertically(initialOffsetY = { it }) +
                    fadeIn() with slideOutVertically(targetOffsetY = { -it }) +
                    fadeOut() using SizeTransform(false)
        }
        CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.h3) {
            AnimatedContent(targetState = hours, transitionSpec = numberTransitionSpec) {
                Text(text = it)
            }
            Text(text = ":")
            AnimatedContent(targetState = minutes, transitionSpec = numberTransitionSpec) {
                Text(text = it)
            }
            Text(text = ":")
            AnimatedContent(targetState = seconds, transitionSpec = numberTransitionSpec) {
                Text(text = it)
            }
        }


    }
}


private fun formatDate(
    timestamp: Long,
    datePattern: String,
    todayPattern: String,
    todayText: String
): String {
    val dateFormat = SimpleDateFormat(datePattern, Locale.getDefault())
    val todayFormat = SimpleDateFormat(todayPattern, Locale.getDefault())
    dateFormat.timeZone = TimeZone.getTimeZone("UTC")
    val date = Date(timestamp)
    val today = Calendar.getInstance().apply { time = Date() }
    val timestampDay = Calendar.getInstance().apply { time = date }
    return if (today.get(Calendar.DAY_OF_YEAR) == timestampDay.get(Calendar.DAY_OF_YEAR)) {
        "$todayText ${todayFormat.format(date)}"
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
                        text = formatDate(
                            timestamp = audio.date,
                            datePattern = stringResource(id = R.string.full_date_format),
                            todayText = stringResource(id = R.string.today_date_text),
                            todayPattern = stringResource(id = R.string.today_date_format)
                        ),
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
                            style = MaterialTheme.typography.subtitle1,
                            fontSize = 14.sp
                        )
                        Text(
                            text = stringResource(id = R.string.time_separator),
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                    Text(
                        text = timeStampToDuration(audio.duration), color = Color.Gray,
                        fontSize = 14.sp
                    )
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
                    value = progress,
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
