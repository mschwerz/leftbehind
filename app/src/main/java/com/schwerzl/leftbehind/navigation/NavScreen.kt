package com.schwerzl.leftbehind.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.schwerzl.leftbehind.screens.AddGeoDeviceScreen
import com.schwerzl.leftbehind.screens.GeoFenceMapScreen
import com.schwerzl.leftbehind.screens.GeoFenceSelectionScreen
import com.schwerzl.leftbehind.screens.ScanBeaconScreen
import kotlinx.serialization.Serializable

@Composable
fun NavScreen(
    navController: NavHostController,
){
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    NavigationSuiteScaffold(
        navigationSuiteItems = {
            Screens.entries.forEach { rootScreen ->
                val test = currentDestination?.hierarchy?.any { it.route == rootScreen.screenId.javaClass.name } == true
                item(
                    icon = {
                        Icon(rootScreen.icon, contentDescription = null)
                    },
                    label = {
                        Text(rootScreen.screen)
                    },
                    onClick = {
                        navController.navigate(rootScreen.screenId)
                    },
                    selected = test,
                )
            }
        }
    ){
        NavHost(navController, startDestination = Screens.HOME.screenId){
            composable<HomeScreen>{
                ScanBeaconScreen()
            }
            composable<GeofenceMapScreen> {
                GeoFenceMapScreen(onAddGeoFence = {navController.navigate(MapScreen)})
            }

            composable<MapScreen>{
                GeoFenceSelectionScreen(onGeoFenceAccept = {
                    navController.navigate(GeoDeviceScreen(it))
                })
            }
            composable<SettingsScreen>{
                ScanBeaconScreen()
            }
            composable<GeoDeviceScreen>{
                AddGeoDeviceScreen(
                    finishFlow = {
                        navController.popBackStack(MapScreen, inclusive = false)
                    }
                )
            }

        }

    }

}

@Serializable
object HomeScreen

@Serializable
object MapScreen

@Serializable
object SettingsScreen

@Serializable
object GeofenceMapScreen

@Serializable
data class GeoDeviceScreen(
    val geofenceId: String
)

enum class Screens(val screen: String, val screenId: Any, val icon:ImageVector){
    HOME("Home", HomeScreen, Icons.Filled.Home),
    MAP("Map", GeofenceMapScreen, Icons.Filled.Email),
    SETTINGS("Settings", SettingsScreen, Icons.Filled.Settings)
}