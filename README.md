# 个人备忘助手

一个私有部署的个人/小团队备忘助手系统。系统围绕“接下来要做什么”和“过去做过什么”展开，支持待办记录、提醒、完成归档、处理日志、周报生成、邮件发送和 CSV 导出。

当前版本是可运行 MVP，适合自己或少数人部署在服务器上使用。

## 功能概览

已实现：

- 登录 / 登出
- 默认管理员初始化
- 快速记录事项
- 高级新建 / 编辑事项
- 待安排事项管理
- 今日行动中心
- 计划视图：逾期、今天、未来、待安排
- 完成记录时间线
- 事项详情和处理记录
- 完成事项时填写完成总结
- 取消事项
- 邮件提醒
- 周报生成
- 周报邮件发送
- 页面配置 SMTP 邮件
- 测试邮件发送
- CSV 导出
- SQLite 存储
- Docker + Caddy 部署草案

主要页面：

- `今日`：当前最需要关注的事项，包括今天、逾期、提醒、最近完成。
- `待安排`：还没有设置时间的事项。
- `计划`：按时间组织未完成事项。
- `记录`：已完成事项和历史查询。
- `回顾`：周报和统计概览。
- `设置`：邮件发送、提醒收件人、周报时间配置。

## 技术栈

后端：

- Java 17
- Spring Boot 3.3
- Spring Web
- Spring Security
- Spring JDBC
- SQLite
- JavaMail
- Spring Scheduler

前端：

- Vue 3
- Vite
- Element Plus
- Axios

部署：

- 单体 Spring Boot Jar 托管前端静态资源
- SQLite 数据文件
- Docker Compose
- Caddy 反向代理和 HTTPS

## 目录结构

```text
.
├── backend
│   ├── pom.xml
│   └── src/main
│       ├── java/com/memoassistant
│       │   ├── auth        # 登录、用户认证
│       │   ├── common      # 通用工具、邮件服务
│       │   ├── config      # 安全、数据库、SPA 转发
│       │   ├── reports     # 周报
│       │   ├── settings    # 邮件/周报设置
│       │   └── tasks       # 事项、处理记录、提醒调度
│       └── resources
│           └── application.yml
├── frontend
│   ├── package.json
│   └── src
│       ├── api
│       ├── assets
│       ├── components
│       └── views
├── deploy
│   ├── Caddyfile
│   ├── Dockerfile
│   └── docker-compose.yml
└── README.md
```

## 本地开发

### 环境要求

- JDK 17
- Maven 3.9+
- Node.js 20+
- npm

### 启动后端

```powershell
cd backend
mvn spring-boot:run
```

后端默认端口：

```text
http://localhost:8081
```

默认数据库：

```text
backend/data/memo.db
```

也可以通过环境变量指定数据库路径：

```powershell
$env:MEMO_DB_PATH="D:\data\memo.db"
```

### 启动前端

```powershell
cd frontend
npm.cmd install
npm.cmd run dev
```

前端开发地址：

```text
http://localhost:5173
```

Vite 会把 `/api` 代理到：

```text
http://localhost:8081
```

### 默认账号

首次启动会自动创建默认管理员：

```text
用户名：admin
密码：admin123
```

生产部署前请修改默认密码。可以在首次启动前通过环境变量指定：

```powershell
$env:MEMO_ADMIN_USERNAME="admin"
$env:MEMO_ADMIN_PASSWORD="change-me"
$env:MEMO_ADMIN_DISPLAY_NAME="Admin"
```

当前版本还没有用户管理页面，默认账号创建后如需改密码，需要后续补用户设置功能或直接维护数据库。

## 构建

### 构建前端

```powershell
cd frontend
npm.cmd run build
```

前端构建产物会写入：

```text
backend/src/main/resources/static
```

这样后端 Jar 可以直接托管前端页面。

### 构建后端 Jar

```powershell
cd backend
mvn package -DskipTests
```

产物：

```text
backend/target/memo-assistant-0.1.0.jar
```

运行：

```powershell
cd backend
java -Dfile.encoding=UTF-8 -jar target/memo-assistant-0.1.0.jar
```

访问：

```text
http://localhost:8081
```

## 邮件配置

邮件配置已经支持页面配置，不需要手动写环境变量。

操作步骤：

1. 登录系统。
2. 进入 `设置`。
3. 填写 SMTP 服务器、端口、用户名、授权码、发件人。
4. 填写提醒收件人。
5. 填写周报收件人。
6. 选择周报发送星期和时间。
7. 点击 `保存设置`。
8. 点击 `发送测试邮件` 验证配置。

