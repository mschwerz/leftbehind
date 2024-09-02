package com.schwerzl.leftbehind.database

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity
data class DeviceEntity(
    @PrimaryKey val address: String,
    @ColumnInfo(name = "name") val name: String,
)

@Dao
interface DeviceDao{
    @Query("SELECT * FROM deviceentity")
    fun getAll(): Flow<List<DeviceEntity>>

    @Insert
    fun insertAll(vararg devices: DeviceEntity)

    @Delete
    fun delete(device: DeviceEntity)
}



@Entity
data class UserGeoFenceEntity(
     @PrimaryKey val uid: Int,
     @ColumnInfo(name = "latitude") val latitude: Double,
     @ColumnInfo(name = "longitude") val longitude: Double,
     @ColumnInfo(name = "radius") val radius: Int,
     @ColumnInfo(name = "name") val name: String,
)

@Dao
interface UserGeoFenceDao{
    @Query("SELECT * FROM usergeofenceentity")
    fun getAll(): List<UserGeoFenceEntity>

    @Insert
    fun insertAll(vararg geofences: UserGeoFenceEntity)

    @Delete
    fun delete(geofence: UserGeoFenceEntity)
}

@Database(entities = [UserGeoFenceEntity::class, DeviceEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao
    abstract fun userGeoFenceDao(): UserGeoFenceDao
}
