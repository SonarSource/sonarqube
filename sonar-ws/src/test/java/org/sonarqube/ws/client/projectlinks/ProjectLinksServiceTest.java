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
package org.sonarqube.ws.client.projectlinks;

import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.ws.WsProjectLinks.CreateWsResponse;
import org.sonarqube.ws.WsProjectLinks.SearchWsResponse;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.ServiceTester;
import org.sonarqube.ws.client.WsConnector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonarqube.ws.client.projectlinks.ProjectLinksWsParameters.PARAM_ID;
import static org.sonarqube.ws.client.projectlinks.ProjectLinksWsParameters.PARAM_NAME;
import static org.sonarqube.ws.client.projectlinks.ProjectLinksWsParameters.PARAM_PROJECT_ID;
import static org.sonarqube.ws.client.projectlinks.ProjectLinksWsParameters.PARAM_PROJECT_KEY;
import static org.sonarqube.ws.client.projectlinks.ProjectLinksWsParameters.PARAM_URL;

public class ProjectLinksServiceTest {
  private static final String PROJECT_ID_VALUE = "project_id_value";
  private static final String PROJECT_KEY_VALUE = "project_key_value";
  private static final String NAME_VALUE = "name_value";
  private static final String URL_VALUE = "url_value";
  private static final long ID_VALUE = 175;

  @Rule
  public ServiceTester<ProjectLinksService> serviceTester = new ServiceTester<>(new ProjectLinksService(mock(WsConnector.class)));

  private ProjectLinksService underTest = serviceTester.getInstanceUnderTest();

  @Test
  public void search_does_GET_request() {
    underTest.search(new SearchWsRequest()
      .setProjectId(PROJECT_ID_VALUE)
      .setProjectKey(PROJECT_KEY_VALUE));

    assertThat(serviceTester.getGetParser()).isSameAs(SearchWsResponse.parser());

    GetRequest getRequest = serviceTester.getGetRequest();

    serviceTester.assertThat(getRequest)
      .hasPath("search")
      .hasParam(PARAM_PROJECT_ID, PROJECT_ID_VALUE)
      .hasParam(PARAM_PROJECT_KEY, PROJECT_KEY_VALUE)
      .andNoOtherParam();
  }

  @Test
  public void create_does_POST_request() {
    underTest.create(new CreateWsRequest()
      .setProjectId(PROJECT_ID_VALUE)
      .setProjectKey(PROJECT_KEY_VALUE)
      .setName(NAME_VALUE)
      .setUrl(URL_VALUE));

    assertThat(serviceTester.getPostParser()).isSameAs(CreateWsResponse.parser());

    PostRequest postRequest = serviceTester.getPostRequest();

    serviceTester.assertThat(postRequest)
      .hasPath("create")
      .hasParam(PARAM_PROJECT_ID, PROJECT_ID_VALUE)
      .hasParam(PARAM_PROJECT_KEY, PROJECT_KEY_VALUE)
      .hasParam(PARAM_NAME, NAME_VALUE)
      .hasParam(PARAM_URL, URL_VALUE)
      .andNoOtherParam();
  }

  @Test
  public void delete_does_POST_request() {
    underTest.delete(new DeleteWsRequest().setId(ID_VALUE));

    assertThat(serviceTester.getPostParser()).isNull();

    PostRequest postRequest = serviceTester.getPostRequest();

    serviceTester.assertThat(postRequest)
      .hasPath("delete")
      .hasParam(PARAM_ID, String.valueOf(ID_VALUE))
      .andNoOtherParam();
  }
}
