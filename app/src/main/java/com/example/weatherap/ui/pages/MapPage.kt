package com.example.weatherap.ui.pages

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getDrawable
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.scale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.weatherap.R
import com.example.weatherap.model.MainViewModel
import com.example.weatherap.model.Weather
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@SuppressLint("UnrememberedMutableState")
@Composable
fun MapPage(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel
) {
    val cameraPositionState =
        rememberCameraPositionState()

    val context = LocalContext.current

    val hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cities = viewModel.cities
        .collectAsStateWithLifecycle(emptyMap())
        .value

    val weatherMap = viewModel.weather
        .collectAsStateWithLifecycle(emptyMap())
        .value

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        onMapClick = { location ->
            viewModel.addCity(location)
        },
        properties = MapProperties(
            isMyLocationEnabled = hasLocationPermission
        ),
        uiSettings = MapUiSettings(
            myLocationButtonEnabled =
                hasLocationPermission
        )
    ) {
        cities.values.forEach { city ->
            if (city.location != null) {
                val weather =
                    weatherMap[city.name] ?: Weather.LOADING

                LaunchedEffect(city.name) {
                    viewModel.loadWeather(city.name)
                }

                LaunchedEffect(weather) {
                    viewModel.loadBitmap(city.name)
                }

                val image = weather.bitmap
                    ?: getDrawable(
                        context,
                        R.drawable.loading
                    )!!.toBitmap()

                val marker = BitmapDescriptorFactory
                    .fromBitmap(
                        image.scale(120, 120)
                    )

                val description =
                    if (weather == Weather.LOADING) {
                        "Carregando clima..."
                    } else {
                        weather.desc
                    }

                Marker(
                    state = MarkerState(
                        position = city.location
                    ),
                    icon = marker,
                    title = city.name,
                    snippet = description
                )
            }
        }
    }
}