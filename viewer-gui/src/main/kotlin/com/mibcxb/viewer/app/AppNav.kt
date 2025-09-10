package com.mibcxb.viewer.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mibcxb.viewer.screen.BROWSE_SCREEN
import com.mibcxb.viewer.screen.BrowseScreen
import com.mibcxb.viewer.screen.DETAIL_SCREEN
import com.mibcxb.viewer.screen.DetailScreen

@Composable
fun AppNav(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = BROWSE_SCREEN,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(BROWSE_SCREEN) {
            BrowseScreen()
        }
        composable(DETAIL_SCREEN) {
            DetailScreen()
        }
    }
}