package com.zelyder.audionotes.ui.components

import com.zelyder.audionotes.R

interface PermissionTextProvider {
    fun getDescriptionResourceId(isPermanentlyDeclined: Boolean): Int
}

class RecordAudioPermissionTextProvider : PermissionTextProvider {
    override fun getDescriptionResourceId(isPermanentlyDeclined: Boolean): Int =
        if (isPermanentlyDeclined) {
            R.string.permission_record_audio_permanently_text
        } else {
            R.string.permission_record_audio_text
        }
}