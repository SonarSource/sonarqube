# 天融信AI代码审计平台 Ubuntu 22.04 部署与环境要求说明

说明：

- 本文件名保留为 `DEPLOY_UBUNTU22_QIANDUANV1.0.md`，便于延续已有交付资料命名。
- 当前这次前端重构代码实际维护分支为：
  - `sonarqube` 仓库：`qianduanV2.0`
  - `sonarqube-webapp` 仓库：`qianduanV2.0`
- 若后续上线、构建、打包、发布，请默认以 `qianduanV2.0` 为准。

## 1. 适用范围

本文档用于以下场景：

- 在 Ubuntu 22.04 服务器上直接从源码构建并部署
- 在本地构建镜像并推送到 Docker Hub，再到 Ubuntu 服务器拉取部署
- 对天融信AI代码审计平台进行日常升级、迁移、排障和环境核查

## 2. 仓库与分支要求

服务器或本地构建机上需要准备两个仓库：

- `sonarqube`
- `sonarqube-webapp`

当前要求统一切换到 `qianduanV2.0`：

```bash
git -C sonarqube switch qianduanV2.0
git -C sonarqube-webapp switch qianduanV2.0
```

## 3. 操作系统要求

推荐系统：

- Ubuntu 22.04.4 LTS 或更高的 Ubuntu 22.04.x 小版本

不建议：

- Ubuntu 20.04
- 精简容器 OS 直接承担构建任务
- 未开启长期安全更新的测试系统

基础工具建议预装：

- `git`
- `curl`
- `wget`
- `unzip`
- `tar`
- `jq`
- `ca-certificates`
- `gnupg`
- `lsb-release`

建议安装命令：

```bash
sudo apt update
sudo apt install -y git curl wget unzip tar jq ca-certificates gnupg lsb-release
```

## 4. 硬件资源要求

### 4.1 最低可用配置

- CPU：4 vCPU
- 内存：8 GB
- 磁盘：100 GB SSD

说明：

- 仅适合小规模验证、演示、功能联调
- 不适合多人并发使用、持续扫描、长期数据沉淀

### 4.2 推荐生产配置

- CPU：8 vCPU 或以上
- 内存：16 GB 或以上
- 磁盘：200 GB SSD 或以上

说明：

- Elasticsearch、Web、CE、PostgreSQL 都会占用内存
- 镜像构建时还会叠加 Node、Yarn、Gradle 的内存消耗
- 如果项目数量多、规则集大、历史数据长，建议直接 16 GB 起步

### 4.3 中大型使用场景建议

- CPU：16 vCPU 或以上
- 内存：32 GB 或以上
- 磁盘：500 GB SSD 或以上

### 4.4 磁盘规划建议

- `/var/lib/docker` 所在分区建议单独规划
- 平台数据目录建议预留独立空间
- PostgreSQL 数据、日志、镜像层、Gradle 缓存都会持续增长

建议至少关注以下目录空间：

- `/var/lib/docker`
- `/srv/topsec-ai-audit`
- `/opt/topsec-ai-audit`
- Docker volume 挂载目录

## 5. Java 要求

### 5.1 结论

当前代码基线要求 **Java 21**。

理由：

- `build.gradle` 使用了 Java toolchain 21
- 项目发行包和运行镜像已按 Java 21 路线构建
- Elasticsearch 启动依赖完整 JDK 模块，不能只用精简 JRE

### 5.2 推荐版本

- `OpenJDK 21`
- `Temurin 21`
- `Oracle JDK 21` 也可，但不作为默认建议

推荐优先：

- `eclipse-temurin:21-jdk-jammy`

### 5.3 严禁使用

- Java 17 作为当前版本运行时
- `jre` 精简镜像
- 使用 `jlink` 二次裁剪后缺少 `jdk.attach`、`jdk.jlink` 等模块的自定义 JRE

### 5.4 宿主机检查命令

```bash
java -version
javac -version
```

预期主版本应为 `21`。

## 6. Node.js、Yarn、Corepack 要求

`sonarqube-webapp/package.json` 当前要求：

- Node.js `>=20.19.1 <21` 或 `>=22.15 <23.12`
- Yarn `4.12.0`

