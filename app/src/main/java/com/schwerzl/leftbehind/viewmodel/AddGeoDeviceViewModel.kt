package com.schwerzl.leftbehind.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.schwerzl.leftbehind.data.database.DeviceDao
import com.schwerzl.leftbehind.data.database.DeviceRegisteredGeoFenceDao
import com.schwerzl.leftbehind.data.database.DeviceRegisteredGeoFenceEntity
import com.schwerzl.leftbehind.navigation.GeoDeviceScreen
import com.schwerzl.leftbehind.screens.BluetoothUIDevice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AddGeoDeviceViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val deviceRepository: DeviceDao,
    private val geoDeviceRepository: DeviceRegisteredGeoFenceDao,
    private val backgroundDispatcher: CoroutineDispatcher
) : ViewModel(){
    private val geoFenceId = savedStateHandle.toRoute<GeoDeviceScreen>().geofenceId
    private val _finishGeoFenceAdd = MutableSharedFlow<Boolean>( replay = 0)
    val finishGeoFenceAdd : SharedFlow<Boolean> = _finishGeoFenceAdd

    val availableDevices = deviceRepository.getAll().map {  list ->
        list.map {
            BluetoothUIDevice(
                name = it.name,
                address = it.address,
                timestamp = null
            )
        }
    }.
    stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private var _selectedDevices = mutableStateListOf<String>()
    val selectedDevices = _selectedDevices



    fun onDeviceSelected(device: BluetoothUIDevice){
        if(_selectedDevices.contains(device.address)){
            _selectedDevices.remove(device.address)
        }else{
            _selectedDevices.add(device.address)
        }
    }

    fun onSave(){
        viewModelScope.launch {
            withContext(backgroundDispatcher){
                val geoDevices = selectedDevices.map {
                    DeviceRegisteredGeoFenceEntity(geoFenceId, it)
                }
                geoDeviceRepository.insertAll(*geoDevices.toTypedArray())
                _finishGeoFenceAdd.emit(true)
            }
        }
    }

}