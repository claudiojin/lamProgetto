package com.example.progetto.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.progetto.R
import com.example.progetto.data.dao.TripDao
import com.example.progetto.data.entity.Trip
import com.example.progetto.data.entity.TripType
import com.example.progetto.ui.components.DateRangePicker
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TripFormScreen(
    tripDao: TripDao,
    initialTrip: Trip? = null,
    onSaved: (Long) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    var destination by remember { mutableStateOf(TextFieldValue(initialTrip?.destination ?: "")) }
    var notes by remember { mutableStateOf(TextFieldValue(initialTrip?.notes ?: "")) }
    var type by remember { mutableStateOf(initialTrip?.type ?: TripType.LOCAL) }
    var startDate by remember { mutableStateOf(initialTrip?.startDate ?: "") }
    var endDate by remember { mutableStateOf(initialTrip?.endDate ?: "") }
    var distanceText by remember { mutableStateOf((initialTrip?.distance ?: 0.0).let { if (it == 0.0) "" else it.toString() }) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (initialTrip == null) stringResource(R.string.add_new_trip) else stringResource(R.string.edit_trip)) },
                navigationIcon = {
                    IconButton(onClick = onCancel) { Icon(Icons.Default.ArrowBack, contentDescription = null) }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = destination,
                onValueChange = { destination = it },
                label = { Text(stringResource(R.string.destination)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text(text = stringResource(R.string.trip_type_label), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TripType.values().forEach { t ->
                    FilterChip(
                        selected = (type == t),
                        onClick = { type = t },
                        label = { Text(t.displayName) }
                    )
                }
            }

            DateRangePicker(
                startDate = startDate,
                endDate = endDate,
                onChange = { s, e ->
                    startDate = s
                    endDate = e
                }
            )

            OutlinedTextField(
                value = distanceText,
                onValueChange = { distanceText = it },
                label = { Text(stringResource(R.string.distance_km)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.notes_optional)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp),
                maxLines = 6
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    if (destination.text.isBlank() || startDate.isBlank() || endDate.isBlank()) return@Button
                    scope.launch {
                        if (initialTrip == null) {
                            val id = tripDao.insert(
                                Trip(
                                    destination = destination.text.trim(),
                                    startDate = startDate,
                                    endDate = endDate,
                                    type = type,
                                    notes = notes.text.trim(),
                                    distance = distanceText.toDoubleOrNull() ?: 0.0
                                )
                            )
                            onSaved(id)
                        } else {
                            val updated = initialTrip.copy(
                                destination = destination.text.trim(),
                                startDate = startDate,
                                endDate = endDate,
                                type = type,
                                notes = notes.text.trim(),
                                distance = distanceText.toDoubleOrNull() ?: initialTrip.distance
                            )
                            tripDao.update(updated)
                            onSaved(updated.id)
                        }
                    }
                },
                enabled = destination.text.isNotBlank() && startDate.isNotBlank() && endDate.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.save)) }
        }
    }
}
