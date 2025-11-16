package com.example.progetto.utils

import android.Manifest
import androidx.compose.runtime.Composable
import com.google.accompanist.permissions.*


object PermissionsHelper {


    val LOCATION_PERMISSIONS = listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )


    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    fun rememberLocationPermissionState(): MultiplePermissionsState {
        return rememberMultiplePermissionsState(
            permissions = LOCATION_PERMISSIONS
        )
    }
}