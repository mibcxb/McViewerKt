package com.mibcxb.viewer.vm

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State

class DetailViewModel : AbsViewModel() {
    private val _filepath = mutableStateOf("")
    val filepath: State<String> get() = _filepath

    fun initFilePath(filepath: String) {
        if (_filepath.value != filepath) {
            _filepath.value = filepath
        }
    }
}