## 总览
- 使用 Media3（ExoPlayer + MediaSession + MediaBrowser）与 Views（XML 布局）实现：首页专辑网格 → 专辑详情列表 → 播放页。
- 数据源：`https://storage.googleapis.com/uamp/catalog.json`（UAMP Catalog）。首次启动拉取并缓存到内存，必要时落地到 `assets` 兜底。
- 架构：单模块 App，按 `data`（网络/解析）→ `playback`（服务/会话）→ `ui`（Fragments）分层，Fragment + Navigation 组件做路由但全部使用 View XML。

## 依赖与配置
1. 添加依赖：
   - `androidx.media3:media3-exoplayer`、`androidx.media3:media3-session`、`androidx.media3:media3-ui`
   - `androidx.lifecycle:lifecycle-viewmodel-ktx`、`lifecycle-livedata-ktx`
   - `androidx.navigation:navigation-fragment`、`navigation-ui`（纯 Views）
   - 网络与解析：`okhttp` + `kotlinx-serialization-json`（同时启用 `kotlin("plugin.serialization")`）
   - 图片加载：`glide`
2. Manifest：
   - `INTERNET` 权限
   - 声明 `MediaLibraryService`：`android:name=.playback.UampMediaLibraryService`，`exported=true`，`foregroundServiceType="mediaPlayback"`，`<action android:name="android.media.browse.MediaBrowserService"/>`
3. 主题与深色模式：沿用现有 `Material3 DayNight`，为卡片与背景定义暗色变体与圆角尺寸。

## 数据模型与仓库（data）
1. 定义序列化模型：`Catalog`、`Album`、`Track`，字段匹配 UAMP JSON（如 `id/title/artist/year/artwork/trackUrl`）。
2. `CatalogRepository`：
   - `suspend fun loadCatalog(): Catalog` 使用 OkHttp 下载 JSON，Moshi/Serialization 解析；失败则读取 `assets/catalog.json`。
   - 提供 `getAlbums()`、`getTracks(albumId)`；在内存缓存。
3. 将 `Album/Track` 转换为 Media3 的 `MediaItem` 与 `MediaMetadata`（包含 `albumTitle/artist/mediaUri/artworkUri/releaseYear` 等）。

## 媒体服务与会话（playback）
1. `UampMediaLibraryService : MediaLibraryService`：
   - 持有 `ExoPlayer` 与 `MediaLibrarySession`，`SessionCallback` 实现：
     - `onGetLibraryRoot` 返回根节点（`ALBUMS`）。
     - `onGetChildren(parentId)`：`ALBUMS` → 专辑列表；`album:{id}` → 该专辑的曲目列表。
     - `onGetItem(mediaId)` 返回单曲/专辑 `MediaItem`。
   - `onAddMediaItems` 解析 `mediaId` 并填充完整 `MediaItem`（包含 `MediaMetadata` 与 `Uri`）。
2. 音频焦点与通知：使用 `Media3` 默认 `MediaStyle` 通知（`PlayerNotificationManager` 由 `session` 自动集成）。
3. 播放队列：点击某首歌时，将专辑曲目列表作为队列，指定 index 直接 `controller.play()`。

## 浏览与控制连接（controller/browser）
- UI 层通过 `MediaBrowser` 连接到服务：`val token = SessionToken(context, ComponentName(context, UampMediaLibraryService::class.java))` → `MediaBrowser.Builder(...).build()`。
- 获取专辑：`browser.getChildren("ALBUMS", page, pageSize)`；获取曲目：`browser.getChildren("album:{id}", ...)`。
- 交互控制：在播放页创建 `MediaController`（或复用 `browser` 的控制），绑定 `Player.Listener` 更新 UI、进度与按钮状态。

