# DigSwim Android App

## 简介
DigSwim 是一款专为游泳爱好者设计的数据可视化 App。本项目采用现代 Android 技术栈构建，展示了深色模式下的酷炫数据可视化。
<img width="1376" height="768" alt="image" src="https://github.com/user-attachments/assets/b7939cde-85da-44e6-9cf0-3319c054d6d0" />

![img_v3_02tl_617d0909-b9c5-426c-ba71-76cb1e49a52g](https://github.com/user-attachments/assets/2c937d99-8169-4510-b849-434b0f92868b) ![img_v3_02tl_c521cbd8-8302-41dd-9241-0139f2d99fag](https://github.com/user-attachments/assets/6ce3bdbf-e157-4acb-accb-3425083c73e7) ![img_v3_02tl_c405620b-7ac1-4aa9-bb4f-b7b0eb1400fg](https://github.com/user-attachments/assets/73aa66b1-0aaf-4c32-8a8c-746d3eda0616) ![img_v3_02tl_b1f89bf3-88d9-4807-95f9-af294cd923cg](https://github.com/user-attachments/assets/652d5c75-343e-4e65-af5c-7d358c2367ea) ![img_v3_02tl_ae414ba7-13ac-42c6-8c9f-6e4d5af2820g](https://github.com/user-attachments/assets/f8d8d656-e5b1-45f1-a26a-4488730b012d)






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
