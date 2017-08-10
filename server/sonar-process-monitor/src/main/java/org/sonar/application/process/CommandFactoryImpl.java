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

import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import org.sonar.application.config.AppSettings;
import org.sonar.process.ProcessId;
import org.sonar.process.ProcessProperties;
import org.sonar.process.logging.LogbackHelper;

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

  private final AppSettings settings;

  public CommandFactoryImpl(AppSettings settings) {
    this.settings = settings;
  }

  @Override
  public EsCommand createEsCommand(AppSettings settings) {
    File homeDir = this.settings.getProps().nonNullValueAsFile(ProcessProperties.PATH_HOME);
    File executable = new File(homeDir, getExecutable());
    if (!executable.exists()) {
      throw new IllegalStateException("Cannot find elasticsearch binary");
    }

    Map<String, String> settingsMap = new EsSettings(this.settings.getProps()).build();

    File logDir = new File(settingsMap.get("path.logs"));
    File confDir = new File(settingsMap.get("path.conf"));
    Path jvmOptionsFile = confDir.toPath().resolve("jvm.options");
    EsCommand res = new EsCommand(ProcessId.ELASTICSEARCH)
      .setWorkDir(executable.getParentFile().getParentFile())
      .setExecutable(executable)
      .setConfDir(confDir)
      .setLog4j2Properties(buildLog4j2Properties(logDir))
      .setArguments(this.settings.getProps().rawProperties())
      .setClusterName(settingsMap.get("cluster.name"))
      .setHost(settingsMap.get("network.host"))
      .setPort(Integer.valueOf(settingsMap.get("transport.tcp.port")))
      .addJvmOption(settings.getProps().nonNullValue(ProcessProperties.SEARCH_JAVA_OPTS))
      .addJvmOption(settings.getProps().nonNullValue(ProcessProperties.SEARCH_JAVA_ADDITIONAL_OPTS))
      .setJvmOptionsFile(jvmOptionsFile)
      .setEnvVariable("ES_JVM_OPTIONS", jvmOptionsFile.toString())
      .setEnvVariable("JAVA_HOME", System.getProperties().getProperty("java.home"));

    settingsMap.forEach((key, value) -> res.addEsOption("-E" + key + "=" + value));

    return res;
  }

  private Properties buildLog4j2Properties(File logDir) {
    // FIXME create a Log4jHelper which shares code with LogbackHelper to build this Properties object + not make LogbackHelper.resolveLevel public + provide correct log format, rolling policy, ...
    String logLevel = LogbackHelper.resolveLevel(settings.getProps(), "sonar.log.level", "sonar.log.level.es").toString();
    Properties log4j2Properties = new Properties();
    log4j2Properties.put("status", "error");
    log4j2Properties.put("appender.rolling.type", "RollingFile");
    log4j2Properties.put("appender.rolling.name", "rolling");
    log4j2Properties.put("appender.rolling.fileName", new File(logDir, "es.log").getAbsolutePath());
    log4j2Properties.put("appender.rolling.layout.type", "PatternLayout");
    log4j2Properties.put("appender.rolling.layout.pattern", "[%d{ISO8601}][%-5p][%-25c{1.}] %marker%.-10000m%n");
    log4j2Properties.put("appender.rolling.filePattern", "${sys:es.logs}-%d{yyyy-MM-dd}.log");
    log4j2Properties.put("appender.rolling.policies.type", "Policies");
    log4j2Properties.put("appender.rolling.policies.time.type", "TimeBasedTriggeringPolicy");
    log4j2Properties.put("appender.rolling.policies.time.interval", "1");
    log4j2Properties.put("appender.rolling.policies.time.modulate", "true");
    log4j2Properties.put("rootLogger.level", logLevel);
    log4j2Properties.put("rootLogger.appenderRef.rolling.ref", "rolling");
    return log4j2Properties;
  }

  private static String getExecutable() {
    if (System.getProperty("os.name").startsWith("Windows")) {
      return "elasticsearch/bin/elasticsearch.bat";
    }
    return "elasticsearch/bin/elasticsearch";
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
