# OpenClaw 服务器部署方案

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 OpenClaw Gateway 迁移到云服务器，本地 Windows 电脑以 Node 模式连接，保持微信通道可用。

**Architecture:**
```
微信 → 服务器(Gateway) → 需要时下发命令到本地电脑(Node)
                              ↓
                         本地电脑提供 exec/文件/browser 能力
```
Gateway 在服务器 24 小时在线处理微信消息和 AI 推理；本地电脑作为 Node 提供执行环境（读写文件、运行命令、浏览器等），只在需要时被调用。

**Tech Stack:** OpenClaw Gateway + Node Host, SSH/Tailscale 组网, Linux 服务器

---

## 架构说明

### 角色分工

| 组件 | 位置 | 职责 |
|------|------|------|
| **Gateway** | 服务器 | 运行 AI Agent、处理微信消息、管理会话、定时任务 |
| **Gateway（微信通道）** | 服务器 | 绑定微信 bot，接收/回复微信消息 |
| **Node Host** | 本地电脑 | 提供 exec（执行命令）、文件读写、浏览器等能力 |
| **本地电脑** | 本地 | 可以关机，不影响微信消息接收 |

### 消息流程

```
微信用户发消息
  → 服务器 Gateway 接收（微信通道）
  → Gateway AI Agent 处理
  → 需要操作本地文件/命令时：Gateway → Node（本地电脑）执行
  → Node 返回结果
  → Gateway 回复微信消息
```

---

## 准备工作

### 服务器要求

| 项目 | 最低配置 | 推荐配置 |
|------|----------|----------|
| CPU | 2 核 | 4 核+ |
| 内存 | 4GB | 8GB+ |
| 硬盘 | 20GB | 40GB+ SSD |
| 系统 | Ubuntu 22.04 / Debian 12 | Ubuntu 24.04 |
| 网络 | 公网 IP 或 Tailscale | 两者皆备最佳 |

### 软件依赖

服务器需预装：
- Node.js ≥ 22（推荐通过 nvm 安装）
- 如用 SSH 组网：openssh-server
- 如用 Tailscale：Tailscale 客户端

---

## 方案 A：Tailscale 组网（推荐）

服务器和本地电脑都加入同一个 Tailscale 网络，Gateway 绑在 tailnet 地址上，Node 直接通过 Tailscale IP 连接。

### 步骤 1：服务器安装 Tailscale

```bash
# 安装 Tailscale
curl -fsSL https://tailscale.com/install.sh | sh

# 启动并登录
sudo tailscale up
```

服务器会获得一个 `100.x.x.x` 的 Tailscale IP，例如 `100.64.0.5`。

### 步骤 2：本地电脑安装 Tailscale

从 https://tailscale.com/download/windows 下载安装，登录同一账号。

本地电脑会获得另一个 Tailscale IP，例如 `100.64.0.10`。

### 步骤 3：服务器安装并配置 Gateway

```bash
# 安装 OpenClaw
npm install -g openclaw

# 交互式配置
openclaw onboard
```

核心配置 `~/.openclaw/openclaw.json`：

```json5
{
  // Gateway 绑定到 Tailscale IP
  gateway: {
    mode: "local",
    port: 18789,
    bind: "tailnet",        // 只监听 Tailscale 网络
    auth: {
      mode: "password",
      password: "your-strong-password-here"
    },
    nodes: {
      pairing: {
        // 允许本地电脑 Tailscale IP 自动批准
        autoApproveCidrs: ["100.64.0.10/32"]
      }
    }
  },

  agents: {
    defaults: {
      workspace: "~/.openclaw/workspace",
      model: {
        primary: "deepseek/deepseek-v4-pro"
      }
    }
  },

  // 微信通道（在服务器上运行）
  channels: {
    // 保留你现有的微信配置
  }
}
```

> ⚠️ `autoApproveCidrs` 仅对首次配对、无 scope 请求的 `role: node` 设备生效。升级 scope 仍需手动批准。

### 步骤 4：服务器启动 Gateway

```bash
# 前台运行（测试阶段）
openclaw gateway --port 18789

# 或安装为服务（生产环境）
openclaw gateway install
openclaw gateway start
```

### 步骤 5：本地电脑安装 Node Host

在本地 Windows 电脑上：

