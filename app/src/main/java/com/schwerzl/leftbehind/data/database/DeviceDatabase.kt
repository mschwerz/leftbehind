package com.schwerzl.leftbehind.data.database

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import com.schwerzl.leftbehind.models.UserGeoFence
import kotlinx.coroutines.flow.Flow
import java.util.UUID

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


//The user requested geofence
@Entity
data class UserGeoFenceEntity(
     @PrimaryKey
     val uid: String = UUID.randomUUID().toString(),
     @ColumnInfo(name = "latitude") val latitude: Double,
     @ColumnInfo(name = "longitude") val longitude: Double,
     @ColumnInfo(name = "radius") val radius: Float,
     @ColumnInfo(name = "name") val name: String,
){
    fun toDomain() : UserGeoFence {
        return UserGeoFence(
            id = uid,
            latitude = latitude,
            longitude = longitude,
            radius = radius,
            name = name
        )
    }
}


@Entity(
    primaryKeys = ["geofenceId", "deviceRequestIdentifier"],
    foreignKeys = [
        ForeignKey(entity = UserGeoFenceEntity::class, parentColumns = ["uid"], childColumns = ["geofenceId"],
            onUpdate = ForeignKey.CASCADE, onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = DeviceEntity::class, parentColumns = ["address"], childColumns = ["deviceRequestIdentifier"],
            onUpdate = ForeignKey.CASCADE, onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index(value = ["deviceRequestIdentifier"])] // Add this line
)

//Connects the user requested geofence to the device's geofence manager
data class DeviceRegisteredGeoFenceEntity(
    @ColumnInfo(name= "geofenceId") val geofenceId: String,
    @ColumnInfo(name= "deviceRequestIdentifier") val deviceRequestIdentifier : String,
)

@Dao
interface UserGeoFenceDao{
    @Query("SELECT * FROM usergeofenceentity")
    fun getAll(): List<UserGeoFenceEntity>

    @Query("SELECT * FROM usergeofenceentity WHERE uid = :uuid")
    fun getGeoFence(uuid: String) : UserGeoFenceEntity

    @Insert
    fun insertAll(vararg geofences: UserGeoFenceEntity)

    @Delete
    fun delete(geofence: UserGeoFenceEntity)

    @Query("SELECT * FROM usergeofenceentity")
    fun getAllFlow(): Flow<List<UserGeoFenceEntity>>

}


@Dao
interface DeviceRegisteredGeoFenceDao{
    @Query(
        """
        SELECT T2.* 
        FROM DeviceRegisteredGeoFenceEntity AS T1
        INNER JOIN DeviceEntity AS T2
        ON T1.deviceRequestIdentifier = T2.address
        WHERE T1.geofenceId = :geofenceId
        """
    )
    fun getDevicesForGeofence(geofenceId: String): List<DeviceEntity>

    @Insert
    fun insertAll(vararg geofences: DeviceRegisteredGeoFenceEntity)

    @Delete
    fun delete(geofence: DeviceRegisteredGeoFenceEntity)
}

@Database(entities = [UserGeoFenceEntity::class, DeviceEntity::class,
    DeviceRegisteredGeoFenceEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao
    abstract fun userGeoFenceDao(): UserGeoFenceDao
    abstract fun deviceRegisteredGeoFenceDao(): DeviceRegisteredGeoFenceDao
}
