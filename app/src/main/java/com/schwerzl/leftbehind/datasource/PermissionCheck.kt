package com.schwerzl.leftbehind.datasource

import android.content.Context
import android.content.pm.PackageManager
import javax.inject.Inject

class PermissionCheck @Inject constructor(
    private val context: Context,
) {
    fun check(permission: String): Boolean {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

}