package com.mibcxb.widget.compose.file

import coil3.key.Keyer
import coil3.request.Options

class FileStubKeyer : Keyer<FileStub> {
    override fun key(data: FileStub, options: Options): String = "filestub:${data.path}"
}