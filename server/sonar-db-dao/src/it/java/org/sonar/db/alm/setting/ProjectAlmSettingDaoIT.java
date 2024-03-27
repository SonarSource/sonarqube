/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.db.alm.setting;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.audit.NoOpAuditPersister;
import org.sonar.db.project.ProjectDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.almsettings.AlmSettingsTesting.newBitbucketProjectAlmSettingDto;
import static org.sonar.db.almsettings.AlmSettingsTesting.newGithubProjectAlmSettingDto;

class ProjectAlmSettingDaoIT {

  private static final long A_DATE = 1_000_000_000_000L;
  private static final long A_DATE_LATER = 1_700_000_000_000L;

  private static final String A_UUID = "SOME_UUID";
  private static final String ANOTHER_UUID = "SOME_UUID2";
  private final TestSystem2 system2 = new TestSystem2().setNow(A_DATE);
  @RegisterExtension
  private final DbTester db = DbTester.create(system2);

  private final DbSession dbSession = db.getSession();
  private final UuidFactory uuidFactory = mock(UuidFactory.class);
  private final ProjectAlmSettingDao underTest = new ProjectAlmSettingDao(system2, uuidFactory, new NoOpAuditPersister());

  @Test
  void select_by_project() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    AlmSettingDto githubAlmSettingDto = db.almSettings().insertGitHubAlmSetting();
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    ProjectDto anotherProject = db.components().insertPrivateProject().getProjectDto();
    ProjectAlmSettingDto githubProjectAlmSettingDto = newGithubProjectAlmSettingDto(githubAlmSettingDto, project);
    underTest.insertOrUpdate(dbSession, githubProjectAlmSettingDto, githubAlmSettingDto.getKey(), anotherProject.getName(),
      anotherProject.getKey());

    assertThat(underTest.selectByProject(dbSession, project).get())
      .extracting(ProjectAlmSettingDto::getUuid, ProjectAlmSettingDto::getAlmSettingUuid, ProjectAlmSettingDto::getProjectUuid,
        ProjectAlmSettingDto::getAlmRepo, ProjectAlmSettingDto::getAlmSlug,
        ProjectAlmSettingDto::getCreatedAt, ProjectAlmSettingDto::getUpdatedAt,
        ProjectAlmSettingDto::getSummaryCommentEnabled, ProjectAlmSettingDto::getMonorepo)
      .containsExactly(A_UUID, githubAlmSettingDto.getUuid(), project.getUuid(),
        githubProjectAlmSettingDto.getAlmRepo(), githubProjectAlmSettingDto.getAlmSlug(),
        A_DATE, A_DATE, githubProjectAlmSettingDto.getSummaryCommentEnabled(), false);

