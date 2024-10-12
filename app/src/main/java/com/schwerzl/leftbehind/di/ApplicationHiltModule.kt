package com.schwerzl.leftbehind.di

import com.schwerzl.leftbehind.data.datasource.AndroidBLEDataSource
import com.schwerzl.leftbehind.data.datasource.BLEDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ApplicationHiltModule {


    @Binds
    internal abstract fun bindsBLEDataSource(
        bleDataSource: AndroidBLEDataSource
    ) : BLEDataSource
}