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
package org.sonar.server.platform;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.read.ListAppender;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.util.StatusPrinter;
import org.junit.Test;
import org.sonar.server.computation.ComputationThreadLauncher;

import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

public class SwitchLogbackAppenderTest {

  LoggerContext loggerContext = new LoggerContext();
  Logger logger = loggerContext.getLogger(this.getClass().getName());
  SwitchLogbackAppender switchAppender;
  ListAppender<ILoggingEvent> console, analyisReports;

  protected void configure(URL file) throws JoranException {
    JoranConfigurator jc = new JoranConfigurator();
    jc.setContext(loggerContext);
    jc.doConfigure(file);

    Logger root = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
    switchAppender = (SwitchLogbackAppender) root.getAppender("SWITCH");
    console = (ListAppender<ILoggingEvent>) switchAppender.getAppender("CONSOLE");
    analyisReports = (ListAppender<ILoggingEvent>) switchAppender.getAppender("ANALYSIS_REPORTS");
  }

  /**
   * Standard log goes to console only
   */
  @Test
  public void standard_log() throws JoranException {
    configure(getClass().getResource("SwitchLogbackAppenderTest/valid-switch.xml"));

    logger.info("hello");

    assertThat(console.list).hasSize(1);
    assertThat(console.list.get(0).getMessage()).isEqualTo("hello");
    assertThat(analyisReports.list).isEmpty();
  }

  /**
   * Compute service log goes to dedicated appender. Warnings and errors are logged in both appenders.
   */
  @Test
  public void compute_service_log_goes_to_dedicated_appender() throws JoranException {
    configure(getClass().getResource("SwitchLogbackAppenderTest/valid-switch.xml"));

    String initialThreadName = Thread.currentThread().getName();
    Thread.currentThread().setName(ComputationThreadLauncher.THREAD_NAME_PREFIX + "test");
    try {
      logger.info("hello");
      assertThat(analyisReports.list).hasSize(1);
      assertThat(analyisReports.list.get(0).getMessage()).isEqualTo("hello");
      assertThat(console.list).isEmpty();

      logger.warn("a warning");
      assertThat(analyisReports.list).hasSize(2);
      assertThat(analyisReports.list.get(1).getMessage()).isEqualTo("a warning");
      assertThat(console.list).hasSize(1);
      assertThat(console.list.get(0).getMessage()).isEqualTo("a warning");

      logger.error("an error");
      assertThat(analyisReports.list).hasSize(3);
      assertThat(analyisReports.list.get(2).getMessage()).isEqualTo("an error");
      assertThat(console.list).hasSize(2);
      assertThat(console.list.get(1).getMessage()).isEqualTo("an error");

    } finally {
      Thread.currentThread().setName(initialThreadName);
    }
  }

  @Test
  public void fail_if_bad_configuration() throws JoranException {
    boolean foundError = false;
    configure(getClass().getResource("SwitchLogbackAppenderTest/invalid-switch.xml"));
    StatusPrinter.printInCaseOfErrorsOrWarnings(loggerContext);
    for (Status status : loggerContext.getStatusManager().getCopyOfStatusList()) {
      Throwable error = status.getThrowable();
      if (error != null) {
        assertThat(error).hasMessage("Invalid appender name: UNKNOWN");
        foundError = true;
      }
    }
    assertThat(foundError).isTrue();
  }

  @Test
  public void test_logback_internals() throws Exception {
    configure(getClass().getResource("SwitchLogbackAppenderTest/valid-switch.xml"));

    assertThat(switchAppender.iteratorForAppenders()).hasSize(2);
    assertThat(switchAppender.isAttached(console)).isTrue();

    assertThat(switchAppender.detachAppender("CONSOLE")).isTrue();
    assertThat(switchAppender.detachAppender(analyisReports)).isTrue();
    switchAppender.detachAndStopAllAppenders();
  }
}