    assertThat(underTest.selectByProject(dbSession, anotherProject)).isNotPresent();
  }

  @Test
  void select_by_alm_setting_and_slugs() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    AlmSettingDto almSettingsDto = db.almSettings().insertBitbucketAlmSetting();
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    ProjectAlmSettingDto bitbucketProjectAlmSettingDto = newBitbucketProjectAlmSettingDto(almSettingsDto, project);
    bitbucketProjectAlmSettingDto.setAlmSlug("slug1");
    underTest.insertOrUpdate(dbSession, bitbucketProjectAlmSettingDto, almSettingsDto.getKey(), project.getName(), project.getKey());
    ProjectAlmSettingDto bitbucketProjectAlmSettingDto2 = newBitbucketProjectAlmSettingDto(almSettingsDto,
      db.components().insertPrivateProject().getProjectDto());
    bitbucketProjectAlmSettingDto2.setAlmSlug("slug2");
    when(uuidFactory.create()).thenReturn(A_UUID + 1);
    underTest.insertOrUpdate(dbSession, bitbucketProjectAlmSettingDto2, almSettingsDto.getKey(), project.getName(), project.getKey());

    Set<String> slugs = new HashSet<>();
    slugs.add("slug1");
    assertThat(underTest.selectByAlmSettingAndSlugs(dbSession, almSettingsDto, slugs))
      .extracting(ProjectAlmSettingDto::getProjectUuid, ProjectAlmSettingDto::getSummaryCommentEnabled)
      .containsExactly(tuple(project.getUuid(), bitbucketProjectAlmSettingDto2.getSummaryCommentEnabled()));
  }

  @Test
  void selectByAlm_whenGivenGithub_onlyReturnsGithubProjects() {
    ProjectAlmSettingDto githubProject1 = createAlmProject(db.almSettings().insertGitHubAlmSetting());
    ProjectAlmSettingDto githubProject2 = createAlmProject(db.almSettings().insertGitHubAlmSetting());
    createAlmProject(db.almSettings().insertGitlabAlmSetting());

    List<ProjectAlmSettingDto> projectAlmSettingDtos = underTest.selectByAlm(dbSession, ALM.GITHUB);

    assertThat(projectAlmSettingDtos)
      .usingRecursiveFieldByFieldElementComparator()
      .containsExactlyInAnyOrder(githubProject1, githubProject2);
  }

  @Test
  void selectByProjectUuidsAndAlm_whenGivenGithubAndProjectUuids_shouldOnlyReturnThose() {
    AlmSettingDto githubSetting = db.almSettings().insertGitHubAlmSetting();
    ProjectAlmSettingDto githubProject = createAlmProject(githubSetting);
    createAlmProject(githubSetting);

    AlmSettingDto gitlabSetting = db.almSettings().insertGitlabAlmSetting();
    ProjectAlmSettingDto gitlabProject = createAlmProject(gitlabSetting);

    List<ProjectAlmSettingDto> projectAlmSettingDtos = underTest.selectByProjectUuidsAndAlm(dbSession, Set.of(githubProject.getProjectUuid(), gitlabProject.getProjectUuid()),
      ALM.GITHUB);

    assertThat(projectAlmSettingDtos)
      .usingRecursiveFieldByFieldElementComparator()
      .containsExactlyInAnyOrder(githubProject);
  }

  private ProjectAlmSettingDto createAlmProject(AlmSettingDto almSettingsDto) {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    when(uuidFactory.create()).thenReturn(project.getUuid() + "_set");
    ProjectAlmSettingDto githubProjectAlmSettingDto = newGithubProjectAlmSettingDto(almSettingsDto, project);
    underTest.insertOrUpdate(dbSession, githubProjectAlmSettingDto, almSettingsDto.getKey(), project.getName(), project.getKey());
    return githubProjectAlmSettingDto;
  }

  @Test
  void select_with_no_slugs_return_empty() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    AlmSettingDto almSettingsDto = db.almSettings().insertBitbucketAlmSetting();

    assertThat(underTest.selectByAlmSettingAndSlugs(dbSession, almSettingsDto, new HashSet<>())).isEmpty();
  }

  @Test
  void select_by_alm_setting_and_repos() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    AlmSettingDto almSettingsDto = db.almSettings().insertGitHubAlmSetting();
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    ProjectAlmSettingDto githubProjectAlmSettingDto = newGithubProjectAlmSettingDto(almSettingsDto, project);
    githubProjectAlmSettingDto.setAlmRepo("repo1");
    underTest.insertOrUpdate(dbSession, githubProjectAlmSettingDto, almSettingsDto.getKey(), project.getName(), project.getKey());
    ProjectAlmSettingDto githubProjectAlmSettingDto2 = newGithubProjectAlmSettingDto(almSettingsDto,
      db.components().insertPrivateProject().getProjectDto());
    githubProjectAlmSettingDto2.setAlmRepo("repo2");
    when(uuidFactory.create()).thenReturn(A_UUID + 1);
    underTest.insertOrUpdate(dbSession, githubProjectAlmSettingDto2, almSettingsDto.getKey(), project.getName(), project.getKey());

    Set<String> repos = new HashSet<>();
    repos.add("repo1");
    assertThat(underTest.selectByAlmSettingAndRepos(dbSession, almSettingsDto, repos))
      .extracting(ProjectAlmSettingDto::getProjectUuid, ProjectAlmSettingDto::getSummaryCommentEnabled)
      .containsExactly(tuple(project.getUuid(), githubProjectAlmSettingDto.getSummaryCommentEnabled()));
  }

  @Test
  void select_with_no_repos_return_empty() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    AlmSettingDto almSettingsDto = db.almSettings().insertGitHubAlmSetting();

    assertThat(underTest.selectByAlmSettingAndRepos(dbSession, almSettingsDto, new HashSet<>())).isEmpty();
  }

  @Test
  void selectAlmTypeAndUrlByProject_returnsCorrectValues() {
    when(uuidFactory.create())
      .thenReturn(A_UUID)
      .thenReturn(ANOTHER_UUID);
    AlmSettingDto almSettingsDto = db.almSettings().insertGitHubAlmSetting();

    ProjectDto project1 = db.components().insertPrivateProject().getProjectDto();
    ProjectDto project2 = db.components().insertPrivateProject().getProjectDto();

    ProjectAlmSettingDto githubProjectAlmSettingDto1 = newGithubProjectAlmSettingDto(almSettingsDto, project1, false);
    ProjectAlmSettingDto githubProjectAlmSettingDto2 = newGithubProjectAlmSettingDto(almSettingsDto, project2, true);

    underTest.insertOrUpdate(dbSession, githubProjectAlmSettingDto1, almSettingsDto.getKey(), project1.getName(), project1.getKey());
    underTest.insertOrUpdate(dbSession, githubProjectAlmSettingDto2, almSettingsDto.getKey(), project2.getName(), project2.getKey());

    assertThat(underTest.selectAlmTypeAndUrlByProject(dbSession))
      .extracting(
        ProjectAlmKeyAndProject::getProjectUuid,
        ProjectAlmKeyAndProject::getAlmId,
        ProjectAlmKeyAndProject::getUrl,
        ProjectAlmKeyAndProject::getMonorepo
      ).containsExactlyInAnyOrder(
        tuple(project1.getUuid(), almSettingsDto.getAlm().getId(), almSettingsDto.getUrl(), false),
        tuple(project2.getUuid(), almSettingsDto.getAlm().getId(), almSettingsDto.getUrl(), true));
  }

  @Test
  void update_existing_binding() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    AlmSettingDto githubAlmSetting = db.almSettings().insertGitHubAlmSetting();
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    ProjectAlmSettingDto projectAlmSettingDto = db.almSettings().insertGitHubProjectAlmSetting(githubAlmSetting, project);
    AlmSettingDto anotherGithubAlmSetting = db.almSettings().insertGitHubAlmSetting();

    system2.setNow(A_DATE_LATER);
    ProjectAlmSettingDto newProjectAlmSettingDto = newGithubProjectAlmSettingDto(anotherGithubAlmSetting, project)
      .setSummaryCommentEnabled(false);
    underTest.insertOrUpdate(dbSession, newProjectAlmSettingDto, githubAlmSetting.getKey(), project.getName(), project.getKey());

    assertThat(underTest.selectByProject(dbSession, project).get())
      .extracting(ProjectAlmSettingDto::getUuid, ProjectAlmSettingDto::getAlmSettingUuid, ProjectAlmSettingDto::getProjectUuid,
        ProjectAlmSettingDto::getAlmRepo, ProjectAlmSettingDto::getAlmSlug,
        ProjectAlmSettingDto::getCreatedAt, ProjectAlmSettingDto::getUpdatedAt,
        ProjectAlmSettingDto::getSummaryCommentEnabled)
      .containsExactly(projectAlmSettingDto.getUuid(), anotherGithubAlmSetting.getUuid(), project.getUuid(),
        newProjectAlmSettingDto.getAlmRepo(), newProjectAlmSettingDto.getAlmSlug(),
        A_DATE, A_DATE_LATER, newProjectAlmSettingDto.getSummaryCommentEnabled());
  }

  @Test
  void deleteByProject() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    AlmSettingDto githubAlmSetting = db.almSettings().insertGitHubAlmSetting();
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    db.almSettings().insertGitHubProjectAlmSetting(githubAlmSetting, project);
    ProjectDto anotherProject = db.components().insertPrivateProject().getProjectDto();
    db.almSettings().insertGitHubProjectAlmSetting(githubAlmSetting, anotherProject);

    underTest.deleteByProject(dbSession, project);

    assertThat(underTest.selectByProject(dbSession, project)).isEmpty();
    assertThat(underTest.selectByProject(dbSession, anotherProject)).isNotEmpty();
  }

  @Test
  void deleteByAlmSetting() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    AlmSettingDto githubAlmSetting = db.almSettings().insertGitHubAlmSetting();
    ProjectDto project1 = db.components().insertPrivateProject().getProjectDto();
    ProjectDto project2 = db.components().insertPrivateProject().getProjectDto();
    db.almSettings().insertGitHubProjectAlmSetting(githubAlmSetting, project1);
    db.almSettings().insertGitHubProjectAlmSetting(githubAlmSetting, project2);

    AlmSettingDto githubAlmSetting1 = db.almSettings().insertGitHubAlmSetting();
    ProjectDto anotherProject = db.components().insertPrivateProject().getProjectDto();
    db.almSettings().insertGitHubProjectAlmSetting(githubAlmSetting1, anotherProject);

    underTest.deleteByAlmSetting(dbSession, githubAlmSetting);

    assertThat(underTest.countByAlmSetting(dbSession, githubAlmSetting)).isZero();
    assertThat(underTest.countByAlmSetting(dbSession, githubAlmSetting1)).isOne();
  }

}
