package com.mibcxb.viewer.cache

data class SmbConnection(
    val host: String,
    val share: String,
    val domain: String?,
    val username: String?,
    val password: String?
) {
    val isAnonymous: Boolean get() = username.isNullOrEmpty()
    val key: String get() = "$host:$share"
}
