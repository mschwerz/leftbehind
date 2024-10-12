package com.schwerzl.leftbehind.viewmodel

import android.location.Location
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schwerzl.leftbehind.data.datasource.LocationDataSource
import com.schwerzl.leftbehind.data.repository.GeoFenceRepository
import com.schwerzl.leftbehind.screens.AddGeoFenceUIData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AddGeoFenceViewModel @Inject constructor(
    private val geoFenceRepository: GeoFenceRepository,
    private val locationDataSource: LocationDataSource,
    private val backgroundDispatcher: CoroutineDispatcher
): ViewModel() {

    private var _selectedMapPoint: MutableState<AddGeoFenceUIData?> = mutableStateOf(null)
    val selectedMapPoint = _selectedMapPoint

    var radius = mutableDoubleStateOf(100.0)

    private var _currentLocation = MutableStateFlow<DataResult<Location?>>(DataResult.Success(null))
    val currentUserLocation = _currentLocation.asStateFlow()

    private val _acceptedGeoFence = MutableSharedFlow<String?>( replay = 0)
    val acceptedGeoFence : SharedFlow<String?> = _acceptedGeoFence

    fun selectedMapPoint(lat: Double, long: Double) {
        _selectedMapPoint.value = AddGeoFenceUIData(
            lat = lat,
            long = long,
        )
    }

    fun fetchCurrentLocation() {
        viewModelScope.launch {
            _currentLocation.value = DataResult.Loading
            try {
                val location = locationDataSource.getLastKnownLocation()
                _currentLocation.value = DataResult.Success(location)
            } catch (e: Exception) {
                _currentLocation.value = DataResult.Error(e)
            }
        }
    }

    fun onGeoFenceAccept(){
        viewModelScope.launch {
            withContext(backgroundDispatcher) {
                selectedMapPoint.value?.let {
                    val dataGeoFence = geoFenceRepository.addGeofence(
                        lat = it.lat,
                        long = it.long,
                        radius = radius.doubleValue.toFloat()
                    )
                    _acceptedGeoFence.emit(dataGeoFence.id)
                }
            }
        }
    }

}