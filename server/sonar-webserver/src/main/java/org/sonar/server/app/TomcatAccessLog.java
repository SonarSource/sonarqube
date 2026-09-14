/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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

import ch.qos.logback.access.common.PatternLayoutEncoder;
import ch.qos.logback.access.common.spi.IAccessEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.encoder.Encoder;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.startup.Tomcat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.MessageException;
import org.sonar.process.Props;
import org.sonar.process.logging.LogbackHelper;

import static java.lang.String.format;
import static org.sonar.process.ProcessProperties.Property.WEB_ACCESSLOGS_TARGET;

class TomcatAccessLog {

  private static final String PROPERTY_ENABLE = "sonar.web.accessLogs.enable";
  private static final String PROPERTY_PATTERN = "sonar.web.accessLogs.pattern";
  private static final String DEFAULT_SQ_ACCESS_LOG_PATTERN = "%h %l %u [%t] \"%r\" %s %b \"%i{Referer}\" \"%i{User-Agent}\" \"%reqAttribute{ID}\" %D";

  private static final String FILE_APPENDER_NAME = "ACCESS_LOG";
  private static final String CONSOLE_APPENDER_NAME = "ACCESS_LOG_CONSOLE";

  /**
   * Where the access log is written, as configured by {@link org.sonar.process.ProcessProperties.Property#WEB_ACCESSLOGS_TARGET}.
   */
  enum Target {
    FILE(true, false),
    CONSOLE(false, true),
    BOTH(true, true);

    private final boolean file;
    private final boolean console;

    Target(boolean file, boolean console) {
      this.file = file;
      this.console = console;
    }

    boolean toFile() {
      return file;
    }

    boolean toConsole() {
      return console;
    }

    /**
     * @param value raw property value, {@code null} when the property is neither set nor defaulted
     */
    static Target parse(@Nullable String value) {
      if (value == null) {
        return FILE;
      }
      try {
        return valueOf(value.trim().toUpperCase(Locale.ENGLISH));
      } catch (IllegalArgumentException e) {
        throw MessageException.of(format("Invalid value for property %s: [%s], only [%s] are allowed", WEB_ACCESSLOGS_TARGET.getKey(), value,
          Arrays.stream(values()).map(t -> t.name().toLowerCase(Locale.ENGLISH)).collect(Collectors.joining(", "))));
      }
    }
  }

  void configure(Tomcat tomcat, Props props) {
    tomcat.setSilent(true);
    tomcat.getService().addLifecycleListener(new LifecycleLogger(LoggerFactory.getLogger(TomcatAccessLog.class)));
    configureLogbackAccess(tomcat, props);
  }

  private static void configureLogbackAccess(Tomcat tomcat, Props props) {
    if (!props.valueAsBoolean(PROPERTY_ENABLE, true)) {
      return;
    }
    Target target = Target.parse(props.value(WEB_ACCESSLOGS_TARGET.getKey(), WEB_ACCESSLOGS_TARGET.getDefaultValue()));

    ProgrammaticLogbackValve valve = new ProgrammaticLogbackValve();
    if (target.toFile()) {
      valve.addAppender(newFileAppender(valve, props));
    }
    if (target.toConsole()) {
      valve.addAppender(newConsoleAppender(valve, props));
    }
    valve.setAsyncSupported(true);
    tomcat.getHost().getPipeline().addValve(valve);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static Appender<IAccessEvent> newFileAppender(ProgrammaticLogbackValve valve, Props props) {
    LogbackHelper.RollingPolicy policy = new LogbackHelper().createRollingPolicy(valve, props, "access");
    // the rolling policy is typed for ILoggingEvent, but only the file plumbing is used here, hence the raw type
    FileAppender appender = policy.createAppender(FILE_APPENDER_NAME);
    appender.setEncoder(newPatternLayoutEncoder(valve, props));
    appender.start();
    return appender;
  }

  /**
   * The console gets exactly what the file gets: same pattern, same text. Nothing about the content of an access log
   * entry depends on where it is written.
   */
  private static Appender<IAccessEvent> newConsoleAppender(ProgrammaticLogbackValve valve, Props props) {
    ConsoleAppender<IAccessEvent> appender = new ConsoleAppender<>();
    appender.setContext(valve);
    appender.setName(CONSOLE_APPENDER_NAME);
    appender.setTarget("System.out");
    appender.setEncoder(newPatternLayoutEncoder(valve, props));
    appender.start();
    return appender;
  }

  private static Encoder<IAccessEvent> newPatternLayoutEncoder(Context context, Props props) {
    PatternLayoutEncoder encoder = new PatternLayoutEncoder();
    encoder.setContext(context);
    encoder.setPattern(accessLogPattern(props));
    encoder.start();
    return encoder;
  }

  private static String accessLogPattern(Props props) {
    String pattern = props.value(PROPERTY_PATTERN, DEFAULT_SQ_ACCESS_LOG_PATTERN);
    return pattern == null ? DEFAULT_SQ_ACCESS_LOG_PATTERN : pattern;
  }

  static class LifecycleLogger implements LifecycleListener {
    private Logger logger;

    LifecycleLogger(Logger logger) {
      this.logger = logger;
    }

    @Override
    public void lifecycleEvent(LifecycleEvent event) {
      if ("after_start".equals(event.getType())) {
        logger.debug("Tomcat is started");

      } else if ("after_destroy".equals(event.getType())) {
        logger.debug("Tomcat is stopped");
      }
    }
  }

}
