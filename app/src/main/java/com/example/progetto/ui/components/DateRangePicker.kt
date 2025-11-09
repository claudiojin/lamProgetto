package com.example.progetto.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePicker(
    startDate: String,
    endDate: String,
    onChange: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    dateFormat: String = "yyyy-MM-dd",
    autoMirrorEndFromStart: Boolean = true
) {
    var showStart by remember { mutableStateOf(false) }
    var showEnd by remember { mutableStateOf(false) }

    val sdf = remember { SimpleDateFormat(dateFormat, Locale.getDefault()) }
    val error: String? = remember(startDate, endDate) {
        try {
            if (startDate.isBlank() || endDate.isBlank()) return@remember null
            val s = sdf.parse(startDate)!!.time
            val e = sdf.parse(endDate)!!.time
            if (e < s) "结束日期不能早于开始日期" else null
        } catch (e: Exception) { "日期格式无效" }
    }

    Column(modifier = modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { showStart = true }, modifier = Modifier.weight(1f)) {
                Text(if (startDate.isBlank()) "选择开始日期" else startDate)
            }
            OutlinedButton(onClick = { showEnd = true }, modifier = Modifier.weight(1f)) {
                Text(if (endDate.isBlank()) "选择结束日期" else endDate)
            }
        }
        if (error != null) {
            Text(text = error, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
        }
    }

    if (showStart) {
        val state = androidx.compose.material3.rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showStart = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val s = sdf.format(Date(millis))
                        val e = if (autoMirrorEndFromStart && endDate.isBlank()) s else endDate
                        onChange(s, e)
                    }
                    showStart = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showStart = false }) { Text("取消") } }
        ) { DatePicker(state = state) }
    }

    if (showEnd) {
        val state = androidx.compose.material3.rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showEnd = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onChange(startDate, sdf.format(Date(millis)))
                    }
                    showEnd = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showEnd = false }) { Text("取消") } }
        ) { DatePicker(state = state) }
    }
}
