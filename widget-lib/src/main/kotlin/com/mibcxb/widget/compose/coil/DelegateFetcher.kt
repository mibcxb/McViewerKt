package com.mibcxb.widget.compose.coil

import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import okio.Buffer

class DelegateFetcher<Target : Any>(
    private val target: Target,
    private val source: DataSource,
    private val getData: (Target) -> Buffer?,
    private val getMime: (Target) -> String?,
    private val options: Options
): Fetcher {
    override suspend fun fetch(): FetchResult? {
        val buffer = getData(target) ?: return null
        return SourceFetchResult(
            source = ImageSource(buffer, options.fileSystem),
            mimeType = getMime(target),
            dataSource = source,
        )
    }

    class Factory<Target : Any>(
        private val source: DataSource,
        private val getData: (Target) -> Buffer?,
        private val getMime: (Target) -> String?
    ) : Fetcher.Factory<Target> {
        override fun create(
            data: Target,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher {
            return DelegateFetcher(data,source, getData, getMime, options)
        }
    }
}