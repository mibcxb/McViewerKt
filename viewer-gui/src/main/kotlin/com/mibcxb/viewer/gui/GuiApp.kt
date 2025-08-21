package com.mibcxb.viewer.gui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mibcxb.viewer.gui.screen.HOME_SCREEN
import com.mibcxb.viewer.gui.screen.HomeScreen

@Composable
fun GuiApp(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = HOME_SCREEN,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        composable(HOME_SCREEN) {
            HomeScreen()
        }
    }
}