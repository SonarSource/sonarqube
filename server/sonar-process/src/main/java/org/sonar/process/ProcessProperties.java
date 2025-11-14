/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.process;

import com.google.common.collect.ImmutableSet;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.core.config.ProxyProperties;
import org.sonar.core.extension.CoreExtension;
import org.sonar.core.extension.ServiceLoaderWrapper;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_ENABLED;
import static org.sonar.process.ProcessProperties.Property.ES_PORT;
import static org.sonar.process.ProcessProperties.Property.SEARCH_HOST;
import static org.sonar.process.ProcessProperties.Property.SEARCH_PORT;

/**
 * Constants shared by search, web server and app processes.
 * They are almost all the properties defined in conf/sonar.properties.
 */
public class ProcessProperties {
  private static final String DEFAULT_FALSE = Boolean.FALSE.toString();

  private final ServiceLoaderWrapper serviceLoaderWrapper;

  public enum Property {
    JDBC_URL("sonar.jdbc.url"),
    JDBC_USERNAME("sonar.jdbc.username", ""),
    JDBC_PASSWORD("sonar.jdbc.password", ""),
    JDBC_DRIVER_PATH("sonar.jdbc.driverPath"),
    JDBC_ADDITIONAL_LIB_PATHS("sonar.jdbc.additionalLibPaths"),
    JDBC_MAX_ACTIVE("sonar.jdbc.maxActive", "60"),
    JDBC_MIN_IDLE("sonar.jdbc.minIdle", "10"),
    JDBC_MAX_WAIT("sonar.jdbc.maxWait", "8000"),
    JDBC_MAX_IDLE_TIMEOUT("sonar.jdbc.idleTimeout", "600000"),
    JDBC_MAX_KEEP_ALIVE_TIME("sonar.jdbc.keepaliveTime", "180000"),
    JDBC_MAX_LIFETIME("sonar.jdbc.maxLifetime", "1800000"),
    JDBC_VALIDATION_TIMEOUT("sonar.jdbc.validationTimeout", "5000"),

    JDBC_EMBEDDED_PORT("sonar.embeddedDatabase.port"),

    PATH_DATA("sonar.path.data", "data"),
    PATH_HOME("sonar.path.home"),
    PATH_LOGS("sonar.path.logs", "logs"),
    PATH_TEMP("sonar.path.temp", "temp"),
    PATH_WEB("sonar.path.web", "web"),

    LOG_LEVEL("sonar.log.level"),
    LOG_LEVEL_APP("sonar.log.level.app"),
    LOG_LEVEL_WEB("sonar.log.level.web"),
    LOG_LEVEL_CE("sonar.log.level.ce"),
    LOG_LEVEL_ES("sonar.log.level.es"),
    LOG_ROLLING_POLICY("sonar.log.rollingPolicy"),
    LOG_MAX_FILES("sonar.log.maxFiles"),
    LOG_CONSOLE("sonar.log.console"),
    LOG_JSON_OUTPUT("sonar.log.jsonOutput", DEFAULT_FALSE),

    SEARCH_HOST("sonar.search.host"),
    SEARCH_PORT("sonar.search.port"),
    ES_PORT("sonar.es.port"),
    SEARCH_JAVA_OPTS("sonar.search.javaOpts", "-Xmx512m -Xms512m -XX:MaxDirectMemorySize=256m -XX:+HeapDumpOnOutOfMemoryError"),
    SEARCH_JAVA_ADDITIONAL_OPTS("sonar.search.javaAdditionalOpts", ""),
    SEARCH_REPLICAS("sonar.search.replicas"),
    SEARCH_INITIAL_STATE_TIMEOUT("sonar.search.initialStateTimeout"),
    SONAR_ES_BOOTSTRAP_CHECKS_DISABLE("sonar.es.bootstrap.checks.disable"),

