package com.schwerzl.leftbehind.data.worker

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.schwerzl.leftbehind.R
import com.schwerzl.leftbehind.data.database.DeviceEntity
import com.schwerzl.leftbehind.data.database.DeviceRegisteredGeoFenceDao
import com.schwerzl.leftbehind.domain.CheckDeviceNearbyUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext


const val TRIGGERING_GEOFENCE_KEY = "TRIGGERING_GEOFENCE"

class CheckDevicesNearbyWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val checkDeviceNearbyUseCase: CheckDeviceNearbyUseCase,
    private val deviceGeoFenceDao: DeviceRegisteredGeoFenceDao
) : CoroutineWorker(
    appContext,
    workerParams
) {

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return super.getForegroundInfo()
    }

    override suspend fun doWork(): Result {
        val geoFences = inputData.getStringArray(TRIGGERING_GEOFENCE_KEY)

        if (geoFences.isNullOrEmpty()) {
            return Result.failure()
        }

        val failedDevices = mutableListOf<DeviceEntity>()

        withContext(Dispatchers.IO){
            val linkedDevices = geoFences.flatMap {
                deviceGeoFenceDao.getDevicesForGeofence(it)
            }
            // Process linkedDevices in parallel
            linkedDevices.map { device ->
                async {
                    if (!checkDeviceNearbyUseCase.isRequestedDeviceNearby(device.address, 5_000)) {
                        failedDevices.add(device)
                    }
                }
            }.awaitAll()
        }

        if (failedDevices.isNotEmpty()) {
            val notification = createLeftBehindNotification(failedDevices)
            val notificationManager = NotificationManagerCompat.from(applicationContext)
            if(ActivityCompat.checkSelfPermission(
                applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            ){
                notificationManager.notify(1, notification)

            }

        }
        return Result.success()
    }


    private fun createLeftBehindNotification(failedDevices: List<DeviceEntity>): Notification {
        val notificationManager = NotificationManagerCompat.from(applicationContext)
        val channelId = "left_behind_channel_id" // Define your channel ID

        // Create a notification channel (if not already created)
        val channel = NotificationChannel(
            channelId,
            "Left Behind Notifications",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)

        // Build the notification content
        val notificationBuilder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with your icon
            .setContentTitle("Left Behind Devices")
            .setContentText(getNotificationContentText(failedDevices))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        // Create the notification
        return notificationBuilder.build()
    }

    private fun getNotificationContentText(failedDevices: List<DeviceEntity>): String {
        return if (failedDevices.size == 1) {
            "You left ${failedDevices[0].name} behind!"
        } else {
            "You left ${failedDevices.size} devices behind!"
        }
    }

}