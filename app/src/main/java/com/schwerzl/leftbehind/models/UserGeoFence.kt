package com.schwerzl.leftbehind.models

data class UserGeoFence(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Float,
)
