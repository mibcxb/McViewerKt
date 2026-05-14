# ViewerPath 统一路径结构 — 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 统一本地文件、Samba 远程文件、压缩包内文件的路径表示为 `ViewerPath`，通过 `ViewerSource` 抽象数据源，用 `ViewerItem` 替代现有 `FileItem`/`SmbTreeItem`/`FileStub` 及所有实现。

**Architecture:** `ViewerPath`（不可变路径解析类）→ `ViewerSource`（数据源接口，含 Local/Samba/Archive 实现）→ `ViewerItem`（统一树节点和网格元素，依赖 ViewerPath + ViewerSource）。所有 Coil keyer/fetcher、导航路由、缓存系统通过 `ViewerPath.raw` 字符串保持兼容。

**Tech Stack:** Kotlin 2.3.20, JetBrains Compose 1.9.0, Coil 3, SMBJ, Apache Commons Compress

**Spec:** `docs/superpowers/specs/2026-05-14-viewer-path-design.md`

---

### Task 1: 创建 ViewerPath

**Files:**
- Create: `widget-lib/src/main/kotlin/com/mibcxb/widget/compose/file/ViewerPath.kt`

- [ ] **Step 1: 创建 ViewerPath 类**

写入 `widget-lib/src/main/kotlin/com/mibcxb/widget/compose/file/ViewerPath.kt`：

```kotlin
package com.mibcxb.widget.compose.file

import androidx.compose.runtime.Stable
import java.io.File

@Stable
class ViewerPath private constructor(
    val raw: String,
    val scheme: Scheme,
    val basePath: String,
    val entryPath: String,
    val archiveLayers: Int = 0,
    val name: String,
    val extension: String,
    val fileType: FileType,
    val length: Long,
    val lastModified: Long,
    val createdAt: Long
) {
    enum class Scheme { File, Samba }

    val isArchiveEntry get() = archiveLayers > 0
    val isLocal get() = scheme == Scheme.File
    val isSamba get() = scheme == Scheme.Samba
    val isFile get() = !FileTypes.isDir(fileType)
    val isDirectory get() = FileTypes.isDir(fileType)

    val parentPath: String by lazy {
        if (isArchiveEntry) {
            val lastSep = raw.lastIndexOf("!/")
            if (lastSep >= 0) raw.substring(0, lastSep) else raw
        } else {
            when (scheme) {
                Scheme.File -> File(basePath).parent?.canonicalPath ?: basePath
                Scheme.Samba -> {
                    val idx = basePath.lastIndexOf('/')
                    if (idx > 1) basePath.substring(0, idx) else basePath
                }
            }
        }
    }

    override fun equals(other: Any?): Boolean = other is ViewerPath && raw == other.raw
    override fun hashCode(): Int = raw.hashCode()
    override fun toString(): String = raw

    companion object {
        fun parse(raw: String): ViewerPath {
            val archiveSepIndex = raw.indexOf("!/")

            if (archiveSepIndex >= 0) {
                val base = raw.substring(0, archiveSepIndex)
                val entry = raw.substring(archiveSepIndex + 2)
                // Parse base (which may itself be an archive path)
                val basePath = parse(base)
                val internalName = File(entry).name
                val ext = internalName.substringAfterLast(".", "").lowercase()
                val fileType = if (entry.endsWith("/") || entry.isEmpty()) {
                    FileType.DIR
                } else {
                    FileType.entries.find { it.extensions.contains(ext) } ?: FileType.NAN
                }
                return ViewerPath(
                    raw = raw,
                    scheme = basePath.scheme,
                    basePath = base,
                    entryPath = entry.trimEnd('/'),
                    archiveLayers = basePath.archiveLayers + 1,
                    name = internalName.ifEmpty { entry.trimEnd('/') },
                    extension = ext,
                    fileType = fileType,
                    length = 0L,
                    lastModified = 0L,
                    createdAt = 0L
                )
            }

            when {
                raw.startsWith("//") -> parseSamba(raw)
                raw.length >= 2 && raw[1] == ':' -> parseFile(File(raw))
                raw.startsWith("/") -> parseFile(File(raw))
                else -> parseFile(File(raw))
            }
        }

        private fun parseSamba(raw: String): ViewerPath {
            val cleaned = raw.removePrefix("//")
            val parts = cleaned.split("/", "\\").filter { it.isNotEmpty() }
            val host = parts.getOrElse(0) { "" }
            val share = parts.getOrElse(1) { "" }
            val remotePath = parts.drop(2).joinToString("/")

            val name = if (remotePath.isEmpty()) share else remotePath.substringAfterLast("/")
            val ext = name.substringAfterLast(".", "").lowercase()
            val fileType = FileType.DIR // SMB root level defaults to DIR; caller should override if needed

            return ViewerPath(
                raw = raw,
                scheme = Scheme.Samba,
                basePath = raw,
                entryPath = "",
                archiveLayers = 0,
                name = name,
                extension = ext,
                fileType = fileType,
                length = 0L,
                lastModified = 0L,
                createdAt = 0L
            )
        }

        private fun parseFile(file: File): ViewerPath {
            val canonical = file.canonicalPath
            val isDir = file.isDirectory
            val fileType = if (isDir) FileType.DIR else {
                val ext = file.extension.lowercase()
                FileType.entries.find { it.extensions.contains(ext) } ?: FileType.NAN
            }
            return ViewerPath(
                raw = canonical,
                scheme = Scheme.File,
                basePath = canonical,
                entryPath = "",
                archiveLayers = 0,
                name = file.name.ifEmpty { canonical },
                extension = file.extension.lowercase(),
                fileType = fileType,
                length = if (isDir) 0L else file.length(),
                lastModified = file.lastModified(),
                createdAt = try {
                    java.nio.file.Files.readAttributes(
                        file.toPath(), java.nio.file.attribute.BasicFileAttributes::class.java
                    ).creationTime().toMillis()
                } catch (_: Exception) { file.lastModified() }
            )
        }

        fun create(file: File): ViewerPath = parseFile(file)

        fun create(host: String, share: String, remotePath: String): ViewerPath {
            return parseSamba(buildSmbString(host, share, remotePath))
        }

        fun createArchived(base: ViewerPath, archiveEntryName: String): ViewerPath {
            val raw = "${base.raw}!/$archiveEntryName"
            return parse(raw)
        }

        fun buildSmbString(host: String, share: String, remotePath: String): String {
            val cleanPath = remotePath.trim('/')
            return if (cleanPath.isEmpty()) "//$host/$share" else "//$host/$share/$cleanPath"
        }
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
./gradlew :widget-lib:compileKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交**

```bash
git add widget-lib/src/main/kotlin/com/mibcxb/widget/compose/file/ViewerPath.kt
git commit -m "feat: add ViewerPath unified path class"
```

---

### Task 2: 创建 ViewerSource 接口 + LocalSource

**Files:**
- Create: `widget-lib/src/main/kotlin/com/mibcxb/widget/compose/file/ViewerSource.kt`
- Create: `widget-lib/src/main/kotlin/com/mibcxb/widget/compose/file/LocalSource.kt`

- [ ] **Step 1: 创建 ViewerSource 接口**

写入 `widget-lib/src/main/kotlin/com/mibcxb/widget/compose/file/ViewerSource.kt`：

```kotlin
package com.mibcxb.widget.compose.file

import java.io.InputStream

interface ViewerSource {
    fun listChildren(path: ViewerPath): List<ViewerPath>
    fun getInputStream(path: ViewerPath): InputStream?
    fun exists(path: ViewerPath): Boolean
}
```

- [ ] **Step 2: 创建 LocalSource**

写入 `widget-lib/src/main/kotlin/com/mibcxb/widget/compose/file/LocalSource.kt`：

```kotlin
package com.mibcxb.widget.compose.file

import java.io.File
import java.io.InputStream

class LocalSource : ViewerSource {
    override fun listChildren(path: ViewerPath): List<ViewerPath> {
        val dir = File(path.basePath)
        if (!dir.isDirectory) return emptyList()
        return dir.listFiles()
            ?.filterNot { it.isHidden }
            ?.map { ViewerPath.create(it) }
            ?: emptyList()
    }

    override fun getInputStream(path: ViewerPath): InputStream? {
        val file = File(path.basePath)
        return if (file.isFile && file.canRead()) file.inputStream() else null
    }

    override fun exists(path: ViewerPath): Boolean = File(path.basePath).exists()
}
```

- [ ] **Step 3: 编译验证**

```bash
./gradlew :widget-lib:compileKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 提交**

```bash
git add widget-lib/src/main/kotlin/com/mibcxb/widget/compose/file/ViewerSource.kt widget-lib/src/main/kotlin/com/mibcxb/widget/compose/file/LocalSource.kt
git commit -m "feat: add ViewerSource interface and LocalSource implementation"
```

---

### Task 3: 创建 SambaSource

**Files:**
- Create: `widget-lib/src/main/kotlin/com/mibcxb/widget/compose/file/samba/SambaSource.kt`

- [ ] **Step 1: 创建 SambaSource**

写入 `widget-lib/src/main/kotlin/com/mibcxb/widget/compose/file/samba/SambaSource.kt`：

```kotlin
package com.mibcxb.widget.compose.file.samba

import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.protocol.commons.EnumWithValue
import com.mibcxb.widget.compose.file.ViewerPath
import com.mibcxb.widget.compose.file.ViewerSource
import java.io.InputStream

class SambaSource(private val session: SmbSession) : ViewerSource {
    val host: String get() = session.host
    val share: String get() = session.share

    override fun listChildren(path: ViewerPath): List<ViewerPath> {
        val remotePath = parseRemotePath(path)
        val entries = session.listFiles(remotePath)
        return entries
            .filter { it.fileName != "." && it.fileName != ".." }
            .filter {
                !EnumWithValue.EnumUtils.isSet(it.fileAttributes, FileAttributes.FILE_ATTRIBUTE_HIDDEN)
            }
            .map { entry ->
                val childPath = if (remotePath.isEmpty() || remotePath == "/") {
                    entry.fileName
                } else {
                    "$remotePath/${entry.fileName}"
                }
                ViewerPath.create(host, share, childPath)
            }
    }

    override fun getInputStream(path: ViewerPath): InputStream? {
        val remotePath = parseRemotePath(path)
        return session.getInputStream(remotePath)
    }

    override fun exists(path: ViewerPath): Boolean {
        val remotePath = parseRemotePath(path)
        return if (remotePath.isEmpty()) session.isConnected
        else session.exists(remotePath)
    }

    private fun parseRemotePath(path: ViewerPath): String {
        val raw = path.basePath
        val prefix = "//$host/$share"
        return raw.removePrefix(prefix).trim('/')
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
./gradlew :widget-lib:compileKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交**

```bash
git add widget-lib/src/main/kotlin/com/mibcxb/widget/compose/file/samba/SambaSource.kt
git commit -m "feat: add SambaSource ViewerSource implementation"
```

---

### Task 4: 更新 ArchiveAccessor 体系以适应 ViewerPath

**Files:**
- Modify: `widget-lib/src/main/kotlin/com/mibcxb/widget/compose/file/archive/ArchiveAccessor.kt`
- Modify: `widget-lib/src/main/kotlin/com/mibcxb/widget/compose/file/archive/ArchiveAccessorFactory.kt`
- Modify: `widget-lib/src/main/kotlin/com/mibcxb/widget/compose/file/archive/ZipFileAccessor.kt`
- Modify: `widget-lib/src/main/kotlin/com/mibcxb/widget/compose/file/archive/SevenZAccessor.kt`

- [ ] **Step 1: 修改 ArchiveAccessor — 接受 String 而非 FileStub**

修改 `widget-lib/src/main/kotlin/com/mibcxb/widget/compose/file/archive/ArchiveAccessor.kt`：

将构造函数从 `FileStub` 改为 `String`，因为只需要路径：

```kotlin
package com.mibcxb.widget.compose.file.archive

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.InputStream

