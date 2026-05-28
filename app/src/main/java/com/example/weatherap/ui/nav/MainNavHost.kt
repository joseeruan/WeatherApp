package com.example.weatherap.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.weatherap.ui.pages.HomePage
import com.example.weatherap.ui.pages.ListPage
import com.example.weatherap.ui.pages.MapPage

@Composable
fun MainNavHost(navController: NavHostController,
                modifier: Modifier = Modifier
) {
    NavHost(navController, startDestination = Route.Home) {
        composable<Route.Home> { HomePage(modifier = modifier) }
        composable<Route.List> { ListPage(modifier = modifier) }
        composable<Route.Map> { MapPage(modifier = modifier) }
    }
}