    WEB_HOST("sonar.web.host"),
    WEB_JAVA_OPTS("sonar.web.javaOpts", "-Xmx512m -Xms128m -XX:+HeapDumpOnOutOfMemoryError"),
    WEB_JAVA_ADDITIONAL_OPTS("sonar.web.javaAdditionalOpts", ""),
    WEB_CONTEXT("sonar.web.context"),
    WEB_PORT("sonar.web.port"),
    WEB_GRACEFUL_STOP_TIMEOUT("sonar.web.gracefulStopTimeOutInMs", "" + 4 * 60 * 1_000L),
    WEB_HTTP_MIN_THREADS("sonar.web.http.minThreads"),
    WEB_HTTP_MAX_THREADS("sonar.web.http.maxThreads"),
    WEB_HTTP_ACCEPT_COUNT("sonar.web.http.acceptCount"),
    WEB_HTTP_KEEP_ALIVE_TIMEOUT("sonar.web.http.keepAliveTimeout"),
    // The  time a user can remain idle (no activity) before the session ends.
    WEB_INACTIVE_SESSION_TIMEOUT_IN_MIN("sonar.web.sessionTimeoutInMinutes"),
    // The time a user can remain logged in, regardless of activity
    WEB_ACTIVE_SESSION_TIMEOUT_IN_MIN("sonar.web.activeSessionTimeoutInMinutes"),
    WEB_SYSTEM_PASS_CODE("sonar.web.systemPasscode"),
    WEB_ACCESSLOGS_ENABLE("sonar.web.accessLogs.enable"),
    WEB_ACCESSLOGS_PATTERN("sonar.web.accessLogs.pattern"),

    CE_JAVA_OPTS("sonar.ce.javaOpts", "-Xmx512m -Xms128m -XX:+HeapDumpOnOutOfMemoryError"),
    CE_JAVA_ADDITIONAL_OPTS("sonar.ce.javaAdditionalOpts", ""),
    CE_GRACEFUL_STOP_TIMEOUT("sonar.ce.gracefulStopTimeOutInMs", "" + 6 * 60 * 60 * 1_000L),

    HTTP_PROXY_HOST("http.proxyHost"),
    HTTPS_PROXY_HOST("https.proxyHost"),
    HTTP_PROXY_PORT("http.proxyPort"),
    HTTPS_PROXY_PORT("https.proxyPort"),
    HTTP_PROXY_USER(ProxyProperties.HTTP_PROXY_USER),
    HTTP_PROXY_PASSWORD(ProxyProperties.HTTP_PROXY_PASSWORD),
    HTTP_NON_PROXY_HOSTS("http.nonProxyHosts", "localhost|127.*|[::1]"),
    HTTP_AUTH_NTLM_DOMAIN("http.auth.ntlm.domain"),
    SOCKS_PROXY_HOST("socksProxyHost"),
    SOCKS_PROXY_PORT("socksProxyPort"),

    CLUSTER_ENABLED("sonar.cluster.enabled", DEFAULT_FALSE),
    CLUSTER_KUBERNETES("sonar.cluster.kubernetes", DEFAULT_FALSE),
    CLUSTER_NODE_TYPE("sonar.cluster.node.type"),
    CLUSTER_SEARCH_HOSTS("sonar.cluster.search.hosts"),
    CLUSTER_HZ_HOSTS("sonar.cluster.hosts"),
    CLUSTER_NODE_HZ_PORT("sonar.cluster.node.port", "9003"),
    CLUSTER_NODE_HOST("sonar.cluster.node.host"),
    CLUSTER_NODE_NAME("sonar.cluster.node.name", "sonarqube-" + UUID.randomUUID().toString()),
    CLUSTER_NAME("sonar.cluster.name", "sonarqube"),
    CLUSTER_WEB_STARTUP_LEADER("sonar.cluster.web.startupLeader"),

