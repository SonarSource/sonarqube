/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.projecttag.ws;

import org.junit.Test;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class ProjectTagsWsTest {

  private ProjectTagsWs underTest = new ProjectTagsWs(singletonList(new FakeAction()));

  @Test
  public void definition() {
    WebService.Context context = new WebService.Context();

    underTest.define(context);

    WebService.Controller controller = context.controller("api/project_tags");
    assertThat(controller.path()).isEqualTo("api/project_tags");
    assertThat(controller.since()).isEqualTo("6.4");
    assertThat(controller.description()).isNotEmpty();
  }

  private static class FakeAction implements ProjectTagsWsAction {
    @Override
    public void define(WebService.NewController context) {
      context.createAction("blaba").setHandler(this);
    }

    @Override
    public void handle(Request request, Response response) {
      // do nothing
    }
  }
}
