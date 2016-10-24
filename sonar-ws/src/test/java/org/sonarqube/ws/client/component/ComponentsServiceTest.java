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

package org.sonarqube.ws.client.component;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService.Param;
import org.sonarqube.ws.client.ServiceTester;
import org.sonarqube.ws.client.WsConnector;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.mock;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_FILTER;

public class ComponentsServiceTest {

  @Rule
  public ServiceTester<ComponentsService> serviceTester = new ServiceTester<>(new ComponentsService(mock(WsConnector.class)));

  private ComponentsService underTest = serviceTester.getInstanceUnderTest();

  @Test
  public void search_projects() {
    underTest.searchProjects(SearchProjectsRequest.builder()
      .setFilter("ncloc > 10")
      .setFacets(singletonList("ncloc"))
      .setPage(3)
      .setPageSize(10)
      .build());

    serviceTester.assertThat(serviceTester.getGetRequest())
      .hasPath("search_projects")
      .hasParam(PARAM_FILTER, "ncloc > 10")
      .hasParam(Param.FACETS, singletonList("ncloc"))
      .hasParam(Param.PAGE, 3)
      .hasParam(Param.PAGE_SIZE, 10)
      .andNoOtherParam();
  }

}
