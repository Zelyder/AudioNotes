package com.zelyder.audionotes.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zelyder.audionotes.R

@Composable
fun CommonDialog(
    title: String?,
    state: MutableState<Boolean>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmText: String = stringResource(id = R.string.dialog_confirm),
    cancelText: String = stringResource(id = R.string.dialog_cancel),
    content: @Composable (() -> Unit)? = null
) {
    if (state.value){
        AlertDialog(
            onDismissRequest = {
                onDismiss()
                state.value = false
            },
            title = title?.let {
                {
                    Column(
                        Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = title)
                        Divider(modifier = Modifier.padding(bottom = 8.dp))
                    }
                }
            },
            text = content,
            dismissButton = {
                Button(onClick = {
                    onDismiss()
                    state.value = false
                }) {
                    Text(text = cancelText)
                }
            },
            confirmButton = {
                Button(onClick = {
                    onConfirm()
                    state.value = false
                }) {
                    Text(text = confirmText)
                }
            }, modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}

@Composable
fun InputDialog(
    title: String?,
    state: MutableState<Boolean>,
    value: MutableState<String>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    CommonDialog(
        title = title,
        state = state,
        onDismiss = onDismiss,
        onConfirm = onConfirm
    ) {
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextField(value = value.value, onValueChange = { value.value = it })
        }
    }

}

@Composable
fun ConfirmDialog(
    title: String?,
    state: MutableState<Boolean>,
    text: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit = {}
) {
    CommonDialog(
        title = title,
        state = state,
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        confirmText = stringResource(id = R.string.yes),
        cancelText = stringResource(id = R.string.no)
    ) {
        Text(text = text)
    }

}