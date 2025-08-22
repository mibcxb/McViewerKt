package com.mibcxb.viewer.app

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

val LocalAppRes: ProvidableCompositionLocal<AppRes> = staticCompositionLocalOf { AppResImpl() }