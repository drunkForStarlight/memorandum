# 个人备忘助手部署方案

状态：已由用户确认执行

服务器：

- IP：175.178.170.121
- 登录用户：ubuntu
- 系统：Ubuntu 24.04.4 LTS
- sudo：可用
- 当前环境：空环境，未安装 Docker / Java / Node / Maven

## 目标

将当前 Spring Boot + Vue + SQLite 项目部署到服务器，提供少量用户使用的私有备忘助手服务。

部署后应满足：

- SQLite 数据持久化到服务器独立数据目录。
- 登录、待办、记录、周报、邮件配置等功能可用。
- 初始阶段不将 HTTP 登录页面直接暴露到公网。
- 通过 SSH 加密隧道访问，保证传输安全。
- 形成可复盘的部署记录。
- 因服务器约一个月后到期，必须准备可迁移的数据目录、备份脚本和迁移包脚本。

## 总体策略

采用 Docker Compose 部署。

为降低服务器构建压力，前端和后端在本地构建：

1. 本地构建 Vue 静态资源。
2. 本地构建 Spring Boot Jar。
3. 上传 Jar、Dockerfile、Compose 配置、部署文档到服务器。
4. 服务器只安装 Docker 和 Docker Compose 插件，不安装 Java / Node / Maven。

初始访问方式：

- 应用只绑定到服务器本地地址 `127.0.0.1:8081`。
- 用户通过 SSH 隧道访问。
- 本地浏览器访问 `http://localhost:18081`。

SSH 隧道示例：

```bash
ssh -L 18081:127.0.0.1:8081 ubuntu@175.178.170.121
```

说明：

- 登录流量经过 SSH 加密通道。
- 未配置域名和 HTTPS 证书前，不开放公网 8081。
- 后续如提供域名，可增加 Caddy 反向代理并开启 80/443 自动 HTTPS。

## 目录规划

部署根目录：

```text
/opt/memorandum
```

目录结构：

```text
/opt/memorandum
├── app
│   ├── memo-assistant.jar
│   ├── Dockerfile
│   └── docker-compose.yml
├── data
│   └── memo.db
├── logs
├── docs
│   ├── DEPLOYMENT_PLAN.md
│   └── DEPLOYMENT_RECORD.md
├── backups
└── scripts
    ├── backup.sh
    └── migration-package.sh
```

权限：

- `/opt/memorandum` 归属 `ubuntu:ubuntu`。
- `/opt/memorandum/data` 权限设为 `700`。
- SQLite 数据库、邮件配置和授权信息都在数据目录中，迁移时必须保留。

## Docker Compose 设计

服务只包含一个应用容器：

```yaml
services:
  memo:
    build: .
    image: memorandum/memo-assistant:latest
    restart: unless-stopped
    environment:
      TZ: Asia/Shanghai
      MEMO_DB_PATH: /data/memo.db
      MEMO_ADMIN_USERNAME: admin
      MEMO_ADMIN_PASSWORD: <部署时生成>
      MEMO_ADMIN_DISPLAY_NAME: Admin
      JAVA_TOOL_OPTIONS: "-Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai"
    volumes:
      - ../data:/data
      - ../logs:/logs
    ports:
      - "127.0.0.1:8081:8081"
```

关键点：

- 端口绑定 `127.0.0.1`，不对公网开放。
- 数据挂载到 `/opt/memorandum/data`。
- 容器和 JVM 显式使用 `Asia/Shanghai`，避免 `LocalDateTime.now()` 按 UTC 写入。
- 初始管理员密码部署时生成，不使用代码默认弱口令。
- 初始化管理员只在数据库不存在或用户不存在时生效；数据库创建后，环境变量中的初始密码不会覆盖已有用户。

## 网络与安全

初始阶段：

- 不开放公网 `8081`。
- 通过 SSH 隧道访问。
- 不安装 Caddy。
- 不申请 HTTPS 证书。

防火墙建议：

- 保留 SSH 端口 22。
- 初始无需开放 80/443/8081。
- 若接入域名，仅开放 80/443，8081 仍只监听本机。

## 迁移与备份准备

由于当前服务器约一个月后到期，部署时同时准备迁移能力：

