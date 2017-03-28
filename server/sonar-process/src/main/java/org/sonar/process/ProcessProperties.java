/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Constants shared by search, web server and app processes.
 * They are almost all the properties defined in conf/sonar.properties.
 */
public class ProcessProperties {
  public static final String CLUSTER_ENABLED = "sonar.cluster.enabled";
  public static final String CLUSTER_CE_DISABLED = "sonar.cluster.ce.disabled";
  public static final String CLUSTER_SEARCH_DISABLED = "sonar.cluster.search.disabled";
  public static final String CLUSTER_SEARCH_HOSTS = "sonar.cluster.search.hosts";
  public static final String CLUSTER_WEB_DISABLED = "sonar.cluster.web.disabled";
  public static final String CLUSTER_HOSTS = "sonar.cluster.hosts";
  public static final String CLUSTER_PORT = "sonar.cluster.port";
  public static final String CLUSTER_NETWORK_INTERFACES = "sonar.cluster.networkInterfaces";
  public static final String CLUSTER_NAME = "sonar.cluster.name";
  public static final String HAZELCAST_LOG_LEVEL = "sonar.log.level.app.hazelcast";
  public static final String CLUSTER_WEB_LEADER = "sonar.cluster.web.startupLeader";
  public static final String CLUSTER_LOCALENDPOINT = "sonar.cluster.localEndPoint";

  public static final String JDBC_URL = "sonar.jdbc.url";
  public static final String JDBC_DRIVER_PATH = "sonar.jdbc.driverPath";
  public static final String JDBC_MAX_ACTIVE = "sonar.jdbc.maxActive";
  public static final String JDBC_MAX_IDLE = "sonar.jdbc.maxIdle";
  public static final String JDBC_MIN_IDLE = "sonar.jdbc.minIdle";
  public static final String JDBC_MAX_WAIT = "sonar.jdbc.maxWait";
  public static final String JDBC_MIN_EVICTABLE_IDLE_TIME_MILLIS = "sonar.jdbc.minEvictableIdleTimeMillis";
  public static final String JDBC_TIME_BETWEEN_EVICTION_RUNS_MILLIS = "sonar.jdbc.timeBetweenEvictionRunsMillis";
  public static final String JDBC_EMBEDDED_PORT = "sonar.embeddedDatabase.port";

  public static final String PATH_DATA = "sonar.path.data";
  public static final String PATH_HOME = "sonar.path.home";
  public static final String PATH_LOGS = "sonar.path.logs";
  public static final String PATH_TEMP = "sonar.path.temp";
  public static final String PATH_WEB = "sonar.path.web";

  public static final String SEARCH_HOST = "sonar.search.host";
  public static final String SEARCH_PORT = "sonar.search.port";
  public static final String SEARCH_HTTP_PORT = "sonar.search.httpPort";
  public static final String SEARCH_JAVA_OPTS = "sonar.search.javaOpts";
  public static final String SEARCH_JAVA_ADDITIONAL_OPTS = "sonar.search.javaAdditionalOpts";

  public static final String WEB_JAVA_OPTS = "sonar.web.javaOpts";

  public static final String WEB_JAVA_ADDITIONAL_OPTS = "sonar.web.javaAdditionalOpts";
  public static final String CE_JAVA_OPTS = "sonar.ce.javaOpts";
  public static final String CE_JAVA_ADDITIONAL_OPTS = "sonar.ce.javaAdditionalOpts";

  /**
   * Used by Orchestrator to ask for shutdown of monitor process
   */
  public static final String ENABLE_STOP_COMMAND = "sonar.enableStopCommand";

  public static final String WEB_ENFORCED_JVM_ARGS = "-Djava.awt.headless=true -Dfile.encoding=UTF-8";

  public static final String CE_ENFORCED_JVM_ARGS = "-Djava.awt.headless=true -Dfile.encoding=UTF-8";

  public static final String HTTP_PROXY_HOST = "http.proxyHost";
  public static final String HTTPS_PROXY_HOST = "https.proxyHost";
  public static final String HTTP_PROXY_PORT = "http.proxyPort";
  public static final String HTTPS_PROXY_PORT = "https.proxyPort";
  public static final String HTTP_PROXY_USER = "http.proxyUser";
  public static final String HTTP_PROXY_PASSWORD = "http.proxyPassword";

  private ProcessProperties() {
    // only static stuff
  }

  public static void completeDefaults(Props props) {
    // init string properties
    for (Map.Entry<Object, Object> entry : defaults().entrySet()) {
      props.setDefault(entry.getKey().toString(), entry.getValue().toString());
    }

    // init ports
    for (Map.Entry<String, Integer> entry : defaultPorts().entrySet()) {
      String key = entry.getKey();
      int port = props.valueAsInt(key, -1);
      if (port == -1) {
        // default port
        props.set(key, String.valueOf((int) entry.getValue()));
      } else if (port == 0) {
        // pick one available port
        props.set(key, String.valueOf(NetworkUtils.freePort()));
      }
    }
  }

  public static Properties defaults() {
    Properties defaults = new Properties();
    defaults.put(SEARCH_HOST, "127.0.0.1");
    defaults.put(SEARCH_JAVA_OPTS, "-Xmx1G -Xms256m -Xss256k -Djna.nosys=true " +
      "-XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:CMSInitiatingOccupancyFraction=75 -XX:+UseCMSInitiatingOccupancyOnly " +
      "-XX:+HeapDumpOnOutOfMemoryError");
    defaults.put(SEARCH_JAVA_ADDITIONAL_OPTS, "");

    defaults.put(PATH_DATA, "data");
    defaults.put(PATH_LOGS, "logs");
    defaults.put(PATH_TEMP, "temp");
    defaults.put(PATH_WEB, "web");

    defaults.put(WEB_JAVA_OPTS, "-Xmx512m -Xms128m -XX:+HeapDumpOnOutOfMemoryError");
    defaults.put(WEB_JAVA_ADDITIONAL_OPTS, "");
    defaults.put(CE_JAVA_OPTS, "-Xmx512m -Xms128m -XX:+HeapDumpOnOutOfMemoryError");
    defaults.put(CE_JAVA_ADDITIONAL_OPTS, "");
    defaults.put(JDBC_MAX_ACTIVE, "60");
    defaults.put(JDBC_MAX_IDLE, "5");
    defaults.put(JDBC_MIN_IDLE, "2");
    defaults.put(JDBC_MAX_WAIT, "5000");
    defaults.put(JDBC_MIN_EVICTABLE_IDLE_TIME_MILLIS, "600000");
    defaults.put(JDBC_TIME_BETWEEN_EVICTION_RUNS_MILLIS, "30000");

    defaults.put(CLUSTER_ENABLED, "false");
    defaults.put(CLUSTER_CE_DISABLED, "false");
    defaults.put(CLUSTER_WEB_DISABLED, "false");
    defaults.put(CLUSTER_SEARCH_DISABLED, "false");
    defaults.put(CLUSTER_NAME, "sonarqube");
    defaults.put(CLUSTER_NETWORK_INTERFACES, "");
    defaults.put(CLUSTER_HOSTS, "");
    defaults.put(CLUSTER_PORT, "9003");
    defaults.put(HAZELCAST_LOG_LEVEL, "WARN");

    return defaults;
  }

  private static Map<String, Integer> defaultPorts() {
    Map<String, Integer> defaults = new HashMap<>();
    defaults.put(SEARCH_PORT, 9001);
    return defaults;
  }
}
