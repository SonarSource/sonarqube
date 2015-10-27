/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.db.purge;

import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.resources.Scopes;
import org.sonar.api.utils.System2;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.ce.CeTaskTypes.REPORT;

@Category(DbTests.class)
public class PurgeDaoTest {

  System2 system2 = mock(System2.class);

  @Rule
  public DbTester dbTester = DbTester.create(system2);

  DbClient dbClient = dbTester.getDbClient();

  DbSession dbSession = dbTester.getSession();

  PurgeDao underTest = dbTester.getDbClient().purgeDao();

  @Test
  public void shouldDeleteAbortedBuilds() {
    dbTester.prepareDbUnit(getClass(), "shouldDeleteAbortedBuilds.xml");
    underTest.purge(newConfigurationWith30Days(), PurgeListener.EMPTY, new PurgeProfiler());
    dbTester.assertDbUnit(getClass(), "shouldDeleteAbortedBuilds-result.xml", "snapshots");
  }

  @Test
  public void should_purge_project() {
    dbTester.prepareDbUnit(getClass(), "shouldPurgeProject.xml");
    underTest.purge(newConfigurationWith30Days(), PurgeListener.EMPTY, new PurgeProfiler());
    dbTester.assertDbUnit(getClass(), "shouldPurgeProject-result.xml", "projects", "snapshots");
  }

  @Test
  public void delete_file_sources_of_disabled_resources() {
    dbTester.prepareDbUnit(getClass(), "delete_file_sources_of_disabled_resources.xml");
    underTest.purge(newConfigurationWith30Days(system2), PurgeListener.EMPTY, new PurgeProfiler());
    dbTester.assertDbUnit(getClass(), "delete_file_sources_of_disabled_resources-result.xml", "file_sources");
  }

  @Test
  public void shouldDeleteHistoricalDataOfDirectoriesAndFiles() {
    dbTester.prepareDbUnit(getClass(), "shouldDeleteHistoricalDataOfDirectoriesAndFiles.xml");
    underTest.purge(new PurgeConfiguration(new IdUuidPair(1L, "1"), new String[]{Scopes.DIRECTORY, Scopes.FILE}, 30), PurgeListener.EMPTY, new PurgeProfiler());
    dbTester.assertDbUnit(getClass(), "shouldDeleteHistoricalDataOfDirectoriesAndFiles-result.xml", "projects", "snapshots");
  }

  @Test
  public void disable_resources_without_last_snapshot() {
    dbTester.prepareDbUnit(getClass(), "disable_resources_without_last_snapshot.xml");
    when(system2.now()).thenReturn(1450000000000L);
    underTest.purge(newConfigurationWith30Days(system2), PurgeListener.EMPTY, new PurgeProfiler());
    dbTester.assertDbUnit(getClass(), "disable_resources_without_last_snapshot-result.xml", new String[] {"issue_close_date", "issue_update_date"}, "projects", "snapshots",
      "issues");
  }

  @Test
  public void shouldDeleteSnapshots() {
    dbTester.prepareDbUnit(getClass(), "shouldDeleteSnapshots.xml");
    underTest.deleteSnapshots(PurgeSnapshotQuery.create().setIslast(false).setResourceId(1L), new PurgeProfiler());
    dbTester.assertDbUnit(getClass(), "shouldDeleteSnapshots-result.xml", "snapshots");
  }

  @Test
  public void shouldSelectPurgeableSnapshots() {
    dbTester.prepareDbUnit(getClass(), "shouldSelectPurgeableSnapshots.xml");
    List<PurgeableSnapshotDto> snapshots = underTest.selectPurgeableSnapshots(1L);

    assertThat(snapshots).hasSize(3);
    assertThat(getById(snapshots, 1L).isLast()).isTrue();
    assertThat(getById(snapshots, 1L).hasEvents()).isFalse();
    assertThat(getById(snapshots, 4L).isLast()).isFalse();
    assertThat(getById(snapshots, 4L).hasEvents()).isFalse();
    assertThat(getById(snapshots, 5L).isLast()).isFalse();
    assertThat(getById(snapshots, 5L).hasEvents()).isTrue();
  }

  @Test
  public void delete_project_and_associated_data() {
    dbTester.prepareDbUnit(getClass(), "shouldDeleteProject.xml");
    underTest.deleteProject(dbSession, "A");
    dbSession.commit();
    assertThat(dbTester.countRowsOfTable("projects")).isZero();
    assertThat(dbTester.countRowsOfTable("snapshots")).isZero();
    assertThat(dbTester.countRowsOfTable("action_plans")).isZero();
    assertThat(dbTester.countRowsOfTable("issues")).isZero();
    assertThat(dbTester.countRowsOfTable("issue_changes")).isZero();
    assertThat(dbTester.countRowsOfTable("file_sources")).isZero();
  }

