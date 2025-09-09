package com.mibcxb.widget.compose.coil

import coil3.key.Keyer
import coil3.request.Options
import com.mibcxb.widget.compose.file.FileStub

class FileStubKeyer : Keyer<FileStub> {
    override fun key(data: FileStub, options: Options): String = "filestub:${data.path}"
}