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
package org.sonar.application;

import java.io.File;
import org.sonar.process.ProcessId;
import org.sonar.process.ProcessProperties;
import org.sonar.process.Props;
import org.sonar.process.monitor.JavaCommand;

import static org.sonar.process.ProcessProperties.HTTPS_PROXY_HOST;
import static org.sonar.process.ProcessProperties.HTTPS_PROXY_PORT;
import static org.sonar.process.ProcessProperties.HTTP_PROXY_HOST;
import static org.sonar.process.ProcessProperties.HTTP_PROXY_PORT;

public class JavaCommandFactoryImpl implements JavaCommandFactory {
  /**
   * Properties about proxy that must be set as system properties
   */
  private static final String[] PROXY_PROPERTY_KEYS = new String[] {
    HTTP_PROXY_HOST,
    HTTP_PROXY_PORT,
    "http.nonProxyHosts",
    HTTPS_PROXY_HOST,
    HTTPS_PROXY_PORT,
    "http.auth.ntlm.domain",
    "socksProxyHost",
    "socksProxyPort"};

  @Override
  public JavaCommand createESCommand(Props props, File workDir) {
      return newJavaCommand(ProcessId.ELASTICSEARCH, props, workDir)
        .addJavaOptions("-Djava.awt.headless=true")
        .addJavaOptions(props.nonNullValue(ProcessProperties.SEARCH_JAVA_OPTS))
        .addJavaOptions(props.nonNullValue(ProcessProperties.SEARCH_JAVA_ADDITIONAL_OPTS))
        .setClassName("org.sonar.search.SearchServer")
        .addClasspath("./lib/common/*")
        .addClasspath("./lib/search/*");
    }

  @Override
  public JavaCommand createWebCommand(Props props, File workDir) {
      JavaCommand command = newJavaCommand(ProcessId.WEB_SERVER, props, workDir)
        .addJavaOptions(ProcessProperties.WEB_ENFORCED_JVM_ARGS)
        .addJavaOptions(props.nonNullValue(ProcessProperties.WEB_JAVA_OPTS))
        .addJavaOptions(props.nonNullValue(ProcessProperties.WEB_JAVA_ADDITIONAL_OPTS))
        // required for logback tomcat valve
        .setEnvVariable(ProcessProperties.PATH_LOGS, props.nonNullValue(ProcessProperties.PATH_LOGS))
        .setClassName("org.sonar.server.app.WebServer")
        .addClasspath("./lib/common/*")
        .addClasspath("./lib/server/*");
      String driverPath = props.value(ProcessProperties.JDBC_DRIVER_PATH);
      if (driverPath != null) {
        command.addClasspath(driverPath);
      }
      return command;
    }

  @Override
  public JavaCommand createCeCommand(Props props, File workDir) {
      JavaCommand command = newJavaCommand(ProcessId.COMPUTE_ENGINE, props, workDir)
        .addJavaOptions(ProcessProperties.CE_ENFORCED_JVM_ARGS)
        .addJavaOptions(props.nonNullValue(ProcessProperties.CE_JAVA_OPTS))
        .addJavaOptions(props.nonNullValue(ProcessProperties.CE_JAVA_ADDITIONAL_OPTS))
        .setClassName("org.sonar.ce.app.CeServer")
        .addClasspath("./lib/common/*")
        .addClasspath("./lib/server/*")
        .addClasspath("./lib/ce/*");
      String driverPath = props.value(ProcessProperties.JDBC_DRIVER_PATH);
      if (driverPath != null) {
        command.addClasspath(driverPath);
      }
      return command;
    }

  private static JavaCommand newJavaCommand(ProcessId id, Props props, File workDir) {
    JavaCommand command = new JavaCommand(id)
      .setWorkDir(workDir)
      .setArguments(props.rawProperties());

    for (String key : PROXY_PROPERTY_KEYS) {
      if (props.contains(key)) {
        command.addJavaOption("-D" + key + "=" + props.value(key));
      }
    }
    // defaults of HTTPS are the same than HTTP defaults
    setSystemPropertyToDefaultIfNotSet(command, props, HTTPS_PROXY_HOST, HTTP_PROXY_HOST);
    setSystemPropertyToDefaultIfNotSet(command, props, HTTPS_PROXY_PORT, HTTP_PROXY_PORT);
    return command;
  }

  private static void setSystemPropertyToDefaultIfNotSet(JavaCommand command, Props props, String httpsProperty, String httpProperty) {
    if (!props.contains(httpsProperty) && props.contains(httpProperty)) {
      command.addJavaOption("-D" + httpsProperty + "=" + props.value(httpProperty));
    }
  }
}
