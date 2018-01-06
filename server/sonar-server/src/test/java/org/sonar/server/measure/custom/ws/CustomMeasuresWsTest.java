/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.measure.custom.ws;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.server.measure.custom.ws.CustomMeasuresWs.ENDPOINT;

public class CustomMeasuresWsTest {
  WsTester ws;

  @Before
  public void setUp() {
    DbClient dbClient = mock(DbClient.class);
    UserSession userSession = mock(UserSession.class);
    ws = new WsTester(new CustomMeasuresWs(
      new DeleteAction(dbClient, userSession),
      new CreateAction(dbClient, userSession, System2.INSTANCE, mock(CustomMeasureValidator.class), mock(CustomMeasureJsonWriter.class), mock(ComponentFinder.class)),
      new UpdateAction(dbClient, userSession, System2.INSTANCE, mock(CustomMeasureValidator.class), mock(CustomMeasureJsonWriter.class))
    ));
  }

  @Test
  public void define_ws() {
    WebService.Controller controller = ws.controller("api/custom_measures");
    assertThat(controller).isNotNull();
    assertThat(controller.description()).isNotEmpty();
    assertThat(controller.actions()).hasSize(3);
  }

  @Test
  public void delete_action_properties() {
    WebService.Action deleteAction = ws.controller(ENDPOINT).action("delete");
    assertThat(deleteAction.isPost()).isTrue();
  }

  @Test
  public void create_action_properties() {
    WebService.Action action = ws.controller(ENDPOINT).action("create");
    assertThat(action.isPost()).isTrue();
  }

  @Test
  public void create_update_properties() {
    WebService.Action action = ws.controller(ENDPOINT).action("update");
    assertThat(action.isPost()).isTrue();
  }
}
