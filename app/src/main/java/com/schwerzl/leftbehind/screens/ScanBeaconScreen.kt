package com.schwerzl.leftbehind.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.schwerzl.leftbehind.viewmodel.ScanBeaconViewModel
import java.text.SimpleDateFormat
import java.util.Date


data class BluetoothUIDevice(
    val name: String,
    val address: String,
    val timestamp: Long?
)

@Composable
fun ScanBeaconScreen(
    viewModel: ScanBeaconViewModel = hiltViewModel()
){
    val backgroundRequest = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {

    }

    val permissionRequest = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        it[Manifest.permission.BLUETOOTH_SCAN]?.let { it1 -> viewModel.onBTPermissionResult(it1) }

        backgroundRequest.launch(
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )

    }
    LaunchedEffect(Unit) {
        permissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                //Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        )
    }

    val deviceList by viewModel.deviceList.collectAsStateWithLifecycle()

    DeviceList(deviceList.first, deviceList.second,
        onSelectDevice = viewModel::selectDevice,
        onRemoveDevice = viewModel::removeDevice
    )
}

@Composable
@Preview
private fun DeviceList(
    trackedList: List<BluetoothUIDevice> = listOf(PreviewDevice.copy(address = "FF:FF:FF:FF:FF:F0")),
    deviceList: List<BluetoothUIDevice> = listOf(PreviewDevice),
    onSelectDevice : (BluetoothUIDevice) -> Unit = {},
    onRemoveDevice : (BluetoothUIDevice) -> Unit = {},
){

    LazyColumn(
        modifier = Modifier.padding(16.dp),
    ) {

        item{
            Text(
                modifier = Modifier.padding(bottom = 16.dp),
                text = "Tracked Devices", style = MaterialTheme.typography.headlineMedium)
        }
        items( trackedList.size,
            key = {trackedList[it].address}) { index ->
            DeviceItem(
                modifier = Modifier.clickable {onRemoveDevice(trackedList[index])},
                device = trackedList[index])
        }

        item{
            Text(
                modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
                text = "Found Devices", style = MaterialTheme.typography.headlineMedium)
        }
        items(deviceList.size,
            key = {deviceList[it].address}) { index ->
            val device = deviceList[index]
            DeviceItem(
                modifier = Modifier.clickable {onSelectDevice(device)},
                device = device)
        }
    }
}

@Composable
@Preview
private fun DeviceItem(
    modifier: Modifier = Modifier,
    device: BluetoothUIDevice = PreviewDevice
){
    Card(
        modifier = modifier
    ) {
        ListItem(
            headlineContent = {
                Text(text = device.name)
            },
            supportingContent = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ){
                    Text(text = device.address)
                    device.timestamp?.let {
                        val formattedDate = SimpleDateFormat.getDateTimeInstance().format(Date(it))
                        Text(text = formattedDate)
                    }
                }
            }
        )
    }
}

val PreviewDevice = BluetoothUIDevice(
    name = "Test Device",
    address = "FF:FF:FF:FF:FF:FF",
    timestamp = System.currentTimeMillis()
)