- 主数据目录：`/opt/memorandum/data`
- 备份目录：`/opt/memorandum/backups`
- 备份脚本：`/opt/memorandum/scripts/backup.sh`
- 迁移包脚本：`/opt/memorandum/scripts/migration-package.sh`

备份脚本用途：

- 打包 `data`、部署配置和部署文档。
- 用于日常备份。

迁移包脚本用途：

- 打包 `data`、`app`、`docs`、`scripts`。
- 用于迁移到新服务器时整体搬迁。

手动生成迁移包：

```bash
/opt/memorandum/scripts/migration-package.sh
```

下载迁移包示例：

```bash
scp ubuntu@175.178.170.121:/opt/memorandum/backups/memorandum-migration-*.tar.gz .
```

新服务器恢复思路：

1. 安装 Docker 和 Docker Compose。
2. 解压迁移包到 `/opt/memorandum`。
3. 确认 `/opt/memorandum/data/memo.db` 存在。
4. 在 `/opt/memorandum/app` 执行 `sudo docker compose up -d --build`。
5. 重新建立 SSH 隧道访问。

## 部署步骤

### 1. 服务器准备

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl gnupg
```

优先安装 Docker 官方仓库和 Docker Compose 插件。
如 Docker 官方仓库或 Docker Hub 网络不可达，则使用 Ubuntu 仓库中的 `docker.io` 和 `docker-compose-v2`，并配置 Docker 镜像加速。

检查：

```bash
sudo docker --version
sudo docker compose version
```

本次实际安装结果：

- Docker 使用 Ubuntu 仓库 `docker.io`。
- Docker Compose 使用 Ubuntu 仓库 `docker-compose-v2`。
- 已配置 Docker registry mirror：`https://mirror.ccs.tencentyun.com`。

### 2. 创建部署目录

```bash
sudo mkdir -p /opt/memorandum/{app,data,logs,docs,backups,scripts}
sudo chown -R ubuntu:ubuntu /opt/memorandum
chmod 700 /opt/memorandum/data
```

### 3. 本地构建

在本地项目根目录执行：

```powershell
cd frontend
npm.cmd install
npm.cmd run build

cd ..\backend
mvn package -DskipTests
```

产物：

```text
backend/target/memo-assistant-0.1.0.jar
```

### 4. 上传文件

上传到服务器：

```text
/opt/memorandum/app/memo-assistant.jar
/opt/memorandum/app/Dockerfile
/opt/memorandum/app/docker-compose.yml
/opt/memorandum/docs/DEPLOYMENT_PLAN.md
/opt/memorandum/docs/DEPLOYMENT_RECORD.md
```

### 5. 生成管理员密码

部署时生成强密码，写入 Compose 环境变量：

```text
MEMO_ADMIN_PASSWORD=<strong-password>
```

部署记录只记录“已生成并交付给用户”，不在文档中明文长期保存密码。

### 6. 构建并启动容器

```bash
cd /opt/memorandum/app
sudo docker compose up -d --build
```

检查：

```bash
sudo docker compose ps
sudo docker compose logs -n 100 memo
curl -I http://127.0.0.1:8081
```

### 7. 本地访问验证

本地建立 SSH 隧道：

```bash
ssh -L 18081:127.0.0.1:8081 ubuntu@175.178.170.121
```

浏览器访问：

```text
http://localhost:18081
```

验证：

- 登录成功。
- 新建事项成功。
- 设置页面可打开。
- 邮件配置可保存。
- 周报页面可打开。

## 回滚方案

如果容器启动失败：

```bash
cd /opt/memorandum/app
sudo docker compose logs memo
sudo docker compose down
```

如果需要回到旧 Jar：

1. 将旧 Jar 从备份恢复到 `/opt/memorandum/app/memo-assistant.jar`。
2. 重新构建并启动：

```bash
sudo docker compose up -d --build
```

如果需要完整停止：

```bash
cd /opt/memorandum/app
sudo docker compose down
```

注意：除非用户明确要求，不删除 `/opt/memorandum/data`。

## 后续增强

- 配置域名和 Caddy HTTPS。
- 增加自动异地备份。
- 增加修改密码和用户管理页面。
- 增加健康检查 endpoint。
- 将部署流程脚本化。
