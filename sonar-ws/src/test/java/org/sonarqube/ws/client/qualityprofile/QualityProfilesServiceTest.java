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
import org.sonarqube.ws.Common.Severity;
import org.sonarqube.ws.QualityProfiles.SearchWsResponse;
import org.sonarqube.ws.QualityProfiles.ShowResponse;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.ServiceTester;
import org.sonarqube.ws.client.WsConnector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_COMPARE_TO_SONAR_WAY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_DEFAULTS;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_FROM_KEY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_LANGUAGE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PARAMS;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PROFILE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PROFILE_KEY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PROFILE_NAME;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PROJECT_KEY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_RULE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_SEVERITY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_TO_NAME;

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

    assertThat(serviceTester.getGetParser()).isSameAs(SearchWsResponse.parser());
    serviceTester.assertThat(getRequest)
      .hasPath("search")
      .hasParam(PARAM_DEFAULTS, true)
      .hasParam(PARAM_PROJECT_KEY, "project")
      .hasParam(PARAM_LANGUAGE, "language")
      .hasParam(PARAM_PROFILE_NAME, "profile")
      .andNoOtherParam();
  }

  @Test
  public void show() {
    underTest.show(new ShowRequest()
      .setProfile("profile")
      .setCompareToSonarWay(true));
    GetRequest getRequest = serviceTester.getGetRequest();

    assertThat(serviceTester.getGetParser()).isSameAs(ShowResponse.parser());
    serviceTester.assertThat(getRequest)
      .hasPath("show")
      .hasParam(PARAM_PROFILE, "profile")
      .hasParam(PARAM_COMPARE_TO_SONAR_WAY, true)
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
      .hasPath("add_project")
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
      .hasPath("remove_project")
      .hasParam(PARAM_LANGUAGE, "xoo")
      .hasParam(PARAM_PROFILE_NAME, "Sonar Way")
      .hasParam(PARAM_PROJECT_KEY, "sample")
      .andNoOtherParam();
  }

  @Test
  public void create() throws Exception {
    underTest.create(CreateRequest.builder()
      .setLanguage("xoo")
      .setProfileName("Sonar Way")
      .build());

    serviceTester.assertThat(serviceTester.getPostRequest())
      .hasPath("create")
      .hasParam(PARAM_LANGUAGE, "xoo")
      .hasParam(PARAM_PROFILE_NAME, "Sonar Way")
      .andNoOtherParam();
  }

  @Test
  public void copy() throws Exception {
    underTest.copy(new CopyRequest("fromKey", "My Sonar Way"));

    serviceTester.assertThat(serviceTester.getPostRequest())
      .hasPath("copy")
      .hasParam(PARAM_FROM_KEY, "fromKey")
      .hasParam(PARAM_TO_NAME, "My Sonar Way")
      .andNoOtherParam();
  }

  @Test
  public void set_default() {
    underTest.setDefault(new SetDefaultRequest("sample"));

    serviceTester.assertThat(serviceTester.getPostRequest())
      .hasPath("set_default")
      .hasParam(PARAM_PROFILE_KEY, "sample")
      .andNoOtherParam();
  }

  @Test
  public void delete() {
    underTest.delete("sample");

    serviceTester.assertThat(serviceTester.getPostRequest())
      .hasPath("delete")
      .hasParam(PARAM_PROFILE_KEY, "sample")
      .andNoOtherParam();
  }

  @Test
  public void deactivate_rule() {
    underTest.deactivateRule("P1", "R1");
    PostRequest request = serviceTester.getPostRequest();

    serviceTester.assertThat(request)
      .hasPath("deactivate_rule")
      .hasParam(PARAM_PROFILE, "P1")
      .hasParam(PARAM_RULE, "R1")
      .andNoOtherParam();
  }

  @Test
  public void activate_rule() {
    underTest.activateRule(ActivateRuleWsRequest.builder()
      .setRuleKey("R1")
      .setProfileKey("P1")
      .setOrganization("O1")
      .setParams("PS1")
      .setSeverity(Severity.INFO)
      .build());
    PostRequest request = serviceTester.getPostRequest();

    serviceTester.assertThat(request)
      .hasPath("activate_rule")
      .hasParam(PARAM_PROFILE, "P1")
      .hasParam(PARAM_RULE, "R1")
      .hasParam(PARAM_ORGANIZATION, "O1")
      .hasParam(PARAM_PARAMS, "PS1")
      .hasParam(PARAM_SEVERITY, Severity.INFO.toString())
      .andNoOtherParam();
  }
}
