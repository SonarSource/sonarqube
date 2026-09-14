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
package org.sonar.application.process;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.sonar.application.config.AppSettings;
import org.sonar.process.Props;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.process.ProcessProperties.Property.LOG_JSON_OUTPUT;
import static org.sonar.process.ProcessProperties.Property.WEB_ACCESSLOGS_ENABLE;
import static org.sonar.process.ProcessProperties.Property.WEB_ACCESSLOGS_TARGET;

public class StreamGobblerTest {

  private AppSettings appSettings = mock(AppSettings.class);
  private Props props = mock(Props.class);

  @Before
  public void before() {
    when(props.valueAsBoolean(LOG_JSON_OUTPUT.getKey(), false)).thenReturn(false);
    when(props.value(WEB_ACCESSLOGS_TARGET.getKey(), "file")).thenReturn("file");
    when(props.valueAsBoolean(WEB_ACCESSLOGS_ENABLE.getKey(), true)).thenReturn(true);
    when(appSettings.getProps()).thenReturn(props);
  }

  /**
   * Access logs only share a sub process's stdout for the Web process, and only once the target opts in.
   */
  private void accessLogsOnConsole() {
    when(props.valueAsBoolean(WEB_ACCESSLOGS_ENABLE.getKey(), true)).thenReturn(true);
    when(props.value(WEB_ACCESSLOGS_TARGET.getKey(), "file")).thenReturn("both");
  }

  @Test
  public void forward_stream_to_log() {
    InputStream stream = IOUtils.toInputStream("one\nsecond log\nthird log\n", StandardCharsets.UTF_8);
    Logger logger = mock(Logger.class);
    Logger startupLogger = mock(Logger.class);

    StreamGobbler gobbler = new StreamGobbler(stream, "WEB", appSettings, logger, startupLogger);
    verifyNoInteractions(logger);

    gobbler.start();
    StreamGobbler.waitUntilFinish(gobbler);

    verify(logger).info("one");
    verify(logger).info("second log");
    verify(logger).info("third log");
    verifyNoMoreInteractions(logger);
    verifyNoInteractions(startupLogger);
  }

  @Test
  public void startupLogIsLoggedWhenJSONFormatIsNotActive() {
    InputStream stream = IOUtils.toInputStream("[startup] Admin is still using default credentials\nsecond log\n",
      StandardCharsets.UTF_8);
    Logger startupLogger = mock(Logger.class);
    Logger logger = mock(Logger.class);

    StreamGobbler gobbler = new StreamGobbler(stream, "WEB", appSettings, logger, startupLogger);
    verifyNoInteractions(startupLogger);

    gobbler.start();
    StreamGobbler.waitUntilFinish(gobbler);

    verify(startupLogger).warn("Admin is still using default credentials");
    verifyNoMoreInteractions(startupLogger);
  }

  /*
   * This is scenario for known limitation of our approach when we detect more than we should - logs here are not really coming
   * from a startup log from subprocess but from some other log but the message contains '[startup]'
   */
  @Test
  public void startupLogIsLoggedWhenJSONFormatNotActiveAndMatchingStringIsIntMiddleOfTheTest() {
    InputStream stream = IOUtils.toInputStream("Some other not [startup] log\nsecond log\n",
      StandardCharsets.UTF_8);
    Logger startupLogger = mock(Logger.class);
    Logger logger = mock(Logger.class);

    StreamGobbler gobbler = new StreamGobbler(stream, "WEB", appSettings, logger, startupLogger);
    verifyNoInteractions(startupLogger);

    gobbler.start();
    StreamGobbler.waitUntilFinish(gobbler);

    verify(startupLogger).warn("log");
    verifyNoMoreInteractions(startupLogger);
  }

  @Test
  public void startupLogIsLoggedWhenJSONFormatIsActive() {
    when(props.valueAsBoolean(LOG_JSON_OUTPUT.getKey(), false)).thenReturn(true);
    InputStream stream = IOUtils.toInputStream("{ \"logger\": \"startup\", \"message\": \"Admin is still using default credentials\"}\n",
      StandardCharsets.UTF_8);
    Logger startupLogger = mock(Logger.class);
    Logger logger = mock(Logger.class);

    StreamGobbler gobbler = new StreamGobbler(stream, "WEB", appSettings, logger, startupLogger);
    verifyNoInteractions(startupLogger);

    gobbler.start();
    StreamGobbler.waitUntilFinish(gobbler);

    verify(startupLogger).warn("Admin is still using default credentials");
    verifyNoMoreInteractions(startupLogger);
  }








  @Test
  public void lineContainingStartupIsDroppedWhenAccessLogsAreNotOnConsole() {
    InputStream stream = IOUtils.toInputStream("Some other not startup log\nsecond log\n", StandardCharsets.UTF_8);
    Logger startupLogger = mock(Logger.class);
    Logger logger = mock(Logger.class);

    StreamGobbler gobbler = new StreamGobbler(stream, "web", appSettings, logger, startupLogger);

    gobbler.start();
    StreamGobbler.waitUntilFinish(gobbler);

    // unchanged from before this feature: only "second log" reaches sonar.log
    verify(logger).info("second log");
    verifyNoMoreInteractions(logger);
    verifyNoInteractions(startupLogger);
  }

