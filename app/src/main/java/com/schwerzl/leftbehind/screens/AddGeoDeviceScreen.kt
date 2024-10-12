package com.schwerzl.leftbehind.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.schwerzl.leftbehind.viewmodel.AddGeoDeviceViewModel


@Composable
fun AddGeoDeviceScreen(
    viewModel: AddGeoDeviceViewModel = hiltViewModel(),
    finishFlow: () -> Unit
){
    val availableDevices by viewModel.availableDevices.collectAsStateWithLifecycle()
    val finishingFlow by viewModel.finishGeoFenceAdd.collectAsStateWithLifecycle(initialValue = false)
    LaunchedEffect(finishingFlow) {
        if(finishingFlow){
            finishFlow()
        }
    }

    AddGeoDeviceContent(
        allDevices = availableDevices,
        selectedDevices = viewModel.selectedDevices,
        onDeviceTapped = viewModel::onDeviceSelected,
        onSave = viewModel::onSave
    )
}

@Composable
@Preview
fun AddGeoDeviceContent(
    allDevices: List<BluetoothUIDevice> = listOf(
        BluetoothUIDevice("123", "Device 1", null),
        BluetoothUIDevice("456", "Device 2", null),
        BluetoothUIDevice("789", "Device 3", null),
    ),
    selectedDevices: List<String> = emptyList(),
    onDeviceTapped : (BluetoothUIDevice) -> Unit = {},
    onSave: () -> Unit = {}
){
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {


        LazyColumn {
            items(
                key = { allDevices[it].address },
                count = allDevices.size,
            ) { index ->
                val device = allDevices[index]
                val isSelected = device.address in selectedDevices

                DeviceItem(
                    modifier = Modifier
                        .clickable {
                            onDeviceTapped(device)
                        }
                        .background(
                            if (isSelected) Color.Green else Color.Transparent
                        ),
                    device = device
                )
            }
        }
        Button(
            onClick = onSave
        ) {
            Text("Save")
        }
    }


}