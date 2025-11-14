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
package org.sonar.server.monitoring;

import org.junit.Test;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;

import static org.assertj.core.api.Assertions.assertThat;

public class MonitoringWsTest {

  private final MonitoringWs underTest = new MonitoringWs(new MonitoringWsAction() {
    @Override
    public void handle(Request request, Response response) {
      // nothing to do
    }

    @Override
    public void define(WebService.NewController context) {
      context.createAction("foo").setHandler(this);
    }
  });

  @Test
  public void define_controller() {
    WebService.Context context = new WebService.Context();

    underTest.define(context);

    WebService.Controller controller = context.controller("api/monitoring");
    assertThat(controller).isNotNull();
    assertThat(controller.description()).isNotEmpty();
    assertThat(controller.since()).isEqualTo("9.3");
    assertThat(controller.actions()).hasSize(1);
  }

}
