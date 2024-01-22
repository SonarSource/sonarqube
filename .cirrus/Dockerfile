ARG JDK_VERSION=17
FROM eclipse-temurin:${JDK_VERSION}-jdk-jammy

ENV DEBIAN_FRONTEND=noninteractive

SHELL ["/bin/bash", "-o", "pipefail", "-c"]
RUN set -o errexit -o nounset \
  && groupadd --system --gid 1000 sonarsource \
  && useradd --system --gid sonarsource --uid 1000 --shell /bin/bash --create-home sonarsource

RUN echo 'Acquire::AllowReleaseInfoChange::Suite "true";' > /etc/apt/apt.conf.d/allow_release_info_change.conf

# https://bugs.debian.org/cgi-bin/bugreport.cgi?bug=863199#23
RUN mkdir -p /usr/share/man/man1 \
  && apt-get update \
  && apt-get -y install --no-install-recommends \
    lsb-release \
    ca-certificates \
    curl \
    wget \
    gnupg \
  && apt-get clean \
  && rm -rf /var/lib/apt/lists/*

ARG NODE_MAJOR=18
RUN DISTRO="$(lsb_release -s -c)" \
  && curl -fsSL https://deb.nodesource.com/gpgkey/nodesource-repo.gpg.key | gpg --dearmor -o /etc/apt/keyrings/nodesource.gpg \
  && echo "deb [signed-by=/etc/apt/keyrings/nodesource.gpg] https://deb.nodesource.com/node_$NODE_MAJOR.x nodistro main" >> /etc/apt/sources.list.d/nodesource.list \
  && curl -sSL https://packages.atlassian.com/api/gpg/key/public | gpg --dearmor -o /etc/apt/keyrings/atlassian.gpg \
  && echo "deb [signed-by=/etc/apt/keyrings/atlassian.gpg] https://packages.atlassian.com/debian/atlassian-sdk-deb/ stable contrib" >> /etc/apt/sources.list.d/atlassian-sdk.list \
  && curl -sSL https://packages.adoptium.net/artifactory/api/gpg/key/public | gpg --dearmor -o /etc/apt/keyrings/adoptium-archive-keyring.gpg \
  && echo "deb [signed-by=/etc/apt/keyrings/adoptium-archive-keyring.gpg] https://packages.adoptium.net/artifactory/deb $DISTRO main" >> /etc/apt/sources.list.d/adoptopenjdk.list \
  && apt-get update \
  && apt-get -y install --no-install-recommends \
    git \
    unzip \
    nodejs="$NODE_MAJOR".* \
    jq \
    expect \
    atlassian-plugin-sdk \
    temurin-8-jdk \
    xmlstarlet \
  && apt-get clean \
  && rm -rf /var/lib/apt/lists/* \
  && npm install -g yarn

# Avoiding JVM Delays Caused by Random Number Generation (https://docs.oracle.com/cd/E13209_01/wlcp/wlss30/configwlss/jvmrand.html)
RUN sed -i 's|securerandom.source=file:/dev/random|securerandom.source=file:/dev/urandom|g' "$JAVA_HOME/conf/security/java.security"

USER sonarsource
WORKDIR /home/sonarsource
