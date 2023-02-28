package com.zelyder.audionotes.data.model

import android.net.Uri

data class Audio(
    val uri: Uri,
    val displayName: String,
    val id: Long,
    val data: String,
    val duration: Int,
    val title: String
)