## UI 页面（Views/XML）
1. `AlbumsFragment`（首页）
   - 布局：`RecyclerView` 网格（2 列），`MaterialCardView` + `ImageView` 显示封面，底部文字区域显示专辑名、艺术家、年份；圆角与阴影与设计一致。
   - 数据：`AlbumsViewModel` 订阅 `LiveData<List<AlbumUI>>`，通过仓库 + `browser` 拉取。
   - 交互：点击卡片 → 跳转 `AlbumDetailFragment(albumId)`。
2. `AlbumDetailFragment`
   - 顶部：封面、标题、艺术家、年份与「共 N 首歌曲」。
   - 列表：`RecyclerView`（纵向），每项显示序号、歌曲名、艺术家、时长；点击某项 → 打开 `PlayerFragment` 并开始播放该专辑队列（从该索引）。
3. `PlayerFragment`
   - 视图：大图封面（`ImageView`，圆角），标题/艺术家文本，`DefaultTimeBar` 或自定义 `SeekBar`，底部五个按钮：随机、上一首、播放/暂停、下一首、循环；右侧收藏心形（本地标记）。
   - 绑定：`MediaController` 的状态驱动 UI；`Player.Listener` 更新进度与按钮图标；`controller.shuffleModeEnabled`/`repeatMode` 切换。
   - 可选：用 `PlayerView` 设置 `useController=false`，仅用于渲染封面与 TimeBar，控制按钮自定义。

## 导航与路由
- `MainActivity` 承载 `NavHostFragment` 与 `nav_graph.xml`：
  - `albums` → `albumDetail/{albumId}` → `player/{albumId}/{trackIndex}`
- 返回按钮与标题按设计图：`Toolbar` 集成 `NavigationUI.setupWithNavController`。

## 错误处理与缓存
- 网络失败时从 `assets/catalog.json` 加载；UI 显示 Snackbar 提示并允许重试。
- 图片加载失败显示占位图；时长解析失败显示 `--:--`。

## 验证与测试
- 手动流程：启动 → 首页专辑网格 → 详情列表 → 点击某曲播放页，验证播放、进度、随机/循环/上一/下一与通知。
- 组件测试：
  - 仓库解析单元测试（JSON → 模型 → MediaItem）。
  - `MediaLibraryService` 的 `onGetChildren` 与 `onAddMediaItems` 行为测试（本地 JVM/ Robolectric）。

## 交付物与文件结构
- `app/src/main/java/com/example/media3uamp/`
  - `data/`：`CatalogRepository.kt`、模型 `Catalog.kt`、`Album.kt`、`Track.kt`
  - `playback/`：`UampMediaLibraryService.kt`、`SessionCallback.kt`
  - `ui/albums/`：`AlbumsFragment.kt`、`AlbumsAdapter.kt`、`AlbumsViewModel.kt`
  - `ui/detail/`：`AlbumDetailFragment.kt`、`AlbumDetailAdapter.kt`、`AlbumDetailViewModel.kt`
  - `ui/player/`：`PlayerFragment.kt`、`PlayerViewModel.kt`
  - `MainActivity.kt`、`navigation/nav_graph.xml`
- `res/layout/`：`fragment_albums.xml`、`item_album.xml`、`fragment_album_detail.xml`、`item_track.xml`、`fragment_player.xml`
- `assets/catalog.json`（兜底）

## 实施步骤
1. 引入依赖与 Manifest、权限配置。
2. 编写数据模型与 `CatalogRepository`，实现网络拉取与缓存。
3. 搭建 `UampMediaLibraryService`、`MediaLibrarySession` 与 `ExoPlayer`；完成根/专辑/曲目节点的浏览与 `MediaItem` 构建。
4. 接入 `MediaBrowser` 与 `MediaController`，实现 UI 三个页面与交互事件绑定。
5. 完成导航、样式与圆角卡片效果，适配深色主题。
6. 添加兜底/错误处理与基础测试，端到端验证播放流程。

如果确认该方案，我将按以上步骤在当前工程中落地实现并与截图一致的 UI 效果。