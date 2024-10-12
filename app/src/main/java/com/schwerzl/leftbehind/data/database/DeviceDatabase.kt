package com.schwerzl.leftbehind.data.database

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.ForeignKey
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
    foreignKeys = [
        ForeignKey(entity = UserGeoFenceEntity::class, parentColumns = ["uid"], childColumns = ["geofenceId"],
            onUpdate = ForeignKey.CASCADE, onDelete = ForeignKey.CASCADE)
    ]
)

//Connects the user requested geofence to the device's geofence manager
data class DeviceRegisteredGeoFenceEntity(
    @PrimaryKey
    @ColumnInfo(name= "geofenceId") val geofenceId: Int,
    @ColumnInfo(name= "deviceRequestIdentifier") val requestId : String,
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

}

@Database(entities = [UserGeoFenceEntity::class, DeviceEntity::class,
    DeviceRegisteredGeoFenceEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao
    abstract fun userGeoFenceDao(): UserGeoFenceDao
    abstract fun deviceRegisteredGeoFenceDao(): DeviceRegisteredGeoFenceDao
}
