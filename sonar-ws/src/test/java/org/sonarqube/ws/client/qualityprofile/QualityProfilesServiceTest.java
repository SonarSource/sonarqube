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
package org.sonarqube.ws.client.qualityprofile;

import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.ws.Common.Severity;
import org.sonarqube.ws.QualityProfiles;
import org.sonarqube.ws.QualityProfiles.SearchWsResponse;
import org.sonarqube.ws.QualityProfiles.ShowResponse;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.ServiceTester;
import org.sonarqube.ws.client.WsConnector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.server.ws.WebService.Param.SELECTED;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_COMPARE_TO_SONAR_WAY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_DEFAULTS;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_FROM_KEY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_GROUP;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_LANGUAGE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_NAME;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_LOGIN;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PARAMS;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_KEY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_QUALITY_PROFILE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PROJECT_KEY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_QUALITY_PROFILE;
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
      .setQualityProfile("profile"));
    GetRequest getRequest = serviceTester.getGetRequest();

    assertThat(serviceTester.getGetParser()).isSameAs(SearchWsResponse.parser());
    serviceTester.assertThat(getRequest)
      .hasPath("search")
      .hasParam(PARAM_DEFAULTS, true)
      .hasParam(PARAM_PROJECT_KEY, "project")
      .hasParam(PARAM_LANGUAGE, "language")
      .hasParam(PARAM_QUALITY_PROFILE, "profile")
      .andNoOtherParam();
  }

  @Test
  public void show() {
    underTest.show(new ShowRequest()
      .setKey("profile")
      .setCompareToSonarWay(true));
    GetRequest getRequest = serviceTester.getGetRequest();

    assertThat(serviceTester.getGetParser()).isSameAs(ShowResponse.parser());
    serviceTester.assertThat(getRequest)
      .hasPath("show")
      .hasParam(PARAM_KEY, "profile")
      .hasParam(PARAM_COMPARE_TO_SONAR_WAY, true)
      .andNoOtherParam();
  }

  @Test
  public void add_project() throws Exception {
    underTest.addProject(AddProjectRequest.builder()
      .setLanguage("xoo")
      .setQualityProfile("Sonar Way")
      .setProjectKey("sample")
      .build());

    serviceTester.assertThat(serviceTester.getPostRequest())
      .hasPath("add_project")
      .hasParam(PARAM_LANGUAGE, "xoo")
      .hasParam(PARAM_QUALITY_PROFILE, "Sonar Way")
      .hasParam(PARAM_PROJECT_KEY, "sample")
      .andNoOtherParam();
  }

  @Test
  public void remove_project() throws Exception {
    underTest.removeProject(RemoveProjectRequest.builder()
      .setLanguage("xoo")
      .setQualityProfile("Sonar Way")
      .setProjectKey("sample")
      .build());

    serviceTester.assertThat(serviceTester.getPostRequest())
      .hasPath("remove_project")
      .hasParam(PARAM_LANGUAGE, "xoo")
      .hasParam(PARAM_QUALITY_PROFILE, "Sonar Way")
      .hasParam(PARAM_PROJECT_KEY, "sample")
      .andNoOtherParam();
  }

  @Test
  public void create() throws Exception {
    underTest.create(CreateRequest.builder()
      .setLanguage("xoo")
      .setName("Sonar Way")
      .build());

    serviceTester.assertThat(serviceTester.getPostRequest())
      .hasPath("create")
      .hasParam(PARAM_LANGUAGE, "xoo")
      .hasParam(PARAM_NAME, "Sonar Way")
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
      .hasParam(QualityProfileWsParameters.PARAM_KEY, "sample")
      .andNoOtherParam();
  }

  @Test
  public void delete() {
    underTest.delete("sample");

    serviceTester.assertThat(serviceTester.getPostRequest())
      .hasPath("delete")
      .hasParam(QualityProfileWsParameters.PARAM_KEY, "sample")
      .andNoOtherParam();
  }

  @Test
  public void deactivate_rule() {
    underTest.deactivateRule("P1", "R1");
    PostRequest request = serviceTester.getPostRequest();

    serviceTester.assertThat(request)
      .hasPath("deactivate_rule")
      .hasParam(PARAM_KEY, "P1")
      .hasParam(PARAM_RULE, "R1")
      .andNoOtherParam();
  }

  @Test
  public void activate_rule() {
    underTest.activateRule(ActivateRuleWsRequest.builder()
      .setRuleKey("R1")
      .setKey("P1")
      .setOrganization("O1")
      .setParams("PS1")
      .setSeverity(Severity.INFO)
      .build());
    PostRequest request = serviceTester.getPostRequest();

    serviceTester.assertThat(request)
      .hasPath("activate_rule")
      .hasParam(PARAM_KEY, "P1")
      .hasParam(PARAM_RULE, "R1")
      .hasParam(PARAM_ORGANIZATION, "O1")
      .hasParam(PARAM_PARAMS, "PS1")
      .hasParam(PARAM_SEVERITY, Severity.INFO.toString())
      .andNoOtherParam();
  }

  @Test
  public void add_user() {
    underTest.addUser(AddUserRequest.builder()
      .setOrganization("O1")
      .setQualityProfile("P1")
      .setLanguage("Xoo")
      .setUserLogin("john")
      .build());
    PostRequest request = serviceTester.getPostRequest();

    serviceTester.assertThat(request)
      .hasPath("add_user")
      .hasParam(PARAM_ORGANIZATION, "O1")
      .hasParam(PARAM_QUALITY_PROFILE, "P1")
      .hasParam(PARAM_LANGUAGE, "Xoo")
      .hasParam(PARAM_LOGIN, "john")
      .andNoOtherParam();
  }

  @Test
  public void remove_user() {
    underTest.removeUser(RemoveUserRequest.builder()
      .setOrganization("O1")
      .setQualityProfile("P1")
      .setLanguage("Xoo")
      .setUserLogin("john")
      .build());
    PostRequest request = serviceTester.getPostRequest();

    serviceTester.assertThat(request)
      .hasPath("remove_user")
      .hasParam(PARAM_ORGANIZATION, "O1")
      .hasParam(PARAM_QUALITY_PROFILE, "P1")
      .hasParam(PARAM_LANGUAGE, "Xoo")
      .hasParam(PARAM_LOGIN, "john")
      .andNoOtherParam();
  }

  @Test
  public void search_users() {
    underTest.searchUsers(SearchUsersRequest.builder()
      .setOrganization("O1")
      .setQualityProfile("P1")
      .setLanguage("Xoo")
      .setQuery("john")
      .setSelected("all")
      .setPage(5)
      .setPageSize(50)
      .build()
    );
    GetRequest getRequest = serviceTester.getGetRequest();

    assertThat(serviceTester.getGetParser()).isSameAs(QualityProfiles.SearchUsersResponse.parser());
    serviceTester.assertThat(getRequest)
      .hasPath("search_users")
      .hasParam(PARAM_ORGANIZATION, "O1")
      .hasParam(PARAM_QUALITY_PROFILE, "P1")
      .hasParam(PARAM_LANGUAGE, "Xoo")
      .hasParam(TEXT_QUERY, "john")
      .hasParam(SELECTED, "all")
      .hasParam(PAGE, 5)
      .hasParam(PAGE_SIZE, 50)
      .andNoOtherParam();
  }

  @Test
  public void add_group() {
    underTest.addGroup(AddGroupRequest.builder()
      .setOrganization("O1")
      .setQualityProfile("P1")
      .setLanguage("Xoo")
      .setGroup("users")
      .build());
    PostRequest request = serviceTester.getPostRequest();

    serviceTester.assertThat(request)
      .hasPath("add_group")
      .hasParam(PARAM_ORGANIZATION, "O1")
      .hasParam(PARAM_QUALITY_PROFILE, "P1")
      .hasParam(PARAM_LANGUAGE, "Xoo")
      .hasParam(PARAM_GROUP, "users")
      .andNoOtherParam();
  }

  @Test
  public void remove_group() {
    underTest.removeGroup(RemoveGroupRequest.builder()
      .setOrganization("O1")
      .setQualityProfile("P1")
      .setLanguage("Xoo")
      .setGroup("users")
      .build());
    PostRequest request = serviceTester.getPostRequest();

    serviceTester.assertThat(request)
      .hasPath("remove_group")
      .hasParam(PARAM_ORGANIZATION, "O1")
      .hasParam(PARAM_QUALITY_PROFILE, "P1")
      .hasParam(PARAM_LANGUAGE, "Xoo")
      .hasParam(PARAM_GROUP, "users")
      .andNoOtherParam();
  }

  @Test
  public void search_groups() {
    underTest.searchGroups(SearchGroupsRequest.builder()
      .setOrganization("O1")
      .setQualityProfile("P1")
      .setLanguage("Xoo")
      .setQuery("users")
      .setSelected("all")
      .setPage(5)
      .setPageSize(50)
      .build()
    );
    GetRequest getRequest = serviceTester.getGetRequest();

    assertThat(serviceTester.getGetParser()).isSameAs(QualityProfiles.SearchGroupsResponse.parser());
    serviceTester.assertThat(getRequest)
      .hasPath("search_groups")
      .hasParam(PARAM_ORGANIZATION, "O1")
      .hasParam(PARAM_QUALITY_PROFILE, "P1")
      .hasParam(PARAM_LANGUAGE, "Xoo")
      .hasParam(TEXT_QUERY, "users")
      .hasParam(SELECTED, "all")
      .hasParam(PAGE, 5)
      .hasParam(PAGE_SIZE, 50)
      .andNoOtherParam();
  }
}
