package com.mibcxb.widget.compose.coil

import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import com.mibcxb.widget.compose.file.FileStub
import okio.Buffer

class FileStubFetcher(
    private val stub: FileStub,
    private val repo: (FileStub) -> ByteArray?,
    private val mime: (FileStub) -> String?,
    private val options: Options
) : Fetcher {
    override suspend fun fetch(): FetchResult? {
        val dataBytes = repo(stub) ?: return null
        return SourceFetchResult(
            source = ImageSource(Buffer().write(dataBytes), options.fileSystem),
            mimeType = mime(stub),
            dataSource = DataSource.DISK,
        )
    }

    class Factory(
        private val repo: (FileStub) -> ByteArray?,
        private val mime: (FileStub) -> String?
    ) : Fetcher.Factory<FileStub> {
        override fun create(
            data: FileStub,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher {
            return FileStubFetcher(data, repo, mime, options)
        }
    }
}