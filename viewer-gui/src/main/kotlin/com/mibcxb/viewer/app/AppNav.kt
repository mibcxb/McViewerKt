package com.mibcxb.viewer.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.mibcxb.viewer.screen.ArchiveScreen
import com.mibcxb.viewer.screen.ArchiveScreenView
import com.mibcxb.viewer.screen.BrowseScreen
import com.mibcxb.viewer.screen.BrowseScreenView
import com.mibcxb.viewer.screen.DetailScreen
import com.mibcxb.viewer.screen.DetailScreenView

@Composable
fun AppNav(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = BrowseScreen,
        modifier = Modifier.fillMaxSize()
    ) {
        composable<BrowseScreen> {
            BrowseScreenView(nav = navController)
        }
        composable<DetailScreen> { backStackEntry ->
            val detailScreen = backStackEntry.toRoute<DetailScreen>()
            DetailScreenView(filepath = detailScreen.filepath, nav = navController)
        }
        composable<ArchiveScreen> { backStackEntry ->
            val archiveScreen = backStackEntry.toRoute<ArchiveScreen>()
            ArchiveScreenView(filepath = archiveScreen.filepath)
        }
    }
}