import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.*
import androidx.compose.ui.unit.dp
import com.example.progetto.data.entity.Trip
import com.example.progetto.data.entity.TripType

/**
 * 编辑旅行界面
 *
 * @param trip 要编辑的旅行（如果为null则是新增）
 * @param onSave 保存回调
 * @param onCancel 取消回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTripScreen(
    trip: Trip?,
    onSave: (Trip) -> Unit,
    onCancel: () -> Unit
) {
    // 表单状态（可编辑的字段）
    var destination by remember { mutableStateOf(trip?.destination ?: "") }
    var startDate by remember { mutableStateOf(trip?.startDate ?: "") }
    var endDate by remember { mutableStateOf(trip?.endDate ?: "") }
    var selectedType by remember { mutableStateOf(trip?.type ?: TripType.LOCAL) }
    var notes by remember { mutableStateOf(trip?.notes ?: "") }
    var distance by remember { mutableStateOf(trip?.distance?.toString() ?: "") }

    // 类型选择器展开状态
    var typeExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 标题
        Text(
            text = if (trip == null) "添加新旅行" else "编辑旅行",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 目的地输入框
        OutlinedTextField(
            value = destination,
            onValueChange = { destination = it },
            label = { Text("目的地") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 开始日期输入框
        OutlinedTextField(
            value = startDate,
            onValueChange = { startDate = it },
            label = { Text("开始日期 (格式: 2025-11-01)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 结束日期输入框
        OutlinedTextField(
            value = endDate,
            onValueChange = { endDate = it },
            label = { Text("结束日期 (格式: 2025-11-03)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 旅行类型选择器
        ExposedDropdownMenuBox(
            expanded = typeExpanded,
            onExpandedChange = { typeExpanded = it }
        ) {
            OutlinedTextField(
                value = selectedType.displayName,
                onValueChange = {},
                readOnly = true,
                label = { Text("旅行类型") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )

            ExposedDropdownMenu(
                expanded = typeExpanded,
                onDismissRequest = { typeExpanded = false }
            ) {
                TripType.values().forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.displayName) },
                        onClick = {
                            selectedType = type
                            typeExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 距离输入框
        OutlinedTextField(
            value = distance,
            onValueChange = { distance = it },
            label = { Text("距离 (公里)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 备注输入框（多行）
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("备注") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            maxLines = 5
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 按钮行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 取消按钮
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("取消")
            }

            // 保存按钮
            Button(
                onClick = {
                    // 创建更新后的Trip对象
                    val updatedTrip = Trip(
                        id = trip?.id ?: 0,  // 保持原ID，新增时为0
                        destination = destination,
                        startDate = startDate,
                        endDate = endDate,
                        type = selectedType,
                        notes = notes,
                        distance = distance.toDoubleOrNull() ?: 0.0,
                        createdAt = trip?.createdAt ?: System.currentTimeMillis()
                    )
                    onSave(updatedTrip)
                },
                modifier = Modifier.weight(1f),
                enabled = destination.isNotBlank() &&
                        startDate.isNotBlank() &&
                        endDate.isNotBlank()  // 必填字段验证
            ) {
                Text("保存")
            }
        }
    }
}