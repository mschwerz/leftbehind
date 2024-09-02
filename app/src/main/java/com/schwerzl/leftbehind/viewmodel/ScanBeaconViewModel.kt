package com.schwerzl.leftbehind.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schwerzl.leftbehind.datasource.BLEDataSource
import com.schwerzl.leftbehind.datasource.FoundDevices
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

const val DEVICE_TIMEOUT = 10_000

@HiltViewModel
class ScanBeaconViewModel @Inject constructor(
private val bleDataSource: BLEDataSource
): ViewModel(){

    val scanning = bleDataSource.scanDevices()
        .scan(mapOf<String, FoundDevices>()){
                accumulator, value -> accumulator.plus(value.associateBy { it.address })
        }.map{
            val now = System.currentTimeMillis()
            it.values.toList().filter {
                now - it.timestamp < DEVICE_TIMEOUT
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

}