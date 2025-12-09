/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.almintegration.ws.github;

import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.auth.github.GithubApplicationClient;
import org.sonar.alm.client.github.GithubApplicationClientImpl;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.alm.pat.AlmPatDto;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDao;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.server.tester.UserSessionRule.standalone;
import static org.sonarqube.ws.AlmIntegrations.GithubRepository;
import static org.sonarqube.ws.AlmIntegrations.ListGithubRepositoriesWsResponse;

public class ListGithubRepositoriesActionIT {

  @Rule
  public UserSessionRule userSession = standalone();

  private final System2 system2 = mock(System2.class);
  private final GithubApplicationClientImpl appClient = mock(GithubApplicationClientImpl.class);

  @Rule
  public DbTester db = DbTester.create(system2);

  private final ProjectAlmSettingDao projectAlmSettingDao = db.getDbClient().projectAlmSettingDao();

  private final WsActionTester ws = new WsActionTester(new ListGithubRepositoriesAction(db.getDbClient(), userSession, appClient, projectAlmSettingDao));

  @Test
  public void fail_when_missing_create_project_permission() {
    TestRequest request = ws.newRequest();
    assertThatThrownBy(request::execute)
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void fail_when_almSetting_does_not_exist() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(GlobalPermission.PROVISION_PROJECTS);

    TestRequest request = ws.newRequest()
        .setParam(ListGithubRepositoriesAction.PARAM_ALM_SETTING, "unknown")
        .setParam(ListGithubRepositoriesAction.PARAM_ORGANIZATION, "test");
    assertThatThrownBy(request::execute)
        .isInstanceOf(NotFoundException.class)
        .hasMessage("GitHub Setting 'unknown' not found");
  }

  @Test
  public void fail_when_personal_access_token_doesnt_exist() {
    AlmSettingDto githubAlmSetting = setupAlm();

    TestRequest request = ws.newRequest()
        .setParam(ListGithubRepositoriesAction.PARAM_ALM_SETTING, githubAlmSetting.getKey())
        .setParam(ListGithubRepositoriesAction.PARAM_ORGANIZATION, "test");
    assertThatThrownBy(request::execute)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("No personal access token found");
  }

  @Test
  public void return_repositories_using_existing_personal_access_token() {
    AlmSettingDto githubAlmSettings = setupAlm();
    AlmPatDto pat = db.almPats().insert(p -> p.setAlmSettingUuid(githubAlmSettings.getUuid()).setUserUuid(userSession.getUuid()));

    when(appClient.listRepositories(eq(githubAlmSettings.getUrl()), argThat(token -> token.getValue().equals(pat.getPersonalAccessToken())), eq("github"), isNull(), eq(1),
      eq(100)))
        .thenReturn(new GithubApplicationClient.Repositories()
          .setTotal(2)
          .setRepositories(Stream.of("HelloWorld", "HelloUniverse")
            .map(name -> new GithubApplicationClient.Repository(name.length(), name, false, "github/" + name,
              "https://github-enterprise.sonarqube.com/api/v3/github/HelloWorld", "main"))
            .toList()));

    ProjectDto project = db.components().insertPrivateProject(componentDto -> componentDto.setKey("github_HelloWorld")).getProjectDto();
    db.almSettings().insertGitHubProjectAlmSetting(githubAlmSettings, project, projectAlmSettingDto -> projectAlmSettingDto.setAlmRepo("github/HelloWorld"));

    ProjectDto project2 = db.components().insertPrivateProject(componentDto -> componentDto.setKey("github_HelloWorld2")).getProjectDto();
    db.almSettings().insertGitHubProjectAlmSetting(githubAlmSettings, project2, projectAlmSettingDto -> projectAlmSettingDto.setAlmRepo("github/HelloWorld"));

    ListGithubRepositoriesWsResponse response = ws.newRequest()
      .setParam(ListGithubRepositoriesAction.PARAM_ALM_SETTING, githubAlmSettings.getKey())
      .setParam(ListGithubRepositoriesAction.PARAM_ORGANIZATION, "github")
      .executeProtobuf(ListGithubRepositoriesWsResponse.class);

    assertThat(response.getPaging())
      .extracting(Common.Paging::getPageIndex, Common.Paging::getPageSize, Common.Paging::getTotal)
      .containsOnly(1, 100, 2);
    assertThat(response.getRepositoriesCount()).isEqualTo(2);
    assertThat(response.getRepositoriesList())
      .extracting(GithubRepository::getKey, GithubRepository::getName, GithubRepository::getSqProjectKey)
      .containsOnly(tuple("github/HelloWorld", "HelloWorld", project.getKey()), tuple("github/HelloUniverse", "HelloUniverse", ""));

    verify(appClient).listRepositories(eq(githubAlmSettings.getUrl()), argThat(token -> token.getValue().equals(pat.getPersonalAccessToken())), eq("github"), isNull(), eq(1),
      eq(100));
  }

  private AlmSettingDto setupAlm() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(GlobalPermission.PROVISION_PROJECTS);

    return db.almSettings().insertGitHubAlmSetting(alm -> alm.setClientId("client_123").setClientSecret("client_secret_123"));
  }
}
