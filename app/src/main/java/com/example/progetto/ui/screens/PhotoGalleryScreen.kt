package com.example.progetto.ui.screens

import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.progetto.data.dao.PhotoDao
import com.example.progetto.data.entity.Photo
import com.example.progetto.utils.PhotoManager
import com.example.progetto.utils.LocationManager
import com.example.progetto.utils.MediaStoreHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 照片相册界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoGalleryScreen(
    tripId: Long,
    tripName: String,
    photoDao: PhotoDao,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationManager = remember { LocationManager(context) }
    val photos by photoDao.getPhotosByTripId(tripId).collectAsState(initial = emptyList())

    var selectedPhoto by remember { mutableStateOf<Photo?>(null) }
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }

    // 照片选择器
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                withContext(Dispatchers.IO) {
                    // 保存照片
                    val filePath = PhotoManager.savePhoto(context, uri)
                    if (filePath != null) {
                        // 获取当前位置（可能为 null）
                        val lastLocation = try {
                            locationManager.getLastLocation()
                        } catch (_: Exception) { null }

                        // 保存到数据库（尽可能附带经纬度）
                        val photo = Photo(
                            tripId = tripId,
                            filePath = filePath,
                            latitude = lastLocation?.latitude,
                            longitude = lastLocation?.longitude
                        )
                        photoDao.insert(photo)
                    }
                }
            }
        }
    }

    // 相机权限申请
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* 结果由下次重组检查 */ }

    // 拍照启动器
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        val captureUri = pendingPhotoUri
        if (success && captureUri != null) {
            scope.launch {
                withContext(Dispatchers.IO) {
                    val filePath = PhotoManager.savePhoto(context, captureUri)
                    if (filePath != null) {
                        val lastLocation = try {
                            locationManager.getLastLocation()
                        } catch (_: Exception) { null }

                        val photo = Photo(
                            tripId = tripId,
                            filePath = filePath,
                            latitude = lastLocation?.latitude,
                            longitude = lastLocation?.longitude
                        )
                        photoDao.insert(photo)

                        // 尝试删除媒体库中的临时条目，避免重复占用空间
                        try { context.contentResolver.delete(captureUri, null, null) } catch (_: Exception) {}
                    }
                }
            }
        }
        pendingPhotoUri = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$tripName - 照片") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FloatingActionButton(
                    onClick = {
                        // 拍照：检查权限，创建目标Uri，启动相机
                        val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                        if (!granted) {
                            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                            return@FloatingActionButton
                        }

                        val uri = MediaStoreHelper.createImageUri(
                            context,
                            "trip_${tripId}_${System.currentTimeMillis()}"
                        )
                        if (uri != null) {
                            pendingPhotoUri = uri
                            takePictureLauncher.launch(uri)
                        }
                    }
                ) {
                    Icon(Icons.Default.AddAPhoto, contentDescription = "拍照")
                }

                FloatingActionButton(
                    onClick = {
                        // 打开照片选择器
                        photoPickerLauncher.launch("image/*")
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "添加照片")
                }
            }
        }
    ) { padding ->
        if (photos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📷", style = MaterialTheme.typography.displayLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("还没有添加照片")
                    Text(
                        "点击右下角+按钮添加照片",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(4.dp)
            ) {
                items(photos) { photo ->
                    PhotoGridItem(
                        photo = photo,
                        onClick = {
                            selectedPhoto = photo
                        }
                    )
                }
            }
        }
    }

    // 照片详情对话框
    selectedPhoto?.let { photo ->
        PhotoDetailDialog(
            photo = photo,
            onDismiss = { selectedPhoto = null },
            onDelete = {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        PhotoManager.deletePhoto(photo.filePath)
                        photoDao.delete(photo)
                    }
                    selectedPhoto = null
                }
            }
        )
    }
}

/**
 * 照片网格项
 */
@Composable
private fun PhotoGridItem(
    photo: Photo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(4.dp)
            .aspectRatio(1f)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(File(photo.filePath))
                .crossfade(true)
                .build(),
            contentDescription = "照片",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

/**
 * 照片详情对话框
 */
@Composable
private fun PhotoDetailDialog(
    photo: Photo,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("照片详情") },
        text = {
            Column {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(File(photo.filePath))
                        .crossfade(true)
                        .build(),
                    contentDescription = "照片",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "拍摄时间：${
                        java.text.SimpleDateFormat(
                            "yyyy-MM-dd HH:mm",
                            java.util.Locale.getDefault()
                        ).format(java.util.Date(photo.timestamp))
                    }",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        },
        dismissButton = {
            TextButton(
                onClick = { showDeleteConfirm = true },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("删除")
            }
        }
    )

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除这张照片吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}
