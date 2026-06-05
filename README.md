# CloudStorage

CloudStorage 是一个基于 Kotlin 与 Jetpack Compose 实现的 Android 网盘客户端 Demo。项目内置 Mock 文件数据与本地 Room 存储，不需要额外部署后端服务即可运行和验证主要功能。

## 功能概览

- 文件列表、目录浏览、筛选、重命名、移动、删除、上传入口
- TXT 阅读器与视频播放页
- 分享链接列表与剪贴板分享链接检测
- 本地数据库缓存与 Mock 远程数据源
- Compose Material3 界面，支持 Android 7.0 及以上设备

## 技术栈

| 类别 | 版本/说明 |
| --- | --- |
| Android Gradle Plugin | 8.13.2 |
| Gradle Wrapper | 8.13 |
| Kotlin | 2.0.21 |
| JDK | 17，推荐使用 Android Studio 自带 JDK |
| compileSdk / targetSdk | 36 |
| minSdk | 24 |
| UI | Jetpack Compose、Material3 |
| 数据 | Room、Mock JSON 数据源 |
| 导航 | Navigation Compose |
| 媒体 | Media3 ExoPlayer、Coil |

## 目录结构

```text
CloudStorage/
├── app/                         # Android App 模块
│   └── src/main/
│       ├── assets/              # Mock 数据，默认读取 mock_files.json
│       ├── java/com/bytedance/cloudstorage/
│       │   ├── data/            # 数据源、Repository、Room、DTO/Entity/Mapper
│       │   ├── domain/          # 领域模型
│       │   ├── navigation/      # Compose 导航
│       │   ├── presentation/    # 页面与 ViewModel
│       │   ├── ui/theme/        # 主题
│       │   └── utils/           # 工具类
│       └── res/                 # Android 资源
├── docs/                        # 需求、设计、架构、测试等文档
├── gradle/libs.versions.toml    # 依赖版本管理
├── build.gradle.kts             # 根 Gradle 配置
└── settings.gradle.kts          # 工程模块配置
```

## 环境准备

1. 安装 Android Studio，并确保可使用 JDK 17。
2. 通过 Android Studio 的 SDK Manager 安装 Android SDK Platform 36。
3. 准备一台 Android 7.0 或更高版本的真机，或创建 API 24+ 的模拟器。
4. 确保本机可以访问 `google()`、`mavenCentral()` 和 `gradlePluginPortal()` 依赖仓库。

## 复现步骤

### 1. 克隆项目

```bash
git clone <repository-url>
cd CloudStorage
```

### 2. 配置 Android SDK 路径

如果使用 Android Studio 打开项目，通常会自动生成 `local.properties`。如果需要手动创建，请在项目根目录新建 `local.properties`：

```properties
# Windows 示例
sdk.dir=C:/Users/<username>/AppData/Local/Android/Sdk

# macOS 示例
# sdk.dir=/Users/<username>/Library/Android/sdk

# Linux 示例
# sdk.dir=/home/<username>/Android/Sdk
```

### 3. 同步依赖

使用 Android Studio 打开项目根目录，等待 Gradle Sync 完成；也可以在命令行执行：

```bash
# Windows
.\gradlew.bat :app:dependencies

# macOS / Linux
./gradlew :app:dependencies
```

### 4. 构建 Debug APK

```bash
# Windows
.\gradlew.bat clean :app:assembleDebug

# macOS / Linux
./gradlew clean :app:assembleDebug
```

构建成功后，APK 位于：

```text
app/build/outputs/apk/debug/app-debug.apk
```

### 5. 运行 App

方式一：使用 Android Studio

1. 打开项目根目录。
2. 选择 `app` 运行配置。
3. 连接真机或启动模拟器。
4. 点击 Run。

方式二：使用命令行安装到已连接设备

```bash
# Windows
.\gradlew.bat :app:installDebug

# macOS / Linux
./gradlew :app:installDebug
```

如果连接了多台设备，请先通过 `adb devices` 确认目标设备。

## Mock 数据说明

项目默认从以下文件读取网盘文件列表和容量信息：

```text
app/src/main/assets/mock_files.json
```

修改该文件后重新安装或清除 App 数据，即可复现不同文件列表场景。当前项目不要求启动后端服务。

如果后续接入真实后端，可新增 `FileRemoteDataSource` 的实现，并在 Repository 绑定处替换现有 Mock 数据源。


