# 个人备忘助手部署记录

状态：部署方案已确认，开始执行

服务器：

- IP：175.178.170.121
- 用户：ubuntu
- 系统：Ubuntu 24.04.4 LTS

## 已完成检查

- SSH 登录 `ubuntu@175.178.170.121` 成功。
- `root@175.178.170.121` 当前密钥不可登录。
- `ubuntu` 用户具备免密 sudo。
- 服务器磁盘约 40G，剩余约 33G。
- 服务器内存约 1.9G，Swap 约 1.9G。
- Docker 未安装。
- Docker Compose 未安装。
- Java 未安装。
- Node 未安装。
- Maven 未安装。
- 80 / 443 / 8081 当前未被占用。
- `/opt` 当前为空。

## 部署执行记录

### 2026-06-02 部署前确认

- 用户确认按部署方案执行。
- 用户补充要求：当前服务器一个月后到期，需要做好数据迁移准备。
- 已将迁移准备纳入部署方案：
  - 数据目录固定为 `/opt/memorandum/data`。
  - 备份目录固定为 `/opt/memorandum/backups`。
  - 部署备份脚本 `/opt/memorandum/scripts/backup.sh`。
  - 部署迁移包脚本 `/opt/memorandum/scripts/migration-package.sh`。

后续部署过程将继续追加记录。

### 2026-06-02 服务器环境准备

- Docker 官方仓库安装尝试失败：
  - Docker Hub / Docker 官方源访问出现 GPG 或连接超时问题。
  - 未安装 `docker-ce`。
- 改用 Ubuntu 24.04 仓库安装：
  - `docker.io`
  - `docker-compose-v2`
- 安装结果：
  - Docker version 29.1.3
  - Docker Compose version 2.40.3
- 创建部署目录：
  - `/opt/memorandum/app`
  - `/opt/memorandum/data`
  - `/opt/memorandum/logs`
  - `/opt/memorandum/docs`
  - `/opt/memorandum/backups`
  - `/opt/memorandum/scripts`
- 设置 `/opt/memorandum/data` 权限为 `700`。

### 2026-06-02 构建与上传

- 本地执行 `npm.cmd install`。
- 本地执行 `npm.cmd run build`，前端产物写入 `backend/src/main/resources/static`。
- 本地执行 `mvn package -DskipTests`，构建成功。
- 上传后端 Jar 到：
  - `/opt/memorandum/app/memo-assistant.jar`
- 上传部署文档到：
  - `/opt/memorandum/docs/DEPLOYMENT_PLAN.md`
  - `/opt/memorandum/docs/DEPLOYMENT_RECORD.md`
- 写入服务器运行配置：
  - `/opt/memorandum/app/Dockerfile`
  - `/opt/memorandum/app/docker-compose.yml`
- 写入迁移相关脚本：
  - `/opt/memorandum/scripts/backup.sh`
  - `/opt/memorandum/scripts/migration-package.sh`

### 2026-06-02 容器启动

- 首次构建镜像时 Docker Hub 访问超时。
- 配置 Docker registry mirror：
  - `https://mirror.ccs.tencentyun.com`
- 重新启动 Docker 后，镜像构建成功。
- 执行：
  - `cd /opt/memorandum/app && sudo docker compose up -d --build`
- 服务状态：
  - 容器名：`app-memo-1`
  - 端口：`127.0.0.1:8081->8081/tcp`
  - 状态：运行中

### 2026-06-02 部署验证中发现并修正的问题

- 首页 `/` 初始返回 401，原因是 SPA 静态入口被安全过滤链拦截。
- 修改后端安全配置：
  - `/api/auth/login` 和 `/api/auth/csrf` 放行。
  - `/api/**` 需要认证。
  - 非 API 前端路由放行，由 Vue 处理登录页和应用路由。
- 首页随后出现 500，原因是 SPA 控制器使用 `forward:/index.html` 时仍经过内部转发链路。
- 修改 SPA 控制器：
  - 直接返回 `classpath:/static/index.html`。
  - 避免内部 forward 再进入安全过滤链。
- 重新构建 Jar、上传并重启容器。

### 2026-06-02 最终验证

- 首页验证：
  - `GET http://127.0.0.1:8081/`
  - 结果：HTTP 200，返回 `text/html;charset=UTF-8`。
- 未登录 API 验证：
  - `GET http://127.0.0.1:8081/api/tasks`
  - 结果：HTTP 401，返回 `{"ok":false,"message":"请先登录"}`。
