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
package org.sonar.server.platform.ws;

import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.api.server.ws.WsTester;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.platform.Platform;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class RestartHandlerTest {

  @Test
  public void restart_if_dev_mode() throws Exception {
    Platform platform = mock(Platform.class);
    Settings settings = new Settings();
    settings.setProperty("sonar.dev", true);
    RestartHandler restartHandler = new RestartHandler(settings, platform);
    PlatformWs ws = new PlatformWs(restartHandler);

    WsTester tester = new WsTester(ws);
    tester.newRequest("restart").execute();

    verify(platform).restartLevel3Container();
  }

  @Test
  public void fail_if_production_mode() throws Exception {
    Platform platform = mock(Platform.class);
    Settings settings = new Settings();
    RestartHandler restartHandler = new RestartHandler(settings, platform);
    PlatformWs ws = new PlatformWs(restartHandler);

    WsTester tester = new WsTester(ws);
    try {
      tester.newRequest("restart").execute();
      fail();
    } catch (BadRequestException e) {
      assertThat(e).hasMessage("Available in development mode only (sonar.dev=true)");
      verifyZeroInteractions(platform);
    }
  }
}