### 需要准备什么

你需要一个发件邮箱，并开启 SMTP 服务。多数邮箱服务商会要求使用“SMTP 授权码”或“应用专用密码”，不要直接使用登录密码。

常见配置示例：

QQ 邮箱：

```text
SMTP 服务器：smtp.qq.com
端口：465
用户名：你的QQ邮箱@qq.com
密码：SMTP 授权码
发件人：你的QQ邮箱@qq.com
需要认证：开启
STARTTLS：关闭
```

163 邮箱：

```text
SMTP 服务器：smtp.163.com
端口：465
用户名：你的邮箱@163.com
密码：SMTP 授权码
发件人：你的邮箱@163.com
需要认证：开启
STARTTLS：关闭
```

587 端口通常这样：

```text
端口：587
需要认证：开启
STARTTLS：开启
```

### 邮件发送逻辑

事项提醒：

- 后端每 60 秒扫描一次待提醒事项。
- 条件：事项未完成、未取消、设置了提醒时间、提醒时间已到、还没有发送过提醒。
- 发送成功后写入 `reminder_sent_at`，避免重复发送。

周报：

- 后端每 60 秒检查一次当前时间。
- 当星期和时间匹配页面配置时生成本周周报。
- 同一天只自动发送一次。
- 手动点击“生成本周周报”不受自动发送限制。

提醒收件人和周报收件人是分开的，可以填写多个邮箱，使用英文逗号分隔。

## 配置项

配置文件：

```text
backend/src/main/resources/application.yml
```

常用环境变量：

| 变量 | 说明 | 默认值 |
| --- | --- | --- |
| `MEMO_DB_PATH` | SQLite 数据库路径 | `./data/memo.db` |
| `MEMO_ADMIN_USERNAME` | 首次启动管理员用户名 | `admin` |
| `MEMO_ADMIN_PASSWORD` | 首次启动管理员密码 | `admin123` |
| `MEMO_ADMIN_DISPLAY_NAME` | 首次启动管理员显示名 | `Admin` |
| `SPRING_MAIL_HOST` | 首次邮件默认 SMTP 服务器 | 空 |
| `SPRING_MAIL_PORT` | 首次邮件默认 SMTP 端口 | `25` |
| `SPRING_MAIL_USERNAME` | 首次邮件默认用户名 | 空 |
| `SPRING_MAIL_PASSWORD` | 首次邮件默认密码/授权码 | 空 |
| `MEMO_MAIL_FROM` | 首次邮件默认发件人 | `memo@example.com` |
| `MEMO_WEEKLY_RECIPIENTS` | 首次周报/提醒默认收件人 | 空 |

邮件环境变量只作为首次启动写入数据库的默认值。系统运行后，以页面 `设置` 保存到 SQLite 的配置为准。

## 数据存储

当前使用 SQLite，默认数据库文件：

```text
backend/data/memo.db
```

如果用 Docker 部署，数据库挂载到：

```text
/data/memo.db
```

主要表：

- `app_users`：用户
- `tasks`：事项主体
- `task_logs`：事项处理记录
- `weekly_reports`：周报
- `system_settings`：邮件、周报等系统设置

SQLite 已启用 WAL 模式。

### 备份建议

私有部署时，建议定期备份整个数据目录：

```text
data/
```

至少包括：

```text
memo.db
memo.db-wal
memo.db-shm
```

简单备份方式：

```powershell
Copy-Item backend\data\memo.db D:\backup\memo-$(Get-Date -Format yyyyMMdd-HHmmss).db
```

更稳妥的方式是在应用停止后备份，或者后续增加在线备份接口。

## 部署

`deploy/` 目录提供了 Docker 部署草案。

### 1. 构建前端和后端

```powershell
cd frontend
npm.cmd install
npm.cmd run build

cd ..\backend
mvn package -DskipTests
```

### 2. 修改部署配置

修改：

```text
deploy/Caddyfile
```

把：

```text
memo.example.com
```

替换成你的域名。

修改：

```text
deploy/docker-compose.yml
```

至少修改默认管理员密码：

```yaml
MEMO_ADMIN_PASSWORD: change-me-before-first-run
```

### 3. 启动

```powershell
cd deploy
docker compose up -d --build
```

服务结构：