推荐固定：

- Node.js `20.19.1`
- Corepack 启用
- Yarn 由 Corepack 接管

建议命令：

```bash
node -v
corepack enable
yarn -v
```

## 7. Docker 与 Compose 要求

### 7.1 版本建议

- Docker Engine：`24.x` 或 `25.x`
- Docker Compose Plugin：`v2.24+`

检查命令：

```bash
docker version
docker compose version
```

### 7.2 国内网络环境建议

如果服务器位于中国大陆，建议为 Docker 配置镜像加速，否则拉取基础镜像容易超时。

常见可选方案：

- 阿里云镜像加速
- 腾讯云镜像加速
- DaoCloud 镜像加速
- 企业内部镜像代理

配置文件：

```bash
sudo mkdir -p /etc/docker
sudo vi /etc/docker/daemon.json
```

参考内容：

```json
{
  "registry-mirrors": [
    "https://<你的加速地址>"
  ],
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "100m",
    "max-file": "3"
  }
}
```

重启 Docker：

```bash
sudo systemctl daemon-reload
sudo systemctl restart docker
```

### 7.3 Docker Hub 登录

如需推送镜像：

```bash
docker login
```

## 8. Gradle 国内源建议

当前项目的 `gradle-wrapper.properties` 已使用腾讯云 Gradle 镜像。

如果服务器网络较差，仍建议额外配置：

- `GRADLE_USER_HOME`
- Maven 仓库镜像
- Gradle Daemon/JVM 参数

推荐在用户目录增加 `~/.gradle/init.gradle`：

```groovy
allprojects {
  repositories {
    mavenLocal()
    maven { url 'https://maven.aliyun.com/repository/public' }
    maven { url 'https://maven.aliyun.com/repository/central' }
    mavenCentral()
    gradlePluginPortal()
  }
}
```

建议 `~/.gradle/gradle.properties`：

```properties
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.jvmargs=-Xms512m -Xmx2048m -Dfile.encoding=UTF-8
```

## 9. 数据库要求

默认部署方案使用：

- PostgreSQL 15

推荐镜像：

- `postgres:15-alpine`

建议：

- 生产环境使用独立数据卷
- 明确设置强密码
- 做好定期备份
- 与应用位于同一内网

## 10. Linux 内核参数要求

### 10.1 必须项

Elasticsearch 启动前，`vm.max_map_count` 必须满足要求。

建议值：

- `524288`

最低不要低于：

- `262144`

检查命令：

```bash
sysctl vm.max_map_count
```

设置命令：

```bash
sudo sysctl -w vm.max_map_count=524288
echo 'vm.max_map_count=524288' | sudo tee /etc/sysctl.d/99-topsec-ai-audit.conf
sudo sysctl --system
```

### 10.2 说明

如果日志出现以下内容，优先检查这里：

- `bootstrap checks failed`
- `vm.max_map_count ... is too low`
- ES 进程不断重启

## 11. 源码拉取与目录规划

推荐源码目录：

```bash
export SRC_ROOT=/srv/topsec-ai-audit
mkdir -p "$SRC_ROOT"
cd "$SRC_ROOT"
```

拉取代码：

```bash
git clone <你的-sonarqube-仓库地址> sonarqube
git clone <你的-sonarqube-webapp-仓库地址> sonarqube-webapp

git -C sonarqube switch qianduanV2.0
git -C sonarqube-webapp switch qianduanV2.0
```

## 12. `.env` 配置要求

至少应明确以下变量：

- `DOCKERHUB_NAMESPACE`
- `IMAGE_NAME`
- `IMAGE_TAG`
- `POSTGRES_PASSWORD`
- `SONAR_CORE_SERVER_BASE_URL`
- `PLUGIN_TAG`
- `SONAR_SEARCH_JAVAOPTS`
- `SONAR_WEB_JAVAOPTS`
- `SONAR_CE_JAVAOPTS`

### 12.1 `.env` 书写规范

必须注意：

- `KEY=value` 两侧不要留空格
- 不要写成 `KEY= -Xmx512m`
- 不要把 JVM 参数单独另起一行
- 含空格的值必须整体放入引号

错误示例：

