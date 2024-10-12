package com.schwerzl.leftbehind.domain

import com.schwerzl.leftbehind.data.datasource.BLEDataSource
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeout

class CheckDeviceNearbyUseCase(
    private val bleDataSource: BLEDataSource
) {
    suspend fun isRequestedDeviceNearby(deviceAddress: String, timeoutMillis: Long): Boolean {
        return try {
            withTimeout(timeoutMillis) {
                bleDataSource.scanDevices()
                    .firstOrNull {devices -> devices.any { it.address == deviceAddress } } != null
            }
        } catch (e: TimeoutCancellationException) {
            false // Device not found within timeout
        }
    }
}