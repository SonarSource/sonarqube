/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
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
  public void selectByProject() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    when(system2.now()).thenReturn(A_DATE);
    AlmSettingDto githubAlmSettingDto = db.almSettings().insertGitHubAlmSetting();
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto anotherProject = db.components().insertPrivateProject();
    ProjectAlmSettingDto githubProjectAlmSettingDto = newGithubProjectAlmSettingDto(githubAlmSettingDto, project);
    underTest.insertOrUpdate(dbSession, githubProjectAlmSettingDto);

    assertThat(underTest.selectByProject(dbSession, project).get())
      .extracting(ProjectAlmSettingDto::getUuid, ProjectAlmSettingDto::getAlmSettingUuid, ProjectAlmSettingDto::getProjectUuid,
        ProjectAlmSettingDto::getAlmRepo, ProjectAlmSettingDto::getAlmSlug,
        ProjectAlmSettingDto::getCreatedAt, ProjectAlmSettingDto::getUpdatedAt)
      .containsExactly(A_UUID, githubAlmSettingDto.getUuid(), project.uuid(),
        githubProjectAlmSettingDto.getAlmRepo(), githubProjectAlmSettingDto.getAlmSlug(),
        A_DATE, A_DATE);

    assertThat(underTest.selectByProject(dbSession, anotherProject)).isNotPresent();
  }

  @Test
  public void update_existing_binding() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    when(system2.now()).thenReturn(A_DATE);
    AlmSettingDto githubAlmSetting = db.almSettings().insertGitHubAlmSetting();
    ComponentDto project = db.components().insertPrivateProject();
    ProjectAlmSettingDto projectAlmSettingDto = db.almSettings().insertGitHubProjectAlmSetting(githubAlmSetting, project);
    AlmSettingDto anotherGithubAlmSetting = db.almSettings().insertGitHubAlmSetting();

    when(system2.now()).thenReturn(A_DATE_LATER);
    ProjectAlmSettingDto newProjectAlmSettingDto = newGithubProjectAlmSettingDto(anotherGithubAlmSetting, project);
    underTest.insertOrUpdate(dbSession, newProjectAlmSettingDto);

    assertThat(underTest.selectByProject(dbSession, project).get())
      .extracting(ProjectAlmSettingDto::getUuid, ProjectAlmSettingDto::getAlmSettingUuid, ProjectAlmSettingDto::getProjectUuid,
        ProjectAlmSettingDto::getAlmRepo, ProjectAlmSettingDto::getAlmSlug,
        ProjectAlmSettingDto::getCreatedAt, ProjectAlmSettingDto::getUpdatedAt)
      .containsExactly(projectAlmSettingDto.getUuid(), anotherGithubAlmSetting.getUuid(), project.uuid(),
        newProjectAlmSettingDto.getAlmRepo(), newProjectAlmSettingDto.getAlmSlug(),
        A_DATE, A_DATE_LATER);
  }

  @Test
  public void deleteByProject() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    when(system2.now()).thenReturn(A_DATE);
    AlmSettingDto githubAlmSetting = db.almSettings().insertGitHubAlmSetting();
    ComponentDto project = db.components().insertPrivateProject();
    db.almSettings().insertGitHubProjectAlmSetting(githubAlmSetting, project);
    ComponentDto anotherProject = db.components().insertPrivateProject();
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
    ComponentDto project1 = db.components().insertPrivateProject();
    ComponentDto project2 = db.components().insertPrivateProject();
    db.almSettings().insertGitHubProjectAlmSetting(githubAlmSetting, project1);
    db.almSettings().insertGitHubProjectAlmSetting(githubAlmSetting, project2);

    AlmSettingDto githubAlmSetting1 = db.almSettings().insertGitHubAlmSetting();
    ComponentDto anotherProject = db.components().insertPrivateProject();
    db.almSettings().insertGitHubProjectAlmSetting(githubAlmSetting1, anotherProject);

    underTest.deleteByAlmSetting(dbSession, githubAlmSetting);

    assertThat(underTest.countByAlmSetting(dbSession, githubAlmSetting)).isZero();
    assertThat(underTest.countByAlmSetting(dbSession, githubAlmSetting1)).isEqualTo(1);
  }

}
