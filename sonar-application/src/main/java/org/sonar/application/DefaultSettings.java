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
import org.sonar.process.Props;

class DefaultSettings {

  private DefaultSettings() {
    // only static stuff
  }

  static final String PATH_LOGS_KEY = "sonar.path.logs";

  static final String ES_PORT_KEY = "sonar.search.port";
  private static final int ES_PORT_DEFVAL = 9001;

  static final String ES_CLUSTER_NAME_KEY = "sonar.search.clusterName";
  private static final String ES_CLUSTER_NAME_DEFVAL = "sonarqube";

  static final String ES_JMX_PORT_KEY = "sonar.search.jmxPort";
  private static final int ES_JMX_PORT_DEFVAL = 9002;

  static final String ES_JAVA_OPTS_KEY = "sonar.search.javaOpts";
  private static final String ES_JAVA_OPTS_DEFVAL = "-server -Xmx256m -Xms256m -Xss256k -Djava.net.preferIPv4Stack=true " +
    "-XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:CMSInitiatingOccupancyFraction=75 -XX:+UseCMSInitiatingOccupancyOnly " +
    "-XX:+HeapDumpOnOutOfMemoryError";


  static final String WEB_JMX_PORT_KEY = "sonar.web.jmxPort";
  private static final int WEB_JMX_PORT_DEFVAL = 9003;

  static final String WEB_JAVA_OPTS_KEY = "sonar.web.javaOpts";
  private static final String WEB_JAVA_OPTS_DEFVAL = "-server -Xmx768m -XX:MaxPermSize=160m -XX:+HeapDumpOnOutOfMemoryError";
  static final String WEB_JAVA_OPTS_APPENDED_VAL = "-Djava.awt.headless=true -Dfile.encoding=UTF-8 -Djruby.management.enabled=false";

  static final String JDBC_LOGIN_KEY = "sonar.jdbc.username";
  private static final String JDBC_LOGIN_DEFVAL = "sonar";
  static final String JDBC_PASSWORD_KEY = "sonar.jdbc.password";
  private static final String JDBC_PASSWORD_DEFVAL = "sonar";

  static void initDefaults(Props props) {
    // elasticsearch
    props.set("sonar.search.type", "TRANSPORT");
    props.setDefault(DefaultSettings.ES_CLUSTER_NAME_KEY, DefaultSettings.ES_CLUSTER_NAME_DEFVAL);
    setDefaultPort(props, DefaultSettings.ES_PORT_KEY, DefaultSettings.ES_PORT_DEFVAL, "Elasticsearch");
    setDefaultPort(props, DefaultSettings.ES_JMX_PORT_KEY, DefaultSettings.ES_JMX_PORT_DEFVAL, "Elasticsearch JMX");
    props.setDefault(DefaultSettings.ES_JAVA_OPTS_KEY, DefaultSettings.ES_JAVA_OPTS_DEFVAL);

    // web
    setDefaultPort(props, DefaultSettings.WEB_JMX_PORT_KEY, DefaultSettings.WEB_JMX_PORT_DEFVAL, "HTTP Server JMX");
    props.setDefault(DefaultSettings.WEB_JAVA_OPTS_KEY, DefaultSettings.WEB_JAVA_OPTS_DEFVAL);
    props.setDefault(DefaultSettings.JDBC_LOGIN_KEY, DefaultSettings.JDBC_LOGIN_DEFVAL);
    props.setDefault(DefaultSettings.JDBC_PASSWORD_KEY, DefaultSettings.JDBC_PASSWORD_DEFVAL);
  }

  private static void setDefaultPort(Props props, String propertyKey, int defaultPort, String label) {
    int port = props.intOf(propertyKey, -1);
    if (port == -1) {
      port = defaultPort;
    } else if (port == 0) {
      port = NetworkUtils.freePort();
    }
    props.set(propertyKey, String.valueOf(port));
  }
}