    CLUSTER_SEARCH_PASSWORD("sonar.cluster.search.password"),
    CLUSTER_ES_KEYSTORE("sonar.cluster.es.ssl.keystore"),
    CLUSTER_ES_TRUSTSTORE("sonar.cluster.es.ssl.truststore"),
    CLUSTER_ES_KEYSTORE_PASSWORD("sonar.cluster.es.ssl.keystorePassword"),
    CLUSTER_ES_TRUSTSTORE_PASSWORD("sonar.cluster.es.ssl.truststorePassword"),
    CLUSTER_ES_HTTP_KEYSTORE("sonar.cluster.es.http.ssl.keystore"),
    CLUSTER_ES_HTTP_KEYSTORE_PASSWORD("sonar.cluster.es.http.ssl.keystorePassword"),
    // search node only settings
    CLUSTER_ES_HOSTS("sonar.cluster.es.hosts"),
    CLUSTER_ES_DISCOVERY_SEED_HOSTS("sonar.cluster.es.discovery.seed.hosts"),
    CLUSTER_NODE_SEARCH_HOST("sonar.cluster.node.search.host"),
    CLUSTER_NODE_SEARCH_PORT("sonar.cluster.node.search.port"),
    CLUSTER_NODE_ES_HOST("sonar.cluster.node.es.host"),
    CLUSTER_NODE_ES_PORT("sonar.cluster.node.es.port"),

    AUTH_JWT_SECRET("sonar.auth.jwtBase64Hs256Secret"),
    SONAR_WEB_SSO_ENABLE("sonar.web.sso.enable", DEFAULT_FALSE),
    SONAR_WEB_SSO_LOGIN_HEADER("sonar.web.sso.loginHeader", "X-Forwarded-Login"),
    SONAR_WEB_SSO_NAME_HEADER("sonar.web.sso.nameHeader", "X-Forwarded-Name"),
    SONAR_WEB_SSO_EMAIL_HEADER("sonar.web.sso.emailHeader", "X-Forwarded-Email"),
    SONAR_WEB_SSO_GROUPS_HEADER("sonar.web.sso.groupsHeader", "X-Forwarded-Groups"),
    SONAR_WEB_SSO_REFRESH_INTERVAL_IN_MINUTES("sonar.web.sso.refreshIntervalInMinutes", "5"),
    SONAR_SECURITY_REALM("sonar.security.realm"),
    SONAR_AUTHENTICATOR_IGNORE_STARTUP_FAILURE("sonar.authenticator.ignoreStartupFailure", DEFAULT_FALSE),

    LDAP_SERVERS("ldap.servers"),
    LDAP_URL("ldap.url"),
    LDAP_BIND_DN("ldap.bindDn"),
    LDAP_BIND_PASSWORD("ldap.bindPassword"),
    LDAP_AUTHENTICATION("ldap.authentication"),
    LDAP_REALM("ldap.realm"),
    LDAP_CONTEXT_FACTORY_CLASS("ldap.contextFactoryClass"),
    LDAP_START_TLS("ldap.StartTLS"),
    LDAP_FOLLOW_REFERRALS("ldap.followReferrals"),
    LDAP_USER_BASE_DN("ldap.user.baseDn"),
    LDAP_USER_REQUEST("ldap.user.request"),
    LDAP_USER_REAL_NAME_ATTRIBUTE("ldap.user.realNameAttribute"),
    LDAP_USER_EMAIL_ATTRIBUTE("ldap.user.emailAttribute"),
    LDAP_GROUP_BASE_DN("ldap.group.baseDn"),
    LDAP_GROUP_REQUEST("ldap.group.request"),
    LDAP_GROUP_ID_ATTRIBUTE("ldap.group.idAttribute"),

    SONAR_TELEMETRY_ENABLE("sonar.telemetry.enable", "true"),
    SONAR_TELEMETRY_URL("sonar.telemetry.url", "https://telemetry.sonarsource.com/sonarqube"),
    SONAR_TELEMETRY_METRICS_URL("sonar.telemetry.metrics.url", "https://telemetry.sonarsource.com/sonarqube/metrics"),
    SONAR_TELEMETRY_FREQUENCY_IN_SECONDS("sonar.telemetry.frequencyInSeconds", "10800"),
    SONAR_TELEMETRY_COMPRESSION("sonar.telemetry.compression", "true"),

    SONAR_UPDATECENTER_ACTIVATE("sonar.updatecenter.activate", "true"),

    /**
     * Used by OrchestratorRule to ask for shutdown of monitor process
     */
    ENABLE_STOP_COMMAND("sonar.enableStopCommand"),

    AUTO_DATABASE_UPGRADE("sonar.autoDatabaseUpgrade", DEFAULT_FALSE);

