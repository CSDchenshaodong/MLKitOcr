# MLKitOcr 📷

离线身份证识别 Android 应用，基于 Google ML Kit 实现。

## 功能

- 📸 拍照识别身份证正面信息
- 🆔 自动提取：姓名、性别、民族、出生日期、住址、公民身份号码
- ✅ 身份证号码自动校验（算法校验）
- 🔒 登录保护
- 🌐 中英文双语支持

## 技术栈

| 技术 | 用途 |
|------|------|
| Kotlin | 开发语言 |
| Jetpack Compose / XML | UI 布局 |
| Google ML Kit | OCR 文字识别 |
| Android CameraX | 相机控制 |
| Coroutines | 异步处理 |

## 快速开始

```bash
# 克隆项目
git clone https://github.com/CSDchenshaodong/MLKitOcr.git
cd MLKitOcr

# 用 Android Studio 打开项目
# 等待 Gradle 同步完成
# 连接真机（需要摄像头权限），点击 Run
```

### 系统要求

- **最低 SDK**: API 24 (Android 7.0)
- **目标 SDK**: API 36 (Android 16)
- **硬件**: 后置摄像头

## 登录

| 账号 | 密码 |
|------|------|
| `admin` | `123456` |

## 项目结构

```
MLKitOcr/
├── app/
│   └── src/main/
│       ├── java/com/example/mlkitocr/
│       │   ├── LoginActivity.kt          # 登录页面
│       │   ├── MainActivity.kt           # 相机拍照页面
│       │   ├── camera/                   # 相机工具
│       │   ├── idcard/                   # 身份证解析
│       │   │   ├── IdCardFrontParser.kt     # 字段解析器
│       │   │   ├── IdCardFrontRecognizer.kt # 识别流程
│       │   │   └── ChinaIdNumberValidator.kt# 身份证校验
│       │   ├── mlkit/                    # ML Kit OCR 引擎
│       │   ├── ocr/                      # 文本行模型
│       │   └── review/                   # 结果确认页面
│       └── res/
│           ├── values/strings.xml        # 英文
│           └── values-zh/strings.xml     # 中文
├── gradle/
└── .github/workflows/                    # CI/CD 自动化
    ├── auto-pr.yml                       # 自动创建 PR
    ├── pr-check.yml                      # PR 检查
    └── cleanup.yml                       # 合并后删分支
```

## 自动化开发

本项目使用全自动 CI/CD 流水线：

```
提需求 🗣️ → 小虾开发 🦐 → git push
    ↓
auto-pr 自动创建 PR 🤖
    ↓
CI 自动检查 ✅
    ↓
自动合并到 main 🔀
```

## License

Internal use.
