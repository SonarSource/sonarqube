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
import org.sonar.server.platform.Platform;
import org.sonar.server.tester.AnonymousMockUserSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class SystemWsTest {

  @Test
  public void define() {
    RestartAction action1 = new RestartAction(mock(Settings.class), mock(Platform.class));
    InfoAction action2 = new InfoAction(new AnonymousMockUserSession());
    SystemWs ws = new SystemWs(action1, action2);
    WebService.Context context = new WebService.Context();

    ws.define(context);

    assertThat(context.controllers()).hasSize(1);
    assertThat(context.controller("api/system").actions()).hasSize(2);
    assertThat(context.controller("api/system").action("info")).isNotNull();
  }
}
