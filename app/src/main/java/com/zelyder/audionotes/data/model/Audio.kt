package com.zelyder.audionotes.data.model

import android.net.Uri

data class Audio(
    val uri: Uri,
    val displayName: String,
    val id: Long,
    val date: Long,
    val duration: Long,
    val title: String
)
