package com.schwerzl.leftbehind.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.schwerzl.leftbehind.data.worker.GeoFenceSyncWorker
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BootRegisterGeoFenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Perform your desired actions here
            //schedule sync with database and geofence manager
            val workManager = WorkManager.getInstance(context)
            val syncRequest = OneTimeWorkRequestBuilder<GeoFenceSyncWorker>().build()

            workManager.enqueue(syncRequest)
        }
    }


}