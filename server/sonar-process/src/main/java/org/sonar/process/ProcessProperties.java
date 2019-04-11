/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * Constants shared by search, web server and app processes.
 * They are almost all the properties defined in conf/sonar.properties.
 */
public class ProcessProperties {

  public enum Property {
    JDBC_URL("sonar.jdbc.url"),
    JDBC_USERNAME("sonar.jdbc.username", ""),
    JDBC_PASSWORD("sonar.jdbc.password", ""),
    JDBC_DRIVER_PATH("sonar.jdbc.driverPath"),
    JDBC_MAX_ACTIVE("sonar.jdbc.maxActive", "60"),
    JDBC_MAX_IDLE("sonar.jdbc.maxIdle", "5"),
    JDBC_MIN_IDLE("sonar.jdbc.minIdle", "2"),
    JDBC_MAX_WAIT("sonar.jdbc.maxWait", "5000"),
    JDBC_MIN_EVICTABLE_IDLE_TIME_MILLIS("sonar.jdbc.minEvictableIdleTimeMillis", "600000"),
    JDBC_TIME_BETWEEN_EVICTION_RUNS_MILLIS("sonar.jdbc.timeBetweenEvictionRunsMillis", "30000"),
    JDBC_EMBEDDED_PORT("sonar.embeddedDatabase.port"),

    PATH_DATA("sonar.path.data", "data"),
    PATH_HOME("sonar.path.home"),
    PATH_LOGS("sonar.path.logs", "logs"),
    PATH_TEMP("sonar.path.temp", "temp"),
    PATH_WEB("sonar.path.web", "web"),

    SEARCH_HOST("sonar.search.host", InetAddress.getLoopbackAddress().getHostAddress()),
    SEARCH_PORT("sonar.search.port", "9001"),
    SEARCH_HTTP_PORT("sonar.search.httpPort"),
    SEARCH_JAVA_OPTS("sonar.search.javaOpts", "-Xms512m -Xmx512m -XX:+HeapDumpOnOutOfMemoryError"),
    SEARCH_JAVA_ADDITIONAL_OPTS("sonar.search.javaAdditionalOpts", ""),
    SEARCH_REPLICAS("sonar.search.replicas"),
    SEARCH_MINIMUM_MASTER_NODES("sonar.search.minimumMasterNodes"),
    SEARCH_INITIAL_STATE_TIMEOUT("sonar.search.initialStateTimeout"),

    WEB_JAVA_OPTS("sonar.web.javaOpts", "-Xmx512m -Xms128m -XX:+HeapDumpOnOutOfMemoryError"),
    WEB_JAVA_ADDITIONAL_OPTS("sonar.web.javaAdditionalOpts", ""),
    WEB_PORT("sonar.web.port"),

    CE_JAVA_OPTS("sonar.ce.javaOpts", "-Xmx512m -Xms128m -XX:+HeapDumpOnOutOfMemoryError"),
    CE_JAVA_ADDITIONAL_OPTS("sonar.ce.javaAdditionalOpts", ""),

    HTTP_PROXY_HOST("http.proxyHost"),
    HTTPS_PROXY_HOST("https.proxyHost"),
    HTTP_PROXY_PORT("http.proxyPort"),
    HTTPS_PROXY_PORT("https.proxyPort"),
    HTTP_PROXY_USER("http.proxyUser"),
    HTTP_PROXY_PASSWORD("http.proxyPassword"),
    HTTP_NON_PROXY_HOSTS("http.nonProxyHosts"),
    HTTP_AUTH_NLM_DOMAN("http.auth.ntlm.domain"),
    SOCKS_PROXY_HOST("socksProxyHost"),
    SOCKS_PROXY_PORT("socksProxyPort"),

    CLUSTER_ENABLED("sonar.cluster.enabled", "false"),
    CLUSTER_NODE_TYPE("sonar.cluster.node.type"),
    CLUSTER_SEARCH_HOSTS("sonar.cluster.search.hosts"),
    CLUSTER_HZ_HOSTS("sonar.cluster.hosts"),
    CLUSTER_NODE_HZ_PORT("sonar.cluster.node.port", "9003"),
    CLUSTER_NODE_HOST("sonar.cluster.node.host"),
    CLUSTER_NODE_NAME("sonar.cluster.node.name", "sonarqube-" + UUID.randomUUID().toString()),
    CLUSTER_NAME("sonar.cluster.name", "sonarqube"),
    CLUSTER_WEB_STARTUP_LEADER("sonar.cluster.web.startupLeader"),

