package com.mibcxb.viewer.vm

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.mibcxb.viewer.cache.CacheApi
import com.mibcxb.viewer.cache.CacheSqlite
import com.mibcxb.viewer.cache.SmbConnection
import com.mibcxb.widget.compose.file.samba.SmbAuth
import com.mibcxb.widget.compose.file.samba.SmbManager
import com.mibcxb.widget.compose.file.samba.SmbSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class SmbErrorType {
    EmptyFields,
    ConnectionFailed
}

class SettingViewModel(
    cacheApi: CacheApi = CacheSqlite(),
    private val smbManager: SmbManager
) : AbsViewModel(cacheApi) {

    private val _smbSessions = mutableStateListOf<SmbSession>()
    val smbSessions: SnapshotStateList<SmbSession> get() = _smbSessions

    private val _savedConnections = mutableStateListOf<SmbConnection>()
    val savedConnections: SnapshotStateList<SmbConnection> get() = _savedConnections

    private val _showAddDialog = mutableStateOf(false)
    val showAddDialog: State<Boolean> get() = _showAddDialog

    private val _addHost = mutableStateOf("")
    val addHost: State<String> get() = _addHost

    private val _addShare = mutableStateOf("")
    val addShare: State<String> get() = _addShare

    private val _addDomain = mutableStateOf("")
    val addDomain: State<String> get() = _addDomain

    private val _addUsername = mutableStateOf("")
    val addUsername: State<String> get() = _addUsername

    private val _addPassword = mutableStateOf("")
    val addPassword: State<String> get() = _addPassword

    private val _addAnonymous = mutableStateOf(true)
    val addAnonymous: State<Boolean> get() = _addAnonymous

    private val _addSaveConnection = mutableStateOf(false)
    val addSaveConnection: State<Boolean> get() = _addSaveConnection

    private val _smbError = mutableStateOf<SmbErrorType?>(null)
    val smbError: State<SmbErrorType?> get() = _smbError

    suspend fun loadSavedConnections() {
        val connections = withContext(Dispatchers.IO) {
            cacheApi.obtainAllSmbConnections()
        }
        _savedConnections.clear()
        _savedConnections.addAll(connections)
    }

    fun refreshSessions() {
        _smbSessions.apply {
            clear()
            addAll(smbManager.connectedSessions)
        }
    }

    fun changeAddHost(value: String) {
        _addHost.value = value
    }

    fun changeAddShare(value: String) {
        _addShare.value = value
    }

    fun changeAddDomain(value: String) {
        _addDomain.value = value
    }

    fun changeAddUsername(value: String) {
        _addUsername.value = value
    }

    fun changeAddPassword(value: String) {
        _addPassword.value = value
    }

    fun changeAddAnonymous(value: Boolean) {
        _addAnonymous.value = value
    }

    fun changeAddSaveConnection(value: Boolean) {
        _addSaveConnection.value = value
    }

    fun showAddDialog() {
        _showAddDialog.value = true
        _smbError.value = null
    }

    fun dismissAddDialog() {
        _showAddDialog.value = false
        _smbError.value = null
    }

    suspend fun connectSmb() {
        val host = _addHost.value.trim()
        val share = _addShare.value.trim()
        if (host.isEmpty() || share.isEmpty()) {
            _smbError.value = SmbErrorType.EmptyFields
            return
        }
        val saveConnection = _addSaveConnection.value
        val anonymous = _addAnonymous.value
        val domain = _addDomain.value.trim().ifEmpty { null }
        val username = _addUsername.value.trim().ifEmpty { null }
        val password = _addPassword.value.ifEmpty { null }
        kotlin.runCatching {
            withContext(Dispatchers.IO) {
                val auth = if (anonymous) SmbAuth() else SmbAuth(domain, username, password)
                smbManager.connect(host, share, auth)
                if (saveConnection) {
                    cacheApi.insertSmbConnection(SmbConnection(host, share, domain, username, password))
                }
            }
        }.onSuccess {
            if (saveConnection) {
                // loadSavedConnections will be triggered by the View
            }
            refreshSessions()
            dismissAddDialog()
        }.onFailure {
            _smbError.value = SmbErrorType.ConnectionFailed
        }
    }

    suspend fun connectFromSaved(conn: SmbConnection) {
        kotlin.runCatching {
            withContext(Dispatchers.IO) {
                val auth = if (conn.isAnonymous) SmbAuth()
                else SmbAuth(conn.domain, conn.username, conn.password)
                smbManager.connect(conn.host, conn.share, auth)
            }
        }.onSuccess {
            refreshSessions()
        }.onFailure {
            _smbError.value = SmbErrorType.ConnectionFailed
        }
    }

    suspend fun disconnectSmb(session: SmbSession) {
        withContext(Dispatchers.IO) {
            smbManager.disconnect(session.host, session.share)
        }
        refreshSessions()
    }

    suspend fun deleteSavedConnection(conn: SmbConnection) {
        withContext(Dispatchers.IO) {
            cacheApi.deleteSmbConnection(conn.host, conn.share)
        }
        loadSavedConnections()
    }

    fun clearSmbError() {
        _smbError.value = null
    }
}
