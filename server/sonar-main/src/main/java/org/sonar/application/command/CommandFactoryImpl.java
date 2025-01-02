/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.application.command;

import java.io.File;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import org.slf4j.LoggerFactory;
import org.sonar.api.internal.MetadataLoader;
import org.sonar.api.utils.Version;
import org.sonar.application.es.EsInstallation;
import org.sonar.application.es.EsLogging;
import org.sonar.application.es.EsSettings;
import org.sonar.application.es.EsYmlSettings;
import org.sonar.process.ProcessId;
import org.sonar.process.ProcessProperties;
import org.sonar.process.Props;
import org.sonar.process.System2;

import static org.sonar.process.ProcessProperties.parseTimeoutMs;
import static org.sonar.process.ProcessProperties.Property.CE_GRACEFUL_STOP_TIMEOUT;
import static org.sonar.process.ProcessProperties.Property.CE_JAVA_ADDITIONAL_OPTS;
import static org.sonar.process.ProcessProperties.Property.CE_JAVA_OPTS;
import static org.sonar.process.ProcessProperties.Property.HTTPS_PROXY_HOST;
import static org.sonar.process.ProcessProperties.Property.HTTPS_PROXY_PORT;
import static org.sonar.process.ProcessProperties.Property.HTTP_AUTH_NTLM_DOMAIN;
import static org.sonar.process.ProcessProperties.Property.HTTP_NON_PROXY_HOSTS;
import static org.sonar.process.ProcessProperties.Property.HTTP_PROXY_HOST;
import static org.sonar.process.ProcessProperties.Property.HTTP_PROXY_PORT;
import static org.sonar.process.ProcessProperties.Property.JDBC_ADDITIONAL_LIB_PATHS;
import static org.sonar.process.ProcessProperties.Property.JDBC_DRIVER_PATH;
import static org.sonar.process.ProcessProperties.Property.PATH_HOME;
import static org.sonar.process.ProcessProperties.Property.PATH_LOGS;
import static org.sonar.process.ProcessProperties.Property.SEARCH_JAVA_ADDITIONAL_OPTS;
import static org.sonar.process.ProcessProperties.Property.SEARCH_JAVA_OPTS;
import static org.sonar.process.ProcessProperties.Property.SOCKS_PROXY_HOST;
import static org.sonar.process.ProcessProperties.Property.SOCKS_PROXY_PORT;
import static org.sonar.process.ProcessProperties.Property.WEB_GRACEFUL_STOP_TIMEOUT;
import static org.sonar.process.ProcessProperties.Property.WEB_JAVA_ADDITIONAL_OPTS;
import static org.sonar.process.ProcessProperties.Property.WEB_JAVA_OPTS;

public class CommandFactoryImpl implements CommandFactory {
  private static final String ENV_VAR_JAVA_TOOL_OPTIONS = "JAVA_TOOL_OPTIONS";
  private static final String ENV_VAR_ES_JAVA_OPTS = "ES_JAVA_OPTS";
  /**
   * Properties about proxy that must be set as system properties
   */
  private static final String[] PROXY_PROPERTY_KEYS = new String[] {
    HTTP_PROXY_HOST.getKey(),
    HTTP_PROXY_PORT.getKey(),
    HTTP_NON_PROXY_HOSTS.getKey(),
    HTTPS_PROXY_HOST.getKey(),
    HTTPS_PROXY_PORT.getKey(),
    HTTP_AUTH_NTLM_DOMAIN.getKey(),
    SOCKS_PROXY_HOST.getKey(),
    SOCKS_PROXY_PORT.getKey()};

  private static final Version SQ_VERSION = MetadataLoader.loadSQVersion(org.sonar.api.utils.System2.INSTANCE);
  private final Props props;
  private final File tempDir;

