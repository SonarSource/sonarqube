/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.app;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.encoder.Encoder;
import java.util.Collection;
import java.util.List;
import org.sonar.process.Props;
import org.sonar.process.logging.LogDomain;
import org.sonar.process.logging.LogLevelConfig;
import org.sonar.process.logging.RootLoggerConfig;
import org.sonar.server.log.ServerProcessLogging;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.sonar.process.ProcessId.WEB_SERVER;
import static org.sonar.process.ProcessProperties.Property.LOG_JSON_OUTPUT;
import static org.sonar.process.logging.AbstractLogHelper.PREFIX_LOG_FORMAT;
import static org.sonar.process.logging.AbstractLogHelper.SUFFIX_LOG_FORMAT;
import static org.sonar.process.logging.LogbackHelper.DEPRECATION_LOGGER_NAME;
import static org.sonar.process.logging.RootLoggerConfig.newRootLoggerConfigBuilder;
import static org.sonar.server.authentication.UserSessionInitializer.USER_LOGIN_MDC_KEY;
import static org.sonar.server.platform.web.logging.EntrypointMDCStorage.ENTRYPOINT_MDC_KEY;
import static org.sonar.server.platform.web.requestid.RequestIdMDCStorage.HTTP_REQUEST_ID_MDC_KEY;

/**
 * Configure logback for the Web Server process. Logs are written to file "web.log" in SQ's log directory.
 */
public class WebServerProcessLogging extends ServerProcessLogging {

  private static final String DEPRECATION_LOG_FILE_PREFIX = "deprecation";

  private static final String ENABLE_LOGIN_PROPERTY = "sonar.deprecationLogs.loginEnabled";

  public WebServerProcessLogging() {
    super(WEB_SERVER, "%X{" + HTTP_REQUEST_ID_MDC_KEY + "}");
  }

  @Override
  protected void extendLogLevelConfiguration(LogLevelConfig.Builder logLevelConfigBuilder) {
    logLevelConfigBuilder.levelByDomain("sql", WEB_SERVER, LogDomain.SQL);
    logLevelConfigBuilder.levelByDomain("es", WEB_SERVER, LogDomain.ES);
    logLevelConfigBuilder.levelByDomain("auth.event", WEB_SERVER, LogDomain.AUTH_EVENT);
    JMX_RMI_LOGGER_NAMES.forEach(loggerName -> logLevelConfigBuilder.levelByDomain(loggerName, WEB_SERVER, LogDomain.JMX));

    logLevelConfigBuilder.offUnlessTrace("org.apache.catalina.core.ContainerBase");
    logLevelConfigBuilder.offUnlessTrace("org.apache.catalina.core.StandardContext");
    logLevelConfigBuilder.offUnlessTrace("org.apache.catalina.core.StandardService");

    LOGGER_NAMES_TO_TURN_OFF.forEach(loggerName -> logLevelConfigBuilder.immutableLevel(loggerName, Level.OFF));
  }

  @Override
  protected void extendConfigure(Props props) {
    configureDeprecatedApiLogger(props);
  }

  @Override
  protected RootLoggerConfig buildRootLoggerConfig(Props props) {
    return getRootLoggerConfigBuilder(props, List.of(USER_LOGIN_MDC_KEY)).build();
  }

  private static RootLoggerConfig.Builder getRootLoggerConfigBuilder(Props props, Collection<String> excludedFields) {
    return newRootLoggerConfigBuilder()
      .setProcessId(WEB_SERVER)
      .setNodeNameField(getNodeNameWhenCluster(props))
      .setThreadIdFieldPattern("%X{" + HTTP_REQUEST_ID_MDC_KEY + "}")
      .setExcludedFields(excludedFields.stream().toList());
  }

  private void configureDeprecatedApiLogger(Props props) {
    LoggerContext context = helper.getRootContext();

    boolean isLoginEnabled = props.valueAsBoolean(ENABLE_LOGIN_PROPERTY, false);
    RootLoggerConfig config = getRootLoggerConfigBuilder(props, isLoginEnabled ? List.of() : List.of(USER_LOGIN_MDC_KEY)).build();

    Encoder<ILoggingEvent> encoder = props.valueAsBoolean(LOG_JSON_OUTPUT.getKey(), Boolean.parseBoolean(LOG_JSON_OUTPUT.getDefaultValue()))
      ? helper.createJsonEncoder(context, config)
      : helper.createPatternLayoutEncoder(context, buildDepractedLogPatrern(config));

    FileAppender<ILoggingEvent> appender = helper.newFileAppender(context, props, DEPRECATION_LOG_FILE_PREFIX, encoder);
    ConsoleAppender<ILoggingEvent> consoleAppender = helper.newConsoleAppender(context, "CONSOLE", encoder);

    Logger deprecated = context.getLogger(DEPRECATION_LOGGER_NAME);
    deprecated.setAdditive(false);
    deprecated.addAppender(appender);
    deprecated.addAppender(consoleAppender);
  }

  private static String buildDepractedLogPatrern(RootLoggerConfig config) {
    String userLoginPattern = " %X{" + USER_LOGIN_MDC_KEY + "}";
    return PREFIX_LOG_FORMAT
      + (isBlank(config.getNodeNameField()) ? "" : (config.getNodeNameField() + " "))
      + config.getProcessId().getKey()
      + "[" + config.getThreadIdFieldPattern() + "]"
      + (config.getExcludedFields().contains(USER_LOGIN_MDC_KEY) ? EMPTY : userLoginPattern)
      + " %X{" + ENTRYPOINT_MDC_KEY + "}"
      + SUFFIX_LOG_FORMAT;
  }

}
