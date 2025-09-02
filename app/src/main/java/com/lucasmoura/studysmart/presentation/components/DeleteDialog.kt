package com.lucasmoura.studysmart.presentation.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.lucasmoura.studysmart.ui.theme.StudySmartTheme

@Composable
fun DeleteDialog(
    modifier: Modifier = Modifier,
    isOpen: Boolean,
    title: String,
    bodyText: String,
    onDismissRequest : () -> Unit,
    onConfirmButton : () -> Unit,
) {


    if(isOpen) {
        AlertDialog(
            title = { Text(text = title) },
            text = { Text(text = bodyText)},
            onDismissRequest = onDismissRequest,
            dismissButton = {
                TextButton(
                    onClick = onDismissRequest
                ) {
                    Text(text = "Cancel")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = onConfirmButton,
                ) {
                    Text(text = "Delete")
                }
            },
            )
    }

}

@Preview
@Composable
private fun AddSubjectDialogPreview() {
    StudySmartTheme {

    }
}