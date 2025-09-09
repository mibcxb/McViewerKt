package com.mibcxb.viewer.log

import org.slf4j.Logger

interface LogApi {
    val logTag: String
    val logger: Logger
}