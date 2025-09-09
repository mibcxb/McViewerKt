package com.mibcxb.viewer.vm

import androidx.lifecycle.ViewModel
import com.mibcxb.viewer.log.LogApi
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class AbsViewModel : ViewModel(), LogApi {
    override val logTag: String = javaClass.simpleName
    override val logger: Logger = LoggerFactory.getLogger(logTag)
}