# 天融信AI代码审计平台 Ubuntu 22.04 Docker 部署文档

本文档对应两个本地源码仓库 `sonarqube` 与 `sonarqube-webapp` 的 `qianduanV1.0` 分支，支持两种部署路径：

- 路径 A：在 Ubuntu 服务器上直接构建并部署，不依赖 Docker Hub
- 路径 B：本地构建镜像后推送到 Docker Hub，再到服务器拉取部署

## 1. 前置条件

- Ubuntu 22.04
- Docker Engine 24+
- Docker Compose Plugin
- Git
- Java 17+
- Node.js 20.19.1+ 或 22.15+
- `corepack`

建议数据库使用 PostgreSQL。本文默认通过 Compose 一并部署 PostgreSQL 15。

## 2. 拉取源码

```bash
export SRC_ROOT=/srv/topsec-ai-audit
mkdir -p "$SRC_ROOT"
cd "$SRC_ROOT"

git clone <你的-sonarqube-仓库地址> sonarqube
git clone <你的-sonarqube-webapp-仓库地址> sonarqube-webapp

git -C sonarqube switch qianduanV1.0
git -C sonarqube-webapp switch qianduanV1.0
```

## 3. 路径 A：在 Ubuntu 服务器上直接构建并部署

这种方式最适合你当前的情况：跳过本机构建镜像，直接把源码放到 Ubuntu 服务器，在服务器本机完成前端、后端、镜像和 Compose 启动。

进入 `sonarqube` 仓库：

```bash
cd "$SRC_ROOT/sonarqube"
cp .env.example .env
```

按需修改 `.env` 中的以下值：

- `POSTGRES_PASSWORD`
- `SONAR_CORE_SERVER_BASE_URL`
- `PLUGIN_TAG`
- `SONAR_SEARCH_JAVAOPTS`
- `SONAR_WEB_JAVAOPTS`
- `SONAR_CE_JAVAOPTS`

执行一键部署：

```bash
./scripts/release/deploy-topsec-on-server.sh
```

该脚本会自动完成：

1. 构建 `sonarqube-webapp`
2. 使用 `WEBAPP_BUILD_PATH` 构建 `sonarqube` 发行包
3. 下载中文语言包插件
4. 通过 `docker compose up -d --build` 构建应用镜像并启动 PostgreSQL 与应用

说明：

- 应用运行镜像必须使用完整 JDK，不能使用精简 JRE，否则 Elasticsearch 会因缺少 `jdk.attach`、`jdk.jlink` 模块而启动失败。

如果你希望分步执行，也可以手动运行：

```bash
./scripts/release/build-topsec-distribution.sh
./scripts/release/fetch-zh-plugin.sh
docker compose up -d --build
```

查看状态：

```bash
docker compose ps
docker compose logs -f app
```

平台默认访问地址：

```bash
http://<服务器IP>:9000
```

如果应用容器已经部署过，但日志里出现以下错误：

- `Module jdk.attach not found`
- `Module jdk.jlink not found`

请在服务器源码目录执行以下恢复命令，重建应用镜像并重启应用容器：

```bash
docker compose build --pull --no-cache app
docker compose up -d --force-recreate app
docker compose logs -f app
```

不要执行 `docker compose down -v`，避免删除数据库和 SonarQube 数据卷。

## 4. 路径 B：本地构建定制发行包与镜像

进入 `sonarqube` 仓库：

```bash
cd "$SRC_ROOT/sonarqube"
cp .env.example .env
```

按需修改 `.env` 中的以下值：

- `DOCKERHUB_NAMESPACE`
- `POSTGRES_PASSWORD`
- `SONAR_CORE_SERVER_BASE_URL`
- `PLUGIN_TAG`

说明：

- 当前脚本默认下载 `sonar-l10n-zh-plugin-26.4`。
- 当前代码基线是 SonarQube `26.5`，因此该插件版本属于“最接近的已发布版本”，如后续发布 `26.5` 对应语言包，建议直接覆盖 `PLUGIN_TAG`。

执行镜像构建：

```bash
./scripts/release/build-topsec-image.sh
```

该脚本会自动完成：

1. 构建 `sonarqube-webapp`
2. 使用 `WEBAPP_BUILD_PATH` 构建 `sonarqube` 发行包
3. 下载中文语言包插件
4. 生成并构建 Docker 镜像

## 5. 推送到 Docker Hub

先登录：

```bash
docker login
```

然后推送：

```bash
./scripts/release/push-topsec-image.sh
```

镜像名格式：

```bash
${DOCKERHUB_NAMESPACE}/${IMAGE_NAME}:${IMAGE_TAG}
```

默认示例：

```bash
your-dockerhub-namespace/topsec-ai-code-audit:qianduanV1.0
```

## 6. Ubuntu 服务器拉取现成镜像部署

在目标服务器准备部署目录：

```bash
sudo mkdir -p /opt/topsec-ai-audit
sudo chown -R "$USER":"$USER" /opt/topsec-ai-audit
cd /opt/topsec-ai-audit
```

将以下文件放到服务器：

- `docker-compose.yml`
- `.env`

也可以直接从仓库拉取：

```bash
git clone <你的-sonarqube-仓库地址> .
git switch qianduanV1.0
cp .env.example .env
```

然后拉取镜像：

```bash
docker pull ${DOCKERHUB_NAMESPACE}/${IMAGE_NAME}:${IMAGE_TAG}
```

启动：

```bash
docker compose up -d
```

查看状态：

```bash
docker compose ps
docker compose logs -f app
```

平台默认访问地址：

```bash
http://<服务器IP>:9000
```

## 7. 生产环境建议

### 7.1 调整内核参数

```bash
echo 'vm.max_map_count=524288' | sudo tee /etc/sysctl.d/99-topsec-ai-audit.conf
sudo sysctl --system
```

### 7.2 调整 `.env`

至少确认以下配置：

- `POSTGRES_PASSWORD`
- `SONAR_CORE_SERVER_BASE_URL`
- `SONAR_SEARCH_JAVAOPTS`
- `SONAR_WEB_JAVAOPTS`
- `SONAR_CE_JAVAOPTS`

### 7.3 反向代理

生产环境建议在 Nginx 或 Traefik 后暴露 80/443，并将 `SONAR_CORE_SERVER_BASE_URL` 配置为外部访问地址。

## 8. 常用运维命令

重启：

```bash
docker compose restart
```

停止：

```bash
docker compose down
```

停止并删除卷：

```bash
docker compose down -v
```

查看应用日志：

```bash
docker compose logs -f app
```

查看数据库日志：

```bash
docker compose logs -f postgres
```

## 17. 发布前核对清单

- 两个仓库均位于 `qianduanV1.0`
- `yarn install --immutable` 成功
- 前端构建成功
- `./gradlew build -x test` 成功
- 数据库连接参数正确
- `docker compose up -d --build` 或 `docker compose up -d` 可正常启动
- 访问首页后，左上角品牌、默认中文、页脚文案、主题色均符合预期
