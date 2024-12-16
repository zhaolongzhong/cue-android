package com.example.cue.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.cue.ui.theme.AppTheme

@Composable
fun MessageInputView(
    message: String,
    onMessageChange: (String) -> Unit,
    isEnabled: Boolean,
    onSend: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val isMessageValid = message.trim().length >= 3

    Column(modifier = modifier) {
        Divider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            TextField(
                value = message,
                onValueChange = onMessageChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                placeholder = { Text("Type a message...") },
                enabled = isEnabled,
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.LightGray.copy(alpha = 0.1f),
                    focusedContainerColor = Color.LightGray.copy(alpha = 0.2f)
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (isMessageValid) {
                            onSend()
                            focusManager.clearFocus()
                        }
                    }
                ),
                minLines = 1,
                maxLines = 5
            )

            Spacer(modifier = Modifier.width(8.dp))

            FilledIconButton(
                onClick = onSend,
                enabled = isMessageValid,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = AppTheme.Icons.Send,
                    contentDescription = "Send"
                )
            }
        }
    }
}