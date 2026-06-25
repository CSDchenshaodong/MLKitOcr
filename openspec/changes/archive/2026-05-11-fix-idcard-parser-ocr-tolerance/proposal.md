## Why

真机测试发现 `IdCardFrontParser` 只能识别身份证号码，地址显示不全，其余字段（姓名、性别、民族、出生日期）完全缺失。根因是解析器对真实 ML Kit OCR 输出的容错能力不足——锚点匹配过于严格、取值逻辑假设理想化坐标、没有回退策略。当前解析器只在精心构造的单元测试数据上工作，无法应对真实拍摄场景。

## What Changes

- 增强锚点匹配策略：支持标签被拆分、部分误识别、与值合并等真实 OCR 场景
- 放宽同行的判定条件：将固定 24px 容差改为与行高成比例的动态阈值
- 值提取支持跨行搜索：不限定必须在锚点同一水平行
- 新增回退策略：当锚点缺失时，利用身份证正面已知的固定布局和正则匹配兜底
- 地址提取逻辑修复：移除过于严苛的 left 过滤条件，改用位置区间 + 终止边界
- 出生日期提取增加跨行容错和数据重组
- **BREAKING**：`IdCardFrontParseResult` 增加 `warnings` 字段，用于传递非致命警告信息

## Capabilities

### New Capabilities

- `idcard-parser-robustness`: 身份证正面解析器的真实场景容错能力，包括模糊锚点匹配、动态行高容差、跨行值提取、回退策略和更强的地址/日期合并逻辑

### Modified Capabilities

<!-- 当前 openspec/specs/ 为空，无已有 capability 需要修改 -->

## Impact

- 修改 `IdCardFrontParser.kt`：核心解析逻辑重构
- 修改 `IdCardFrontFields.kt`：增加 `warnings` 字段
- 修改 `IdCardFrontParserTest.kt`：新增真实 OCR 场景测试用例
- 不影响 CameraX、ML Kit 引擎、Review 界面等模块
