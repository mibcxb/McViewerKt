# Changelog

## 2026-05-13

- **SMB / Samba file browsing**: connect to SMB shares, browse remote directories in the sidebar tree, and view files in the grid.
- **SmbTreeItem**: expandable tree nodes for SMB directories with lazy child loading.
- **SmbFileStub**: `FileStub` implementation backed by SMB sessions, with `getInputStream()` for reading remote files.
- **SmbManager**: global singleton for managing SMB sessions (connect, disconnect, list).
- **SmbFetcher**: custom Coil3 fetcher that resolves `//host/share/path` strings and reads image data over SMB.
- **Detail screen SMB support**: display SMB-hosted images with next/prev navigation and thumbnail generation.
- **SMB connection management UI** (`SettingScreenView`): add, save, connect, disconnect, and delete SMB connections.
- **FileStubFilter** typealias `(FileStub) -> Boolean` replacing `java.io.FileFilter`.
- Unified tree model: `TreeView<TreeItem>` dispatches on `is FileItem` / `is SmbTreeItem`.
- SettingViewModel suspend functions use `withContext(Dispatchers.IO)` for DB/network I/O.
- `SmbFileStub` constructor made `internal`; public `create()` factory.
- Browse grid sorting.
- Updated dependencies.

## 2026-01-04

- `ArchiveScreen` back button.
- Added `ComposablesIcons` (Tabler icons).

## 2025-12-31

- `DetailScreen` preview list (bottom thumbnail strip).

## 2025-12-30

- `DetailScreen` next/prev navigation between images.

## 2025-12-17

- Rebuilt `BrowseScreen` → `BrowseScreenViewNew` with Compose Navigation.

## 2025-09-30

- Added `ThumbCreator` for generating image thumbnails.

## 2025-09-17

- Added custom font Oppo Sans 4.0 via Compose resources.
- Moved archive handling to `widget-lib`.
- Added `MimeTypes` utility.
- Added scrollbar for file list and tree views.
- Added 7z archive support.
- Upgraded Compose to 1.9.0; added zip archive support.

## 2025-09-12

- Added SVG image support.
- Added `SkiaUtils` for image processing.
- Fixed list flickering during thumbnail generation.

## 2025-09-10

- Added `DetailScreenView`.
- Renamed `HomeScreen` → `BrowseScreen`.
- DB file stored under `user.home`.

## 2025-09-09

- Coil `DelegateFetcher` for loading cached image thumbnails.
- SQLite-based image cache (`CacheSqlite`).

## 2025-09-08

- Added upward (parent directory) navigation button.
- Added image preview panel.
- Added `FileGridView`.

## 2025-08-22

- Added `HomeScreen` and `TreeView`.

## 2025-08-21

- Added ViewModel and Compose Navigation.
- Fixed source code directory structure.

## 2025-07-02

- Created Gradle project (`common-lib`, `widget-lib`, `viewer-lib`, `viewer-gui`).
- Added devcontainer configuration.

## 2024-12-29

- Initial commit.
