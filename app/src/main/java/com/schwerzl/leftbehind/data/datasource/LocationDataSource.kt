package com.schwerzl.leftbehind.data.datasource

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

class LocationDataSource @Inject constructor(
    private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient
) {


    suspend fun getLastKnownLocation() : Location? {
        check(
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED
        ){ "Missing location permission" }

        val currentLocation = fusedLocationClient.getCurrentLocation(
            CurrentLocationRequest.Builder().setDurationMillis(5_000).build(),
            null
        ).await()

        return currentLocation
    }


    fun getCurrentLocation() : Flow<Location?> {

        check(
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED
        ){ "Missing location permission" }

        val locationRequest = LocationRequest.Builder(5_000)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .build()

        return callbackFlow {
            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    Timber.d("Location update: ${result.lastLocation}")
                    result.lastLocation?.let {
                        trySend(it)
                    }
                }
            }
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

            awaitClose {
                fusedLocationClient.removeLocationUpdates(locationCallback)
            }
        }
    }

}