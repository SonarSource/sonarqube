/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.alm.client.azure.AzureDevOpsHttpClient;
import org.sonar.alm.client.azure.GsonAzureRepo;
import org.sonar.alm.client.bitbucket.bitbucketcloud.BitbucketCloudRestClient;
import org.sonar.alm.client.bitbucketserver.BitbucketServerRestClient;
import org.sonar.alm.client.github.GithubGlobalSettingsValidator;
import org.sonar.alm.client.gitlab.GitlabApplicationClient;
import org.sonar.alm.client.gitlab.Project;
import org.sonar.api.config.internal.Encryption;
import org.sonar.api.config.internal.Settings;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.auth.github.ExpiringAppInstallationToken;
import org.sonar.auth.github.GithubAppConfiguration;
import org.sonar.auth.github.GithubApplicationClient;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDao;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonarsource.dop.translation.api.ProjectBindingsQuery;
import org.sonarsource.dop.translation.api.model.ProjectBinding;
import org.sonarsource.dop.translation.api.model.ProjectBindings;
import org.sonarsource.organizations.server.DefaultOrganizationProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProjectBindingsServiceServerImplIT {

  @RegisterExtension
  private final LogTesterJUnit5 logTester = new LogTesterJUnit5();

  private final AuditPersister auditPersister = mock(AuditPersister.class);
  @RegisterExtension
  private final DbTester db = DbTester.create(new TestSystem2().setNow(System.currentTimeMillis()), auditPersister);

  private final GithubApplicationClient githubApplicationClient = mock(GithubApplicationClient.class);
  private final GithubGlobalSettingsValidator githubGlobalSettingsValidator = mock(GithubGlobalSettingsValidator.class);
  private final GitlabApplicationClient gitlabApplicationClient = mock(GitlabApplicationClient.class);
  private final AzureDevOpsHttpClient azureDevOpsHttpClient = mock(AzureDevOpsHttpClient.class);
  private final BitbucketServerRestClient bitbucketServerRestClient = mock(BitbucketServerRestClient.class);
  private final BitbucketCloudRestClient bitbucketCloudRestClient = mock(BitbucketCloudRestClient.class);
  private final Encryption encryption = mock(Encryption.class);
  private final Settings settings = createMockSettings();

  private final ProjectBindingsServiceServerImpl underTest = new ProjectBindingsServiceServerImpl(
    db.getDbClient(), githubApplicationClient, githubGlobalSettingsValidator, gitlabApplicationClient,
    azureDevOpsHttpClient, bitbucketServerRestClient, bitbucketCloudRestClient, settings);

  private ProjectDto project;

  private Settings createMockSettings() {
    Settings mockSettings = mock(Settings.class);
    when(mockSettings.getEncryption()).thenReturn(encryption);
    return mockSettings;
  }

  @BeforeEach
  void before() {
    project = db.components().insertPrivateProject().getProjectDto();
  }

  @Test
  void getProjectBinding_whenIdIsNull_throwsNotFoundExceptionRatherThanNpe() {
    assertThatThrownBy(() -> underTest.getProjectBinding(null))
      .isInstanceOf(NotFoundException.class);
  }

  @Test
  void getProjectBinding_whenIdUnknown_throwsNotFoundException() {
    assertThatThrownBy(() -> underTest.getProjectBinding("unknown"))
      .isInstanceOf(NotFoundException.class);
  }

  @Test
  void getProjectBinding_whenUrlAndRepoIdAlreadyStored_returnsThemWithoutCallingAlmClient() {
    AlmSettingDto githubAlmSetting = db.almSettings().insertGitHubAlmSetting();
    ProjectAlmSettingDto projectAlmSetting = db.almSettings().insertGitHubProjectAlmSetting(githubAlmSetting, project,
      dto -> dto.setUrl("https://github.com/sonarsource/sonar-enterprise").setRepoId("123456"));
    clearInvocations(auditPersister);

    ProjectBinding binding = underTest.getProjectBinding(projectAlmSetting.getUuid());

    assertThat(binding.getId()).isEqualTo(projectAlmSetting.getUuid());
    assertThat(binding.getProjectId()).isEqualTo(project.getUuid());
    assertThat(binding.getDevOpsPlatform()).isEqualTo("github");
    assertThat(binding.getUrl()).isEqualTo("https://github.com/sonarsource/sonar-enterprise");
    assertThat(binding.getRepositoryId()).isEqualTo("123456");
    assertThat(binding.getSlug()).isEqualTo(projectAlmSetting.getAlmRepo());
    verifyNoInteractions(githubApplicationClient);
    verifyNoInteractions(auditPersister);
  }

  @Test
  void getProjectBinding_whenBitbucketServer_mapsToBitbucketDevOpsPlatformWithAlmSlugAsSlug() {
    AlmSettingDto bitbucketAlmSetting = db.almSettings().insertBitbucketAlmSetting();
    ProjectAlmSettingDto projectAlmSetting = db.almSettings().insertBitbucketProjectAlmSetting(bitbucketAlmSetting, project,
      dto -> dto.setUrl("https://bitbucket.example.com/rest/api/1.0/projects/KEY/repos/repo").setRepoId("42"));

    ProjectBinding binding = underTest.getProjectBinding(projectAlmSetting.getUuid());

    assertThat(binding.getDevOpsPlatform()).isEqualTo("bitbucket");
    assertThat(binding.getRepositoryId()).isEqualTo("42");
    assertThat(binding.getSlug()).isEqualTo(projectAlmSetting.getAlmSlug());
  }

  @Test
  void getProjectBinding_whenAzureDevOps_mapsToEmptySlug() {
    AlmSettingDto azureAlmSetting = db.almSettings().insertAzureAlmSetting();
    ProjectAlmSettingDto projectAlmSetting = db.almSettings().insertAzureProjectAlmSetting(azureAlmSetting, project,
      dto -> dto.setUrl("https://dev.azure.com/org/project/_git/repo").setRepoId("guid-1234"));

    ProjectBinding binding = underTest.getProjectBinding(projectAlmSetting.getUuid());

    assertThat(binding.getDevOpsPlatform()).isEqualTo("azure_devops");
    assertThat(binding.getSlug()).isEmpty();
  }

  @Test
  void getProjectBinding_whenUrlAndRepoIdMissing_resolvesLiveAndPersistsWithoutAuditing() {
    AlmSettingDto gitlabAlmSetting = db.almSettings().insertGitlabAlmSetting();
    ProjectAlmSettingDto projectAlmSetting = db.almSettings().insertGitlabProjectAlmSetting(gitlabAlmSetting, project,
      dto -> dto.setAlmRepo("123").setAlmSlug("group/repo"));
    Project gitlabProject = mock(Project.class);
    when(gitlabProject.getWebUrl()).thenReturn("https://gitlab.example.com/group/repo");
    when(gitlabProject.getId()).thenReturn(123L);
    when(gitlabApplicationClient.getProject(anyString(), anyString(), eq(123L))).thenReturn(gitlabProject);
    clearInvocations(auditPersister);

    ProjectBinding binding = underTest.getProjectBinding(projectAlmSetting.getUuid());

    assertThat(binding.getUrl()).isEqualTo("https://gitlab.example.com/group/repo");
    assertThat(binding.getRepositoryId()).isEqualTo("123");
    assertThat(binding.getSlug()).isEqualTo("group/repo");

    ProjectAlmSettingDto reloaded = db.getDbClient().projectAlmSettingDao().selectByUuid(db.getSession(), projectAlmSetting.getUuid()).orElseThrow();
    assertThat(reloaded.getUrl()).isEqualTo("https://gitlab.example.com/group/repo");
    assertThat(reloaded.getRepoId()).isEqualTo("123");
    verifyNoInteractions(auditPersister);
  }

  @Test
  void getProjectBinding_whenGithubUrlAndRepoIdMissing_resolvesLiveUsingScopedInstallationTokenAndPersists() {
    AlmSettingDto githubAlmSetting = db.almSettings().insertGitHubAlmSetting();
    ProjectAlmSettingDto projectAlmSetting = db.almSettings().insertGitHubProjectAlmSetting(githubAlmSetting, project,
      dto -> dto.setAlmRepo("sonarsource/sonar-enterprise"));
    GithubAppConfiguration githubAppConfiguration = new GithubAppConfiguration(1L, "private-key", githubAlmSetting.getUrl());
    when(githubGlobalSettingsValidator.validate(any(AlmSettingDto.class))).thenReturn(githubAppConfiguration);
    when(githubApplicationClient.getInstallationId(githubAppConfiguration, "sonarsource/sonar-enterprise")).thenReturn(Optional.of(99L));
    ExpiringAppInstallationToken token = mock(ExpiringAppInstallationToken.class);
    // bareRepositoryName: the installation token must be scoped to the bare repo name, not "owner/repo"
    when(githubApplicationClient.createAppInstallationToken(githubAppConfiguration, 99L, "sonar-enterprise")).thenReturn(Optional.of(token));
    GithubApplicationClient.Repository repository = new GithubApplicationClient.Repository(555L, "sonar-enterprise", false,
      "sonarsource/sonar-enterprise", "https://github.com/sonarsource/sonar-enterprise", "master");
    when(githubApplicationClient.getRepository(anyString(), eq(token), eq("sonarsource/sonar-enterprise"))).thenReturn(Optional.of(repository));
    clearInvocations(auditPersister);

    ProjectBinding binding = underTest.getProjectBinding(projectAlmSetting.getUuid());

    assertThat(binding.getUrl()).isEqualTo("https://github.com/sonarsource/sonar-enterprise");
    assertThat(binding.getRepositoryId()).isEqualTo("555");

    ProjectAlmSettingDto reloaded = db.getDbClient().projectAlmSettingDao().selectByUuid(db.getSession(), projectAlmSetting.getUuid()).orElseThrow();
    assertThat(reloaded.getUrl()).isEqualTo("https://github.com/sonarsource/sonar-enterprise");
    assertThat(reloaded.getRepoId()).isEqualTo("555");
    verifyNoInteractions(auditPersister);
  }

  @Test
  void getProjectBinding_whenAzureUrlAndRepoIdMissing_resolvesLiveAndPersists() {
    AlmSettingDto azureAlmSetting = db.almSettings().insertAzureAlmSetting();
    ProjectAlmSettingDto projectAlmSetting = db.almSettings().insertAzureProjectAlmSetting(azureAlmSetting, project,
      dto -> dto.setAlmRepo("repo").setAlmSlug("project"));
    GsonAzureRepo azureRepo = new GsonAzureRepo("guid-1234", "repo", "https://dev.azure.com/org/project/_apis/git/repositories/repo",
      "https://dev.azure.com/org/project/_git/repo", null, "");
    when(azureDevOpsHttpClient.getRepo(anyString(), anyString(), eq("project"), eq("repo"))).thenReturn(azureRepo);
    clearInvocations(auditPersister);

    ProjectBinding binding = underTest.getProjectBinding(projectAlmSetting.getUuid());

    assertThat(binding.getUrl()).isEqualTo("https://dev.azure.com/org/project/_git/repo");
    assertThat(binding.getRepositoryId()).isEqualTo("guid-1234");

    ProjectAlmSettingDto reloaded = db.getDbClient().projectAlmSettingDao().selectByUuid(db.getSession(), projectAlmSetting.getUuid()).orElseThrow();
    assertThat(reloaded.getUrl()).isEqualTo("https://dev.azure.com/org/project/_git/repo");
    assertThat(reloaded.getRepoId()).isEqualTo("guid-1234");
    verifyNoInteractions(auditPersister);
  }

  @Test
  void getProjectBinding_whenBitbucketServerUrlAndRepoIdMissing_resolvesLiveAndPersists() {
    AlmSettingDto bitbucketAlmSetting = db.almSettings().insertBitbucketAlmSetting();
    ProjectAlmSettingDto projectAlmSetting = db.almSettings().insertBitbucketProjectAlmSetting(bitbucketAlmSetting, project,
      dto -> dto.setAlmRepo("PROJECT_KEY").setAlmSlug("repo-slug"));
    org.sonar.alm.client.bitbucketserver.Repository repository = new org.sonar.alm.client.bitbucketserver.Repository()
      .setSlug("repo-slug")
      .setName("Repo")
      .setId(42L)
      .setLinks(new org.sonar.alm.client.bitbucketserver.Repository.Links(
        java.util.List.of(new org.sonar.alm.client.bitbucketserver.Repository.Link("https://bitbucket.example.com/projects/PROJECT_KEY/repos/repo-slug"))));
    when(bitbucketServerRestClient.getRepo(anyString(), anyString(), eq("PROJECT_KEY"), eq("repo-slug"))).thenReturn(repository);
    clearInvocations(auditPersister);

    ProjectBinding binding = underTest.getProjectBinding(projectAlmSetting.getUuid());

    assertThat(binding.getUrl()).isEqualTo("https://bitbucket.example.com/projects/PROJECT_KEY/repos/repo-slug");
    assertThat(binding.getRepositoryId()).isEqualTo("42");

    ProjectAlmSettingDto reloaded = db.getDbClient().projectAlmSettingDao().selectByUuid(db.getSession(), projectAlmSetting.getUuid()).orElseThrow();
    assertThat(reloaded.getUrl()).isEqualTo("https://bitbucket.example.com/projects/PROJECT_KEY/repos/repo-slug");
    assertThat(reloaded.getRepoId()).isEqualTo("42");
    verifyNoInteractions(auditPersister);
  }

  @Test
  void getProjectBinding_whenBitbucketCloudUrlAndRepoIdMissing_resolvesLiveViaOAuthAndPersists() {
    AlmSettingDto bitbucketCloudAlmSetting = db.almSettings().insertBitbucketCloudAlmSetting();
    ProjectAlmSettingDto projectAlmSetting = db.almSettings().insertBitbucketCloudProjectAlmSetting(bitbucketCloudAlmSetting, project,
      dto -> dto.setAlmRepo("repo-slug"));
    when(bitbucketCloudRestClient.createAccessToken(anyString(), anyString())).thenReturn("oauth-token");
    org.sonar.alm.client.bitbucket.bitbucketcloud.Repository repository = new org.sonar.alm.client.bitbucket.bitbucketcloud.Repository(
      "uuid-5678", "repo-slug", "Repo", null, null)
      .setLinks(new org.sonar.alm.client.bitbucket.bitbucketcloud.Repository.Links(
        new org.sonar.alm.client.bitbucket.bitbucketcloud.Repository.Link("https://bitbucket.org/workspace/repo-slug")));
    when(bitbucketCloudRestClient.getRepoWithAccessToken("oauth-token", bitbucketCloudAlmSetting.getAppId(), "repo-slug")).thenReturn(repository);
    clearInvocations(auditPersister);

    ProjectBinding binding = underTest.getProjectBinding(projectAlmSetting.getUuid());

    assertThat(binding.getUrl()).isEqualTo("https://bitbucket.org/workspace/repo-slug");
    assertThat(binding.getRepositoryId()).isEqualTo("uuid-5678");
    assertThat(binding.getSlug()).isEqualTo("repo-slug");

    ProjectAlmSettingDto reloaded = db.getDbClient().projectAlmSettingDao().selectByUuid(db.getSession(), projectAlmSetting.getUuid()).orElseThrow();
    assertThat(reloaded.getUrl()).isEqualTo("https://bitbucket.org/workspace/repo-slug");
    assertThat(reloaded.getRepoId()).isEqualTo("uuid-5678");
    verifyNoInteractions(auditPersister);
  }

  @Test
  void getProjectBinding_whenBitbucketCloudUuidMissing_returnsEmptyUrlAndRepoIdWithoutThrowing() {
    AlmSettingDto bitbucketCloudAlmSetting = db.almSettings().insertBitbucketCloudAlmSetting();
    ProjectAlmSettingDto projectAlmSetting = db.almSettings().insertBitbucketCloudProjectAlmSetting(bitbucketCloudAlmSetting, project,
      dto -> dto.setAlmRepo("repo-slug"));
    when(bitbucketCloudRestClient.createAccessToken(anyString(), anyString())).thenReturn("oauth-token");
    org.sonar.alm.client.bitbucket.bitbucketcloud.Repository repository = new org.sonar.alm.client.bitbucket.bitbucketcloud.Repository(
      null, "repo-slug", "Repo", null, null)
      .setLinks(new org.sonar.alm.client.bitbucket.bitbucketcloud.Repository.Links(
        new org.sonar.alm.client.bitbucket.bitbucketcloud.Repository.Link("https://bitbucket.org/workspace/repo-slug")));
    when(bitbucketCloudRestClient.getRepoWithAccessToken("oauth-token", bitbucketCloudAlmSetting.getAppId(), "repo-slug")).thenReturn(repository);

    ProjectBinding binding = underTest.getProjectBinding(projectAlmSetting.getUuid());

    assertThat(binding.getUrl()).isEmpty();
    assertThat(binding.getRepositoryId()).isEmpty();

    ProjectAlmSettingDto reloaded = db.getDbClient().projectAlmSettingDao().selectByUuid(db.getSession(), projectAlmSetting.getUuid()).orElseThrow();
    assertThat(reloaded.getUrl()).isNull();
    assertThat(reloaded.getRepoId()).isNull();
  }

  @Test
  void searchProjectBindings_whenMultipleBitbucketCloudBindingsShareTheSameAlmSetting_mintsOnlyOneOAuthToken() {
    AlmSettingDto bitbucketCloudAlmSetting = db.almSettings().insertBitbucketCloudAlmSetting();
    db.almSettings().insertBitbucketCloudProjectAlmSetting(bitbucketCloudAlmSetting, project, dto -> dto.setAlmRepo("repo-one"));
    ProjectDto secondProject = db.components().insertPrivateProject().getProjectDto();
    db.almSettings().insertBitbucketCloudProjectAlmSetting(bitbucketCloudAlmSetting, secondProject, dto -> dto.setAlmRepo("repo-two"));
    when(bitbucketCloudRestClient.createAccessToken(anyString(), anyString())).thenReturn("oauth-token");
    when(bitbucketCloudRestClient.getRepoWithAccessToken(eq("oauth-token"), anyString(), anyString()))
      .thenAnswer(invocation -> new org.sonar.alm.client.bitbucket.bitbucketcloud.Repository(
        "uuid-" + invocation.getArgument(2), invocation.getArgument(2), "Repo", null, null)
        .setLinks(new org.sonar.alm.client.bitbucket.bitbucketcloud.Repository.Links(
          new org.sonar.alm.client.bitbucket.bitbucketcloud.Repository.Link("https://bitbucket.org/workspace/" + invocation.getArgument(2)))));

    ProjectBindings result = underTest.searchProjectBindings(queryByOrganizationId(DefaultOrganizationProvider.ID.toString()));

    assertThat(result.getBindings()).hasSize(2);
    // The OAuth token is workspace-wide: minting it once and reusing it for every binding of this ALM setting
    // avoids hammering the token endpoint once per binding in a single search.
    verify(bitbucketCloudRestClient, times(1)).createAccessToken(anyString(), anyString());
  }

  @Test
  void getProjectBinding_whenLiveResolutionFails_returnsEmptyUrlAndRepoIdWithoutThrowing() {
    AlmSettingDto gitlabAlmSetting = db.almSettings().insertGitlabAlmSetting();
    ProjectAlmSettingDto projectAlmSetting = db.almSettings().insertGitlabProjectAlmSetting(gitlabAlmSetting, project,
      dto -> dto.setAlmRepo("123"));
    when(gitlabApplicationClient.getProject(anyString(), anyString(), anyLong())).thenThrow(new IllegalStateException("boom"));

    ProjectBinding binding = underTest.getProjectBinding(projectAlmSetting.getUuid());

    assertThat(binding.getUrl()).isEmpty();
    assertThat(binding.getRepositoryId()).isEmpty();

    ProjectAlmSettingDto reloaded = db.getDbClient().projectAlmSettingDao().selectByUuid(db.getSession(), projectAlmSetting.getUuid()).orElseThrow();
    assertThat(reloaded.getUrl()).isNull();
    assertThat(reloaded.getRepoId()).isNull();
  }

  @Test
  void getProjectBinding_whenAlmRepoContainsCrlf_neverLeaksItRawIntoTheLog() {
    AlmSettingDto gitlabAlmSetting = db.almSettings().insertGitlabAlmSetting();
    String maliciousAlmRepo = "123\r\nFAKE-LOG-LINE-INJECTED-BY-ATTACKER";
    ProjectAlmSettingDto projectAlmSetting = db.almSettings().insertGitlabProjectAlmSetting(gitlabAlmSetting, project,
      dto -> dto.setAlmRepo(maliciousAlmRepo));

    // Non-numeric almRepo forces Long.parseLong to throw — its own NumberFormatException message would otherwise
    // embed the raw, unsanitized string verbatim (see resolveGitlab's catch block).
    ProjectBinding binding = underTest.getProjectBinding(projectAlmSetting.getUuid());

    assertThat(binding.getUrl()).isEmpty();
    assertThat(binding.getRepositoryId()).isEmpty();
    assertThat(logTester.logs(Level.WARN))
      .isNotEmpty()
      .allSatisfy(log -> assertThat(log).doesNotContain("\r").doesNotContain("\n"));
  }

  @Test
  void getProjectBinding_whenPersistingResolvedValueFails_stillReturnsResolvedValueAndRollsBackTheSession() {
    AlmSettingDto gitlabAlmSetting = db.almSettings().insertGitlabAlmSetting();
    ProjectAlmSettingDto projectAlmSetting = db.almSettings().insertGitlabProjectAlmSetting(gitlabAlmSetting, project,
      dto -> dto.setAlmRepo("123"));
    Project gitlabProject = mock(Project.class);
    when(gitlabProject.getWebUrl()).thenReturn("https://gitlab.example.com/group/repo");
    when(gitlabProject.getId()).thenReturn(123L);
    when(gitlabApplicationClient.getProject(anyString(), anyString(), eq(123L))).thenReturn(gitlabProject);

    ProjectAlmSettingDao spiedDao = spy(db.getDbClient().projectAlmSettingDao());
    doThrow(new IllegalStateException("simulated DB failure")).when(spiedDao)
      .updateUrlAndRepoId(any(DbSession.class), anyString(), anyString(), anyString());
    // Must be a spy on a *real* session, not a mock: the reads that happen before the persist step (fetching the
    // binding, the ALM setting, and — via the real DAO spied above — issuing the actual failing UPDATE) all need a
    // genuine mapper-backed session; only the single write call is overridden.
    DbSession spiedSession = spy(db.getDbClient().openSession(false));
    DbClient spiedDbClient = spy(db.getDbClient());
    doReturn(spiedDao).when(spiedDbClient).projectAlmSettingDao();
    doReturn(spiedSession).when(spiedDbClient).openSession(false);
    ProjectBindingsServiceServerImpl underTestWithFailingPersist = new ProjectBindingsServiceServerImpl(
      spiedDbClient, githubApplicationClient, githubGlobalSettingsValidator, gitlabApplicationClient,
      azureDevOpsHttpClient, bitbucketServerRestClient, bitbucketCloudRestClient, settings);

    ProjectBinding binding = underTestWithFailingPersist.getProjectBinding(projectAlmSetting.getUuid());

    // the read itself still returns the correctly-resolved (just not persisted) value — a persist failure must
    // not turn into a resolution failure
    assertThat(binding.getUrl()).isEqualTo("https://gitlab.example.com/group/repo");
    assertThat(binding.getRepositoryId()).isEqualTo("123");

    verify(spiedSession).rollback();
    verify(spiedSession, never()).commit();

    ProjectAlmSettingDto reloaded = db.getDbClient().projectAlmSettingDao().selectByUuid(db.getSession(), projectAlmSetting.getUuid()).orElseThrow();
    assertThat(reloaded.getUrl()).isNull();
    assertThat(reloaded.getRepoId()).isNull();
  }

  @Test
  void searchProjectBindings_byProjectId_returnsMatchingBinding() {
    AlmSettingDto githubAlmSetting = db.almSettings().insertGitHubAlmSetting();
    ProjectAlmSettingDto projectAlmSetting = db.almSettings().insertGitHubProjectAlmSetting(githubAlmSetting, project,
      dto -> dto.setUrl("https://github.com/sonarsource/sonar-enterprise").setRepoId("1"));

    ProjectBindings result = underTest.searchProjectBindings(query(project.getUuid(), null, null, null, null));

    assertThat(result.getBindings()).extracting(ProjectBinding::getId).containsExactly(projectAlmSetting.getUuid());
    assertThat(result.getPage().getTotal()).isEqualTo(1);
  }

  @Test
  void searchProjectBindings_byUrl_returnsMatchingBinding() {
    AlmSettingDto githubAlmSetting = db.almSettings().insertGitHubAlmSetting();
    ProjectAlmSettingDto projectAlmSetting = db.almSettings().insertGitHubProjectAlmSetting(githubAlmSetting, project,
      dto -> dto.setUrl("https://github.com/sonarsource/sonar-enterprise").setRepoId("1"));

    ProjectBindings result = underTest.searchProjectBindings(query(null, "https://github.com/sonarsource/sonar-enterprise", null, null, null));

    assertThat(result.getBindings()).extracting(ProjectBinding::getId).containsExactly(projectAlmSetting.getUuid());
  }

  @Test
  void searchProjectBindings_byDevOpsPlatformAndRepositoryId_returnsMatchingBinding() {
    AlmSettingDto githubAlmSetting = db.almSettings().insertGitHubAlmSetting();
    ProjectAlmSettingDto projectAlmSetting = db.almSettings().insertGitHubProjectAlmSetting(githubAlmSetting, project,
      dto -> dto.setUrl("https://github.com/sonarsource/sonar-enterprise").setRepoId("999"));

    ProjectBindings result = underTest.searchProjectBindings(queryByDevOpsPlatform("github", "999"));

    assertThat(result.getBindings()).extracting(ProjectBinding::getId).containsExactly(projectAlmSetting.getUuid());
  }

  @Test
  void searchProjectBindings_byOrganizationIdMatchingSingleTenant_returnsAllBindings() {
    AlmSettingDto githubAlmSetting = db.almSettings().insertGitHubAlmSetting();
    db.almSettings().insertGitHubProjectAlmSetting(githubAlmSetting, project,
      dto -> dto.setUrl("https://github.com/sonarsource/sonar-enterprise").setRepoId("1"));

    ProjectBindings result = underTest.searchProjectBindings(queryByOrganizationId(DefaultOrganizationProvider.ID.toString()));

    assertThat(result.getBindings()).hasSize(1);
  }

  @Test
  void searchProjectBindings_byOrganizationId_resolvesOneBindingLiveWithoutBreakingOthersOnTheSharedSession() {
    AlmSettingDto githubAlmSetting = db.almSettings().insertGitHubAlmSetting();
    ProjectAlmSettingDto storedBinding = db.almSettings().insertGitHubProjectAlmSetting(githubAlmSetting, project,
      dto -> dto.setUrl("https://github.com/sonarsource/sonar-enterprise").setRepoId("1"));

    ProjectDto secondProject = db.components().insertPrivateProject().getProjectDto();
    ProjectAlmSettingDto liveBinding = db.almSettings().insertGitHubProjectAlmSetting(githubAlmSetting, secondProject,
      dto -> dto.setAlmRepo("sonarsource/other-repo"));
    GithubAppConfiguration githubAppConfiguration = new GithubAppConfiguration(1L, "private-key", githubAlmSetting.getUrl());
    when(githubGlobalSettingsValidator.validate(any(AlmSettingDto.class))).thenReturn(githubAppConfiguration);
    when(githubApplicationClient.getInstallationId(githubAppConfiguration, "sonarsource/other-repo")).thenReturn(Optional.of(88L));
    ExpiringAppInstallationToken token = mock(ExpiringAppInstallationToken.class);
    when(githubApplicationClient.createAppInstallationToken(githubAppConfiguration, 88L, "other-repo")).thenReturn(Optional.of(token));
    GithubApplicationClient.Repository repository = new GithubApplicationClient.Repository(777L, "other-repo", false,
      "sonarsource/other-repo", "https://github.com/sonarsource/other-repo", "main");
    when(githubApplicationClient.getRepository(anyString(), eq(token), eq("sonarsource/other-repo"))).thenReturn(Optional.of(repository));

    // Both bindings are resolved on the same DbSession within one searchProjectBindings call: the stored one is a
    // plain read, the other requires a live resolution-and-persist mid-iteration. Neither should affect the other.
    ProjectBindings result = underTest.searchProjectBindings(queryByOrganizationId(DefaultOrganizationProvider.ID.toString()));

    assertThat(result.getBindings()).extracting(ProjectBinding::getId, ProjectBinding::getUrl, ProjectBinding::getRepositoryId)
      .containsExactlyInAnyOrder(
        tuple(storedBinding.getUuid(), "https://github.com/sonarsource/sonar-enterprise", "1"),
        tuple(liveBinding.getUuid(), "https://github.com/sonarsource/other-repo", "777"));

    ProjectAlmSettingDto reloadedLive = db.getDbClient().projectAlmSettingDao().selectByUuid(db.getSession(), liveBinding.getUuid()).orElseThrow();
    assertThat(reloadedLive.getUrl()).isEqualTo("https://github.com/sonarsource/other-repo");
    assertThat(reloadedLive.getRepoId()).isEqualTo("777");
  }

  @Test
  void searchProjectBindings_whenOneBindingsAlmSettingIsMissing_skipsItButStillReturnsTheOthers() {
    AlmSettingDto githubAlmSetting = db.almSettings().insertGitHubAlmSetting();
    String sharedUrl = "https://github.com/sonarsource/shared-repo";
    ProjectAlmSettingDto healthyBinding = db.almSettings().insertGitHubProjectAlmSetting(githubAlmSetting, project,
      dto -> dto.setUrl(sharedUrl).setRepoId("1"));

    // A binding whose alm_setting_uuid points at nothing — simulates the ALM setting having been deleted
    // concurrently with this read. selectByUrl (unlike the organizationId/devOpsPlatform paths) doesn't join
    // alm_settings, so this orphaned row is still found by the search and only fails later, inside toProjectBinding.
    ProjectDto orphanProject = db.components().insertPrivateProject().getProjectDto();
    ProjectAlmSettingDto orphanBinding = new ProjectAlmSettingDto()
      .setAlmSettingUuid("does-not-exist")
      .setProjectUuid(orphanProject.getUuid())
      .setAlmRepo("owner/repo")
      .setMonorepo(false)
      .setUrl(sharedUrl)
      .setRepoId("2");
    db.getDbClient().projectAlmSettingDao().insertOrUpdate(db.getSession(), orphanBinding, "orphan-key", orphanProject.getName(), orphanProject.getKey());
    db.commit();

    ProjectBindings result = underTest.searchProjectBindings(query(null, sharedUrl, null, null, null));

    assertThat(result.getBindings()).extracting(ProjectBinding::getId).containsExactly(healthyBinding.getUuid());
    assertThat(result.getPage().getTotal()).isEqualTo(1);
  }

  @Test
  void searchProjectBindings_byOrganizationIdNotMatchingSingleTenant_returnsEmpty() {
    AlmSettingDto githubAlmSetting = db.almSettings().insertGitHubAlmSetting();
    db.almSettings().insertGitHubProjectAlmSetting(githubAlmSetting, project,
      dto -> dto.setUrl("https://github.com/sonarsource/sonar-enterprise").setRepoId("1"));

    ProjectBindings result = underTest.searchProjectBindings(queryByOrganizationId("some-other-organization-id"));

    assertThat(result.getBindings()).isEmpty();
    assertThat(result.getPage().getTotal()).isZero();
  }

  @Test
  void searchProjectBindings_whenNoParameterProvided_throwsBadRequestException() {
    ProjectBindingsQuery query = query(null, null, null, null, null);

    assertThatThrownBy(() -> underTest.searchProjectBindings(query))
      .isInstanceOf(BadRequestException.class);
  }

  @Test
  void searchProjectBindings_whenMultipleParametersProvided_throwsBadRequestException() {
    ProjectBindingsQuery query = query(project.getUuid(), "url", null, null, null);

    assertThatThrownBy(() -> underTest.searchProjectBindings(query))
      .isInstanceOf(BadRequestException.class);
  }

  @Test
  void searchProjectBindings_whenDevOpsPlatformProvidedWithoutRepositoryId_throwsBadRequestException() {
    ProjectBindingsQuery query = queryByDevOpsPlatform("github", null);

    assertThatThrownBy(() -> underTest.searchProjectBindings(query))
      .isInstanceOf(BadRequestException.class);
  }

  private static ProjectBindingsQuery query(String projectId, String url, String devOpsPlatform, String repositoryId, String organizationId) {
    return new ProjectBindingsQuery(projectId, url, devOpsPlatform, repositoryId, organizationId, null);
  }

  private static ProjectBindingsQuery queryByDevOpsPlatform(String devOpsPlatform, String repositoryId) {
    return new ProjectBindingsQuery(null, null, devOpsPlatform, repositoryId, null, null);
  }

  private static ProjectBindingsQuery queryByOrganizationId(String organizationId) {
    return new ProjectBindingsQuery(null, null, null, null, organizationId, null);
  }

}
