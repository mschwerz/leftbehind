package com.schwerzl.leftbehind.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.schwerzl.leftbehind.domain.CheckDeviceNearbyUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

class CheckDevicesNearbyWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val checkDeviceNearbyUseCase: CheckDeviceNearbyUseCase
) : CoroutineWorker(
    appContext,
    workerParams
) {

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return super.getForegroundInfo()
    }

    override suspend fun doWork(): Result {
        TODO("Not yet implemented")


        checkDeviceNearbyUseCase.isRequestedDeviceNearby()

    }
}