    AUTH_JWT_SECRET("sonar.auth.jwtBase64Hs256Secret"),
    SONAR_WEB_SSO_ENABLE("sonar.web.sso.enable", "false"),
    SONAR_WEB_SSO_LOGIN_HEADER("sonar.web.sso.loginHeader", "X-Forwarded-Login"),
    SONAR_WEB_SSO_NAME_HEADER("sonar.web.sso.nameHeader", "X-Forwarded-Name"),
    SONAR_WEB_SSO_EMAIL_HEADER("sonar.web.sso.emailHeader", "X-Forwarded-Email"),
    SONAR_WEB_SSO_GROUPS_HEADER("sonar.web.sso.groupsHeader", "X-Forwarded-Groups"),
    SONAR_WEB_SSO_REFRESH_INTERVAL_IN_MINUTES("sonar.web.sso.refreshIntervalInMinutes", "5"),
    SONAR_SECURITY_REALM("sonar.security.realm"),
    SONAR_AUTHENTICATOR_IGNORE_STARTUP_FAILURE("sonar.authenticator.ignoreStartupFailure", "false"),

    SONAR_TELEMETRY_ENABLE("sonar.telemetry.enable", "true"),
    SONAR_TELEMETRY_URL("sonar.telemetry.url", "https://telemetry.sonarsource.com/sonarqube"),
    SONAR_TELEMETRY_FREQUENCY_IN_SECONDS("sonar.telemetry.frequencyInSeconds", "21600"),

    SONAR_UPDATECENTER_ACTIVATE("sonar.updatecenter.activate", "true"),

    SONARCLOUD_ENABLED("sonar.sonarcloud.enabled", "false"),
    SONARCLOUD_HOMEPAGE_URL("sonar.homepage.url", ""),
    SONAR_PRISMIC_ACCESS_TOKEN("sonar.prismic.accessToken", ""),
    SONAR_ANALYTICS_TRACKING_ID("sonar.analytics.trackingId", ""),
    ONBOARDING_TUTORIAL_SHOW_TO_NEW_USERS("sonar.onboardingTutorial.showToNewUsers", "true"),

    BITBUCKETCLOUD_APP_KEY("sonar.bitbucketcloud.appKey", "sonarcloud"),
    BITBUCKETCLOUD_ENDPOINT("sonar.bitbucketcloud.endpoint", "https://api.bitbucket.org"),

    /**
     * Used by Orchestrator to ask for shutdown of monitor process
     */
    ENABLE_STOP_COMMAND("sonar.enableStopCommand"),

    // whether the blue/green deployment of server is enabled
    BLUE_GREEN_ENABLED("sonar.blueGreenEnabled", "false");

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

  private ProcessProperties() {
    // only static stuff
  }

  public static void completeDefaults(Props props) {
    // init string properties
    for (Map.Entry<Object, Object> entry : defaults().entrySet()) {
      props.setDefault(entry.getKey().toString(), entry.getValue().toString());
    }

    fixPortIfZero(props, Property.SEARCH_HOST.getKey(), Property.SEARCH_PORT.getKey());
  }

  public static Properties defaults() {
    Properties defaults = new Properties();
    defaults.putAll(Arrays.stream(Property.values())
      .filter(Property::hasDefaultValue)
      .collect(Collectors.toMap(Property::getKey, Property::getDefaultValue)));
    return defaults;
  }

  private static void fixPortIfZero(Props props, String addressPropertyKey, String portPropertyKey) {
    String port = props.value(portPropertyKey);
    if ("0".equals(port)) {
      String address = props.nonNullValue(addressPropertyKey);
      try {
        props.set(portPropertyKey, String.valueOf(NetworkUtilsImpl.INSTANCE.getNextAvailablePort(InetAddress.getByName(address))));
      } catch (UnknownHostException e) {
        throw new IllegalStateException("Cannot resolve address [" + address + "] set by property [" + addressPropertyKey + "]", e);
      }
    }
  }
}