```text
浏览器
  |
Caddy / HTTPS
  |
Spring Boot 应用
  |
SQLite 数据文件
```

### 4. HTTPS

Caddy 会根据域名自动申请证书。请确保：

- 域名解析到服务器公网 IP
- 服务器开放 80 和 443
- Caddyfile 中域名正确

## 安全说明

已实现：

- Spring Security 登录
- BCrypt 密码哈希
- Session + Cookie 登录态
- CSRF 防护
- SPA CSRF token 适配
- Cookie `HttpOnly`
- SameSite `Lax`
- 后端接口鉴权

部署建议：

- 必须使用 HTTPS。
- 生产环境不要使用默认密码。
- SQLite 数据目录不要暴露到 Web 目录。
- 邮件授权码保存在 SQLite 中，服务器文件权限要管好。
- 不要把 `data/`、`*.db`、日志文件提交到版本库。
- 邮件测试时优先使用专门的通知邮箱，不建议使用私人主邮箱。

## API 概览

认证：

```text
GET  /api/auth/csrf
GET  /api/auth/me
POST /api/auth/login
POST /api/auth/logout
```

事项：

```text
GET  /api/tasks?view=all
GET  /api/tasks/{id}
POST /api/tasks
PUT  /api/tasks/{id}
POST /api/tasks/{id}/logs
POST /api/tasks/{id}/complete
POST /api/tasks/{id}/cancel
GET  /api/tasks/export.csv
```

周报：

```text
GET  /api/reports/weekly
POST /api/reports/weekly/generate
POST /api/reports/weekly/{id}/send
```

设置：

```text
GET  /api/settings/mail
PUT  /api/settings/mail
POST /api/settings/mail/test
```

## 开发说明

前端开发：

- API 客户端在 `frontend/src/api/client.js`。
- 页面入口在 `frontend/src/App.vue`。
- 全局样式在 `frontend/src/assets/styles.css`。
- 快速记录组件是 `QuickTaskCreate.vue`。
- 事项详情是 `TaskDetail.vue`。
- 高级编辑是 `TaskEditor.vue`。

后端开发：

- 启动类：`MemoAssistantApplication.java`
- 数据库初始化：`DatabaseInitializer.java`
- 安全配置：`SecurityConfig.java`
- SPA CSRF：`SpaCsrfTokenRequestHandler.java`
- 事项模块：`tasks/`
- 周报模块：`reports/`
- 设置模块：`settings/`
- 邮件发送：`MailService.java`

前端构建后会覆盖：

```text
backend/src/main/resources/static
```

如果修改前端后希望 Jar 中包含新页面，需要重新执行：

```powershell
cd frontend
npm.cmd run build

cd ..\backend
mvn package -DskipTests
```

## 当前限制

这是 MVP，还存在一些明确限制：

- 没有用户管理页面。
- 没有修改密码页面。
- 邮件授权码明文保存在本地 SQLite 中。
- 没有附件功能。
- 没有重复事项。
- 没有移动端专项优化。
- 没有完整自动化测试。
- 周报是规则模板生成，不包含 AI 总结。
- CSV 导出较基础，暂不支持 Excel/PDF。
- 邮件发送失败只记录日志，页面没有完整失败追踪。

## 常见问题

### 页面能打开，但创建事项 403

通常是旧后端进程还没重启，或者浏览器缓存了旧前端。处理：

1. 重启后端。
2. 刷新页面。
3. 必要时清理浏览器缓存。

系统已经适配 SPA CSRF，正常页面操作会自动携带 `X-XSRF-TOKEN`。

### 中文乱码

建议启动 Jar 时加：

```powershell
java -Dfile.encoding=UTF-8 -jar target/memo-assistant-0.1.0.jar
```

Docker Compose 中已经配置：

```yaml
JAVA_TOOL_OPTIONS: "-Dfile.encoding=UTF-8"
```

### 邮件发不出去

检查：

- SMTP 服务器是否正确。
- 端口是否正确。
- 是否需要 STARTTLS。
- 用户名是否是完整邮箱。
- 密码是否是 SMTP 授权码。
- 服务器是否能访问邮箱服务商 SMTP 端口。
- 发件人是否和邮箱账号匹配。

优先在 `设置` 页面点击测试邮件，再看后端日志。

### 8081 端口被占用

可以临时换端口：

```powershell
java -jar target/memo-assistant-0.1.0.jar --server.port=18081
```

如果用前端开发服务器，需要同步修改 `frontend/vite.config.js` 的代理地址。

