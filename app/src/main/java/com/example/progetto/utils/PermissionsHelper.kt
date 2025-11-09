package com.example.progetto.utils

import android.Manifest
import androidx.compose.runtime.Composable
import com.google.accompanist.permissions.*

/**
 * 权限助手
 *
 * 类比Web：这是中间件（Middleware）
 * 检查用户是否有访问权限
 */
object PermissionsHelper {

    /**
     * 位置权限列表
     */
    val LOCATION_PERMISSIONS = listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    /**
     * 检查是否有位置权限
     */
    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    fun rememberLocationPermissionState(): MultiplePermissionsState {
        return rememberMultiplePermissionsState(
            permissions = LOCATION_PERMISSIONS
        )
    }
}