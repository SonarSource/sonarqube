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
package org.sonar.process.command;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import org.sonar.process.ProcessId;
import org.sonar.process.ProcessProperties;
import org.sonar.process.Props;
import org.sonar.process.es.EsLogging;
import org.sonar.process.es.EsSettings;

import static org.sonar.process.ProcessProperties.HTTPS_PROXY_HOST;
import static org.sonar.process.ProcessProperties.HTTPS_PROXY_PORT;
import static org.sonar.process.ProcessProperties.HTTP_PROXY_HOST;
import static org.sonar.process.ProcessProperties.HTTP_PROXY_PORT;

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

  private final Props props;

  public CommandFactoryImpl(Props props) {
    this.props = props;
  }

  @Override
  public EsCommand createEsCommand() {
    File homeDir = props.nonNullValueAsFile(ProcessProperties.PATH_HOME);
    File executable = new File(homeDir, getExecutable());
    if (!executable.exists()) {
      throw new IllegalStateException("Cannot find elasticsearch binary");
    }

    Map<String, String> settingsMap = new EsSettings(props).build();

    File logDir = new File(settingsMap.get("path.logs"));
    File confDir = new File(settingsMap.get("path.conf"));
    EsCommand res = new EsCommand(ProcessId.ELASTICSEARCH)
      .setWorkDir(executable.getParentFile().getParentFile())
      .setExecutable(executable)
      .setConfDir(confDir)
      .setLog4j2Properties(new EsLogging().createProperties(props, logDir))
      .setArguments(props.rawProperties())
      .setClusterName(settingsMap.get("cluster.name"))
      .setHost(settingsMap.get("network.host"))
      .setPort(Integer.valueOf(settingsMap.get("transport.tcp.port")))
      .addJvmOption(props.nonNullValue(ProcessProperties.SEARCH_JAVA_OPTS))
      .addJvmOption(props.nonNullValue(ProcessProperties.SEARCH_JAVA_ADDITIONAL_OPTS))
      .setEnvVariable("JAVA_HOME", System.getProperties().getProperty("java.home"));

    settingsMap.forEach((key, value) -> res.addEsOption("-E" + key + "=" + value));

    return res;
  }

  private static String getExecutable() {
    if (System.getProperty("os.name").startsWith("Windows")) {
      return "elasticsearch/bin/elasticsearch.bat";
    }
    return "elasticsearch/bin/elasticsearch";
  }

  @Override
  public JavaCommand createWebCommand(boolean leader) {
    File homeDir = props.nonNullValueAsFile(ProcessProperties.PATH_HOME);
    JavaCommand command = newJavaCommand(ProcessId.WEB_SERVER, homeDir)
      .addJavaOptions(ProcessProperties.WEB_ENFORCED_JVM_ARGS)
      .addJavaOptions(props.nonNullValue(ProcessProperties.WEB_JAVA_OPTS))
      .addJavaOptions(props.nonNullValue(ProcessProperties.WEB_JAVA_ADDITIONAL_OPTS))
      // required for logback tomcat valve
      .setEnvVariable(ProcessProperties.PATH_LOGS, props.nonNullValue(ProcessProperties.PATH_LOGS))
      .setArgument("sonar.cluster.web.startupLeader", Boolean.toString(leader))
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
  public JavaCommand createCeCommand() {
    File homeDir = props.nonNullValueAsFile(ProcessProperties.PATH_HOME);
    JavaCommand command = newJavaCommand(ProcessId.COMPUTE_ENGINE, homeDir)
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

  private JavaCommand newJavaCommand(ProcessId id, File homeDir) {
    JavaCommand command = new JavaCommand(id)
      .setWorkDir(homeDir)
      .setArguments(props.rawProperties());

    for (String key : PROXY_PROPERTY_KEYS) {
      getPropsValue(key).ifPresent(val -> command.addJavaOption("-D" + key + "=" + val));
    }

    // defaults of HTTPS are the same than HTTP defaults
    setSystemPropertyToDefaultIfNotSet(command, HTTPS_PROXY_HOST, HTTP_PROXY_HOST);
    setSystemPropertyToDefaultIfNotSet(command, HTTPS_PROXY_PORT, HTTP_PROXY_PORT);
    return command;
  }

  private void setSystemPropertyToDefaultIfNotSet(JavaCommand command,
    String httpsProperty, String httpProperty) {
    Optional<String> httpValue = getPropsValue(httpProperty);
    Optional<String> httpsValue = getPropsValue(httpsProperty);
    if (!httpsValue.isPresent() && httpValue.isPresent()) {
      command.addJavaOption("-D" + httpsProperty + "=" + httpValue.get());
    }
  }

  private Optional<String> getPropsValue(String key) {
    return Optional.ofNullable(props.value(key));
  }
}
