package com.schwerzl.leftbehind.viewmodel

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schwerzl.leftbehind.datasource.LocationDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GeoFenceViewModel @Inject constructor(
    private val locationDataSource: LocationDataSource
) : ViewModel(){

    private var _currentLocation = MutableStateFlow<DataResult<Location?>>(DataResult.Success(null))
    val currentUserLocation = _currentLocation.asStateFlow()


    fun selectedMapPoint() {

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