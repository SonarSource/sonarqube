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
package org.sonar.server.pushapi;

import java.util.Arrays;
import org.junit.Test;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;

import static org.assertj.core.api.Assertions.assertThat;

public class ServerPushWsTest {

  private DummyServerPushAction dummyServerPushAction = new DummyServerPushAction();
  private ServerPushWs underTest = new ServerPushWs(Arrays.asList(dummyServerPushAction));

  @Test
  public void define_ws() {
    WebService.Context context = new WebService.Context();

    underTest.define(context);

    WebService.Controller controller = context.controller("api/push");
    assertThat(controller).isNotNull();
    assertThat(controller.path()).isEqualTo("api/push");
    assertThat(controller.since()).isEqualTo("9.4");
    assertThat(controller.description()).isNotEmpty();
    assertThat(controller.actions()).isNotEmpty();
  }

  private static class DummyServerPushAction extends ServerPushAction {

    @Override
    public void define(WebService.NewController context) {
      context.createAction("foo").setHandler(this);
    }

    @Override
    public void handle(Request request, Response response) {

    }
  }
}
