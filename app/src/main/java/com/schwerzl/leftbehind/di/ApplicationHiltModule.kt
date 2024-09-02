package com.schwerzl.leftbehind.di

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.core.content.ContextCompat.getSystemService
import com.schwerzl.leftbehind.datasource.AndroidBLEDataSource
import com.schwerzl.leftbehind.datasource.BLEDataSource
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ApplicationHiltModule {


    @Binds
    internal abstract fun bindsBLEDataSource(
        bleDataSource: AndroidBLEDataSource
    ) : BLEDataSource





}