package com.schwerzl.leftbehind.viewmodel

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schwerzl.leftbehind.data.datasource.LocationDataSource
import com.schwerzl.leftbehind.data.repository.GeoFenceRepository
import com.schwerzl.leftbehind.screens.GeoFenceUIData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class GeoFenceViewModel @Inject constructor(
    private val locationDataSource: LocationDataSource,
    private val geoFenceRepository: GeoFenceRepository,
    private val backgroundDispatcher: CoroutineDispatcher,
) : ViewModel(){


    val userGeoFences = geoFenceRepository.getGeofences()
        .map {
            it.map { entity ->
                GeoFenceUIData(
                    id = entity.id,
                    name = entity.name,
                    lat = entity.latitude,
                    long = entity.longitude,
                    radius = entity.radius
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private var _currentLocation = MutableStateFlow<DataResult<Location?>>(DataResult.Success(null))
    val currentUserLocation = _currentLocation.asStateFlow()


    fun selectedMapPoint(lat: Double, long: Double) {
        viewModelScope.launch {
            withContext(backgroundDispatcher) {
                Timber.d("Adding the point to the repo $lat, $long")
                geoFenceRepository.addGeofence(lat, long)
            }
        }
    }

    fun onTapMarker(geoFenceUIData: GeoFenceUIData){
        viewModelScope.launch {
            withContext(backgroundDispatcher){
                geoFenceRepository.removeGeofence(geoFenceUIData.id)
            }
        }
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

}

sealed interface DataResult<out T> {
    data object Loading : DataResult<Nothing>
    data class Success<T>(val lst: T) : DataResult<T>
    data class Error(val err: Throwable) : DataResult<Nothing>
}