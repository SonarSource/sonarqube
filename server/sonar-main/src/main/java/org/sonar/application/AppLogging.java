/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.encoder.Encoder;
import org.sonar.application.config.AppSettings;
import org.sonar.application.process.StreamGobbler;
import org.sonar.process.ProcessId;
import org.sonar.process.logging.LogLevelConfig;
import org.sonar.process.logging.LogbackHelper;
import org.sonar.process.logging.RootLoggerConfig;

import static org.slf4j.Logger.ROOT_LOGGER_NAME;
import static org.sonar.application.process.StreamGobbler.LOGGER_GOBBLER;
import static org.sonar.process.logging.RootLoggerConfig.newRootLoggerConfigBuilder;

/**
 * Configure logback for the APP process.
 *
 * <p>
 * SonarQube's logging use cases:
 * <ol>
 *   <li>
 *     SQ started as a background process (with {@code sonar.sh start}):
 *     <ul>
 *       <li>
 *         logs produced by the JVM before logback is setup in the APP JVM or which can't be caught by logback
 *         (such as JVM crash) must be written to sonar.log
 *       </li>
 *       <li>
 *         logs produced by the sub process JVMs before logback is setup in the subprocess JVMs or which can't be caught
 *         by logback (such as JVM crash) must be written to sonar.log
 *       </li>
 *       <li>each JVM writes its own logs into its dedicated file</li>
 *     </ul>
 *   </li>
 *   <li>
 *     SQ started in console with wrapper (ie. with {@code sonar.sh console}):
 *     <ul>
 *       <li>
 *         logs produced by the APP JVM before logback is setup in the APP JVM or which can't be caught by logback
 *         (such as JVM crash) must be written to sonar.log
 *       </li>
 *       <li>
 *         logs produced by the sub process JVMs before logback is setup in the subprocess JVMs or which can't be caught
 *         by logback (such as JVM crash) must be written to sonar.log
 *       </li>
 *       <li>each JVM writes its own logs into its dedicated file</li>
 *       <li>APP JVM logs are written to the APP JVM {@code System.out}</li>
 *     </ul>
 *   </li>
 *   <li>
 *     SQ started from command line (ie. {@code java -jar sonar-application-X.Y.jar}):
 *     <ul>
 *       <li>
 *         logs produced by the APP JVM before logback is setup in the APP JVM or which can't be caught by logback
 *         (such as JVM crash) are the responsibility of the user to be dealt with
 *       </li>
 *       <li>
 *         logs produced by the sub process JVMs before logback is setup in the subprocess JVMs or which can't be caught
 *         by logback (such as JVM crash) must be written to APP's {@code System.out}
 *       </li>
 *       <li>each JVM writes its own logs into its dedicated file</li>
 *       <li>APP JVM logs are written to the APP JVM {@code System.out}</li>
 *     </ul>
 *   </li>
 *   <li>
 *     SQ started from an IT (ie. from command line with {@code option -Dsonar.log.console=true}):
 *     <ul>
 *       <li>
 *         logs produced by the APP JVM before logback is setup in the APP JVM or which can't be caught by logback
 *         (such as JVM crash) are the responsibility of the developer or maven to be dealt with
 *       </li>
 *       <li>
 *         logs produced by the sub process JVMs before logback is setup in the subprocess JVMs or which can't be caught
 *         by logback (such as JVM crash) must be written to APP's {@code System.out} and are the responsibility of the
 *         developer or maven to be dealt with
 *       </li>
 *       <li>each JVM writes its own logs into its dedicated file</li>
 *       <li>logs of all 4 JVMs are also written to the APP JVM {@code System.out}</li>
 *     </ul>
 *   </li>
 * </ol>
 * </p>
 *
 */
public class AppLogging {

  private static final String CONSOLE_LOGGER = "console";
  private static final String CONSOLE_PLAIN_APPENDER = "CONSOLE";
  private static final String APP_CONSOLE_APPENDER = "APP_CONSOLE";
  private static final String GOBBLER_PLAIN_CONSOLE = "GOBBLER_CONSOLE";
  private static final RootLoggerConfig APP_ROOT_LOGGER_CONFIG = newRootLoggerConfigBuilder()
    .setProcessId(ProcessId.APP)
    .build();

  private final LogbackHelper helper = new LogbackHelper();
  private final AppSettings appSettings;

  public AppLogging(AppSettings appSettings) {
    this.appSettings = appSettings;
  }

  public LoggerContext configure() {
    LoggerContext ctx = helper.getRootContext();
    ctx.reset();

    helper.enableJulChangePropagation(ctx);

    configureConsole(ctx);
    if (helper.isAllLogsToConsoleEnabled(appSettings.getProps()) || !appSettings.getProps().valueAsBoolean("sonar.wrapped", false)) {
      configureWithLogbackWritingToFile(ctx);
    } else {
      configureWithWrapperWritingToFile(ctx);
    }
    helper.apply(
      LogLevelConfig.newBuilder(helper.getRootLoggerName())
        .rootLevelFor(ProcessId.APP)
        .immutableLevel("com.hazelcast",
          Level.toLevel("WARN"))
        .build(),
      appSettings.getProps());

    return ctx;
  }

