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
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.server.platform.Platform;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class SystemWsTest {

  @Test
  public void define() throws Exception {
    Platform platform = mock(Platform.class);
    Settings settings = new Settings();
    RestartHandler restartHandler = new RestartHandler(settings, platform, mock(System2.class));
    SystemWs ws = new SystemWs(restartHandler);
    WebService.Context context = new WebService.Context();

    ws.define(context);

    assertThat(context.controllers()).hasSize(1);
    assertThat(context.controller("api/system")).isNotNull();
    assertThat(context.controller("api/system").actions()).isNotEmpty();
  }
}