  public CommandFactoryImpl(Props props, File tempDir, System2 system2) {
    this.props = props;
    this.tempDir = tempDir;
    String javaToolOptions = system2.getenv(ENV_VAR_JAVA_TOOL_OPTIONS);
    if (javaToolOptions != null && !javaToolOptions.trim().isEmpty()) {
      LoggerFactory.getLogger(CommandFactoryImpl.class)
        .warn("JAVA_TOOL_OPTIONS is defined but will be ignored. " +
          "Use properties sonar.*.javaOpts and/or sonar.*.javaAdditionalOpts in sonar.properties to change SQ JVM processes options");
    }
    String esJavaOpts = system2.getenv(ENV_VAR_ES_JAVA_OPTS);
    if (esJavaOpts != null && !esJavaOpts.trim().isEmpty()) {
      LoggerFactory.getLogger(CommandFactoryImpl.class)
        .warn("ES_JAVA_OPTS is defined but will be ignored. " +
          "Use properties sonar.search.javaOpts and/or sonar.search.javaAdditionalOpts in sonar.properties to change SQ JVM processes options");
    }
  }

  @Override
  public JavaCommand<EsServerCliJvmOptions> createEsCommand() {
    EsInstallation esInstallation = createEsInstallation();
    String esHomeDirectoryAbsolutePath = esInstallation.getHomeDirectory().getAbsolutePath();
    return new JavaCommand<EsServerCliJvmOptions>(ProcessId.ELASTICSEARCH, esInstallation.getHomeDirectory())
      .setEsInstallation(esInstallation)
      .setJvmOptions(new EsServerCliJvmOptions(esInstallation))
      .setEnvVariable("ES_JAVA_HOME", System.getProperties().getProperty("java.home"))
      .setClassName("org.elasticsearch.launcher.CliToolLauncher")
      .addClasspath(Paths.get(esHomeDirectoryAbsolutePath, "lib").toAbsolutePath() + File.separator + "*")
      .addClasspath(Paths.get(esHomeDirectoryAbsolutePath, "lib", "cli-launcher").toAbsolutePath() + File.separator + "*");
  }

  private EsInstallation createEsInstallation() {
    EsInstallation esInstallation = new EsInstallation(props);
    Map<String, String> settingsMap = new EsSettings(props, esInstallation, System2.INSTANCE).build();

    esInstallation
      .setLog4j2Properties(new EsLogging().createProperties(props, esInstallation.getLogDirectory()))
      .setEsJvmOptions(new EsJvmOptions(props, tempDir)
        .addFromMandatoryProperty(props, SEARCH_JAVA_OPTS.getKey())
        .addFromMandatoryProperty(props, SEARCH_JAVA_ADDITIONAL_OPTS.getKey()))
      .setEsYmlSettings(new EsYmlSettings(settingsMap))
      .setHost(settingsMap.get("http.host"))
      .setHttpPort(Integer.parseInt(settingsMap.get("http.port")));
    return esInstallation;
  }

  @Override
  public JavaCommand<WebJvmOptions> createWebCommand(boolean leader) {
    File homeDir = props.nonNullValueAsFile(PATH_HOME.getKey());

    WebJvmOptions jvmOptions = new WebJvmOptions(tempDir)
      .addFromMandatoryProperty(props, WEB_JAVA_OPTS.getKey())
      .addFromMandatoryProperty(props, WEB_JAVA_ADDITIONAL_OPTS.getKey());
    addProxyJvmOptions(jvmOptions);

    JavaCommand<WebJvmOptions> command = new JavaCommand<WebJvmOptions>(ProcessId.WEB_SERVER, homeDir)
      .setReadsArgumentsFromFile(true)
      .setArguments(props.rawProperties())
      .setJvmOptions(jvmOptions)
      .setGracefulStopTimeoutMs(getGracefulStopTimeoutMs(props, WEB_GRACEFUL_STOP_TIMEOUT))
      // required for logback tomcat valve
      .setEnvVariable(PATH_LOGS.getKey(), props.nonNullValue(PATH_LOGS.getKey()))
      .setArgument("sonar.cluster.web.startupLeader", Boolean.toString(leader))
      .setClassName("org.sonar.server.app.WebServer")
      .addClasspath("./lib/sonar-application-" + SQ_VERSION + ".jar");
    String driverPath = props.value(JDBC_DRIVER_PATH.getKey());
    if (driverPath != null) {
      command.addClasspath(driverPath);
    }
    addToClassPathAdditionalMSSQLLibraries(command);
    command.suppressEnvVariable(ENV_VAR_JAVA_TOOL_OPTIONS);
    return command;
  }

