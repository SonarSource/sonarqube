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

public class StreamGobblerTest {

  private AppSettings appSettings = mock(AppSettings.class);
  private Props props = mock(Props.class);

  @Before
  public void before() {
    when(props.valueAsBoolean(LOG_JSON_OUTPUT.getKey(), false)).thenReturn(false);
    when(appSettings.getProps()).thenReturn(props);
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
