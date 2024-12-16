package com.example.cue.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputDialog(
    title: String,
    message: String? = null,
    initialValue: String = "",
    keyboardType: Int = android.text.InputType.TYPE_CLASS_TEXT,
    validator: ((String) -> Boolean)? = null,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(TextFieldValue(initialValue)) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                if (message != null) {
                    Text(message)
                }
                TextField(
                    value = text,
                    onValueChange = { 
                        text = it
                        isError = validator?.let { validate -> !validate(it.text) } ?: false
                    },
                    isError = isError,
                    keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(
                        keyboardType = when (keyboardType) {
                            android.text.InputType.TYPE_CLASS_NUMBER -> KeyboardType.Number
                            else -> KeyboardType.Text
                        }
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text.text) },
                enabled = !isError && text.text.isNotBlank()
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}