  private void addToClassPathAdditionalMSSQLLibraries(JavaCommand<? extends JvmOptions> command) {
    String additionalLibPath = props.value(JDBC_ADDITIONAL_LIB_PATHS.getKey());
    if (additionalLibPath == null || additionalLibPath.isEmpty()) {
      return;
    }
    for (String path : additionalLibPath.split(";")) {
      command.addClasspath(path);
    }
  }

  @Override
  public JavaCommand<CeJvmOptions> createCeCommand() {
    File homeDir = props.nonNullValueAsFile(PATH_HOME.getKey());

    CeJvmOptions jvmOptions = new CeJvmOptions(tempDir)
      .addFromMandatoryProperty(props, CE_JAVA_OPTS.getKey())
      .addFromMandatoryProperty(props, CE_JAVA_ADDITIONAL_OPTS.getKey());
    addProxyJvmOptions(jvmOptions);

    JavaCommand<CeJvmOptions> command = new JavaCommand<CeJvmOptions>(ProcessId.COMPUTE_ENGINE, homeDir)
      .setReadsArgumentsFromFile(true)
      .setArguments(props.rawProperties())
      .setJvmOptions(jvmOptions)
      .setGracefulStopTimeoutMs(getGracefulStopTimeoutMs(props, CE_GRACEFUL_STOP_TIMEOUT))
      .setClassName("org.sonar.ce.app.CeServer")
      .addClasspath("./lib/sonar-application-" + SQ_VERSION + ".jar");
    String driverPath = props.value(JDBC_DRIVER_PATH.getKey());
    if (driverPath != null) {
      command.addClasspath(driverPath);
    }
    addToClassPathAdditionalMSSQLLibraries(command);
    command.suppressEnvVariable(ENV_VAR_JAVA_TOOL_OPTIONS);
    return command;
  }

  private static long getGracefulStopTimeoutMs(Props props, ProcessProperties.Property property) {
    String value = Optional.ofNullable(props.value(property.getKey()))
      .orElse(property.getDefaultValue());
    // give some time to CE/Web to shutdown itself after graceful stop timed out
    long gracePeriod = 30 * 1_000L;
    return parseTimeoutMs(property, value) + gracePeriod;
  }

  private <T extends JvmOptions> void addProxyJvmOptions(JvmOptions<T> jvmOptions) {
    for (String key : PROXY_PROPERTY_KEYS) {
      getPropsValue(key).ifPresent(val -> jvmOptions.add("-D" + key + "=" + val));
    }

    // defaults of HTTPS are the same than HTTP defaults
    setSystemPropertyToDefaultIfNotSet(jvmOptions, HTTPS_PROXY_HOST.getKey(), HTTP_PROXY_HOST.getKey());
    setSystemPropertyToDefaultIfNotSet(jvmOptions, HTTPS_PROXY_PORT.getKey(), HTTP_PROXY_PORT.getKey());
  }

  private void setSystemPropertyToDefaultIfNotSet(JvmOptions jvmOptions,
    String httpsProperty, String httpProperty) {
    Optional<String> httpValue = getPropsValue(httpProperty);
    Optional<String> httpsValue = getPropsValue(httpsProperty);
    if (!httpsValue.isPresent() && httpValue.isPresent()) {
      jvmOptions.add("-D" + httpsProperty + "=" + httpValue.get());
    }
  }

  private Optional<String> getPropsValue(String key) {
    return Optional.ofNullable(props.value(key));
  }
}
