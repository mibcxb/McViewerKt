package com.mibcxb.viewer.cache

interface CacheApi {
    fun isPathExists(path: String): Boolean

    fun insertCacheThumb(path: String, data: ByteArray): Boolean

    fun obtainCacheThumb(path: String): ByteArray?
}