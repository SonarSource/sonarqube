/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.newcodeperiod.ws;

import org.junit.Test;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.documentation.DocumentationLinkGenerator;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class NewCodePeriodsWsTest {
  private String actionKey = randomAlphanumeric(10);
  private DocumentationLinkGenerator documentationLinkGenerator = mock(DocumentationLinkGenerator.class);


  private NewCodePeriodsWs underTest = new NewCodePeriodsWs(
    documentationLinkGenerator, new NewCodePeriodsWsAction() {
      @Override
      public void define(WebService.NewController context) {
        context.createAction(actionKey).setHandler(this);
      }

      @Override
      public void handle(Request request, Response response) {

      }
    }
  );

  @Test
  public void define_ws() {
    WebService.Context context = new WebService.Context();

    underTest.define(context);

    WebService.Controller controller = context.controller("api/new_code_periods");
    assertThat(controller).isNotNull();
    assertThat(controller.description()).isNotEmpty();
    assertThat(controller.actions()).hasSize(1);
  }

}
