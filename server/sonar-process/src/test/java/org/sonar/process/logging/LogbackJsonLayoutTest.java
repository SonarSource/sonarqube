/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.process.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.LoggingEvent;
import com.google.gson.Gson;
import java.time.Instant;
import java.util.List;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.process.logging.LogbackJsonLayout.DATE_FORMATTER;

public class LogbackJsonLayoutTest {

  private final LogbackJsonLayout underTest = new LogbackJsonLayout("web", "");

  @Test
  public void test_simple_log() {
    LoggingEvent event = new LoggingEvent("org.foundation.Caller", (Logger) LoggerFactory.getLogger("the.logger"), Level.WARN, "the message", null, new Object[0]);

    String log = underTest.doLayout(event);

    JsonLog json = new Gson().fromJson(log, JsonLog.class);
    assertThat(json.process).isEqualTo("web");
    assertThat(json.timestamp).isEqualTo(DATE_FORMATTER.format(Instant.ofEpochMilli(event.getTimeStamp())));
    assertThat(json.severity).isEqualTo("WARN");
    assertThat(json.logger).isEqualTo("the.logger");
    assertThat(json.message).isEqualTo("the message");
    assertThat(json.stacktrace).isNull();
    assertThat(json.fromMdc).isNull();
    assertThat(json.nodename).isNull();
  }

  @Test
  public void test_simple_log_with_hostname() {
    LoggingEvent event = new LoggingEvent("org.foundation.Caller", (Logger) LoggerFactory.getLogger("the.logger"), Level.WARN, "the message", null, new Object[0]);

    LogbackJsonLayout underTestWithNodeName = new LogbackJsonLayout("web", "my-nodename");
    String log = underTestWithNodeName.doLayout(event);

    JsonLog json = new Gson().fromJson(log, JsonLog.class);
    assertThat(json.process).isEqualTo("web");
    assertThat(json.timestamp).isEqualTo(DATE_FORMATTER.format(Instant.ofEpochMilli(event.getTimeStamp())));
    assertThat(json.severity).isEqualTo("WARN");
    assertThat(json.logger).isEqualTo("the.logger");
    assertThat(json.message).isEqualTo("the message");
    assertThat(json.stacktrace).isNull();
    assertThat(json.fromMdc).isNull();
    assertThat(json.nodename).isEqualTo("my-nodename");
  }

  @Test
  public void test_log_with_throwable_and_no_cause() {
    Throwable exception = new IllegalStateException("BOOM");
    LoggingEvent event = new LoggingEvent("org.foundation.Caller", (Logger) LoggerFactory.getLogger("the.logger"), Level.WARN, "the message", exception, new Object[0]);

    String log = underTest.doLayout(event);

    JsonLog json = new Gson().fromJson(log, JsonLog.class);
    assertThat(json.process).isEqualTo("web");
    assertThat(json.timestamp).isEqualTo(DATE_FORMATTER.format(Instant.ofEpochMilli(event.getTimeStamp())));
    assertThat(json.severity).isEqualTo("WARN");
    assertThat(json.logger).isEqualTo("the.logger");
    assertThat(json.message).isEqualTo("the message");
    assertThat(json.stacktrace).hasSizeGreaterThan(5);
    assertThat(json.stacktrace[0]).isEqualTo("java.lang.IllegalStateException: BOOM");
    assertThat(json.stacktrace[1]).startsWith("at ").contains("LogbackJsonLayoutTest.test_log_with_throwable");
    assertThat(json.fromMdc).isNull();
  }

  @Test
  public void test_log_with_throwable_and_cause() {
    Throwable rootCause = new IllegalArgumentException("Root cause");
    Throwable exception = new IllegalStateException("BOOM", rootCause);
    LoggingEvent event = new LoggingEvent("org.foundation.Caller", (Logger) LoggerFactory.getLogger("the.logger"), Level.WARN, "the message", exception, new Object[0]);

    String log = underTest.doLayout(event);

    JsonLog json = new Gson().fromJson(log, JsonLog.class);
    assertThat(json.stacktrace).hasSizeGreaterThan(5);
    assertThat(json.stacktrace[0]).isEqualTo("java.lang.IllegalStateException: BOOM");
    assertThat(json.stacktrace[1]).contains("at org.sonar.process.logging.LogbackJsonLayoutTest.test_log_with_throwable_and_cause");
    assertThat(json.stacktrace)
      .contains("\tCaused by: ")
      .contains("\tjava.lang.IllegalArgumentException: Root cause");
  }

  @Test
  public void test_log_with_suppressed_throwable() {
    Exception exception = new Exception("BOOM");
    exception.addSuppressed(new IllegalStateException("foo"));
    LoggingEvent event = new LoggingEvent("org.foundation.Caller", (Logger) LoggerFactory.getLogger("the.logger"), Level.WARN, "the message", exception, new Object[0]);

    String log = underTest.doLayout(event);

    JsonLog json = new Gson().fromJson(log, JsonLog.class);
    assertThat(json.stacktrace).hasSizeGreaterThan(5);
    assertThat(json.stacktrace[0]).isEqualTo("java.lang.Exception: BOOM");
    assertThat(json.stacktrace).contains("Suppressed: java.lang.IllegalStateException: foo");
  }

  @Test
  public void test_log_with_message_arguments() {
    LoggingEvent event = new LoggingEvent("org.foundation.Caller", (Logger) LoggerFactory.getLogger("the.logger"), Level.WARN, "the {}", null, new Object[] {"message"});

    String log = underTest.doLayout(event);

    JsonLog json = new Gson().fromJson(log, JsonLog.class);
    assertThat(json.process).isEqualTo("web");
    assertThat(json.timestamp).isEqualTo(DATE_FORMATTER.format(Instant.ofEpochMilli(event.getTimeStamp())));
    assertThat(json.severity).isEqualTo("WARN");
    assertThat(json.logger).isEqualTo("the.logger");
    assertThat(json.message).isEqualTo("the message");
    assertThat(json.stacktrace).isNull();
    assertThat(json.fromMdc).isNull();
  }

  @Test
  public void test_log_with_MDC() {
    try {
      LoggingEvent event = new LoggingEvent("org.foundation.Caller", (Logger) LoggerFactory.getLogger("the.logger"), Level.WARN, "the message", null, new Object[0]);
      MDC.put("fromMdc", "foo");

      String log = underTest.doLayout(event);

      JsonLog json = new Gson().fromJson(log, JsonLog.class);
      assertThat(json.fromMdc).isEqualTo("foo");
    } finally {
      MDC.clear();
    }
  }

  @Test
  public void doLayout_whenMDC_shouldNotContainExcludedFields() {
    try {
      LogbackJsonLayout logbackJsonLayout = new LogbackJsonLayout("web", "", List.of("fromMdc"));
      LoggingEvent event = new LoggingEvent("org.foundation.Caller", (Logger) LoggerFactory.getLogger("the.logger"), Level.WARN, "the message", null, new Object[0]);
      MDC.put("fromMdc", "foo");

      String log = logbackJsonLayout.doLayout(event);

      JsonLog json = new Gson().fromJson(log, JsonLog.class);
      assertThat(json.fromMdc).isNull();
    } finally {
      MDC.clear();
    }
  }

  private static class JsonLog {
    private String process;
    private String timestamp;
    private String severity;
    private String logger;
    private String message;
    private String fromMdc;
    private String[] stacktrace;
    private String nodename;
  }
}
