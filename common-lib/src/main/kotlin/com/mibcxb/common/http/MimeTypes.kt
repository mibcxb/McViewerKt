package com.mibcxb.common.http

object MimeTypes {
    const val IMAGE_BMP = "image/bmp"
    const val IMAGE_JPEG = "image/jpeg"
    const val IMAGE_PNG = "image/png"
    const val IMAGE_SVG = "image/svg+xml"
    const val IMAGE_GIF = "image/gif"
    const val IMAGE_WEBP = "image/webp"
    const val IMAGE_TIFF = "image/tiff"

    const val APP_XML = "application/xml"
    const val APP_ZIP = "application/zip"
    const val APP_7Z = "application/x-7z-compressed"

    fun optMimeTypeByExtension(extension: String): String? {
        val normalized = extension.substringAfter('.').lowercase()
        return when (normalized) {
            "bmp" -> IMAGE_BMP
            "jpg", "jpeg" -> IMAGE_JPEG
            "png" -> IMAGE_PNG
            "svg" -> IMAGE_SVG
            "gif" -> IMAGE_GIF
            "webp" -> IMAGE_WEBP
            "tif", "tiff" -> IMAGE_TIFF
            "xml" -> APP_XML
            "zip" -> APP_ZIP
            "7z" -> APP_7Z
            else -> null
        }
    }
}