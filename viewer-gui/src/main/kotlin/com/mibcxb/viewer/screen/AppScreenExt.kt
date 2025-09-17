package com.mibcxb.viewer.screen

import kotlinx.serialization.Serializable

@Serializable
object BrowseScreen

@Serializable
data class DetailScreen(val filepath: String)

@Serializable
data class ArchiveScreen(val filepath: String)