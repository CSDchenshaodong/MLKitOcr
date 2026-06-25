# 项目概览

## 基本信息

- **项目名称**：MLKitOcr
- **包名**：`com.example.mlkitocr`
- **应用类型**：Android 原生应用（Kotlin）
- **业务领域**：离线 OCR 身份证识别
- **当前版本**：1.0（versionCode 1）

## 技术栈

### 构建与环境
- **构建系统**：Gradle（Kotlin DSL）
- **Android Gradle Plugin**：9.2.1
- **Gradle Wrapper**：9.4.1
- **编译 SDK**：36（minorApiLevel 1）
- **最低支持**：API 24（Android 7.0）
- **目标 SDK**：36
- **JDK**：21（通过 foojay-resolver-convention 自动获取）
- **Kotlin**：由 AGP 管理版本
- **Java 源码/目标**：11

### 核心依赖
- **ML Kit**：text-recognition-chinese 16.0.1（离线中文 OCR）
- **CameraX**：1.6.1（camera2、core、lifecycle、view）
- **Kotlinx Coroutines**：1.8.1（异步处理）

### UI 框架
- **AppCompat**：1.6.1
- **Material Design 3**：1.10.0（DayNight NoActionBar 主题）
- **Activity KTX**：1.8.0
- **ConstraintLayout**：2.1.4
- **Edge-to-Edge**：enableEdgeToEdge() 全屏沉浸

### 测试
- **JUnit**：4.13.2（单元测试）
- **AndroidX Test JUnit**：1.1.5（仪器测试）
- **Espresso**：3.5.1（UI 测试）

### 依赖管理
- 所有版本统一在 `gradle/libs.versions.toml` 中管理
- 禁止在 `build.gradle.kts` 中硬编码版本号

## 项目架构

### 包结构
```
com.example.mlkitocr
├── MainActivity.kt          — 主界面：相机预览 + 拍照 + OCR 识别
├── camera/                  — 相机相关
│   ├── BitmapCropper.kt     — 根据取景框比例裁剪图片
│   ├── CapturedFrame.kt     — 拍照帧数据封装
│   └── IdCardOverlayView.kt — 身份证取景框自定义 View
├── mlkit/                   — ML Kit 引擎
│   └── MlKitOcrEngine.kt    — ML Kit OCR 封装（初始化和文本识别）
├── ocr/                     — OCR 通用模型
│   ├── TextBounds.kt        — 文本边界框
│   └── OcrTextLine.kt       — OCR 识别行数据
├── idcard/                  — 身份证业务逻辑
│   ├── IdCardFrontFields.kt        — 身份证正面字段数据类
│   ├── ParsedField.kt              — 解析后的单个字段
│   ├── RecognitionConfidence.kt    — 识别置信度枚举（HIGH/PARTIAL/LOW）
│   ├── IdCardFrontRecognizer.kt    — 识别流程编排（OCR → 解析 → 校验）
│   ├── IdCardFrontParser.kt        — OCR 结果解析为身份证字段
│   └── ChinaIdNumberValidator.kt   — 中国身份证号码校验
└── review/                  — 识别结果确认页
    └── ReviewActivity.kt    — 展示识别结果，支持确认或重新拍摄
```

### 架构模式
采用 **MVVM 风格** 的分层架构：
- **UI 层**：MainActivity + ReviewActivity，直接持有业务组件
- **业务层**：IdCardFrontRecognizer 编排识别流程，IdCardFrontParser 解析字段
- **引擎层**：MlKitOcrEngine 封装 ML Kit SDK 调用
- **异步模型**：Kotlin Coroutines（Main Dispatcher 更新 UI，Default Dispatcher 执行 OCR）
- **无 DI 框架**：组件通过构造函数或 `lazy` 初始化，暂未引入 Hilt/Dagger

### 识别流程
```
相机拍照 → BitmapCropper 裁剪 → MlKitOcrEngine 文本识别
→ IdCardFrontParser 字段解析 → ChinaIdNumberValidator 校验
→ 置信度评估 → ReviewActivity 确认
```

## 功能模块

| 模块 | 说明 |
|------|------|
| 相机预览 | CameraX + PreviewView，后置摄像头，实时取景框引导 |
| 拍照裁剪 | 点击拍照 → 根据 IdCardOverlayView 比例裁剪 → 输出 Bitmap |
| OCR 识别 | ML Kit 离线中文文本识别，无需网络 |
| 身份证解析 | 解析姓名、性别、民族、出生日期、住址、身份证号码 6 个字段 |
| 号码校验 | 中国身份证号码校验（校验码算法 + 格式校验） |
| 置信度评估 | HIGH / PARTIAL / LOW 三级评估，决定是否直接进入确认页 |
| 结果确认 | ReviewActivity 展示识别结果，支持确认或重拍 |

## 开发规范

### 命名规范
- **文件命名**：大驼峰，按模块后缀区分 —— `Activity`、`View`、`Parser`、`Validator`、`Engine`
- **包命名**：全小写，按功能分层 —— `camera`、`mlkit`、`ocr`、`idcard`、`review`
- **资源文件**：下划线小写 —— `activity_main.xml`、`bg_capture_hint.xml`
- **布局命名**：以 `activity_` 前缀区分页面
- **字符串资源**：`capture_`、`review_` 前缀区分功能模块

### 代码规范
- 使用 **Kotlin** 编写，遵循 Kotlin 官方代码风格（`kotlin.code.style=official`）
- `lateinit` 用于视图绑定，`lazy` 用于组件初始化
- `runCatching {}.onSuccess {}.onFailure {}` 用于错误处理
- `CoroutineScope(Job() + Dispatchers.Main)` 用于 UI 协程
- 禁止中文注释，代码自解释优先；必要时使用英文简短注释
- Activity 数量尽量少，当前仅 MainActivity + ReviewActivity

### 安全与性能
- **离线识别**：ML Kit 离线模型，无网络传输，数据安全
- **Release 包**：未开启混淆（`isMinifyEnabled = false`）
- **ProGuard 规则**：`proguard-rules.pro` 为空，按需补充 ML Kit Keep 规则
- **权限管理**：仅请求 CAMERA 权限，运行时动态申请
- **竖屏锁定**：未强制竖屏，当前以相机预览方向为准

## 开发约束规则（给 Claude Code 强制执行）
1. 所有需求必须先走 OpenSpec：需求梳理 → 提案与 Spec → 按 Spec 实现 → 归档
2. 不允许随意新增全局变量、不允许硬编码关键配置
3. 修改现有代码前先阅读原有逻辑，兼容旧逻辑不破坏性改动
4. 每完成一个功能/任务，自动补全简要注释
5. 遇到不确定需求，先提问确认，不自行脑补实现
6. 代码格式、缩进、命名完全跟随项目现有风格
7. 依赖版本统一管理在 `gradle/libs.versions.toml`，不允许在 build.gradle.kts 中硬编码

## 版本与发布规范
- 版本号规则：主版本.次版本.迭代号
- 功能迭代以 OpenSpec 归档记录为准
- 重大变更必须留存 design 设计文档

## 协作规则
- 由 Claude Code 作为专属开发代理，严格遵守本 project.md 约束
- 所有功能设计、任务拆分、编码、重构均通过 OpenSpec 流程管理
- 任何新增需求、bug 修复、代码重构都走 /openspec 流程
