/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonarqube.ws.client.favorite;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService.Param;
import org.sonarqube.ws.client.ServiceTester;
import org.sonarqube.ws.client.WsConnector;

import static org.mockito.Mockito.mock;
import static org.sonarqube.ws.client.favorite.FavoritesWsParameters.PARAM_COMPONENT;

public class FavoritesServiceTest {
  @Rule
  public ServiceTester<FavoritesService> serviceTester = new ServiceTester<>(new FavoritesService(mock(WsConnector.class)));

  private FavoritesService underTest = serviceTester.getInstanceUnderTest();

  @Test
  public void add() {
    underTest.add("my_project");

    serviceTester.assertThat(serviceTester.getPostRequest())
      .hasPath("add")
      .hasParam(PARAM_COMPONENT, "my_project")
      .andNoOtherParam();
  }

  @Test
  public void remove() {
    underTest.remove("my_project");

    serviceTester.assertThat(serviceTester.getPostRequest())
      .hasPath("remove")
      .hasParam(PARAM_COMPONENT, "my_project")
      .andNoOtherParam();
  }

  @Test
  public void search() {
    underTest.search(new SearchRequest().setPage(42).setPageSize(255));

    serviceTester.assertThat(serviceTester.getGetRequest())
      .hasPath("search")
      .hasParam(Param.PAGE, 42)
      .hasParam(Param.PAGE_SIZE, 255)
      .andNoOtherParam();
  }
}
