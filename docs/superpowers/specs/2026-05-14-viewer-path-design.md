# ViewerPath 统一路径结构设计

## 背景

当前项目中路径表示为纯字符串，散落在三套独立类体系中：

- **Tree 层**: `FileItem`（本地）、`SmbTreeItem`（SMB）
- **Grid 层**: `FileStubImpl`（本地）、`SmbFileStub`（SMB）、`ArchiveEntryStub`（zip/7z）
- 压缩包入口不知道自己属于哪个压缩包文件，无法支持嵌套压缩包浏览

## 目标

1. 统一本地文件、Samba 远程文件、压缩包内文件的路径表示
2. 类型安全：不混淆不同来源的路径字符串
3. 压缩包入口可追溯父压缩包，支持嵌套浏览
4. 向后兼容现有 Coil、导航路由、SQLite 缓存

---

## 1. 路径字符串格式

| 场景 | 格式 |
|---|---|
| Windows 本地 | `C:\Users\xxx\photo.jpg` |
| macOS/Linux 本地 | `/home/xxx/photo.jpg` |
| Samba 远程 | `//host/share/folder/image.png` |
| 压缩包内（本地） | `C:\archive.zip!/folder/image.png` |
| 压缩包内（Samba） | `//host/share/archive.zip!/image.png` |
| 嵌套压缩包 | `C:\a.zip!/inner.zip!/deep.png` |

- `!/` 分隔压缩包层级（借鉴 Java NIO 惯例）
- 保留原生路径分隔符（Windows `\`，Unix `/`）
- Samba 以 `//` 开头（UNC 惯例）；Windows 本地第二个字符为 `:`；Unix 本地以 `/` 开头

---

## 2. ViewerPath

唯一的路径数据类，构建时解析所有字段，不可变。

```kotlin
@Stable
class ViewerPath private constructor(
    val raw: String,
    val scheme: Scheme,           // File 或 Samba
    val basePath: String,         // 不含压缩包部分，如 "C:\Users\xxx\archive.zip"
    val entryPath: String,        // 最内层压缩包内的路径，如 "folder/image.png"，无压缩包为空
    val archiveLayers: Int = 0,   // 压缩包嵌套层数

    val name: String,             // 文件名（含扩展名）
    val extension: String,        // 扩展名（小写，无点）
    val fileType: FileType,       // 文件类型枚举
    val length: Long,             // 文件大小
    val lastModified: Long,       // 最后修改时间
    val createdAt: Long           // 创建时间
) {
    enum class Scheme { File, Samba }

    val isArchiveEntry get() = archiveLayers > 0
    val isLocal get() = scheme == Scheme.File
    val isSamba get() = scheme == Scheme.Samba
    val isFile get() = !FileTypes.isDir(fileType)
    val isDirectory get() = FileTypes.isDir(fileType)
    val parentPath: String by lazy { ... }

    companion object {
        fun parse(raw: String): ViewerPath
        fun create(file: java.io.File): ViewerPath
        fun create(host: String, share: String, remotePath: String): ViewerPath
        fun createArchived(base: ViewerPath, archiveEntryName: String): ViewerPath
    }
}
```

---

## 3. ViewerSource

抽象数据源接口，解耦文件 IO 逻辑。扩展新协议只需实现此接口。

```kotlin
interface ViewerSource {
    fun listChildren(path: ViewerPath): List<ViewerPath>
    fun getInputStream(path: ViewerPath): InputStream?
    fun exists(path: ViewerPath): Boolean
}
```

实现：
- `LocalSource` — 基于 `java.io.File`
- `SambaSource` — 基于 `SmbSession`
- `ArchiveSource` — 基于 `ArchiveAccessor`

---

## 4. ViewerItem

统一 Tree 节点和 Grid 元素。替代 `FileItem`、`SmbTreeItem`、`FileStub` 及所有实现。

```kotlin
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
    fun setExpanded(enabled: Boolean)
    fun setSelected(enabled: Boolean)

    // Grid 委托
    val isDirectory get() = path.isDirectory
    val isFile get() = path.isFile
    val isLocal get() = path.isLocal
    val isSamba get() = path.isSamba
    val isArchiveEntry get() = path.isArchiveEntry

    fun refreshList()       // 委托给 source.listChildren()
    fun getInputStream()    // 委托给 source.getInputStream()
}
```

`FileTree` 容器的泛型绑定到 `ViewerItem`。

---

## 5. 集成

| 系统 | 当前 | 迁移后 |
|---|---|---|
| Coil Keyer | `FileStubKeyer` + `ArchiveEntryKeyer` | `ViewerItemKeyer`，key = `"viewer:${path.raw}"` |
| Coil Fetcher | `SmbFetcher` 匹配 `//` 开头 | 匹配 `ViewerPath.scheme == Samba` |
| 导航路由 | `DetailScreen(filepath: String)` | 不变，存 `ViewerPath.raw` |
| SQLite 缓存 | `CacheApi(path: String)` | 不变，传 `ViewerPath.raw` |

---

## 6. 迁移范围

| 操作 | 文件 |
|---|---|
| **新增** | `ViewerPath.kt`, `ViewerSource.kt`, `LocalSource.kt`, `SambaSource.kt`, `ArchiveSource.kt`, `ViewerItem.kt`, `ViewerItemKeyer.kt` |
| **修改** | `FileTree.kt`, `FileGridView.kt`, `BrowseViewModel.kt`, `BrowseScreen.kt`, `SmbFetcher.kt`, Coil 注册配置 |
| **删除** | `FileItem.kt`, `SmbTreeItem.kt`, `FileStub.kt`, `FileStubImpl.kt`, `SmbFileStub.kt`, `ArchiveEntryStub.kt`, `ZipEntryStub.kt`, `SevenZEntryStub.kt`, `FileStubNone.kt`, `FileStubKeyer.kt`, `ArchiveEntryKeyer.kt` |
| **无需改** | `TreeView.kt`, `CacheApi.kt`, `CacheSqlite.kt`, 导航路由定义 |
