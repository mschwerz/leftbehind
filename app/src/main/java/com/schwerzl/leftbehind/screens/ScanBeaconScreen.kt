package com.schwerzl.leftbehind.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.schwerzl.leftbehind.datasource.FoundDevices
import com.schwerzl.leftbehind.viewmodel.ScanBeaconViewModel
import java.text.SimpleDateFormat
import java.util.Date


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
    val deviceList by viewModel.scanning.collectAsStateWithLifecycle()

    DeviceList(deviceList)

}

@Composable
private fun DeviceList(
    deviceList: List<FoundDevices>
){

    LazyColumn {
        items(deviceList.size,
            key = {deviceList[it].address}) { index ->
            DeviceItem(deviceList[index])
        }
    }
}

@Composable
@Preview
private fun DeviceItem(
    device: FoundDevices = FoundDevices(
        deviceName = "Test Device",
        address = "FF:FF:FF:FF:FF:FF",
        uuids = null
    )
){
    Card {
        ListItem(
            headlineContent = {
                Text(text = device.deviceName)
            },
            supportingContent = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ){
                    Text(text = device.address)


                    val formattedDate = SimpleDateFormat.getDateTimeInstance().format(Date(device.timestamp))

                    Text(text = formattedDate)
                }
            }
        )
    }
}