abstract class ArchiveAccessor(val filePath: String) {
    protected val logTag: String = javaClass.simpleName
    protected val logger: Logger = LoggerFactory.getLogger(logTag)

    abstract fun prepare()
    abstract fun release()

    abstract fun getEntryList(filter: (ArchiveEntryStub) -> Boolean = { true }): List<ArchiveEntryStub>

    abstract fun getInputStream(stub: ArchiveEntryStub): InputStream?
}
```

- [ ] **Step 2: 修改 ArchiveAccessorFactory**

修改 `widget-lib/src/main/kotlin/com/mibcxb/widget/compose/file/archive/ArchiveAccessorFactory.kt`：

```kotlin
package com.mibcxb.widget.compose.file.archive

import com.mibcxb.widget.compose.file.FileType

fun interface ArchiveAccessorFactory {
    fun createArchiveAccessor(fileType: FileType, filePath: String): ArchiveAccessor?
}
```

- [ ] **Step 3: 修改 ZipFileAccessor — 使用 String 路径**

修改 `widget-lib/src/main/kotlin/com/mibcxb/widget/compose/file/archive/ZipFileAccessor.kt`：

```kotlin
package com.mibcxb.widget.compose.file.archive

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.commons.io.IOUtils
import java.io.InputStream

class ZipFileAccessor(filePath: String) : ArchiveAccessor(filePath) {
    private var _zipFile: ZipFile? = null

    override fun prepare() {
        _zipFile = runCatching {
            ZipFile.Builder().setFile(filePath).get()
        }.onFailure { logger.warn(logTag, it.message, it) }.getOrNull()
    }

    override fun release() {
        IOUtils.closeQuietly(_zipFile)
        _zipFile = null
    }

    override fun getEntryList(filter: (ArchiveEntryStub) -> Boolean): List<ArchiveEntryStub> {
        val zipFile = _zipFile ?: return emptyList()
        val subList = mutableListOf<ArchiveEntryStub>()
        val entries = zipFile.entries
        while (entries.hasMoreElements()) {
            val zipEntry = entries.nextElement()
            val zipEntryStub = ZipEntryStub(zipEntry)
            if (filter(zipEntryStub)) {
                subList.add(zipEntryStub)
            }
        }
        return subList
    }

    override fun getInputStream(stub: ArchiveEntryStub): InputStream? {
        val zipFile = _zipFile
        if (zipFile != null) {
            val archiveEntry = stub.archiveEntry
            if (archiveEntry is ZipArchiveEntry) {
                return zipFile.getInputStream(archiveEntry)
            }
        }
        return null
    }
}
```

- [ ] **Step 4: 修改 SevenZAccessor — 使用 String 路径**

修改 `widget-lib/src/main/kotlin/com/mibcxb/widget/compose/file/archive/SevenZAccessor.kt`：

```kotlin
package com.mibcxb.widget.compose.file.archive

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.io.IOUtils
import java.io.InputStream

class SevenZAccessor(filePath: String) : ArchiveAccessor(filePath) {

    private var _sevenZFile: SevenZFile? = null

    override fun prepare() {
        _sevenZFile = runCatching {
            SevenZFile.Builder().setFile(filePath).get()
        }.onFailure { logger.warn(logTag, it.message, it) }.getOrNull()
    }

    override fun release() {
        IOUtils.closeQuietly(_sevenZFile)
        _sevenZFile = null
    }

    override fun getEntryList(filter: (ArchiveEntryStub) -> Boolean): List<ArchiveEntryStub> {
        val sevenZFile = _sevenZFile ?: return emptyList()
        val subList = mutableListOf<ArchiveEntryStub>()
        val iterator = sevenZFile.entries.iterator()
        while (iterator.hasNext()) {
            val sevenZEntry = iterator.next()
            val sevenZEntryStub = SevenZEntryStub(sevenZEntry)
            if (filter(sevenZEntryStub)) {
                subList.add(sevenZEntryStub)
            }
        }
        return subList
    }

    override fun getInputStream(stub: ArchiveEntryStub): InputStream? {
        val sevenZFile = _sevenZFile
        if (sevenZFile != null) {
            val archiveEntry = stub.archiveEntry
            if (archiveEntry is SevenZArchiveEntry) {
                return sevenZFile.getInputStream(archiveEntry)
            }
        }
        return null
    }
}
```

- [ ] **Step 5: 编译验证**

```bash
./gradlew :widget-lib:compileKotlin
```

Expected: 可能有 ArchiveViewModel 的编译错误（引用了已变更的 ArchiveAccessor 构造函数）。这是预期的——后续任务会修复。

- [ ] **Step 6: 提交**

```bash
git add widget-lib/src/main/kotlin/com/mibcxb/widget/compose/file/archive/ArchiveAccessor.kt widget-lib/src/main/kotlin/com/mibcxb/widget/compose/file/archive/ArchiveAccessorFactory.kt widget-lib/src/main/kotlin/com/mibcxb/widget/compose/file/archive/ZipFileAccessor.kt widget-lib/src/main/kotlin/com/mibcxb/widget/compose/file/archive/SevenZAccessor.kt
git commit -m "refactor: change ArchiveAccessor to accept String path instead of FileStub"
```

---

### Task 5: 创建 ArchiveSource

**Files:**
- Create: `widget-lib/src/main/kotlin/com/mibcxb/widget/compose/file/archive/ArchiveSource.kt`

- [ ] **Step 1: 创建 ArchiveSource**

写入 `widget-lib/src/main/kotlin/com/mibcxb/widget/compose/file/archive/ArchiveSource.kt`：

```kotlin
package com.mibcxb.widget.compose.file.archive

import com.mibcxb.widget.compose.file.FileType
import com.mibcxb.widget.compose.file.ViewerPath
import com.mibcxb.widget.compose.file.ViewerSource
import java.io.InputStream

