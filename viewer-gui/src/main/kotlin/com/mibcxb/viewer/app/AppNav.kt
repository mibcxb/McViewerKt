package com.mibcxb.viewer.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mibcxb.viewer.screen.HOME_SCREEN
import com.mibcxb.viewer.screen.HomeScreen

@Composable
fun AppNav(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = HOME_SCREEN,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(HOME_SCREEN) {
            HomeScreen()
        }
    }
}