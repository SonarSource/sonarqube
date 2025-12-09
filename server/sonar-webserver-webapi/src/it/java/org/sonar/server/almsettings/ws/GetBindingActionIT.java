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
package org.sonar.server.almsettings.ws;

import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.alm.client.azure.AzureDevOpsHttpClient;
import org.sonar.alm.client.azure.GsonAzureRepo;
import org.sonar.alm.client.gitlab.GitlabApplicationClient;
import org.sonar.alm.client.gitlab.Project;
import org.sonar.api.config.internal.Encryption;
import org.sonar.api.config.internal.Settings;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.AlmSettings;
import org.sonarqube.ws.AlmSettings.GetBindingWsResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.permission.ProjectPermission.USER;
import static org.sonar.test.JsonAssert.assertJson;

class GetBindingActionIT {

  @RegisterExtension
  private final UserSessionRule userSession = UserSessionRule.standalone();
  @RegisterExtension
  private final DbTester db = DbTester.create(System2.INSTANCE);

  private final GitlabApplicationClient gitlabApplicationClient = mock(GitlabApplicationClient.class);
  private final AzureDevOpsHttpClient azureDevOpsHttpClient = mock(AzureDevOpsHttpClient.class);
  private final Encryption encryption = mock(Encryption.class);
  private final Settings settings = createMockSettings();
  private final WsActionTester ws = new WsActionTester(
    new GetBindingAction(db.getDbClient(), userSession, new ComponentFinder(db.getDbClient(), null), gitlabApplicationClient, azureDevOpsHttpClient, settings));

  private UserDto user;
  private ProjectDto project;

  private Settings createMockSettings() {
    Settings mockSettings = mock(Settings.class);
    when(mockSettings.getEncryption()).thenReturn(encryption);
    return mockSettings;
  }

  @BeforeEach
  void before() {
    user = db.users().insertUser();
    project = db.components().insertPrivateProject().getProjectDto();
  }

