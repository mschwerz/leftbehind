package com.schwerzl.leftbehind.datasource

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber
import javax.inject.Inject

@SuppressLint("MissingPermission")
class AndroidBLEDataSource @Inject constructor(
    private val bluetoothAdapter: BluetoothAdapter,
    private val permissionCheck: PermissionCheck
): BLEDataSource{

    override fun boundedDevices(): List<FoundDevices> {
        return if(permissionCheck.check(Manifest.permission.BLUETOOTH_CONNECT)){
            Timber.d("${bluetoothAdapter.bondedDevices.size}")

            bluetoothAdapter.bondedDevices.map { it.toFoundDevice() }
        } else{
            emptyList()
        }
    }

    override fun scanDevices(): Flow<List<FoundDevices>> {
        val bleScanner = bluetoothAdapter.bluetoothLeScanner
        return callbackFlow {
            val callback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    trySend(listOf(result.device.toFoundDevice()))
                }

                override fun onBatchScanResults(results: List<ScanResult>) {
                    trySend(results.map { it.device.toFoundDevice() })
                }
            }

            bleScanner.startScan(callback)

            awaitClose { bleScanner.stopScan(callback) }
        }
    }
}

@SuppressLint("MissingPermission")
fun BluetoothDevice.toFoundDevice(
) : FoundDevices{
    return FoundDevices(this?.name ?: "Unknown Device", this.address, this.uuids?.map {uuid -> uuid.toString() },)
}