```dotenv
SONAR_WEB_JAVAOPTS=
-Xmx512m
```

正确示例：

```dotenv
SONAR_WEB_JAVAOPTS=-Xmx512m -Xms256m
SONAR_CE_JAVAOPTS=-Xmx512m -Xms256m
SONAR_SEARCH_JAVAOPTS=-Xmx512m -Xms512m
```

## 13. 路径 A：服务器本机构建并部署

适用场景：

- 本地开发机环境复杂，构建脚本不稳定
- 不希望依赖 Docker Hub
- 服务器本身网络、CPU、内存条件较好

执行：

```bash
cd "$SRC_ROOT/sonarqube"
cp .env.example .env
./scripts/release/check-topsec-host.sh
./scripts/release/deploy-topsec-on-server.sh
```

该路径会自动完成：

1. 构建 `sonarqube-webapp`
2. 构建 `sonarqube` 发行包
3. 拉取中文语言包插件
4. 构建应用镜像
5. 启动 PostgreSQL 和应用容器

## 14. 路径 B：本地构建镜像后推送 Docker Hub

适用场景：

- 服务器不具备完整构建条件
- 需要多台服务器复用同一镜像
- 需要把交付件固定为镜像版本

本地构建：

```bash
cd /path/to/sonarqube
cp .env.example .env
./scripts/release/build-topsec-image.sh
```

推送镜像：

```bash
docker login
./scripts/release/push-topsec-image.sh
```

镜像示例：

```bash
${DOCKERHUB_NAMESPACE}/${IMAGE_NAME}:${IMAGE_TAG}
```

## 15. 服务器拉取镜像并部署

部署目录建议：

```bash
sudo mkdir -p /opt/topsec-ai-audit
sudo chown -R "$USER":"$USER" /opt/topsec-ai-audit
cd /opt/topsec-ai-audit
```

准备文件：

- `docker-compose.yml`
- `.env`

如果需要，也可以直接把 `sonarqube` 仓库拉到服务器，仅使用其中的部署文件：

```bash
git clone <你的-sonarqube-仓库地址> .
git switch qianduanV2.0
cp .env.example .env
```

拉镜像并启动：

```bash
docker pull ${DOCKERHUB_NAMESPACE}/${IMAGE_NAME}:${IMAGE_TAG}
./scripts/release/check-topsec-host.sh
docker compose up -d
```

查看状态：

```bash
docker compose ps
docker compose logs -f app
docker compose logs -f postgres
```

默认访问地址：

```bash
http://<服务器IP>:9000
```

## 16. 常见故障与排查

### 16.1 `.env: line xx: -Xmx512m: command not found`

原因：

- `.env` 中把 JVM 参数拆成了多行

处理：

- 修正为单行 `KEY=value`

### 16.2 `Module jdk.attach not found` 或 `Module jdk.jlink not found`

原因：

- 镜像内使用了精简 JRE

处理：

- 改为完整 `JDK 21`
- 重新构建镜像

### 16.3 ES 进程持续重启

优先检查：

1. `vm.max_map_count`
2. 容器镜像是否为完整 JDK
3. `SONAR_SEARCH_JAVAOPTS` 是否异常
4. 宿主机内存是否过低

### 16.4 页面打不开但容器在重启

排查命令：

```bash
docker compose ps
docker compose logs app --tail 200
docker inspect topsec-sonarqube --format '{{.State.ExitCode}}'
```

## 17. 常用运维命令

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

说明：

- `down -v` 会删除数据卷，生产环境谨慎执行

查看应用日志：

```bash
docker compose logs -f app
```

查看数据库日志：

```bash
docker compose logs -f postgres
```

## 18. 发布前核对清单

- 两个仓库都已切换到 `qianduanV2.0`
- `java -version` 为 21
- `node -v`、`yarn -v` 满足当前项目要求
- Docker 与 Compose 版本符合要求
- Docker 国内源已按需配置
- Gradle 国内源已按需配置
- `.env` 中 JVM 参数为单行格式
- `./scripts/release/check-topsec-host.sh` 检查通过
- `docker compose ps` 状态正常
- 首页左上角品牌、浏览器标签图标、导航配色、页脚信息均符合预期
