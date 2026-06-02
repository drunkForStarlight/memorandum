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