```powershell
# 安装 OpenClaw（如果还没装）
npm install -g openclaw

# 配置 Gateway 连接信息
# 编辑 ~/.openclaw/openclaw.json
```

本地电脑配置：

```json5
{
  gateway: {
    mode: "remote",
    remote: {
      transport: "direct",
      url: "ws://100.64.0.5:18789",  // 服务器 Tailscale IP
      password: "your-strong-password-here"
    }
  },

  nodeHost: {
    browserProxy: {
      enabled: true  // 允许 Agent 通过 Node 使用浏览器
    }
  }
}
```

### 步骤 6：本地电脑启动 Node 并配对

```powershell
# 前台运行
openclaw node run --host 100.64.0.5 --port 18789

# 安装为 Windows 服务（开机自启）
openclaw node install --host 100.64.0.5 --port 18789
```

首次连接会在 Gateway 端产生一个待审批的配对请求。如果设置了 `autoApproveCidrs`，会自动批准。

手动批准（在服务器上）：

```bash
openclaw devices list
openclaw devices approve <requestId>
```

### 步骤 7：验证

```bash
# 在服务器上检查 Node 状态
openclaw nodes list --connected

# 测试：让 Agent 在本地电脑执行命令
# 在微信上发送："执行 dir D:\GitDemo 看看"
```

---

## 方案 B：SSH 隧道组网

如果不用 Tailscale，通过 SSH 隧道暴露服务器 Gateway 端口到本地。

### 架构差别

| 组件 | 方案 A (Tailscale) | 方案 B (SSH) |
|------|-------------------|--------------|
| Gateway bind | `tailnet` | `loopback`（更安全） |
| Node 连接方式 | 直连 Tailscale IP | SSH 隧道转发 + localhost |
| 额外依赖 | Tailscale | SSH server |
| 安全性 | Tailscale 网络隔离 | SSH 加密隧道 |

### 步骤差异

服务器配置（loopback + SSH）：

```json5
{
  gateway: {
    mode: "local",
    port: 18789,
    bind: "loopback",       // 只监听 127.0.0.1，外部不可直接访问
    auth: {
      mode: "password",
      password: "your-strong-password-here"
    }
  }
}
```

本地电脑建立 SSH 隧道：

```powershell
# 保持隧道常开
ssh -N -L 18789:127.0.0.1:18789 user@your-server-ip
```

本地 Node 配置：

```json5
{
  gateway: {
    mode: "remote",
    remote: {
      transport: "direct",
      url: "ws://127.0.0.1:18789",  // 走 SSH 隧道
      password: "your-strong-password-here"
    }
  }
}
```

> ⚠️ Windows 上需要类似 macOS LaunchAgent 的持久化方案。可用 `schtasks` 创建计划任务，或用 `nssm` 将 SSH 隧道注册为服务。

---

## 方案选择

| 维度 | 方案 A (Tailscale) | 方案 B (SSH) |
|------|-------------------|--------------|
| 安装复杂度 | ⭐⭐ 低 | ⭐⭐⭐ 中 |
| 维护成本 | 低（自动组网） | 中（需保持隧道） |
| 网络稳定性 | 依赖 Tailscale 中继 | 依赖 SSH 连接 |
| 安全性 | Tailscale 端到端加密 | SSH 加密隧道 |
| 本地开机自启 | Node 服务即可 | Node + SSH 隧道两个服务 |
| 适合场景 | 有 Tailscale 或愿意装 | 已有 SSH 访问 |

**推荐方案 A（Tailscale）**，配置最简单，维护成本最低。

---

## 本地电脑开关机的影响

### 本地关机时

- ✅ 微信消息正常接收（Gateway 在服务器上）
- ✅ AI 对话正常进行
- ❌ Agent 调用本地文件/命令时失败（Node 离线）
- ❌ 需要本地代码的能力不可用

### 解决方案

Agent 需要在代码中判断 Node 是否在线：

```kotlin
// Agent 逻辑层面
if (node.isOnline()) {
    // 使用 exec host=node 操作本地文件
} else {
    // 告知用户当前不可操作本地资源
}
```

或者在 Agent prompt 中说明：

```markdown
## 本地资源访问
当需要访问 D:\GitDemo 下的项目文件时，使用 exec 工具并指定 `host=node`。
如果 Node 不可用（本地电脑关机），告知用户当前无法访问本地文件。
```