- 管理员认证验证：
  - 用户名：`admin`
  - 初始管理员强密码：已生成并交付给用户。
  - Basic Auth 验证 `/api/auth/me` 返回当前管理员。
  - 表单登录验证 `/api/auth/login` 返回 `{"ok":true,"message":"登录成功"}`，随后 `/api/auth/me` 返回当前管理员。
- 端口验证：
  - `8081` 只监听 `127.0.0.1`。
  - 未对公网开放应用端口。
- 数据目录：
  - `/opt/memorandum/data/memo.db`
- 当前访问方式：
  - 本地执行 `ssh -L 18081:127.0.0.1:8081 ubuntu@175.178.170.121`
  - 浏览器访问 `http://localhost:18081`

### 2026-06-02 迁移准备

- 已创建备份脚本：
  - `/opt/memorandum/scripts/backup.sh`
- 已创建迁移包脚本：
  - `/opt/memorandum/scripts/migration-package.sh`
- 已在最终文档同步后生成当前备份：
  - `/opt/memorandum/backups/memorandum-data-20260602-154742.tar.gz`
- 已在最终文档同步后生成当前迁移包：
  - `/opt/memorandum/backups/memorandum-migration-20260602-154742.tar.gz`
- 下载迁移包示例：
  - `scp ubuntu@175.178.170.121:/opt/memorandum/backups/memorandum-migration-20260602-154742.tar.gz .`

## 当前最终状态

- 部署完成。
- 应用运行中。
- 数据已持久化到 `/opt/memorandum/data`。
- 迁移包已准备。
- 部署方案和部署记录已落盘到服务器 `/opt/memorandum/docs`。

## 2026-06-03 时区问题修复记录

### 问题现象

- 用户在北京时间下午完成任务后，页面时间线显示为上午九点多。
- 服务器宿主机时间正常：
  - 宿主机为 `Asia/Shanghai`
  - 宿主机时间显示为 CST
- 容器内时间初始为 UTC：
  - 容器 `date` 显示 `Wed Jun 3 09:10:09 AM UTC 2026`

### 根因

- 后端完成时间、创建时间、更新时间和处理日志时间由 `LocalDateTime.now()` 生成。
- `LocalDateTime.now()` 使用 JVM 默认时区。
- Docker 容器/JVM 默认时区是 UTC。
- `spring.jackson.time-zone: Asia/Shanghai` 不会改变 JVM 默认时区，也不会改变 `LocalDateTime.now()` 的取值。
- 前端展示时间时只是将 `T` 替换为空格并截断到分钟，没有做时区转换，所以页面展示的是后端写入数据库的 UTC 本地时间字符串。

### 修复

- 更新 `/opt/memorandum/app/docker-compose.yml`：
  - 增加 `TZ=Asia/Shanghai`
  - 将 `JAVA_TOOL_OPTIONS` 改为 `-Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai`
- 重启容器后验证：
  - 宿主机时间：`Wed Jun 3 05:13:47 PM CST 2026`
  - 容器时间：`Wed Jun 3 05:13:47 PM CST 2026`
  - 容器环境变量包含 `TZ=Asia/Shanghai`
  - 容器环境变量包含 `JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai`

### 已有数据修正

- 修正前先生成备份：
  - `/opt/memorandum/backups/memorandum-data-20260603-171219.tar.gz`
- 对服务端自动生成的时间字段加 8 小时：
  - `tasks.completed_at`
  - `tasks.created_at`
  - `tasks.updated_at`
  - `tasks.reminder_sent_at`
  - `task_logs.created_at`
  - `weekly_reports.sent_at`
  - `weekly_reports.created_at`
  - `system_settings.updated_at`
  - `app_users.created_at`
  - `app_users.updated_at`
- 未调整用户手工选择的业务时间：
  - `tasks.due_at`
  - `tasks.remind_at`

### 验证结果

- 任务 1 完成时间已从 `2026-06-03T09:04:08...` 修正为 `2026-06-03T17:04:08...`。
- 任务 1 处理日志已从 `09:03/09:04` 修正为 `17:03/17:04`。
- 首页返回 HTTP 200。
- API 返回的完成记录时间为北京时间。
- 修复后生成备份：
  - `/opt/memorandum/backups/memorandum-data-20260603-171408.tar.gz`
- 修复后生成迁移包：
  - `/opt/memorandum/backups/memorandum-migration-20260603-171408.tar.gz`
- 文档同步后再次生成当前最新备份：
  - `/opt/memorandum/backups/memorandum-data-20260603-171629.tar.gz`
- 文档同步后再次生成当前最新迁移包：
  - `/opt/memorandum/backups/memorandum-migration-20260603-171629.tar.gz`