class ArchiveSource(
    private val accessor: ArchiveAccessor,
    private val factory: ArchiveAccessorFactory = ArchiveAccessorFactory { _, _ -> null }
) : ViewerSource {

    override fun listChildren(path: ViewerPath): List<ViewerPath> {
        val entries = accessor.getEntryList()
        return entries.map { entry ->
            ViewerPath.createArchived(path, entry.archiveEntry.name)
        }
    }

    override fun getInputStream(path: ViewerPath): InputStream? {
        val stub = findEntry(path.entryPath) ?: return null
        return accessor.getInputStream(stub)
    }

    override fun exists(path: ViewerPath): Boolean {
        return findEntry(path.entryPath) != null
    }

    private fun findEntry(entryPath: String): ArchiveEntryStub? {
        return accessor.getEntryList().find { it.archiveEntry.name == entryPath }
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
./gradlew :widget-lib:compileKotlin
```

Expected: BUILD SUCCESSFUL (ArchiveViewModel errors are from previous step, not this one)

- [ ] **Step 3: 提交**

```bash
git add widget-lib/src/main/kotlin/com/mibcxb/widget/compose/file/archive/ArchiveSource.kt
git commit -m "feat: add ArchiveSource ViewerSource implementation"
```

---

### Task 6: 创建 ViewerItem

**Files:**
- Create: `widget-lib/src/main/kotlin/com/mibcxb/widget/compose/file/ViewerItem.kt`

- [ ] **Step 1: 创建 ViewerItem**

写入 `widget-lib/src/main/kotlin/com/mibcxb/widget/compose/file/ViewerItem.kt`：

```kotlin
package com.mibcxb.widget.compose.file

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.mibcxb.widget.compose.tree.Expandable
import com.mibcxb.widget.compose.tree.Selectable
import com.mibcxb.widget.compose.tree.TreeItem
import java.io.InputStream

typealias ViewerItemFilter = (ViewerItem) -> Boolean

@Stable
class ViewerItem(
    val path: ViewerPath,
    internal var source: ViewerSource? = null
) : TreeItem, Expandable, Selectable {

    override val id: String get() = path.raw
    override val name: String get() = path.name
    override val children: SnapshotStateList<ViewerItem> = mutableStateListOf()

    // Tree
    private var _expanded by mutableStateOf(false)
    override val expanded: Boolean get() = _expanded

    private var _selected by mutableStateOf(false)
    override val selected: Boolean get() = _selected

    fun setExpanded(enabled: Boolean) {
        if (_expanded != enabled) {
            _expanded = enabled
        }
    }

    fun setSelected(enabled: Boolean) {
        if (_selected != enabled) {
            _selected = enabled
        }
    }

    // Grid delegates
    val extension: String get() = path.extension
    val fileType: FileType get() = path.fileType
    val length: Long get() = path.length
    val lastModified: Long get() = path.lastModified
    val createdAt: Long get() = path.createdAt
    val isDirectory get() = path.isDirectory
    val isFile get() = path.isFile
    val isLocal get() = path.isLocal
    val isSamba get() = path.isSamba
    val isArchiveEntry get() = path.isArchiveEntry
    val isImage get() = FileTypes.isImage(path.fileType)
    val isArchive get() = FileTypes.isArchive(path.fileType)

    fun refreshList(filter: ViewerItemFilter = { true }) {
        source?.let { src ->
            val paths = src.listChildren(path)
            children.clear()
            children.addAll(paths.filter(filter).map { ViewerItem(it, source) })
        }
    }

    fun getInputStream(): InputStream? = source?.getInputStream(path)

    fun exists(): Boolean = source?.exists(path) ?: false
}
```

- [ ] **Step 2: 编译验证**

```bash
./gradlew :widget-lib:compileKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交**

```bash
git add widget-lib/src/main/kotlin/com/mibcxb/widget/compose/file/ViewerItem.kt
git commit -m "feat: add ViewerItem - unified tree node and grid element"
```

---

### Task 7: 更新 FileTree 泛型为 ViewerItem

**Files:**
- Modify: `widget-lib/src/main/kotlin/com/mibcxb/widget/compose/tree/FileTree.kt`

- [ ] **Step 1: 修改 FileTree**

将 `FileItem` 替换为 `ViewerItem`：

```kotlin
package com.mibcxb.widget.compose.tree

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.mibcxb.widget.compose.file.ViewerItem

@Stable
class FileTree(branches: List<ViewerItem> = emptyList()) {
    val branches: SnapshotStateList<ViewerItem> = mutableStateListOf<ViewerItem>().apply { addAll(branches) }
}
```

- [ ] **Step 2: 编译验证**

```bash
./gradlew :widget-lib:compileKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交**

```bash
git add widget-lib/src/main/kotlin/com/mibcxb/widget/compose/tree/FileTree.kt
git commit -m "refactor: update FileTree to use ViewerItem"
```

---

### Task 8: 创建 ViewerItemKeyer 并更新 SmbFetcher

**Files:**
- Create: `widget-lib/src/main/kotlin/com/mibcxb/widget/compose/file/ViewerItemKeyer.kt`
- Modify: `widget-lib/src/main/kotlin/com/mibcxb/widget/compose/coil/SmbFetcher.kt`

- [ ] **Step 1: 创建 ViewerItemKeyer**

写入 `widget-lib/src/main/kotlin/com/mibcxb/widget/compose/file/ViewerItemKeyer.kt`：

```kotlin
package com.mibcxb.widget.compose.file

import coil3.key.Keyer
import coil3.request.Options

class ViewerItemKeyer : Keyer<ViewerItem> {
    override fun key(data: ViewerItem, options: Options): String = "viewer:${data.path.raw}"
}
```

- [ ] **Step 2: 更新 SmbFetcher — 使用 ViewerPath 判断**

修改 `widget-lib/src/main/kotlin/com/mibcxb/widget/compose/coil/SmbFetcher.kt`，用 `ViewerPath.parse()` 替代 `SmbFileStub.parseSmbPath()`：

```kotlin
package com.mibcxb.widget.compose.coil

import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import com.mibcxb.widget.compose.file.ViewerPath
import com.mibcxb.widget.compose.file.samba.SmbManager
import okio.Buffer

class SmbFetcher(
    private val path: String,
    private val remotePath: String,
    private val host: String,
    private val share: String,
    private val smbManager: SmbManager,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val session = smbManager.getSession(host, share) ?: return null
        val inputStream = session.getInputStream(remotePath) ?: return null
        val bytes = inputStream.use { it.readBytes() }
        return SourceFetchResult(
            source = ImageSource(Buffer().write(bytes), options.fileSystem),
            mimeType = "image/*",
            dataSource = DataSource.NETWORK
        )
    }

    class Factory(
        private val smbManager: SmbManager
    ) : Fetcher.Factory<String> {
        override fun create(data: String, options: Options, imageLoader: ImageLoader): Fetcher? {
            val parsed = ViewerPath.parse(data)
            if (parsed.scheme != ViewerPath.Scheme.Samba) return null
            val raw = parsed.basePath.removePrefix("//")
            val parts = raw.split("/").filter { it.isNotEmpty() }
            if (parts.size < 2) return null
            val host = parts[0]
            val share = parts[1]
            val remotePath = parts.drop(2).joinToString("/")
            return SmbFetcher(data, remotePath, host, share, smbManager, options)
        }
    }
}
```

- [ ] **Step 3: 编译验证**

```bash
./gradlew :widget-lib:compileKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 提交**

```bash
git add widget-lib/src/main/kotlin/com/mibcxb/widget/compose/file/ViewerItemKeyer.kt widget-lib/src/main/kotlin/com/mibcxb/widget/compose/coil/SmbFetcher.kt
git commit -m "feat: add ViewerItemKeyer, update SmbFetcher to use ViewerPath"
```

---

### Task 9: 更新 FileGridView 使用 ViewerItem

**Files:**
- Modify: `widget-lib/src/main/kotlin/com/mibcxb/widget/compose/grid/FileGridView.kt`

- [ ] **Step 1: 修改 FileGridView 签名和实现**

将 `FileStub` 替换为 `ViewerItem`：

```kotlin
package com.mibcxb.widget.compose.grid

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.decode.DataSource
import com.mibcxb.widget.compose.coil.DelegateFetcher
import com.mibcxb.widget.compose.file.ViewerItem
import com.mibcxb.widget.compose.file.ViewerItemFilter
import com.mibcxb.widget.compose.file.ViewerItemKeyer
import com.mibcxb.widget.compose.file.FileType
import com.mibcxb.widget.widget_lib.generated.resources.Res
import com.mibcxb.widget.widget_lib.generated.resources.file_unknown
import com.mibcxb.widget.widget_lib.generated.resources.folder_normal
import okio.Buffer
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

enum class FileSortType {
    Filename, FileLength, CreateTime, LastModified
}

enum class FileGridSize {
    Small, Middle, Large
}

@Composable
fun FileGridView(
    item: ViewerItem,
    modifier: Modifier = Modifier,
    sortType: FileSortType = FileSortType.Filename,
    itemSize: FileGridSize = FileGridSize.Middle,
    onSingleClick: (ViewerItem) -> Unit = {},
    onDoubleClick: (ViewerItem) -> Unit = {},
    cacheLoader: (ViewerItem) -> Buffer? = { null },
    errorLoader: (ViewerItem) -> DrawableResource? = { null },
    imageLoader: (ViewerItem) -> DrawableResource? = { null },
    fileFilter: ViewerItemFilter = { true }
) {
    if (item.fileType != FileType.DIR) {
        return
    }
    val itemWidth = when(itemSize) {
        FileGridSize.Small -> 96.dp
        FileGridSize.Large -> 144.dp
        else -> 120.dp
    }
    val itemHeight = when(itemSize) {
        FileGridSize.Small -> 128.dp
        FileGridSize.Large -> 192.dp
        else -> 160.dp
    }
    val fontSize = when(itemSize) {
        FileGridSize.Small -> 14.sp
        FileGridSize.Large -> 16.sp
        else -> 16.sp
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(itemWidth),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
    ) {
        val sortedFiles = item.children.filter(fileFilter).sortedWith(
            compareBy<ViewerItem> { !it.isDirectory }.thenComparator { a, b ->
                when (sortType) {
                    FileSortType.Filename -> a.name.compareTo(b.name, ignoreCase = true)
                    FileSortType.FileLength -> a.length.compareTo(b.length)
                    FileSortType.CreateTime -> a.createdAt.compareTo(b.createdAt)
                    FileSortType.LastModified -> a.lastModified.compareTo(b.lastModified)
                }
            }
        )
        items(sortedFiles, key = { it.id }) { fileItem ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(itemWidth, itemHeight)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                    .combinedClickable(
                        onClick = { onSingleClick(fileItem) },
                        onDoubleClick = { onDoubleClick(fileItem) })
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    val fallback = errorLoader(fileItem)
                        ?: if (item.isDirectory) Res.drawable.folder_normal else Res.drawable.file_unknown
                    val platformContext = LocalPlatformContext.current
                    Box(modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f)) {
                        AsyncImage(
                            model = fileItem,
                            contentDescription = null,
                            error = painterResource(fallback),
                            placeholder = painterResource(fallback),
                            contentScale = ContentScale.Fit,
                            imageLoader = ImageLoader.Builder(platformContext).components {
                                add(ViewerItemKeyer())
                                add(
                                    DelegateFetcher.Factory(
                                        source = DataSource.DISK,
                                        getData = cacheLoader,
                                        getMime = { "image/png" }
                                    ))
                            }.build(),
                            modifier = Modifier.fillMaxSize()
                        )
                        val fileTypeImage = imageLoader(fileItem)
                        if (fileTypeImage != null) {
                            Image(
                                painterResource(fileTypeImage),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp).align(Alignment.BottomEnd)
                            )
                        }
                    }
                    Text(
                        fileItem.name,
                        fontSize = fontSize,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp).fillMaxWidth().weight(1f)
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 2: 编译验证** (widget-lib)

```bash
./gradlew :widget-lib:compileKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交**

```bash
git add widget-lib/src/main/kotlin/com/mibcxb/widget/compose/grid/FileGridView.kt
git commit -m "refactor: update FileGridView to use ViewerItem instead of FileStub"
```

---

### Task 10: 更新 TreeView 的 FileTreeView 适配 ViewerItem

**Files:**
- Modify: `widget-lib/src/main/kotlin/com/mibcxb/widget/compose/tree/TreeView.kt`

- [ ] **Step 1: 修改 FileTreeView composable**

将 `FileItem` 替换为 `ViewerItem`：

```kotlin
package com.mibcxb.widget.compose.tree

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mibcxb.widget.compose.file.FileType
import com.mibcxb.widget.compose.file.ViewerItem
import com.mibcxb.widget.widget_lib.generated.resources.Res
import com.mibcxb.widget.widget_lib.generated.resources.file
import com.mibcxb.widget.widget_lib.generated.resources.folder_normal
import com.mibcxb.widget.widget_lib.generated.resources.folder_opened
import com.mibcxb.widget.widget_lib.generated.resources.image
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun <T : TreeItem> TreeView(
    rootList: List<T>,
    modifier: Modifier = Modifier,
    onSingleClick: (T) -> Unit = {},
    onDoubleClick: (T) -> Unit = {},
    iconLoader: ((T) -> DrawableResource?)? = null
) {
    val vScroll = rememberLazyListState()
    val hScroll = rememberScrollState()
    Box(modifier = modifier) {
        LazyColumn(
            state = vScroll,
            contentPadding = PaddingValues(vertical = 8.dp),
            modifier = modifier.fillMaxSize().horizontalScroll(hScroll)
        ) {
            items(rootList, key = { it.id }) {
                TreeNodeView(it, 0, onSingleClick, onDoubleClick, iconLoader)
            }
        }
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(vScroll),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
        )
        HorizontalScrollbar(
            adapter = rememberScrollbarAdapter(hScroll),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        )
    }
}

@Composable
private fun <T : TreeItem> TreeNodeView(
    nodeItem: T,
    level: Int = 0,
    onSingleClick: (T) -> Unit = {},
    onDoubleClick: (T) -> Unit = {},
    iconLoader: ((T) -> DrawableResource?)? = null
) {
    val whiteStart = 16.dp * level
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = whiteStart).heightIn(min = 16.dp, max = 24.dp).combinedClickable(
            onClick = { onSingleClick(nodeItem) },
            onDoubleClick = { onDoubleClick(nodeItem) })
    ) {
        val iconSize = 20.dp
        val hPadding = 8.dp
        val drawable: DrawableResource? = iconLoader?.invoke(nodeItem)
        if (drawable != null) {
            Icon(
                painterResource(drawable),
                contentDescription = null,
                modifier = Modifier.padding(start = hPadding, end = hPadding / 2).size(iconSize)
            )
        } else {
            Spacer(modifier = Modifier.padding(start = hPadding, end = hPadding / 2).size(iconSize))
        }
        Text(nodeItem.name, modifier = Modifier.padding(end = hPadding))
    }
    if (nodeItem is Expandable) {
        if (nodeItem.expanded) {
            nodeItem.children.forEach {
                TreeNodeView(it as T, level + 1, onSingleClick, onDoubleClick, iconLoader)
            }
        }
    }
}

@Composable
fun FileTreeView(
    fileTree: FileTree,
    modifier: Modifier = Modifier,
    onSingleClick: (ViewerItem) -> Unit = {},
    onDoubleClick: (ViewerItem) -> Unit = {},
    iconLoader: ((ViewerItem) -> DrawableResource?)? = {
        when (it.fileType) {
            FileType.NAN -> null
            FileType.DIR -> if (it.expanded) Res.drawable.folder_opened else Res.drawable.folder_normal
            else -> Res.drawable.file
        }
    }
) {
    TreeView(
        rootList = fileTree.branches,
        modifier = modifier,
        onSingleClick = onSingleClick,
        onDoubleClick = onDoubleClick
    ) { item ->
        if (iconLoader != null) {
            iconLoader(item)
        } else {
            when (item.fileType) {
                FileType.DIR -> if (item.expanded) Res.drawable.folder_opened else Res.drawable.folder_normal
                FileType.JPG -> Res.drawable.image
                FileType.PNG -> Res.drawable.image
                else -> null
            }
        }
    }
}
```

注意：`TreeView<T : TreeItem>` 泛型不变，无需修改。`FileTreeView` 的 `iconLoader` 从 `FileItem` 改为 `ViewerItem`。

- [ ] **Step 2: 编译验证**

```bash
./gradlew :widget-lib:compileKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交**

```bash
git add widget-lib/src/main/kotlin/com/mibcxb/widget/compose/tree/TreeView.kt
git commit -m "refactor: update FileTreeView to use ViewerItem"
```

---

### Task 11: 更新 BrowseViewModel

**Files:**
- Modify: `viewer-gui/src/main/kotlin/com/mibcxb/viewer/vm/BrowseViewModel.kt`

- [ ] **Step 1: 重写 BrowseViewModel**

将 `FileItem`、`SmbTreeItem`、`FileStub` 全部替换为 `ViewerItem`，`ViewerPath` 统一路径处理：

```kotlin
package com.mibcxb.viewer.vm

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.lifecycle.viewModelScope
import com.mibcxb.common.skia.SkiaUtils
import com.mibcxb.viewer.cache.CacheApi
import com.mibcxb.viewer.cache.CacheSqlite
import com.mibcxb.widget.compose.file.FileType
import com.mibcxb.widget.compose.file.FileTypes
import com.mibcxb.widget.compose.file.LocalSource
import com.mibcxb.widget.compose.file.ViewerItem
import com.mibcxb.widget.compose.file.ViewerItemFilter
import com.mibcxb.widget.compose.file.ViewerPath
import com.mibcxb.widget.compose.file.samba.SambaSource
import com.mibcxb.widget.compose.file.samba.SmbManager
import com.mibcxb.widget.compose.grid.FileGridSize
import com.mibcxb.widget.compose.grid.FileSortType
import com.mibcxb.widget.compose.tree.FileTree
import com.mibcxb.widget.compose.tree.TreeItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okio.Buffer
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.SamplingMode
import java.io.File

class BrowseViewModel(
    cacheApi: CacheApi = CacheSqlite(),
    private val smbManager: SmbManager
) : AbsViewModel(cacheApi) {
    private val _treeRoots = mutableStateListOf<ViewerItem>()
    val treeRoots: SnapshotStateList<ViewerItem> get() = _treeRoots

    val fileTree: FileTree
        get() = FileTree(_treeRoots.toList())

    private val _currentItem = mutableStateOf(emptyViewerItem)
    val currentItem: State<ViewerItem> get() = _currentItem

    private val _currentPath = mutableStateOf("")
    val currentPath: State<String> get() = _currentPath

    private val _previewImageItem = mutableStateOf(emptyViewerItem)
    val previewImageItem: State<ViewerItem> get() = _previewImageItem

    private val _fileTypeList = mutableStateListOf(*(FileTypes.images + FileTypes.archives))
    val fileTypeList: SnapshotStateList<FileType> get() = _fileTypeList

    private val _searchName = mutableStateOf("")
    val searchName: State<String> get() = _searchName

    private val _fileSortType = mutableStateOf(FileSortType.Filename)
    val fileSortType: State<FileSortType> get() = _fileSortType

    private val _fileGridSize = mutableStateOf(FileGridSize.Middle)
    val fileGridSize: State<FileGridSize> get() = _fileGridSize

    private val fileFilter: ViewerItemFilter = { item ->
        val extNames = fileTypeList.flatMap { it.extensions.toList() }
        when {
            item.isDirectory -> true
            item.isFile -> extNames.contains(item.extension)
            else -> false
        }
    }

    private val _selectedTreeItem = mutableStateOf<ViewerItem?>(null)
    val selectedTreeItem: State<ViewerItem?> get() = _selectedTreeItem

    private var treeInitializing = false

    companion object {
        private val emptyViewerItem = ViewerItem(ViewerPath.parse(""))
    }

    fun initFileTree() {
        if (treeInitializing) return
        if (_treeRoots.isNotEmpty()) {
            syncSmbTree()
            return
        }
        treeInitializing = true
        viewModelScope.launch(Dispatchers.IO) {
            if (_treeRoots.isNotEmpty()) return@launch
            val localSource = LocalSource()
            val localRoots = File.listRoots()
                .filter { it.canRead() }
                .map { ViewerItem(ViewerPath.create(it), localSource) }
            _treeRoots.addAll(localRoots)
            val firstLocal = localRoots.firstOrNull()
            if (firstLocal != null) {
                _selectedTreeItem.value = firstLocal
                _currentItem.value = firstLocal.apply { refreshList(fileFilter) }
                _currentPath.value = firstLocal.path.raw
            }
            syncSmbTree()
            treeInitializing = false
        }
    }

    fun syncSmbTree() {
        val smbSessions = smbManager.connectedSessions
        val currentKeys = smbSessions.map { "${it.host}:${it.share}" }.toSet()

        val existingSmbRoots = _treeRoots.filter { it.isSamba }
        val toRemove = existingSmbRoots.filter {
            val parsed = ViewerPath.parse(it.id)
            val raw = parsed.basePath.removePrefix("//")
            val parts = raw.split("/").filter { s -> s.isNotEmpty() }
            val key = if (parts.size >= 2) "${parts[0]}:${parts[1]}" else ""
            key !in currentKeys
        }
        if (toRemove.isNotEmpty()) {
            _treeRoots.removeAll(toRemove)
            val curItem = _currentItem.value
            if (curItem.isSamba) {
                val parsed = ViewerPath.parse(curItem.id)
                val raw = parsed.basePath.removePrefix("//")
                val parts = raw.split("/").filter { s -> s.isNotEmpty() }
                val key = if (parts.size >= 2) "${parts[0]}:${parts[1]}" else ""
                if (key !in currentKeys) {
                    resetToFirstLocalRoot()
                }
            }
        }

        val existingKeys = _treeRoots.filter { it.isSamba }
            .map {
                val parsed = ViewerPath.parse(it.id)
                val raw = parsed.basePath.removePrefix("//")
                val parts = raw.split("/").filter { s -> s.isNotEmpty() }
                if (parts.size >= 2) "${parts[0]}:${parts[1]}" else ""
            }.toSet()

        for (session in smbSessions) {
            val key = "${session.host}:${session.share}"
            if (key !in existingKeys) {
                val source = SambaSource(session)
                val path = ViewerPath.create(session.host, session.share, "")
                val item = ViewerItem(
                    path = ViewerPath(
                        raw = path.raw,
                        scheme = path.scheme,
                        basePath = path.basePath,
                        entryPath = "",
                        archiveLayers = 0,
                        name = "${session.share}@${session.host}",
                        extension = "",
                        fileType = FileType.DIR,
                        length = 0L,
                        lastModified = 0L,
                        createdAt = 0L
                    ),
                    source = source
                )
                _treeRoots.add(item)
            }
        }
    }

    private fun resetToFirstLocalRoot() {
        val firstLocal = _treeRoots.firstOrNull { it.isLocal } ?: return
        _selectedTreeItem.value = firstLocal
        _currentItem.value = firstLocal.apply { refreshList(fileFilter) }
        _currentPath.value = firstLocal.path.raw
    }

    fun singleClickTreeItem(item: ViewerItem) {
        if (_selectedTreeItem.value != item) {
            _selectedTreeItem.value = item
        }
        changeCurrentItem(item.path.basePath)
    }

    fun doubleClickTreeItem(item: ViewerItem) {
        item.setExpanded(!item.expanded)
        if (item.expanded) {
            item.refreshList(fileFilter)
            // also open directory in grid
            changeCurrentItem(item.path.raw)
        }
    }

    fun singleClickGridItem(item: ViewerItem) {
        if (item.isLocal) {
            _previewImageItem.value = item
        }
    }

    fun doubleClickGridItem(item: ViewerItem) {
        if (item.isDirectory) {
            _currentItem.value = item.apply { refreshList(fileFilter) }
            _currentPath.value = item.path.raw
        }
    }

    fun changeFilePath(newPath: String) {
        if (_currentPath.value != newPath) {
            _currentPath.value = newPath
        }
    }

    fun changeSearchName(newName: String) {
        if (_searchName.value != newName) {
            _searchName.value = newName
        }
    }

    fun changeFileSortType(index: Int) {
        val sortType = FileSortType.entries.getOrNull(index) ?: return
        changeFileSortType(sortType)
    }

    fun changeFileSortType(newType: FileSortType) {
        if (_fileSortType.value != newType) {
            _fileSortType.value = newType
        }
    }

    fun changeFileGridSize(index: Int) {
        val sizeList = FileGridSize.entries.toList()
        if (index in sizeList.indices) {
            changeFileGridSize(sizeList[index])
        }
    }

    fun changeFileGridSize(newSize: FileGridSize) {
        if (_fileGridSize.value != newSize) {
            _fileGridSize.value = newSize
        }
    }

    fun removeSearchName() {
        changeSearchName("")
    }

    fun goToTargetPath() {
        goToTargetPath(_currentPath.value)
    }

    fun goToTargetPath(newPath: String) {
        if (newPath.isNotBlank()) {
            changeCurrentItem(newPath)
        }
    }

    fun goToParentPath() {
        val curItem = _currentItem.value
        val parentRaw = curItem.path.parentPath
        if (parentRaw != curItem.path.raw) {
            changeCurrentItem(parentRaw)
        }
    }

    fun refreshCurrent() {
        _currentItem.value.refreshList(fileFilter)
    }

    private fun changeCurrentItem(pathStr: String) {
        val vp = ViewerPath.parse(pathStr)
        when (vp.scheme) {
            ViewerPath.Scheme.File -> changeFileItem(vp)
            ViewerPath.Scheme.Samba -> changeSambaItem(vp)
        }
    }

    private fun changeFileItem(vp: ViewerPath) {
        val dir = File(vp.basePath)
        if (!dir.exists() || !dir.isDirectory) return
        if (_currentItem.value.path.raw == vp.raw) return
        val source = LocalSource()
        _currentItem.value = ViewerItem(vp, source).apply { refreshList(fileFilter) }
        _currentPath.value = vp.raw
    }

    private fun changeSambaItem(vp: ViewerPath) {
        val raw = vp.basePath.removePrefix("//")
        val parts = raw.split("/").filter { it.isNotEmpty() }
        if (parts.size < 2) return
        val host = parts[0]
        val share = parts[1]
        val remotePath = parts.drop(2).joinToString("/")
        val session = smbManager.getSession(host, share) ?: return
        val source = SambaSource(session)
        val fullPath = ViewerPath.create(host, share, remotePath)
        _currentItem.value = ViewerItem(fullPath, source).apply { refreshList(fileFilter) }
        _currentPath.value = fullPath.raw
    }
}
```

注意：现有 `AbsViewModel` 中的 `getThumbBuffer` 仍使用 `FileStub`，后续 Task 修正。

- [ ] **Step 2: 编译验证**（此时会遇到 AbsViewModel 和 BrowseScreenViewNew 的编译错误——预期中的）

```bash
./gradlew :viewer-gui:compileKotlin
```

Expected: 编译错误（BrowseScreenViewNew 引用旧类型，AbsViewModel 使用 FileStub）

- [ ] **Step 3: 提交**

```bash
git add viewer-gui/src/main/kotlin/com/mibcxb/viewer/vm/BrowseViewModel.kt
git commit -m "refactor: update BrowseViewModel to use ViewerItem and ViewerPath"
```

---

### Task 12: 更新 AbsViewModel

**Files:**
- Modify: `viewer-gui/src/main/kotlin/com/mibcxb/viewer/vm/AbsViewModel.kt`

- [ ] **Step 1: 将 getThumbBuffer 改为接受 ViewerItem**

```kotlin
package com.mibcxb.viewer.vm

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.lifecycle.ViewModel
import com.mibcxb.common.skia.SkiaUtils
import com.mibcxb.viewer.cache.CacheApi
import com.mibcxb.viewer.cache.CacheSqlite
import com.mibcxb.viewer.log.LogApi
import com.mibcxb.widget.compose.file.FileType
import com.mibcxb.widget.compose.file.ViewerItem
import okio.Buffer
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.SamplingMode
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

abstract class AbsViewModel(protected val cacheApi: CacheApi = CacheSqlite()) : ViewModel(), LogApi {
    override val logTag: String = javaClass.simpleName
    override val logger: Logger = LoggerFactory.getLogger(logTag)

    fun getThumbBuffer(curItem: ViewerItem): Buffer? {
        val dataBytes = getThumbnail(curItem) ?: return null
        return Buffer().write(dataBytes)
    }

    fun getThumbnail(curItem: ViewerItem): ByteArray? {
        if (!curItem.isImage) return null
        val curBytes = cacheApi.obtainCacheThumb(curItem.path.raw)
        if (curBytes != null) return curBytes
        val newBytes = when {
            curItem.isLocal -> {
                genThumbSkia(File(curItem.path.basePath))
            }
            curItem.isSamba -> {
                val data = curItem.getInputStream()?.use { it.readBytes() } ?: return null
                genThumbSkia(data, curItem.extension)
            }
            else -> return null
        }
        if (newBytes != null) {
            val flag = cacheApi.insertCacheThumb(curItem.path.raw, newBytes)
            logger.debug("insertCacheThumb: $flag, path: ${curItem.path.raw}, size: ${newBytes.size}")
        }
        return newBytes
    }

    private fun genThumbSkia(
        file: File,
        target: Size = Size(160f, 120f),
        format: EncodedImageFormat = EncodedImageFormat.PNG,
        samplingMode: SamplingMode = SamplingMode.DEFAULT,
        quality: Int = 90
    ): ByteArray? = genThumbSkia(file.readBytes(), file.extension, target, format, samplingMode, quality)

    private fun genThumbSkia(
        data: ByteArray,
        extension: String,
        target: Size = Size(160f, 120f),
        format: EncodedImageFormat = EncodedImageFormat.PNG,
        samplingMode: SamplingMode = SamplingMode.DEFAULT,
        quality: Int = 90
    ): ByteArray? = kotlin.runCatching {
        if (FileType.SVG.extensions.contains(extension.lowercase())) {
            SkiaUtils.svgThumb(data, target = target, format = format, quality = quality)
        } else {
            SkiaUtils.genThumb(data, target, format, samplingMode, quality)
        }
    }.onFailure { logger.error(logTag, it.message, it) }.getOrNull()

    fun getThumbBitmap(curItem: ViewerItem): ImageBitmap? {
        val dataBytes = getThumbnail(curItem) ?: return null
        return Image.makeFromEncoded(dataBytes).toComposeImageBitmap()
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
./gradlew :viewer-gui:compileKotlin
```

Expected: 只有 BrowseScreenViewNew 编译错误（因为它仍引用旧类型）；AbsViewModel 应编译通过。

- [ ] **Step 3: 提交**

```bash
git add viewer-gui/src/main/kotlin/com/mibcxb/viewer/vm/AbsViewModel.kt
git commit -m "refactor: update AbsViewModel to use ViewerItem instead of FileStub"
```

---

### Task 13: 更新 BrowseScreenViewNew

**Files:**
- Modify: `viewer-gui/src/main/kotlin/com/mibcxb/viewer/screen/BrowseScreenViewNew.kt`

- [ ] **Step 1: 重写 BrowseScreenViewNew — 替换所有类型**

将 `FileItem`、`SmbTreeItem`、`FileStub`、`SmbFileStub` 全部替换为 `ViewerItem`：

```kotlin
package com.mibcxb.viewer.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.mibcxb.viewer.app.LocalAppRes
import com.mibcxb.viewer.cache.CacheApi
import com.mibcxb.viewer.vm.BrowseViewModel
import com.mibcxb.viewer_gui.generated.resources.Res
import com.mibcxb.viewer_gui.generated.resources.ic_cancel
import com.mibcxb.viewer_gui.generated.resources.ic_copy
import com.mibcxb.viewer_gui.generated.resources.ic_file_archive
import com.mibcxb.viewer_gui.generated.resources.ic_file_image
import com.mibcxb.viewer_gui.generated.resources.ic_folder
import com.mibcxb.viewer_gui.generated.resources.ic_folder_create
import com.mibcxb.viewer_gui.generated.resources.ic_folder_upward
import com.mibcxb.viewer_gui.generated.resources.ic_next
import com.mibcxb.viewer_gui.generated.resources.ic_refresh
import com.mibcxb.viewer_gui.generated.resources.ic_search
import com.mibcxb.viewer_gui.generated.resources.ic_settings
import com.mibcxb.viewer_gui.generated.resources.ic_sort
import com.mibcxb.viewer_gui.generated.resources.ic_zoom_in
import com.mibcxb.viewer_gui.generated.resources.ic_zoom_out
import com.mibcxb.viewer_gui.generated.resources.icon_filetype_jpg
import com.mibcxb.viewer_gui.generated.resources.icon_filetype_png
import com.mibcxb.viewer_gui.generated.resources.icon_filetype_svg
import com.mibcxb.viewer_gui.generated.resources.item_large
import com.mibcxb.viewer_gui.generated.resources.item_middle
import com.mibcxb.viewer_gui.generated.resources.item_small
import com.mibcxb.viewer_gui.generated.resources.sort_type
import com.mibcxb.viewer_gui.generated.resources.text_create_folder
import com.mibcxb.viewer_gui.generated.resources.text_files
import com.mibcxb.viewer_gui.generated.resources.text_input_keyword
import com.mibcxb.viewer_gui.generated.resources.text_preview
import com.mibcxb.widget.compose.Divider
import com.mibcxb.widget.compose.file.FileType
import com.mibcxb.widget.compose.file.FileTypes
import com.mibcxb.widget.compose.file.ViewerItem
import com.mibcxb.widget.compose.file.ViewerPath
import com.mibcxb.widget.compose.file.samba.SmbManager
import com.mibcxb.widget.compose.grid.FileGridSize
import com.mibcxb.widget.compose.grid.FileGridView
import com.mibcxb.widget.compose.tree.TreeView
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BrowseScreenViewNew(
    cacheApi: CacheApi,
    smbManager: SmbManager,
    vm: BrowseViewModel = viewModel { BrowseViewModel(cacheApi, smbManager) },
    nav: NavController
) {
    val appRes = LocalAppRes.current
    Column(modifier = Modifier.fillMaxSize()) {
        ToolbarView(
            vm,
            nav,
            modifier = Modifier.fillMaxWidth().height(appRes.dimen.functionHeight)
                .padding(horizontal = appRes.dimen.paddingLarge)
        )
        Divider(appRes.dimen.dividerWidth, appRes.color.dividerNormal)
        ContentView(
            vm,
            nav,
            modifier = Modifier.fillMaxWidth().weight(1f)
        )
    }

    LaunchedEffect(Unit) {
        vm.initFileTree()
    }
}

@Composable
private fun ToolbarView(vm: BrowseViewModel, nav: NavController, modifier: Modifier = Modifier) {
    val appRes = LocalAppRes.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { nav.navigate(SettingScreen) },
            modifier = Modifier.size(appRes.dimen.iconButtonSize)
        ) {
            Image(
                painterResource(Res.drawable.ic_settings),
                contentDescription = null,
                modifier = Modifier.wrapContentSize()
            )
        }
    }
}

@Composable
private fun FuncRowView(vm: BrowseViewModel, modifier: Modifier = Modifier) {
    val appRes = LocalAppRes.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { vm.goToParentPath() },
            modifier = Modifier.size(appRes.dimen.iconButtonSize)
        ) {
            Image(
                painterResource(Res.drawable.ic_folder_upward),
                contentDescription = null,
                modifier = Modifier.wrapContentSize()
            )
        }
        IconButton(
            onClick = { vm.refreshCurrent() },
            modifier = Modifier.padding(start = appRes.dimen.paddingPanel).size(appRes.dimen.iconButtonSize)
        ) {
            Image(
                painterResource(Res.drawable.ic_refresh),
                contentDescription = null,
                modifier = Modifier.wrapContentSize()
            )
        }
        Row(
            modifier = Modifier
                .padding(appRes.dimen.paddingPanel)
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(appRes.dimen.cornerNormal))
                .border(
                    width = appRes.dimen.borderWidth,
                    color = appRes.color.borderNormal,
                    shape = RoundedCornerShape(appRes.dimen.cornerNormal)
                )
                .padding(horizontal = appRes.dimen.paddingPanel),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painterResource(Res.drawable.ic_search),
                contentDescription = null,
                modifier = Modifier.wrapContentSize()
            )
            val searchName by remember { vm.searchName }
            BasicTextField(
                value = searchName,
                onValueChange = { vm.changeSearchName(it) },
                modifier = Modifier.padding(horizontal = appRes.dimen.paddingSmall).weight(1f),
                maxLines = 1
            ) { innerTextField ->
                if (searchName.isEmpty()) {
                    Text(stringResource(Res.string.text_input_keyword))
                }
                innerTextField()
            }
            if (searchName.isNotEmpty()) {
                IconButton(
                    onClick = { vm.removeSearchName() },
                    modifier = Modifier.size(appRes.dimen.searchNameIcon)
                ) {
                    Image(
                        painterResource(Res.drawable.ic_cancel),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .padding(appRes.dimen.paddingPanel)
                .wrapContentWidth()
                .fillMaxHeight()
                .clip(RoundedCornerShape(appRes.dimen.cornerNormal))
                .border(
                    width = appRes.dimen.borderWidth,
                    color = appRes.color.borderNormal,
                    shape = RoundedCornerShape(appRes.dimen.cornerNormal)
                )
                .clickable {}
                .padding(horizontal = appRes.dimen.paddingPanel),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painterResource(Res.drawable.ic_folder_create),
                contentDescription = null,
                modifier = Modifier.padding(end = appRes.dimen.paddingSmall).wrapContentSize()
            )
            Text(stringResource(Res.string.text_create_folder))
        }
        val fileGridSize by remember { vm.fileGridSize }
        IconButton(
            onClick = { vm.changeFileGridSize(fileGridSize.ordinal - 1) },
            modifier = Modifier.padding(start = appRes.dimen.paddingPanel).size(appRes.dimen.iconButtonSize)
        ) {
            Image(
                painterResource(Res.drawable.ic_zoom_out),
                contentDescription = null,
                modifier = Modifier.wrapContentSize()
            )
        }
        Row(
            modifier = Modifier
                .padding(
                    horizontal = appRes.dimen.paddingPanel,
                    vertical = appRes.dimen.paddingPanel
                )
                .wrapContentWidth()
                .fillMaxHeight()
                .border(
                    width = appRes.dimen.borderWidth,
                    color = appRes.color.borderNormal,
                    shape = RoundedCornerShape(appRes.dimen.cornerNormal)
                )
        ) {
            Box(
                modifier = Modifier
                    .wrapContentWidth()
                    .fillMaxHeight()
                    .clip(
                        RoundedCornerShape(
                            topStart = appRes.dimen.cornerNormal,
                            bottomStart = appRes.dimen.cornerNormal
                        )
                    )
                    .let {
                        if (fileGridSize == FileGridSize.Small) {
                            it.background(
                                Color.LightGray,
                                RoundedCornerShape(
                                    topStart = appRes.dimen.cornerNormal,
                                    bottomStart = appRes.dimen.cornerNormal
                                )
                            )
                        } else {
                            it
                        }
                    }
                    .clickable { vm.changeFileGridSize(FileGridSize.Small) }
                    .padding(start = appRes.dimen.paddingPanel, end = appRes.dimen.paddingSmall),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(Res.string.item_small))
            }
            Box(
                modifier = Modifier
                    .wrapContentWidth()
                    .fillMaxHeight()
                    .let {
                        if (fileGridSize == FileGridSize.Middle) {
                            it.background(Color.LightGray)
                        } else {
                            it
                        }
                    }
                    .clickable { vm.changeFileGridSize(FileGridSize.Middle) }
                    .padding(horizontal = appRes.dimen.paddingSmall),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(Res.string.item_middle))
            }
            Box(
                modifier = Modifier
                    .wrapContentWidth()
                    .fillMaxHeight()
                    .clip(
                        RoundedCornerShape(
                            topEnd = appRes.dimen.cornerNormal,
                            bottomEnd = appRes.dimen.cornerNormal
                        )
                    )
                    .let {
                        if (fileGridSize == FileGridSize.Large) {
                            it.background(
                                Color.LightGray,
                                RoundedCornerShape(
                                    topEnd = appRes.dimen.cornerNormal,
                                    bottomEnd = appRes.dimen.cornerNormal
                                )
                            )
                        } else {
                            it
                        }
                    }
                    .clickable { vm.changeFileGridSize(FileGridSize.Large) }
                    .padding(start = appRes.dimen.paddingSmall, end = appRes.dimen.paddingPanel),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(Res.string.item_large))
            }
        }
        IconButton(
            onClick = { vm.changeFileGridSize(fileGridSize.ordinal + 1) },
            modifier = Modifier.size(appRes.dimen.iconButtonSize)
        ) {
            Image(
                painterResource(Res.drawable.ic_zoom_in),
                contentDescription = null,
                modifier = Modifier.wrapContentSize()
            )
        }
        Row(
            modifier = Modifier.padding(start = appRes.dimen.paddingLarge).wrapContentWidth().fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            var expanded by remember { mutableStateOf(false) }
            val sortType by remember { vm.fileSortType }
            val sortTypes = stringArrayResource(Res.array.sort_type)
            Text(sortTypes[sortType.ordinal])
            DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                sortTypes.forEachIndexed { index, label ->
                    DropdownMenuItem(onClick = { vm.changeFileSortType(index) }) {
                        Text(text = label)
                    }
                }
            }
            IconButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.size(appRes.dimen.iconButtonSize)
            ) {
                Image(
                    painterResource(Res.drawable.ic_sort),
                    contentDescription = null,
                    modifier = Modifier.wrapContentSize()
                )
            }
        }
    }
}

