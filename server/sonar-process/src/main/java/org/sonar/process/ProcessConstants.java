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

/**
 * Constants shared by search, web server and monitor processes.
 * It represents more or less all the properties commented in conf/sonar.properties
 */
public interface ProcessConstants {

  String CLUSTER_ACTIVATE = "sonar.cluster.activate";
  String CLUSTER_MASTER = "sonar.cluster.master";
  String CLUSTER_MASTER_HOST = "sonar.cluster.masterHost";
  String CLUSTER_NAME = "sonar.cluster.name";
  String CLUSTER_NODE_NAME = "sonar.node.name";

  String JDBC_URL = "sonar.jdbc.url";
  String JDBC_LOGIN = "sonar.jdbc.username";
  String JDBC_PASSWORD = "sonar.jdbc.password";
  String JDBC_DRIVER_PATH = "sonar.jdbc.driverPath";
  String JDBC_MAX_ACTIVE = "sonar.jdbc.maxActive";
  String JDBC_MAX_IDLE = "sonar.jdbc.maxIdle";
  String JDBC_MIN_IDLE = "sonar.jdbc.minIdle";
  String JDBC_MAX_WAIT = "sonar.jdbc.maxWait";
  String JDBC_MIN_EVICTABLE_IDLE_TIME_MILLIS = "sonar.jdbc.minEvictableIdleTimeMillis";
  String JDBC_TIME_BETWEEN_EVICTION_RUNS_MILLIS = "sonar.jdbc.timeBetweenEvictionRunsMillis";

  String PATH_DATA = "sonar.path.data";
  String PATH_HOME = "sonar.path.home";
  String PATH_LOGS = "sonar.path.logs";
  String PATH_TEMP = "sonar.path.temp";
  String PATH_WEB = "sonar.path.web";

  String SEARCH_PORT = "sonar.search.port";
  String SEARCH_JAVA_OPTS = "sonar.search.javaOpts";
  String SEARCH_JAVA_ADDITIONAL_OPTS = "sonar.search.javaAdditionalOpts";
  String SEARCH_TYPE = "sonar.search.type";

  String WEB_JAVA_OPTS = "sonar.web.javaOpts";
  String WEB_JAVA_ADDITIONAL_OPTS = "sonar.web.javaAdditionalOpts";

  /**
   * Used by Orchestrator to ask for shutdown of monitor process
   */
  String ENABLE_STOP_COMMAND = "sonar.enableStopCommand";

  // Constants declared by the ES plugin ListUpdate (see sonar-search)
  // that are used by sonar-server
  String ES_PLUGIN_LISTUPDATE_SCRIPT_NAME = "listUpdate";
  String ES_PLUGIN_LISTUPDATE_ID_FIELD = "idField";
  String ES_PLUGIN_LISTUPDATE_ID_VALUE = "idValue";
  String ES_PLUGIN_LISTUPDATE_FIELD = "field";
  String ES_PLUGIN_LISTUPDATE_VALUE = "value";
}