  /**
   * Creates a non additive logger dedicated to printing message as is (ie. assuming they are already formatted).
   *
   * It creates a dedicated appender to the System.out which applies no formatting the logs it receives.
   */
  private void configureConsole(LoggerContext loggerContext) {
    Encoder<ILoggingEvent> encoder = createGobblerEncoder(loggerContext);
    ConsoleAppender<ILoggingEvent> consoleAppender = helper.newConsoleAppender(loggerContext, CONSOLE_PLAIN_APPENDER, encoder);

    Logger consoleLogger = loggerContext.getLogger(CONSOLE_LOGGER);
    consoleLogger.setAdditive(false);
    consoleLogger.addAppender(consoleAppender);
  }

  /**
   * The process has been started by orchestrator (ie. via {@code java -jar} and optionally passing the option {@code -Dsonar.log.console=true}).
   * Therefor, APP's System.out (and System.err) are <strong>not</strong> copied to sonar.log by the wrapper and
   * printing to sonar.log must be done at logback level.
   */
  private void configureWithLogbackWritingToFile(LoggerContext ctx) {
    // configure all logs (ie. root logger) to be written to sonar.log and also to the console with formatting
    // in practice, this will be only APP's own logs as logs from sub processes LOGGER_GOBBLER and LOGGER_GOBBLER
    // is configured below to be detached from root
    // so, this will make all APP's log to be both written to sonar.log and visible in the console
    configureRootWithLogbackWritingToFile(ctx);

    // if option -Dsonar.log.console=true has been set, sub processes will write their logs to their own files but also
    // copy them to their System.out.
    // otherwise, the only logs to be expected in LOGGER_GOBBLER are those before logback is setup in subprocesses or
    // when their JVM crashes
    // they must be printed to App's System.out as is (as they are already formatted)
    // logger is configured to be non additive as we don't want these logs to be written to sonar.log and duplicated in
    // the console (with an incorrect formatting)
    configureGobbler(ctx);
  }

  /**
   * SQ has been started by the wrapper (ie. with sonar.sh) therefor, APP's System.out (and System.err) are written to
   * sonar.log by the wrapper.
   */
  private void configureWithWrapperWritingToFile(LoggerContext ctx) {
    // configure all logs (ie. root logger) to be written to console with formatting
    // in practice, this will be only APP's own logs as logs from sub processes are written to LOGGER_GOBBLER and
    // LOGGER_GOBBLER is configured below to be detached from root
    // logs are written to the console because we want them to be in sonar.log and the wrapper will write any log
    // from APP's System.out and System.err to sonar.log
    Logger rootLogger = ctx.getLogger(ROOT_LOGGER_NAME);
    Encoder<ILoggingEvent> encoder = helper.createEncoder(appSettings.getProps(), APP_ROOT_LOGGER_CONFIG, ctx);
    rootLogger.addAppender(createAppConsoleAppender(ctx, encoder));

    // in regular configuration, sub processes are not copying their logs to their System.out, so, the only logs to be
    // expected in LOGGER_GOBBLER are those before logback is setup in subprocesses or when JVM crashes
    // so, they must be printed to App's System.out as is (as they are already formatted) and the wrapper will write
    // them to sonar.log
    // logger is configured to be non additive as we don't want these logs written to sonar.log and duplicated in the
    // console with an incorrect formatting
    configureGobbler(ctx);
  }

  private void configureRootWithLogbackWritingToFile(LoggerContext ctx) {
    Logger rootLogger = ctx.getLogger(ROOT_LOGGER_NAME);
    Encoder<ILoggingEvent> encoder = helper.createEncoder(appSettings.getProps(), APP_ROOT_LOGGER_CONFIG, ctx);
    FileAppender<ILoggingEvent> fileAppender = helper.newFileAppender(ctx, appSettings.getProps(), APP_ROOT_LOGGER_CONFIG, encoder);
    rootLogger.addAppender(fileAppender);
    rootLogger.addAppender(createAppConsoleAppender(ctx, encoder));
  }

  /**
   * Configure the logger to which logs from sub processes are written to
   * (called {@link StreamGobbler#LOGGER_GOBBLER}) by {@link StreamGobbler},
   * to be:
   * <ol>
   *   <li>non additive (ie. these logs will be output by the appender of {@link StreamGobbler#LOGGER_GOBBLER} and only this one)</li>
   *   <li>write logs as is (ie. without any extra formatting)</li>
   *   <li>write exclusively to App's System.out</li>
   * </ol>
   */
  private void configureGobbler(LoggerContext ctx) {
    Logger gobblerLogger = ctx.getLogger(LOGGER_GOBBLER);
    gobblerLogger.setAdditive(false);
    Encoder<ILoggingEvent> encoder = createGobblerEncoder(ctx);
    gobblerLogger.addAppender(helper.newConsoleAppender(ctx, GOBBLER_PLAIN_CONSOLE, encoder));
  }

  private ConsoleAppender<ILoggingEvent> createAppConsoleAppender(LoggerContext ctx, Encoder<ILoggingEvent> encoder) {
    return helper.newConsoleAppender(ctx, APP_CONSOLE_APPENDER, encoder);
  }

  /**
   * Simply displays the message received as input.
   */
  private static Encoder<ILoggingEvent> createGobblerEncoder(LoggerContext context) {
    PatternLayoutEncoder encoder = new PatternLayoutEncoder();
    encoder.setContext(context);
    encoder.setPattern("%msg%n");
    encoder.start();
    return encoder;
  }
}
