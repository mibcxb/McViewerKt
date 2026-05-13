package com.mibcxb.viewer.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.mibcxb.viewer.cache.CacheSqlite
import com.mibcxb.viewer.screen.ArchiveScreen
import com.mibcxb.viewer.screen.ArchiveScreenView
import com.mibcxb.viewer.screen.BrowseScreen
import com.mibcxb.viewer.screen.BrowseScreenViewNew
import com.mibcxb.viewer.screen.DetailScreen
import com.mibcxb.viewer.screen.DetailScreenView
import com.mibcxb.viewer.screen.SettingScreen
import com.mibcxb.viewer.screen.SettingScreenView
import com.mibcxb.widget.compose.file.samba.SmbManager

@Composable
fun NewNav(navController: NavHostController = rememberNavController()) {
    val cacheApi = remember { CacheSqlite() }
    val smbManager = remember { SmbManager() }
    NavHost(
        navController = navController,
        startDestination = BrowseScreen,
        modifier = Modifier.fillMaxSize()
    ) {
        composable<BrowseScreen> {
            BrowseScreenViewNew(cacheApi = cacheApi, smbManager = smbManager, nav = navController)
        }
        composable<SettingScreen> {
            SettingScreenView(cacheApi = cacheApi, smbManager = smbManager, nav = navController)
        }
        composable<DetailScreen> { backStackEntry ->
            val detailScreen = backStackEntry.toRoute<DetailScreen>()
            DetailScreenView(cacheApi = cacheApi, smbManager = smbManager, filepath = detailScreen.filepath, nav = navController)
        }
        composable<ArchiveScreen> { backStackEntry ->
            val archiveScreen = backStackEntry.toRoute<ArchiveScreen>()
            ArchiveScreenView(cacheApi = cacheApi, filepath = archiveScreen.filepath, nav = navController)
        }
    }
}