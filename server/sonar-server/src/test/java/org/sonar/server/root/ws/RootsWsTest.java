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
package org.sonar.server.root.ws;

import org.junit.Test;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;

public class RootsWsTest {
  private RootsWs underTest = new RootsWs(new DummyRootsWsAction());
  private WsTester wsTester = new WsTester(underTest);

  @Test
  public void verify_definition() {
    assertThat(wsTester.context().controllers()).hasSize(1);
    WebService.Controller controller = wsTester.context().controller("api/roots");
    assertThat(controller.description()).isEqualTo("Manage root users");
    assertThat(controller.since()).isEqualTo("6.2");
  }

  private static class DummyRootsWsAction implements RootsWsAction {
    @Override
    public void define(WebService.NewController context) {
      context.createAction("ooo").setHandler(this);
    }

    @Override
    public void handle(Request request, Response response) {

    }
  }
}