    /**
     * Properties that are defined for each LDAP server from the `ldap.servers` property
     */
    public static final Set<String> MULTI_SERVER_LDAP_SETTINGS = ImmutableSet.of(
      "ldap.*.url",
      "ldap.*.bindDn",
      "ldap.*.bindPassword",
      "ldap.*.authentication",
      "ldap.*.realm",
      "ldap.*.contextFactoryClass",
      "ldap.*.StartTLS",
      "ldap.*.followReferrals",
      "ldap.*.user.baseDn",
      "ldap.*.user.request",
      "ldap.*.user.realNameAttribute",
      "ldap.*.user.emailAttribute",
      "ldap.*.group.baseDn",
      "ldap.*.group.request",
      "ldap.*.group.idAttribute");

    private final String key;
    private final String defaultValue;

    Property(String key, @Nullable String defaultValue) {
      this.key = key;
      this.defaultValue = defaultValue;
    }

    Property(String key) {
      this(key, null);
    }

    public String getKey() {
      return key;
    }

    public String getDefaultValue() {
      Objects.requireNonNull(defaultValue, "There's no default value on this property");
      return defaultValue;
    }

    public boolean hasDefaultValue() {
      return defaultValue != null;
    }
  }

  public ProcessProperties(ServiceLoaderWrapper serviceLoaderWrapper) {
    this.serviceLoaderWrapper = serviceLoaderWrapper;
  }

  public void completeDefaults(Props props) {
    // init string properties
    for (Map.Entry<Object, Object> entry : defaults().entrySet()) {
      props.setDefault(entry.getKey().toString(), entry.getValue().toString());
    }

    boolean clusterEnabled = props.valueAsBoolean(CLUSTER_ENABLED.getKey(), false);
    if (!clusterEnabled) {
      props.setDefault(SEARCH_HOST.getKey(), InetAddress.getLoopbackAddress().getHostAddress());
      props.setDefault(SEARCH_PORT.getKey(), "9001");
      fixPortIfZero(props, Property.SEARCH_HOST.getKey(), SEARCH_PORT.getKey());
      fixEsTransportPortIfNull(props);
    }
  }

  private Properties defaults() {
    Properties defaults = new Properties();
    defaults.putAll(Arrays.stream(Property.values())
      .filter(Property::hasDefaultValue)
      .collect(Collectors.toMap(Property::getKey, Property::getDefaultValue)));
    defaults.putAll(loadDefaultsFromExtensions());
    return defaults;
  }

  private Map<String, String> loadDefaultsFromExtensions() {
    Map<String, String> propertyDefaults = new HashMap<>();
    Set<CoreExtension> extensions = serviceLoaderWrapper.load();
    for (CoreExtension ext : extensions) {
      for (Map.Entry<String, String> property : ext.getExtensionProperties().entrySet()) {
        if (propertyDefaults.put(property.getKey(), property.getValue()) != null) {
          throw new IllegalStateException(format("Configuration error: property definition named '%s' found in multiple extensions.",
            property.getKey()));
        }
      }
    }

    return propertyDefaults;
  }

  private static void fixPortIfZero(Props props, String addressPropertyKey, String portPropertyKey) {
    String port = props.value(portPropertyKey);
    if ("0".equals(port)) {
      String address = props.nonNullValue(addressPropertyKey);
      int allocatedPort = NetworkUtilsImpl.INSTANCE.getNextAvailablePort(address)
        .orElseThrow(() -> new IllegalStateException("Cannot resolve address [" + address + "] set by property [" + addressPropertyKey + "]"));
      props.set(portPropertyKey, String.valueOf(allocatedPort));
    }
  }

  private static void fixEsTransportPortIfNull(Props props) {
    String port = props.value(ES_PORT.getKey());
    if (port == null) {
      int allocatedPort = NetworkUtilsImpl.INSTANCE.getNextAvailablePort(InetAddress.getLoopbackAddress().getHostAddress())
        .orElseThrow(() -> new IllegalStateException("Cannot resolve address for Elasticsearch TCP transport port"));
      props.set(ES_PORT.getKey(), String.valueOf(allocatedPort));
    }
  }

  public static long parseTimeoutMs(Property property, String value) {
    long l = Long.parseLong(value);
    checkState(l >= 1, "value of %s must be >= 1", property);
    return l;
  }
}