@Composable
private fun PathRowView(vm: BrowseViewModel, modifier: Modifier = Modifier) {
    val appRes = LocalAppRes.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val currentPath by remember { vm.currentPath }
        if (currentPath.isNotBlank()) {
            val vp = ViewerPath.parse(currentPath)
            val segments = buildPathSegments(vp)
            LazyRow(modifier = Modifier.weight(1f)) {
                itemsIndexed(segments) { index, segment ->
                    PathSegmentView(index, segment, index == segments.lastIndex) {
                        val newPath = buildPathFromSegments(vp, segments, index)
                        vm.goToTargetPath(newPath)
                    }
                }
            }
            IconButton(
                onClick = {},
                modifier = Modifier.size(appRes.dimen.iconButtonSize)
            ) {
                Image(
                    painterResource(Res.drawable.ic_copy),
                    contentDescription = null,
                    modifier = Modifier.wrapContentSize()
                )
            }
        }
    }
}

private fun buildPathSegments(vp: ViewerPath): List<String> {
    return when (vp.scheme) {
        ViewerPath.Scheme.File -> buildLocalPathSegments(vp.basePath)
        ViewerPath.Scheme.Samba -> {
            val raw = vp.basePath.removePrefix("//")
            val parts = raw.split("/").filter { it.isNotEmpty() }
            if (parts.size >= 2) {
                listOf("//${parts[0]}/${parts[1]}") + parts.drop(2)
            } else {
                listOf(vp.basePath)
            }
        }
    }
}

