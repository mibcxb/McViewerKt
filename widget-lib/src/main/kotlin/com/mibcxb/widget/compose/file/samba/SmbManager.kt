package com.mibcxb.widget.compose.file.samba

import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.SmbConfig
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class SmbManager {
    private val logger = LoggerFactory.getLogger("SmbManager")

    private val client: SMBClient = SMBClient(
        SmbConfig.builder()
            .withTimeout(30, TimeUnit.SECONDS)
            .withSoTimeout(30000)
            .build()
    )

    private val sessions = ConcurrentHashMap<String, SmbSession>()

    val connectedSessions: List<SmbSession>
        get() = sessions.values.filter { it.isConnected }

    val sessionCount: Int
        get() = sessions.size

    fun connect(
        host: String,
        share: String,
        auth: SmbAuth = SmbAuth()
    ): SmbSession {
        val key = "$host:$share"
        val existing = sessions[key]
        if (existing != null && existing.isConnected) {
            return existing
        }

        val session = SmbSession(host, share, client)
        session.connect(auth)
        sessions[key] = session
        return session
    }

    fun disconnect(host: String, share: String) {
        val key = "$host:$share"
        sessions.remove(key)?.disconnect()
    }

    fun disconnectAll() {
        sessions.values.forEach { it.disconnect() }
        sessions.clear()
    }

    fun getSession(host: String, share: String): SmbSession? {
        val key = "$host:$share"
        return sessions[key]?.takeIf { it.isConnected }
    }

    fun getOrConnectSession(
        host: String,
        share: String,
        auth: SmbAuth = SmbAuth()
    ): SmbSession {
        return getSession(host, share) ?: connect(host, share, auth)
    }

    fun close() {
        disconnectAll()
        runCatching { client.close() }
    }
}
