/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.sonar.api.server.ws.RailsHandler;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.ws.WsTester;

public class AuthenticationWsTest {

  WsTester tester = new WsTester(new AuthenticationWs());

  @Test
  public void define_ws() {
    WebService.Controller controller = tester.controller("api/authentication");
    assertThat(controller).isNotNull();
    assertThat(controller.description()).isNotEmpty();
    assertThat(controller.actions()).hasSize(2);

    WebService.Action validate = controller.action("validate");
    assertThat(validate).isNotNull();
    assertThat(validate.handler()).isInstanceOf(RailsHandler.class);
    assertThat(validate.responseExampleAsString()).isNotEmpty();
    assertThat(validate.params()).hasSize(1);

    WebService.Action login = controller.action("login");
    assertThat(login).isNotNull();
    assertThat(login.handler()).isNotNull();
    assertThat(login.isPost()).isTrue();
    assertThat(login.params()).hasSize(2);
  }
}
