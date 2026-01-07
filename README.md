# DigSwim Android App

## 简介
DigSwim 是一款专为游泳爱好者设计的数据可视化 App。本项目采用现代 Android 技术栈构建，展示了深色模式下的酷炫数据可视化。

## 技术栈
*   **语言**: Kotlin
*   **UI**: Jetpack Compose (Material 3)
*   **架构**: MVVM + Clean Architecture
*   **依赖注入**: Hilt
*   **异步处理**: Coroutines + Flow

## 如何运行
1.  使用 **Android Studio Hedgehog** 或更高版本打开本项目根目录。
2.  等待 Gradle Sync 完成。
3.  连接 Android 设备或启动模拟器。
4.  运行 `app` 模块。

## 功能预览
*   **周视图概览**: 展示每周的游泳距离、时长及每日活动趋势（迷你柱状图）。
*   **展开详情**: 点击周卡片，可展开查看该周的具体游泳记录列表。
*   **深色模式**: 默认启用深色主题，配合荧光绿高亮，符合运动极客审美。

## 目录结构
*   `app/src/main/java/com/digswim/app/ui`: UI 组件与页面
*   `app/src/main/java/com/digswim/app/data`: 数据层 (Repository)
*   `app/src/main/java/com/digswim/app/model`: 数据模型
*   `app/src/main/java/com/digswim/app/di`: Hilt 依赖注入模块
