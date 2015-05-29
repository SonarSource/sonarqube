/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
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

/**
 * Constants shared by search, web server and app processes.
 * They are almost all the properties defined in conf/sonar.properties.
 */
public class ProcessProperties {

  public static final String CLUSTER_ACTIVATE = "sonar.cluster.activate";
  public static final String CLUSTER_MASTER = "sonar.cluster.master";
  public static final String CLUSTER_MASTER_HOST = "sonar.cluster.masterHost";
  public static final String CLUSTER_NAME = "sonar.cluster.name";
  public static final String CLUSTER_NODE_NAME = "sonar.node.name";

  public static final String JDBC_URL = "sonar.jdbc.url";
  public static final String JDBC_LOGIN = "sonar.jdbc.username";
  public static final String JDBC_PASSWORD = "sonar.jdbc.password";
  public static final String JDBC_DRIVER_PATH = "sonar.jdbc.driverPath";
  public static final String JDBC_MAX_ACTIVE = "sonar.jdbc.maxActive";
  public static final String JDBC_MAX_IDLE = "sonar.jdbc.maxIdle";
  public static final String JDBC_MIN_IDLE = "sonar.jdbc.minIdle";
  public static final String JDBC_MAX_WAIT = "sonar.jdbc.maxWait";
  public static final String JDBC_MIN_EVICTABLE_IDLE_TIME_MILLIS = "sonar.jdbc.minEvictableIdleTimeMillis";
  public static final String JDBC_TIME_BETWEEN_EVICTION_RUNS_MILLIS = "sonar.jdbc.timeBetweenEvictionRunsMillis";

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

  /**
   * Used by Orchestrator to ask for shutdown of monitor process
   */
  public static final String ENABLE_STOP_COMMAND = "sonar.enableStopCommand";

  // Constants declared by the ES plugin ListUpdate (see sonar-search)
  // that are used by sonar-server
  public static final String ES_PLUGIN_LISTUPDATE_SCRIPT_NAME = "listUpdate";
  public static final String ES_PLUGIN_LISTUPDATE_ID_FIELD = "idField";
  public static final String ES_PLUGIN_LISTUPDATE_ID_VALUE = "idValue";
  public static final String ES_PLUGIN_LISTUPDATE_FIELD = "field";
  public static final String ES_PLUGIN_LISTUPDATE_VALUE = "value";

  public static final String WEB_ENFORCED_JVM_ARGS = "-Djava.awt.headless=true -Dfile.encoding=UTF-8 -Djruby.management.enabled=false " +
    // jruby is slow with java 8: https://jira.sonarsource.com/browse/SONAR-6115
    "-Djruby.compile.invokedynamic=false";

  private ProcessProperties() {
    // only static stuff
  }

  public static void completeDefaults(Props props) {
    // init string properties
    for (Map.Entry<String, String> entry : defaults().entrySet()) {
      props.setDefault(entry.getKey(), entry.getValue());
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

  public static Map<String, String> defaults() {
    Map<String, String> defaults = new HashMap<>();
    defaults.put(ProcessProperties.CLUSTER_NAME, "sonarqube");
    defaults.put(ProcessProperties.CLUSTER_NODE_NAME, "sonar-" + System.currentTimeMillis());

    defaults.put(ProcessProperties.SEARCH_HOST, "127.0.0.1");
    defaults.put(ProcessProperties.SEARCH_JAVA_OPTS, "-Xmx1G -Xms256m -Xss256k -Djava.net.preferIPv4Stack=true " +
      "-XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:CMSInitiatingOccupancyFraction=75 -XX:+UseCMSInitiatingOccupancyOnly " +
      "-XX:+HeapDumpOnOutOfMemoryError");
    defaults.put(ProcessProperties.SEARCH_JAVA_ADDITIONAL_OPTS, "");

    defaults.put(ProcessProperties.WEB_JAVA_OPTS, "-Xmx768m -XX:MaxPermSize=160m -XX:+HeapDumpOnOutOfMemoryError -Djava.net.preferIPv4Stack=true");
    defaults.put(ProcessProperties.WEB_JAVA_ADDITIONAL_OPTS, "");
    defaults.put(ProcessProperties.JDBC_URL, "jdbc:h2:tcp://localhost:9092/sonar");
    defaults.put(ProcessProperties.JDBC_LOGIN, "sonar");
    defaults.put(ProcessProperties.JDBC_PASSWORD, "sonar");
    defaults.put(ProcessProperties.JDBC_MAX_ACTIVE, "50");
    defaults.put(ProcessProperties.JDBC_MAX_IDLE, "5");
    defaults.put(ProcessProperties.JDBC_MIN_IDLE, "2");
    defaults.put(ProcessProperties.JDBC_MAX_WAIT, "5000");
    defaults.put(ProcessProperties.JDBC_MIN_EVICTABLE_IDLE_TIME_MILLIS, "600000");
    defaults.put(ProcessProperties.JDBC_TIME_BETWEEN_EVICTION_RUNS_MILLIS, "30000");
    return defaults;
  }

  private static Map<String, Integer> defaultPorts() {
    Map<String, Integer> defaults = new HashMap<>();
    defaults.put(ProcessProperties.SEARCH_PORT, 9001);
    return defaults;
  }
}