private fun buildLocalPathSegments(path: String): List<String> {
    val segments = mutableListOf<String>()
    val nioPath = java.nio.file.Paths.get(path).normalize()
    if (nioPath.root != null) {
        segments.add(nioPath.root.toString())
    }
    if (nioPath.nameCount > 0) {
        nioPath.forEach { segments.add(it.toString()) }
    }
    return segments
}

private fun buildPathFromSegments(vp: ViewerPath, segments: List<String>, endIndex: Int): String {
    return when (vp.scheme) {
        ViewerPath.Scheme.Samba -> {
            val root = segments.first()
            val subPaths = segments.drop(1).take(endIndex)
            if (subPaths.isEmpty()) root else "$root/${subPaths.joinToString("/")}"
        }
        ViewerPath.Scheme.File -> {
            segments.subList(0, endIndex + 1).joinToString(File.separator)
        }
    }
}

@Composable
private fun PathSegmentView(index: Int, segment: String, last: Boolean = false, onClick: (Int) -> Unit = {}) {
    val appRes = LocalAppRes.current
    Row(modifier = Modifier.wrapContentSize(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = segment,
            modifier = Modifier.wrapContentSize().clip(RoundedCornerShape(appRes.dimen.cornerNormal))
                .clickable { onClick(index) }.padding(appRes.dimen.paddingSmall)
        )
        if (!last) {
            Image(
                painterResource(Res.drawable.ic_next),
                contentDescription = null,
                colorFilter = ColorFilter.tint(appRes.color.pathArrowTint),
                modifier = Modifier.padding(horizontal = appRes.dimen.paddingSmall).size(appRes.dimen.pathArrowSize)
            )
        }
    }
}

