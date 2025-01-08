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
package org.sonar.server.projectdump.ws;

import org.junit.Test;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;

import static org.assertj.core.api.Assertions.assertThat;

public class ProjectDumpWsTest {
  @Test
  public void testDefine() {
    ProjectDumpWs underTest = new ProjectDumpWs(new DumbAction());

    WebService.Context context = new WebService.Context();
    underTest.define(context);

    WebService.Controller controller = context.controller("api/project_dump");
    assertThat(controller.since()).isNotEmpty();
    assertThat(controller.description()).isNotEmpty();
    assertThat(controller.isInternal()).isFalse();
    assertThat(controller.action("dumb")).isNotNull();
  }

  private static class DumbAction implements ProjectDumpAction {
    @Override
    public void define(WebService.NewController newController) {
      newController.createAction("dumb").setHandler(this);
    }

    @Override
    public void handle(Request request, Response response) {

    }
  }
}
