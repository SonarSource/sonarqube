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
package org.sonar.application.process;

import org.sonar.application.config.AppSettings;
import org.sonar.process.ProcessId;
import org.sonar.process.ProcessProperties;

import java.io.File;
import java.util.Optional;

import static org.sonar.process.ProcessProperties.*;

public class CommandFactoryImpl implements CommandFactory {
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

  private final AppSettings settings;

  public CommandFactoryImpl(AppSettings settings) {
    this.settings = settings;
  }

  @Override
  public JavaCommand createEsCommand() {
    File homeDir = settings.getProps().nonNullValueAsFile(ProcessProperties.PATH_HOME);
    return newJavaCommand(ProcessId.ELASTICSEARCH, homeDir)
      .addJavaOptions("-Djava.awt.headless=true")
      .addJavaOptions(settings.getProps().nonNullValue(ProcessProperties.SEARCH_JAVA_OPTS))
      .addJavaOptions(settings.getProps().nonNullValue(ProcessProperties.SEARCH_JAVA_ADDITIONAL_OPTS))
      .setClassName("org.sonar.search.SearchServer")
      .addClasspath("./lib/common/*")
      .addClasspath("./lib/search/*");
  }

  @Override
  public JavaCommand createWebCommand(boolean leader) {
    File homeDir = settings.getProps().nonNullValueAsFile(ProcessProperties.PATH_HOME);
    JavaCommand command = newJavaCommand(ProcessId.WEB_SERVER, homeDir)
      .addJavaOptions(ProcessProperties.WEB_ENFORCED_JVM_ARGS)
      .addJavaOptions(settings.getProps().nonNullValue(ProcessProperties.WEB_JAVA_OPTS))
      .addJavaOptions(settings.getProps().nonNullValue(ProcessProperties.WEB_JAVA_ADDITIONAL_OPTS))
      // required for logback tomcat valve
      .setEnvVariable(ProcessProperties.PATH_LOGS, settings.getProps().nonNullValue(ProcessProperties.PATH_LOGS))
      .setArgument("sonar.cluster.web.startupLeader", Boolean.toString(leader))
      .setClassName("org.sonar.server.app.WebServer")
      .addClasspath("./lib/common/*")
      .addClasspath("./lib/server/*");
    String driverPath = settings.getProps().value(ProcessProperties.JDBC_DRIVER_PATH);
    if (driverPath != null) {
      command.addClasspath(driverPath);
    }
    return command;
  }

  @Override
  public JavaCommand createCeCommand() {
    File homeDir = settings.getProps().nonNullValueAsFile(ProcessProperties.PATH_HOME);
    JavaCommand command = newJavaCommand(ProcessId.COMPUTE_ENGINE, homeDir)
      .addJavaOptions(ProcessProperties.CE_ENFORCED_JVM_ARGS)
      .addJavaOptions(settings.getProps().nonNullValue(ProcessProperties.CE_JAVA_OPTS))
      .addJavaOptions(settings.getProps().nonNullValue(ProcessProperties.CE_JAVA_ADDITIONAL_OPTS))
      .setClassName("org.sonar.ce.app.CeServer")
      .addClasspath("./lib/common/*")
      .addClasspath("./lib/server/*")
      .addClasspath("./lib/ce/*");
    String driverPath = settings.getProps().value(ProcessProperties.JDBC_DRIVER_PATH);
    if (driverPath != null) {
      command.addClasspath(driverPath);
    }
    return command;
  }

  private JavaCommand newJavaCommand(ProcessId id, File homeDir) {
    JavaCommand command = new JavaCommand(id)
      .setWorkDir(homeDir)
      .setArguments(settings.getProps().rawProperties());

    for (String key : PROXY_PROPERTY_KEYS) {
      settings.getValue(key).ifPresent(val -> command.addJavaOption("-D" + key + "=" + val));
    }

    // defaults of HTTPS are the same than HTTP defaults
    setSystemPropertyToDefaultIfNotSet(command, HTTPS_PROXY_HOST, HTTP_PROXY_HOST);
    setSystemPropertyToDefaultIfNotSet(command, HTTPS_PROXY_PORT, HTTP_PROXY_PORT);
    return command;
  }

  private void setSystemPropertyToDefaultIfNotSet(JavaCommand command,
    String httpsProperty, String httpProperty) {
    Optional<String> httpValue = settings.getValue(httpProperty);
    Optional<String> httpsValue = settings.getValue(httpsProperty);
    if (!httpsValue.isPresent() && httpValue.isPresent()) {
      command.addJavaOption("-D" + httpsProperty + "=" + httpValue.get());
    }
  }
}
