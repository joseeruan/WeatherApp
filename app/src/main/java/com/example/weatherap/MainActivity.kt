package com.example.weatherap

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.util.Consumer
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.weatherap.api.WeatherService
import com.example.weatherap.db.fb.FBDatabase
import com.example.weatherap.db.local.LocalDatabase
import com.example.weatherap.model.MainViewModel
import com.example.weatherap.model.MainViewModelFactory
import com.example.weatherap.monitor.ForecastMonitor
import com.example.weatherap.repo.Repository
import com.example.weatherap.ui.nav.BottomNavBar
import com.example.weatherap.ui.nav.BottomNavItem
import com.example.weatherap.ui.nav.MainNavHost
import com.example.weatherap.ui.nav.Route
import com.example.weatherap.ui.theme.WeatherApTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            val fbDB = remember { FBDatabase() }
            val user = remember { Firebase.auth.currentUser }
            val localDB = remember(user) { LocalDatabase(this, user?.uid ?: "default") }
            val repo = remember(fbDB, localDB) { Repository(fbDB, localDB) }
            val weatherService = remember { WeatherService(this) }
            val monitor = remember { ForecastMonitor(this) }

            val viewModel : MainViewModel = viewModel(
                factory = MainViewModelFactory(repo, weatherService, monitor)
            )

            DisposableEffect(Unit) {
                val listener = Consumer<Intent> { intent ->
                    intent.getStringExtra("city")?.let { cityName ->
                        viewModel.city = cityName
                        viewModel.page = Route.Home
                    }
                }
                addOnNewIntentListener(listener)
                onDispose { removeOnNewIntentListener(listener) }
            }

            LaunchedEffect(Unit) {
                intent.getStringExtra("city")?.let { cityName ->
                    viewModel.city = cityName
                    viewModel.page = Route.Home
                }
            }

            var showDialog by remember { mutableStateOf(false) }
            val currentRoute = navController.currentBackStackEntryAsState()
            val showButton = currentRoute.value?.destination?.hasRoute(Route.List::class) == true

            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions(),
                onResult = { }
            )

            LaunchedEffect(Unit) {
                val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                }
                launcher.launch(permissions.toTypedArray())
            }

            WeatherApTheme {
                if (showDialog) CityDialog(
                    onDismiss = { showDialog = false },
                    onConfirm = { city ->
                        if (city.isNotBlank()) viewModel.addCity(city)
                        showDialog = false
                    })
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                val name = viewModel.user?.name ?: "[carregando...]"
                                Text("Bem-vindo/a! $name")
                            },
                            actions = {
                                IconButton(onClick = { Firebase.auth.signOut()}) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                        contentDescription = "Sair"
                                    )
                                }
                            }
                        )
                    },
                    bottomBar = {
                        val items = listOf(
                            BottomNavItem.HomeButton,
                            BottomNavItem.ListButton,
                            BottomNavItem.MapButton,
                        )
                        BottomNavBar(viewModel, navController, items)
                    },
                    floatingActionButton = {
                        if (showButton) {
                            FloatingActionButton(onClick = { showDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Adicionar")
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        MainNavHost(navController = navController, viewModel = viewModel)
                    }
                    LaunchedEffect(viewModel.page) {
                        navController.navigate(viewModel.page) {
                            navController.graph.startDestinationRoute?.let {
                                popUpTo(it) { saveState = true }
                                restoreState = true
                            }
                            launchSingleTop = true
                        }
                    }
                }
            }
        }
    }
}