---

## 执行计划

### Task 1：服务器环境准备

- [ ] **Step 1：购买/准备服务器**

选择云服务商（阿里云、腾讯云、AWS Lightsail 等），最低 2C4G Ubuntu 22.04+。

- [ ] **Step 2：安装基础环境**

```bash
# SSH 登录服务器
ssh root@<server-ip>

# 安装 Node.js（推荐 nvm）
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.1/install.sh | bash
source ~/.bashrc
nvm install 24

# 验证
node --version  # 应 ≥ v24
```

- [ ] **Step 3：安装 Tailscale（方案 A）**

```bash
curl -fsSL https://tailscale.com/install.sh | sh
sudo tailscale up
# 记下分配的 Tailscale IP
tailscale ip -4
```

- [ ] **Step 4：安装 OpenClaw**

```bash
npm install -g openclaw
openclaw --version
```

### Task 2：服务器 Gateway 配置

- [ ] **Step 5：运行 onboard 向导**

```bash
openclaw onboard
```

- [ ] **Step 6：配置微信通道**

将现有微信 bot 配置迁移到服务器。密钥等敏感信息通过 `openclaw config set` 或直接编辑 `~/.openclaw/openclaw.json` 完成。

- [ ] **Step 7：配置 Gateway bind 和 auth**

```bash
# 方案 A（Tailscale）
openclaw config set gateway.bind tailnet
openclaw config set gateway.auth.password "your-password-here"
openclaw config set gateway.auth.mode password

# 方案 B（loopback + SSH）
openclaw config set gateway.bind loopback
openclaw config set gateway.auth.password "your-password-here"
openclaw config set gateway.auth.mode password
```

- [ ] **Step 8：安装 Gateway 服务并启动**

```bash
openclaw gateway install
openclaw gateway start
openclaw gateway status
```

### Task 3：本地电脑 Node 配置

- [ ] **Step 9：配置本地 Gateway 为 remote 模式**

编辑 `~/.openclaw/openclaw.json`：

```json5
{
  gateway: {
    mode: "remote",
    remote: {
      transport: "direct",
      url: "ws://<server-tailscale-ip>:18789",
      password: "same-password-as-server"
    }
  }
}
```

- [ ] **Step 10：安装 Node 服务**

```powershell
openclaw node install --host <server-tailscale-ip> --port 18789
openclaw node start
```

- [ ] **Step 11：在服务器端批准配对**

```bash
openclaw devices list
openclaw devices approve <requestId>
```

### Task 4：验证与调整

- [ ] **Step 12：验证微信通道正常**

用微信给 bot 发消息，确认能收到并回复。

- [ ] **Step 13：验证 Node 可用**

在微信上发一条需要操作本地文件的指令，验证 Agent 能否通过 Node 执行。

```bash
# 在 Gateway 端验证
openclaw nodes list --connected
```

- [ ] **Step 14：配置自动启动**

确保服务器 Gateway 服务开机自启：

```bash
openclaw gateway status  # 确认 installed
```

本地电脑 Node 服务开机自启（Windows 上 `openclaw node install` 已注册为服务）。

- [ ] **Step 15：迁移 workspace 文件**

将现有的 `SOUL.md`、`IDENTITY.md`、`USER.md`、`TOOLS.md`、`AGENTS.md` 等配置文件复制到服务器 workspace：

```bash
scp -r ~/.openclaw/workspace/*.md user@server:~/.openclaw/workspace/
```

---

## 注意事项

1. **密码安全**：`gateway.auth.password` 不要用弱密码。可通过环境变量 `OPENCLAW_GATEWAY_PASSWORD` 注入，避免明文写在配置文件里。

2. **微信通道密钥**：从本地迁移到服务器时，注意微信 bot 的 token/aeskey 等敏感信息不要泄露。

3. **Agent workspace**：本地 `D:\GitDemo` 等路径仍然只在本地电脑上。Agent 通过 `exec host=node` 访问本地文件时，路径保持不变。

4. **本地关机**：Agent 调用 Node 前应检查连接状态。如果 Node 离线，可以提示用户。

5. **防火墙**：方案 A（Tailscale）不需要开放额外端口。方案 B（SSH）只需开放 22 端口。