@Composable
private fun ContentView(vm: BrowseViewModel, nav: NavController, modifier: Modifier = Modifier) {
    val appRes = LocalAppRes.current
    Row(modifier = modifier) {
        Column(modifier = Modifier.weight(0.25f).fillMaxHeight()) {
            val treeRoots = remember { vm.treeRoots }
            TreeView(
                rootList = treeRoots,
                modifier = Modifier.fillMaxWidth().weight(1f),
                onSingleClick = { item -> vm.singleClickTreeItem(item) },
                onDoubleClick = { item ->
                    when {
                        item.isImage -> nav.navigate(DetailScreen(item.path.raw))
                        item.isArchive -> nav.navigate(ArchiveScreen(item.path.raw))
                        item.isDirectory -> vm.doubleClickTreeItem(item)
                    }
                },
                iconLoader = { item ->
                    when {
                        item.isDirectory -> Res.drawable.ic_folder
                        else -> Res.drawable.ic_file_image
                    }
                }
            )
            Divider(appRes.dimen.dividerWidth)
            Preview(vm, modifier = Modifier.fillMaxWidth().fillMaxHeight(0.35f))
        }
        Divider(appRes.dimen.dividerWidth, vertical = true)
        Column(modifier = Modifier.weight(0.75f).fillMaxHeight()) {
            val currentItem by remember { vm.currentItem }
            if (currentItem.exists() && currentItem.isDirectory) {
                FuncRowView(
                    vm,
                    modifier = Modifier.fillMaxWidth().height(appRes.dimen.functionHeight)
                        .padding(horizontal = appRes.dimen.paddingLarge)
                )
                Divider(appRes.dimen.dividerWidth, appRes.color.dividerNormal)
                PathRowView(
                    vm,
                    modifier = Modifier.fillMaxWidth().height(appRes.dimen.functionHeight)
                        .padding(horizontal = appRes.dimen.paddingLarge)
                )
                Divider(appRes.dimen.dividerWidth, appRes.color.dividerNormal)
                val fileGridSize by remember { vm.fileGridSize }
                val searchName by remember { vm.searchName }
                val sortType by remember { vm.fileSortType }
                FileGridView(
                    item = currentItem,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    sortType = sortType,
                    itemSize = fileGridSize,
                    onSingleClick = { vm.singleClickGridItem(it) },
                    onDoubleClick = {
                        when {
                            it.isImage -> nav.navigate(DetailScreen(it.path.raw))
                            it.isArchive -> nav.navigate(ArchiveScreen(it.path.raw))
                            it.isDirectory -> vm.doubleClickGridItem(it)
                        }
                    },
                    cacheLoader = { vm.getThumbBuffer(it) },
                    errorLoader = {
                        when {
                            it.isDirectory -> Res.drawable.ic_folder
                            it.isArchive -> Res.drawable.ic_file_archive
                            else -> Res.drawable.ic_file_image
                        }
                    },
                    imageLoader = {
                        when (it.fileType) {
                            FileType.JPG -> Res.drawable.icon_filetype_jpg
                            FileType.PNG -> Res.drawable.icon_filetype_png
                            FileType.SVG -> Res.drawable.icon_filetype_svg
                            else -> null
                        }
                    }
                ) {
                    it.name.contains(searchName, ignoreCase = true)
                }
                Divider(appRes.dimen.dividerWidth, appRes.color.dividerNormal)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.padding(end = appRes.dimen.paddingPanel).fillMaxWidth().height(24.dp)
                ) {
                    Text(stringResource(Res.string.text_files, currentItem.children.size))
                }
            }
        }
    }
}

