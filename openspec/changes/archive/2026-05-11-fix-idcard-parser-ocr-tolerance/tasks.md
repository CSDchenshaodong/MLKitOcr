## 1. 数据模型更新

- [x] 1.1 `IdCardFrontParseResult` 增加 `warnings: List<String>` 字段，默认值 `emptyList()`
- [x] 1.2 `IdCardFrontParser.lowConfidence()` 工厂方法适配新字段

## 2. 锚点匹配容错改造

- [x] 2.1 实现 `fuzzyMatchAnchor()` 函数：对每个 OCR 行做前缀匹配（检测行文本是否以标签开头，返回匹配后的标签和剩余文本）
- [x] 2.2 实现编辑距离匹配作为 Level 2 回退：对短标签（2-4字）计算编辑距离 ≤1，且候选文本长度差 ≤2
- [x] 2.3 重构 `AnchorMap`：对每个锚点先用前缀匹配，失败后用编辑距离匹配，记录匹配方式（exact / prefix / fuzzy）
- [x] 2.4 对拆分标签做相邻行合并检测：如检测到 `公民身份` 和 `号码` 相邻行，合并为 `公民身份号码` 锚点

## 3. 动态行高容差

- [x] 3.1 将 `isSameRow()` 的固定 24px 容差改为 `anchor.rowHeight * 0.6f`，其中 `rowHeight = bounds.bottom - bounds.top`
- [x] 3.2 新增 `OcrTextLine` 扩展属性 `centerY` 和 `rowHeight`，避免重复计算

## 4. 字段值提取跨行搜索

- [x] 4.1 重构 `extractRightValue()`：Step 1 同行右侧搜索（使用新容差），Step 2 正下方搜索（top >= anchor.bottom, left 接近 anchor.left），Step 3 右下搜索
- [x] 4.2 处理标签与值在同一 OCR 行的情况：从模糊锚点匹配中取剩余文本作为值
- [x] 4.3 `extractBirthDate()` 改用与 `extractRightValue()` 相同的多方向搜索逻辑，并增强跨行数字重组

## 5. 地址提取修复

- [x] 5.1 重写 `extractAddress()`：移除 `left >= addressAnchor.right` 硬约束
- [x] 5.2 收集 `addressAnchor.top` 到 `idNumberAnchor.top`（或图像底部）之间所有行，按 `top` 排序合并
- [x] 5.3 排除锚点行本身和包含 `公民身份号码` 等已知标签的行

## 6. 回退策略实现

- [x] 6.1 实现 `fallbackExtract()` 函数：当 `hasKnownAnchor()` 为 false 时调用
- [x] 6.2 正则提取 18 位身份证号码（允许末尾 X/x）作为第一基准点
- [x] 6.3 以身份证号码行为基准向上推导地址区域（号码行上方与上一锚点之间的行）
- [x] 6.4 正则提取 YYYY-MM-DD 或 YYYY.MM.DD 格式出生日期
- [x] 6.5 回退产生的字段在 `warnings` 中记录相应警告信息

## 7. 测试用例更新

- [x] 7.1 新增测试：标签与值合并在一行（如 `姓名张三`）
- [x] 7.2 新增测试：标签被 OCR 拆分为两行
- [x] 7.3 新增测试：标签有单字符误识别（如 `性刃` → `性别`）
- [x] 7.4 新增测试：仅有身份证号码锚点可用时的回退提取
- [x] 7.5 新增测试：完全无锚点时的正则兜底
- [x] 7.6 新增测试：地址跨 4 行合并
- [x] 7.7 新增测试：出生日期被拆分为 3 个独立 OCR 行
- [x] 7.8 运行 `./gradlew test` 确保所有测试通过
- [x] 7.9 运行 `./gradlew assembleDebug` 确保编译通过
