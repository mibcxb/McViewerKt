package com.mibcxb.widget.compose.coil

import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import com.mibcxb.widget.compose.file.ViewerPath
import com.mibcxb.widget.compose.file.samba.SmbManager
import okio.Buffer

class SmbFetcher(
    private val path: String,
    private val remotePath: String,
    private val host: String,
    private val share: String,
    private val smbManager: SmbManager,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val session = smbManager.getSession(host, share) ?: return null
        val inputStream = session.getInputStream(remotePath) ?: return null
        val bytes = inputStream.use { it.readBytes() }
        return SourceFetchResult(
            source = ImageSource(Buffer().write(bytes), options.fileSystem),
            mimeType = "image/*",
            dataSource = DataSource.NETWORK
        )
    }

    class Factory(
        private val smbManager: SmbManager
    ) : Fetcher.Factory<String> {
        override fun create(data: String, options: Options, imageLoader: ImageLoader): Fetcher? {
            val parsed = ViewerPath.parse(data)
            if (parsed.scheme != ViewerPath.Scheme.Samba) return null
            val raw = parsed.basePath.removePrefix("//")
            val parts = raw.split("/").filter { it.isNotEmpty() }
            if (parts.size < 2) return null
            val host = parts[0]
            val share = parts[1]
            val remotePath = parts.drop(2).joinToString("/")
            return SmbFetcher(data, remotePath, host, share, smbManager, options)
        }
    }
}
