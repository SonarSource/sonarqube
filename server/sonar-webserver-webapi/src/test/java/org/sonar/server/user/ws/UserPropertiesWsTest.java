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
package org.sonar.server.user.ws;

import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.ws.RemovedWebServiceHandler;

import static org.assertj.core.api.Assertions.assertThat;

public class UserPropertiesWsTest {

  private UserPropertiesWs underTest = new UserPropertiesWs();

  @Test
  public void define_ws() {
    WebService.Context context = new WebService.Context();

    underTest.define(context);

    WebService.Controller controller = context.controller("api/user_properties");
    assertThat(controller).isNotNull();
    assertThat(controller.description()).isNotEmpty();
    assertThat(controller.actions()).hasSize(1);

    WebService.Action index = controller.action("index");
    assertThat(index.since()).isEqualTo("2.6");
    assertThat(index.deprecatedSince()).isEqualTo("6.3");
    assertThat(index.handler()).isSameAs(RemovedWebServiceHandler.INSTANCE);
    assertThat(index.responseExample()).isEqualTo(RemovedWebServiceHandler.INSTANCE.getResponseExample());
  }

}