  private static Stream<Arguments> githubBindingParameters() {
    return Stream.of(
      Arguments.of("GitHub.com", "https://api.github.com", "https://github.com/my-repo"),
      Arguments.of("GitHub Enterprise Server", "https://github.enterprise.com/api/v3", "https://github.enterprise.com/my-repo"),
      Arguments.of("GitHub Enterprise Server with trailing slash", "https://api.company.ghe.com/", "https://company.ghe.com/my-repo"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("githubBindingParameters")
  void get_github_project_binding(String testName, String githubApiUrl, String expectedRepositoryUrl) {
    userSession.logIn(user).addProjectPermission(USER, project);
    AlmSettingDto githubAlmSetting = db.almSettings().insertGitHubAlmSetting(setting -> setting.setUrl(githubApiUrl));
    ProjectAlmSettingDto githubProjectAlmSetting = db.almSettings().insertGitHubProjectAlmSetting(
      githubAlmSetting, project, projectAlmSetting -> projectAlmSetting.setAlmRepo("my-repo"));

    GetBindingWsResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .executeProtobuf(GetBindingWsResponse.class);

    assertThat(response)
      .extracting(
        GetBindingWsResponse::getAlm,
        GetBindingWsResponse::getKey,
        GetBindingWsResponse::getRepository,
        GetBindingWsResponse::getUrl,
        GetBindingWsResponse::getSummaryCommentEnabled,
        GetBindingWsResponse::getRepositoryUrl)
      .containsExactly(
        AlmSettings.Alm.github,
        githubAlmSetting.getKey(),
        githubProjectAlmSetting.getAlmRepo(),
        githubAlmSetting.getUrl(),
        true,
        expectedRepositoryUrl);
  }

  @Test
  void get_azure_project_binding() {
    userSession.logIn(user).addProjectPermission(USER, project);
    AlmSettingDto almSetting = db.almSettings().insertAzureAlmSetting(
      setting -> setting.setUrl("https://dev.azure.com/org").setPersonalAccessToken("token"));
    ProjectAlmSettingDto projectAlmSettingDto = db.almSettings().insertAzureMonoRepoProjectAlmSetting(almSetting, project);

    String expectedRepositoryUrl = "https://dev.azure.com/org/" + projectAlmSettingDto.getAlmSlug() + "/_git/" + projectAlmSettingDto.getAlmRepo();
    when(encryption.isEncrypted(anyString())).thenReturn(false);
    when(azureDevOpsHttpClient.getRepo("https://dev.azure.com/org", "token", projectAlmSettingDto.getAlmSlug(), projectAlmSettingDto.getAlmRepo()))
      .thenReturn(new GsonAzureRepo("repo-id", projectAlmSettingDto.getAlmRepo(), "api-url", expectedRepositoryUrl, null, "refs/heads/main"));

    GetBindingWsResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .executeProtobuf(GetBindingWsResponse.class);

    assertThat(response)
      .extracting(
        GetBindingWsResponse::getAlm,
        GetBindingWsResponse::getKey,
        GetBindingWsResponse::getUrl,
        GetBindingWsResponse::getRepository,
        GetBindingWsResponse::getSlug,
        GetBindingWsResponse::hasSummaryCommentEnabled,
        GetBindingWsResponse::getMonorepo,
        GetBindingWsResponse::getRepositoryUrl)
      .containsExactly(
        AlmSettings.Alm.azure,
        almSetting.getKey(),
        almSetting.getUrl(),
        projectAlmSettingDto.getAlmRepo(),
        projectAlmSettingDto.getAlmSlug(),
        false,
        true,
        expectedRepositoryUrl);
  }

  private static Stream<Arguments> gitlabBindingParameters() {
    return Stream.of(
      Arguments.of("GitLab.com", "https://gitlab.com", "https://gitlab.com/my-group/my-project"),
      Arguments.of("Self-hosted GitLab", "https://gitlab.company.com", "https://gitlab.company.com/company-group/company-project"),
      Arguments.of("Self-hosted GitLab with trailing slash", "https://gitlab.company.com/", "https://gitlab.company.com/another-group/another-project"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("gitlabBindingParameters")
  void get_gitlab_project_binding(String testName, String gitlabUrl, String expectedRepositoryUrl) {
    var projectId = "12345";
    UserDto user = db.users().insertUser();
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    userSession.logIn(user).addProjectPermission(USER, project);

    AlmSettingDto almSetting = db.almSettings().insertGitlabAlmSetting(setting -> setting.setUrl(gitlabUrl).setPersonalAccessToken("token"));
    ProjectAlmSettingDto projectAlmSettingDto = db.almSettings().insertGitlabProjectAlmSetting(
      almSetting, project, projectAlmSetting -> projectAlmSetting.setAlmRepo(projectId));

    when(encryption.isEncrypted(anyString())).thenReturn(false);
    when(gitlabApplicationClient.getProject(gitlabUrl, "token", Long.parseLong(projectId)))
      .thenReturn(new Project(
        Long.parseLong(projectId),
        "project-name",
        "namespace/project-name",
        "project-name",
        "namespace/project-name",
        expectedRepositoryUrl));

    GetBindingWsResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .executeProtobuf(GetBindingWsResponse.class);

    assertThat(response)
      .extracting(
        GetBindingWsResponse::getAlm,
        GetBindingWsResponse::getKey,
        GetBindingWsResponse::getRepository,
        GetBindingWsResponse::getUrl,
        GetBindingWsResponse::hasUrl,
        GetBindingWsResponse::hasSummaryCommentEnabled,
        GetBindingWsResponse::getRepositoryUrl)
      .containsExactly(
        AlmSettings.Alm.gitlab,
        almSetting.getKey(),
        projectAlmSettingDto.getAlmRepo(),
        almSetting.getUrl(),
        true,
        false,
        expectedRepositoryUrl);
  }

  @Test
  void get_bitbucket_project_binding() {
    userSession.logIn(user).addProjectPermission(USER, project);
    AlmSettingDto almSetting = db.almSettings().insertBitbucketAlmSetting();
    ProjectAlmSettingDto projectAlmSettingDto = db.almSettings().insertBitbucketProjectAlmSetting(almSetting, project);

    GetBindingWsResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .executeProtobuf(GetBindingWsResponse.class);

    assertThat(response)
      .extracting(
        GetBindingWsResponse::getAlm,
        GetBindingWsResponse::getKey,
        GetBindingWsResponse::getRepository,
        GetBindingWsResponse::getUrl,
        GetBindingWsResponse::getSlug,
        GetBindingWsResponse::hasSummaryCommentEnabled)
      .containsExactly(
        AlmSettings.Alm.bitbucket,
        almSetting.getKey(),
        projectAlmSettingDto.getAlmRepo(),
        almSetting.getUrl(),
        projectAlmSettingDto.getAlmSlug(),
        false);
  }

  @Test
  void get_github_project_binding_returns_without_repository_url_when_url_construction_fails() {
    userSession.logIn(user).addProjectPermission(USER, project);
    AlmSettingDto githubAlmSetting = db.almSettings().insertGitHubAlmSetting(setting -> setting.setUrl("invalid-url"));
    ProjectAlmSettingDto githubProjectAlmSetting = db.almSettings().insertGitHubProjectAlmSetting(
      githubAlmSetting, project, projectAlmSetting -> projectAlmSetting.setAlmRepo("my-repo"));

    GetBindingWsResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .executeProtobuf(GetBindingWsResponse.class);

    assertThat(response)
      .extracting(
        GetBindingWsResponse::getAlm,
        GetBindingWsResponse::getKey,
        GetBindingWsResponse::getRepository,
        GetBindingWsResponse::hasRepositoryUrl)
      .containsExactly(
        AlmSettings.Alm.github,
        githubAlmSetting.getKey(),
        githubProjectAlmSetting.getAlmRepo(),
        false);
  }

  @Test
  void get_gitlab_project_binding_returns_without_repository_url_when_project_id_is_invalid() {
    userSession.logIn(user).addProjectPermission(USER, project);
    AlmSettingDto almSetting = db.almSettings().insertGitlabAlmSetting(setting -> setting.setUrl("https://gitlab.com").setPersonalAccessToken("token"));
    db.almSettings().insertGitlabProjectAlmSetting(
      almSetting, project, projectAlmSetting -> projectAlmSetting.setAlmRepo("invalid-project-id"));

    when(encryption.isEncrypted(anyString())).thenReturn(false);

    GetBindingWsResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .executeProtobuf(GetBindingWsResponse.class);

    assertThat(response)
      .extracting(
        GetBindingWsResponse::getAlm,
        GetBindingWsResponse::getKey,
        GetBindingWsResponse::hasRepositoryUrl)
      .containsExactly(
        AlmSettings.Alm.gitlab,
        almSetting.getKey(),
        false);
  }

  @Test
  void get_gitlab_project_binding_returns_without_repository_url_when_api_call_fails() {
    var projectId = "12345";
    userSession.logIn(user).addProjectPermission(USER, project);
    AlmSettingDto almSetting = db.almSettings().insertGitlabAlmSetting(setting -> setting.setUrl("https://gitlab.com").setPersonalAccessToken("token"));
    db.almSettings().insertGitlabProjectAlmSetting(
      almSetting, project, projectAlmSetting -> projectAlmSetting.setAlmRepo(projectId));

    when(encryption.isEncrypted(anyString())).thenReturn(false);
    when(gitlabApplicationClient.getProject("https://gitlab.com", "token", Long.parseLong(projectId)))
      .thenThrow(new RuntimeException("API connection error"));

    GetBindingWsResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .executeProtobuf(GetBindingWsResponse.class);

    assertThat(response)
      .extracting(
        GetBindingWsResponse::getAlm,
        GetBindingWsResponse::getKey,
        GetBindingWsResponse::hasRepositoryUrl)
      .containsExactly(
        AlmSettings.Alm.gitlab,
        almSetting.getKey(),
        false);
  }

  @Test
  void get_azure_project_binding_returns_without_repository_url_when_api_call_fails() {
    userSession.logIn(user).addProjectPermission(USER, project);
    AlmSettingDto almSetting = db.almSettings().insertAzureAlmSetting(
      setting -> setting.setUrl("https://dev.azure.com/org").setPersonalAccessToken("token"));
    ProjectAlmSettingDto projectAlmSettingDto = db.almSettings().insertAzureMonoRepoProjectAlmSetting(almSetting, project);

    when(encryption.isEncrypted(anyString())).thenReturn(false);
    when(azureDevOpsHttpClient.getRepo("https://dev.azure.com/org", "token", projectAlmSettingDto.getAlmSlug(), projectAlmSettingDto.getAlmRepo()))
      .thenThrow(new RuntimeException("API connection error"));

    GetBindingWsResponse response = ws.newRequest()
      .setParam("project", project.getKey())
      .executeProtobuf(GetBindingWsResponse.class);

    assertThat(response)
      .extracting(
        GetBindingWsResponse::getAlm,
        GetBindingWsResponse::getKey,
        GetBindingWsResponse::hasRepositoryUrl)
      .containsExactly(
        AlmSettings.Alm.azure,
        almSetting.getKey(),
        false);
  }

  @Test
  void fail_when_project_does_not_exist() {
    userSession.logIn(user).addProjectPermission(USER, project);
    AlmSettingDto githubAlmSetting = db.almSettings().insertGitHubAlmSetting();
    db.almSettings().insertGitHubProjectAlmSetting(githubAlmSetting, project);

    assertThatThrownBy(() -> ws.newRequest()
      .setParam("project", "unknown")
      .execute())
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void fail_when_missing_browse_permission_on_project() {
    userSession.logIn(user);
    AlmSettingDto githubAlmSetting = db.almSettings().insertGitHubAlmSetting();
    db.almSettings().insertGitHubProjectAlmSetting(githubAlmSetting, project);

    assertThatThrownBy(() -> ws.newRequest()
      .setParam("project", project.getKey())
      .execute())
        .isInstanceOf(ForbiddenException.class);
  }

  @Test
  void json_example() {
    userSession.logIn(user).addProjectPermission(USER, project);
    AlmSettingDto githubAlmSetting = db.almSettings().insertGitHubAlmSetting(
      almSettingDto -> almSettingDto
        .setKey("GitHub Server - Dev Team")
        .setUrl("https://github.enterprise.com")
        .setAppId("12345")
        .setPrivateKey("54684654"));
    db.almSettings().insertGitHubProjectAlmSetting(githubAlmSetting, project, projectAlmSetting -> projectAlmSetting.setAlmRepo("team/project"));

    String response = ws.newRequest()
      .setParam("project", project.getKey())
      .execute().getInput();

    assertJson(response).isSimilarTo(getClass().getResource("get_binding-example.json"));
  }

  @Test
  void definition() {
    WebService.Action def = ws.getDef();

    assertThat(def)
      .extracting(
        WebService.Action::since,
        WebService.Action::isPost,
        action -> action.responseExampleAsString().isEmpty())
      .containsExactly("8.1", false, false);
    assertThat(def.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactly(tuple("project", true));
  }

}
