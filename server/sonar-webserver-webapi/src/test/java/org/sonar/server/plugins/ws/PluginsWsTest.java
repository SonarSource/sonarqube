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
package org.sonar.server.plugins.ws;

import org.junit.Test;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;

import static org.assertj.core.api.Assertions.assertThat;

public class PluginsWsTest {
  private PluginsWs underTest = new PluginsWs(new DummyPluginsWsAction());

  @Test
  public void defines_controller_and_binds_PluginsWsActions() {
    WebService.Context context = new WebService.Context();

    underTest.define(context);

    WebService.Controller controller = context.controller("api/plugins");
    assertThat(controller).isNotNull();
    assertThat(controller.since()).isEqualTo("5.2");
    assertThat(controller.description()).isNotEmpty();
    assertThat(controller.actions()).extracting("key").containsOnly("dummy");
  }

  private static class DummyPluginsWsAction implements PluginsWsAction {
    @Override
    public void define(WebService.NewController context) {
      context
        .createAction("dummy")
        .setDescription("Dummy Description")
        .setPost(true)
        .setSince("5.3")
        .setHandler(this);
    }

    @Override
    public void handle(Request request, Response response) {
      // not relevant to test
    }
  }
}
