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
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.platform.Platform;
import org.sonar.server.ws.WsTester;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class SystemRestartWsActionTest {

  Settings settings = new Settings();
  Platform platform = mock(Platform.class);
  SystemRestartWsAction sut = new SystemRestartWsAction(settings, platform);

  @Test
  public void restart_if_dev_mode() throws Exception {
    settings.setProperty("sonar.web.dev", true);

    SystemWs ws = new SystemWs(sut);

    WsTester tester = new WsTester(ws);
    tester.newPostRequest("api/system", "restart").execute();
    verify(platform).restart();
  }

  @Test
  public void fail_if_production_mode() throws Exception {
    SystemWs ws = new SystemWs(sut);

    WsTester tester = new WsTester(ws);
    try {
      tester.newPostRequest("api/system", "restart").execute();
      fail();
    } catch (ForbiddenException e) {
      verifyZeroInteractions(platform);
    }
  }
}
