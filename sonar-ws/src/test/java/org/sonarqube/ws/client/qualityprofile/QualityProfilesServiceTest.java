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
package org.sonarqube.ws.client.qualityprofile;

import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.ws.QualityProfiles;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.ServiceTester;
import org.sonarqube.ws.client.WsConnector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_DEFAULTS;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_LANGUAGE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PROFILE_NAME;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PROJECT_KEY;

public class QualityProfilesServiceTest {

  @Rule
  public ServiceTester<QualityProfilesService> serviceTester = new ServiceTester<>(new QualityProfilesService(mock(WsConnector.class)));

  private QualityProfilesService underTest = serviceTester.getInstanceUnderTest();

  @Test
  public void search() {
    underTest.search(new SearchWsRequest()
      .setDefaults(true)
      .setProjectKey("project")
      .setLanguage("language")
      .setProfileName("profile"));
    GetRequest getRequest = serviceTester.getGetRequest();

    assertThat(serviceTester.getGetParser()).isSameAs(QualityProfiles.SearchWsResponse.parser());
    serviceTester.assertThat(getRequest)
      .hasParam(PARAM_DEFAULTS, true)
      .hasParam(PARAM_PROJECT_KEY, "project")
      .hasParam(PARAM_LANGUAGE, "language")
      .hasParam(PARAM_PROFILE_NAME, "profile")
      .andNoOtherParam();
  }

  @Test
  public void add_project() throws Exception {
    underTest.addProject(AddProjectRequest.builder()
      .setLanguage("xoo")
      .setProfileName("Sonar Way")
      .setProjectKey("sample")
      .build());

    serviceTester.assertThat(serviceTester.getPostRequest())
      .hasParam(PARAM_LANGUAGE, "xoo")
      .hasParam(PARAM_PROFILE_NAME, "Sonar Way")
      .hasParam(PARAM_PROJECT_KEY, "sample")
      .andNoOtherParam();
  }

  @Test
  public void remove_project() throws Exception {
    underTest.removeProject(RemoveProjectRequest.builder()
      .setLanguage("xoo")
      .setProfileName("Sonar Way")
      .setProjectKey("sample")
      .build());

    serviceTester.assertThat(serviceTester.getPostRequest())
      .hasParam(PARAM_LANGUAGE, "xoo")
      .hasParam(PARAM_PROFILE_NAME, "Sonar Way")
      .hasParam(PARAM_PROJECT_KEY, "sample")
      .andNoOtherParam();
  }

  @Test
  public void create_project() throws Exception {
    underTest.create(CreateRequest.builder()
      .setLanguage("xoo")
      .setProfileName("Sonar Way")
      .build());

    serviceTester.assertThat(serviceTester.getPostRequest())
      .hasParam(PARAM_LANGUAGE, "xoo")
      .hasParam(PARAM_PROFILE_NAME, "Sonar Way")
      .andNoOtherParam();
  }
}
