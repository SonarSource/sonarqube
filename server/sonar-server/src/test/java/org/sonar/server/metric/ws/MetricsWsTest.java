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
package org.sonar.server.metric.ws;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.server.user.UserSession;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class MetricsWsTest {

  WsTester ws;

  @Before
  public void setUp() {
    DbClient dbClient = mock(DbClient.class);
    UserSession userSession = mock(UserSession.class);
    ws = new WsTester(new MetricsWs(
      new SearchAction(dbClient),
      new CreateAction(dbClient, userSession),
      new UpdateAction(dbClient, userSession),
      new DeleteAction(dbClient, userSession),
      new TypesAction(),
      new DomainsAction(dbClient)));
  }

  @Test
  public void define_ws() {
    WebService.Controller controller = ws.controller("api/metrics");
    assertThat(controller).isNotNull();
    assertThat(controller.description()).isNotEmpty();
    assertThat(controller.actions()).hasSize(6);
  }

}
