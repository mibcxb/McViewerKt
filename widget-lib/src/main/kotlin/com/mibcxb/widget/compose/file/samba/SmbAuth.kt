package com.mibcxb.widget.compose.file.samba

data class SmbAuth(
    val domain: String? = null,
    val username: String? = null,
    val password: String? = null
) {
    val isAnonymous: Boolean get() = username.isNullOrEmpty()
    val isGuest: Boolean get() = username.equals("guest", ignoreCase = true)
}
