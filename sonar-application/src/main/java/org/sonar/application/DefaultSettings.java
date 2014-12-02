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
package org.sonar.application;

import org.sonar.process.NetworkUtils;
import org.sonar.process.ProcessConstants;
import org.sonar.process.Props;

import java.util.HashMap;
import java.util.Map;

class DefaultSettings {

  private DefaultSettings() {
    // only static stuff
  }

  static void init(Props props) {
    // forced property
    props.set(ProcessConstants.SEARCH_TYPE, "TRANSPORT");

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

  private static Map<String, String> defaults() {
    Map<String, String> defaults = new HashMap<String, String>();
    defaults.put(ProcessConstants.CLUSTER_NAME, "sonarqube");
    defaults.put(ProcessConstants.SEARCH_JAVA_OPTS, "-Xmx1G -Xms256m -Xss256k -Djava.net.preferIPv4Stack=true " +
      "-XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:CMSInitiatingOccupancyFraction=75 -XX:+UseCMSInitiatingOccupancyOnly " +
      "-XX:+HeapDumpOnOutOfMemoryError");
    defaults.put(ProcessConstants.SEARCH_JAVA_ADDITIONAL_OPTS, "");
    defaults.put(ProcessConstants.CLUSTER_NODE_NAME, "sonar-" + System.currentTimeMillis());
    defaults.put(ProcessConstants.WEB_JAVA_OPTS, "-Xmx768m -XX:MaxPermSize=160m -XX:+HeapDumpOnOutOfMemoryError -Djava.net.preferIPv4Stack=true");
    defaults.put(ProcessConstants.WEB_JAVA_ADDITIONAL_OPTS, "");
    defaults.put(ProcessConstants.JDBC_URL, "jdbc:h2:tcp://localhost:9092/sonar");
    defaults.put(ProcessConstants.JDBC_LOGIN, "sonar");
    defaults.put(ProcessConstants.JDBC_PASSWORD, "sonar");
    defaults.put(ProcessConstants.JDBC_MAX_ACTIVE, "50");
    defaults.put(ProcessConstants.JDBC_MAX_IDLE, "5");
    defaults.put(ProcessConstants.JDBC_MIN_IDLE, "2");
    defaults.put(ProcessConstants.JDBC_MAX_WAIT, "5000");
    defaults.put(ProcessConstants.JDBC_MIN_EVICTABLE_IDLE_TIME_MILLIS, "600000");
    defaults.put(ProcessConstants.JDBC_TIME_BETWEEN_EVICTION_RUNS_MILLIS, "30000");
    return defaults;
  }

  private static Map<String, Integer> defaultPorts() {
    Map<String, Integer> defaults = new HashMap<String, Integer>();
    defaults.put(ProcessConstants.SEARCH_PORT, 9001);
    return defaults;
  }
}
