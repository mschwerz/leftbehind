package com.schwerzl.leftbehind.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schwerzl.leftbehind.database.DeviceDao
import com.schwerzl.leftbehind.database.DeviceEntity
import com.schwerzl.leftbehind.datasource.BLEDataSource
import com.schwerzl.leftbehind.datasource.FoundDevices
import com.schwerzl.leftbehind.screens.BluetoothUIDevice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

const val DEVICE_TIMEOUT = 10_000

@HiltViewModel
class ScanBeaconViewModel @Inject constructor(
    private val bleDataSource: BLEDataSource,
    private val deviceDao: DeviceDao,
    private val offloadDispatcher : CoroutineDispatcher
    ): ViewModel(){

        private val trackedDevices = deviceDao.getAll().map {
            it.map { deviceEntity ->
                BluetoothUIDevice(
                    address = deviceEntity.address,
                    name = deviceEntity.name,
                    timestamp = null
                ) }
        }

    private val scanning = bleDataSource.scanDevices()
        .scan(mapOf<String, FoundDevices>()){
                accumulator, value -> accumulator.plus(value.associateBy { it.address })
        }.map{
            val now = System.currentTimeMillis()
            it.values.toList().filter {
                now - it.timestamp < DEVICE_TIMEOUT
            }.map { filterDevice -> filterDevice.toUIDevice() }
        }


    val deviceList = combine(trackedDevices,
        scanning
        ){ tracked, scanning ->
        Pair(
            tracked,
            scanning.filter { scanned -> tracked.none { scanned.address == it.address }  }

        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        Pair(emptyList(), emptyList())
    )



    fun selectDevice(foundDevice : BluetoothUIDevice){
        viewModelScope.launch {
            withContext(offloadDispatcher) {
                deviceDao.insertAll(
                    DeviceEntity(
                        address = foundDevice.address,
                        name = foundDevice.name
                    )
                )
            }
        }
    }

    fun removeDevice(device : BluetoothUIDevice){
        viewModelScope.launch {
            withContext(offloadDispatcher) {
                deviceDao.delete(
                    DeviceEntity(
                        address = device.address,
                        name = device.name
                    )
                )
            }
        }
    }
}


fun FoundDevices.toUIDevice() = BluetoothUIDevice(
    address = address,
    name = deviceName,
    timestamp = timestamp
)