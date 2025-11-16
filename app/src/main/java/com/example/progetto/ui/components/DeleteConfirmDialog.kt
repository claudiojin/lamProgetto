package com.example.progetto.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.example.progetto.data.entity.Trip

@Composable
fun DeleteConfirmDialog(
    trip: Trip,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Conferma") },
        text = { Text("Sicuro di eliminare「${trip.destination}」.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Elimina")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}