  @Test
  public void lineContainingStartupIsDroppedWhenAccessLogsAreDisabled() {
    // target opts in, but access logs are off altogether: nothing but core logs reaches this stdout
    when(props.value(WEB_ACCESSLOGS_TARGET.getKey(), "file")).thenReturn("both");
    when(props.valueAsBoolean(WEB_ACCESSLOGS_ENABLE.getKey(), true)).thenReturn(false);
    InputStream stream = IOUtils.toInputStream("at org.apache.catalina.startup.Tomcat.start\nsecond log\n", StandardCharsets.UTF_8);
    Logger startupLogger = mock(Logger.class);
    Logger logger = mock(Logger.class);

    StreamGobbler gobbler = new StreamGobbler(stream, "web", appSettings, logger, startupLogger);

    gobbler.start();
    StreamGobbler.waitUntilFinish(gobbler);

    verify(logger).info("second log");
    verifyNoMoreInteractions(logger);
    verifyNoInteractions(startupLogger);
  }

  @Test
  public void accessLogContainingStartupIsForwardedVerbatimWhenAccessLogsAreOnConsole() {
    accessLogsOnConsole();
    String accessLog = "1.2.3.4 - - [12/Sep/2026:10:00:00 +0000] \"GET /startup HTTP/1.1\" 200 12 \"-\" \"curl [startup]\" \"AX\" 4";
    InputStream stream = IOUtils.toInputStream(accessLog + "\n", StandardCharsets.UTF_8);
    Logger startupLogger = mock(Logger.class);
    Logger logger = mock(Logger.class);

    StreamGobbler gobbler = new StreamGobbler(stream, "web", appSettings, logger, startupLogger);

    gobbler.start();
    StreamGobbler.waitUntilFinish(gobbler);

    // neither diverted to the startup logger nor dropped: an attacker-controlled User-Agent cannot forge a warning
    verify(logger).info(accessLog);
    verifyNoInteractions(startupLogger);
  }

  @Test
  public void realStartupLogIsStillRoutedWhenAccessLogsAreOnConsole() {
    accessLogsOnConsole();
    InputStream stream = IOUtils.toInputStream(
      "2026.09.14 16:29:50 WARN  app[][startup] Admin is still using default credentials\n", StandardCharsets.UTF_8);
    Logger startupLogger = mock(Logger.class);
    Logger logger = mock(Logger.class);

    StreamGobbler gobbler = new StreamGobbler(stream, "web", appSettings, logger, startupLogger);

    gobbler.start();
    StreamGobbler.waitUntilFinish(gobbler);

    verify(startupLogger).warn("Admin is still using default credentials");
    verifyNoInteractions(logger);
  }

  @Test
  public void lineEndingWithStartupMarkerDoesNotStopTheLogForwarding() {
    InputStream stream = IOUtils.toInputStream("""
      Some other not [startup]
      [startup] Admin is still using default credentials
      """, StandardCharsets.UTF_8);
    Logger startupLogger = mock(Logger.class);
    Logger logger = mock(Logger.class);

    StreamGobbler gobbler = new StreamGobbler(stream, "WEB", appSettings, logger, startupLogger);

    gobbler.start();
    StreamGobbler.waitUntilFinish(gobbler);

    // the first line used to throw and kill the thread, silencing the startup log that follows it
    verify(startupLogger).warn("");
    verify(startupLogger).warn("Admin is still using default credentials");
    verifyNoMoreInteractions(startupLogger);
  }

  @Test
  public void malformedJsonContainingStartupDoesNotStopTheLogForwarding() {
    when(props.valueAsBoolean(LOG_JSON_OUTPUT.getKey(), false)).thenReturn(true);
    InputStream stream = IOUtils.toInputStream("""
      { "logger": "startup", not json at all
      { "logger": "startup", "message": "Admin is still using default credentials"}
      """, StandardCharsets.UTF_8);
    Logger startupLogger = mock(Logger.class);
    Logger logger = mock(Logger.class);

    StreamGobbler gobbler = new StreamGobbler(stream, "WEB", appSettings, logger, startupLogger);

    gobbler.start();
    StreamGobbler.waitUntilFinish(gobbler);

    // the malformed line used to throw and kill the thread, silencing the startup log that follows it
    verify(startupLogger).warn("Admin is still using default credentials");
    verifyNoMoreInteractions(startupLogger);
  }

  @Test
  public void plainTextAccessLogIsForwardedWhenJsonLoggingIsEnabledAndAccessLogsAreOnConsole() {
    when(props.valueAsBoolean(LOG_JSON_OUTPUT.getKey(), false)).thenReturn(true);
    accessLogsOnConsole();
    String accessLog = "1.2.3.4 - - [12/Sep/2026:10:00:00 +0000] \"GET /startup HTTP/1.1\" 200 12";
    InputStream stream = IOUtils.toInputStream(accessLog + "\n", StandardCharsets.UTF_8);
    Logger startupLogger = mock(Logger.class);
    Logger logger = mock(Logger.class);

    StreamGobbler gobbler = new StreamGobbler(stream, "web", appSettings, logger, startupLogger);

    gobbler.start();
    StreamGobbler.waitUntilFinish(gobbler);

    verify(logger).info(accessLog);
    verifyNoInteractions(startupLogger);
  }

  @Test
  public void startupLogIsNotLoggedWhenJSONFormatIsActiveAndLogHasWrongName() {
    when(props.valueAsBoolean(LOG_JSON_OUTPUT.getKey(), false)).thenReturn(true);
    InputStream stream = IOUtils.toInputStream("{ \"logger\": \"wrong-logger\", \"message\": \"Admin 'startup' is still using default credentials\"}\n",
      StandardCharsets.UTF_8);
    Logger startupLogger = mock(Logger.class);
    Logger logger = mock(Logger.class);

    StreamGobbler gobbler = new StreamGobbler(stream, "WEB", appSettings, logger, startupLogger);
    verifyNoInteractions(startupLogger);

    gobbler.start();
    StreamGobbler.waitUntilFinish(gobbler);

    verifyNoMoreInteractions(startupLogger);
  }
}
