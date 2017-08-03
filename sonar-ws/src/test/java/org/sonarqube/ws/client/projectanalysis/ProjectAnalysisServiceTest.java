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
package org.sonarqube.ws.client.projectanalysis;

import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.ws.ProjectAnalyses;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.ServiceTester;
import org.sonarqube.ws.client.WsConnector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonarqube.ws.client.projectanalysis.EventCategory.QUALITY_GATE;
import static org.sonarqube.ws.client.projectanalysis.ProjectAnalysesWsParameters.PARAM_BRANCH;
import static org.sonarqube.ws.client.projectanalysis.ProjectAnalysesWsParameters.PARAM_CATEGORY;
import static org.sonarqube.ws.client.projectanalysis.ProjectAnalysesWsParameters.PARAM_PROJECT;

public class ProjectAnalysisServiceTest {

  @Rule
  public ServiceTester<ProjectAnalysisService> serviceTester = new ServiceTester<>(new ProjectAnalysisService(mock(WsConnector.class)));

  private ProjectAnalysisService underTest = serviceTester.getInstanceUnderTest();

  @Test
  public void search() {
    underTest.search(SearchRequest.builder()
      .setProject("project")
      .setBranch("my_branch")
      .setCategory(QUALITY_GATE)
      .setPage(10)
      .setPageSize(50)
    .build());
    GetRequest getRequest = serviceTester.getGetRequest();

    assertThat(serviceTester.getGetParser()).isSameAs(ProjectAnalyses.SearchResponse.parser());
    serviceTester.assertThat(getRequest)
      .hasParam(PARAM_PROJECT, "project")
      .hasParam(PARAM_BRANCH, "my_branch")
      .hasParam(PARAM_CATEGORY, QUALITY_GATE.name())
      .hasParam(PAGE, 10)
      .hasParam(PAGE_SIZE, 50)
      .andNoOtherParam();
  }

  @Test
  public void search_with_minimal_fields() {
    underTest.search(SearchRequest.builder()
      .setProject("project")
      .build());
    GetRequest getRequest = serviceTester.getGetRequest();

    assertThat(serviceTester.getGetParser()).isSameAs(ProjectAnalyses.SearchResponse.parser());
    serviceTester.assertThat(getRequest)
      .hasParam(PARAM_PROJECT, "project")
      .hasParam(PAGE, 1)
      .hasParam(PAGE_SIZE, 100)
      .andNoOtherParam();
  }

}
