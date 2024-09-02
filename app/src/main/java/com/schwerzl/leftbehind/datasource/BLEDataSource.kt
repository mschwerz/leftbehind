package com.schwerzl.leftbehind.datasource

import kotlinx.coroutines.flow.Flow

data class FoundDevices(
    val deviceName: String,
    val address: String,
    val uuids: List<String>?,
    val timestamp: Long = System.currentTimeMillis()

)

interface BLEDataSource {

    fun boundedDevices() : List<FoundDevices>
    fun scanDevices() : Flow<List<FoundDevices>>
}