@Composable
private fun Preview(vm: BrowseViewModel, modifier: Modifier = Modifier) {
    val appRes = LocalAppRes.current
    Column(modifier = modifier) {
        Text(
            stringResource(Res.string.text_preview),
            modifier = Modifier.padding(start = appRes.dimen.paddingPanel, top = appRes.dimen.paddingSmall)
                .wrapContentSize()
        )
        Box(modifier = Modifier.fillMaxWidth().weight(1.0f).background(appRes.color.imagePreviewBackground)) {
            val previewImageItem by remember { vm.previewImageItem }
            if (previewImageItem.isFile) {
                AsyncImage(
                    model = previewImageItem.path.raw,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.padding(appRes.dimen.paddingSmall).fillMaxWidth().wrapContentHeight()
        ) {

        }
    }
}
```

注意：移除了 `when (item) { is FileItem -> ... is SmbTreeItem -> ... }` 的类型分支，因为统一为 `ViewerItem`。

- [ ] **Step 2: 编译验证**

```bash
./gradlew :viewer-gui:compileKotlin
```

Expected: 可能有 ArchiveViewModel 和 DetailViewModel 的错误（因为它们仍使用旧类型）

- [ ] **Step 3: 提交**

```bash
git add viewer-gui/src/main/kotlin/com/mibcxb/viewer/screen/BrowseScreenViewNew.kt
git commit -m "refactor: update BrowseScreenViewNew to use ViewerItem"
```

---

### Task 14: 更新 ArchiveViewModel 和 ArchiveScreenView

**Files:**
- Modify: `viewer-gui/src/main/kotlin/com/mibcxb/viewer/vm/ArchiveViewModel.kt`
- Modify: `viewer-gui/src/main/kotlin/com/mibcxb/viewer/screen/ArchiveScreenView.kt`

- [ ] **Step 1: 重写 ArchiveViewModel**

```kotlin
package com.mibcxb.viewer.vm

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.mibcxb.common.http.MimeTypes
import com.mibcxb.viewer.cache.CacheApi
import com.mibcxb.viewer.cache.CacheSqlite
import com.mibcxb.widget.compose.file.archive.ArchiveAccessor
import com.mibcxb.widget.compose.file.archive.ArchiveAccessorFactory
import com.mibcxb.widget.compose.file.archive.ArchiveEntryStub
import com.mibcxb.widget.compose.file.archive.SevenZAccessor
import com.mibcxb.widget.compose.file.archive.ZipFileAccessor
import com.mibcxb.widget.compose.file.ViewerItem
import com.mibcxb.widget.compose.file.ViewerPath
import com.mibcxb.widget.compose.file.FileType
import com.mibcxb.widget.compose.file.LocalSource
import okio.Buffer
import java.io.File

class ArchiveViewModel(cacheApi: CacheApi = CacheSqlite()) : AbsViewModel(cacheApi) {
    private val _filepath = mutableStateOf("")
    val filepath: State<String> get() = _filepath

    private val _subEntryList = mutableStateListOf<ViewerItem>()
    val subEntryList: SnapshotStateList<ViewerItem> get() = _subEntryList

    private val _subEntryItem = mutableStateOf<ViewerItem?>(null)
    val subEntryItem: State<ViewerItem?> get() = _subEntryItem

    private val accessorFactory: ArchiveAccessorFactory = ArchiveAccessorFactory { fileType, filePath ->
        when (fileType) {
            FileType.ZIP -> ZipFileAccessor(filePath)
            FileType.SevenZ -> SevenZAccessor(filePath)
            else -> null
        }
    }
    private var archiveAccessor: ArchiveAccessor? = null

    fun initFilePath(filepath: String) {
        if (_filepath.value != filepath) {
            _filepath.value = filepath
            prepareArchive()
        }
    }

    private fun prepareArchive() {
        val archivePath = _filepath.value
        val vp = ViewerPath.parse(archivePath)
        val archiveFile = File(vp.basePath)
        if (!archiveFile.exists() || !archiveFile.isFile) return

        val accessor = accessorFactory.createArchiveAccessor(vp.fileType, vp.basePath)
        if (accessor != null) {
            archiveAccessor = accessor.apply { prepare() }
            val extensions = listOf(FileType.JPG, FileType.PNG).flatMap { it.extensions.toList() }.toTypedArray()
            val imgEntryList = accessor.getEntryList { stub ->
                !stub.isDirectory() && stub.extension in extensions
            }
            _subEntryList.clear()
            _subEntryList.addAll(imgEntryList.map { entry ->
                ViewerItem(
                    ViewerPath.createArchived(vp, entry.archiveEntry.name),
                    null
                )
            })
        }
    }

    fun singleClickListItem(item: ViewerItem) {
        if (_subEntryItem.value != item) {
            _subEntryItem.value = item
        }
    }

    fun getSubEntryData(item: ViewerItem): Buffer? {
        val accessor = archiveAccessor ?: return null
        val stub = ArchiveEntryStub.getByPath(accessor, item.path.entryPath) ?: return null
        val stream = accessor.getInputStream(stub) ?: return null
        return stream.use { stream ->
            val output = Buffer()
            val buffer = ByteArray(8 * 1024)
            var length: Int
            while (stream.read(buffer).also { length = it } > 0) {
                output.write(buffer, 0, length)
            }
            output
        }
    }

    fun getSubEntryMime(item: ViewerItem): String? = MimeTypes.optMimeTypeByExtension(item.extension)

    override fun onCleared() {
        archiveAccessor?.release()
        archiveAccessor = null
    }
}
```

注意：`ArchiveEntryStub.getByPath()` 需要新增一个辅助方法。

- [ ] **Step 2: 在 ArchiveEntryStub 中添加静态辅助方法**

在 `widget-lib/src/main/kotlin/com/mibcxb/widget/compose/file/archive/ArchiveEntryStub.kt` 的 companion object 中添加：

```kotlin
companion object {
    fun getByPath(accessor: ArchiveAccessor, entryPath: String): ArchiveEntryStub? {
        return accessor.getEntryList().find { it.archiveEntry.name == entryPath }
    }
}
```

- [ ] **Step 3: 更新 ArchiveScreenView**

将 `ArchiveEntryStub` 替换为 `ViewerItem`，`ArchiveEntryKeyer` 替换为 `ViewerItemKeyer`：

```kotlin
package com.mibcxb.viewer.screen

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.Image
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.decode.DataSource
import com.mibcxb.viewer.app.LocalAppRes
import com.mibcxb.viewer.cache.CacheApi
import com.mibcxb.viewer.vm.ArchiveViewModel
import com.mibcxb.viewer_gui.generated.resources.Res
import com.mibcxb.viewer_gui.generated.resources.ic_arrow_circle_left
import com.mibcxb.viewer_gui.generated.resources.icon_filetype_jpg
import com.mibcxb.viewer_gui.generated.resources.icon_filetype_png
import com.mibcxb.viewer_gui.generated.resources.icon_filetype_svg
import com.mibcxb.widget.compose.Divider
import com.mibcxb.widget.compose.coil.DelegateFetcher
import com.mibcxb.widget.compose.file.FileType
import com.mibcxb.widget.compose.file.ViewerItem
import com.mibcxb.widget.compose.file.ViewerItemKeyer
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun ArchiveScreenView(
    cacheApi: CacheApi,
    vm: ArchiveViewModel = viewModel { ArchiveViewModel(cacheApi) },
    filepath: String = "",
    nav: NavController
) {
    val appRes = LocalAppRes.current
    Row(modifier = Modifier.fillMaxSize()) {
        val vScroll = rememberLazyListState()
        val hScroll = rememberScrollState()
        Column(modifier = Modifier.weight(0.225f).fillMaxHeight()) {
            Row(
                modifier = Modifier.fillMaxWidth().height(appRes.dimen.functionHeight)
                    .padding(horizontal = appRes.dimen.paddingLarge),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { nav.popBackStack() },
                    modifier = Modifier.size(appRes.dimen.iconButtonSize)
                ) {
                    Image(
                        painterResource(Res.drawable.ic_arrow_circle_left),
                        contentDescription = null,
                        modifier = Modifier.wrapContentSize()
                    )
                }
            }
            Divider(appRes.dimen.dividerWidth, appRes.color.dividerNormal)
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                val subEntryList = remember { vm.subEntryList }
                LazyColumn(
                    state = vScroll,
                    contentPadding = PaddingValues(vertical = appRes.dimen.paddingPanel),
                    modifier = Modifier.wrapContentWidth().fillMaxHeight().horizontalScroll(hScroll)
                ) {
                    items(subEntryList, key = { it.id }) { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .heightIn(
                                    min = appRes.dimen.archiveItemHeightMin,
                                    max = appRes.dimen.archiveItemHeightMax
                                )
                                .combinedClickable(
                                    onClick = { vm.singleClickListItem(item) },
                                    onDoubleClick = { }
                                )
                        ) {
                            val iconSize = appRes.dimen.archiveItemIconSize
                            val hPadding = appRes.dimen.paddingPanel
                            val drawable: DrawableResource? = when (item.fileType) {
                                FileType.JPG -> Res.drawable.icon_filetype_jpg
                                FileType.PNG -> Res.drawable.icon_filetype_png
                                FileType.SVG -> Res.drawable.icon_filetype_svg
                                else -> null
                            }
                            if (drawable != null) {
                                Icon(
                                    painterResource(drawable),
                                    contentDescription = null,
                                    modifier = Modifier.padding(start = hPadding, end = hPadding / 2).size(iconSize)
                                )
                            } else {
                                Spacer(modifier = Modifier.padding(start = hPadding, end = hPadding / 2).size(iconSize))
                            }
                            Text(item.path.entryPath.ifEmpty { item.name }, modifier = Modifier.padding(end = hPadding).wrapContentWidth())
                        }
                    }
                }
                VerticalScrollbar(
                    adapter = rememberScrollbarAdapter(vScroll),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                )
                HorizontalScrollbar(
                    adapter = rememberScrollbarAdapter(hScroll),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                )
            }
        }
        Divider(appRes.dimen.dividerWidth, vertical = true)
        Box(modifier = Modifier.weight(0.775f).fillMaxHeight()) {
            val subEntryItem by remember { vm.subEntryItem }
            if (subEntryItem != null) {
                val platformContext = LocalPlatformContext.current
                AsyncImage(
                    model = subEntryItem,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    imageLoader = ImageLoader.Builder(platformContext).components {
                        add(ViewerItemKeyer())
                        add(
                            DelegateFetcher.Factory(
                                source = DataSource.DISK,
                                getData = vm::getSubEntryData,
                                getMime = vm::getSubEntryMime
                            )
                        )
                    }.build(),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        vm.initFilePath(filepath)
    }
}
```

- [ ] **Step 4: 编译验证**

```bash
./gradlew :viewer-gui:compileKotlin
```

Expected: 只剩 DetailViewModel/DetailScreenView 的编译错误

- [ ] **Step 5: 提交**

```bash
git add viewer-gui/src/main/kotlin/com/mibcxb/viewer/vm/ArchiveViewModel.kt viewer-gui/src/main/kotlin/com/mibcxb/viewer/screen/ArchiveScreenView.kt widget-lib/src/main/kotlin/com/mibcxb/widget/compose/file/archive/ArchiveEntryStub.kt
git commit -m "refactor: update ArchiveViewModel and ArchiveScreenView to use ViewerItem"
```

---

### Task 15: 更新 DetailViewModel 和 DetailScreenView

**Files:**
- Modify: `viewer-gui/src/main/kotlin/com/mibcxb/viewer/vm/DetailViewModel.kt`
- Modify: `viewer-gui/src/main/kotlin/com/mibcxb/viewer/screen/DetailScreenView.kt`

- [ ] **Step 1: 重写 DetailViewModel**

```kotlin
package com.mibcxb.viewer.vm

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import com.mibcxb.viewer.cache.CacheApi
import com.mibcxb.viewer.cache.CacheSqlite
import com.mibcxb.widget.compose.file.ViewerItem
import com.mibcxb.widget.compose.file.ViewerItemFilter
import com.mibcxb.widget.compose.file.ViewerPath
import com.mibcxb.widget.compose.file.FileTypes
import com.mibcxb.widget.compose.file.LocalSource
import com.mibcxb.widget.compose.file.samba.SambaSource
import com.mibcxb.widget.compose.file.samba.SmbManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class DetailViewModel(
    cacheApi: CacheApi = CacheSqlite(),
    private val smbManager: SmbManager? = null
) : AbsViewModel(cacheApi) {

    private val _parentItem = mutableStateOf(emptyViewerItem)
    val parentItem: State<ViewerItem> get() = _parentItem

    private val _currentPath = mutableStateOf("")
    val currentPath: State<String> get() = _currentPath

    private val _showList = mutableStateOf(false)
    val showList: State<Boolean> get() = _showList

    private val fileFilter: ViewerItemFilter = { item ->
        val extNames = FileTypes.images.flatMap { it.extensions.toList() }
        when {
            item.isFile -> extNames.contains(item.extension)
            else -> false
        }
    }

    companion object {
        private val emptyViewerItem = ViewerItem(ViewerPath.parse(""))
    }

    fun initFilePath(filepath: String) {
        viewModelScope.launch {
            val vp = ViewerPath.parse(filepath)
            when (vp.scheme) {
                ViewerPath.Scheme.Samba -> initSambaFilePath(vp)
                ViewerPath.Scheme.File -> initLocalFilePath(vp)
            }
        }
    }

    private suspend fun initSambaFilePath(vp: ViewerPath) {
        val raw = vp.basePath.removePrefix("//")
        val parts = raw.split("/").filter { it.isNotEmpty() }
        if (parts.size < 2) return
        val host = parts[0]
        val share = parts[1]
        val remotePath = parts.drop(2).joinToString("/")
        val session = smbManager?.getSession(host, share) ?: return
        withContext(Dispatchers.IO) {
            val source = SambaSource(session)
            val item = ViewerItem(ViewerPath.create(host, share, remotePath), source)
            if (!item.isFile) return@withContext
            val parentPath = remotePath.substringBeforeLast("/", "")
            val parentVp = ViewerPath.create(host, share, parentPath)
            _parentItem.value = ViewerItem(parentVp, source).apply { refreshList(fileFilter) }
            _currentPath.value = vp.raw
        }
    }

    private suspend fun initLocalFilePath(vp: ViewerPath) {
        withContext(Dispatchers.IO) {
            val file = File(vp.basePath)
            if (file.exists() && file.isFile) {
                val parent = file.parentFile
                if (parent != null) {
                    val source = LocalSource()
                    _parentItem.value = ViewerItem(ViewerPath.create(parent), source).apply { refreshList(fileFilter) }
                }
                _currentPath.value = vp.raw
            }
        }
    }

    fun next() { change(1) }
    fun prev() { change(-1) }

    private fun change(delta: Int) {
        val parent = _parentItem.value
        if (!parent.isDirectory || parent.children.isEmpty()) return
        val currentPath = _currentPath.value
        if (currentPath.isBlank()) return
        val subFiles = parent.children
        val curIndex = subFiles.indexOfFirst { it.path.raw == currentPath }
        if (curIndex == -1) return
        val newIndex = curIndex + delta
        if (newIndex !in subFiles.indices) return
        val target = subFiles[newIndex]
        changeFilePath(target.path.raw)
    }

    fun changeFilePath(newPath: String) {
        if (_currentPath.value != newPath) {
            _currentPath.value = newPath
        }
    }
}
```

- [ ] **Step 2: 更新 DetailScreenView**

将 `FileStub` 替换为 `ViewerItem`，`FileStubKeyer` 替换为 `ViewerItemKeyer`：

```kotlin
package com.mibcxb.viewer.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.decode.DataSource
import com.mibcxb.viewer.app.LocalAppRes
import com.mibcxb.viewer.cache.CacheApi
import com.mibcxb.viewer.vm.DetailViewModel
import com.mibcxb.viewer_gui.generated.resources.Res
import com.mibcxb.viewer_gui.generated.resources.ic_arrow_circle_left
import com.mibcxb.viewer_gui.generated.resources.ic_arrow_circle_right
import com.mibcxb.viewer_gui.generated.resources.ic_cancel
import com.mibcxb.widget.compose.coil.DelegateFetcher
import com.mibcxb.widget.compose.coil.SmbFetcher
import com.mibcxb.widget.compose.file.ViewerItem
import com.mibcxb.widget.compose.file.ViewerItemKeyer
import com.mibcxb.widget.compose.file.samba.SmbManager
import org.jetbrains.compose.resources.painterResource

@Composable
fun DetailScreenView(
    cacheApi: CacheApi,
    smbManager: SmbManager? = null,
    vm: DetailViewModel = viewModel { DetailViewModel(cacheApi, smbManager) },
    filepath: String = "",
    nav: NavController
) {
    val appRes = LocalAppRes.current
    val platformContext = LocalPlatformContext.current
    val mainImageLoader = remember(smbManager) {
        ImageLoader.Builder(platformContext).components {
            smbManager?.let { add(SmbFetcher.Factory(it)) }
        }.build()
    }
    val thumbImageLoader = remember {
        ImageLoader.Builder(platformContext).components {
            add(ViewerItemKeyer())
            add(
                DelegateFetcher.Factory(
                    source = DataSource.DISK,
                    getData = { vm.getThumbBuffer(it as ViewerItem) },
                    getMime = { "image/png" }
                ))
        }.build()
    }
    Box(modifier = Modifier.fillMaxSize().background(color = Color.LightGray)) {
        val currentPath by remember { vm.currentPath }
        AsyncImage(model = currentPath, contentDescription = null, modifier = Modifier.fillMaxSize(), imageLoader = mainImageLoader)

        IconButton(
            onClick = { vm.next() },
            modifier = Modifier
                .padding(appRes.dimen.paddingGiant)
                .size(appRes.dimen.detailIconSize)
                .align(Alignment.CenterEnd)
        ) {
            Image(
                painterResource(Res.drawable.ic_arrow_circle_right),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }

        IconButton(
            onClick = { vm.prev() },
            modifier = Modifier
                .padding(appRes.dimen.paddingGiant)
                .size(appRes.dimen.detailIconSize)
                .align(Alignment.CenterStart)
        ) {
            Image(
                painterResource(Res.drawable.ic_arrow_circle_left),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }

        IconButton(
            onClick = { nav.popBackStack() },
            modifier = Modifier
                .padding(appRes.dimen.paddingGiant)
                .size(appRes.dimen.detailIconSize)
                .align(Alignment.TopEnd)
        ) {
            Image(painterResource(Res.drawable.ic_cancel), contentDescription = null, modifier = Modifier.fillMaxSize())
        }

        val parentItem by remember { vm.parentItem }
        val showList by remember { vm.showList }
        if (showList && parentItem.isDirectory) {
            LazyRow(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxSize().background(Color.DarkGray),
                contentPadding = PaddingValues(
                    horizontal = appRes.dimen.paddingLarge,
                    vertical = appRes.dimen.paddingPanel
                ),
                horizontalArrangement = Arrangement.spacedBy(appRes.dimen.paddingPanel)
            ) {
                items(parentItem.children, key = { it.id }) { fileItem ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .wrapContentSize()
                            .padding(appRes.dimen.paddingSmall)
                            .combinedClickable(
                                onClick = { vm.changeFilePath(fileItem.path.raw) }
                            )) {
                        AsyncImage(
                            model = fileItem,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            imageLoader = thumbImageLoader,
                            modifier = Modifier.size(appRes.dimen.detailPreviewWidth, appRes.dimen.detailPreviewHeight)
                        )
                        Text(
                            fileItem.name,
                            color = Color.White,
                            fontSize = appRes.dimen.detailTextSize,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(top = appRes.dimen.paddingSmall)
                                .widthIn(0.dp, appRes.dimen.detailPreviewWidth)
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        vm.initFilePath(filepath)
    }
}
```

- [ ] **Step 3: 编译验证**

```bash
./gradlew :viewer-gui:compileKotlin
```

Expected: BUILD SUCCESSFUL（所有旧类型引用已清除）

- [ ] **Step 4: 提交**

```bash
git add viewer-gui/src/main/kotlin/com/mibcxb/viewer/vm/DetailViewModel.kt viewer-gui/src/main/kotlin/com/mibcxb/viewer/screen/DetailScreenView.kt
git commit -m "refactor: update DetailViewModel and DetailScreenView to use ViewerItem"
```

---

### Task 16: 清理 ArchiveEntryStub 并删除旧文件

`ArchiveEntryStub` 当前实现 `FileStub` 接口。需要在删除 `FileStub` 之前先解除依赖。

**Files:**
- Modify: `widget-lib/src/main/kotlin/com/mibcxb/widget/compose/file/archive/ArchiveEntryStub.kt`
- Delete: `widget-lib/src/main/kotlin/com/mibcxb/widget/compose/tree/FileItem.kt`
- Delete: `widget-lib/src/main/kotlin/com/mibcxb/widget/compose/tree/SmbTreeItem.kt`
- Delete: `widget-lib/src/main/kotlin/com/mibcxb/widget/compose/file/FileStub.kt`
- Delete: `widget-lib/src/main/kotlin/com/mibcxb/widget/compose/file/FileStubImpl.kt`
- Delete: `widget-lib/src/main/kotlin/com/mibcxb/widget/compose/file/samba/SmbFileStub.kt`
- Delete: `widget-lib/src/main/kotlin/com/mibcxb/widget/compose/file/FileStubNone.kt`
- Delete: `widget-lib/src/main/kotlin/com/mibcxb/widget/compose/file/FileStubKeyer.kt`
- Delete: `widget-lib/src/main/kotlin/com/mibcxb/widget/compose/file/archive/ArchiveEntryKeyer.kt`

保留：`ArchiveEntryStub.kt`、`ZipEntryStub.kt`、`SevenZEntryStub.kt`（仍被 `ArchiveAccessor` 内部使用）。

- [ ] **Step 1: 修改 ArchiveEntryStub — 移除 FileStub 依赖**

将 `ArchiveEntryStub` 改为独立抽象类，不继承 `FileStub`，保留原有字段和逻辑：

```kotlin
package com.mibcxb.widget.compose.file.archive

import com.mibcxb.widget.compose.file.FileType
import org.apache.commons.compress.archivers.ArchiveEntry
import kotlin.io.path.Path
import kotlin.io.path.name

abstract class ArchiveEntryStub(val archiveEntry: ArchiveEntry) {
    protected val archiveEntryPath = Path(archiveEntry.name)
    val dirLevel = archiveEntryPath.nameCount

    val path: String get() = archiveEntry.name
    val name: String get() = archiveEntryPath.name
    val extension: String get() = name.substringAfterLast(".")
    val fileType: FileType = if (archiveEntry.isDirectory) {
        FileType.DIR
    } else {
        FileType.entries.find { it.extensions.contains(extension) } ?: FileType.NAN
    }
    val length: Long get() = archiveEntry.size
    val lastModified: Long get() = archiveEntry.lastModifiedDate?.time ?: 0L
    val createdAt: Long get() = lastModified

    fun isDirectory(): Boolean = archiveEntry.isDirectory
    fun isFile(): Boolean = !archiveEntry.isDirectory

    companion object {
        fun getByPath(accessor: ArchiveAccessor, entryPath: String): ArchiveEntryStub? {
            return accessor.getEntryList().find { it.archiveEntry.name == entryPath }
        }
    }
}
```

- [ ] **Step 2: 删除旧文件**

```bash
git rm widget-lib/src/main/kotlin/com/mibcxb/widget/compose/tree/FileItem.kt
git rm widget-lib/src/main/kotlin/com/mibcxb/widget/compose/tree/SmbTreeItem.kt
git rm widget-lib/src/main/kotlin/com/mibcxb/widget/compose/file/FileStub.kt
git rm widget-lib/src/main/kotlin/com/mibcxb/widget/compose/file/FileStubImpl.kt
git rm widget-lib/src/main/kotlin/com/mibcxb/widget/compose/file/samba/SmbFileStub.kt
git rm widget-lib/src/main/kotlin/com/mibcxb/widget/compose/file/FileStubNone.kt
git rm widget-lib/src/main/kotlin/com/mibcxb/widget/compose/file/FileStubKeyer.kt
git rm widget-lib/src/main/kotlin/com/mibcxb/widget/compose/file/archive/ArchiveEntryKeyer.kt
```

- [ ] **Step 3: 编译验证 — 确保无残留引用**

```bash
./gradlew :widget-lib:compileKotlin; if ($?) { ./gradlew :viewer-gui:compileKotlin }
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 提交**

```bash
git add widget-lib/src/main/kotlin/com/mibcxb/widget/compose/file/archive/ArchiveEntryStub.kt
git commit -m "chore: remove FileStub dependency from ArchiveEntryStub, delete old FileItem/FileStub/SmbTreeItem classes"
```

---

### Task 17: 全量编译 + 测试验证

- [ ] **Step 1: 完整编译**

```bash
./gradlew build -x test
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 运行 widget-lib 测试**

```bash
./gradlew :widget-lib:test
```

Expected: BUILD SUCCESSFUL, tests pass

- [ ] **Step 3: 运行 viewer-lib 测试**

```bash
./gradlew :viewer-lib:test
```

Expected: BUILD SUCCESSFUL, tests pass

- [ ] **Step 4: 运行 common-lib 测试**

```bash
./gradlew :common-lib:test
```

Expected: BUILD SUCCESSFUL, tests pass

- [ ] **Step 5: 修复测试失败**（如有）

检查失败的测试并修复类型引用。

- [ ] **Step 6: 提交**

```bash
git add -u
git commit -m "chore: fix remaining references and tests after ViewerPath migration"
```

---

### Task 18: 运行 desktop app 功能验证

- [ ] **Step 1: 启动应用**

```bash
./gradlew :viewer-gui:run
```

- [ ] **Step 2: 验证功能**
  - 左侧文件树显示本地驱动器
  - 点击树节点展开目录
  - 右侧网格显示文件
  - 路径面包屑正确渲染
  - 树图标正确（文件夹/文件）
  - SMB 连接后树中显示远程节点
  - 双击图片文件导航到 DetailScreen
  - 双击压缩包导航到 ArchiveScreen

- [ ] **Step 3: 关闭应用**
