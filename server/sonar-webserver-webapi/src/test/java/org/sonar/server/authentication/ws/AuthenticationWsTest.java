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
package org.sonar.server.authentication.ws;

import java.util.Collections;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;

import static org.assertj.core.api.Assertions.assertThat;

public class AuthenticationWsTest {
  private AuthenticationWs underTest = new AuthenticationWs(Collections.singletonList(new AuthenticationWsAction() {
    @Override
    public void define(WebService.NewController controller) {
      controller.createAction("foo")
        .setHandler((request, response) -> {
          throw new UnsupportedOperationException("not implemented");
        });
    }
  }));

  @Test
  public void define_ws() {
    WebService.Context context = new WebService.Context();

    underTest.define(context);

    WebService.Controller controller = context.controller("api/authentication");
    assertThat(controller).isNotNull();
    assertThat(controller.description()).isNotEmpty();
    assertThat(controller.actions()).hasSize(1);

    WebService.Action fooAction = controller.action("foo");
    assertThat(fooAction).isNotNull();
    assertThat(fooAction.handler()).isNotNull();
  }
}