  @Test
  public void delete_project_in_ce_activity_when_deleting_project() {
    ComponentDto projectToBeDeleted = ComponentTesting.newProjectDto();
    ComponentDto anotherLivingProject = ComponentTesting.newProjectDto();
    dbClient.componentDao().insert(dbSession, projectToBeDeleted, anotherLivingProject);

    // Insert 2 rows in CE_ACTIVITY : one for the project that will be deleted, and on on another project
    insertCeActivity(projectToBeDeleted.uuid());
    insertCeActivity(anotherLivingProject.uuid());
    dbSession.commit();

    underTest.deleteProject(dbSession, projectToBeDeleted.uuid());
    dbSession.commit();

    assertThat(dbTester.countRowsOfTable("ce_activity")).isEqualTo(1);
  }

  @Test
  public void delete_view_and_child() {
    dbTester.prepareDbUnit(getClass(), "view_sub_view_and_tech_project.xml");

    underTest.deleteProject(dbSession, "A");
    dbSession.commit();
    assertThat(dbTester.countSql("select count(id) from projects where uuid='A'")).isZero();
    assertThat(dbTester.countRowsOfTable("projects")).isZero();
  }

  @Test
  public void delete_view_sub_view_and_tech_project() {
    dbTester.prepareDbUnit(getClass(), "view_sub_view_and_tech_project.xml");

    // technical project
    underTest.deleteProject(dbSession, "D");
    dbSession.commit();
    assertThat(dbTester.countSql("select count(id) from projects where uuid='D'")).isZero();

    // sub view
    underTest.deleteProject(dbSession, "B");
    dbSession.commit();
    assertThat(dbTester.countSql("select count(id) from projects where uuid='B'")).isZero();

    // view
    underTest.deleteProject(dbSession, "A");
    dbSession.commit();
    assertThat(dbTester.countSql("select count(id) from projects where uuid='A'")).isZero();
  }

  @Test
  public void should_delete_old_closed_issues() {
    dbTester.prepareDbUnit(getClass(), "should_delete_old_closed_issues.xml");
    underTest.purge(newConfigurationWith30Days(), PurgeListener.EMPTY, new PurgeProfiler());
    dbTester.assertDbUnit(getClass(), "should_delete_old_closed_issues-result.xml", "issues", "issue_changes");
  }

  @Test
  public void should_delete_all_closed_issues() {
    dbTester.prepareDbUnit(getClass(), "should_delete_all_closed_issues.xml");
    underTest.purge(new PurgeConfiguration(new IdUuidPair(1L, "1"), new String[0], 0), PurgeListener.EMPTY, new PurgeProfiler());
    dbTester.assertDbUnit(getClass(), "should_delete_all_closed_issues-result.xml", "issues", "issue_changes");
  }

  private CeActivityDto insertCeActivity(String componentUuid) {
    CeQueueDto queueDto = new CeQueueDto();
    queueDto.setUuid(Uuids.create());
    queueDto.setTaskType(REPORT);
    queueDto.setComponentUuid(componentUuid);
    queueDto.setSubmitterLogin("henri");
    queueDto.setCreatedAt(1_300_000_000_000L);

    CeActivityDto dto = new CeActivityDto(queueDto);
    dto.setStatus(CeActivityDto.Status.SUCCESS);
    dto.setStartedAt(1_500_000_000_000L);
    dto.setExecutedAt(1_500_000_000_500L);
    dto.setExecutionTimeMs(500L);
    dbClient.ceActivityDao().insert(dbSession, dto);
    return dto;
  }

  private static PurgeableSnapshotDto getById(List<PurgeableSnapshotDto> snapshots, long id) {
    for (PurgeableSnapshotDto snapshot : snapshots) {
      if (snapshot.getSnapshotId() == id) {
        return snapshot;
      }
    }
    return null;
  }

  private static PurgeConfiguration newConfigurationWith30Days() {
    return new PurgeConfiguration(new IdUuidPair(1L, "1"), new String[0], 30);
  }

  private static PurgeConfiguration newConfigurationWith30Days(System2 system2) {
    return new PurgeConfiguration(new IdUuidPair(1L, "1"), new String[0], 30, system2);
  }
}
