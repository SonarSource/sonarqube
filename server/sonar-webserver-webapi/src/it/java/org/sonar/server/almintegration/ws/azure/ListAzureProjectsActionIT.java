/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.almintegration.ws.azure;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.alm.client.azure.AzureDevOpsHttpClient;
import org.sonar.alm.client.azure.GsonAzureProject;
import org.sonar.alm.client.azure.GsonAzureProjectList;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbTester;
import org.sonar.db.alm.pat.AlmPatDto;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.AlmIntegrations.AzureProject;
import org.sonarqube.ws.AlmIntegrations.ListAzureProjectsWsResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.alm.integration.pat.AlmPatsTesting.newAlmPatDto;
import static org.sonar.db.permission.GlobalPermission.PROVISION_PROJECTS;

public class ListAzureProjectsActionIT {
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();

  private final AzureDevOpsHttpClient azureDevOpsHttpClient = mock(AzureDevOpsHttpClient.class);
  private final WsActionTester ws = new WsActionTester(new ListAzureProjectsAction(db.getDbClient(), userSession, azureDevOpsHttpClient));

  @Before
  public void before() {
    mockClient(ImmutableList.of(new GsonAzureProject("name", "description"),
      new GsonAzureProject("name", null)));
  }

  private void mockClient(List<GsonAzureProject> projects) {
    GsonAzureProjectList projectList = new GsonAzureProjectList();
    projectList.setValues(projects);
    when(azureDevOpsHttpClient.getProjects(anyString(), anyString())).thenReturn(projectList);
  }

  @Test
  public void list_projects() {
    AlmSettingDto almSetting = insertAlmSetting();

    ListAzureProjectsWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .executeProtobuf(ListAzureProjectsWsResponse.class);

    assertThat(response.getProjectsCount()).isEqualTo(2);
    assertThat(response.getProjectsList())
      .extracting(AzureProject::getName, AzureProject::getDescription)
      .containsExactly(tuple("name", "description"), tuple("name", ""));
  }

  @Test
  public void list_projects_alphabetically_sorted() {
    mockClient(ImmutableList.of(new GsonAzureProject("BBB project", "BBB project description"),
      new GsonAzureProject("AAA project 1", "AAA project description"),
      new GsonAzureProject("zzz project", "zzz project description"),
      new GsonAzureProject("aaa project", "aaa project description")));
    AlmSettingDto almSetting = insertAlmSetting();

    ListAzureProjectsWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .executeProtobuf(ListAzureProjectsWsResponse.class);

    assertThat(response.getProjectsCount()).isEqualTo(4);
    assertThat(response.getProjectsList())
      .extracting(AzureProject::getName, AzureProject::getDescription)
      .containsExactly(tuple("aaa project", "aaa project description"), tuple("AAA project 1", "AAA project description"),
        tuple("BBB project", "BBB project description"), tuple("zzz project", "zzz project description"));
  }

  @Test
  public void check_pat_is_missing() {
    insertUser();
    AlmSettingDto almSetting = db.almSettings().insertAzureAlmSetting();

    TestRequest request = ws.newRequest()
      .setParam("almSetting", almSetting.getKey());

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("No personal access token found");
  }

  @Test
  public void fail_check_alm_setting_not_found() {
    UserDto user = insertUser();
    AlmPatDto almPatDto = newAlmPatDto();
    db.getDbClient().almPatDao().insert(db.getSession(), almPatDto, user.getLogin(), null);

    TestRequest request = ws.newRequest()
      .setParam("almSetting", "testKey");

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("DevOps Platform Setting 'testKey' not found");
  }

  @Test
  public void fail_when_not_logged_in() {
    TestRequest request = ws.newRequest()
      .setParam("almSetting", "anyvalue");

    assertThatThrownBy(request::execute)
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void fail_when_no_creation_project_permission() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);

    TestRequest request = ws.newRequest()
      .setParam("almSetting", "anyvalue");

    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void definition() {
    WebService.Action def = ws.getDef();

    assertThat(def.since()).isEqualTo("8.6");
    assertThat(def.isPost()).isFalse();
    assertThat(def.responseExampleFormat()).isEqualTo("json");
    assertThat(def.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(tuple("almSetting", true));
  }

  private UserDto insertUser() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);
    return user;
  }

  private AlmSettingDto insertAlmSetting() {
    UserDto user = insertUser();
    AlmSettingDto almSetting = db.almSettings().insertAzureAlmSetting();
    db.almPats().insert(dto -> {
      dto.setAlmSettingUuid(almSetting.getUuid());
      dto.setUserUuid(user.getUuid());
    });
    return almSetting;
  }
}
