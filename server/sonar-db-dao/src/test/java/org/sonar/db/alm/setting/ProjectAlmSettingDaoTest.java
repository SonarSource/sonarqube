/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.project.ProjectDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.almsettings.AlmSettingsTesting.newBitbucketProjectAlmSettingDto;
import static org.sonar.db.almsettings.AlmSettingsTesting.newGithubProjectAlmSettingDto;

public class ProjectAlmSettingDaoTest {

  private static final long A_DATE = 1_000_000_000_000L;
  private static final long A_DATE_LATER = 1_700_000_000_000L;

  private static final String A_UUID = "SOME_UUID";
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  private System2 system2 = mock(System2.class);
  @Rule
  public DbTester db = DbTester.create(system2);

  private DbSession dbSession = db.getSession();
  private UuidFactory uuidFactory = mock(UuidFactory.class);
  private ProjectAlmSettingDao underTest = new ProjectAlmSettingDao(system2, uuidFactory);

  @Test
  public void select_by_project() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    when(system2.now()).thenReturn(A_DATE);
    AlmSettingDto githubAlmSettingDto = db.almSettings().insertGitHubAlmSetting();
    ProjectDto project = db.components().insertPrivateProjectDto();
    ProjectDto anotherProject = db.components().insertPrivateProjectDto();
    ProjectAlmSettingDto githubProjectAlmSettingDto = newGithubProjectAlmSettingDto(githubAlmSettingDto, project);
    underTest.insertOrUpdate(dbSession, githubProjectAlmSettingDto);

    assertThat(underTest.selectByProject(dbSession, project).get())
      .extracting(ProjectAlmSettingDto::getUuid, ProjectAlmSettingDto::getAlmSettingUuid, ProjectAlmSettingDto::getProjectUuid,
        ProjectAlmSettingDto::getAlmRepo, ProjectAlmSettingDto::getAlmSlug,
        ProjectAlmSettingDto::getCreatedAt, ProjectAlmSettingDto::getUpdatedAt)
      .containsExactly(A_UUID, githubAlmSettingDto.getUuid(), project.getUuid(),
        githubProjectAlmSettingDto.getAlmRepo(), githubProjectAlmSettingDto.getAlmSlug(),
        A_DATE, A_DATE);

    assertThat(underTest.selectByProject(dbSession, anotherProject)).isNotPresent();
  }

  @Test
  public void select_by_alm_setting_and_slugs() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    when(system2.now()).thenReturn(A_DATE);
    AlmSettingDto almSettingsDto = db.almSettings().insertBitbucketAlmSetting();
    ProjectDto project = db.components().insertPrivateProjectDto();
    ProjectAlmSettingDto githubProjectAlmSettingDto = newBitbucketProjectAlmSettingDto(almSettingsDto, project);
    githubProjectAlmSettingDto.setAlmSlug("slug1");
    underTest.insertOrUpdate(dbSession, githubProjectAlmSettingDto);
    ProjectAlmSettingDto githubProjectAlmSettingDto2 = newBitbucketProjectAlmSettingDto(almSettingsDto, db.components().insertPrivateProjectDto());
    githubProjectAlmSettingDto2.setAlmSlug("slug2");
    when(uuidFactory.create()).thenReturn(A_UUID + 1);
    underTest.insertOrUpdate(dbSession, githubProjectAlmSettingDto2);

    Set<String> slugs = new HashSet<>();
    slugs.add("slug1");
    assertThat(underTest.selectByAlmSettingAndSlugs(dbSession, almSettingsDto, slugs))
      .extracting(ProjectAlmSettingDto::getProjectUuid)
      .containsExactly(project.getUuid());
  }

  @Test
  public void select_with_no_slugs_return_empty() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    when(system2.now()).thenReturn(A_DATE);
    AlmSettingDto almSettingsDto = db.almSettings().insertBitbucketAlmSetting();

    assertThat(underTest.selectByAlmSettingAndSlugs(dbSession, almSettingsDto, new HashSet<>())).isEmpty();
  }

  @Test
  public void update_existing_binding() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    when(system2.now()).thenReturn(A_DATE);
    AlmSettingDto githubAlmSetting = db.almSettings().insertGitHubAlmSetting();
    ProjectDto project = db.components().insertPrivateProjectDto();
    ProjectAlmSettingDto projectAlmSettingDto = db.almSettings().insertGitHubProjectAlmSetting(githubAlmSetting, project);
    AlmSettingDto anotherGithubAlmSetting = db.almSettings().insertGitHubAlmSetting();

    when(system2.now()).thenReturn(A_DATE_LATER);
    ProjectAlmSettingDto newProjectAlmSettingDto = newGithubProjectAlmSettingDto(anotherGithubAlmSetting, project);
    underTest.insertOrUpdate(dbSession, newProjectAlmSettingDto);

    assertThat(underTest.selectByProject(dbSession, project).get())
      .extracting(ProjectAlmSettingDto::getUuid, ProjectAlmSettingDto::getAlmSettingUuid, ProjectAlmSettingDto::getProjectUuid,
        ProjectAlmSettingDto::getAlmRepo, ProjectAlmSettingDto::getAlmSlug,
        ProjectAlmSettingDto::getCreatedAt, ProjectAlmSettingDto::getUpdatedAt)
      .containsExactly(projectAlmSettingDto.getUuid(), anotherGithubAlmSetting.getUuid(), project.getUuid(),
        newProjectAlmSettingDto.getAlmRepo(), newProjectAlmSettingDto.getAlmSlug(),
        A_DATE, A_DATE_LATER);
  }

  @Test
  public void deleteByProject() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    when(system2.now()).thenReturn(A_DATE);
    AlmSettingDto githubAlmSetting = db.almSettings().insertGitHubAlmSetting();
    ProjectDto project = db.components().insertPrivateProjectDto();
    db.almSettings().insertGitHubProjectAlmSetting(githubAlmSetting, project);
    ProjectDto anotherProject = db.components().insertPrivateProjectDto();
    db.almSettings().insertGitHubProjectAlmSetting(githubAlmSetting, anotherProject);

    underTest.deleteByProject(dbSession, project);

    assertThat(underTest.selectByProject(dbSession, project)).isEmpty();
    assertThat(underTest.selectByProject(dbSession, anotherProject)).isNotEmpty();
  }

  @Test
  public void deleteByAlmSetting() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    when(system2.now()).thenReturn(A_DATE);
    AlmSettingDto githubAlmSetting = db.almSettings().insertGitHubAlmSetting();
    ProjectDto project1 = db.components().insertPrivateProjectDto();
    ProjectDto project2 = db.components().insertPrivateProjectDto();
    db.almSettings().insertGitHubProjectAlmSetting(githubAlmSetting, project1);
    db.almSettings().insertGitHubProjectAlmSetting(githubAlmSetting, project2);

    AlmSettingDto githubAlmSetting1 = db.almSettings().insertGitHubAlmSetting();
    ProjectDto anotherProject = db.components().insertPrivateProjectDto();
    db.almSettings().insertGitHubProjectAlmSetting(githubAlmSetting1, anotherProject);

    underTest.deleteByAlmSetting(dbSession, githubAlmSetting);

    assertThat(underTest.countByAlmSetting(dbSession, githubAlmSetting)).isZero();
    assertThat(underTest.countByAlmSetting(dbSession, githubAlmSetting1)).isEqualTo(1);
  }

}
