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
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.encoder.Encoder;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Properties;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.startup.Tomcat;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.sonar.api.utils.MessageException;
import org.sonar.process.Props;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.process.ProcessProperties.Property.LOG_JSON_OUTPUT;
import static org.sonar.process.ProcessProperties.Property.PATH_LOGS;
import static org.sonar.process.ProcessProperties.Property.WEB_ACCESSLOGS_TARGET;

public class TomcatAccessLogTest {

  private static final long EVENT_TIMESTAMP = ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli();

  private static final String FILE_APPENDER = "ACCESS_LOG";
  private static final String CONSOLE_APPENDER = "ACCESS_LOG_CONSOLE";

  TomcatAccessLog underTest = new TomcatAccessLog();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private Props props;

  @Before
  public void setHome() throws IOException {
    File homeDir = temp.newFolder("home");
    System.setProperty("SONAR_HOME", homeDir.getAbsolutePath());
    props = new Props(new Properties());
    props.set(PATH_LOGS.getKey(), temp.newFolder().getAbsolutePath());
  }

  @Test
  public void enable_access_logs_by_Default() {
    Tomcat tomcat = mock(Tomcat.class, Mockito.RETURNS_DEEP_STUBS);
    underTest.configure(tomcat, props);

    verify(tomcat.getHost().getPipeline()).addValve(any(ProgrammaticLogbackValve.class));
  }

  @Test
  public void access_logs_are_written_to_file_only_by_default() {
    ProgrammaticLogbackValve valve = configureAndCaptureValve();

    assertThat(valve.getAppender(FILE_APPENDER)).isInstanceOf(FileAppender.class);
    assertThat(valve.getAppender(CONSOLE_APPENDER)).isNull();
  }

  @Test
  public void access_logs_are_written_to_file_only_when_target_is_file() {
    props.set(WEB_ACCESSLOGS_TARGET.getKey(), "file");

    ProgrammaticLogbackValve valve = configureAndCaptureValve();

    assertThat(valve.getAppender(FILE_APPENDER)).isInstanceOf(FileAppender.class);
    assertThat(valve.getAppender(CONSOLE_APPENDER)).isNull();
  }

  @Test
  public void access_logs_are_written_to_console_only_when_target_is_console() {
    props.set(WEB_ACCESSLOGS_TARGET.getKey(), "console");

    ProgrammaticLogbackValve valve = configureAndCaptureValve();

    assertThat(valve.getAppender(FILE_APPENDER)).isNull();
    assertThat(valve.getAppender(CONSOLE_APPENDER)).isInstanceOf(ConsoleAppender.class);
  }

  @Test
  public void access_logs_are_written_to_file_and_console_when_target_is_both() {
    props.set(WEB_ACCESSLOGS_TARGET.getKey(), "both");

    ProgrammaticLogbackValve valve = configureAndCaptureValve();

    assertThat(valve.getAppender(FILE_APPENDER)).isInstanceOf(FileAppender.class);
    assertThat(valve.getAppender(CONSOLE_APPENDER)).isInstanceOf(ConsoleAppender.class);
  }

  @Test
  public void target_is_case_insensitive_and_trimmed() {
    props.set(WEB_ACCESSLOGS_TARGET.getKey(), " BoTh ");

    ProgrammaticLogbackValve valve = configureAndCaptureValve();

    assertThat(valve.getAppender(FILE_APPENDER)).isNotNull();
    assertThat(valve.getAppender(CONSOLE_APPENDER)).isNotNull();
  }

  @Test
  public void target_falls_back_to_file_when_property_resolves_to_null() {
    assertThat(TomcatAccessLog.Target.parse(null)).isEqualTo(TomcatAccessLog.Target.FILE);
  }

  @Test
  public void fail_when_target_is_not_supported() {
    props.set(WEB_ACCESSLOGS_TARGET.getKey(), "stdout");
    Tomcat tomcat = mock(Tomcat.class, Mockito.RETURNS_DEEP_STUBS);

    assertThatThrownBy(() -> underTest.configure(tomcat, props))
      .isInstanceOf(MessageException.class)
      .hasMessage("Invalid value for property sonar.web.accessLogs.target: [stdout], only [file, console, both] are allowed");
  }

  @Test
  public void no_valve_when_access_logs_are_disabled() {
    props.set("sonar.web.accessLogs.enable", "false");
    props.set(WEB_ACCESSLOGS_TARGET.getKey(), "console");
    Tomcat tomcat = mock(Tomcat.class, Mockito.RETURNS_DEEP_STUBS);

    underTest.configure(tomcat, props);

    verifyNoInteractions(tomcat.getHost().getPipeline());
  }

  @Test
  public void console_appender_uses_plain_text_encoder_by_default() {
    props.set(WEB_ACCESSLOGS_TARGET.getKey(), "console");

    ProgrammaticLogbackValve valve = configureAndCaptureValve();

    assertThat(encoderOf(valve, CONSOLE_APPENDER)).isInstanceOf(PatternLayoutEncoder.class);
  }




  @Test
  public void console_appender_uses_configured_pattern_when_json_output_is_disabled() {
    props.set(WEB_ACCESSLOGS_TARGET.getKey(), "console");
    props.set("sonar.web.accessLogs.pattern", "%h %s");

    ProgrammaticLogbackValve valve = configureAndCaptureValve();

    assertThat(((PatternLayoutEncoder) encoderOf(valve, CONSOLE_APPENDER)).getPattern()).isEqualTo("%h %s");
  }


  @Test
  public void appenders_are_started() {
    props.set(WEB_ACCESSLOGS_TARGET.getKey(), "both");

    ProgrammaticLogbackValve valve = configureAndCaptureValve();

    assertThat(valve.getAppender(FILE_APPENDER).isStarted()).isTrue();
    assertThat(valve.getAppender(CONSOLE_APPENDER).isStarted()).isTrue();
  }

