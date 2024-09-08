package com.schwerzl.leftbehind.di

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.room.Room
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.schwerzl.leftbehind.database.AppDatabase
import com.schwerzl.leftbehind.datasource.PermissionCheck
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class ApplicationProvider {

    @Provides
    @Singleton
    fun bleAdapter(
        @ApplicationContext context: Context
    ): BluetoothAdapter {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return bluetoothManager.adapter
    }

    @Provides
    @Singleton
    fun permissionCheck(
        @ApplicationContext context: Context
    ): PermissionCheck {
        return PermissionCheck(context)
    }

    @Provides
    @Singleton
    fun appDatabase(
        @ApplicationContext context: Context
    ) : AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "database-name").build()
    }

    @Provides
    @Singleton
    fun bleDeviceDao(
        appDatabase: AppDatabase
    ) = appDatabase.deviceDao()

    @Provides
    @Singleton
    fun geoFenceDao(
        appDatabase: AppDatabase
    ) = appDatabase.userGeoFenceDao()


    @Provides
    fun offloadDispatcher() = Dispatchers.IO

    @Provides
    @Singleton
    fun fusedLocationProvider(@ApplicationContext context: Context) : FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @Provides
    fun applicationContext(@ApplicationContext context: Context) = context

}