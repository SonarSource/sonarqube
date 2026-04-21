FROM eclipse-temurin:21-jdk-jammy

ENV SONARQUBE_HOME=/opt/topsec-sonarqube \
    SONARQUBE_DATA=/opt/topsec-sonarqube/data \
    SONARQUBE_LOGS=/opt/topsec-sonarqube/logs \
    SONARQUBE_TEMP=/opt/topsec-sonarqube/temp \
    SONARQUBE_EXTENSIONS=/opt/topsec-sonarqube/extensions \
    SONARQUBE_BUNDLED_PLUGINS=/opt/topsec-bundled-plugins \
    LANG=C.UTF-8 \
    LC_ALL=C.UTF-8

RUN apt-get update \
  && apt-get install -y --no-install-recommends procps unzip wget \
  && rm -rf /var/lib/apt/lists/* \
  && groupadd --system --gid 1000 sonarqube \
  && useradd --system --uid 1000 --gid 1000 --home-dir "${SONARQUBE_HOME}" --shell /usr/sbin/nologin sonarqube

COPY build/docker/sonarqube.zip /tmp/sonarqube.zip

RUN unzip -q /tmp/sonarqube.zip -d /opt \
  && mv /opt/sonarqube-* "${SONARQUBE_HOME}" \
  && rm -f /tmp/sonarqube.zip \
  && mkdir -p "${SONARQUBE_DATA}" "${SONARQUBE_LOGS}" "${SONARQUBE_TEMP}" "${SONARQUBE_EXTENSIONS}/plugins" "${SONARQUBE_BUNDLED_PLUGINS}"

COPY build/docker/plugins/ "${SONARQUBE_BUNDLED_PLUGINS}/"
COPY docker/topsec/run.sh /usr/local/bin/topsec-sonarqube-run

RUN chmod +x /usr/local/bin/topsec-sonarqube-run \
  && chown -R sonarqube:sonarqube /usr/local/bin/topsec-sonarqube-run "${SONARQUBE_HOME}" "${SONARQUBE_BUNDLED_PLUGINS}"

USER sonarqube
WORKDIR ${SONARQUBE_HOME}

VOLUME ["/opt/topsec-sonarqube/data", "/opt/topsec-sonarqube/extensions", "/opt/topsec-sonarqube/logs"]

EXPOSE 9000

ENTRYPOINT ["/usr/local/bin/topsec-sonarqube-run"]