  @Test
  public void console_appender_uses_the_same_encoder_settings_as_the_file_appender() {
    props.set(WEB_ACCESSLOGS_TARGET.getKey(), "both");
    props.set(LOG_JSON_OUTPUT.getKey(), "true");

    ProgrammaticLogbackValve valve = configureAndCaptureValve();

    PatternLayoutEncoder fileEncoder = (PatternLayoutEncoder) encoderOf(valve, FILE_APPENDER);
    PatternLayoutEncoder consoleEncoder = (PatternLayoutEncoder) encoderOf(valve, CONSOLE_APPENDER);
    assertThat(consoleEncoder.getPattern()).isEqualTo(fileEncoder.getPattern());
  }

  @Test
  public void file_appender_writes_the_default_pattern_byte_for_byte() throws IOException {
    File logsDir = temp.newFolder();
    props.set(PATH_LOGS.getKey(), logsDir.getAbsolutePath());

    appendTo(configureAndCaptureValve(), FILE_APPENDER);

    assertThat(Files.readString(new File(logsDir, "access.log").toPath(), StandardCharsets.UTF_8))
      .isEqualTo("1.2.3.4 - - [" + expectedTimestamp() + "] \"GET /api/issues/search?ps=1 HTTP/1.1\" 200 12"
        + " \"https://sq.example.com/\" \"SonarScanner/6.0\" \"AXbc123\" 42" + System.lineSeparator());
  }

  @Test
  public void console_appender_writes_the_very_same_line_as_the_file_appender() throws IOException {
    File logsDir = temp.newFolder();
    props.set(PATH_LOGS.getKey(), logsDir.getAbsolutePath());
    props.set(WEB_ACCESSLOGS_TARGET.getKey(), "both");

    ProgrammaticLogbackValve valve = configureAndCaptureValve();
    PrintStream stdout = System.out;
    ByteArrayOutputStream console = new ByteArrayOutputStream();
    try (PrintStream captured = new PrintStream(console, true, StandardCharsets.UTF_8)) {
      System.setOut(captured);
      appendTo(valve, CONSOLE_APPENDER);
    } finally {
      System.setOut(stdout);
    }
    appendTo(valve, FILE_APPENDER);

    assertThat(console.toString(StandardCharsets.UTF_8))
      .isEqualTo(Files.readString(new File(logsDir, "access.log").toPath(), StandardCharsets.UTF_8));
  }

  /**
   * Logback-access renders %t in the JVM's default time zone, so the expected value cannot hardcode an offset.
   */
  private static String expectedTimestamp() {
    return DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z", Locale.US)
      .withZone(ZoneId.systemDefault())
      .format(Instant.ofEpochMilli(EVENT_TIMESTAMP));
  }

  private static void appendTo(ProgrammaticLogbackValve valve, String appenderName) {
    Appender<IAccessEvent> appender = valve.getAppender(appenderName);
    appender.doAppend(accessEvent());
    appender.stop();
  }

  private static IAccessEvent accessEvent() {
    IAccessEvent event = mock(IAccessEvent.class);
    when(event.getTimeStamp()).thenReturn(EVENT_TIMESTAMP);
    when(event.getRemoteHost()).thenReturn("1.2.3.4");
    when(event.getRemoteUser()).thenReturn("-");
    when(event.getRequestURL()).thenReturn("GET /api/issues/search?ps=1 HTTP/1.1");
    when(event.getStatusCode()).thenReturn(200);
    when(event.getContentLength()).thenReturn(12L);
    when(event.getRequestHeader("Referer")).thenReturn("https://sq.example.com/");
    when(event.getRequestHeader("User-Agent")).thenReturn("SonarScanner/6.0");
    when(event.getAttribute("ID")).thenReturn("AXbc123");
    when(event.getElapsedTime()).thenReturn(42L);
    return event;
  }

  @Test
  public void log_when_started_and_stopped() {
    Logger logger = mock(Logger.class);
    TomcatAccessLog.LifecycleLogger listener = new TomcatAccessLog.LifecycleLogger(logger);

    LifecycleEvent event = new LifecycleEvent(mock(Lifecycle.class), "before_init", null);
    listener.lifecycleEvent(event);
    verifyNoInteractions(logger);

    event = new LifecycleEvent(mock(Lifecycle.class), "after_start", null);
    listener.lifecycleEvent(event);
    verify(logger).debug("Tomcat is started");

    event = new LifecycleEvent(mock(Lifecycle.class), "after_destroy", null);
    listener.lifecycleEvent(event);
    verify(logger).debug("Tomcat is stopped");
  }

  private ProgrammaticLogbackValve configureAndCaptureValve() {
    Tomcat tomcat = mock(Tomcat.class, Mockito.RETURNS_DEEP_STUBS);
    underTest.configure(tomcat, props);

    ArgumentCaptor<ProgrammaticLogbackValve> captor = ArgumentCaptor.forClass(ProgrammaticLogbackValve.class);
    verify(tomcat.getHost().getPipeline()).addValve(captor.capture());
    return captor.getValue();
  }

  @SuppressWarnings("unchecked")
  private static Encoder<IAccessEvent> encoderOf(ProgrammaticLogbackValve valve, String appenderName) {
    Appender<IAccessEvent> appender = valve.getAppender(appenderName);
    if (appender instanceof FileAppender) {
      return ((FileAppender<IAccessEvent>) appender).getEncoder();
    }
    return ((ConsoleAppender<IAccessEvent>) appender).getEncoder();
  }
}
