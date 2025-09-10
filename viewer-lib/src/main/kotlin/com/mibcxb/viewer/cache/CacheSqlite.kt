package com.mibcxb.viewer.cache

import com.mibcxb.viewer.McViewer
import com.mibcxb.viewer.log.LogApi
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.security.MessageDigest
import java.sql.Connection
import java.sql.DriverManager

class CacheSqlite : CacheApi, LogApi {
    override val logTag: String = "CacheSqlite"
    override val logger: Logger = LoggerFactory.getLogger(logTag)

    private val connection: Connection

    init {
        val dbPath = File(McViewer.dataRoot,"McViewer.db").absolutePath
        connection = DriverManager.getConnection("jdbc:sqlite:$dbPath")
        createTables()
    }

    private fun createTables() {
        val sql = """
            CREATE TABLE IF NOT EXISTS mcv_image_cache
            (
                mcvic_hash TEXT NOT NULL
                    CONSTRAINT mcv_image_cache_pk
                        PRIMARY KEY
                            ON CONFLICT ROLLBACK,
                mcvic_path TEXT NOT NULL
                    CONSTRAINT mcv_image_cache_pkey
                        UNIQUE
                            ON CONFLICT ROLLBACK,
                mcvic_data BLOB NOT NULL
            );
        """.trimIndent()
        connection.createStatement().execute(sql)
    }

    override fun isPathExists(path: String): Boolean {
        val hash = pathToSha256(path) ?: return false
        val sql = """
            SELECT EXISTS(
                SELECT 1 FROM mcv_image_cache WHERE mcvic_hash = ?
            )
        """.trimIndent()
        return connection.prepareStatement(sql).use { ps ->
            ps.setString(1, hash)
            ps.executeQuery().use { rs ->
                rs.next() && rs.getInt(1) == 1
            }
        }
    }

    override fun insertCacheThumb(path: String, data: ByteArray): Boolean {
        val hash = pathToSha256(path) ?: return false
        val sql = """
            INSERT INTO mcv_image_cache(mcvic_hash, mcvic_path, mcvic_data)
                VALUES (?, ?, ?) ON CONFLICT(mcvic_hash) DO UPDATE SET
                    mcvic_hash = excluded.mcvic_hash,
                    mcvic_path = excluded.mcvic_path,
                    mcvic_data = excluded.mcvic_data
        """.trimIndent()

       val rows = connection.prepareStatement(sql).use { ps ->
            ps.setString(1, hash)
            ps.setString(2, path)
            ps.setBytes(3, data)
            ps.executeUpdate()
        }
        return rows > 0
    }

    override fun obtainCacheThumb(path: String): ByteArray? {
        val hash = pathToSha256(path) ?: return null
        val select = "SELECT mcvic_data FROM mcv_image_cache WHERE mcvic_hash = ?"
        return connection.prepareStatement(select).use { ps ->
            ps.setString(1, hash)
            ps.executeQuery().use { rs ->
                if (rs.next()) rs.getBytes(1) else null
            }
        }
    }

    private fun pathToSha256(path: String): String? {
        if (path.isBlank()) {
            return null
        }
        return kotlin.runCatching {
            val pathBytes = path.toByteArray()
            MessageDigest.getInstance("SHA-256").digest(pathBytes)
                .joinToString("") { "%02x".format(it) }
        }.onFailure { logger.warn(logTag, it.message, it) }.getOrNull()
    }
}